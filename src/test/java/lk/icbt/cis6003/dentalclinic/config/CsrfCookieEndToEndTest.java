package lk.icbt.cis6003.dentalclinic.config;

import lk.icbt.cis6003.dentalclinic.model.Role;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the running server over real HTTP, to prove that a receptionist can
 * still save something after logging in.
 *
 * WHY THIS TEST EXISTS, AND WHY IT IS NOT A MockMvc TEST
 * There was a bug here that every other test passed straight over. Spring
 * Security throws the CSRF token away at login and loads a new one only when
 * something asks for it. Nothing asked, so no new token cookie was ever sent,
 * and the first save after logging in was refused with 403 and no explanation.
 * Reading worked perfectly, which made it look as though the system was fine.
 *
 * The MockMvc tests could not catch it. The test framework supplies a valid
 * token itself rather than reading the cookie, so none of them ever depended on
 * the cookie being sent. The bug only appeared when the server was driven the
 * way a browser drives it: log in, follow the redirect, read the cookie, send
 * it back.
 *
 * So this test uses a real port and a real HTTP client, and carries the cookies
 * from one request to the next by hand, exactly as a browser would. It is
 * slower than the others, and it is the only kind that could have found this.
 */
@DisplayName("CSRF token over real HTTP")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CsrfCookieEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RestTemplate http;

    /** The cookies the "browser" is holding, exactly as a browser would keep them. */
    private final List<String> heldCookies = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Redirects must NOT be followed, because the login answer is a
        // redirect and its cookies have to be read before moving on.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setOutputStreaming(false);
        http = new RestTemplate(factory);
        http.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;   // let the test read 4xx answers instead of throwing
            }
        });

        heldCookies.clear();

        if (userRepository.findByUsername("reception").isEmpty()) {
            User user = new User();
            user.setUsername("reception");
            user.setPasswordHash(passwordEncoder.encode("Recep@123"));
            user.setFullName("Front Desk Receptionist");
            user.setEmail("reception@sunrisedental.lk");
            user.setRole(Role.RECEPTIONIST);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }

    @Test
    @DisplayName("after logging in, the token from the cookie lets a write through")
    void tokenFromCookieLetsAWriteThrough() {
        // 1. Open the login page. This is where the browser first receives a
        //    CSRF token.
        ResponseEntity<String> loginPage = send(HttpMethod.GET, "/login", null, null);
        assertThat(loginPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentToken()).as("a token is issued with the login page").isNotBlank();

        // 2. Sign in, sending that token back the way the login form would.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "reception");
        form.add("password", "Recep@123");
        form.add("_csrf", currentToken());

        ResponseEntity<String> signIn = send(
                HttpMethod.POST, "/login", MediaType.APPLICATION_FORM_URLENCODED, form);

        assertThat(signIn.getStatusCode())
                .as("signing in redirects rather than showing the form again")
                .isEqualTo(HttpStatus.FOUND);
        assertThat(signIn.getHeaders().getLocation()).asString().doesNotContain("error");

        // 3. Follow the redirect, as a browser does. THIS is the request that
        //    must hand over a fresh token, because the login answer threw the
        //    old one away. Before the fix, nothing came back here and the
        //    browser was left holding a token the server had forgotten.
        send(HttpMethod.GET, "/api/dentists", null, null);

        assertThat(currentToken())
                .as("a fresh token must be issued on the first request after signing in")
                .isNotBlank();

        // 4. Now try to save something, sending that token in the header.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-XSRF-TOKEN", currentToken());
        headers.add(HttpHeaders.COOKIE, String.join("; ", heldCookies));

        String booking = """
                {"fullName":"Kamal Silva","address":"No. 42, Galle Road, Colombo 03",
                 "contactNumber":"0771234567","dentistId":1,"treatmentId":1,
                 "appointmentDate":"%s","appointmentTime":"09:00:00"}
                """.formatted(java.time.LocalDate.now().plusDays(7));

        ResponseEntity<String> save = http.exchange(
                url("/api/appointments"), HttpMethod.POST, new HttpEntity<>(booking, headers), String.class);

        // The booking itself cannot succeed here, because this test database has
        // no dentists or treatments in it. That is fine and is not the point.
        // The point is that it must NOT be 403: getting past the CSRF check is
        // what proves the token in the cookie is the one the server expects.
        assertThat(save.getStatusCode())
                .as("the write must get past the CSRF check, body was: %s", save.getBody())
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("a write with no token at all is still refused")
    void writeWithoutTokenIsStillRefused() {
        send(HttpMethod.GET, "/login", null, null);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "reception");
        form.add("password", "Recep@123");
        form.add("_csrf", currentToken());
        send(HttpMethod.POST, "/login", MediaType.APPLICATION_FORM_URLENCODED, form);
        send(HttpMethod.GET, "/api/dentists", null, null);

        // Same signed-in browser, but the token header is left off.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, String.join("; ", heldCookies));

        ResponseEntity<String> save = http.exchange(
                url("/api/appointments"), HttpMethod.POST, new HttpEntity<>("{}", headers), String.class);

        assertThat(save.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- the small amount of browser behaviour this test needs ---------------

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    /** Sends a request with the cookies held so far, then stores any new ones. */
    private ResponseEntity<String> send(HttpMethod method,
                                        String path,
                                        MediaType contentType,
                                        MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        if (!heldCookies.isEmpty()) {
            headers.add(HttpHeaders.COOKIE, String.join("; ", heldCookies));
        }

        HttpEntity<?> request = (form != null) ? new HttpEntity<>(form, headers) : new HttpEntity<>(headers);
        ResponseEntity<String> response = http.exchange(url(path), method, request, String.class);

        rememberCookiesFrom(response);
        return response;
    }

    /**
     * Keeps the cookies the server sent, replacing any of the same name and
     * dropping the ones it asked to delete. That is all a browser does.
     */
    private void rememberCookiesFrom(ResponseEntity<String> response) {
        List<String> sent = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (sent == null) {
            return;
        }
        for (String setCookie : sent) {
            String pair = setCookie.split(";", 2)[0];
            String name = pair.split("=", 2)[0];
            String value = pair.contains("=") ? pair.split("=", 2)[1] : "";

            heldCookies.removeIf(held -> held.startsWith(name + "="));
            boolean beingDeleted = value.isEmpty() || setCookie.contains("Max-Age=0");
            if (!beingDeleted) {
                heldCookies.add(pair);
            }
        }
    }

    /** The CSRF token this "browser" is currently holding, or an empty string. */
    private String currentToken() {
        return heldCookies.stream()
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .map(cookie -> cookie.substring("XSRF-TOKEN=".length()))
                .findFirst()
                .orElse("");
    }
}
