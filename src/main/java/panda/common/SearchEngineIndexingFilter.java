package panda.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SearchEngineIndexingFilter extends OncePerRequestFilter {

    private static final String X_ROBOTS_TAG = "noindex, nofollow, noarchive, nosnippet";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader("X-Robots-Tag", X_ROBOTS_TAG);
        filterChain.doFilter(request, response);
    }
}
