package com.collabsphere_first.security.config;

import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.security.CommunityPermissionEvaluator;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.security.jwt.JwtAuthenticationFilter;
import com.collabsphere_first.security.oauth2.CustomOAuth2UserService;
import com.collabsphere_first.security.oauth2.OAuth2FailureHandler;
import com.collabsphere_first.security.oauth2.OAuth2SuccessHandler;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${collobster.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
    private String corsAllowedOrigins;

    private final CustomOAuth2UserService customOAuth2UserService;

    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    private final OAuth2FailureHandler oAuth2FailureHandler;

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MembershipRepository membershipRepository;
    private final CommunityRoleService communityRoleService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.sameOrigin())
                        .xssProtection(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        ))
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(
                        new OriginValidationFilter(corsAllowedOrigins),
                        JwtAuthenticationFilter.class
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v1/auth/**",
                                "/api/v1/system/health",
                                "/v1/system/health",
                                "/api/v1/explore/**",
                                "/v1/explore/**",
                                "/api/v1/storage/raw/**",
                                "/v1/storage/raw/**",
                                "/h2-console/**",
                                "/actuator/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/ws/**",
                                "/realtime",
                                "/realtime/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error"
                        )
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .oauth2Login(oauth -> oauth

                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )

                        .successHandler(oAuth2SuccessHandler)

                        .failureHandler(oAuth2FailureHandler)
                )

                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI() != null && request.getRequestURI().startsWith("/api")
                        )
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customUserDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    // ── Communities Module Beans ──────────────────────────────────────────────

    /**
     * Registers the CommunityPermissionEvaluator to power
     * @PreAuthorize("hasPermission(communityId, 'MODERATOR')") on all community endpoints.
     */
    @Bean
    public CommunityPermissionEvaluator communityPermissionEvaluator(
            com.collabsphere_first.community.service.CommunityMembershipCacheService membershipCacheService) {
        return new CommunityPermissionEvaluator(membershipRepository, communityRoleService, membershipCacheService);
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CommunityPermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Idempotency-Key", "X-Webhook-Signature"));
        config.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
