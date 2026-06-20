package com.bipin.dieto;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FoodNutritionRegistry {

    public static class NutritionInfo {
        public final String name;
        public final String serving;
        public final int calories; // kcal
        public final float carbs; // grams
        public final float protein; // grams
        public final float unsaturatedFat; // grams
        public final float saturatedFat; // grams
        public final Map<String, Integer> vitamins; // Vitamin name -> % Daily Value (DV)

        public NutritionInfo(String name, String serving, int calories, float carbs, float protein, float unsaturatedFat, float saturatedFat, Map<String, Integer> vitamins) {
            this.name = name;
            this.serving = serving;
            this.calories = calories;
            this.carbs = carbs;
            this.protein = protein;
            this.unsaturatedFat = unsaturatedFat;
            this.saturatedFat = saturatedFat;
            this.vitamins = vitamins;
        }

        public float getTotalFat() {
            return unsaturatedFat + saturatedFat;
        }
    }

    private static final Map<String, NutritionInfo> registry = new HashMap<>();

    public static Map<String, NutritionInfo> getAllFoods() {
        return new HashMap<>(registry);
    }

    public static NutritionInfo getNutrition(String rawLabel) {
        if (rawLabel == null) return null;
        return registry.get(rawLabel.toLowerCase().trim());
    }

    public static boolean isFood(String rawLabel) {
        if (rawLabel == null) return false;
        return registry.containsKey(rawLabel.toLowerCase().trim());
    }

    public static void loadRegistry(Context context) {
        registry.clear();

        // 1. Load default foods from assets/foods.json
        try {
            InputStream is = context.getAssets().open("foods.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String key = obj.getString("key");
                String name = obj.getString("name");
                String serving = obj.getString("serving");
                int calories = obj.getInt("calories");
                float carbs = (float) obj.getDouble("carbs");
                float protein = (float) obj.getDouble("protein");
                float unsaturatedFat = (float) obj.getDouble("unsaturatedFat");
                float saturatedFat = (float) obj.getDouble("saturatedFat");

                JSONObject vitsObj = obj.getJSONObject("vitamins");
                Map<String, Integer> vitamins = new HashMap<>();
                java.util.Iterator<String> keys = vitsObj.keys();
                while (keys.hasNext()) {
                    String vitKey = keys.next();
                    vitamins.put(vitKey, vitsObj.getInt(vitKey));
                }

                registry.put(key, new NutritionInfo(name, serving, calories, carbs, protein, unsaturatedFat, saturatedFat, vitamins));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Load customized user overrides or scanned cover facts from storage
        SharedPreferences prefs = context.getSharedPreferences("dieto_nutrition_registry", Context.MODE_PRIVATE);
        Set<String> keysToCheck = new HashSet<>(registry.keySet());
        keysToCheck.add("scanned_cover");

        for (String label : keysToCheck) {
            String prefix = label + "_";
            if (prefs.contains(prefix + "name")) {
                String name = prefs.getString(prefix + "name", "");
                String serving = prefs.getString(prefix + "serving", "");
                int calories = prefs.getInt(prefix + "calories", 0);
                float carbs = prefs.getFloat(prefix + "carbs", 0f);
                float protein = prefs.getFloat(prefix + "protein", 0f);
                float unsaturatedFat = prefs.getFloat(prefix + "unsaturatedFat", 0f);
                float saturatedFat = prefs.getFloat(prefix + "saturatedFat", 0f);

                Map<String, Integer> vitamins = new HashMap<>();
                String vitsStr = prefs.getString(prefix + "vitamins", "");
                if (!vitsStr.isEmpty()) {
                    String[] parts = vitsStr.split(",");
                    for (String part : parts) {
                        String[] kv = part.split(":");
                        if (kv.length == 2) {
                            try {
                                vitamins.put(kv[0], Integer.parseInt(kv[1]));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                registry.put(label, new NutritionInfo(name, serving, calories, carbs, protein, unsaturatedFat, saturatedFat, vitamins));
            }
        }
    }

    public static void saveNutrition(Context context, String label, NutritionInfo info) {
        String cleanLabel = label.toLowerCase().trim();
        registry.put(cleanLabel, info);
        
        SharedPreferences prefs = context.getSharedPreferences("dieto_nutrition_registry", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String prefix = cleanLabel + "_";
        editor.putString(prefix + "name", info.name);
        editor.putString(prefix + "serving", info.serving);
        editor.putInt(prefix + "calories", info.calories);
        editor.putFloat(prefix + "carbs", info.carbs);
        editor.putFloat(prefix + "protein", info.protein);
        editor.putFloat(prefix + "unsaturatedFat", info.unsaturatedFat);
        editor.putFloat(prefix + "saturatedFat", info.saturatedFat);
        
        StringBuilder vitsStr = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : info.vitamins.entrySet()) {
            if (!first) vitsStr.append(",");
            vitsStr.append(e.getKey()).append(":").append(e.getValue());
            first = false;
        }
        editor.putString(prefix + "vitamins", vitsStr.toString());
        editor.apply();
    }
}
