package Frontend.CustomException;

public class LexerException extends RuntimeException {
    private final String value;
    public LexerException(String value) {
        super("Frontend.Lexer exception");
        this.value = value;
    }

    public LexerException(String value, String message) {
        super(message);
        this.value = value;
    }

    public LexerException(String value, String message, Throwable cause) {
        super(message, cause);
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}