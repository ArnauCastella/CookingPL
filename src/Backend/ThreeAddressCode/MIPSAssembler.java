package Backend.ThreeAddressCode;

import Frontend.Grammar.BNFSymbol;
import Frontend.Parser.Tree;
import Frontend.SymbolTable.Symbol;
import Frontend.SymbolTable.SymbolTable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MIPSAssembler {

    private ThreeAddressCode tac;

    public MIPSAssembler(ThreeAddressCode tac) {
        this.tac = tac;
    }

    private void declareVariablesMIPS (BufferedWriter writer, Tree<Integer, SymbolTable> symbolTable) throws IOException {
        // Get all the variables in the Symbols table for a given head.
        SymbolTable data = symbolTable.getData();

        Hashtable<String, Symbol> keysTable = new Hashtable<String, Symbol>();
        keysTable = data.getTable();
        // Getting the variables as keys.

        // Getting an iterator for the hashtable's values
        Iterator<String> symbolIterator = keysTable.keySet().iterator();
        // Looping through the hashtable and retrieving each symbol
        while (symbolIterator.hasNext()) {
            String key = symbolIterator.next();
            Symbol symbol = keysTable.get(key);

            if (symbol.getType().equals("g") || symbol.getType().equals("temp")) {
                writer.write(key+": .word 0");
            } else if (symbol.getType().equals("ml")) {
                writer.write(key+": .word 0.00");
            }
            writer.newLine();
        }

        // Getting leaves of current node.
        ArrayList<Tree<Integer, SymbolTable>> leaves = symbolTable.getLeaves();

        // Call a method for each node in the ArrayList
        for (Tree<Integer, SymbolTable> node : leaves) {
            // Call your desired method on the 'tree' object
            declareVariablesMIPS(writer, node);
        }
    }

    public void generateMIPSFromTAC(List<TACNode> tacList, Tree<Integer, SymbolTable> symbolTable) throws IOException {

        // Write MIPS Code to file.
        String fileName = "Cooking_PL_Assembly_Code.asm";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            // Declare variables.
            //writer.write(".data");
            //writer.newLine();

            //declareVariablesMIPS(writer, symbolTable);

            // Write .text to start with program instructions.
            writer.write(".text");
            writer.newLine();

            // Store stack pointer memory.
            int auxCounter = ThreeAddressCode.addressCounter - 4;
            writer.write("subu $sp,$sp,"+Integer.toString(auxCounter));
            writer.newLine();

            List<String> mipsCode = new ArrayList<>();
            for (TACNode tacInstruction : tacList) {
                //String[] parts = tacInstruction.split(" ");
                String res = tacInstruction.getResult();
                String op = tacInstruction.getOp();
                String opr1 = tacInstruction.getOperand1();
                String opr2 = tacInstruction.getOperand2();

                String mipsInstruction = switch (op) {
                    case "load_literal" -> "li " + res + "," + opr1;
                    case "sum" -> "add " + res + "," + opr1 + "," + opr2;
                    case "assignment" -> "sw " + opr1 + "," + res + "($sp)";
                    case "assignmentTwo" -> "lw " + res + "," + opr1 + "($sp)";
                    case "load_word" -> "lw " + res + "," + opr1 + "($sp)";
                    case "load_word_reg_to_reg" -> "lw " + res + "," + opr1;
                    case "subtraction" -> "sub " + res + "," + opr1 + "," + opr2;
                    case "multiply" -> "mul " + res + "," + opr1 + "," + opr2;
                    case "division" -> "div " + opr1 + "," + opr2+"\n"+"mflo "+res;
                    case "branch_not_equal" -> "bne " + opr1 + "," + opr2+ "," +res;
                    case "branch_greater_than" -> "bgt " + opr2 + "," + opr1 + "," +res;
                    case "branch_less_than" -> "blt " + opr1 + "," + opr2+ "," +res;
                    case "branch_less_equal" -> "ble " + opr1 + "," + opr2+ "," +res;
                    case "branch_more_equal" -> "bge " + opr2 + "," + opr1+ "," +res;
                    case "argument_load" -> "lw " + "$a"+opr1 + "," + opr2+ "($sp)";
                    case "parameter_to_offset" -> "sw " + "$a"+opr1 + "," + opr2+ "($sp)";
                    case "return_assignment" -> "sw " + "$v0," + opr2 + "($sp)";
                    case "backup_ra" -> "sw $ra,"+res;
                    case "restore_ra" -> "lw $ra,"+res;
                    case "ra_to_sX" -> "lw $s"+opr1+","+res;
                    case "sX_to_sp" -> "sw $s"+opr1+","+res;
                    case "endif" -> res + ":";
                    case "jump" -> "j " + res;
                    case "jump_function" -> "jal " + res;
                    case "label" -> "L" + res +":";
                    case "function_label" -> res +":";
                    case "return_ra" -> "jr $ra";
                    case "labelTwo" -> res+":";
                    case "main" -> "MAIN:";
                    case "jump_main" -> "j MAIN";
                    default -> "";
                };

                mipsCode.add(mipsInstruction);
            }

            for (String instruction : mipsCode) {
                writer.write(instruction);
                writer.newLine();
            }


            // Release stack pointer memory
            writer.write("addu $sp,$sp,"+Integer.toString(auxCounter));
            writer.newLine();

            // Exit the program
            writer.write("li $v0,10");
            writer.newLine();

            writer.write("syscall");
            writer.newLine();

            System.out.println("MIPS assembly code written to file: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
