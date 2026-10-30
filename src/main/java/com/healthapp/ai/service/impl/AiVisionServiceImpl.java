package com.healthapp.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.ai.config.GeminiProperties;
import com.healthapp.ai.dto.response.FoodAnalysisResponse;
import com.healthapp.ai.service.AiVisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVisionServiceImpl implements AiVisionService {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    private static final int MAX_IMAGE_DIMENSION = 1024;

    private static final String SYSTEM_INSTRUCTION = """
        You are a Senior Clinical Dietitian and Southeast Asian / Thai Food Specialist with 15+ years of experience in visual food portion estimation.
        Analyze the provided food photograph with professional dietetic precision.
        
        You MUST return a JSON object with EXACTLY these property names:
        - "foodName": English name of the dish (e.g. "Beef Red Curry", "Pad Krapao Gai")
        - "thaiName": Accurate Thai script name of the dish (e.g. "แกงเผ็ดเนื้อ", "ข้าวกะเพราไก่ไข่ดาว")
        - "portionWeightGrams": Estimated net weight in grams (e.g. 350.0)
        - "confidenceScore": Confidence score between 0.0 and 1.0 (e.g. 0.95)
        - "calories": Total energy in kcal as integer (e.g. 545)
        - "proteinG": Protein in grams (e.g. 24.5)
        - "carbsG": Total carbohydrates in grams (e.g. 13.8)
        - "fatG": Total fat in grams (e.g. 43.2)
        - "sodiumMg": Sodium in milligrams, including hidden sodium from fish sauce, paste, MSG (e.g. 1420.0)
        - "sugarG": Sugar in grams (e.g. 8.5)
        - "fiberG": Dietary fiber in grams (e.g. 4.6)
        - "cookingMethod": Cooking method (e.g. "BOILED", "STIR_FRIED", "DEEP_FRIED", "STEAMED", "GRILLED", "RAW")
        - "ingredients": Array of detected ingredients (e.g. ["เนื้อวัวติดมัน", "กะทิ", "พริกแกงเผ็ด"])
        - "keyIngredients": Array of 2 to 4 major calorie-contributing components (e.g. cooked rice, chicken, fried egg), each an object with: "name" (Thai string), "weightGrams" (number), "calories" (integer), "proteinG" (number), "carbsG" (number), "fatG" (number)
        - "healthTips": Practical dietetic advice in Thai explaining nutritional pros and cons of this dish
        """;

    @Override
    public FoodAnalysisResponse analyzeFood(byte[] imageBytes, String mimeType, String userNotes) {
        String apiKey = geminiProperties.getApiKey();

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equalsIgnoreCase("dummy")) {
            log.warn("Gemini API key is not configured. Falling back to realistic Mock Food Vision analysis.");
            return getMockAnalysis(userNotes);
        }

        try {
            // Compress & downscale large images to ~150-250KB for sub-2-second speed
            byte[] processedBytes = downscaleImageIfNeeded(imageBytes);
            String base64Image = Base64.getEncoder().encodeToString(processedBytes);
            String model = geminiProperties.getModel();
            String url = String.format("%s/models/%s:generateContent?key=%s",
                    geminiProperties.getBaseUrl(), model, apiKey);

            String promptText = "Analyze this food image. Identify the dish name in both English and Thai, estimate portion weight in grams, break down calories and all 7 nutrients, list ingredients, and give health tips in Thai.";
            if (userNotes != null && !userNotes.trim().isEmpty()) {
                promptText += " User specific notes/context: " + userNotes.trim();
            }

            Map<String, Object> responseSchema = buildJsonSchema();

            Map<String, Object> requestPayload = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
                    ),
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", promptText),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", "image/jpeg",
                                            "data", base64Image
                                    ))
                            ))
                    ),
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json",
                            "response_schema", responseSchema,
                            "temperature", 0.2
                    )
            );

            log.info("Sending image to Gemini Vision API (Model: {}, ImageSize: {} KB)...",
                    model, processedBytes.length / 1024);
            long startTime = System.currentTimeMillis();

            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Gemini Vision API responded successfully in {} ms ({} seconds)",
                    duration, String.format("%.2f", duration / 1000.0));

            return parseGeminiResponse(responseBody, model);

        } catch (Exception e) {
            log.error("Error communicating with Gemini Vision API: {}. Returning Mock Fallback.", e.getMessage(), e);
            return getMockAnalysis(userNotes);
        }
    }

    private byte[] downscaleImageIfNeeded(byte[] originalBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (original == null) {
                return originalBytes;
            }

            int width = original.getWidth();
            int height = original.getHeight();

            if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
                return originalBytes;
            }

            int newWidth;
            int newHeight;
            if (width > height) {
                newWidth = MAX_IMAGE_DIMENSION;
                newHeight = (int) ((double) height / width * MAX_IMAGE_DIMENSION);
            } else {
                newHeight = MAX_IMAGE_DIMENSION;
                newWidth = (int) ((double) width / height * MAX_IMAGE_DIMENSION);
            }

            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            byte[] result = baos.toByteArray();
            log.info("Optimized input image from {} KB down to {} KB ({}x{} -> {}x{})",
                    originalBytes.length / 1024, result.length / 1024, width, height, newWidth, newHeight);
            return result;
        } catch (Exception e) {
            log.warn("Image downscaling skipped due to: {}. Using original bytes.", e.getMessage());
            return originalBytes;
        }
    }

    private Map<String, Object> buildJsonSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("foodName", Map.of("type", "STRING", "description", "English name of the dish"));
        properties.put("thaiName", Map.of("type", "STRING", "description", "Thai name of the dish"));
        properties.put("portionWeightGrams", Map.of("type", "NUMBER", "description", "Estimated portion in grams"));
        properties.put("confidenceScore", Map.of("type", "NUMBER", "description", "Confidence score 0.0-1.0"));
        properties.put("calories", Map.of("type", "INTEGER", "description", "Calories in kcal"));
        properties.put("proteinG", Map.of("type", "NUMBER", "description", "Protein in grams"));
        properties.put("carbsG", Map.of("type", "NUMBER", "description", "Carbs in grams"));
        properties.put("fatG", Map.of("type", "NUMBER", "description", "Fat in grams"));
        properties.put("sodiumMg", Map.of("type", "NUMBER", "description", "Sodium in mg"));
        properties.put("sugarG", Map.of("type", "NUMBER", "description", "Sugar in grams"));
        properties.put("fiberG", Map.of("type", "NUMBER", "description", "Fiber in grams"));
        properties.put("cookingMethod", Map.of("type", "STRING", "description", "Cooking method"));
        properties.put("ingredients", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));

        Map<String, Object> keyIngredientProperties = new LinkedHashMap<>();
        keyIngredientProperties.put("name", Map.of("type", "STRING", "description", "Ingredient name in Thai"));
        keyIngredientProperties.put("weightGrams", Map.of("type", "NUMBER", "description", "Estimated weight in grams"));
        keyIngredientProperties.put("calories", Map.of("type", "INTEGER", "description", "Calories in kcal"));
        keyIngredientProperties.put("proteinG", Map.of("type", "NUMBER", "description", "Protein in grams"));
        keyIngredientProperties.put("carbsG", Map.of("type", "NUMBER", "description", "Carbs in grams"));
        keyIngredientProperties.put("fatG", Map.of("type", "NUMBER", "description", "Fat in grams"));

        properties.put("keyIngredients", Map.of(
                "type", "ARRAY",
                "description", "2 to 4 major calorie-contributing ingredients",
                "items", Map.of(
                        "type", "OBJECT",
                        "properties", keyIngredientProperties,
                        "required", List.of("name", "weightGrams", "calories", "proteinG", "carbsG", "fatG")
                )
        ));

        properties.put("healthTips", Map.of("type", "STRING", "description", "Health tips in Thai"));

        return Map.of(
                "type", "OBJECT",
                "properties", properties,
                "required", List.of("foodName", "thaiName", "portionWeightGrams", "calories", "proteinG", "carbsG", "fatG", "sodiumMg", "healthTips")
        );
    }

    private FoodAnalysisResponse parseGeminiResponse(String responseBody, String model) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No response candidate returned by Gemini API");
        }

        String jsonText = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        log.info("Gemini raw parsed JSON: {}", jsonText);

        FoodAnalysisResponse response = objectMapper.readValue(jsonText, FoodAnalysisResponse.class);
        response.setAiModelUsed(model);
        response.setIsMock(false);
        return response;
    }

    @Override
    public String getActiveModel() {
        return geminiProperties.getModel();
    }

    @Override
    public boolean isLiveMode() {
        String key = geminiProperties.getApiKey();
        return key != null && !key.trim().isEmpty() && !key.equalsIgnoreCase("dummy");
    }

    private FoodAnalysisResponse getMockAnalysis(String userNotes) {
        return FoodAnalysisResponse.builder()
                .foodName("Pad Krapao Gai with Fried Egg")
                .thaiName("ข้าวกะเพราไก่ไข่ดาว")
                .portionWeightGrams(350.0)
                .confidenceScore(0.96)
                .calories(620)
                .proteinG(32.5)
                .carbsG(65.0)
                .fatG(24.0)
                .sodiumMg(890.0)
                .sugarG(3.5)
                .fiberG(2.1)
                .cookingMethod("STIR_FRIED")
                .ingredients(List.of(
                        "ข้าวสวย (Cooked Jasmine Rice)",
                        "เนื้ออกไก่ (Chicken Breast)",
                        "ไข่ดาว (Fried Egg)",
                        "ใบกะเพรา (Holy Basil)",
                        "พริกขี้หนูสด (Chili)",
                        "กระเทียม (Garlic)",
                        "น้ำมันพืช (Vegetable Oil)",
                        "ซอสปรุงรสและน้ำปลา (Seasoning Sauce & Fish Sauce)"
                ))
                .keyIngredients(List.of(
                        FoodAnalysisResponse.KeyIngredientDto.builder()
                                .name("ข้าวสวย")
                                .weightGrams(180.0)
                                .calories(234)
                                .proteinG(4.5)
                                .carbsG(50.4)
                                .fatG(0.6)
                                .build(),
                        FoodAnalysisResponse.KeyIngredientDto.builder()
                                .name("เนื้ออกไก่ผัดกะเพรา")
                                .weightGrams(120.0)
                                .calories(220)
                                .proteinG(21.5)
                                .carbsG(4.0)
                                .fatG(13.0)
                                .build(),
                        FoodAnalysisResponse.KeyIngredientDto.builder()
                                .name("ไข่ดาว")
                                .weightGrams(50.0)
                                .calories(120)
                                .proteinG(6.5)
                                .carbsG(0.6)
                                .fatG(10.0)
                                .build()
                ))
                .healthTips("เมนูนี้ให้โปรตีนสูงถึง 32.5 กรัม เหมาะสำหรับการสร้างกล้ามเนื้อ แต่อาจมีโซเดียมและน้ำมันผัดค่อนข้างมาก หากกำลังควบคุมน้ำหนักแนะนำลดการราดพริกน้ำปลาเพิ่ม"
                        + (userNotes != null && !userNotes.isEmpty() ? " (หมายเหตุผู้ใช้: " + userNotes + ")" : ""))
                .aiModelUsed(geminiProperties.getModel() + " [Mock Fallback Mode]")
                .isMock(true)
                .build();
    }
}
