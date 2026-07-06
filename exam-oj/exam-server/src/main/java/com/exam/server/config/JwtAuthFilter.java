package com.exam.server.config;

import com.exam.common.JwtUtil;
import com.exam.common.SecurityUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/user/login") || path.startsWith("/api/user/email/sendCode")) {
            chain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String token = auth.substring(7);
                if (!JwtUtil.isExpired(token)) {
                    SecurityUtil.set(JwtUtil.getUserId(token), JwtUtil.getRole(token));
                }
            } catch (Exception ignored) {
            }
        }
        chain.doFilter(request, response);
        SecurityUtil.clear();
    }
}
