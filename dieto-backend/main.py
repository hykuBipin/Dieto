from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from typing import List, Optional
import agent
import nutrition
import wellness
import ui

app = FastAPI(
    title="Dieto Swiggy AI Health Coach API",
    description="Backend service matching Swiggy MCP orders to Dieto Nutrition and Wellness pipelines."
)

@app.get("/", response_class=HTMLResponse)
async def serve_ui():
    """
    Serves the beautiful Swiggy-like Web UI simulator dashboard.
    """
    return ui.HTML_CONTENT

# ----------------- Schemas -----------------

class RecommendRequest(BaseModel):
    query: str
    remaining_calories: int
    remaining_protein: float
    token: Optional[str] = None

class SyncOrderRequest(BaseModel):
    order_id: str
    token: Optional[str] = None

class ComparePlateRequest(BaseModel):
    order_id: str
    detected_items: List[str]

class RecoveryRequest(BaseModel):
    excess_calories: int

# ----------------- Mock Swiggy Orders -----------------
# Maps mock order IDs to list of ordered dish items
MOCK_SWIGGY_ORDERS = {
    "ord_swiggy_7711": ["Chicken Biryani", "Chicken 65", "Lime Soda"],
    "ord_swiggy_8822": ["Paneer Tikka Roll", "Lime Soda"],
    "ord_swiggy_9933": ["Tandoori Chicken Salad"]
}

# ----------------- Endpoints -----------------

@app.post("/recommend")
async def get_recommendations(req: RecommendRequest):
    """
    Search Swiggy dishes by query, rank them based on remaining calories/protein target budget,
    and returns them decorated with Dieto Fit Scores.
    """
    try:
        results = await agent.recommend_menu_items(
            query=req.query,
            remaining_calories=req.remaining_calories,
            remaining_protein=req.remaining_protein,
            token=req.token
        )
        return {
            "query": req.query,
            "remaining_calories": req.remaining_calories,
            "remaining_protein": req.remaining_protein,
            "options": results
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/sync-order")
async def sync_order(req: SyncOrderRequest):
    """
    Connects to Swiggy MCP order logs, resolves the ordered items, and estimates
    the overall nutritional stats to sync directly into the user's daily tracker.
    """
    order_id = req.order_id
    items = MOCK_SWIGGY_ORDERS.get(order_id)
    
    if not items:
        # Fallback if unknown order_id is sent
        items = ["Chicken Biryani"]
        
    total_calories = 0
    total_carbs = 0.0
    total_protein = 0.0
    total_fat = 0.0
    mapped_items = []

    for item in items:
        nut = nutrition.estimate_nutrition(item)
        total_calories += nut["calories"]
        total_carbs += nut["carbs"]
        total_protein += nut["protein"]
        total_fat += nut["fat"]
        mapped_items.append(nut)

    return {
        "order_id": order_id,
        "items_found": [item for item in items],
        "mapped_nutrition": mapped_items,
        "total_calories": total_calories,
        "total_carbs": total_carbs,
        "total_protein": total_protein,
        "total_fat": total_fat
    }

@app.post("/compare-plate")
async def compare_plate(req: ComparePlateRequest):
    """
    Multimodal Sync: Compares the ordered Swiggy items calories vs the actual scanned
    plate items (e.g. YOLO/Vision result) to calculate portion/calorie variance.
    """
    # 1. Calculate original order estimate
    order_items = MOCK_SWIGGY_ORDERS.get(req.order_id, ["Chicken Biryani"])
    order_calories = sum(nutrition.estimate_nutrition(i)["calories"] for i in order_items)
    
    # 2. Calculate actual plate scanned estimate
    plate_calories = sum(nutrition.estimate_nutrition(i)["calories"] for i in req.detected_items)
    
    diff = plate_calories - order_calories
    
    message = "Your plate matches your order perfectly! Great portion control. 🎉"
    if diff > 0:
        message = f"Your actual plate appears to contain more food than the original order estimate (+{diff} kcal)."
    elif diff < 0:
        message = f"You ate lighter than estimated (-{abs(diff)} kcal). Excellent!"
        
    # Recovery recommendations
    coach_advice = wellness.get_recovery_advice(diff if diff > 0 else 0)
    
    return {
        "order_id": req.order_id,
        "order_estimated_calories": order_calories,
        "plate_scanned_calories": plate_calories,
        "calorie_difference": diff,
        "comparison_result": message,
        "coach_advice": coach_advice
    }

@app.post("/recovery")
async def get_recovery(req: RecoveryRequest):
    """
    Exposes the AI Recovery Coach routines directly to provide balanced wellness advice.
    """
    advice = wellness.get_recovery_advice(req.excess_calories)
    return advice

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
