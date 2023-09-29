package Backend.ThreeAddressCode;

import Frontend.CustomException.SemanticException;
import Frontend.Grammar.BNFSymbol;
import Frontend.Parser.Tree;
import Frontend.SymbolTable.Symbol;
import Frontend.SymbolTable.SymbolTable;

import java.util.*;

public class ThreeAddressCode {

    Tree<Integer, BNFSymbol> tree;
    Tree<Integer, SymbolTable> symbolTable;
    private List<TACNode> tacRules;
    private Stack<TACNode> tacStack;

    public static int addressCounter;
    private static int labelCounter;
    private static int functionCallsCount;

    private ArrayList<String> raArray = new ArrayList<>();

    // Create an ArrayList of strings
    private ArrayList<String> labelsArray = new ArrayList<>();

    private String currentFunctionName;

    private boolean otherwiseEndIfControlFlag;

    private int argumentCount;

    public ThreeAddressCode(Tree<Integer, BNFSymbol> tree, Tree<Integer, SymbolTable> symbolTable) {
        this.tree = tree;
        this.symbolTable = symbolTable;
        addressCounter = 0;
        labelCounter = 0;
        argumentCount = 0;
        functionCallsCount = 0;
    }

    public List<TACNode> generateTacMap() {
        tacRules = new ArrayList<>();
        tacStack = new Stack<TACNode>();
        Queue<Tree<Integer, BNFSymbol>> cursor = new LinkedList<>();
        TACNode tacNode;

        //Stack<Tree<Integer, BNFSymbol>> cursor = new Stack<Tree<Integer, BNFSymbol>>();
        cursor.addAll(tree.getLeaves());

        // Tree<Integer, BNFSymbol> tree = queue.poll();

        //Collections.reverse(tree.getLeaves());
        //cursor.addAll(tree.getLeaves());

        // Auxiliary
        LinkedList<Tree<Integer, BNFSymbol>> newInstructions;

        while (!cursor.isEmpty()) {
            Tree<Integer, BNFSymbol> currentElement = cursor.peek();
            switch (currentElement.getData().getName()) {
                case "variable_declaration_local","variable_declaration_global","variable_declaration_aux" -> {
                    BNFSymbol currentElementId = currentElement.getLeaves().get(4).getData(); // Getting the variable symbol.
                    Tree<Integer, SymbolTable> currentSymbolTable = symbolTable.getTree(currentElementId.getScopeId());

                    // Update address of variable in symbols table.
                    symbolTable.getTree(currentElementId.getScopeId()).getData().getTable().get(currentElementId.getValue()).setAddressOffset(addressCounter);

                    // Check for g / ml / temp.
                    BNFSymbol value = currentElement.getLeaves().get(1).getData();
                    if (value.getValue().equals("hot")) {
                        tacNode = new TACNode(1, "$t0","1",null, "load_literal");
                    } else if (value.getValue().equals("cold")) {
                        tacNode = new TACNode(1, "$t0","0",null, "load_literal");
                    } else {
                        // Rounding the value.
                        tacNode = new TACNode(1, "$t0",String.valueOf(Math.round(Double.parseDouble(value.getValue()))),null, "load_literal");
                    }
                    tacRules.add(tacNode);
                    //tacStack.push(tacNode);

                    // Grams (int), mililiters (float), and temperature (bool), all are 4 bytes.
                    tacNode = new TACNode(2, Integer.toString(addressCounter),"$t0",null, "assignment");
                    tacRules.add(tacNode);
                    addressCounter += 4;

                    cursor.poll();

                    // Check for special variable_declaration_aux case nested inside variable_declaration.
                    if (currentElement.getLeaves().get(currentElement.getLeaves().size()-1).getData().getName().equals("variable_declaration_aux")) {
                        newInstructions = new LinkedList<>();
                        newInstructions.addAll(currentElement.getLeaves());
                        newInstructions.addAll(cursor);
                        cursor = newInstructions;
                        //cursor.add(currentElement.getLeaves().get(currentElement.getLeaves().size()-1));
                    }
                }
                case "variable_assignation" -> {
                    // Special case. We flip the order of the leaves in the queue.
                    cursor.poll();
                    newInstructions = new LinkedList<>();
                    Collections.reverse(currentElement.getLeaves());
                    newInstructions.addAll(currentElement.getLeaves());
                    newInstructions.addAll(cursor);
                    cursor = newInstructions;
                }
                case "equals_token" -> {
                    Collections.reverse(currentElement.getParent().getLeaves());

                    BNFSymbol symb = currentElement.getParent().getLeaves().get(currentElement.getParent().getLeaves().size() - 1).getData();
                    if (!symb.getName().equals("addition") && !symb.getName().equals("subtraction") && !symb.getName().equals("multiplication") && !symb.getName().equals("division")) {
                        // In case it's a simple assignation.
                        BNFSymbol opr = currentElement.getParent().getLeaves().get(2).getData();
                        if (!opr.getName().equals("id")) {
                            tacNode = new TACNode(1, "$t0",String.valueOf(Math.round(Double.parseDouble(opr.getValue()))),null, "load_literal");
                        } else {
                            //variableOffset = findVariableOnExternalScope();
                            int variableOffset = findVariableOnExternalScope(currentElement.getParent().getLeaves().get(2)).getAddressOffset();
                            tacNode = new TACNode(1, "$t0",Integer.toString(variableOffset),null, "load_word");
                        }
                        tacRules.add(tacNode);

                        // Moving $t0 to the offset variable.
                    }

                    int variableOffset = findVariableOnExternalScope(currentElement.getParent().getLeaves().get(0)).getAddressOffset();

                    tacNode = new TACNode(4, Integer.toString(variableOffset),"$t0",null, "assignment");
                    tacRules.add(tacNode);


                    cursor.poll();
                }
                case "addition", "subtraction", "multiplication", "division" -> {
                    BNFSymbol opr1 = currentElement.getLeaves().get(0).getData();
                    BNFSymbol opr2 = currentElement.getLeaves().get(2).getData();

                    int variableOffset = 0;

                    if (!opr1.getName().equals("id")) {
                        tacNode = new TACNode(1, "$t0",String.valueOf(Math.round(Double.parseDouble(opr1.getValue()))),null, "load_literal");
                    } else {
                        //variableOffset = findVariableOnExternalScope();
                        variableOffset = findVariableOnExternalScope(currentElement.getLeaves().get(0)).getAddressOffset();
                        tacNode = new TACNode(1, "$t0",Integer.toString(variableOffset),null, "load_word");
                    }
                    tacRules.add(tacNode);

                    if (!opr2.getName().equals("id")) {
                        tacNode = new TACNode(1, "$t1",String.valueOf(Math.round(Double.parseDouble(opr2.getValue()))),null, "load_literal");
                    } else {
                        variableOffset = findVariableOnExternalScope(currentElement.getLeaves().get(2)).getAddressOffset();
                        tacNode = new TACNode(1, "$t1",Integer.toString(variableOffset),null, "load_word");
                    }
                    tacRules.add(tacNode);

                    if (currentElement.getData().getName().equals("addition")) {
                        tacNode = new TACNode(3, "$t0","$t0","$t1", "sum");
                    } else if (currentElement.getData().getName().equals("subtraction")) {
                        tacNode = new TACNode(9, "$t0","$t0","$t1", "subtraction");

                    } else if (currentElement.getData().getName().equals("multiplication")) {
                        tacNode = new TACNode(9, "$t0","$t0","$t1", "multiply");

                    } else if (currentElement.getData().getName().equals("division")) {
                        tacNode = new TACNode(9, "$t0","$t0","$t1", "division");
                    }

                    tacRules.add(tacNode);

                    cursor.poll();
                }
                case "if_clause" -> {
                    Tree<Integer, BNFSymbol> element = currentElement.getLeaves().get(3).getLeaves().get(currentElement.getLeaves().get(3).getLeaves().size() - 1);

                    generateBranchCondition(element);

                    // Checking if there are logical operators and handling accordingly.
                    if (currentElement.getLeaves().size() > 4 && currentElement.getLeaves().get(4).getData().getName().equals("condition_aux")) {
                        element = currentElement.getLeaves().get(4);
                        String operatorType = element.getLeaves().get(0).getData().getValue();
                        element = element.getLeaves().get(element.getLeaves().size() - 1).getLeaves().get(1);
                        switch (operatorType) {
                            case "and" -> {
                                labelCounter--;
                                generateBranchCondition(element);
                            }
                            case "or" -> {
                                // We introduce the jump in case it is already correct.
                                tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),null, null, "jump");
                                tacRules.add(tacNode);

                                // We introduce the label of the last condition.
                                int auxLabelCounter = labelCounter - 1;
                                tacNode = new TACNode(9, Integer.toString(auxLabelCounter),null, null, "label");
                                tacRules.add(tacNode);

                                // Remove label.
                                labelsArray.remove(labelsArray.size() - 1);

                                // Include the new label.
                                labelsArray.add("L"+labelCounter);
                                labelCounter++;

                                generateBranchConditionOr(element);

                                auxLabelCounter = labelCounter - 2;
                                tacNode = new TACNode(9, "L"+Integer.toString(auxLabelCounter),null, null, "jump");
                                tacRules.add(tacNode);

                                // Placing the labels in case the conditions match.
                                tacNode = new TACNode(9, labelsArray.get(0).substring(1),null, null, "label");
                                tacRules.add(tacNode);

                                // Removing it.
                                labelsArray.remove(0);

                                tacNode = new TACNode(9, labelsArray.get(0).substring(1),null, null, "label");
                                tacRules.add(tacNode);

                                labelsArray.remove(0);
                            }
                        }
                    }

                    cursor.poll();
                }
                case "endif_token" -> {
                    if (!otherwiseEndIfControlFlag) {
                        //while (labelsArray.size() > 0) {
                            int lastIndex = labelsArray.size() - 1;
                            tacNode = new TACNode(9, labelsArray.get(lastIndex),null, null, "endif");
                            labelsArray.remove(lastIndex);
                            tacRules.add(tacNode);
                        //}
                    } else {
                        // Add after otherwise flag.
                        tacNode = new TACNode(9, Integer.toString(labelCounter),null, null, "label");
                        tacRules.add(tacNode);
                        otherwiseEndIfControlFlag = false;

                        labelCounter++;
                    }


                    cursor.poll();
                }
                case "otherwise_token" -> {
                    // Jump to after otherwise.
                    int auxLabelCounter = labelCounter;
                    tacNode = new TACNode(9, "L"+Integer.toString(auxLabelCounter),null, null, "jump");
                    tacRules.add(tacNode);

                    // Create flow control. Add label.
                    tacNode = new TACNode(9, labelsArray.get(labelsArray.size()-1).substring(1),null, null, "label");
                    tacRules.add(tacNode);

                    labelsArray.remove(labelsArray.size()-1);

                    //tacNode = new TACNode(9, "L"+Integer.toString(auxLabelCounter),null, null, "jump");
                    //tacRules.add(tacNode);

                    otherwiseEndIfControlFlag = true;
                    cursor.poll();
                }
                case "for_token" -> {
                    ArrayList<Tree<Integer, BNFSymbol>> element = currentElement.getParent().getLeaves();
                    Tree<Integer, BNFSymbol> forVariable = element.get(5);

                    int offset = findVariableOnExternalScope(element.get(5)).getAddressOffset();

                    // Load literal to register.
                    tacNode = new TACNode(9, "$t0",String.valueOf(Math.round(Double.parseDouble(element.get(8).getData().getValue()))), null, "load_literal");
                    tacRules.add(tacNode);

                    // Store register to variable offset.
                    tacNode = new TACNode(9, Integer.toString(offset), "$t0", null, "assignment");
                    tacRules.add(tacNode);

                    // Create flow control. Add label.
                    tacNode = new TACNode(9, Integer.toString(labelCounter), null, null, "label");
                    tacRules.add(tacNode);

                    if (element.get(3).getData().getName().equals("id")) {
                        // Limit is ID.
                        // Find offset in symbols table.
                        int offset2 = findVariableOnExternalScope(element.get(3)).getAddressOffset();
                        tacNode = new TACNode(9, "$t5", Integer.toString(offset2), null, "assignmentTwo");
                        tacRules.add(tacNode);
                    } else {
                        // Limit is LITERAL.
                        // Storing for loop limit to register.
                        tacNode = new TACNode(9, "$t5",String.valueOf(Math.round(Double.parseDouble(element.get(3).getData().getValue()))), null, "load_literal");
                        tacRules.add(tacNode);
                    }

                    labelsArray.add("L"+labelCounter);
                    labelCounter++; // Increase label for control flow.


                    tacNode = new TACNode(9, "$t4",Integer.toString(offset), null, "load_word");
                    tacRules.add(tacNode);

                    if (element.get(2).getData().getValue().equals("entirely")) {
                        // Branch on greater than.
                        tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),"$t5", "$t4", "branch_greater_than");
                        tacRules.add(tacNode);
                    } else {
                        // Branch on greater than.
                        tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),"$t5", "$t4", "branch_more_equal");
                        tacRules.add(tacNode);
                    }


                    labelsArray.add("L"+labelCounter);
                    labelCounter++; // Increase label for control flow.

                    cursor.poll();
                }
                case "stop_oven_token" -> {
                    // Label control.
                    int jumpIndex = labelsArray.size() - 2;
                    int auxLabelCounter = labelCounter - 2;

                    tacNode = new TACNode(9, labelsArray.get(jumpIndex),null, null, "jump");
                    tacRules.add(tacNode);
                    labelsArray.remove(jumpIndex);

                    int lastIndex = labelsArray.size() - 1;

                    // Find the index of 'L'
                    int index = labelsArray.get(lastIndex).indexOf("L");

                    // Extract the substring after 'L'
                    String label = labelsArray.get(lastIndex).substring(index + 1);

                    // Label post-for control.
                    tacNode = new TACNode(9, label,null, null, "label");
                    tacRules.add(tacNode);
                    labelsArray.remove(lastIndex);

                    //labelCounter++;

                    cursor.poll();
                }
                case "function_declaration#" -> {
                    argumentCount = 0;
                    ArrayList<Tree<Integer, BNFSymbol>> element = currentElement.getLeaves();

                    currentFunctionName = element.get(1).getData().getValue();

                    tacNode = new TACNode(9, element.get(1).getData().getValue(),null, null, "function_label");
                    tacRules.add(tacNode);

                    // Storing $ra into $s0
                    raArray.add(Integer.toString(addressCounter)+ "($sp)");
                    tacNode = new TACNode(9, raArray.get(raArray.size()-1),null, null, "backup_ra");
                    tacRules.add(tacNode);

                    addressCounter+=4;

                    cursor.poll();
                    newInstructions = new LinkedList<>();
                    newInstructions.addAll(currentElement.getLeaves());
                    newInstructions.addAll(cursor);
                    cursor = newInstructions;
                }
                case "turn_page_token" -> {
                    tacNode = new TACNode(9, null,null, null, "return_ra");
                    tacRules.add(tacNode);

                    cursor.poll();
                }
                case "id" -> {
                    if (currentElement.getParent().getData().getName().equals("parameters") || currentElement.getParent().getData().getName().equals("parameters#")) {
                        ArrayList<Tree<Integer, BNFSymbol>> element = currentElement.getParent().getLeaves();

                        //symbolTableAux.getLeaves().add(0, currentElement.getParent());
                        String parameterName = element.get(2).getData().getValue();
                        String parameterType = currentElement.getParent().getLeaves().get(0).getData().getValue();
                        int parameterScopeId = currentElement.getData().getScopeId();

                        symbolTable.getTree(parameterScopeId).getData().get(parameterName).setAddressOffset(addressCounter);

                        // Adjust address Counter.
                        if (parameterType.equals("g") || parameterType.equals("temp") || parameterType.equals("ml")) {
                            addressCounter += 4;
                        }

                        tacNode = new TACNode(1, null,Integer.toString(argumentCount++), Integer.toString(symbolTable.getTree(parameterScopeId).getData().get(parameterName).getAddressOffset()), "parameter_to_offset");
                        tacRules.add(tacNode);

                    } else if (currentElement.getParent().getData().getName().equals("return_function")) {
                        // Assign value in offset of current element to register $v0.
                        int offset = findVariableOnExternalScope(currentElement).getAddressOffset();
                        tacNode = new TACNode(9, "$v0", Integer.toString(offset), null, "load_word");
                        tacRules.add(tacNode);
                    }

                    cursor.poll();
                }
                case "variable_assignation_function", "function_call" -> {
                    // Flip the order of operations.
                    cursor.poll();
                    Collections.reverse((currentElement.getLeaves()));
                    newInstructions = new LinkedList<>();
                    newInstructions.addAll(currentElement.getLeaves());
                    newInstructions.addAll(cursor);
                    cursor = newInstructions;
                }
                case "put_token" -> {
                    if (!currentElement.getParent().getLeaves().get(0).getData().getName().equals("function_call")) {
                        // Store $ra at $s0 to keep accross function calls.
                        if (raArray.size() > 0) {
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "ra_to_sX");
                            tacRules.add(tacNode);
                        }

                        // If no arguments are passed.
                        tacNode = new TACNode(9, currentElement.getParent().getLeaves().get(0).getData().getValue(), null, null, "jump_function");
                        tacRules.add(tacNode);

                        if (raArray.size() > 0) {
                            // s0 to stack pointer
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "sX_to_sp");
                            tacRules.add(tacNode);

                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), null, null, "restore_ra");
                            tacRules.add(tacNode);

                            raArray.remove(raArray.size() - 1);
                        }
                    } else {
                        // If arguments are passed.

                        int parameterCount = 0;
                        // Passing them to $a registers based on variable offsets.
                        ArrayList<Frontend.Parser.Tree<Integer,BNFSymbol>> element = currentElement.getParent().getLeaves().get(0).getLeaves();

                        while (element.get(0).getData().getName().equals("arguments_function_call") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call#") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call")) {
                            if (element.get(0).getData().getName().equals("with_token")) {
                                int varOffset = findVariableOnExternalScope(element.get(1)).getAddressOffset();
                                tacNode = new TACNode(9, null, Integer.toString(parameterCount), Integer.toString(varOffset), "argument_load");
                                tacRules.add(tacNode);
                                parameterCount++;
                            }
                            if (element.get(element.size() - 1).getData().getName().equals("arguments_function_call#") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call")) {
                                element = element.get(element.size() - 1).getLeaves();
                            } else {
                                element = element.get(0).getLeaves();
                            }
                        }

                        if (element.get(0).getData().getName().equals("with_token")) {
                            int varOffset = findVariableOnExternalScope(element.get(1)).getAddressOffset();
                            tacNode = new TACNode(9, null, Integer.toString(parameterCount), Integer.toString(varOffset), "argument_load");
                            tacRules.add(tacNode);
                            parameterCount++;
                        }

                        if (raArray.size() > 0) {
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "ra_to_sX");
                            tacRules.add(tacNode);
                        }

                        tacNode = new TACNode(9, currentElement.getParent().getLeaves().get(0).getLeaves().get(1).getData().getValue(), null, null, "jump_function");
                        tacRules.add(tacNode);

                        if (raArray.size() > 0) {
                            // s0 to stack pointer
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "sX_to_sp");
                            tacRules.add(tacNode);

                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), null, null, "restore_ra");
                            tacRules.add(tacNode);

                            raArray.remove(raArray.size() - 1);
                        }
                    }


                    tacNode = new TACNode(9, null, null, Integer.toString(findVariableOnExternalScope(currentElement.getParent().getLeaves().get(3)).getAddressOffset()), "return_assignment");
                    tacRules.add(tacNode);

                    functionCallsCount++;

                    cursor.poll();
                }
                case "set_token" -> {
                    // System.out.println("X");
                    cursor.poll();
                }
                case "serving_statement" -> {
                    tacNode = new TACNode(1, null,null,null, "main");
                    tacRules.add(tacNode);
                    cursor.poll();
                }
                case "step_statement" -> {
                    tacNode = new TACNode(1, null,null,null, "jump_main");
                    tacRules.add(tacNode);
                    cursor.poll();
                }
                case "see_function" -> {
                    // Checking if it has parameters.
                    if (!currentElement.getLeaves().get(1).getData().getName().equals("function_call")) {
                        // Store $ra at $s0 to keep accross function calls.
                        if (raArray.size() > 0) {
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "ra_to_sX");
                            tacRules.add(tacNode);
                        }

                        tacNode = new TACNode(9, currentElement.getLeaves().get(1).getData().getValue(), null, null, "jump_function");
                        tacRules.add(tacNode);

                        // Restoring $ra
                        // int auxAddressCounter = addressCounter - (4 * (addressCounter % functionCallsCount));
                        if (raArray.size() > 0) {
                            // s0 to stack pointer
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "sX_to_sp");
                            tacRules.add(tacNode);

                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), null, null, "restore_ra");
                            tacRules.add(tacNode);

                            raArray.remove(raArray.size() - 1);
                        }
                        //addressCounter -= 4;
                    } else {
                        // If it has parameters, we must know how many and pass them to $a0-$a3.
                        // Finding number of parameters.
                        int parameterCount = 0;
                        // Passing them to $a registers based on variable offsets.
                        ArrayList<Frontend.Parser.Tree<Integer,BNFSymbol>> element = currentElement.getLeaves().get(1).getLeaves();

                        while (element.get(element.size() - 1).getData().getName().equals("arguments_function_call") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call#")) {
                            if (element.get(0).getData().getName().equals("with_token")) {
                                int varOffset = findVariableOnExternalScope(element.get(1)).getAddressOffset();
                                tacNode = new TACNode(9, null, Integer.toString(parameterCount), Integer.toString(varOffset), "argument_load");
                                tacRules.add(tacNode);
                                parameterCount++;
                            }
                            element = element.get(element.size() - 1).getLeaves();
                        }

                        if (element.get(0).getData().getName().equals("with_token")) {
                            int varOffset = findVariableOnExternalScope(element.get(1)).getAddressOffset();
                            tacNode = new TACNode(9, null, Integer.toString(parameterCount), Integer.toString(varOffset), "argument_load");
                            tacRules.add(tacNode);
                            parameterCount++;
                        }

                        // Store $ra at $s0 to keep accross function calls.
                        if (raArray.size() > 0) {
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "ra_to_sX");
                            tacRules.add(tacNode);
                        }

                        // Then jump to function.
                        tacNode = new TACNode(9, currentElement.getLeaves().get(1).getLeaves().get(0).getData().getValue(), null, null, "jump_function");
                        tacRules.add(tacNode);

                        if (raArray.size() > 0) {
                            // s0 to stack pointer
                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), Integer.toString(functionCallsCount), null, "sX_to_sp");
                            tacRules.add(tacNode);

                            tacNode = new TACNode(9, raArray.get(raArray.size() - 1), null, null, "restore_ra");
                            tacRules.add(tacNode);

                            raArray.remove(raArray.size() - 1);
                        }
                    }
                    functionCallsCount++;
                    cursor.poll();
                }
                default -> {
                    cursor.poll();
                    newInstructions = new LinkedList<>();
                    newInstructions.addAll(currentElement.getLeaves());
                    newInstructions.addAll(cursor);
                    cursor = newInstructions;
                    //cursor.addAll(currentElement.getLeaves());
                }
            }
        }

        return tacRules;
    }

    private void generateBranchCondition(Tree<Integer, BNFSymbol> element) {
        String comparingToValue;
        TACNode tacNode;
        if (element.getLeaves().get(0).getData().getName().equals("id")) {
            comparingToValue = Integer.toString(findVariableOnExternalScope(element.getLeaves().get(0)).getAddressOffset());
        } else {
            comparingToValue = element.getLeaves().get(0).getData().getValue();
            if (comparingToValue.equals("hot")) {
                comparingToValue = "1";
            } else if (comparingToValue.equals("cold")) {
                comparingToValue = "0";
            }
        }

        int variableOffset = findVariableOnExternalScope(element.getLeaves().get(3)).getAddressOffset();

        tacNode = new TACNode(9, "$t0",String.valueOf(Math.round(Double.parseDouble(comparingToValue))), null, "load_literal");
        tacRules.add(tacNode);

        tacNode = new TACNode(9, "$t5",Integer.toString(variableOffset), null, "load_word");
        tacRules.add(tacNode);

        if (element.getParent().getLeaves().get(0).getData().getName().equals("exactly_token")) {
            tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),"$t0", "$t5", "branch_not_equal");
        } else if (element.getParent().getLeaves().get(0).getLeaves().get(0).getData().getValue().equals("more")){
            tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),"$t5", "$t0", "branch_less_equal");
        } else if (element.getParent().getLeaves().get(0).getLeaves().get(0).getData().getValue().equals("less")){
            tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),"$t0", "$t5", "branch_more_equal");
        } else if (element.getParent().getLeaves().get(0).getLeaves().get(0).getData().getValue().equals("morequal")){
            tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),"$t0", "$t5", "branch_less_than");
        } else if (element.getParent().getLeaves().get(0).getLeaves().get(0).getData().getValue().equals("lessequal")){
            tacNode = new TACNode(9, "L"+Integer.toString(labelCounter),"$t0", "$t5", "branch_greater_than");
        }
        tacRules.add(tacNode);

        labelsArray.add("L"+labelCounter); // Keeping track of the current labels.
        labelCounter++;
    }

    private void generateBranchConditionOr(Tree<Integer, BNFSymbol> element) {
        String comparingToValue;
        TACNode tacNode;
        if (element.getLeaves().get(0).getData().getName().equals("id")) {
            comparingToValue = Integer.toString(findVariableOnExternalScope(element.getLeaves().get(0)).getAddressOffset());
        } else {
            comparingToValue = element.getLeaves().get(0).getData().getValue();
        }

        int variableOffset = findVariableOnExternalScope(element.getLeaves().get(3)).getAddressOffset();

        tacNode = new TACNode(9, "$t0",String.valueOf(Math.round(Double.parseDouble(comparingToValue))), null, "load_literal");
        tacRules.add(tacNode);

        tacNode = new TACNode(9, "$t5",Integer.toString(variableOffset), null, "load_word");
        tacRules.add(tacNode);

        if (element.getParent().getLeaves().get(0).getData().getName().equals("exactly_token")) {
            tacNode = new TACNode(9, "L"+Integer.toString(++labelCounter),"$t0", "$t5", "branch_not_equal");
        } else if (element.getParent().getLeaves().get(0).getLeaves().get(0).getData().getValue().equals("more")){
            tacNode = new TACNode(9, "L"+Integer.toString(++labelCounter),"$t5", "$t0", "branch_less_than");
        } else {
            tacNode = new TACNode(9, "L"+Integer.toString(++labelCounter),"$t0", "$t5", "branch_greater_than");
        }
        tacRules.add(tacNode);

        labelsArray.add("L"+(--labelCounter)); // Keeping track of the current labels.
        labelsArray.add("L"+(++labelCounter)); // Keeping track of the current labels.
        labelCounter++;
    }

    private int findFunctionScopeOnExternalScope(Tree<Integer, BNFSymbol> operands){
        int currentScope = 0;
        String varName = operands.getData().getValue();
        while (symbolTable.getTree(currentScope).getData().get(varName) == null && (symbolTable.getTree(currentScope).getParent() == null || symbolTable.getTree(currentScope).getParent().getHead() == 0)) {
            currentScope += 1;
            if (currentScope == symbolTable.getLeaves().size()) {
                break;
            }
            if (symbolTable.getTree(currentScope).getData().get(varName) != null && !symbolTable.getTree(currentScope).getData().get(varName).getInfoLabel().equals("function")) {
                currentScope += 1;
            }
        }
        return currentScope;
    }

    private Symbol findVariableOnExternalScope(Tree<Integer, BNFSymbol> operands){
        int currentScope = operands.getData().getScopeId();
        String varName = operands.getData().getValue();
        while (symbolTable.getTree(currentScope).getData().get(varName) == null && symbolTable.getTree(currentScope).getParent() != null) {
            //currentScope = operands.getParent().getData().getScopeId();
            currentScope = symbolTable.getTree(currentScope).getParent().getHead();
            if (currentScope == -1) {
                currentScope = 0;
                break;
            }
        }

        if (symbolTable.getTree(currentScope).getData().get(varName) == null) {
            throw new SemanticException("Exception While Performing Frontend. Ingredient not declared!");
        }

        return symbolTable.getTree(currentScope).getData().get(varName);
    }

    private Symbol findFunctionOnExternalScope(Tree<Integer, BNFSymbol> operands){
        int currentScope = 0;
        String varName = operands.getData().getValue();
        while (symbolTable.getTree(currentScope).getData().get(varName) == null && (symbolTable.getTree(currentScope).getParent() == null || symbolTable.getTree(currentScope).getParent().getHead() == 0)) {
            currentScope += 1;
            if (currentScope == symbolTable.getLeaves().size()) {
                break;
            }
            if (symbolTable.getTree(currentScope).getData().get(varName) != null && !symbolTable.getTree(currentScope).getData().get(varName).getInfoLabel().equals("function")) {
                currentScope += 1;
            }
        }
        return symbolTable.getTree(currentScope).getData().get(varName);
    }

    /*
    public Map<Integer, List<BNFSymbol>> generateTacMap() {
        Map<Integer, List<BNFSymbol>> TacMap = new HashMap<>();
        Integer idx = 0;

        Stack<Tree<Integer, BNFSymbol>> cursor = new Stack<Tree<Integer, BNFSymbol>>();
        cursor.addAll(tree.getLeaves());
        while (cursor.size() > 0) {
            List<BNFSymbol> tac = generateOneTac();
            TacMap.put(idx, tac);
        }

        return TacMap;
    }

    public List<BNFSymbol> generateOneTac() {

        List<BNFSymbol> tac = new ArrayList<>(4);

        return tac;
    }
     */

    public List<TACNode> getTAC() {
        return tacRules;
    }
}
