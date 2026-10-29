package com.healthapp.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ผลลัพธ์การวิเคราะห์ภาพอาหารและโภชนาการจาก Gemini Flash Vision")
public class FoodAnalysisResponse {

    @Schema(description = "ชื่ออาหารภาษาอังกฤษ", example = "Pad Krapao Gai with Fried Egg")
    private String foodName;

    @Schema(description = "ชื่ออาหารภาษาไทย", example = "ข้าวกะเพราไก่ไข่ดาว")
    private String thaiName;

    @Schema(description = "ประมาณการน้ำหนักสุทธิต่อจาน (กรัม)", example = "350.0")
    private Double portionWeightGrams;

    @Schema(description = "ระดับความมั่นใจของ AI (0.0 - 1.0)", example = "0.96")
    private Double confidenceScore;

    @Schema(description = "พลังงานรวม (กิโลแคลอรี)", example = "620")
    private Integer calories;

    @Schema(description = "โปรตีน (กรัม)", example = "32.5")
    private Double proteinG;

    @Schema(description = "คาร์โบไฮเดรต (กรัม)", example = "65.0")
    private Double carbsG;

    @Schema(description = "ไขมัน (กรัม)", example = "24.0")
    private Double fatG;

    @Schema(description = "โซเดียม (มิลลิกรัม)", example = "890.0")
    private Double sodiumMg;

    @Schema(description = "น้ำตาล (กรัม)", example = "3.5")
    private Double sugarG;

    @Schema(description = "ใยอาหาร (กรัม)", example = "2.1")
    private Double fiberG;

    @Schema(description = "กรรมวิธีการปรุงอาหาร", example = "STIR_FRIED")
    private String cookingMethod;

    @Schema(description = "รายการวัตถุดิบและส่วนประกอบหลักที่ตรวจพบ")
    private List<String> ingredients;

    @Schema(description = "คำแนะนำสุขภาพจากนักกำหนดอาหาร", example = "เมนูนี้ให้โปรตีนสูง เหมาะสำหรับการสร้างกล้ามเนื้อ แต่มีปริมาณโซเดียมและน้ำมันค่อนข้างมาก แนะนำลดการราดพริกน้ำปลาเพิ่ม")
    private String healthTips;

    @Schema(description = "โมเดล AI ที่ใช้ประมวลผล", example = "gemini-3.8-flash")
    private String aiModelUsed;

    @Schema(description = "สถานะว่าเป็นผลลัพธ์จำลอง (Mock) หรือไม่", example = "false")
    private Boolean isMock;
}

