def get_recovery_advice(excess_calories: int) -> dict:
    """
    Returns non-judgmental wellness and recovery suggestions when daily calorie limits are exceeded.
    Maintains a healthy balance, avoids mandatory workout punishment claims, and targets gentle recovery.
    """
    if excess_calories <= 0:
        return {
            "title": "Intake is on track! 🎉",
            "message": "You are within your target diet boundaries. Keep up the good work!",
            "recommendations": [
                "💧 Continue normal hydration",
                "😴 Maintain healthy sleep habits"
            ]
        }

    # Custom suggestions based on severity of excess calories
    suggestions = [
        "💧 Hydrate with water or a warm unsweetened drink like lemon-mint tea.",
        "😴 Prioritize getting 7-8 hours of normal sleep to support metabolic recovery.",
        "🥗 Adjust your next meal to be slightly lighter (e.g., a green salad or vegetable soup) without skipping it entirely."
    ]

    if excess_calories < 150:
        message = f"You are slightly above your calorie target by {excess_calories} kcal. There is no need to worry or overcompensate!"
        suggestions.insert(0, "🚶 Try a brief 10-minute walk after eating to assist standard digestion.")
    elif excess_calories <= 300:
        message = f"You're currently {excess_calories} kcal over today's target. Let's make gentle choices for the rest of the day."
        suggestions.insert(0, "🚶 Try an easy 15-20 minute stroll in the evening.")
        suggestions.append("🧘 Optional movement: 3 × 10 light jumping jacks or gentle stretching, if comfortable.")
    else:
        message = f"You're {excess_calories} kcal above today's goal. Let's focus on simple routines to re-balance."
        suggestions.insert(0, "🚶 Take a 20-30 minute relaxed walk to stay active.")
        suggestions.append("🧘 Optional movement: 3 × 12 jumping jacks or light yoga exercises, if comfortable.")

    return {
        "title": "Flexible Meal Status 🎉",
        "message": message,
        "recommendations": suggestions
    }
