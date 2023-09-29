package Frontend.Semantic;

import Frontend.CustomException.SemanticException;
import Frontend.Grammar.BNFSymbol;
import Frontend.Parser.Tree;
import Frontend.SymbolTable.*;

import java.util.*;

public class Semantic {
    private final Tree<Integer, BNFSymbol> tree;
    private final Tree<Integer, SymbolTable> SymbolTable;


    public Semantic(Tree<Integer, BNFSymbol> tree, Tree<Integer, SymbolTable> SymbolTable) {
        this.tree = tree;
        this.SymbolTable = SymbolTable;
    }

    public void analyse() {
        Stack<Tree<Integer, BNFSymbol>> cursor = new Stack<Tree<Integer, BNFSymbol>>();
        cursor.addAll(tree.getLeaves());
        while (cursor.size() > 0) {
            Tree<Integer, BNFSymbol> currentElement = cursor.peek();
            switch (currentElement.getData().getName()) {
                case "variable_declaration" -> {
                    List<Tree<Integer, BNFSymbol>> children = new ArrayList<>(currentElement.getLeaves()).stream().filter(c -> !c.getData().getName().matches("dash|datatype|assignment")).toList();
                    BNFSymbol value = children.stream().filter(c -> !Objects.equals(c.getData().getName(), "id")).findFirst().get().getData();
                    BNFSymbol id = children.stream().filter(c -> Objects.equals(c.getData().getName(), "id")).findFirst().get().getData();
                    Symbol symbol = SymbolTable.getTree(id.getScopeId()).getData().get(id.getValue());
                    switch (symbol.getType()) {
                        case "g" -> {
                            assert Objects.equals(value.getName(), "integer");
                        }
                        case "ml" -> {
                            assert Objects.equals(value.getName(), "float");
                        }
                        case "temp" -> {
                            assert Objects.equals(value.getName(), "true") || Objects.equals(value.getName(), "false");
                        }
                        default -> throw new SemanticException();
                    }
                    cursor.pop();
                }
                case "addition", "subtraction", "multiplication", "division", "modulo" -> {
                    String resultType = checkOperationOperand(currentElement);
                    switch (resultType) {
                        case "integer" -> {}
                        case "ml" -> {}
                        case "temp" -> {}
                        default -> throw new SemanticException("Exception While Performing Frontend. Arithmetic operation improperly defined!");
                    }
                    cursor.pop();
                }
                case "true", "false" -> {
                    ArrayList<Tree<Integer, BNFSymbol>> booleanAssignationOperands = currentElement.getParent().getLeaves();
                    String resultType = checkBooleanAssignationOperand(booleanAssignationOperands);
                    switch (resultType) {
                        case "true" -> {}
                        case "false" -> {}
                        default -> throw new SemanticException("Exception While Performing Frontend. Temperature assignation improperly defined!");
                    }
                    cursor.pop();
                }
                case "bake_token" -> {
                    ArrayList<Tree<Integer, BNFSymbol>> forLoopOperands = currentElement.getParent().getLeaves();
                    String resultType = checkForLoopOperands(forLoopOperands);
                    switch (resultType) {
                        case "valid" -> {}
                        default -> throw new SemanticException("Exception While Performing Frontend. Bake instruction improperly defined!");
                    }
                    cursor.pop();
                }
                case "put_token" -> {
                    ArrayList<Tree<Integer, BNFSymbol>> variableFunctionAssignationOperands = currentElement.getParent().getLeaves();
                    String resultType = checkForVariableAssignationFunctionOperands(variableFunctionAssignationOperands);
                    switch (resultType) {
                        case "valid" -> {}
                        default -> throw new SemanticException("Exception While Performing Frontend. Invalid assignation of types between ingredient and step!");
                    }
                    cursor.pop();
                }
                case "if_clause" -> {
                    ArrayList<Tree<Integer, BNFSymbol>> ifClauseOperands = currentElement.getLeaves().get(3).getLeaves().get(1).getLeaves();
                    String resultType = checkIfClauseOperands(ifClauseOperands);

                    String resultType2 = "valid";
                    if (currentElement.getLeaves().get(4) != null && currentElement.getLeaves().get(4).getData().getName().equals("condition_aux")) {
                        ArrayList<Tree<Integer, BNFSymbol>>  ifClauseOperandsExtra = currentElement.getLeaves().get(4).getLeaves().get(3).getLeaves().get(1).getLeaves();
                        resultType2 = checkIfClauseOperands(ifClauseOperandsExtra);
                    }

                    if (!resultType.equals("valid") || !resultType2.equals("valid")) {
                        throw new SemanticException("Exception While Performing Frontend. Invalid declaration of if statement!");
                    }

                    cursor.pop();
                }
                case "set_token" -> {
                    // Checking for simple functions or return functions.
                    if (currentElement.getParent().getData().getName().equals("return_function")) {
                        if (!currentElement.getParent().getLeaves().get(2).getData().getName().equals("arguments")) {
                            Tree<Integer, BNFSymbol> returnValue = currentElement.getParent().getLeaves().get(6);
                            Tree<Integer, BNFSymbol> function = currentElement.getParent().getParent().getLeaves().get(1);
                            String resultType = checkForReturnTypeFunction(returnValue, function);
                            switch (resultType) {
                                case "valid" -> {}
                                default -> throw new SemanticException("Exception While Performing Frontend. Invalid return value assigned to Step!");
                            }
                        } else {
                            Tree<Integer, BNFSymbol> returnValue = currentElement.getParent().getLeaves().get(7);
                            Tree<Integer, BNFSymbol> function = currentElement.getParent().getParent().getLeaves().get(1);
                            String resultType = checkForReturnTypeFunction(returnValue, function);
                            switch (resultType) {
                                case "valid" -> {}
                                default -> throw new SemanticException("Exception While Performing Frontend. Invalid return value assigned to Step!");
                            }
                        }

                    }
                    cursor.pop();
                }
                case "see_token" -> {
                    // Checking for simple functions or no return functions.
                    // Seing if it has parameters.
                    String label;
                    String type;
                    if (currentElement.getParent().getLeaves().get(1).getLeaves().size() > 0) {
                        label = findFunctionOnExternalScope(currentElement.getParent().getLeaves().get(1).getLeaves().get(0)).getInfoLabel();
                        type = findFunctionOnExternalScope(currentElement.getParent().getLeaves().get(1).getLeaves().get(0)).getType();
                        if (!label.equals("function") && !type.equals("void")) {
                            throw new SemanticException("Exception While Performing Frontend. Invalid call of non-void function!");
                        }
                        // Checking for accepted number of parameters.
                        int numberOfParameters = findNumberOfParameters(currentElement.getParent().getLeaves().get(1).getLeaves());
                        if (numberOfParameters > 4) {
                            throw new SemanticException("Exception While Performing Frontend. Too many parameters used for the the Step! A maximum of 4 is accepted.");
                        } else {
                            // Comparing if the number of parameters passed matches the number of arguments declared in the function.
                            int functionScope = findFunctionScopeOnExternalScope(currentElement.getParent().getLeaves().get(1).getLeaves().get(0));
                            int numberOfArguments = 0;
                            Hashtable<String, Symbol> table = SymbolTable.getTree(functionScope).getData().getTable();

                            int num_g_arg = 0, num_ml_arg = 0, num_temp_arg = 0;
                            for (Map.Entry<String, Symbol> entry : table.entrySet()) {
                                Symbol symbol = entry.getValue();
                                if (symbol.getInfoLabel().equals("parameter")) {
                                    numberOfArguments++;
                                    switch (symbol.getType()) {
                                        case "g" -> {
                                            num_g_arg++;
                                        }
                                        case "ml" -> {
                                            num_ml_arg++;
                                        }
                                        case "temp" -> {
                                            num_temp_arg++;
                                        }
                                    }
                                }
                            }
                            if (numberOfParameters != numberOfArguments) {
                                throw new SemanticException("Exception While Performing Frontend. The number of parameters passed does not equal the number of arguments received by the Step!");
                            } else {
                                // Checking that the types of parameters and arguments match.
                                int num_g = findNumberOfGrams(currentElement.getParent().getLeaves().get(1).getLeaves());
                                int num_ml = findNumberOfMililiters(currentElement.getParent().getLeaves().get(1).getLeaves());
                                int num_temp = findNumberOfTemperatures(currentElement.getParent().getLeaves().get(1).getLeaves());

                                if (num_g != num_g_arg || num_ml != num_ml_arg || num_temp != num_temp_arg) {
                                    throw new SemanticException("Exception While Performing Frontend. The data types of the parameters do not match with the datatypes of the arguments of a Step!");
                                }
                            }
                        }
                    } else {
                        label = findFunctionOnExternalScope(currentElement.getParent().getLeaves().get(1)).getInfoLabel();
                        type = findFunctionOnExternalScope(currentElement.getParent().getLeaves().get(1)).getType();
                        if (!label.equals("function") && !type.equals("void")) {
                            throw new SemanticException("Exception While Performing Frontend. Invalid call of non-void function!");
                        }
                    }
                    cursor.pop();
                }
                case "using_token" -> {
                    // Checking for more than 4 arguments passed.
                    int numberOfArguments = findNumberOfArguments(currentElement.getParent().getLeaves());
                    if (numberOfArguments > 4) {
                        throw new SemanticException("Exception While Performing Frontend. Too many arguments used in the Step! A maximum of 4 is accepted.");
                    }
                    cursor.pop();
                }
                default -> {
                    cursor.pop();
                    cursor.addAll(currentElement.getLeaves());
                }
            }
        }
    }

