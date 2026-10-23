package ch.example.adressen.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The login: a form on login.html, a cookie session, and what each role may do from security.yml. Not signed in: the API
 * answers 401, a page leads to the login. Signed in without the right: 403. What the entity doesn't allow at all
 * (`@operations`) is answered with 405 by the controller, after this check.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** What a user who has to choose a password may still reach. */
    private static final Set<String> OPEN_WHILE_CHANGING =
            Set.of("/api/password", "/api/me", "/logout", "/change-password.html", "/app.css", "/error");

    /** The Bootstrap style sheet (WebJar) that the sign-in and the password page use. */
    private static final String BOOTSTRAP = "/webjars/bootstrap/**";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityStore store) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/login", "/h2-console/**"))
                .authorizeHttpRequests(requests -> requests.requestMatchers(
                                "/login.html", "/login", "/app.css", "/error", BOOTSTRAP)
                        .permitAll()
                        // The H2 console (development, application.yml) shows every table: only for the role ADMIN, whatever security.yml says per entity.
                        .requestMatchers("/h2-console/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/me", "/api/password", "/change-password.html")
                        .authenticated()
                        .requestMatchers("/api/**")
                        .access((authentication, context) ->
                                new AuthorizationDecision(permits(store, authentication.get(), context.getRequest())))
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form.loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            if (store.mustChangePassword(authentication.getName())) {
                                response.sendRedirect("/change-password.html");
                            } else {
                                // back to the page that led to the login (e.g. the H2 console), else the home page
                                new SavedRequestAwareAuthenticationSuccessHandler()
                                        .onAuthenticationSuccess(request, response, authentication);
                            }
                        })
                        .failureUrl("/login.html?error"))
                // The H2 console is a page with frames of its own origin.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login.html"))
                .exceptionHandling(handling -> handling.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterBefore(
                        new PasswordChangeFilter(store),
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        return http.build();
    }

    /** Whether the user may do what the request does to the entity of its path. */
    static boolean permits(SecurityStore store, Authentication authentication, HttpServletRequest request) {
        EntityAccess.Resource resource = EntityAccess.resourceOf(request.getRequestURI());
        if (resource == null) {
            return authentication != null && authentication.isAuthenticated();
        }
        String operation = EntityAccess.operationOf(request.getMethod());
        // What the entity doesn't allow for anybody ends in the 405 of the controller, whoever asks.
        if (!resource.operations().contains(operation)) {
            return authentication != null && authentication.isAuthenticated();
        }
        return store.permissions(rolesOf(authentication), resource).contains(operation);
    }

    /** The roles of the signed-in user: the authorities ROLE_x (Spring adds others of its own, such as FACTOR_PASSWORD). */
    public static java.util.List<String> rolesOf(Authentication authentication) {
        return AuthorityUtils.authorityListToSet(authentication.getAuthorities()).stream()
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList();
    }

    /** Makes the browser get the XSRF-TOKEN cookie on the first request (the pages send it back as a header). */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }

    /** Until a user who has to choose a password has done so, everything but the way to do it is closed. */
    static final class PasswordChangeFilter extends OncePerRequestFilter {
        private final SecurityStore store;

        PasswordChangeFilter(SecurityStore store) {
            this.store = store;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication();
            String path = request.getRequestURI();
            boolean signedIn = authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication
                            instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
            if (signedIn && store.mustChangePassword(authentication.getName()) && !OPEN_WHILE_CHANGING.contains(path)
                    && !path.startsWith("/webjars/bootstrap/")) {
                if (path.startsWith("/api/")) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"password change required\"}");
                } else {
                    response.sendRedirect("/change-password.html");
                }
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
