package Frontend.SymbolTable;

public class Symbol {

    //private final Optional<Integer> arg_number; //Number of args expected in a function
//    public enum datatype {
//        temp,
//        gram,
//        milliliter
//    }
    private String type;
    private String infoLabel;
    private int addressOffset;

    public Symbol() {}
    
    /*public Optional<Integer> get_arg_number() {
        return arg_number;
    }*/

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public int getAddressOffset() {
        return addressOffset;
    }

    public void setAddressOffset(int addressOffset) {
        this.addressOffset = addressOffset;
    }

    public String getInfoLabel() {
        return infoLabel;
    }

    public void setInfoLabel(String infoLabel) {
        this.infoLabel = infoLabel;
    }
}
