import os
import io
import glob
import time
import base64
import logging
import threading
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Optional

os.environ.setdefault("CUDA_LAUNCH_BLOCKING", "1")

import torch
from PIL import Image
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from diffusers import StableDiffusionPipeline, DPMSolverMultistepScheduler, EulerDiscreteScheduler, \
    EulerAncestralDiscreteScheduler, DDIMScheduler, UniPCMultistepScheduler

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_DIR = os.environ.get("SD_MODEL_DIR", os.path.join(os.path.dirname(__file__), "..", "sd-models"))
MODEL_DIR = os.path.abspath(MODEL_DIR)

SAMPLER_MAP = {
    "Euler": EulerDiscreteScheduler,
    "Euler a": EulerAncestralDiscreteScheduler,
    "DPM++ 2M Karras": lambda: DPMSolverMultistepScheduler(use_karras_sigmas=True),
    "DPM++ SDE Karras": lambda: DPMSolverMultistepScheduler(use_karras_sigmas=True, algorithm_type="sde-dpmsolver++"),
    "DDIM": DDIMScheduler,
    "UniPC": UniPCMultistepScheduler,
}

current_pipeline = None
current_checkpoint = None
_pipeline_lock = threading.Lock()


class GenerateRequest(BaseModel):
    prompt: str
    negative_prompt: str = ""
    width: int = Field(default=512, ge=256, le=2048)
    height: int = Field(default=512, ge=256, le=2048)
    steps: int = Field(default=20, ge=1, le=150)
    cfg_scale: float = Field(default=7.5, ge=1.0, le=30.0)
    seed: Optional[int] = None
    sampler_name: Optional[str] = None
    checkpoint_name: Optional[str] = None
    task_id: Optional[str] = None


_progress_store: dict = {}


class ProgressResponse(BaseModel):
    task_id: str
    step: int
    total_steps: int
    elapsed: float
    its: float
    finished: bool


class GenerateResponse(BaseModel):
    success: bool
    image_base64: Optional[str] = None
    seed: Optional[int] = None
    error_message: Optional[str] = None


class ModelInfo(BaseModel):
    name: str
    filename: str
    path: str
    size_mb: float


def scan_models():
    models = []
    if not os.path.isdir(MODEL_DIR):
        return models
    for ext in ("*.safetensors",):
        for filepath in glob.glob(os.path.join(MODEL_DIR, "**", ext), recursive=True):
            filename = os.path.basename(filepath)
            name = os.path.splitext(filename)[0]
            size_mb = os.path.getsize(filepath) / (1024 * 1024)
            models.append(ModelInfo(
                name=name,
                filename=filename,
                path=filepath,
                size_mb=round(size_mb, 2)
            ))
    return models


def get_pipeline(checkpoint_name: Optional[str]):
    global current_pipeline, current_checkpoint

    models = scan_models()
    if not models:
        raise HTTPException(status_code=503, detail=f"No .safetensors models found in {MODEL_DIR}")

    target_model = None
    if checkpoint_name:
        for m in models:
            if m.name == checkpoint_name or m.filename == checkpoint_name:
                target_model = m
                break
        if not target_model:
            raise HTTPException(status_code=404, detail=f"Model not found: {checkpoint_name}")
    else:
        target_model = models[0]

    with _pipeline_lock:
        if current_pipeline is not None and current_checkpoint == target_model.path:
            return current_pipeline

        logger.info(f"Loading model: {target_model.filename} from {target_model.path}")

        if current_pipeline is not None:
            del current_pipeline
            if torch.cuda.is_available():
                torch.cuda.empty_cache()

        device = "cuda" if torch.cuda.is_available() else "cpu"
        dtype = torch.float16 if device == "cuda" else torch.float32

        try:
            pipe = StableDiffusionPipeline.from_single_file(
                target_model.path,
                torch_dtype=dtype,
                safety_checker=None,
                requires_safety_checker=False,
            )
        except Exception as e:
            logger.warning(f"First load attempt failed ({e}), retrying with hub download...")
            pipe = StableDiffusionPipeline.from_single_file(
                target_model.path,
                torch_dtype=dtype,
                safety_checker=None,
                requires_safety_checker=False,
                local_files_only=False,
            )
        pipe = pipe.to(device)

        if device == "cuda":
            pipe.enable_attention_slicing()

        current_pipeline = pipe
        current_checkpoint = target_model.path
        logger.info(f"Model loaded successfully on {device}: {target_model.filename}")
        return pipe


def get_scheduler(sampler_name: Optional[str]):
    if not sampler_name or sampler_name not in SAMPLER_MAP:
        return None
    scheduler_cls = SAMPLER_MAP[sampler_name]
    if callable(scheduler_cls) and not isinstance(scheduler_cls, type):
        return scheduler_cls()
    return scheduler_cls()


