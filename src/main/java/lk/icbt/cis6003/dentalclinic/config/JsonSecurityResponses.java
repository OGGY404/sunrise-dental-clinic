package lk.icbt.cis6003.dentalclinic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.icbt.cis6003.dentalclinic.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * The answers the web service gives when security stops a request.
 *
 * WHY THESE EXIST
 * By default Spring Security answers a blocked request with a redirect to the
 * login page, or with an HTML error page. That is right for a person using a
 * browser and wrong for a program calling /api/**: a login page is not
 * something a program can read, and it would look like a successful reply.
 *
 * These two write the same ApiError body that every other failure uses, so the
 * caller reads "message" whatever went wrong, including when the problem was
 * that they were not allowed in.
 *
 * They are refused before any controller runs, which is why this cannot be done
 * in GlobalExceptionHandler: at this point in the request there is no
 * controller yet, only filters.
 */
@Component
public class JsonSecurityResponses implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityResponses(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Nobody is logged in, and this needs a login. */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException problem) throws IOException {

        write(request, response, HttpStatus.UNAUTHORIZED, "Not signed in",
                "Please sign in before using this service.");
    }

    /** Somebody is logged in, but this is not theirs to do. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException problem) throws IOException {

        write(request, response, HttpStatus.FORBIDDEN, "Not allowed",
                "Your account does not have permission to do that.");
    }

    private void write(HttpServletRequest request,
                       HttpServletResponse response,
                       HttpStatus status,
                       String error,
                       String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = new ApiError(status.value(), error, message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
