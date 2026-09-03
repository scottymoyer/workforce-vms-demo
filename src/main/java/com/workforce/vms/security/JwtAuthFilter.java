package com.workforce.vms.security;

import io.jsonwebtoken.Claims;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Lightweight JWT filter. If a valid "Authorization: Bearer &lt;token&gt;" is present,
 * it resolves the user and pushes the identity onto the Datadog trace so every
 * downstream trace + security signal carries usr.id (account attribution).
 *
 * Intentionally does NOT enforce authentication on endpoints — the demo needs the
 * vulnerable endpoints reachable both anonymously and as an attributed user.
 */
@Component
@Order(1)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                String username = claims.getSubject();
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);
                request.setAttribute("usr.id", username);
                // Attribute the trace + any security signal to this account.
                DatadogUserTracking.setUser(username, email, role);
            } catch (Exception ignored) {
                // Invalid/expired token → treat as anonymous for the demo.
            }
        }
        chain.doFilter(request, response);
    }
}
