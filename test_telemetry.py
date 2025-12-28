import requests
import uuid
import json
from datetime import datetime

BASE_URL = "https://x.loratech.dev/api"

session = requests.Session()
session.trust_env = False

def print_result(name, response):
    print(f"\n{'='*50}")
    print(f"TEST: {name}")
    print(f"Status: {response.status_code}")
    try:
        data = response.json()
        print(f"Response: {json.dumps(data, indent=2, ensure_ascii=False)[:500]}")
    except:
        print(f"Response: {response.text[:500]}")
    print(f"{'='*50}")

def test_post_telemetry():
    server_id = str(uuid.uuid4())
    payload = {
        "server_id": server_id,
        "plugin_version": "1.0.0-TEST",
        "minecraft_version": "1.21.4",
        "java_version": "21",
        "system_info": {
            "os": "Windows 11",
            "arch": "amd64",
            "cores": 8
        },
        "sent_at": datetime.now().isoformat(),
        "events": [
            {
                "type": "STARTUP",
                "timestamp": datetime.now().isoformat(),
                "data": {
                    "java_version": "21",
                    "os_name": "Windows 11",
                    "os_arch": "amd64"
                }
            },
            {
                "type": "SYSTEM_HEALTH",
                "timestamp": datetime.now().isoformat(),
                "data": {
                    "tps": 19.8,
                    "avg_tps": 19.5,
                    "min_tps": 18.0,
                    "used_memory_mb": 2048,
                    "max_memory_mb": 4096,
                    "online_players": 15,
                    "max_players": 100,
                    "active_mutes": 2,
                    "loaded_chunks": 500,
                    "total_entities": 1200,
                    "plugin_load_time_ms": 150,
                    "uptime_minutes": 120
                }
            },
            {
                "type": "USAGE",
                "timestamp": datetime.now().isoformat(),
                "data": {
                    "action": "moderation",
                    "category": "toxicity",
                    "feature": "chat_filter",
                    "violations_count": 25,
                    "messages_processed": 1500,
                    "api_calls": 1400,
                    "api_failures": 5,
                    "api_success_rate": 99.6,
                    "uptime_minutes": 120
                }
            },
            {
                "type": "FILTER",
                "timestamp": datetime.now().isoformat(),
                "data": {
                    "total_checks": 1500,
                    "total_blocks": 45,
                    "block_rate_percent": 3.0,
                    "triggers_by_type": {"SPAM": 20, "FLOOD": 15, "LINK": 10},
                    "bypasses_by_type": {"SPAM": 2, "LINK": 1}
                }
            },
            {
                "type": "CACHE",
                "timestamp": datetime.now().isoformat(),
                "data": {
                    "cache_hits": 800,
                    "cache_misses": 200,
                    "hit_rate_percent": 80.0,
                    "cache_size": 500,
                    "cache_max_size": 1000
                }
            },
            {
                "type": "ERROR",
                "timestamp": datetime.now().isoformat(),
                "data": {
                    "error_type": "APITimeoutException",
                    "message": "API request timed out after 5000ms",
                    "context": "ChatListener.moderate",
                    "stack_trace": "at dev.loratech.guard.api.LoraApiClient.moderate(LoraApiClient.java:55)"
                }
            }
        ]
    }
    
    response = session.post(f"{BASE_URL}/telemetry", json=payload)
    print_result("POST /telemetry", response)
    return server_id

def test_get_stats():
    response = session.get(f"{BASE_URL}/telemetry/stats", params={"hours": 24})
    print_result("GET /telemetry/stats", response)

def test_get_servers():
    response = session.get(f"{BASE_URL}/telemetry/servers")
    print_result("GET /telemetry/servers", response)

def test_get_health():
    response = session.get(f"{BASE_URL}/telemetry/health", params={"hours": 24})
    print_result("GET /telemetry/health", response)

def test_get_errors():
    response = session.get(f"{BASE_URL}/telemetry/errors", params={"limit": 10})
    print_result("GET /telemetry/errors", response)

def test_get_usage():
    response = session.get(f"{BASE_URL}/telemetry/usage", params={"hours": 24, "limit": 10})
    print_result("GET /telemetry/usage", response)

def test_get_filter():
    response = session.get(f"{BASE_URL}/telemetry/filter", params={"hours": 24})
    print_result("GET /telemetry/filter", response)

def test_get_cache():
    response = session.get(f"{BASE_URL}/telemetry/cache", params={"hours": 24})
    print_result("GET /telemetry/cache", response)

def test_get_sessions():
    response = session.get(f"{BASE_URL}/telemetry/sessions", params={"limit": 10})
    print_result("GET /telemetry/sessions", response)

def test_get_categories():
    response = session.get(f"{BASE_URL}/telemetry/categories", params={"hours": 24})
    print_result("GET /telemetry/categories", response)

def test_get_features():
    response = session.get(f"{BASE_URL}/telemetry/features")
    print_result("GET /telemetry/features", response)

def test_get_hourly():
    response = session.get(f"{BASE_URL}/telemetry/hourly")
    print_result("GET /telemetry/hourly", response)

def test_get_config():
    response = session.get(f"{BASE_URL}/telemetry/config")
    print_result("GET /telemetry/config", response)

def test_get_performance():
    response = session.get(f"{BASE_URL}/telemetry/performance", params={"hours": 24})
    print_result("GET /telemetry/performance", response)

def test_get_punishments():
    response = session.get(f"{BASE_URL}/telemetry/punishments", params={"hours": 24})
    print_result("GET /telemetry/punishments", response)

if __name__ == "__main__":
    print("\n" + "="*60)
    print("LORAGUARD TELEMETRY API TEST")
    print(f"Base URL: {BASE_URL}")
    print("="*60)
    
    print("\n[1] Testing POST /telemetry (sending test data)...")
    server_id = test_post_telemetry()
    
    print("\n[2] Testing GET endpoints...")
    test_get_stats()
    test_get_servers()
    test_get_health()
    test_get_errors()
    test_get_usage()
    test_get_filter()
    test_get_cache()
    test_get_sessions()
    test_get_categories()
    test_get_features()
    test_get_hourly()
    test_get_config()
    test_get_performance()
    test_get_punishments()
    
    print("\n" + "="*60)
    print("ALL TESTS COMPLETED")
    print("="*60)
