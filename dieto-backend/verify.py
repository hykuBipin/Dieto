from fastapi.testclient import TestClient
from main import app
import json

client = TestClient(app)

def run_tests():
    print("==================================================")
    print("   DIETO SWIGGY BACKEND SERVICE VERIFIER")
    print("==================================================\n")

    # Test 1: POST /recommend
    print("--- Test 1: Recommendation Engine ('Spicy Food') ---")
    recommend_payload = {
        "query": "spicy",
        "remaining_calories": 580,
        "remaining_protein": 30.0
    }
    resp = client.post("/recommend", json=recommend_payload)
    print(f"Status: {resp.status_code}")
    print(json.dumps(resp.json(), indent=2))
    print("\n")

    # Test 2: POST /sync-order
    print("--- Test 2: Sync Swiggy Order ('ord_swiggy_7711') ---")
    sync_payload = {
        "order_id": "ord_swiggy_7711"
    }
    resp = client.post("/sync-order", json=sync_payload)
    print(f"Status: {resp.status_code}")
    print(json.dumps(resp.json(), indent=2))
    print("\n")

    # Test 3: POST /compare-plate (Vision & Cart disparity)
    print("--- Test 3: Multimodal Vision Sync ('ord_swiggy_7711' vs plate scan) ---")
    compare_payload = {
        "order_id": "ord_swiggy_7711",
        "detected_items": ["Chicken Biryani", "Chicken 65", "Raita"]
    }
    resp = client.post("/compare-plate", json=compare_payload)
    print(f"Status: {resp.status_code}")
    print(json.dumps(resp.json(), indent=2))
    print("\n")

    # Test 4: POST /recovery (Coach advice)
    print("--- Test 4: Recovery Advice for calorie excess ---")
    recovery_payload = {
        "excess_calories": 270
    }
    resp = client.post("/recovery", json=recovery_payload)
    print(f"Status: {resp.status_code}")
    print(json.dumps(resp.json(), indent=2))
    print("\n")

if __name__ == "__main__":
    run_tests()
