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

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVisionServiceImpl implements AiVisionService {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    private static final String SYSTEM_INSTRUCTION = """
        You are a Senior Clinical Dietitian and Southeast Asian / Thai Food Specialist with 15+ years of experience in visual food portion estimation.
        Analyze the provided food photograph with professional dietetic precision:
        1. Identify the specific dish name in both English and accurate Thai script.
        2. Estimate the realistic portion weight (in grams) relative to a standard 22-25cm dinner plate.
        3. Break down the 7 core nutritional values:
           - calories (kcal, integer)
           - proteinG (grams, 1 decimal)
           - carbsG (grams, 1 decimal)
           - fatG (grams, 1 decimal)
           - sodiumMg (milligrams, accurately calculating hidden sodium from fish sauce, soy sauce, oyster sauce, MSG)
           - sugarG (grams, including hidden sugars in curries, marinades, dressings)
           - fiberG (grams)
        4. Detect the primary cooking method (BOILED, STEAMED, STIR_FRIED, DEEP_FRIED, GRILLED, RAW).
        5. List all visible and inferred ingredients.
        6. Provide actionable, practical dietetic health advice in Thai (e.g., sodium reduction, protein booster, glycemic control).
        7. Provide a confidence score between 0.0 and 1.0.
        Output MUST strictly conform to the provided JSON Schema.
        """;

    @Override
    public FoodAnalysisResponse analyzeFood(byte[] imageBytes, String mimeType, String userNotes) {
        String apiKey = geminiProperties.getApiKey();

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equalsIgnoreCase("dummy")) {
            log.warn("Gemini API key is not configured. Falling back to realistic Mock Food Vision analysis.");
            return getMockAnalysis(userNotes);
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String model = geminiProperties.getModel();
            String url = String.format("%s/models/%s:generateContent?key=%s",
                    geminiProperties.getBaseUrl(), model, apiKey);

            String promptText = "Analyze this food image. Provide detailed nutritional breakdown.";
            if (userNotes != null && !userNotes.trim().isEmpty()) {
                promptText += " User specific notes/context: " + userNotes.trim();
            }

            Map<String, Object> requestPayload = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
                    ),
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", promptText),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", mimeType != null ? mimeType : "image/jpeg",
                                            "data", base64Image
                                    ))
                            ))
                    ),
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json",
                            "temperature", 0.2
                    )
            );

            log.info("Sending image to Gemini Vision API (Model: {})...", model);
            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            return parseGeminiResponse(responseBody, model);

        } catch (Exception e) {
            log.error("Error communicating with Gemini Vision API: {}. Returning Mock Fallback.", e.getMessage(), e);
            return getMockAnalysis(userNotes);
        }
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
                .healthTips("เมนูนี้ให้โปรตีนสูงถึง 32.5 กรัม เหมาะสำหรับการสร้างกล้ามเนื้อ แต่อาจมีโซเดียมและน้ำมันผัดค่อนข้างมาก หากกำลังควบคุมน้ำหนักแนะนำลดการราดพริกน้ำปลาเพิ่ม"
                        + (userNotes != null && !userNotes.isEmpty() ? " (หมายเหตุผู้ใช้: " + userNotes + ")" : ""))
                .aiModelUsed(geminiProperties.getModel() + " [Mock Fallback Mode]")
                .isMock(true)
                .build();
    }
}