    private int findNumberOfArguments(ArrayList<Frontend.Parser.Tree<Integer,BNFSymbol>> element) {
        int count = 0;
        while (element.get(element.size() - 1).getData().getName().equals("parameters") || element.get(element.size() - 1).getData().getName().equals("parameters#")) {
            if (!element.get(element.size() - 1).getLeaves().get(0).getData().getName().equals("coma")) {
                count++;
            }
            element = element.get(element.size() - 1).getLeaves();
        }
        return count;
    }

    private int findNumberOfParameters(ArrayList<Frontend.Parser.Tree<Integer,BNFSymbol>> element) {
        int count = 0;
        while (element.get(element.size() - 1).getData().getName().equals("arguments_function_call") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call#")) {
            if (element.get(0).getData().getName().equals("with_token")) {
                count++;
            }
            element = element.get(element.size() - 1).getLeaves();
        }
        if (element.get(0).getData().getName().equals("with_token")) {
            count++;
        }
        return count;
    }

    private int findNumberOfGrams(ArrayList<Frontend.Parser.Tree<Integer,BNFSymbol>> element) {
        int count = 0;
        while (element.get(element.size() - 1).getData().getName().equals("arguments_function_call") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call#")) {
            if (element.get(0).getData().getName().equals("with_token")) {
                if( findVariableOnExternalScope(element.get(1)).getType().equals("g")) {
                    count++;
                }
            }
            element = element.get(element.size() - 1).getLeaves();
        }
        if (element.get(0).getData().getName().equals("with_token")) {
            if(findVariableOnExternalScope(element.get(1)).getType().equals("g")) {
                count++;
            }
        }
        return count;
    }

