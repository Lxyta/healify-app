"""
Healify AI 功能测试脚本
========================
测试流程：注册 → 建档案 → 造多周体重数据 → 生成计划 → AI分析/优化

用法：
  python test_ai_features.py             # 完整流程
  python test_ai_features.py --quick     # 跳过服务检查，快速跑
  python test_ai_features.py --steps     # 逐步确认模式
"""

import requests
import json
import sys
import time
import random
from datetime import date, timedelta

BASE_URL = "http://localhost:8080"
AI_URL = "http://localhost:8000"
TOKEN = None
USERNAME = f"test_{random.randint(1000, 9999)}"
EMAIL = f"{USERNAME}@test.com"
PASSWORD = "test123456"

GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
RESET = "\033[0m"


def log(step, msg, ok=True):
    color = GREEN if ok else RED
    print(f"{color}[{step}]{RESET} {msg}")


def step_header(title):
    print(f"\n{'='*55}")
    print(f"{CYAN}  {title}{RESET}")
    print(f"{'='*55}")


def check_services():
    """检查后端和 AI 服务是否在线"""
    step_header("检查服务状态")

    try:
        r = requests.get(f"{AI_URL}/api/ai/health", timeout=3)
        log("AI服务", f"✅ 在线 — {r.json()}", True)
    except Exception as e:
        log("AI服务", f"❌ 无法连接 {AI_URL} — 请先启动: cd ai-service && uvicorn main:app --port 8000", False)
        return False

    try:
        r = requests.get(f"{BASE_URL}/api/auth/login", timeout=3)
        log("后端", "✅ 在线 (8080)", True)
    except Exception as e:
        log("后端", f"❌ 无法连接 {BASE_URL} — 请先启动: cd backend && mvnw spring-boot:run", False)
        return False

    return True


def register():
    """1. 注册新用户"""
    step_header(f"1. 注册用户 ({USERNAME})")

    resp = requests.post(f"{BASE_URL}/api/auth/register", json={
        "username": USERNAME,
        "email": EMAIL,
        "password": PASSWORD,
        "nickname": f"测试{USERNAME[-2:]}"
    })
    data = resp.json()
    global TOKEN
    TOKEN = data.get("token")

    if TOKEN:
        log("注册", f"✅ userId={data['userId']}, token={TOKEN[:30]}...")
        return TOKEN
    else:
        log("注册", f"❌ {data}", False)
        # 可能已存在，尝试登录
        return login()


def login():
    """备选：登录已有用户"""
    resp = requests.post(f"{BASE_URL}/api/auth/login", json={
        "username": USERNAME, "password": PASSWORD
    })
    data = resp.json()
    global TOKEN
    TOKEN = data.get("token")
    if TOKEN:
        log("登录", f"✅ userId={data['userId']}", True)
        return TOKEN
    log("登录", f"❌ {data}", False)
    sys.exit(1)


def auth_headers():
    return {"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"}


def create_profile():
    """2. 创建健康档案"""
    step_header("2. 创建健康档案")

    profile = {
        "gender": "MALE",
        "birthDate": "1990-05-15",
        "heightCm": 175.0,
        "currentWeightKg": 82.5,
        "targetWeightKg": 72.0,
        "activityLevel": "MODERATE",
        "dietPreference": "OMNIVORE",
        "allergies": '["花生"]',
        "dailyCalorieGoal": 1800,
        "dailyProteinG": 100,
        "exerciseFrequency": 4,
        "exerciseDuration": 45
    }

    resp = requests.put(f"{BASE_URL}/api/profile", json=profile, headers=auth_headers())
    if resp.status_code == 200:
        data = resp.json()
        log("档案", f"✅ 身高{data['heightCm']}cm 当前{data['currentWeightKg']}kg 目标{data['targetWeightKg']}kg")
        return data
    else:
        log("档案", f"❌ {resp.status_code} {resp.text}", False)
        sys.exit(1)