def ensure_config_cache():
    from huggingface_hub import snapshot_download
    import warnings
    with warnings.catch_warnings():
        warnings.filterwarnings("ignore", category=UserWarning, module="huggingface_hub")
        try:
            snapshot_download(
                "runwayml/stable-diffusion-v1-5",
                allow_patterns=["*.json", "*.txt", "*.md", "model_index.json"],
                ignore_patterns=["*.bin", "*.safetensors", "*.ckpt", "*.pth", "vae/**"],
            )
            logger.info("HF config cache ready for SD 1.5")
        except Exception as e:
            logger.warning(f"Failed to cache HF config (will retry on first request): {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    ensure_config_cache()
    try:
        import psutil
        avail_gb = psutil.virtual_memory().available / (1024**3)
        if avail_gb < 9:
            logger.warning(f"Low memory: {avail_gb:.1f} GB free, model loading may be slow. Close other apps if possible.")
    except ImportError:
        logger.warning("psutil not available, skipping memory check")
    models = scan_models()
    if models:
        logger.info(f"Warming up model: {models[0].filename}")
        try:
            get_pipeline(None)
            logger.info(f"Model warmup complete on {torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'CPU'}")
        except Exception as e:
            logger.warning(f"Model warmup failed (will load on first request): {e}")
    yield


app = FastAPI(title="Local Model Inference Service", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    models = scan_models()
    return {
        "status": "ok",
        "model_dir": MODEL_DIR,
        "models_count": len(models),
        "device": "cuda" if torch.cuda.is_available() else "cpu",
        "current_model": os.path.basename(current_checkpoint) if current_checkpoint else None
    }


@app.get("/models")
def list_models():
    models = scan_models()
    return {"models": [m.model_dump() for m in models]}


@app.post("/generate", response_model=GenerateResponse)
def generate(req: GenerateRequest):
    task_id = req.task_id
    try:
        pipe = get_pipeline(req.checkpoint_name)

        original_scheduler = pipe.scheduler
        scheduler = get_scheduler(req.sampler_name)
        if scheduler is not None:
            if isinstance(scheduler, type(pipe.scheduler)):
                pipe.scheduler = scheduler.from_config(pipe.scheduler.config)
            else:
                pipe.scheduler = scheduler

        actual_seed = req.seed if req.seed is not None else int(time.time_ns()) % (2**31)
        generator = torch.Generator(device=pipe.device).manual_seed(actual_seed)

        generate_kwargs = dict(
            prompt=req.prompt,
            negative_prompt=req.negative_prompt if req.negative_prompt else None,
            width=req.width,
            height=req.height,
            num_inference_steps=req.steps,
            guidance_scale=req.cfg_scale,
            generator=generator,
        )

        if task_id:
            _progress_store[task_id] = {
                "step": 0,
                "total_steps": req.steps,
                "elapsed": 0.0,
                "its": 0.0,
                "finished": False
            }
            start_time = time.time()

            def callback_on_step_end(pipeline, step, timestep, callback_kwargs):
                elapsed = time.time() - start_time
                its = (step + 1) / elapsed if elapsed > 0 else 0.0
                _progress_store[task_id] = {
                    "step": step + 1,
                    "total_steps": req.steps,
                    "elapsed": elapsed,
                    "its": its,
                    "finished": False
                }

            generate_kwargs["callback_on_step_end"] = callback_on_step_end

        result = pipe(**generate_kwargs)

        pipe.scheduler = original_scheduler

        image: Image.Image = result.images[0]
        buffer = io.BytesIO()
        image.save(buffer, format="PNG")
        image_b64 = base64.b64encode(buffer.getvalue()).decode("utf-8")

        if task_id:
            elapsed = time.time() - start_time
            its = req.steps / elapsed if elapsed > 0 else 0
            _progress_store[task_id] = {
                "step": req.steps,
                "total_steps": req.steps,
                "elapsed": elapsed,
                "its": its,
                "finished": True
            }

        return GenerateResponse(
            success=True,
            image_base64=image_b64,
            seed=actual_seed
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Generation failed: {e}", exc_info=True)
        if task_id and task_id in _progress_store:
            del _progress_store[task_id]
        return GenerateResponse(
            success=False,
            error_message=str(e)
        )


@app.get("/progress/{task_id}")
def get_progress(task_id: str):
    if task_id not in _progress_store:
        return {"task_id": task_id, "step": 0, "total_steps": 30, "elapsed": 0, "its": 0, "finished": False}
    p = _progress_store[task_id]
    if p["finished"]:
        del _progress_store[task_id]
    return p




if __name__ == "__main__":
    import uvicorn
    os.makedirs(MODEL_DIR, exist_ok=True)
    logger.info(f"Model directory: {MODEL_DIR}")
    uvicorn.run(app, host="0.0.0.0", port=5000)