    private int findNumberOfMililiters(ArrayList<Frontend.Parser.Tree<Integer,BNFSymbol>> element) {
        int count = 0;
        while (element.get(element.size() - 1).getData().getName().equals("arguments_function_call") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call#")) {
            if (element.get(0).getData().getName().equals("with_token")) {
                if( findVariableOnExternalScope(element.get(1)).getType().equals("ml")) {
                    count++;
                }
            }
            element = element.get(element.size() - 1).getLeaves();
        }
        if (element.get(0).getData().getName().equals("with_token")) {
            if(findVariableOnExternalScope(element.get(1)).getType().equals("ml")) {
                count++;
            }
        }
        return count;
    }

    private int findNumberOfTemperatures(ArrayList<Frontend.Parser.Tree<Integer,BNFSymbol>> element) {
        int count = 0;
        while (element.get(element.size() - 1).getData().getName().equals("arguments_function_call") || element.get(element.size() - 1).getData().getName().equals("arguments_function_call#")) {
            if (element.get(0).getData().getName().equals("with_token")) {
                if(findVariableOnExternalScope(element.get(1)).getType().equals("temp")) {
                    count++;
                }
            }
            element = element.get(element.size() - 1).getLeaves();
        }
        if (element.get(0).getData().getName().equals("with_token")) {
            if(findVariableOnExternalScope(element.get(1)).getType().equals("temp")) {
                count++;
            }
        }
        return count;
    }

    private Symbol findVariableOnExternalScope(Tree<Integer, BNFSymbol> operands){
        int currentScope = operands.getData().getScopeId();
        String varName = operands.getData().getValue();
        while (SymbolTable.getTree(currentScope).getData().get(varName) == null && SymbolTable.getTree(currentScope).getParent() != null) {
            //currentScope = operands.getParent().getData().getScopeId();
            currentScope = SymbolTable.getTree(currentScope).getParent().getHead();
            if (currentScope == -1) {
                currentScope = 0;
                break;
            }
        }

        if (SymbolTable.getTree(currentScope).getData().get(varName) == null) {
            throw new SemanticException("Exception While Performing Frontend. Ingredient not declared!");
        }

        return SymbolTable.getTree(currentScope).getData().get(varName);
    }

