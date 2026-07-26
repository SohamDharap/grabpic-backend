package com.grabpic.backend.security;

import com.grabpic.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        log.debug("Skipping JWT filter for public endpoint: {}", request.getRequestURI());
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/actuator/health") ||
               path.equals("/actuator/info") ||
               path.equals("/api/system/status");
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        log.debug("Processing JWT filter for: {}", request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid Authorization header found");
            log.debug("JWT token validation failed");
        }

        filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtUtil.validateToken(token)) {
            log.debug("JWT token validated successfully for email: {}", email);
            String email = jwtUtil.extractEmail(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("Setting authentication for user: {}", email);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Get user ID from database and set as request attribute
                com.grabpic.backend.entity.UserDetails user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                
                request.setAttribute("userId", user.getId());
                request.setAttribute("userEmail", user.getEmail());
                request.setAttribute("userRole", user.getRole());

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("Authentication successful for user: {} with role: {}", email, user.getRole());
            }
        }

        log.debug("JWT token validation failed");
        }

        filterChain.doFilter(request, response);
    }
}