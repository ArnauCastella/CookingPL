package Frontend.CustomException;

public class SemanticException extends RuntimeException {

    public SemanticException() {
        super("Exception While Performing Frontend.Semantic Analysis");
    }

    public SemanticException(String message) {
        super(message);
    }

    public SemanticException(String message, Throwable cause) {
        super(message, cause);
    }
}