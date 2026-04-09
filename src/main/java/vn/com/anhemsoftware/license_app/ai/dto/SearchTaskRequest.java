package vn.com.anhemsoftware.license_app.ai.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SearchTaskRequest(
                @JsonPropertyDescription("Keyword to search in title or description") String keyword,

                @JsonPropertyDescription("Filter by completion status: true or false") Boolean completed,

                @JsonPropertyDescription("Filter by favorite task: true or false") Boolean favorite,

                @JsonPropertyDescription("Filter by priority level: LOW, MEDIUM, HIGH, CRITICAL") String priority,

                @JsonPropertyDescription("Filter due date less than or equal to (before) this date, format yyyy-MM-dd") LocalDate dueDateBefore,

                @JsonPropertyDescription("Filter due date greater than or equal to (after) this date, format yyyy-MM-dd") LocalDate dueDateAfter) {
}
