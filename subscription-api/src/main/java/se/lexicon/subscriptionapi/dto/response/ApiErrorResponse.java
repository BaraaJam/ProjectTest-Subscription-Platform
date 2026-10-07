package se.lexicon.subscriptionapi.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        List<String> details
) {

    // Convenience constructor for single message (no details list)
    public ApiErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, LocalDateTime.now(), List.of());
    }

    // Convenience constructor with details list (for Bean Validation field errors)
    public ApiErrorResponse(int status, String error, String message, String path, List<String> details) {
        this(status, error, message, path, LocalDateTime.now(), details);
    }
}
