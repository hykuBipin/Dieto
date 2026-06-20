package com.bipin.dieto;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MealLogManager {

    public static class MealEntry {
        public final String timestamp;
        public final String summary;
        public final int calories;
        public final float carbs;
        public final float protein;
        public final float unsaturatedFat;
        public final float saturatedFat;

        public MealEntry(String timestamp, String summary, int calories, float carbs, float protein, float unsaturatedFat, float saturatedFat) {
            this.timestamp = timestamp;
            this.summary = summary;
            this.calories = calories;
            this.carbs = carbs;
            this.protein = protein;
            this.unsaturatedFat = unsaturatedFat;
            this.saturatedFat = saturatedFat;
        }
    }

    private static final String PREF_NAME = "dieto_meal_logger";
    private static final String KEY_LOGS = "meal_logs";

    public static List<MealEntry> getLoggedMeals(Context context) {
        List<MealEntry> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String logsStr = prefs.getString(KEY_LOGS, "[]");
        try {
            JSONArray arr = new JSONArray(logsStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new MealEntry(
                        obj.getString("timestamp"),
                        obj.getString("summary"),
                        obj.getInt("calories"),
                        (float) obj.getDouble("carbs"),
                        (float) obj.getDouble("protein"),
                        (float) obj.getDouble("unsaturatedFat"),
                        (float) obj.getDouble("saturatedFat")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void logMeal(Context context, String summary, int calories, float carbs, float protein, float unsaturatedFat, float saturatedFat) {
        List<MealEntry> existing = getLoggedMeals(context);
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        String time = sdf.format(new java.util.Date());

        existing.add(new MealEntry(time, summary, calories, carbs, protein, unsaturatedFat, saturatedFat));

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        JSONArray arr = new JSONArray();
        for (MealEntry entry : existing) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("timestamp", entry.timestamp);
                obj.put("summary", entry.summary);
                obj.put("calories", entry.calories);
                obj.put("carbs", entry.carbs);
                obj.put("protein", entry.protein);
                obj.put("unsaturatedFat", entry.unsaturatedFat);
                obj.put("saturatedFat", entry.saturatedFat);
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        editor.putString(KEY_LOGS, arr.toString());
        editor.apply();
    }

    public static void clearHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOGS, "[]").apply();
    }
}
