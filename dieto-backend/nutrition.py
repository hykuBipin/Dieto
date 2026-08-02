import re

# Local nutritional lookup for common restaurant food items (per standard serving/portion)
NUTRITION_DATABASE = {
    "paneer tikka roll": {
        "name": "Paneer Tikka Roll",
        "calories": 550,
        "carbs": 48.0,
        "protein": 18.0,
        "fat": 24.0,
        "is_estimated": False
    },
    "double cheese burger": {
        "name": "Double Cheese Burger",
        "calories": 680,
        "carbs": 42.0,
        "protein": 28.0,
        "fat": 36.0,
        "is_estimated": False
    },
    "tandoori chicken salad": {
        "name": "Tandoori Chicken Salad",
        "calories": 350,
        "carbs": 12.0,
        "protein": 35.0,
        "fat": 10.0,
        "is_estimated": False
    },
    "butter chicken": {
        "name": "Butter Chicken & Naan",
        "calories": 850,
        "carbs": 85.0,
        "protein": 38.0,
        "fat": 42.0,
        "is_estimated": False
    },
    "chicken biryani": {
        "name": "Chicken Biryani",
        "calories": 650,
        "carbs": 70.0,
        "protein": 32.0,
        "fat": 20.0,
        "is_estimated": False
    },
    "egg biryani": {
        "name": "Egg Biryani",
        "calories": 590,
        "carbs": 68.0,
        "protein": 24.0,
        "fat": 18.0,
        "is_estimated": False
    },
    "paneer rice bowl": {
        "name": "Paneer Rice Bowl",
        "calories": 560,
        "carbs": 65.0,
        "protein": 20.0,
        "fat": 22.0,
        "is_estimated": False
    },
    "chicken tikka wrap": {
        "name": "Chicken Tikka Wrap",
        "calories": 480,
        "carbs": 38.0,
        "protein": 32.0,
        "fat": 15.0,
        "is_estimated": False
    },
    "chicken 65": {
        "name": "Chicken 65",
        "calories": 280,
        "carbs": 10.0,
        "protein": 25.0,
        "fat": 16.0,
        "is_estimated": False
    },
    "lime soda": {
        "name": "Lime Soda",
        "calories": 120,
        "carbs": 30.0,
        "protein": 0.0,
        "fat": 0.0,
        "is_estimated": False
    },
    "mint juice": {
        "name": "Mint Juice (Detox)",
        "calories": 45,
        "carbs": 10.0,
        "protein": 1.0,
        "fat": 0.0,
        "is_estimated": False
    },
    "raita": {
        "name": "Raita",
        "calories": 80,
        "carbs": 6.0,
        "protein": 3.0,
        "fat": 4.0,
        "is_estimated": False
    }
}

def estimate_nutrition(dish_name: str) -> dict:
    """
    Estimates the nutritional values of a dish name using exact lookup
    with a keyword fallback matching heuristic.
    """
    name_clean = dish_name.lower().strip()
    
    # Try exact lookup first
    if name_clean in NUTRITION_DATABASE:
        return NUTRITION_DATABASE[name_clean]
        
    # Heuristic fallback matching based on common food keywords
    if "biryani" in name_clean:
        if "chicken" in name_clean:
            return {**NUTRITION_DATABASE["chicken biryani"], "name": dish_name, "is_estimated": True}
        if "egg" in name_clean:
            return {**NUTRITION_DATABASE["egg biryani"], "name": dish_name, "is_estimated": True}
        return {
            "name": dish_name,
            "calories": 620,
            "carbs": 70.0,
            "protein": 22.0,
            "fat": 18.0,
            "is_estimated": True
        }
        
    if "paneer" in name_clean:
        if "roll" in name_clean or "wrap" in name_clean:
            return {**NUTRITION_DATABASE["paneer tikka roll"], "name": dish_name, "is_estimated": True}
        return {
            "name": dish_name,
            "calories": 540,
            "carbs": 45.0,
            "protein": 18.0,
            "fat": 22.0,
            "is_estimated": True
        }
        
    if "chicken" in name_clean:
        if "salad" in name_clean:
            return {**NUTRITION_DATABASE["tandoori chicken salad"], "name": dish_name, "is_estimated": True}
        if "wrap" in name_clean or "roll" in name_clean:
            return {**NUTRITION_DATABASE["chicken tikka wrap"], "name": dish_name, "is_estimated": True}
        if "butter" in name_clean or "curry" in name_clean:
            return {**NUTRITION_DATABASE["butter chicken"], "name": dish_name, "is_estimated": True}
        return {
            "name": dish_name,
            "calories": 420,
            "carbs": 15.0,
            "protein": 30.0,
            "fat": 18.0,
            "is_estimated": True
        }
        
    if "burger" in name_clean:
        return {**NUTRITION_DATABASE["double cheese burger"], "name": dish_name, "is_estimated": True}
        
    if "salad" in name_clean:
        return {
            "name": dish_name,
            "calories": 180,
            "carbs": 15.0,
            "protein": 5.0,
            "fat": 6.0,
            "is_estimated": True
        }
        
    if "soda" in name_clean or "coke" in name_clean or "sprite" in name_clean:
        return {**NUTRITION_DATABASE["lime soda"], "name": dish_name, "is_estimated": True}
        
    if "juice" in name_clean or "detox" in name_clean:
        return {**NUTRITION_DATABASE["mint juice"], "name": dish_name, "is_estimated": True}

    # General fallback
    return {
        "name": dish_name,
        "calories": 400,
        "carbs": 40.0,
        "protein": 12.0,
        "fat": 14.0,
        "is_estimated": True
    }
