package com.healthapp.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "ผลลัพธ์การวิเคราะห์ภาพอาหารและโภชนาการจาก Gemini Flash Vision")
public class FoodAnalysisResponse {

    @JsonAlias({"name", "dish_name", "dishName", "englishName", "food_name"})
    @Schema(description = "ชื่ออาหารภาษาอังกฤษ", example = "Pad Krapao Gai with Fried Egg")
    private String foodName;

    @JsonAlias({"thai_name", "name_thai", "thaiDishName", "thai"})
    @Schema(description = "ชื่ออาหารภาษาไทย", example = "ข้าวกะเพราไก่ไข่ดาว")
    private String thaiName;

    @JsonAlias({"portion_weight_grams", "portionWeight", "portion_weight_g", "weight_g", "estimated_weight_g", "portion_weight"})
    @Schema(description = "ประมาณการน้ำหนักสุทธิต่อจาน (กรัม)", example = "350.0")
    private Double portionWeightGrams;

    @JsonAlias({"confidence_score", "confidence"})
    @Schema(description = "ระดับความมั่นใจของ AI (0.0 - 1.0)", example = "0.96")
    private Double confidenceScore;

    @JsonAlias({"calories_kcal", "total_calories", "energy_kcal", "calorie"})
    @Schema(description = "พลังงานรวม (กิโลแคลอรี)", example = "620")
    private Integer calories;

    @JsonAlias({"protein", "protein_g", "protein_grams"})
    @Schema(description = "โปรตีน (กรัม)", example = "32.5")
    private Double proteinG;

    @JsonAlias({"carbs", "carbs_g", "carbohydrates", "carbohydrates_g"})
    @Schema(description = "คาร์โบไฮเดรต (กรัม)", example = "65.0")
    private Double carbsG;

    @JsonAlias({"fat", "fat_g", "total_fat_g"})
    @Schema(description = "ไขมัน (กรัม)", example = "24.0")
    private Double fatG;

    @JsonAlias({"sodium", "sodium_mg"})
    @Schema(description = "โซเดียม (มิลลิกรัม)", example = "890.0")
    private Double sodiumMg;

    @JsonAlias({"sugar", "sugar_g"})
    @Schema(description = "น้ำตาล (กรัม)", example = "3.5")
    private Double sugarG;

    @JsonAlias({"fiber", "fiber_g", "dietary_fiber_g"})
    @Schema(description = "ใยอาหาร (กรัม)", example = "2.1")
    private Double fiberG;

    @JsonAlias({"cooking_method", "method"})
    @Schema(description = "กรรมวิธีการปรุงอาหาร", example = "STIR_FRIED")
    private String cookingMethod;

    @JsonAlias({"ingredient_list", "ingredients_list"})
    @Schema(description = "รายการวัตถุดิบและส่วนประกอบหลักที่ตรวจพบ")
    private List<String> ingredients;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "วัตถุดิบสำคัญในจานอาหารพร้อมน้ำหนักและสารอาหาร")
    public static class KeyIngredientDto {
        @Schema(description = "ชื่อวัตถุดิบ เช่น ข้าวสวย, เนื้อไก่, ไข่ดาว", example = "ข้าวสวย")
        private String name;

        @Schema(description = "น้ำหนักประมาณการ (กรัม)", example = "180.0")
        private Double weightGrams;

        @Schema(description = "พลังงาน (กิโลแคลอรี)", example = "234")
        private Integer calories;

        @Schema(description = "โปรตีน (กรัม)", example = "4.5")
        private Double proteinG;

        @Schema(description = "คาร์โบไฮเดรต (กรัม)", example = "50.0")
        private Double carbsG;

        @Schema(description = "ไขมัน (กรัม)", example = "0.6")
        private Double fatG;
    }

    @JsonAlias({"key_ingredients", "majorIngredients", "major_ingredients"})
    @Schema(description = "รายการวัตถุดิบสำคัญ 2-4 ตัวหลักในจานพร้อมน้ำหนักกรัม")
    private List<KeyIngredientDto> keyIngredients;

    @JsonAlias({"health_tips", "healthTips", "advice", "dietetic_advice", "health_advice", "recommendations", "health_tip"})
    @Schema(description = "คำแนะนำสุขภาพจากนักกำหนดอาหาร", example = "เมนูนี้ให้โปรตีนสูง เหมาะสำหรับการสร้างกล้ามเนื้อ แต่มีปริมาณโซเดียมและน้ำมันค่อนข้างมาก แนะนำลดการราดพริกน้ำปลาเพิ่ม")
    private String healthTips;

    @Schema(description = "โมเดล AI ที่ใช้ประมวลผล", example = "gemini-3.8-flash")
    private String aiModelUsed;

    @Schema(description = "สถานะว่าเป็นผลลัพธ์จำลอง (Mock) หรือไม่", example = "false")
    private Boolean isMock;
}
