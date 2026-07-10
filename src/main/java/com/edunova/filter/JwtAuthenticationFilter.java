package com.edunova.filter;


import com.edunova.config.ApplicationUserDetailsService;
import com.edunova.module.superadmin.repository.UserRepository;
import com.edunova.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// ── JWT Authentication Filter ──────────────────────────────────
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ApplicationUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && !jwtUtil.isRefreshToken(token)) {
            try {
                jwtUtil.validateAndExtractClaims(token);
                String userId = jwtUtil.extractUsername(token);
                //String role   = jwtUtil.extractRole(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                if (userDetails.isEnabled()) {
                    var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    var user = userRepository.findByUserEmail(userId)
                            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
                    LoggedInUserContextDetails.setCurrentUser(user);

                   /* Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, AuthorityUtils.commaSeparatedStringToAuthorityList(role));
                    SecurityContextHolder.getContext().setAuthentication(authentication);*/
                }
            } catch (ExpiredJwtException e) {
                log.warn("JWT Expired error: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");

                response.getWriter().write("""
                        {
                            "message":"Access token expired",
                            "code":"TOKEN_EXPIRED",
                            "status":"401"
                        }
                        """);

                return; // Stop processing
            } catch (Exception ex) {
                log.warn("JWT processing failed: {}", ex.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/",
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs",
            "/swagger-ui/",
            "/swagger-ui.html"
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        return PUBLIC_PATH_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix));
    }
}