    private Symbol findFunctionOnExternalScope(Tree<Integer, BNFSymbol> operands){
        int currentScope = 0;
        String varName = operands.getData().getValue();
        while (SymbolTable.getTree(currentScope).getData().get(varName) == null && (SymbolTable.getTree(currentScope).getParent() == null || SymbolTable.getTree(currentScope).getParent().getHead() == 0)) {
            currentScope += 1;
            if (currentScope == SymbolTable.getLeaves().size()) {
                break;
            }
            if (SymbolTable.getTree(currentScope).getData().get(varName) != null && !SymbolTable.getTree(currentScope).getData().get(varName).getInfoLabel().equals("function")) {
                currentScope += 1;
            }
        }
        return SymbolTable.getTree(currentScope).getData().get(varName);
    }

    private int findFunctionScopeOnExternalScope(Tree<Integer, BNFSymbol> operands){
        int currentScope = 0;
        String varName = operands.getData().getValue();
        while (SymbolTable.getTree(currentScope).getData().get(varName) == null && (SymbolTable.getTree(currentScope).getParent() == null || SymbolTable.getTree(currentScope).getParent().getHead() == 0)) {
            currentScope += 1;
            if (currentScope == SymbolTable.getLeaves().size()) {
                break;
            }
            if (SymbolTable.getTree(currentScope).getData().get(varName) != null && !SymbolTable.getTree(currentScope).getData().get(varName).getInfoLabel().equals("function")) {
                currentScope += 1;
            }
        }
        return currentScope;
    }

    private String checkForReturnTypeFunction(Tree<Integer, BNFSymbol> returningValue, Tree<Integer, BNFSymbol> function) {
        String returningValueType = returningValue.getData().getName();

        if (returningValueType.equals("id")) {
            returningValueType = findVariableOnExternalScope(returningValue).getType();
        }

        String functionType = findVariableOnExternalScope(function).getType();

        if (returningValueType.equals("g") && functionType.equals("g")) {
            return "valid";
        }
        if (returningValueType.equals("integer") && functionType.equals("g")) {
            return "valid";
        }
        if (returningValueType.equals("ml") && functionType.equals("ml")) {
            return "valid";
        }
        if (returningValueType.equals("float") && functionType.equals("ml")) {
            return "valid";
        }
        if (returningValueType.equals("temp") && functionType.equals("temp")) {
            return "valid";
        }
        if (returningValueType.equals("hot") && functionType.equals("temp")) {
            return "valid";
        }
        if (returningValueType.equals("true") && functionType.equals("temp")) {
            return "valid";
        }
        return "default";
    }

    private String checkForVariableAssignationFunctionOperands(ArrayList<Tree<Integer, BNFSymbol>> operands) {
        Symbol idTypeOpr1 = findVariableOnExternalScope(operands.get(2));
        if (idTypeOpr1 == null) {
            throw new SemanticException("Exception While Performing Frontend. Ingredient not declared in recipe!");
        }

        Symbol idTypeOpr2;
        if (operands.get(5).getData().getName().equals("function_call")) {
            idTypeOpr2 = findFunctionOnExternalScope(operands.get(5).getLeaves().get(0));
        } else {
            idTypeOpr2 = findFunctionOnExternalScope(operands.get(5));
        }

        if (idTypeOpr2 == null) {
            throw new SemanticException("Exception While Performing Frontend. Step not declared in recipe!");
        }
        if (idTypeOpr1.getType().equals(idTypeOpr2.getType())) {
            return "valid";
        }
        return "default";
    }

    private String checkIfClauseOperands(ArrayList<Tree<Integer, BNFSymbol>> operands) {
        String idTypeOpr1 = operands.get(0).getData().getName();
        if (operands.get(0).getData().getName().equals("id")) {
            idTypeOpr1 = findVariableOnExternalScope(operands.get(0)).getType();
            if (idTypeOpr1 == null) {
                throw new SemanticException("Exception While Performing Frontend. Ingredient is not defined!");
            }
        }

        String idTypeOpr2 = findVariableOnExternalScope(operands.get(3)).getType();
        if (idTypeOpr2 == null) {
            throw new SemanticException("Exception While Performing Frontend. Ingredient is not defined!");
        }

        if (idTypeOpr1.equals("integer") && idTypeOpr2.equals("g")) {
            return "valid";
        }
        if (idTypeOpr1.equals("g") && idTypeOpr2.equals("integer")) {
            return "valid";
        }
        if (idTypeOpr1.equals("float") && idTypeOpr2.equals("ml")) {
            return "valid";
        }
        if (idTypeOpr1.equals("ml") && idTypeOpr2.equals("float")) {
            return "valid";
        }
        if (idTypeOpr1.equals("temp") && idTypeOpr2.equals("true")) {
            return "valid";
        }
        if (idTypeOpr1.equals("true") && idTypeOpr2.equals("temp")) {
            return "valid";
        }
        if (idTypeOpr1.equals("temp") && idTypeOpr2.equals("false")) {
            return "valid";
        }
        if (idTypeOpr1.equals("false") && idTypeOpr2.equals("temp")) {
            return "valid";
        }
        return "default";
    }

