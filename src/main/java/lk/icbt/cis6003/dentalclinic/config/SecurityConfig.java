package lk.icbt.cis6003.dentalclinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.RequestMatcherDelegatingAccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

/**
 * Who may use the system, and how login state is kept (FR1, and FR7 roles).
 *
 * This one file answers three assessed requirements: username and password
 * login, sessions and cookies used effectively, and role based access.
 *
 * HOW LOGIN STATE IS KEPT
 * A session cookie, named SUNRISEID and configured in application.properties.
 * It is marked HttpOnly, so page scripts cannot read it, and SameSite=Lax, so
 * another website cannot make the browser send it. It expires after thirty
 * minutes of inactivity, which matters at a reception desk where the screen is
 * left unattended.
 *
 * WHY CROSS SITE REQUEST FORGERY PROTECTION IS LEFT ON
 * Keeping login state in a cookie is exactly what makes that attack possible:
 * the browser attaches the cookie to any request to this site, including one
 * triggered by a different site the receptionist has open. Turning the
 * protection off would have made the web service easier to call by hand, and
 * would have been the wrong trade.
 *
 * The token is stored in a cookie a page script can read, so the booking screen
 * can send it back in the X-XSRF-TOKEN header. Reads need no token, because
 * reading changes nothing.
 *
 * WHY THERE ARE TWO KINDS OF REFUSAL
 * A person browsing a clinic page is redirected to the login screen. A program
 * calling /api/** gets 401 or 403 with a JSON message instead, because a login
 * page is not something a program can read, and returning one would look like
 * a successful reply.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Anything under /api/ is a web service call, not a person browsing. */
    private static final RequestMatcher WEB_SERVICE =
            request -> request.getRequestURI().startsWith("/api/");

    private final JsonSecurityResponses jsonSecurityResponses;

    public SecurityConfig(JsonSecurityResponses jsonSecurityResponses) {
        this.jsonSecurityResponses = jsonSecurityResponses;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(rules -> rules
                // The login page and the things a browser needs to draw it.
                .requestMatchers("/login", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                // ROLE BASED ACCESS (FR7)
                // The admin area covers the price list, staff accounts and the
                // clinic-wide reports. A receptionist books and bills; they do
                // not decide what a filling costs. The rule is written here and
                // not inside the controllers, so it still applies to a screen
                // that has not been built yet.
                .requestMatchers("/api/admin/**", "/api/reports/**", "/admin/**").hasRole("ADMIN")

                // Everything else needs a login. This is deliberately the last
                // rule, so a new screen is protected by default and has to be
                // opened up on purpose rather than by being forgotten.
                .anyRequest().authenticated())

            .formLogin(login -> login
                // Spring Security draws the login page for now. Step 7 replaces
                // it with the clinic's own Thymeleaf page.
                .defaultSuccessUrl("/", false)
                .permitAll())

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("SUNRISEID")
                .permitAll())

            .sessionManagement(session -> session
                // A new session id is issued at login, so a session id captured
                // before login cannot be used afterwards. This is the defence
                // against session fixation.
                .sessionFixation(fixation -> fixation.changeSessionId()))

            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))

            .exceptionHandling(handling -> handling
                // A web service call that is not signed in gets 401 and JSON.
                .defaultAuthenticationEntryPointFor(jsonSecurityResponses, WEB_SERVICE)
                // Everyone else is sent to the login screen.
                //
                // This second line is not decoration. Spring Security applies a
                // single entry point mapping to every request and ignores its
                // matcher; only when there are two or more does it start
                // choosing by path. Without this line, a receptionist opening
                // the clinic in a browser was answered 401 instead of being
                // shown the login page. The tests caught it.
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"), AnyRequestMatcher.INSTANCE)
                .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    /**
     * A refused permission is answered as JSON for the web service, and as the
     * ordinary error page for someone browsing.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        LinkedHashMap<RequestMatcher, AccessDeniedHandler> byPath = new LinkedHashMap<>();
        byPath.put(WEB_SERVICE, jsonSecurityResponses);

        return new RequestMatcherDelegatingAccessDeniedHandler(byPath, new AccessDeniedHandlerImpl());
    }

    /**
     * BCrypt, which is what the password_hash column stores.
     *
     * BCrypt is deliberately slow and salts every password separately, so two
     * members of staff who choose the same password still get different hashes,
     * and guessing them one by one is expensive. Strength 10 is the default and
     * matches the hashes already seeded in data.sql.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
