package Backend.ThreeAddressCode;

import Frontend.Grammar.BNFSymbol;

import java.util.ArrayList;
import java.util.List;

public class TACNode {
    private int label;
    private String result;
    private String operand1;
    private String operand2;
    private String op;

    public TACNode(int label, String result, String operand1, String operand2, String op) {
        this.label = label;
        this.result = result;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.op = op;
    }

    public int getLabel() {
        return label;
    }

    public String getResult() {
        return result;
    }

    public String getOperand1() {
        return operand1;
    }

    public String getOperand2() {
        return operand2;
    }

    public String getOp() {
        return op;
    }
}
