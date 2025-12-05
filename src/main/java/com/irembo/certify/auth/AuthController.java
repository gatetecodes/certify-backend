package com.irembo.certify.auth;

import com.irembo.certify.auth.dto.LoginRequest;
import com.irembo.certify.auth.dto.LoginResponse;
import com.irembo.certify.security.CustomUserDetails;
import com.irembo.certify.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principalToUser(principal));

        log.info("LOGIN requested for email={}, authenticated as email={}, role={}",
         request.email(), principal.getUsername(), principal.getRole());

        return ResponseEntity.ok(new LoginResponse(
                token,
                principal.getId(),
                principal.getTenantId(),
                principal.getFullName(),
                principal.getUsername(),
                principal.getRole()
        ));
    }

    /**
     * Lightweight adapter to reuse JwtService
     */
    private com.irembo.certify.user.User principalToUser(CustomUserDetails principal) {
        com.irembo.certify.user.User user = new com.irembo.certify.user.User();
        user.setId(principal.getId());
        user.setTenantId(principal.getTenantId());
        user.setEmail(principal.getUsername());
        user.setPasswordHash(principal.getPassword());
        user.setRole(principal.getRole());
        user.setFullName(principal.getFullName());
        return user;
    }
}
