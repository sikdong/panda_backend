package panda.analytics.dto;

import java.util.List;

public record AdminDauResponseDto(
        List<AdminDauDailyMetricDto> data
) {
}
