package ro.ionutz.exception;

public class UncheckedCustomException extends RuntimeException {
    public UncheckedCustomException(String message, Throwable cause) {
        super(message, cause);
    }
}
