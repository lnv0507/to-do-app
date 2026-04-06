package vn.com.anhemsoftware.license_app.ai.dto;

import java.util.List;

public record MultipleTaskFavoriteRequest(
        List<Long> ids,
        boolean favorite
) {}
