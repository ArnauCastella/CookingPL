package Frontend.Lexer;

import Frontend.CustomException.LexerException;
import Frontend.Grammar.BNFSymbol;
import Frontend.Parser.Tree;
import Frontend.Precompiler.Precompiler;
import Frontend.SymbolTable.SymbolTable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {

    public enum ScopeDeclarationType {
        Curly_bracket, //C style scope creation, using {}
        Indentation, //Python style scope creation, using \t or spaces
        None //No scope declaration possible
    }
    private final Pattern token_pattern;
    private final List<Map.Entry<String,Pattern>> dictionary;
    private final Precompiler precompiler; //High level language: source code

    private final Tree<Integer, SymbolTable> symbol_table;
    private BNFSymbol currentToken;
    private Matcher instruction_matcher;
    private boolean asReachedEOF;

    //The level of the current scope, 0 is global, 1 is a local scope, 2 is a local scope of a local scope, etc.
    private int current_scope_level = 0;

    private int symbol_table_unique_key = 0;

    private Tree<Integer, SymbolTable> parent_symbol_table;

    private ScopeDeclarationType scope_declaration_type;

    public Lexer(Precompiler precompiler, List<Map.Entry<String,Pattern>> dictionary, Boolean case_sensitive, Tree<Integer, SymbolTable> SymbolTable, ScopeDeclarationType scopeDeclarationType, String token_pattern) {
        this.precompiler = precompiler;
        this.dictionary = dictionary;
        this.symbol_table = SymbolTable;
        this.parent_symbol_table = symbol_table;
        this.scope_declaration_type = scopeDeclarationType;
        if (case_sensitive) {
            this.token_pattern = Pattern.compile(token_pattern);
        } else {
            this.token_pattern = Pattern.compile(token_pattern, Pattern.CASE_INSENSITIVE);
        }
    }
    public Lexer(Precompiler precompiler, List<Map.Entry<String,Pattern>> dictionary, Boolean case_sensitive, Tree<Integer, SymbolTable> SymbolTable, ScopeDeclarationType scopeDeclarationType) {
        this(precompiler, dictionary, case_sensitive, SymbolTable, scopeDeclarationType, "\".*?\"|[^\\s:()+]+|(\\(|\\)|:|\\+)|\\r\\n|\\n|\\t|(    )");
    }

    public boolean nextInstruction() { //Load the next instruction (line) in the lexer
        String currentInstruction;
        if (precompiler.hasNext()) {
            currentInstruction = precompiler.next();
            if (scope_declaration_type != ScopeDeclarationType.None)
                if (scope_declaration_type == ScopeDeclarationType.Indentation) {
                    //if (currentInstruction.startsWith("\t") || currentInstruction.startsWith("    ") || currentInstruction.matches("\\d+\\..*")) {
                        currentInstruction = updateScopeLevel2(currentInstruction);
                    //}
                } else throw new LexerException("This scope declaration type isn't supported yet !");

            instruction_matcher = token_pattern.matcher(currentInstruction);
            //System.out.println("[Debug] Current Frontend.Lexer Statement: " + currentInstruction);
            return true;
        } else if (!asReachedEOF) {
            asReachedEOF = true;
            return true;
        } else {
            instruction_matcher = null;
            return false;
        }
    }

    private String updateScopeLevel(String currentInstruction) {
        String stripedCurrentInstruction = currentInstruction;
        int instructionScopeLevel = 0;
        //Calculate the instruction scope level and get rid of the tokens corresponding to that as we don't need them anymore

        while (stripedCurrentInstruction.startsWith("\t") || stripedCurrentInstruction.startsWith("    ")) {
            stripedCurrentInstruction = stripedCurrentInstruction.replaceFirst("\t|    ", "");
            instructionScopeLevel++;
        }

        if (stripedCurrentInstruction.matches("\\d+\\..*")) {
            instructionScopeLevel++;
        }

        if (instructionScopeLevel != current_scope_level) { //The scope level changed since last instruction
            if (instructionScopeLevel == current_scope_level + 1) {
                //We need to create a new scope
                current_scope_level++;
                symbol_table_unique_key++;
                //Create a new symbol table for the new scope
                parent_symbol_table.addNode(symbol_table_unique_key, new SymbolTable(), null);
                //Replace the parent of the next scope with the new one we created
                parent_symbol_table = symbol_table.getTree(symbol_table_unique_key);
            } else if (instructionScopeLevel < current_scope_level) {
                //We need to move up in the symbol table as we left the scope
                while (instructionScopeLevel < current_scope_level) {
                    current_scope_level--;
                    parent_symbol_table = parent_symbol_table.getParent();
                }
            } else {
                throw new LexerException("Indentation error !");
            }
        }
        return stripedCurrentInstruction;
    }

    private String updateScopeLevel2(String currentInstruction) {
        String stripedCurrentInstruction = currentInstruction;
        //int instructionScopeLevel = 0;
        //Calculate the instruction scope level and get rid of the tokens corresponding to that as we don't need them anymore

        while (stripedCurrentInstruction.startsWith("\t") || stripedCurrentInstruction.startsWith("    ")) {
            stripedCurrentInstruction = stripedCurrentInstruction.replaceFirst("\t|    ", "");
            //instructionScopeLevel++;
        }

        String lowerCaseStripedCurrentInstruction = stripedCurrentInstruction.toLowerCase();

        // Big scope creation.
        if (lowerCaseStripedCurrentInstruction.matches("\\d+\\..*") || lowerCaseStripedCurrentInstruction.contains("serving")) {
            // We need to create a new scope.
            current_scope_level++;

            // Header control variable.
            symbol_table_unique_key++;

            // Create a new symbol table for the new scope.
            parent_symbol_table.getTree(0).addNode(symbol_table_unique_key, new SymbolTable(), null);
        }

        // Sub Scope Creation
        if (lowerCaseStripedCurrentInstruction.contains("bake for")
                || lowerCaseStripedCurrentInstruction.contains("if you have")
                || lowerCaseStripedCurrentInstruction.contains("otherwise")) {

            // Header control variable.
            symbol_table_unique_key++;

            // We need to create a new scope.
            current_scope_level++;

            int parent_Scope = current_scope_level - 1;

            // The parent of the new scope is the node with the current scope.
            parent_symbol_table.getTree(parent_Scope).addNode(symbol_table_unique_key, new SymbolTable(), null);
        }


        return stripedCurrentInstruction;
    }


    public boolean nextToken() { //Load and parse the next token
        if (instruction_matcher != null && instruction_matcher.find()) { //If we match any token in the instruction
            String token = instruction_matcher.group(); //Get the token
            //For each token in the dictionary
            for (Map.Entry<String, Pattern> terminal : dictionary) {
                //System.out.println(entry.getKey() + "/" + entry.getValue());
                Matcher token_matcher = terminal.getValue().matcher(token);
                if (token_matcher.find()) {
                    String matched = token_matcher.group();
                    currentToken = new BNFSymbol(terminal.getKey(), matched, true, BNFSymbol.SymbolType.Symbol);
                    if (scope_declaration_type != ScopeDeclarationType.None)
                        if (Objects.equals(currentToken.getName(), "id"))
                            // currentToken.setScopeId(parent_symbol_table.getHead());
                            currentToken.setScopeId(current_scope_level);
                    return true;
                }
            }
            throw new Frontend.CustomException.LexerException(token);
        } else if (asReachedEOF && currentToken.getType() != BNFSymbol.SymbolType.End) {
            currentToken = new BNFSymbol("$", null, true, BNFSymbol.SymbolType.End);
            return true;
        }
        return false; //EOF, return false, no token left
    }
    public BNFSymbol getCurrentToken() {
        return currentToken;
    }
}
