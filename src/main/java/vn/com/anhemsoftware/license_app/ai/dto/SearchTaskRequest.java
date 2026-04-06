package vn.com.anhemsoftware.license_app.ai.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SearchTaskRequest(
        @JsonPropertyDescription("Từ khóa để tìm kiếm trong tiêu đề hoặc mô tả") 
        String keyword,
        
        @JsonPropertyDescription("Lọc theo trạng thái hoàn thành: true hoặc false") 
        Boolean completed,
        
        @JsonPropertyDescription("Lọc theo task yêu thích: true hoặc false") 
        Boolean favorite,
        
        @JsonPropertyDescription("Lọc theo mức độ ưu tiên: LOW, MEDIUM, HIGH, CRITICAL") 
        String priority,
        
        @JsonPropertyDescription("Lọc due date nhỏ hơn hoặc bằng (trước) ngày này, format yyyy-MM-dd") 
        LocalDate dueDateBefore,
        
        @JsonPropertyDescription("Lọc due date lớn hơn hoặc bằng (sau) ngày này, format yyyy-MM-dd") 
        LocalDate dueDateAfter
) {}
