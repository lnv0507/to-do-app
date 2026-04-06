package vn.com.anhemsoftware.license_app.ai.dto;

import vn.com.anhemsoftware.license_app.entity.Priority;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record CreateTaskRequest(
        @JsonProperty(required = true) 
        @JsonPropertyDescription("Tiêu đề của task, bắt buộc phải có") 
        String title,
        
        @JsonPropertyDescription("Mô tả chi tiết cho task") 
        String description,
        
        @JsonPropertyDescription("Trạng thái hoàn thành: true nếu đã xong, false nếu chưa") 
        boolean completed,
        
        @JsonPropertyDescription("Mức độ ưu tiên. CHÚ Ý PHẢI LÀ MỘT TRONG CÁC GIÁ TRỊ SAU: LOW, MEDIUM, HIGH, CRITICAL") 
        Priority priority,
        
        @JsonPropertyDescription("Đặc tả chi tiết của task") 
        String specification,
        
        @JsonPropertyDescription("Ngày hết hạn của task, format mặc định yyyy-MM-dd") 
        LocalDate dueDate,
        
        @JsonPropertyDescription("Đường dẫn hình ảnh của task") 
        String imageUrl
) {}
