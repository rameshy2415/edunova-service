package com.edunova.auth.controller;

import com.edunova.auth.dto.AuthRequest;
import com.edunova.auth.dto.AuthResponse;
import com.edunova.common.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bkp-auth")
@RequiredArgsConstructor
public class AuthControllerBkp {
    private final AuthenticationManager authenticationManager;
    private final Environment env;

    @Value("${app.jwt.secret}")
    public String secret;

    @Value("${app.jwt.access-token-expiry-ms}")
    public long expiry;

    @PostMapping("/logins")
    public ResponseEntity<ApiResponse<AuthResponse>> staffLogin(@RequestBody AuthRequest.LoginRequest request, HttpServletRequest httpRequest) {


//        AuthResponse.builder()
//                .token("jbfjefbjefbjeb")
//                .refreshToken("hbhbbeb")
//                .tokenType("Bearer")
//                .user(AuthResponse.UserInfo.builder()
//                        .id(user.getId())
//                        .schoolId(user.getSchoolId())
//                        .firstName(user.getFirstName())
//                        .lastName(user.getLastName())
//                        .email(user.getEmail())
//                        .mobile(user.getMobile())
//                        .roles(user.getRoles().stream()
//                                .map(Role::getName)
//                                .collect(Collectors.toList()))
//                        .build())
//                .build();



       //AuthResponse response = authService.staffLogin(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", null));
    }

    @PostMapping("/apiLogin")
    public ResponseEntity<ApiResponse<AuthResponse>> apiLogin (@RequestBody AuthRequest.LoginRequest request) {
        String jwt = "";
        Authentication authentication = UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword());
        Authentication authenticationResponse = authenticationManager.authenticate(authentication);
        if(null != authenticationResponse && authenticationResponse.isAuthenticated()) {
            if (null != env) {
                //String secret = env.getProperty(ApplicationConstants.JWT_SECRET_KEY, ApplicationConstants.JWT_SECRET_DEFAULT_VALUE);
                SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                jwt = Jwts.builder().issuer("EduNova").subject("JWT Token")
                        .claim("username", authenticationResponse.getName())
                        .claim("name",authenticationResponse.getName())
                        //.claim("email",authenticationResponse.get())
                        .claim("authorities", authenticationResponse.getAuthorities().stream().map(
                                GrantedAuthority::getAuthority).collect(Collectors.joining(",")))
                        .issuedAt(new java.util.Date())
                        .expiration(new java.util.Date((new java.util.Date()).getTime() + expiry))
                        .signWith(secretKey).compact();
            }
        }


        var authResponse = AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(jwt)
                .tokenType("Bearer")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
       /* return ResponseEntity.status(HttpStatus.OK).header(ApplicationConstants.JWT_HEADER,jwt)
                .body(new LoginResponseDTO(HttpStatus.OK.getReasonPhrase(), jwt));*/
    }
}
