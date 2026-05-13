#!/usr/bin/env python3
"""API全面测试脚本 - 07阶段测试联调（完整版）"""
import requests
import json

BASE_URL = "http://localhost:8080"

def test_login(email="admin@nebula.com", password="admin123"):
    """测试登录接口"""
    print("=" * 50)
    print("测试: 用户登录")
    print("=" * 50)
    
    url = BASE_URL + "/api/auth/login"
    data = {"email": email, "password": password}
    
    try:
        response = requests.post(url, json=data)
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                token = result["data"]["token"]
                print("登录成功! 用户: " + result['data']['username'] + ", 角色: " + result['data']['role'])
                return token
            else:
                print("登录失败: " + str(result.get('message')))
        else:
            print("请求失败: " + str(response.status_code))
    except Exception as e:
        print("异常: " + str(e))
    return None

def test_text2image(token):
    """测试文生图接口"""
    print("\n" + "=" * 50)
    print("测试: 文生图")
    print("=" * 50)
    
    url = BASE_URL + "/api/text2image"
    headers = {"Authorization": "Bearer " + token}
    data = {
        "prompt": "一只可爱的猫咪",
        "styleId": 1,
        "imageSize": "512x512"
    }
    
    try:
        response = requests.post(url, json=data, headers=headers)
        print("状态码: " + str(response.status_code))
        result = response.json()
        print("响应: " + json.dumps(result, indent=2, ensure_ascii=False))
        
        if response.status_code == 200 and result.get("code") == 200:
            print("文生图请求成功!")
            return True
        else:
            print("文生图请求处理: " + str(result.get('message', '未知错误')))
            return False
    except Exception as e:
        print("异常: " + str(e))
        return False

def test_image2text(token):
    """测试图生文接口"""
    print("\n" + "=" * 50)
    print("测试: 图生文")
    print("=" * 50)
    
    url = BASE_URL + "/api/image2text"
    headers = {"Authorization": "Bearer " + token}
    data = {
        "imageUrl": "https://picsum.photos/seed/test/400/300",
        "prompt": "描述这张图片"
    }
    
    try:
        response = requests.post(url, json=data, headers=headers)
        print("状态码: " + str(response.status_code))
        result = response.json()
        print("响应: " + json.dumps(result, indent=2, ensure_ascii=False))
        
        if response.status_code == 200 and result.get("code") == 200:
            print("图生文请求成功!")
            return True
        else:
            print("图生文请求处理: " + str(result.get('message', '未知错误')))
            return False
    except Exception as e:
        print("异常: " + str(e))
        return False

def test_training_task(token):
    """测试训练任务接口"""
    print("\n" + "=" * 50)
    print("测试: 训练任务")
    print("=" * 50)
    
    url = BASE_URL + "/api/training/tasks"
    headers = {"Authorization": "Bearer " + token}
    
    try:
        response = requests.get(url, headers=headers)
        print("状态码: " + str(response.status_code))
        result = response.json()
        print("响应: " + json.dumps(result, indent=2, ensure_ascii=False))
        
        if response.status_code == 200 and result.get("code") == 200:
            print("获取训练任务列表成功!")
            return True
        else:
            print("训练任务请求: " + str(result.get('message', '未知错误')))
            return False
    except Exception as e:
        print("异常: " + str(e))
        return False

def test_admin_dashboard(token):
    """测试管理员仪表盘"""
    print("\n" + "=" * 50)
    print("测试: 管理员仪表盘")
    print("=" * 50)
    
    url = BASE_URL + "/api/admin/dashboard/stats"
    headers = {"Authorization": "Bearer " + token}
    
    try:
        response = requests.get(url, headers=headers)
        print("状态码: " + str(response.status_code))
        result = response.json()
        print("响应: " + json.dumps(result, indent=2, ensure_ascii=False))
        
        if response.status_code == 200 and result.get("code") == 200:
            print("获取管理员仪表盘数据成功!")
            return True
        else:
            print("管理员仪表盘: " + str(result.get('message', '未知错误')))
            return False
    except Exception as e:
        print("异常: " + str(e))
        return False

def test_admin_configs(token):
    """测试系统配置接口"""
    print("\n" + "=" * 50)
    print("测试: 系统配置")
    print("=" * 50)
    
    url = BASE_URL + "/api/admin/configs"
    headers = {"Authorization": "Bearer " + token}
    
    try:
        response = requests.get(url, headers=headers)
        print("状态码: " + str(response.status_code))
        result = response.json()
        print("响应: " + json.dumps(result, indent=2, ensure_ascii=False))
        
        if response.status_code == 200 and result.get("code") == 200:
            print("获取系统配置成功!")
            return True
        else:
            print("系统配置: " + str(result.get('message', '未知错误')))
            return False
    except Exception as e:
        print("异常: " + str(e))
        return False

def main():
    """主测试流程"""
    print("\n" + "#" * 50)
    print("# 07阶段 - 完整API联调测试")
    print("#" * 50)
    
    # 测试登录
    token = test_login()
    if not token:
        print("\n登录失败，测试终止")
        return
    
    # 测试各个功能模块
    print("\n【业务功能测试】")
    test_text2image(token)
    test_image2text(token)
    test_training_task(token)
    
    # 测试管理员功能
    print("\n【管理员功能测试】")
    test_admin_dashboard(token)
    test_admin_configs(token)
    
    print("\n" + "#" * 50)
    print("# API联调测试完成")
    print("# 请在前端浏览器中继续手动测试页面功能")
    print("#" * 50)

if __name__ == "__main__":
    main()
