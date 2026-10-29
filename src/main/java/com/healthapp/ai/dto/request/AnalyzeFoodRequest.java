package com.healthapp.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "คำขอวิเคราะห์ภาพอาหารผ่าน Base64 (ทางเลือกนอกเหนือจาก Multipart Form-Data)")
public class AnalyzeFoodRequest {

    @Schema(description = "รูปภาพอาหารที่เข้ารหัสแบบ Base64 (ไม่รวม data:image/... prefix)")
    private String imageBase64;

    @Schema(description = "MIME Type ของรูปภาพ เช่น image/jpeg, image/png", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "ข้อมูลเพิ่มเติมจากผู้ใช้ เช่น 'ไม่ใส่ผงชูรส', 'ไก่ไม่เอาหนัง'", example = "ไข่ดาวไม่สุก ข้าวน้อย")
    private String userNotes;
}

