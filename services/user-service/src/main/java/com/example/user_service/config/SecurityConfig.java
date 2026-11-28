package com.example.user_service.config;


import com.example.user_service.security.GoogleLoginFailureHandler;
import com.example.user_service.security.GoogleLoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/*
 * Three filter chains. Spring sends each request to the first chain (lowest @Order) whose securityMatcher fits it.
 * Separate chains are needed because each group of endpoints needs different rules that can't live in one chain:
 * public endpoints check no token (so an expired token can't block login), Google login needs a session,
 * and everything else must be stateless and require a JWT.
 */
@Configuration
public class SecurityConfig {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    // Endpoints anyone can call without being logged in.
    public static final RequestMatcher PUBLIC_ENDPOINTS = new OrRequestMatcher(
            PATHS.matcher(HttpMethod.POST, "/auth/login"),
            PATHS.matcher(HttpMethod.POST, "/auth/register"),
            PATHS.matcher(HttpMethod.POST, "/auth/refresh-token"),
            PATHS.matcher(HttpMethod.POST, "/auth/logout"),
            PATHS.matcher(HttpMethod.POST, "/auth/forgot-password"),
            PATHS.matcher(HttpMethod.POST, "/auth/reset-password"),
            PATHS.matcher(HttpMethod.POST, "/users/me/delete/confirm"),
            PATHS.matcher(HttpMethod.POST, "/users/me/email/confirm"),
            PATHS.matcher(HttpMethod.GET, "/.well-known/jwks.json")
    );

    /*
     * Google login paths: /oauth2/authorization/google starts the login, /login/oauth2/code/google is Google's callback.
     * Needed so the Google chain only handles these two paths. Without a matcher it would catch every request,
     * and the session it allows would leak into the rest of the API.
     */
    public static final RequestMatcher OAUTH2_ENDPOINTS = new OrRequestMatcher(
            PATHS.matcher("/oauth2/**"),
            PATHS.matcher("/login/oauth2/**")
    );

    /*
     * Public endpoints: no token checked, no session, everything allowed.
     * No resource server here on purpose: the browser may still send an old or expired access token,
     * and checking it would return 401 on login, refresh or logout, the very endpoints used to fix that.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PUBLIC_ENDPOINTS)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /*
     * Google login. The only chain allowed a session: it holds the OAuth state between the redirect to Google and the callback.
     * The success/failure handlers drop the session and redirect the browser to the frontend.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain oauth2LoginSecurityFilterChain(HttpSecurity http,
                                                              GoogleLoginSuccessHandler successHandler,
                                                              GoogleLoginFailureHandler failureHandler) throws Exception {
        http
                .securityMatcher(OAUTH2_ENDPOINTS)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));

        return http.build();
    }

    /*
     * Everything else. No securityMatcher, so it catches all remaining requests and must stay last.
     * Requires a valid JWT in the Authorization header (Tyk copies it there from the access_token cookie).
     */
    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/users/{id}/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/{id}/roles").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        })
                        .jwt(jwt -> {}));

        return http.build();
    }

    /*
     * Turns the JWT "roles" claim (e.g. ["ADMIN"]) into Spring authorities (ROLE_ADMIN) so hasRole("ADMIN") works.
     * Needed because by default Spring only reads the "scope"/"scp" claim and prefixes it with SCOPE_,
     * so our "roles" claim would be ignored and every hasRole(...) check would fail.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // Hashes and checks passwords with BCrypt.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
