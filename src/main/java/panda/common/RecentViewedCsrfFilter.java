package panda.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RecentViewedCsrfFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;

    public RecentViewedCsrfFilter(
            @Value("${app.web.allowed-origins:http://localhost:5173,https://pandarealestate.store,https://www.pandarealestate.store}") String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (HttpMethod.POST.matches(method) && uri.matches("^/api/v1/listings/\\d+/view$")) {
            return false;
        }
        if (HttpMethod.DELETE.matches(method) && "/api/v1/listings/recent-viewed".equals(uri)) {
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            if (!allowedOrigins.contains(origin)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid origin");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String refererOrigin = extractOrigin(referer);
        if (refererOrigin == null || !allowedOrigins.contains(refererOrigin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid referer");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractOrigin(String referer) {
        try {
            URI uri = URI.create(referer);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            if (uri.getPort() > 0) {
                return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
            }
            return uri.getScheme() + "://" + uri.getHost();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
