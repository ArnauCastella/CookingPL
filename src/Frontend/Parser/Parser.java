package Frontend.Parser;

import Frontend.CustomException.ParserException;
import Frontend.Grammar.BNFSymbol;
import Frontend.Lexer.*;
import Frontend.SymbolTable.SymbolTable;
import Frontend.SymbolTable.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public class Parser {
    private final Lexer lexer;
    private final HashMap<String, HashMap<BNFSymbol, List<BNFSymbol>>> parsingTable;
    private final Tree<Integer, SymbolTable> symbolTable;
    private final Stack<BNFSymbol> stack = new Stack<>();
    private final List<BNFSymbol> currentStatement = new ArrayList<BNFSymbol>();

    private boolean parametersCovered;

    public Parser(Lexer lexer, HashMap<String, HashMap<BNFSymbol, List<BNFSymbol>>> parsingTable, Tree<Integer, SymbolTable> symbolTable) {
        this.lexer = lexer;
        this.parsingTable = parsingTable;
        this.symbolTable = symbolTable;
        this.parametersCovered = false;
    }

    public boolean nextStatement() {
        currentStatement.clear();
        if (lexer.nextInstruction()) {
            while (lexer.nextToken()) {
                currentStatement.add(lexer.getCurrentToken());
            }
            return true;
        }
        return false;
    }

    private void addToSymbolTable(BNFSymbol token) {
        Tree<Integer, SymbolTable> tree = symbolTable.getTree(token.getScopeId());

        if (!tree.getData().contains(token.getValue())) {
            tree.getData().put(token.getValue(), new Symbol());
        } else { //Variable is already defined in scope !
            throw new ParserException("Variable defined twice in the same scope !");
        }
    }

    public void addVariableLabelToSymbolTable() {
        BNFSymbol name = currentStatement.get(4);

        if (name.getScopeId() != -1 && Objects.equals(name.getName(), "id")) {
            symbolTable.getTree(name.getScopeId()).getData().setLabel(name.getValue(), "variable");
        }
    }

    public void addFunctionLabelToSymbolTable() {
        BNFSymbol name = currentStatement.get(1);

        if (name.getScopeId() != -1 && Objects.equals(name.getName(), "id")) {
            symbolTable.getTree(name.getScopeId()).getData().setLabel(name.getValue(), "function");
        }
    }

    public void addParameterLabelToSymbolTable(BNFSymbol name) {
        if (name.getScopeId() != -1 && Objects.equals(name.getName(), "id")) {
            symbolTable.getTree(name.getScopeId()).getData().setLabel(name.getValue(), "parameter");
        }
    }

    public void addTypeToSymbolTable() {
        BNFSymbol type = currentStatement.get(2);
        BNFSymbol name = currentStatement.get(4);

        if (name.getScopeId() != -1 && Objects.equals(name.getName(), "id") && Objects.equals(type.getName(), "datatype")) {
            symbolTable.getTree(name.getScopeId()).getData().setType(name.getValue(), type.getValue());
        }
    }

    public void addTypeToSymbolTableReturningFunction(BNFSymbol type) {
        BNFSymbol name = currentStatement.get(1);

        if (name.getScopeId() != -1 && Objects.equals(name.getName(), "id") && Objects.equals(type.getName(), "datatype")) {
            symbolTable.getTree(name.getScopeId()).getData().setType(name.getValue(), type.getValue());
        }
    }

    public void addTypeToSymbolTableNoReturningFunction() {
        BNFSymbol name = currentStatement.get(1);
        symbolTable.getTree(name.getScopeId()).getData().setType(name.getValue(), "void");
    }


    public void addTypeToSymbolTableReturnFunctionParameter(int positionOfParameter) {
        BNFSymbol name = currentStatement.get(positionOfParameter);
        positionOfParameter-=2;
        BNFSymbol type = currentStatement.get(positionOfParameter);


        if (name.getScopeId() != -1 && Objects.equals(name.getName(), "id") && Objects.equals(type.getName(), "datatype")) {
            symbolTable.getTree(name.getScopeId()).getData().setType(name.getValue(), type.getValue());
        }
    }

    public Tree<Integer, BNFSymbol> createParseTree() {

        Integer uniqueKey = -1;

        Tree<Integer, BNFSymbol> tree = null;
        Tree<Integer, BNFSymbol> parentTree = null;

        while(nextStatement()) {

            for (BNFSymbol token : currentStatement) {
                //System.out.println(token.getName());
                HashMap<BNFSymbol, List<BNFSymbol>> parsingTableRow;

                while (true) {

                    if (token.getType() == BNFSymbol.SymbolType.End && stack.peek().getType() == BNFSymbol.SymbolType.End) {
                        break;
                    }

                    if (parentTree != null && parentTree.getNumberOfLeaves() == parentTree.getMaxLeavesNb()) {
                        parentTree = parentTree.getParent();
                        continue;
                    }

                    if (stack.size() > 0) {
                        parsingTableRow = parsingTable.get(stack.peek().getName());
                        //Populate the symbol table with datatypes
                        //Todo: complete this for functions return type
                        if (Objects.equals(stack.peek().getName(), "variable_declaration_local") || Objects.equals(stack.peek().getName(), "variable_declaration_global")) {
                            if (!currentStatement.get(0).getName().equals("steps")) {
                                addToSymbolTable(currentStatement.get(4));
                                addTypeToSymbolTable();
                                addVariableLabelToSymbolTable();
                            }
                        } else if (Objects.equals(stack.peek().getName(), "variable_declaration_aux")) {
                            if (currentStatement.size() >= 4 && currentStatement.get(4).getName().equals("id")) {
                                addToSymbolTable(currentStatement.get(4));
                                addTypeToSymbolTable();
                                addVariableLabelToSymbolTable();
                            }
                        } else if (Objects.equals(stack.peek().getName(), "function_declaration")) {
                            if (currentStatement.get(0).getName().equals("num_dot")) {
                                parametersCovered = false;
                                addToSymbolTable(currentStatement.get(1));
                                // Checking if it's a returning function or a void.
                                if (currentStatement.size() > 2 && currentStatement.get(2).getValue().equals("prepares")) {
                                    addTypeToSymbolTableReturningFunction(currentStatement.get(3));
                                } else {
                                    addTypeToSymbolTableNoReturningFunction();
                                }
                                addFunctionLabelToSymbolTable();
                                //addFunctionLabelToSymbolTable();
                            }
                        } else if (Objects.equals(stack.peek().getName(), "parameters")) {
                            // Find all the parameters, skipping the function name.
                            if (!parametersCovered) {
                                for (int i = 2; i < currentStatement.size(); i++) {
                                    if (currentStatement.get(i).getName().equals("id")) {
                                        // If ID, add to symbols table.
                                        addToSymbolTable(currentStatement.get(i));
                                        addTypeToSymbolTableReturnFunctionParameter(i);
                                        addParameterLabelToSymbolTable(currentStatement.get(i));
                                    }
                                }
                                parametersCovered = true;
                            }
                            //addFunctionLabelToSymbolTable();
                        }
                    } else {
                        parsingTableRow = parsingTable.get("start");
                    }

                    if (parsingTableRow != null) { // non terminal
                        List<BNFSymbol> parsingTableRule;
                        try {
                            parsingTableRule = new ArrayList<>(parsingTableRow.entrySet().stream().filter(column -> Objects.equals(column.getKey().getName(), token.getName())).findFirst().get().getValue());
                        } catch (Exception e) {
                            throw new ParserException(String.format("No grammar rule found for token '%s'",token.getValue()), e);
                        }
                        Collections.reverse(parsingTableRule);

                        if (uniqueKey != -1) {
                            if (uniqueKey == 0) {
                                tree = new Tree<>(uniqueKey, stack.peek(), parsingTableRule.size());
                                parentTree = tree;
                            } else {
                                tree.addNode(parentTree.getHead(), uniqueKey, stack.peek(), parsingTableRule.size());
                                parentTree = tree.getTree(uniqueKey);
                            }
                            stack.pop();
                        }
                        uniqueKey++;
                        stack.addAll(parsingTableRule);
                    } else { // terminal
                        if (tree == null || (stack.peek().getType() != BNFSymbol.SymbolType.Epsilon && !Objects.equals(token.getName(), stack.peek().getName()))) {
                            throw new ParserException("There is a Syntax Error somewhere in your code. Error while creating parse tree.");
                        }
                        if (stack.peek().getType() != BNFSymbol.SymbolType.Epsilon) {
                            tree.addNode(parentTree.getHead(), uniqueKey, token,0);
                        } else {
                            tree.addNode(parentTree.getHead(), uniqueKey, stack.peek(),0);
                        }
                        parentTree = tree.getTree(uniqueKey).getParent();
                        uniqueKey++;

                        BNFSymbol lastPoppedValue = stack.pop();
                        if (lastPoppedValue.getType() != BNFSymbol.SymbolType.Epsilon) {
                            break;
                        }
                    }
                }
            }
        }
        return tree;
    }

    public void simplifyParseTree(Tree<Integer, BNFSymbol> tree) {
        Stack<Tree<Integer, BNFSymbol>> cursor = new Stack<Tree<Integer, BNFSymbol>>();
        cursor.addAll(tree.getLeaves());
        ArrayList<Integer> visitedBranches = new ArrayList<>();
        while (cursor.size() > 0) {
            Tree<Integer, BNFSymbol> currentElement = cursor.peek();
            if (currentElement.getData().getType() == BNFSymbol.SymbolType.Epsilon || currentElement.getNumberOfLeaves() == 0) {
                cursor.pop();
                if (!currentElement.getData().isTerminal()) {
                    currentElement.getParent().setLeaves(tree.getTree(currentElement.getParent().getHead()).getLeaves().stream().filter(t -> !Objects.equals(t.getHead(), currentElement.getHead())).collect(Collectors.toList()));
                } else {
                    visitedBranches.add(currentElement.getHead());
                }
            } else if (currentElement.getLeaves().stream().allMatch(leaf -> visitedBranches.contains(leaf.getHead()))) {
                cursor.pop();
                visitedBranches.add(currentElement.getHead());
                if (currentElement.getNumberOfLeaves() == 1) {
                    // Check position of child in the parent's list of children.
                    int position = currentElement.getParent().getLeaves().indexOf(currentElement);
                    List<Tree<Integer, BNFSymbol>> leaves = currentElement.getParent().getLeaves().stream().filter(t -> !Objects.equals(t.getHead(), currentElement.getHead())).collect(Collectors.toList());
                    // Add back to the right index previously found.
                    leaves.addAll(position, currentElement.getLeaves());
                    //leaves.addAll(currentElement.getLeaves());
                    currentElement.getParent().setLeaves(leaves);
                }
            } else {
                cursor.addAll(currentElement.getLeaves().stream().filter(leaf -> !visitedBranches.contains(leaf.getHead())).toList());
            }
        }
    }

    public void removeUselessRules(Tree<Integer, BNFSymbol> tree) {
        Stack<Tree<Integer, BNFSymbol>> cursor = new Stack<Tree<Integer, BNFSymbol>>();
        cursor.addAll(tree.getLeaves());
        ArrayList<Integer> visitedBranches = new ArrayList<>();

        while (cursor.size() > 0) {
            Tree<Integer, BNFSymbol> currentElement = cursor.peek();
            if (currentElement.getData().getName().matches("shadow_rule.*")) {
                cursor.pop();
                int position = currentElement.getParent().getLeaves().indexOf(currentElement);
                if (position != -1) {
                    List<Tree<Integer, BNFSymbol>> leaves = currentElement.getParent().getLeaves().stream().filter(t -> !Objects.equals(t.getHead(), currentElement.getHead())).collect(Collectors.toList());
                    leaves.addAll(position, currentElement.getLeaves());
                    currentElement.getParent().setLeaves(leaves);
                    cursor.addAll(currentElement.getParent().getLeaves().stream().filter(leaf -> !visitedBranches.contains(leaf.getHead())).toList());
                }
            } else {
                cursor.pop();
                if (currentElement.getLeaves().size() > 0) {
                    cursor.addAll(currentElement.getLeaves().stream().filter(leaf -> !visitedBranches.contains(leaf.getHead())).toList());
                    for (Tree<Integer, BNFSymbol> leaf : currentElement.getLeaves()) {
                        visitedBranches.add(leaf.getHead());
                    }
                } else {
                    visitedBranches.add(currentElement.getHead());
                }
            }
        }
    }
}
