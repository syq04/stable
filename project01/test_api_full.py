#!/usr/bin/env python3
"""API全面测试脚本 - 07阶段测试联调"""
import requests
import json

BASE_URL = "http://localhost:8080"

def test_login():
    """测试登录接口"""
    print("=" * 50)
    print("测试1: 用户登录")
    print("=" * 50)
    
    url = f"{BASE_URL}/api/auth/login"
    data = {
        "email": "admin@nebula.com",
        "password": "admin123"
    }
    
    try:
        response = requests.post(url, json=data)
        print(f"状态码: {response.status_code}")
        print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 200:
                token = result["data"]["token"]
                print(f"✅ 登录成功! Token已获取")
                return token
            else:
                print(f"❌ 登录失败: {result.get('message')}")
                return None
        else:
            print(f"❌ 请求失败: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ 异常: {e}")
        return None

def test_get_profile(token):
    """测试获取用户资料"""
    print("\n" + "=" * 50)
    print("测试2: 获取用户资料")
    print("=" * 50)
    
    url = f"{BASE_URL}/api/user/profile"
    headers = {"Authorization": f"Bearer {token}"}
    
    try:
        response = requests.get(url, headers=headers)
        print(f"状态码: {response.status_code}")
        print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        
        if response.status_code == 200:
            print("✅ 获取用户资料成功!")
            return True
        else:
            print(f"❌ 获取失败")
            return False
    except Exception as e:
        print(f"❌ 异常: {e}")
        return False

def test_register():
    """测试注册接口"""
    print("\n" + "=" * 50)
    print("测试3: 用户注册")
    print("=" * 50)
    
    url = f"{BASE_URL}/api/auth/register"
    data = {
        "username": "testuser",
        "email": "test@example.com",
        "password": "test123456"
    }
    
    try:
        response = requests.post(url, json=data)
        print(f"状态码: {response.status_code}")
        print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        
        if response.status_code == 200:
            print("✅ 注册成功!")
            return True
        else:
            print(f"❌ 注册失败 (可能用户已存在)")
            return False
    except Exception as e:
        print(f"❌ 异常: {e}")
        return False

def test_styles(token):
    """测试风格列表接口"""
    print("\n" + "=" * 50)
    print("测试4: 获取风格列表")
    print("=" * 50)
    
    url = f"{BASE_URL}/api/styles/active"
    headers = {"Authorization": f"Bearer {token}"}
    
    try:
        response = requests.get(url, headers=headers)
        print(f"状态码: {response.status_code}")
        print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        
        if response.status_code == 200:
            print("✅ 获取风格列表成功!")
            return True
        else:
            print(f"❌ 获取失败")
            return False
    except Exception as e:
        print(f"❌ 异常: {e}")
        return False

def test_admin_users(token):
    """测试管理员获取用户列表"""
    print("\n" + "=" * 50)
    print("测试5: 管理员获取用户列表")
    print("=" * 50)
    
    url = f"{BASE_URL}/api/admin/users"
    headers = {"Authorization": f"Bearer {token}"}
    
    try:
        response = requests.get(url, headers=headers)
        print(f"状态码: {response.status_code}")
        print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        
        if response.status_code == 200:
            print("✅ 获取管理员用户列表成功!")
            return True
        else:
            print(f"❌ 获取失败")
            return False
    except Exception as e:
        print(f"❌ 异常: {e}")
        return False

def test_unauthorized():
    """测试未授权访问"""
    print("\n" + "=" * 50)
    print("测试6: 未授权访问测试")
    print("=" * 50)
    
    url = f"{BASE_URL}/api/user/profile"
    
    try:
        response = requests.get(url)
        print(f"状态码: {response.status_code}")
        print(f"响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        
        if response.status_code == 401:
            print("✅ 未授权访问被正确拦截!")
            return True
        else:
            print(f"❌ 未授权访问未被拦截")
            return False
    except Exception as e:
        print(f"❌ 异常: {e}")
        return False

def main():
    """主测试流程"""
    print("\n" + "#" * 50)
    print("# 07阶段 - API接口测试")
    print("#" * 50)
    
    # 测试1: 登录
    token = test_login()
    if not token:
        print("\n❌ 登录失败，后续测试终止")
        return
    
    # 测试2: 获取用户资料
    test_get_profile(token)
    
    # 测试3: 注册新用户
    test_register()
    
    # 测试4: 获取风格列表
    test_styles(token)
    
    # 测试5: 管理员接口
    test_admin_users(token)
    
    # 测试6: 未授权访问
    test_unauthorized()
    
    print("\n" + "#" * 50)
    print("# API测试完成")
    print("#" * 50)

if __name__ == "__main__":
    main()
