package vn.com.anhemsoftware.license_app.ai.dto;

import vn.com.anhemsoftware.license_app.entity.Priority;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record CreateTaskRequest(
        @JsonProperty(required = true) 
        @JsonPropertyDescription("Task title, mandatory") 
        String title,
        
        @JsonPropertyDescription("Detailed description for the task") 
        String description,
        
        @JsonPropertyDescription("Completion status: true if finished, false if not") 
        boolean completed,
        
        @JsonPropertyDescription("Priority level. MUST BE ONE OF THE FOLLOWING VALUES: LOW, MEDIUM, HIGH, CRITICAL") 
        Priority priority,
        
        @JsonPropertyDescription("Detailed specification of the task") 
        String specification,
        
        @JsonPropertyDescription("Task due date, default format yyyy-MM-dd") 
        LocalDate dueDate,
        
        @JsonPropertyDescription("Image URL of the task") 
        String imageUrl
) {}
