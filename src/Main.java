import Backend.ThreeAddressCode.MIPSAssembler;
import Backend.ThreeAddressCode.TACNode;
import Backend.ThreeAddressCode.ThreeAddressCode;
import Frontend.CustomException.LexerException;
import Frontend.Grammar.BNFParser;
import Frontend.Grammar.BNFSymbol;
import Frontend.Lexer.*;
import Frontend.Parser.*;
import Frontend.Precompiler.Precompiler;
import Frontend.Semantic.Semantic;
import Frontend.SymbolTable.SymbolTable;

import java.util.*;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        try {
            Boolean case_sensitive = false;
            //Tree containing the symbol tables for all the program scopes
            Tree<Integer, SymbolTable> symbolTable = new Tree<>(0, new SymbolTable());

            //Frontend.Grammar parsing
            BNFParser bnfParser = new BNFParser("grammar.bnf", case_sensitive);
            List<Map.Entry<String, Pattern>> dictionary = bnfParser.getDictionary();
            HashMap<String, HashMap<BNFSymbol, List<BNFSymbol>>> parsingTable = bnfParser.getParsingTable();

            //Code parsing
            //Todo: Get rid of the pre-compiler
            Precompiler precompiler = new Precompiler("example-source-code.cpl","\r\n|\n");
            precompiler.allowSingleLineComment("#");

            //Lexical analysis
            Lexer lexer = new Lexer(precompiler, dictionary, false, symbolTable, Lexer.ScopeDeclarationType.Indentation);

            System.out.println("OK INFO: Lexical Analysis Completed successfully.");
            System.out.println("PROGRESS INFO: Compilation 25% completed.");

            //Syntax analysis
            Parser parser = new Parser(lexer, parsingTable, symbolTable);
            Tree<Integer, BNFSymbol> tree = parser.createParseTree();
            parser.simplifyParseTree(tree);
            parser.removeUselessRules(tree);

            System.out.println("OK INFO: Syntax Analysis Completed successfully.");
            System.out.println("PROGRESS INFO: Compilation 50% completed.");

            //Frontend.Semantic analysis
            //Todo: Create the semantic analysis
            Semantic semantic = new Semantic(tree, symbolTable);
            semantic.analyse();

            System.out.println("OK INFO: Semantic Analysis Completed successfully.");
            System.out.println("PROGRESS INFO: Compilation 60% completed.");

            ThreeAddressCode threeAddressCode = new ThreeAddressCode(tree, symbolTable);
            threeAddressCode.generateTacMap();

            System.out.println("OK INFO: Three Address Code Generated successfully.");
            System.out.println("PROGRESS INFO: Compilation 80% completed.");

            // MIPS
            List<TACNode> tacListSample = new ArrayList<>();
            TACNode tacNode;

            List<TACNode> tacList = threeAddressCode.getTAC();
            MIPSAssembler mipsAssembler = new MIPSAssembler(threeAddressCode);
            mipsAssembler.generateMIPSFromTAC(tacList, symbolTable); // List<TACNode> tacList, Tree<Integer, SymbolTable> symbolTable

            System.out.println("OK INFO: MIPS Assembly Generated successfully.");
            System.out.println("INFO: Compilation 100% completed.");
        } catch (Exception e) {
            //Todo: improve exceptions by adding line and/or column number
            // Todo: try to recover from errors if possible
            if (e instanceof LexerException) {
                System.out.println(((LexerException) e).getValue());
            }
            throw new RuntimeException(e);
        }
    }
}