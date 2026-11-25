package io.multilang.oj.http;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LocalRequestFilter extends OncePerRequestFilter {
    private static final Set<String> LOCAL_NAMES = Set.of("localhost", "127.0.0.1");
    // The largest authoring request can contain 20 cases plus statement text, encoded as JSON.
    private static final long MAX_JSON_BYTES = 3L * 1024 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain next)
            throws ServletException, IOException {
        String host = request.getServerName();
        String origin = request.getHeader("Origin");
        if (!LOCAL_NAMES.contains(host) || "cross-site".equals(request.getHeader("Sec-Fetch-Site"))
                || (origin != null && !isLocalOrigin(origin, request.getLocalPort()))) {
            response.sendError(403);
            return;
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        response.setHeader("Cache-Control", "no-store");
        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            long size = request.getContentLengthLong();
            if (size < 0 || size > MAX_JSON_BYTES) {
                response.sendError(size < 0 ? 411 : 413);
                return;
            }
        }
        next.doFilter(request, response);
    }

    private static boolean isLocalOrigin(String origin, int port) {
        try {
            URI parsed = URI.create(origin);
            return "http".equals(parsed.getScheme()) && parsed.getHost() != null
                    && LOCAL_NAMES.contains(parsed.getHost()) && parsed.getRawUserInfo() == null
                    && parsed.getRawQuery() == null && parsed.getRawFragment() == null
                    && "".equals(parsed.getRawPath()) && (parsed.getPort() == -1 ? 80 : parsed.getPort()) == port;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