def seed_weight_history():
    """
    3. 伪造近 4 周体重记录（模拟真实使用场景）

    直接用 API 逐条写入（绕过"每周一条"限制，每条约 7 天前）。
    场景：从 82.5kg 开始减重，每周下降约 0.5-0.8kg。
    """
    step_header("3. 伪造 4 周体重历史")

    # 模拟 4 周体重变化（减重趋势）
    weights = [
        (28, 82.5, "初始记录"),
        (21, 82.0, "开始控制饮食"),
        (14, 81.3, "加入有氧运动"),
        (7,  80.7, "体重稳步下降"),
    ]

    records = []
    for days_ago, kg, note in weights:
        dto = {"weightKg": kg, "note": note}
        # 注意：POST /api/weight 会记录到"本周"，我们这里需要改 recordedAt
        # 直接通过 API 写会定位到本周，需要用 record-and-optimize 但会被覆盖
        # 折中：调 POST /api/weight 记录，然后在 DB 里改日期

        # 实际上 /api/weight 的 recordedAt 默认是 today
        # 为了测试，我们直接用 record-and-optimize 带不同体重值来造数据
        # 但那个也会生成计划...
        # 最简单：直接调 POST /api/weight 然后用 DB UPDATE 改日期

        resp = requests.post(f"{BASE_URL}/api/weight", json=dto, headers=auth_headers())
        if resp.status_code == 200:
            data = resp.json()
            records.append((data["id"], days_ago, kg, note))
            log("体重记录", f"✅ id={data['id']} {kg}kg — {note}")
        else:
            log("体重记录", f"⚠️ {resp.status_code} {resp.text}", False)

    if records:
        print(f"\n{YELLOW}  ⚠ 注意：体重记录的 recorded_at 日期全是今天。")
        print(f"  需要手动在数据库中修改日期，才能让 AI 看到趋势：{RESET}")
        print()
        for rid, days_ago, kg, _ in records:
            target_date = date.today() - timedelta(days=days_ago)
            print(f"  UPDATE weight_records SET recorded_at = '{target_date}' WHERE id = {rid};")
        print()
        print(f"{YELLOW}  或者在 Apifox 中调 /api/weight/record-and-optimize 多次，")
        print(f"  每次手动改请求体中的 weightKg 值来模拟多周记录。{RESET}")

    return records


def generate_plan():
    """4. 生成初始计划"""
    step_header("4. AI 生成一周计划")

    print(f"  正在调用 AI 生成个性化计划（可能需要 10-30 秒）...")
    resp = requests.post(f"{BASE_URL}/api/plans/generate", headers=auth_headers())

    if resp.status_code == 200:
        data = resp.json()
        total_meals = sum(len(d.get("meals", [])) for d in data.get("days", []))
        total_ex = sum(len(d.get("exercises", [])) for d in data.get("days", []))
        log("生成计划", f"✅ {data['weekStartDate']} — {total_meals}餐 {total_ex}项运动")
        return data
    else:
        log("生成计划", f"❌ {resp.status_code} {resp.text}", False)
        return None


def test_trend_analysis():
    """5. 测试 AI 趋势分析"""
    step_header("5. AI 体重趋势分析 (GET /api/plans/trend-analysis)")

    print(f"  调用 AI 分析体重变化趋势...")
    resp = requests.get(f"{BASE_URL}/api/plans/trend-analysis", headers=auth_headers())

    if resp.status_code == 200:
        data = resp.json()
        log("趋势方向", f"{data.get('trend', 'N/A')}")
        log("总变化量", f"{data.get('changeKg', 'N/A')} kg")
        log("周均速率", f"{data.get('ratePerWeek', 'N/A')} kg/周")
        log("AI评估", data.get("assessment", "")[:80] + "...")
        log("AI建议", data.get("suggestion", "")[:80] + "...")
        return data
    else:
        log("趋势分析", f"❌ {resp.status_code} {resp.text}", False)
        return None


