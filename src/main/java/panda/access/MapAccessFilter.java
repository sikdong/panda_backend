package panda.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MapAccessFilter extends OncePerRequestFilter {

    private final MapAccessService mapAccessService;

    public MapAccessFilter(MapAccessService mapAccessService) {
        this.mapAccessService = mapAccessService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return !request.getRequestURI().startsWith("/api/v1/map/listings");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader("X-Map-Access-Token");
        if (!mapAccessService.isAuthorized(token)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "지도 매물 비밀번호를 입력해 주세요.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
