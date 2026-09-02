package lk.icbt.cis6003.dentalclinic.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Makes sure the XSRF-TOKEN cookie is actually sent to the browser.
 *
 * WHY THIS IS NEEDED, AND HOW THE PROBLEM SHOWED ITSELF
 * Spring Security 6 loads the CSRF token lazily: the cookie is only written if
 * something during the request actually asks for the token value. At login it
 * also throws the old token away and issues a new one, for the good reason that
 * a token captured before login must not still work afterwards.
 *
 * Put those two together and there is a gap. The login response deletes the old
 * cookie, nothing on that response asks for the new token, so no new cookie is
 * written. The very next save the receptionist tries is then refused, with no
 * explanation, because the browser is holding a token the server has forgotten.
 *
 * That is exactly what happened the first time the system was driven end to end
 * against the running server. Every read worked and every write came back 403.
 * The automated tests had not caught it, because the test framework supplies a
 * valid token itself instead of reading the cookie, so it never depended on the
 * cookie being written. It took a real request to find it.
 *
 * Asking for the token value here forces it to be written on every response, so
 * the browser always holds the token the server is currently expecting.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            // Reading the value is what causes the cookie to be written. The
            // returned string is deliberately not used for anything else.
            token.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