def test_record_and_optimize():
    """6. 测试核心接口：记录体重 + 分析 + 优化"""
    step_header("6. 核心：记录体重+AI分析+优化 (POST /api/weight/record-and-optimize)")

    # 记录一个新体重（模拟本周最新称重）
    new_weight = 80.2
    print(f"  记录本周体重 {new_weight}kg，并触发 AI 分析与优化...")
    resp = requests.post(
        f"{BASE_URL}/api/weight/record-and-optimize",
        json={"weightKg": new_weight, "note": "本周核心测试"},
        headers=auth_headers()
    )

    if resp.status_code == 200:
        data = resp.json()
        log("趋势方向", f"{data.get('trend', 'N/A')}")
        log("总变化量", f"{data.get('changeKg', 'N/A')} kg")
        log("周均速率", f"{data.get('ratePerWeek', 'N/A')} kg/周")
        log("计划有变", f"{data.get('planChanged', False)}")
        log("变化摘要", data.get("planChangeSummary", "无"))

        opt_plan = data.get("optimizedPlan")
        if opt_plan:
            total_meals = sum(len(d.get("meals", [])) for d in opt_plan.get("days", []))
            total_ex = sum(len(d.get("exercises", [])) for d in opt_plan.get("days", []))
            log("优化后计划", f"{opt_plan['weekStartDate']} — {total_meals}餐 {total_ex}项运动")

        return data
    else:
        log("核心接口", f"❌ {resp.status_code} {resp.text}", False)
        return None


def print_summary():
    """打印测试使用的凭据和 curl 速查"""
    step_header("测试总结")
    print(f"""
  {CYAN}用户凭据{RESET}
    username : {USERNAME}
    password : {PASSWORD}
    token    : {TOKEN[:40] if TOKEN else '无'}...

  {CYAN}Apifox 环境变量{RESET}
    base_url : {BASE_URL}
    token    : {TOKEN}

  {CYAN}手动 curl 测试（复制即用）{RESET}

    # 查看当前计划
    curl -s {BASE_URL}/api/plans/current \\
      -H "Authorization: Bearer {TOKEN}" | python -m json.tool

    # AI 趋势分析
    curl -s {BASE_URL}/api/plans/trend-analysis \\
      -H "Authorization: Bearer {TOKEN}" | python -m json.tool

    # 记录体重 + AI 优化（核心）
    curl -s -X POST {BASE_URL}/api/weight/record-and-optimize \\
      -H "Authorization: Bearer {TOKEN}" \\
      -H "Content-Type: application/json" \\
      -d '{{"weightKg": 80.0, "note": "测试"}}' | python -m json.tool

    # 体重历史
    curl -s {BASE_URL}/api/weight/history \\
      -H "Authorization: Bearer {TOKEN}" | python -m json.tool
""")


# ============================================================
if __name__ == "__main__":
    quick = "--quick" in sys.argv
    steps = "--steps" in sys.argv

    print(f"{CYAN}")
    print("  ╔══════════════════════════════════════════╗")
    print("  ║     Healify AI 功能测试脚本              ║")
    print("  ╚══════════════════════════════════════════╝")
    print(f"{RESET}")

    if not quick:
        if not check_services():
            sys.exit(1)
    else:
        print("  ⚡ 跳过服务检查 (--quick)")

    # 1. 注册/登录
    register()

    if steps:
        input(f"\n  {YELLOW}按回车继续 → 创建健康档案...{RESET}")

    # 2. 建档案
    create_profile()

    if steps:
        input(f"\n  {YELLOW}按回车继续 → 伪造体重历史...{RESET}")

    # 3. 造体重数据
    seed_weight_history()

    if steps:
        input(f"\n  {YELLOW}按回车继续 → AI 生成初始计划...{RESET}")

    # 4. 生成初始计划
    plan = generate_plan()
    if not plan:
        print(f"\n{RED}  计划生成失败，后续测试可能无意义。检查 AI 服务是否正常。{RESET}")
        if not steps:
            sys.exit(1)

    if steps:
        input(f"\n  {YELLOW}按回车继续 → AI 趋势分析...{RESET}")

    # 5. 趋势分析
    test_trend_analysis()

    if steps:
        input(f"\n  {YELLOW}按回车继续 → 核心接口测试...{RESET}")

    # 6. 核心：记录体重 + 分析 + 优化
    test_record_and_optimize()

    # 汇总
    print_summary()
