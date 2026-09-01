package lk.icbt.cis6003.dentalclinic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * The one shape every failure comes back in.
 *
 * WHY EVERY ERROR LOOKS THE SAME
 * Without this, a missing appointment would return a Spring error page, a
 * broken clinic rule would return a stack trace, and a bad form would return
 * something else again. The screen would then need three ways to read a
 * failure. With one shape, the screen reads "message" every time and shows it.
 *
 * fieldErrors is only present when the form itself was wrong. It maps the name
 * of each box that failed to the sentence explaining why, so the screen can put
 * the message under the right box instead of at the top of the page.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiError {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Map<String, String> fieldErrors = new TreeMap<>();

    public ApiError(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /** Records one box that was filled in wrongly. */
    public void addFieldError(String field, String problem) {
        fieldErrors.put(field, problem);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
