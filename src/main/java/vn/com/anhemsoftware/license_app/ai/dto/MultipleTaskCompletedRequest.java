package vn.com.anhemsoftware.license_app.ai.dto;

import java.util.List;

public record MultipleTaskCompletedRequest(
        List<Long> ids,
        boolean completed
) {}
