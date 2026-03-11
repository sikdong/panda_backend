package panda.analytics;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import panda.analytics.dto.AdminDauResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dau")
    public AdminDauResponseDto getDau(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        return new AdminDauResponseDto(analyticsService.getDailyMetrics(startDate, endDate));
    }
}