    private String checkForLoopOperands(ArrayList<Tree<Integer, BNFSymbol>> operands) {
        // Find the type of id.
        String operand1 = operands.get(3).getData().getName();
        String operand2 = operands.get(5).getData().getName();
        String operand3 = operands.get(8).getData().getName();

        if (operand3.equals("integer") && operand2.equals("id")) {
            Symbol idTypeOpr2 = findVariableOnExternalScope(operands.get(5));
            if (idTypeOpr2 == null) {
                throw new SemanticException("Exception While Performing Frontend. Ingredient not declared in recipe!");
            }
            if (operand1.equals("id")) {
                // Find type of id1 and id2.
                Symbol idTypeOpr1 = findVariableOnExternalScope(operands.get(3));
                if (idTypeOpr1 == null) {
                    throw new SemanticException("Exception While Performing Frontend. Variable not defined!");
                }
                if (idTypeOpr1.getType().equals("g") && idTypeOpr2.getType().equals("g")) {
                    return "valid";
                } else {
                    return "default";
                }
            } else if (operand1.equals("integer") && idTypeOpr2.getType().equals("g")) {
                return "valid";
            } else {
                return "default";
            }
        }
        return "default";
    }

    private String checkBooleanAssignationOperand(ArrayList<Tree<Integer, BNFSymbol>> operands) {
        // Find the type of id.
        Symbol operand1 = findVariableOnExternalScope(operands.get(4));
        String operand2 = operands.get(1).getData().getName();
        if (operand1.getType().equals("temp") && (operand2.equals("true"))) {
            return "true";
        }

        if (operand1.getType().equals("temp") && (operand2.equals("false"))) {
            return "false";
        }
        return "default";
    }

    private String checkOperationOperand(Tree<Integer, BNFSymbol> node) {
        ArrayList<Tree<Integer, BNFSymbol>> assignationLeaves = node.getParent().getLeaves();

        Symbol idType = findVariableOnExternalScope(assignationLeaves.get(0));
        if (idType == null) {
            throw new SemanticException("Exception While Performing Frontend. Variable not defined!");
        }

        List<Tree<Integer, BNFSymbol>> children = new ArrayList<>(node.getLeaves()).stream().filter(c-> !c.getData().getName().matches("add|strain|mix|chop|mod|open_parenthesis|close_parenthesis")).toList();

        String operand1;
        String operand2;

        operand1 = switch (children.get(0).getData().getName()) {
            case "id" -> findVariableOnExternalScope(children.get(0)).getType();
            case "integer", "float" -> children.get(0).getData().getName();
            default -> checkOperationOperand(children.get(0));
        };

        operand2 = switch (children.get(1).getData().getName()) {
            case "id" -> findVariableOnExternalScope(children.get(1)).getType();
            case "integer", "float" -> children.get(1).getData().getName();
            default -> checkOperationOperand(children.get(1));
        };

        if ("g".matches(idType.getType()) && "integer".matches(operand1) && "integer".matches(operand2)) {
            return "integer";
        }
        if ("g".matches(idType.getType()) &&  "g".matches(operand1) && "integer".matches(operand2)) {
            // Find type of id in Symbols table.
            return "integer";
        }
        if ("g".matches(idType.getType()) && "integer".matches(operand1) && "g".matches(operand2)) {
            return "integer";
        }
        if ("g".matches(idType.getType()) && "g".matches(operand1) && "g".matches(operand2)) {
            return "integer";
        }
        if ("ml".matches(idType.getType()) && "float".matches(operand1) && "float".matches(operand2)) {
            return "ml";
        }
        if ("ml".matches(idType.getType()) && "float".matches(operand1) && "ml".matches(operand2)) {
            return "ml";
        }
        if ("ml".matches(idType.getType()) && "ml".matches(operand1) && "float".matches(operand2)) {
            return "ml";
        }
        if ("ml".matches(idType.getType()) && "ml".matches(operand1) && "ml".matches(operand2)) {
            return "ml";
        }
        return "default";
    }
}
