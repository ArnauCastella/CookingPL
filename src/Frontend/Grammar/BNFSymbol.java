package Frontend.Grammar;

public class BNFSymbol {

    public enum SymbolType {
        String,
        Regex,
        Symbol, //Any symbol (either terminal or non-terminal)
        Spechar, //Any special char (e.g. parentheses, plus sign, etc.)
        Epsilon, //the null (ε) symbol
        End //the $ symbol
    }
    private final boolean isTerminal;
    private final String value;
    private final String name;
    private final SymbolType type;

    private int ScopeId = -1;

    public BNFSymbol(String name, String value, boolean isTerminal, SymbolType type) {
        this.name = name;
        this.value = value;
        this.isTerminal = isTerminal;
        this.type = type;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public SymbolType getType() {
        return type;
    }
    public void setScopeId(int id) {
        ScopeId = id;
    }
    public int getScopeId() {
        return ScopeId;
    }
}
