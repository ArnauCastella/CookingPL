package Frontend.CustomException;

public class BNFException extends RuntimeException {
    private final String statement;
    public BNFException(String statement) {
        super("BNF exception");
        this.statement = statement;
    }

    public BNFException(String statement, String message) {
        super(message);
        this.statement = statement;
    }

    public BNFException(String statement, String message, Throwable cause) {
        super(message, cause);
        this.statement = statement;
    }

    public String getStatement() {
        return this.statement;
    }
}
