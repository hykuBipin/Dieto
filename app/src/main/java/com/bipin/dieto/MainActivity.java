package com.bipin.dieto;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    public enum DietGoal {
        WEIGHT_LOSS("Weight Loss", 450, 45, 35, 10, 3),
        BALANCED("Balanced", 650, 70, 35, 20, 5),
        MUSCLE_GAIN("Muscle Gain", 850, 95, 55, 25, 7);

        public final String label;
        public final int calories;
        public final float carbs;
        public final float protein;
        public final float unsaturatedFat;
        public final float saturatedFat;

        DietGoal(String label, int calories, float carbs, float protein, float unsaturatedFat, float saturatedFat) {
            this.label = label;
            this.carbs = carbs;
            this.protein = protein;
            this.unsaturatedFat = unsaturatedFat;
            this.saturatedFat = saturatedFat;
            this.calories = calories;
        }
    }

    public static class ManualBox {
        public final String label;
        public final float left;
        public final float top;
        public final float right;
        public final float bottom;
        public final float portion;

        public ManualBox(String label, float left, float top, float right, float bottom, float portion) {
            this.label = label;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.portion = portion;
        }
    }

    private DietGoal currentGoal = DietGoal.BALANCED;
    private boolean isFrozen = false;
    private Map<String, Float> lastConsensusFoods = new HashMap<>();
    private List<BoundingBoxView.Box> lastYoloBoxes = new ArrayList<>();
    private final List<ManualBox> manualBoxes = new ArrayList<>();
    private TextRecognizer textRecognizer;

    private PreviewView previewView;
    private BoundingBoxView boxView;

    // UI elements for nutrition tracking
    private TextView tvDetectedItems;
    private TextView tvCalorieVal;
    
    private ProgressBar pbCarbs;
    private TextView tvCarbsVal;
    
    private ProgressBar pbProtein;
    private TextView tvProteinVal;
    
    private ProgressBar pbUnsaturatedFat;
    private TextView tvUnsaturatedFatVal;
    
    private ProgressBar pbSaturatedFat;
    private TextView tvSaturatedFatVal;
    
    private TextView tvVitaminsVal;
    private TextView tvDieticianTip;

    // Goal Selector TextViews (acting as buttons)
    private TextView btnGoalWeightLoss;
    private TextView btnGoalBalanced;
    private TextView btnGoalMuscleGain;

    // Action buttons
    private Button btnFreeze;
    private Button btnViewDetails;
    private long lastInferenceTime = 0;

    private YoloOnnxDetector detector;
    private final List<String> labels = new ArrayList<>();
    private java.util.concurrent.ExecutorService cameraExecutor;

    private static final int CAMERA_REQUEST = 100;

    // Rolling frame history to stabilize detection
    private final List<List<String>> frameHistory = new ArrayList<>();
    private static final int HISTORY_WINDOW = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        boxView = findViewById(R.id.boxView);

        // Bind nutrition UI components
        tvDetectedItems = findViewById(R.id.tvDetectedItems);
        tvCalorieVal = findViewById(R.id.tvCalorieVal);
        
        pbCarbs = findViewById(R.id.pbCarbs);
        tvCarbsVal = findViewById(R.id.tvCarbsVal);
        
        pbProtein = findViewById(R.id.pbProtein);
        tvProteinVal = findViewById(R.id.tvProteinVal);
        
        pbUnsaturatedFat = findViewById(R.id.pbUnsaturatedFat);
        tvUnsaturatedFatVal = findViewById(R.id.tvUnsaturatedFatVal);
        
        pbSaturatedFat = findViewById(R.id.pbSaturatedFat);
        tvSaturatedFatVal = findViewById(R.id.tvSaturatedFatVal);
        
        tvVitaminsVal = findViewById(R.id.tvVitaminsVal);
        tvDieticianTip = findViewById(R.id.tvDieticianTip);

        // Bind custom selectors
        btnGoalWeightLoss = findViewById(R.id.btnGoalWeightLoss);
        btnGoalBalanced = findViewById(R.id.btnGoalBalanced);
        btnGoalMuscleGain = findViewById(R.id.btnGoalMuscleGain);

        btnFreeze = findViewById(R.id.btnFreeze);
        btnViewDetails = findViewById(R.id.btnViewDetails);

        cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

        // Setup goal click listeners
        btnGoalWeightLoss.setOnClickListener(v -> selectGoal(DietGoal.WEIGHT_LOSS));
        btnGoalBalanced.setOnClickListener(v -> selectGoal(DietGoal.BALANCED));
        btnGoalMuscleGain.setOnClickListener(v -> selectGoal(DietGoal.MUSCLE_GAIN));

        // Setup freeze/live button listener
        btnFreeze.setOnClickListener(v -> {
            isFrozen = !isFrozen;
            if (isFrozen) {
                btnFreeze.setText("Scan Live");
                btnFreeze.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4DFF6F61")));
            } else {
                btnFreeze.setText("Freeze");
                btnFreeze.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2A2A30")));
            }
        });

        // Setup details dialog listener
        btnViewDetails.setOnClickListener(v -> showDetailsDialog());

        // Initialize dynamic registry from storage
        FoodNutritionRegistry.loadRegistry(this);

        // Bind header log and history buttons
        TextView btnRecordMeal = findViewById(R.id.btnRecordMeal);
        TextView btnViewHistory = findViewById(R.id.btnViewHistory);
        btnRecordMeal.setOnClickListener(v -> recordCurrentMeal());
        btnViewHistory.setOnClickListener(v -> showHistoryDialog());

        // Initialize state selector
        selectGoal(DietGoal.BALANCED);

        // Load labels
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(getAssets().open("labels.txt"))
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    labels.add(line.trim());
                }
            }
            reader.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // Load ONNX model
        try {
            detector = new YoloOnnxDetector(getAssets(), "yolov8n.onnx");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize ML Kit Text Recognizer
        try {
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Touch to manually annotate food items
        boxView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                float x = event.getX() / boxView.getWidth();
                float y = event.getY() / boxView.getHeight();
                showManualAddDialog(x, y);
                return true;
            }
            return false;
        });

        // Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST
            );
        }
    }

    private void selectGoal(DietGoal goal) {
        currentGoal = goal;

        // Visual feedback for selectors
        btnGoalWeightLoss.setSelected(goal == DietGoal.WEIGHT_LOSS);
        btnGoalBalanced.setSelected(goal == DietGoal.BALANCED);
        btnGoalMuscleGain.setSelected(goal == DietGoal.MUSCLE_GAIN);

        // Update displays immediately
        recalculateAndRefreshUI();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // PREVIEW
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // IMAGE ANALYSIS (YOLO INPUT STREAM)
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                try {
                    if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                        } else {
                            android.util.Log.e("MainActivity", "No cameras available on this device.");
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    // =========================
    // YOLO INFERENCE PIPELINE
    // =========================
    private void analyzeImage(ImageProxy imageProxy) {
        if (detector == null || isFrozen) {
            imageProxy.close();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInferenceTime < 1000) { // Limit to 1 frame per second
            imageProxy.close();
            return;
        }
        lastInferenceTime = currentTime;

        try {
            Bitmap bitmap = ImageUtils.toBitmap(imageProxy);
            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            long startTime = System.currentTimeMillis();
            List<YoloOnnxDetector.Detection> detections = detector.detect(bitmap);
            long duration = System.currentTimeMillis() - startTime;

            android.util.Log.d("MainActivity", "Inference took " + duration + " ms. Detected: " + detections.size() + " objects");

            // Compute yoloConsensus portions from detections in this active frame
            Map<String, Float> yoloConsensus = new HashMap<>();
            List<BoundingBoxView.Box> yoloBoxes = new ArrayList<>();
            float viewWidth = boxView.getWidth();
            float viewHeight = boxView.getHeight();

            for (YoloOnnxDetector.Detection d : detections) {
                String name = (d.classId >= 0 && d.classId < labels.size()) ? labels.get(d.classId) : "";
                if (FoodNutritionRegistry.isFood(name)) {
                    float ref = getReferenceArea(name);
                    float area = d.w * d.h;
                    float portion = Math.round((area / ref) * 10f) / 10f;
                    portion = Math.max(0.1f, Math.min(3.0f, portion));

                    yoloConsensus.put(name, yoloConsensus.getOrDefault(name, 0f) + portion);

                    FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(name);
                    String displayLabel = (info != null ? info.name : name) + " (" + String.format("%.1f", portion) + " serv)";

                    BoundingBoxView.Box box = new BoundingBoxView.Box();
                    box.left = d.x * viewWidth;
                    box.top = d.y * viewHeight;
                    box.right = (d.x + d.w) * viewWidth;
                    box.bottom = (d.y + d.h) * viewHeight;
                    box.label = displayLabel;
                    box.confidence = d.confidence;
                    yoloBoxes.add(box);
                }
            }

            // Compute OCR text recognition
            if (textRecognizer != null) {
                try {
                    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                    com.google.android.gms.tasks.Task<Text> ocrTask = textRecognizer.process(inputImage);
                    Text ocrText = com.google.android.gms.tasks.Tasks.await(ocrTask);
                    if (ocrText != null) {
                        String fullOcrText = ocrText.getText();
                        FoodNutritionRegistry.NutritionInfo scannedInfo = parseNutritionFromText(fullOcrText);

                        if (scannedInfo != null) {
                            Text.TextBlock targetBlock = null;
                            for (Text.TextBlock block : ocrText.getTextBlocks()) {
                                String blockText = block.getText().toLowerCase();
                                if (blockText.contains("nutrition") || blockText.contains("calories") || blockText.contains("kcal")) {
                                    targetBlock = block;
                                    break;
                                }
                            }
                            if (targetBlock == null && !ocrText.getTextBlocks().isEmpty()) {
                                targetBlock = ocrText.getTextBlocks().get(0);
                            }

                            if (targetBlock != null) {
                                Rect rect = targetBlock.getBoundingBox();
                                if (rect != null) {
                                    float left = (float) rect.left / bitmap.getWidth();
                                    float top = (float) rect.top / bitmap.getHeight();
                                    float right = (float) rect.right / bitmap.getWidth();
                                    float bottom = (float) rect.bottom / bitmap.getHeight();

                                    String key = "scanned_cover";
                                    FoodNutritionRegistry.saveNutrition(MainActivity.this, key, scannedInfo);

                                    float area = (right - left) * (bottom - top);
                                    float portion = Math.round((area / 0.08f) * 10f) / 10f;
                                    portion = Math.max(0.1f, Math.min(3.0f, portion));

                                    yoloConsensus.put(key, yoloConsensus.getOrDefault(key, 0f) + portion);

                                    BoundingBoxView.Box box = new BoundingBoxView.Box();
                                    box.left = left * viewWidth;
                                    box.top = top * viewHeight;
                                    box.right = right * viewWidth;
                                    box.bottom = bottom * viewHeight;
                                    box.label = scannedInfo.name + " (" + Math.round(scannedInfo.calories * portion) + " kcal) [OCR Cover]";
                                    box.confidence = 1.0f;
                                    yoloBoxes.add(box);
                                }
                            }
                        } else {
                            for (Text.TextBlock block : ocrText.getTextBlocks()) {
                                String blockText = block.getText().toLowerCase().trim();
                                for (String key : FoodNutritionRegistry.getAllFoods().keySet()) {
                                    if (key.equals("scanned_cover")) continue;
                                    if (containsWord(blockText, key)) {
                                        Rect rect = block.getBoundingBox();
                                        if (rect != null) {
                                            float left = (float) rect.left / bitmap.getWidth();
                                            float top = (float) rect.top / bitmap.getHeight();
                                            float right = (float) rect.right / bitmap.getWidth();
                                            float bottom = (float) rect.bottom / bitmap.getHeight();

                                            float area = (right - left) * (bottom - top);
                                            float ref = getReferenceArea(key);
                                            float portion = Math.round((area / ref) * 10f) / 10f;
                                            portion = Math.max(0.1f, Math.min(3.0f, portion));

                                            yoloConsensus.put(key, yoloConsensus.getOrDefault(key, 0f) + portion);

                                            FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(key);
                                            BoundingBoxView.Box box = new BoundingBoxView.Box();
                                            box.left = left * viewWidth;
                                            box.top = top * viewHeight;
                                            box.right = right * viewWidth;
                                            box.bottom = bottom * viewHeight;
                                            box.label = (info != null ? info.name : key) + " (" + String.format("%.1f", portion) + " serv) [OCR]";
                                            box.confidence = 0.95f;
                                            yoloBoxes.add(box);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(() -> {
                lastConsensusFoods = yoloConsensus;
                lastYoloBoxes = yoloBoxes;
                recalculateAndRefreshUI();
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
    }

    private float getReferenceArea(String label) {
        switch (label.toLowerCase().trim()) {
            case "banana": return 0.04f;
            case "apple": return 0.03f;
            case "orange": return 0.03f;
            case "broccoli": return 0.02f;
            case "carrot": return 0.02f;
            case "hot dog": return 0.05f;
            case "pizza": return 0.08f;
            case "donut": return 0.03f;
            case "cake": return 0.05f;
            case "sandwich": return 0.06f;
            case "milk": return 0.06f;
            case "egg": return 0.03f;
            default: return 0.04f;
        }
    }

    private boolean containsWord(String text, String word) {
        if (text == null || word == null) return false;
        return text.matches("(?i).*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*");
    }

    private FoodNutritionRegistry.NutritionInfo parseNutritionFromText(String fullText) {
        String lower = fullText.toLowerCase();
        if (!lower.contains("calories") && !lower.contains("kcal") && !lower.contains("nutrition")) {
            return null;
        }

        try {
            int calories = extractValue(lower, new String[]{"calories", "energy", "kcal"}, 120);
            float carbs = extractFloatValue(lower, new String[]{"carbohydrate", "carbs", "carb"}, 10f);
            float protein = extractFloatValue(lower, new String[]{"protein", "proteins"}, 8f);
            float satFat = extractFloatValue(lower, new String[]{"saturated", "sat fat", "sat. fat"}, 2f);
            float unsatFat = extractFloatValue(lower, new String[]{"unsaturated", "unsat fat", "unsat. fat"}, 3f);

            String foodName = "Packed Food Item";
            for (String key : FoodNutritionRegistry.getAllFoods().keySet()) {
                if (containsWord(lower, key)) {
                    FoodNutritionRegistry.NutritionInfo regInfo = FoodNutritionRegistry.getNutrition(key);
                    if (regInfo != null) {
                        foodName = regInfo.name;
                    }
                    break;
                }
            }

            Map<String, Integer> vitamins = new HashMap<>();
            vitamins.put("Scanned Label", 100);

            return new FoodNutritionRegistry.NutritionInfo(
                    foodName + " (Cover Scanned)",
                    "1 package",
                    calories,
                    carbs,
                    protein,
                    unsatFat,
                    satFat,
                    vitamins
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int extractValue(String text, String[] keywords, int fallback) {
        for (String kw : keywords) {
            int idx = text.indexOf(kw);
            if (idx != -1) {
                String sub = text.substring(idx + kw.length());
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(sub);
                if (m.find()) {
                    return Integer.parseInt(m.group());
                }
            }
        }
        return fallback;
    }

    private float extractFloatValue(String text, String[] keywords, float fallback) {
        for (String kw : keywords) {
            int idx = text.indexOf(kw);
            if (idx != -1) {
                String sub = text.substring(idx + kw.length());
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)").matcher(sub);
                if (m.find()) {
                    return Float.parseFloat(m.group(1));
                }
            }
        }
        return fallback;
    }

    private void recalculateAndRefreshUI() {
        int totalCalories = 0;
        float totalCarbs = 0f;
        float totalProtein = 0f;
        float totalUnsaturatedFat = 0f;
        float totalSaturatedFat = 0f;
        Map<String, Integer> totalVitamins = new HashMap<>();

        List<BoundingBoxView.Box> displayBoxes = new ArrayList<>();
        float viewWidth = boxView.getWidth();
        float viewHeight = boxView.getHeight();

        // 1. Process YOLO detections
        for (Map.Entry<String, Float> entry : lastConsensusFoods.entrySet()) {
            String label = entry.getKey();
            float portion = entry.getValue();
            FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(label);
            if (info != null) {
                totalCalories += Math.round(info.calories * portion);
                totalCarbs += info.carbs * portion;
                totalProtein += info.protein * portion;
                totalUnsaturatedFat += info.unsaturatedFat * portion;
                totalSaturatedFat += info.saturatedFat * portion;
                for (Map.Entry<String, Integer> vit : info.vitamins.entrySet()) {
                    totalVitamins.put(vit.getKey(), totalVitamins.getOrDefault(vit.getKey(), 0) + Math.round(vit.getValue() * portion));
                }
            }
        }

        if (lastYoloBoxes != null) {
            displayBoxes.addAll(lastYoloBoxes);
        }

        // 2. Process Manual detections
        Map<String, Float> manualConsensus = new HashMap<>();
        for (ManualBox m : manualBoxes) {
            String label = m.label;
            float portion = m.portion;
            manualConsensus.put(label, manualConsensus.getOrDefault(label, 0f) + portion);

            FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(label);
            if (info != null) {
                totalCalories += Math.round(info.calories * portion);
                totalCarbs += info.carbs * portion;
                totalProtein += info.protein * portion;
                totalUnsaturatedFat += info.unsaturatedFat * portion;
                totalSaturatedFat += info.saturatedFat * portion;
                for (Map.Entry<String, Integer> vit : info.vitamins.entrySet()) {
                    totalVitamins.put(vit.getKey(), totalVitamins.getOrDefault(vit.getKey(), 0) + Math.round(vit.getValue() * portion));
                }

                BoundingBoxView.Box box = new BoundingBoxView.Box();
                box.left = m.left * viewWidth;
                box.top = m.top * viewHeight;
                box.right = m.right * viewWidth;
                box.bottom = m.bottom * viewHeight;
                box.label = info.name + " (" + Math.round(info.calories * portion) + " kcal) [Manual]";
                box.confidence = 1.0f;
                displayBoxes.add(box);
            }
        }

        boxView.setBoxes(displayBoxes);

        // UI fields
        tvCalorieVal.setText(totalCalories + " / " + currentGoal.calories);

        Map<String, Float> combinedConsensus = new HashMap<>();
        for (Map.Entry<String, Float> e : lastConsensusFoods.entrySet()) {
            combinedConsensus.put(e.getKey(), combinedConsensus.getOrDefault(e.getKey(), 0f) + e.getValue());
        }
        for (Map.Entry<String, Float> e : manualConsensus.entrySet()) {
            combinedConsensus.put(e.getKey(), combinedConsensus.getOrDefault(e.getKey(), 0f) + e.getValue());
        }

        if (combinedConsensus.isEmpty()) {
            tvDetectedItems.setText("Tap screen to annotate, or point camera at plate...");
        } else {
            StringBuilder sb = new StringBuilder("Plate: ");
            boolean first = true;
            for (Map.Entry<String, Float> entry : combinedConsensus.entrySet()) {
                FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(entry.getKey());
                if (info != null) {
                    if (!first) sb.append(", ");
                    sb.append(info.name).append(" (x").append(String.format("%.1f", entry.getValue())).append(" serv)");
                    first = false;
                }
            }
            tvDetectedItems.setText(sb.toString());
        }

        pbCarbs.setMax((int) currentGoal.carbs);
        pbCarbs.setProgress(Math.round(totalCarbs));
        tvCarbsVal.setText(String.format("%.1f / %.0fg", totalCarbs, currentGoal.carbs));

        pbProtein.setMax((int) currentGoal.protein);
        pbProtein.setProgress(Math.round(totalProtein));
        tvProteinVal.setText(String.format("%.1f / %.0fg", totalProtein, currentGoal.protein));

        pbUnsaturatedFat.setMax((int) currentGoal.unsaturatedFat);
        pbUnsaturatedFat.setProgress(Math.round(totalUnsaturatedFat));
        tvUnsaturatedFatVal.setText(String.format("%.1f / %.0fg", totalUnsaturatedFat, currentGoal.unsaturatedFat));

        pbSaturatedFat.setMax((int) currentGoal.saturatedFat);
        pbSaturatedFat.setProgress(Math.round(totalSaturatedFat));
        tvSaturatedFatVal.setText(String.format("%.1f / %.0fg", totalSaturatedFat, currentGoal.saturatedFat));

        if (totalVitamins.isEmpty()) {
            tvVitaminsVal.setText("No significant vitamins detected.");
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Integer> entry : totalVitamins.entrySet()) {
                if (!first) sb.append(" | ");
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("% DV");
                first = false;
            }
            tvVitaminsVal.setText(sb.toString());
        }

        String advice = generateDieticianAdvice(totalCalories, totalCarbs, totalProtein, totalUnsaturatedFat, totalSaturatedFat, totalVitamins, combinedConsensus);
        tvDieticianTip.setText(advice);
    }

    private void showManualAddDialog(float x, float y) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Food Annotation");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("Select Food Item:");
        tvLabel.setPadding(0, 0, 0, 4);
        layout.addView(tvLabel);

        final android.widget.Spinner spinner = new android.widget.Spinner(this);
        final List<String> foodKeys = new ArrayList<>(FoodNutritionRegistry.getAllFoods().keySet());
        java.util.Collections.sort(foodKeys);
        
        List<String> displayNames = new ArrayList<>();
        for (String k : foodKeys) {
            FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(k);
            displayNames.add(info != null ? info.name : k);
        }
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, displayNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        layout.addView(spinner);

        TextView tvPortion = new TextView(this);
        tvPortion.setText("\nPortion / Servings (e.g. 1.0, 1.5, 0.5):");
        tvPortion.setPadding(0, 8, 0, 4);
        layout.addView(tvPortion);

        final EditText etPortion = new EditText(this);
        etPortion.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPortion.setText("1.0");
        layout.addView(etPortion);

        builder.setView(layout);

        builder.setPositiveButton("Add to Plate", (dialog, which) -> {
            try {
                int selectedIndex = spinner.getSelectedItemPosition();
                String label = foodKeys.get(selectedIndex);
                float portion = Float.parseFloat(etPortion.getText().toString().trim());
                if (portion <= 0) {
                    Toast.makeText(MainActivity.this, "Portion size must be greater than 0.", Toast.LENGTH_SHORT).show();
                    return;
                }

                float boxSize = 0.16f;
                float left = Math.max(0f, x - boxSize / 2f);
                float top = Math.max(0f, y - boxSize / 2f);
                float right = Math.min(1f, x + boxSize / 2f);
                float bottom = Math.min(1f, y + boxSize / 2f);

                ManualBox newBox = new ManualBox(label, left, top, right, bottom, portion);
                manualBoxes.add(newBox);
                
                Toast.makeText(MainActivity.this, "Added annotation!", Toast.LENGTH_SHORT).show();
                recalculateAndRefreshUI();

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Invalid portion entered.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String generateDieticianAdvice(int calories, float carbs, float protein, float unsaturatedFat, float saturatedFat, Map<String, Integer> vitamins, Map<String, Float> consensusFoods) {
        if (consensusFoods.isEmpty()) {
            return "Point camera at your plate. Select your diet profile above to get personalized suggestions.";
        }

        List<String> adviceList = new ArrayList<>();

        if (saturatedFat > currentGoal.saturatedFat) {
            float excess = saturatedFat - currentGoal.saturatedFat;
            String highSaturatedFatFood = "";
            float maxSaturatedFat = 0f;
            for (String label : consensusFoods.keySet()) {
                FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(label);
                if (info != null && info.saturatedFat > maxSaturatedFat) {
                    maxSaturatedFat = info.saturatedFat;
                    highSaturatedFatFood = info.name;
                }
            }
            if (!highSaturatedFatFood.isEmpty()) {
                adviceList.add("⚠️ Saturated fat exceeds target by " + String.format("%.1fg", excess) + ". Consider reducing or replacing " + highSaturatedFatFood + " with fruits/veg.");
            } else {
                adviceList.add("⚠️ Saturated fat exceeds target by " + String.format("%.1fg", excess) + ". Reduce greasy/fried items.");
            }
        }

        if (calories > currentGoal.calories + 100) {
            int excess = calories - currentGoal.calories;
            adviceList.add("🔴 Calories are " + excess + " kcal over your meal limit. Save some for later!");
        } else if (calories < currentGoal.calories - 150) {
            int deficit = currentGoal.calories - calories;
            if (protein < currentGoal.protein) {
                adviceList.add("🟢 Deficit of " + deficit + " kcal. To boost calories and protein, add 1 serving of Sandwich (250 kcal, 12g protein).");
            } else {
                adviceList.add("🟢 Deficit of " + deficit + " kcal. Add a Banana (89 kcal) or Orange (47 kcal) for clean energy.");
            }
        }

        if (protein < currentGoal.protein - 5) {
            float deficit = currentGoal.protein - protein;
            float sandwichPortions = deficit / 12f;
            adviceList.add("💪 Protein short by " + String.format("%.1fg", deficit) + ". Suggest adding " + String.format("%.1f", sandwichPortions) + " portions of Sandwich.");
        }

        if (unsaturatedFat < currentGoal.unsaturatedFat - 5) {
            float deficit = currentGoal.unsaturatedFat - unsaturatedFat;
            adviceList.add("🥑 Unsaturated fat is below target by " + String.format("%.1fg", deficit) + ". Add avocados, nuts, or a healthy Sandwich.");
        }

        boolean hasGoodVitamins = false;
        String bestVitamin = "";
        int bestValue = 0;
        for (Map.Entry<String, Integer> e : vitamins.entrySet()) {
            if (e.getValue() >= 50) {
                hasGoodVitamins = true;
                if (e.getValue() > bestValue) {
                    bestValue = e.getValue();
                    bestVitamin = e.getKey();
                }
            }
        }
        if (hasGoodVitamins) {
            adviceList.add("✨ Excellent source of " + bestVitamin + " (" + bestValue + "% DV) on your plate!");
        } else {
            adviceList.add("🥕 Add carrots (for Vitamin A) or broccoli/oranges (for Vitamin C) to boost your vitamins.");
        }

        if (adviceList.isEmpty()) {
            return "Perfect! This plate matches your " + currentGoal.label + " target exceptionally well.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < adviceList.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(adviceList.get(i));
        }
        return sb.toString();
    }

    private void showDetailsDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.setBackgroundColor(android.graphics.Color.parseColor("#18181A"));

        // Title
        TextView header = new TextView(this);
        header.setText("Plate Detections Breakdown");
        header.setTextColor(android.graphics.Color.WHITE);
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, (int) (4 * getResources().getDisplayMetrics().density));
        layout.addView(header);

        // Subtitle Tip
        TextView subtitle = new TextView(this);
        subtitle.setText("💡 Tap YOLO items to customize nutrition values.\n💡 Tap manually added items to edit/delete them.");
        subtitle.setTextColor(android.graphics.Color.parseColor("#FFD54F"));
        subtitle.setTextSize(11);
        subtitle.setPadding(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        layout.addView(subtitle);

        boolean hasItems = !lastConsensusFoods.isEmpty() || !manualBoxes.isEmpty();

        if (!hasItems) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No food items currently scanned or annotated.\nTap on the screen viewport to manually add a food item.");
            emptyText.setTextColor(android.graphics.Color.GRAY);
            emptyText.setTextSize(14);
            layout.addView(emptyText);
        } else {
            // 1. Display YOLO items
            for (Map.Entry<String, Float> entry : lastConsensusFoods.entrySet()) {
                String label = entry.getKey();
                float portion = entry.getValue();
                FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(label);
                if (info != null) {
                    TextView foodText = new TextView(this);
                    foodText.setText(info.name + " [Auto-Scanned]\n" +
                            " • Portion: " + String.format("%.1f", portion) + " serving(s)\n" +
                            " • Calories: " + Math.round(info.calories * portion) + " kcal\n" +
                            " • Protein: " + String.format("%.1fg", info.protein * portion) + "\n" +
                            " • Carbohydrates: " + String.format("%.1fg", info.carbs * portion) + "\n" +
                            " • Unsaturated Fat: " + String.format("%.1fg", info.unsaturatedFat * portion) + "\n" +
                            " • Saturated Fat: " + String.format("%.1fg", info.saturatedFat * portion));
                    foodText.setTextColor(android.graphics.Color.parseColor("#E2E2E6"));
                    foodText.setTextSize(14);
                    foodText.setLineSpacing(2, 1);
                    foodText.setPadding(10, 10, 10, (int) (10 * getResources().getDisplayMetrics().density));
                    
                    android.util.TypedValue outValue = new android.util.TypedValue();
                    getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                    foodText.setBackgroundResource(outValue.resourceId);
                    foodText.setClickable(true);
                    foodText.setFocusable(true);
                    foodText.setOnClickListener(v -> {
                        dialog.dismiss();
                        showEditNutritionDialog(label, info);
                    });

                    View divider = new View(this);
                    divider.setBackgroundColor(android.graphics.Color.parseColor("#26FFFFFF"));
                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);
                    lp.setMargins(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));

                    layout.addView(foodText);
                    layout.addView(divider, lp);
                }
            }

            // 2. Display Manual items
            for (int index = 0; index < manualBoxes.size(); index++) {
                final int mIndex = index;
                ManualBox m = manualBoxes.get(index);
                FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(m.label);
                if (info != null) {
                    TextView foodText = new TextView(this);
                    foodText.setText(info.name + " [Manual Annotation]\n" +
                            " • Portion: " + String.format("%.1f", m.portion) + " serving(s)\n" +
                            " • Calories: " + Math.round(info.calories * m.portion) + " kcal\n" +
                            " • Protein: " + String.format("%.1fg", info.protein * m.portion) + "\n" +
                            " • Carbohydrates: " + String.format("%.1fg", info.carbs * m.portion) + "\n" +
                            " • Unsaturated Fat: " + String.format("%.1fg", info.unsaturatedFat * m.portion) + "\n" +
                            " • Saturated Fat: " + String.format("%.1fg", info.saturatedFat * m.portion));
                    foodText.setTextColor(android.graphics.Color.parseColor("#80DEEA"));
                    foodText.setTextSize(14);
                    foodText.setLineSpacing(2, 1);
                    foodText.setPadding(10, 10, 10, (int) (10 * getResources().getDisplayMetrics().density));

                    android.util.TypedValue outValue = new android.util.TypedValue();
                    getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                    foodText.setBackgroundResource(outValue.resourceId);
                    foodText.setClickable(true);
                    foodText.setFocusable(true);
                    
                    foodText.setOnClickListener(v -> {
                        dialog.dismiss();
                        showEditOrDeleteManualDialog(mIndex, m);
                    });

                    View divider = new View(this);
                    divider.setBackgroundColor(android.graphics.Color.parseColor("#26FFFFFF"));
                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2);
                    lp.setMargins(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));

                    layout.addView(foodText);
                    layout.addView(divider, lp);
                }
            }
        }

        scrollView.addView(layout);
        dialog.setContentView(scrollView);
        dialog.show();
    }

    private void showEditOrDeleteManualDialog(int index, ManualBox m) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modify Annotation: " + FoodNutritionRegistry.getNutrition(m.label).name);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView tvPortion = new TextView(this);
        tvPortion.setText("Edit Portions / Servings:");
        tvPortion.setPadding(0, 0, 0, 4);
        layout.addView(tvPortion);

        final EditText etPortion = new EditText(this);
        etPortion.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPortion.setText(String.valueOf(m.portion));
        layout.addView(etPortion);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                float portion = Float.parseFloat(etPortion.getText().toString().trim());
                if (portion <= 0) {
                    Toast.makeText(MainActivity.this, "Portion size must be greater than 0.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                manualBoxes.set(index, new ManualBox(m.label, m.left, m.top, m.right, m.bottom, portion));
                Toast.makeText(MainActivity.this, "Annotation updated!", Toast.LENGTH_SHORT).show();
                recalculateAndRefreshUI();

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Invalid portion size.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("Delete", (dialog, which) -> {
            manualBoxes.remove(index);
            Toast.makeText(MainActivity.this, "Annotation removed.", Toast.LENGTH_SHORT).show();
            recalculateAndRefreshUI();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditNutritionDialog(String label, FoodNutritionRegistry.NutritionInfo info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Nutrition: " + info.name);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText etServing = new EditText(this);
        etServing.setHint("Serving size (e.g. 1 slice)");
        etServing.setText(info.serving);
        layout.addView(etServing);

        final EditText etCalories = new EditText(this);
        etCalories.setHint("Calories (kcal)");
        etCalories.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCalories.setText(String.valueOf(info.calories));
        layout.addView(etCalories);

        final EditText etCarbs = new EditText(this);
        etCarbs.setHint("Carbs (g)");
        etCarbs.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etCarbs.setText(String.valueOf(info.carbs));
        layout.addView(etCarbs);

        final EditText etProtein = new EditText(this);
        etProtein.setHint("Protein (g)");
        etProtein.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etProtein.setText(String.valueOf(info.protein));
        layout.addView(etProtein);

        final EditText etUnsatFat = new EditText(this);
        etUnsatFat.setHint("Unsaturated Fat (g)");
        etUnsatFat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etUnsatFat.setText(String.valueOf(info.unsaturatedFat));
        layout.addView(etUnsatFat);

        final EditText etSatFat = new EditText(this);
        etSatFat.setHint("Saturated Fat (g)");
        etSatFat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etSatFat.setText(String.valueOf(info.saturatedFat));
        layout.addView(etSatFat);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialogInterface, i) -> {
            try {
                String serving = etServing.getText().toString().trim();
                int calories = Integer.parseInt(etCalories.getText().toString().trim());
                float carbs = Float.parseFloat(etCarbs.getText().toString().trim());
                float protein = Float.parseFloat(etProtein.getText().toString().trim());
                float unsatFat = Float.parseFloat(etUnsatFat.getText().toString().trim());
                float satFat = Float.parseFloat(etSatFat.getText().toString().trim());

                FoodNutritionRegistry.NutritionInfo newInfo = new FoodNutritionRegistry.NutritionInfo(
                        info.name, serving, calories, carbs, protein, unsatFat, satFat, info.vitamins
                );

                FoodNutritionRegistry.saveNutrition(MainActivity.this, label, newInfo);
                Toast.makeText(MainActivity.this, info.name + " nutrition updated!", Toast.LENGTH_SHORT).show();

                recalculateAndRefreshUI();

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error: Invalid numbers entered.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void recordCurrentMeal() {
        Map<String, Float> manualConsensus = new HashMap<>();
        for (ManualBox m : manualBoxes) {
            manualConsensus.put(m.label, manualConsensus.getOrDefault(m.label, 0f) + m.portion);
        }

        Map<String, Float> combinedConsensus = new HashMap<>();
        for (Map.Entry<String, Float> e : lastConsensusFoods.entrySet()) {
            combinedConsensus.put(e.getKey(), combinedConsensus.getOrDefault(e.getKey(), 0f) + e.getValue());
        }
        for (Map.Entry<String, Float> e : manualConsensus.entrySet()) {
            combinedConsensus.put(e.getKey(), combinedConsensus.getOrDefault(e.getKey(), 0f) + e.getValue());
        }

        if (combinedConsensus.isEmpty()) {
            Toast.makeText(this, "No food detected on the plate to log.", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalCalories = 0;
        float totalCarbs = 0f;
        float totalProtein = 0f;
        float totalUnsaturatedFat = 0f;
        float totalSaturatedFat = 0f;

        StringBuilder summaryBuilder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Float> entry : combinedConsensus.entrySet()) {
            String label = entry.getKey();
            float portion = entry.getValue();
            FoodNutritionRegistry.NutritionInfo info = FoodNutritionRegistry.getNutrition(label);
            if (info != null) {
                totalCalories += Math.round(info.calories * portion);
                totalCarbs += info.carbs * portion;
                totalProtein += info.protein * portion;
                totalUnsaturatedFat += info.unsaturatedFat * portion;
                totalSaturatedFat += info.saturatedFat * portion;

                if (!first) summaryBuilder.append(", ");
                summaryBuilder.append(info.name).append(" x").append(String.format("%.1f", portion));
                first = false;
            }
        }

        MealLogManager.logMeal(this, summaryBuilder.toString(), totalCalories, totalCarbs, totalProtein, totalUnsaturatedFat, totalSaturatedFat);
        Toast.makeText(this, "Meal recorded successfully!", Toast.LENGTH_SHORT).show();
    }

    private void showHistoryDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.setBackgroundColor(android.graphics.Color.parseColor("#18181A"));

        // Header Title
        TextView header = new TextView(this);
        header.setText("Daily Meal History Log");
        header.setTextColor(android.graphics.Color.WHITE);
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        layout.addView(header);

        List<MealLogManager.MealEntry> loggedMeals = MealLogManager.getLoggedMeals(this);

        if (loggedMeals.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No meals recorded today. Click the '+ RECORD' button to save scanned plates.");
            emptyText.setTextColor(android.graphics.Color.GRAY);
            emptyText.setTextSize(14);
            emptyText.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density));
            layout.addView(emptyText);
        } else {
            int dailyCal = 0;
            float dailyCarbs = 0;
            float dailyProtein = 0;
            float dailyUnsat = 0;
            float dailySat = 0;

            for (MealLogManager.MealEntry entry : loggedMeals) {
                dailyCal += entry.calories;
                dailyCarbs += entry.carbs;
                dailyProtein += entry.protein;
                dailyUnsat += entry.unsaturatedFat;
                dailySat += entry.saturatedFat;
            }

            TextView summaryCard = new TextView(this);
            summaryCard.setText(String.format("DAILY CUMULATIVE TOTALS:\n" +
                    " • Calories: %d kcal\n" +
                    " • Carbohydrates: %.1fg\n" +
                    " • Protein: %.1fg\n" +
                    " • Unsaturated Fat: %.1fg\n" +
                    " • Saturated Fat: %.1fg", dailyCal, dailyCarbs, dailyProtein, dailyUnsat, dailySat));
            summaryCard.setTextColor(android.graphics.Color.parseColor("#FFD54F"));
            summaryCard.setTextSize(14);
            summaryCard.setTypeface(null, android.graphics.Typeface.BOLD);
            summaryCard.setPadding((int) (12 * getResources().getDisplayMetrics().density),
                    (int) (12 * getResources().getDisplayMetrics().density),
                    (int) (12 * getResources().getDisplayMetrics().density),
                    (int) (12 * getResources().getDisplayMetrics().density));

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(android.graphics.Color.parseColor("#222226"));
            gd.setCornerRadius(16f);
            gd.setStroke(2, android.graphics.Color.parseColor("#33FFFFFF"));
            summaryCard.setBackground(gd);

            LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpCard.setMargins(0, 0, 0, (int) (16 * getResources().getDisplayMetrics().density));
            layout.addView(summaryCard, lpCard);

            View divSummary = new View(this);
            divSummary.setBackgroundColor(android.graphics.Color.parseColor("#26FFFFFF"));
            layout.addView(divSummary, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));

            for (int i = loggedMeals.size() - 1; i >= 0; i--) {
                MealLogManager.MealEntry entry = loggedMeals.get(i);
                TextView mealText = new TextView(this);
                mealText.setText(String.format("[%s] %s\n" +
                                " • Calories: %d kcal | Carbs: %.1fg\n" +
                                " • Protein: %.1fg | Unsat Fat: %.1fg | Sat Fat: %.1fg",
                        entry.timestamp, entry.summary, entry.calories, entry.carbs, entry.protein, entry.unsaturatedFat, entry.saturatedFat));
                mealText.setTextColor(android.graphics.Color.parseColor("#E2E2E6"));
                mealText.setTextSize(13);
                mealText.setLineSpacing(2, 1);
                mealText.setPadding(0, (int) (10 * getResources().getDisplayMetrics().density), 0, (int) (10 * getResources().getDisplayMetrics().density));
                layout.addView(mealText);

                View divider = new View(this);
                divider.setBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"));
                LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
                layout.addView(divider, lpDiv);
            }
        }

        Button btnClear = new Button(this);
        btnClear.setText("Clear History Log");
        btnClear.setTextColor(android.graphics.Color.WHITE);
        btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EC407A")));

        LinearLayout.LayoutParams lpClear = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        lpClear.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);

        btnClear.setOnClickListener(v -> {
            MealLogManager.clearHistory(MainActivity.this);
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "History log cleared.", Toast.LENGTH_SHORT).show();
        });
        layout.addView(btnClear, lpClear);

        scrollView.addView(layout);
        dialog.setContentView(scrollView);
        dialog.show();
    }

    // =========================
    // PERMISSION RESULT
    // =========================
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}