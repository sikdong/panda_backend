package panda.analytics.dto;

public record AdminDauDailyMetricDto(
        String date,
        Integer dau,
        Integer visits
) {
}
