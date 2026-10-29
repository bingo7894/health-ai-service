package com.healthapp.ai.service;

import com.healthapp.ai.dto.response.FoodAnalysisResponse;

public interface AiVisionService {

    /**
     * วิเคราะห์ภาพอาหารและคำนวณสารอาหารโภชนาการ
     *
     * @param imageBytes ไบต์ของรูปภาพอาหาร
     * @param mimeType ชนิดไฟล์ภาพ เช่น image/jpeg, image/png
     * @param userNotes หมายเหตุเพิ่มเติมจากผู้ใช้ (ถ้ามี)
     * @return FoodAnalysisResponse ข้อมูลสารอาหารและคำแนะนำ
     */
    FoodAnalysisResponse analyzeFood(byte[] imageBytes, String mimeType, String userNotes);

    /**
     * ดึงชื่อโมเดล AI ที่กำลังใช้งานอยู่
     */
    String getActiveModel();

    /**
     * ตรวจสอบว่ากำลังทำงานในโหมด Gemini Live หรือ Mock Fallback
     */
    boolean isLiveMode();
}

