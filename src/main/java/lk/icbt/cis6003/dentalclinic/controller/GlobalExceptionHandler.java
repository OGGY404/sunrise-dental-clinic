package lk.icbt.cis6003.dentalclinic.controller;

import jakarta.servlet.http.HttpServletRequest;
import lk.icbt.cis6003.dentalclinic.dto.ApiError;
import lk.icbt.cis6003.dentalclinic.exception.BadRequestException;
import lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException;
import lk.icbt.cis6003.dentalclinic.exception.ClinicException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.exception.SlotUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * The single place that turns a problem into an HTTP answer.
 *
 * PRESENTATION TIER. Nothing below this class knows what an HTTP status code
 * is, and that is the point: the business tier throws a plain Java exception
 * carrying a sentence for the receptionist, and this class decides how that
 * sentence travels back over the web.
 *
 * WHY EACH STATUS WAS CHOSEN
 *
 *   400 Bad Request        the form is wrong. The caller can fix it and retry.
 *   404 Not Found          that appointment or bill number does not exist.
 *   409 Conflict           the slot was free a moment ago and is not now. The
 *                          request was perfectly valid; someone else got there
 *                          first. That is exactly what Conflict means, and it
 *                          is why double booking has its own status.
 *   422 Unprocessable      the request was understood and correctly formed, but
 *                          a clinic rule refuses it, for example billing a
 *                          visit that has not happened. Retrying the same
 *                          request unchanged will never work.
 *   500 Internal error     a real bug. The message is deliberately vague,
 *                          because the details belong in the log, not on a
 *                          screen a patient may be standing in front of.
 *
 * The difference between 400 and 422 matters for the screen: a 400 means "you
 * typed it wrong, correct the box", a 422 means "the clinic will not allow
 * this".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * A form failed its validation annotations.
     *
     * Every box that failed is listed by name, so the screen can put each
     * message under the box it belongs to.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidForm(MethodArgumentNotValidException problem,
                                                      HttpServletRequest request) {
        ApiError body = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "Some of the details are not correct. Please check the fields listed and try again.",
                request.getRequestURI());

        for (FieldError fieldError : problem.getBindingResult().getFieldErrors()) {
            body.addFieldError(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * A value in the address could not be read as the type it should be, for
     * example a date written as 08-09-2026 instead of 2026-09-08.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleWrongType(MethodArgumentTypeMismatchException problem,
                                                    HttpServletRequest request) {
        String message = "The value '" + problem.getValue() + "' could not be read as "
                + problem.getName() + ". A date must be written as yyyy-MM-dd, for example 2026-09-08.";

        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(), "Bad request", message, request.getRequestURI()));
    }

    /** A required item in the address was missing altogether. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException problem,
                                                           HttpServletRequest request) {
        String message = "Please supply " + problem.getParameterName() + ".";

        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(), "Bad request", message, request.getRequestURI()));
    }

    /**
     * The body was not readable JSON at all, or an enum value was not one the
     * clinic accepts, for example a payment method of BITCOIN.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException problem,
                                                         HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad request",
                "The details sent could not be read. Please check the form and try again.",
                request.getRequestURI()));
    }

    /** That reference number does not exist. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException problem,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                HttpStatus.NOT_FOUND.value(), "Not found", problem.getMessage(), request.getRequestURI()));
    }

    /** Someone else booked that slot first. */
    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ApiError> handleSlotTaken(SlotUnavailableException problem,
                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                HttpStatus.CONFLICT.value(), "Slot unavailable", problem.getMessage(), request.getRequestURI()));
    }

    /** A clinic rule refuses this, and retrying unchanged will not help. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBrokenRule(BusinessRuleException problem,
                                                     HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ApiError(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), "Rule not satisfied",
                problem.getMessage(), request.getRequestURI()));
    }

    /**
     * Anything else the clinic code threw, including BadRequestException.
     *
     * This handler is last of the clinic ones on purpose. Spring always picks
     * the most specific handler, so the four above win for their own types and
     * this one only catches what is left.
     */
    @ExceptionHandler({BadRequestException.class, ClinicException.class})
    public ResponseEntity<ApiError> handleOtherClinicProblem(ClinicException problem,
                                                             HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(), "Bad request", problem.getMessage(), request.getRequestURI()));
    }

    /**
     * The address matches nothing in the whole application.
     *
     * This is listed by name rather than left to the catch-all, because
     * NoResourceFoundException extends ServletException and not
     * ErrorResponseException, so the handler below does not cover it. Without
     * this, every mistyped address came back as 500 and looked like a server
     * fault instead of a typing mistake. The security tests caught it.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNothingAtThatAddress(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                HttpStatus.NOT_FOUND.value(), "Not found",
                "There is nothing at that address.", request.getRequestURI()));
    }

    /**
     * Another problem Spring itself already knows the right status for, such as
     * a ResponseStatusException raised deeper in the framework.
     */
    @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
    public ResponseEntity<ApiError> handleSpringsOwnError(org.springframework.web.ErrorResponseException problem,
                                                          HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(problem.getStatusCode().value());
        String message = (status == HttpStatus.NOT_FOUND)
                ? "There is nothing at that address."
                : problem.getBody().getDetail();

        return ResponseEntity.status(status).body(new ApiError(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }

    /** The address exists but not for this kind of request, for example GET instead of POST. */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleWrongMethod(
            org.springframework.web.HttpRequestMethodNotSupportedException problem,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ApiError(
                HttpStatus.METHOD_NOT_ALLOWED.value(), "Method not allowed",
                "That address cannot be used with " + problem.getMethod() + ".",
                request.getRequestURI()));
    }

    /**
     * The person is logged in, but this is not theirs to do.
     *
     * This is handled by name so it does not fall through to the catch-all
     * below and come back as 500. A refused permission is not a bug, and 403
     * is the honest answer.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleNotAllowed(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                HttpStatus.FORBIDDEN.value(), "Not allowed",
                "Your account does not have permission to do that.", request.getRequestURI()));
    }

    /**
     * A genuine bug.
     *
     * The real cause goes to the log where a developer can find it. The screen
     * gets a plain sentence, because a stack trace on a reception desk monitor
     * helps nobody and can leak how the system is built.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAnythingElse(Exception problem, HttpServletRequest request) {
        log.error("Unexpected failure while handling {}", request.getRequestURI(), problem);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal error",
                "Something went wrong at our end. Please try again, and tell the system administrator if it keeps happening.",
                request.getRequestURI()));
    }
}
