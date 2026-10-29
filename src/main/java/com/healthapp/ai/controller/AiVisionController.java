package com.healthapp.ai.controller;

import com.healthapp.ai.dto.request.AnalyzeFoodRequest;
import com.healthapp.ai.dto.response.FoodAnalysisResponse;
import com.healthapp.ai.service.AiVisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "2. AI Food Vision Operations", description = "API สำหรับส่งภาพถ่ายอาหารไปให้ Google Gemini Flash วิเคราะห์และคำนวณโภชนาการ")
public class AiVisionController {

    private final AiVisionService aiVisionService;

    @Operation(
        summary = "วิเคราะห์ภาพถ่ายอาหาร (Upload File Multipart/Form-Data)",
        description = "อัปโหลดภาพถ่ายอาหารจากกล้องหรืออัลบั้มรูป เพื่อให้ Gemini Flash สแกนระบุชื่อเมนู คำนวณน้ำหนัก และแจกแจงสารอาหาร 7 ชนิด"
    )
    @ApiResponse(responseCode = "200", description = "วิเคราะห์สำเร็จ ได้ผลลัพธ์เป็น JSON โภชนาการ",
        content = @Content(schema = @Schema(implementation = FoodAnalysisResponse.class)))
    @PostMapping(value = "/analyze-food", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FoodAnalysisResponse> analyzeFoodMultipart(
            @Parameter(description = "ไฟล์รูปภาพอาหาร (JPEG/PNG/WEBP)", required = true)
            @RequestParam("image") MultipartFile image,
            @Parameter(description = "หมายเหตุเพิ่มเติม (ถ้ามี) เช่น 'หวานน้อย', 'ไม่ใส่น้ำมัน'")
            @RequestParam(value = "userNotes", required = false) String userNotes
    ) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] imageBytes = image.getBytes();
            String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
            FoodAnalysisResponse response = aiVisionService.analyzeFood(imageBytes, mimeType, userNotes);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to read image bytes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "วิเคราะห์ภาพอาหารผ่าน Base64 JSON (สำหรับ Mobile Client หรือ Background Sync)",
        description = "ส่งภาพที่แปลงเป็น Base64 String เพื่อให้ AI วิเคราะห์ (ไม่ต้องใช้ Multipart)"
    )
    @PostMapping(value = "/analyze-food/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FoodAnalysisResponse> analyzeFoodJson(
            @RequestBody AnalyzeFoodRequest request
    ) {
        if (request == null || request.getImageBase64() == null || request.getImageBase64().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64().trim());
            String mimeType = request.getMimeType() != null ? request.getMimeType() : "image/jpeg";
            FoodAnalysisResponse response = aiVisionService.analyzeFood(imageBytes, mimeType, request.getUserNotes());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 payload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "ตรวจสอบสถานะการทำงานของ AI Service และโมเดลที่ใช้งาน",
        description = "ตรวจสอบสถานะ Service, โมเดล AI ที่ตั้งค่าไว้ และตรวจสอบว่าใช้งานโหมด Live หรือ Mock Fallback"
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "health-ai-service",
                "activeModel", aiVisionService.getActiveModel(),
                "isLiveMode", aiVisionService.isLiveMode(),
                "timestamp", System.currentTimeMillis()
        ));
    }
}

