package Frontend.Grammar;

import Frontend.CustomException.BNFException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BNFParser {

    private final List<Map.Entry<String,String>> terminalMap = new ArrayList<>();
    private final List<Map.Entry<String, Pattern>> dictionary;
    private final List<Map.Entry<String,String>> nonTerminalMap = new ArrayList<>();
    //Todo: REMOVE THE STARTING STATEMENT FROM THERE, AND ADD IT AS A PROPER STATIC PRODUCTION RULE
    // S ::= starting $
    private final AbstractMap.SimpleEntry<String, HashMap<String, List<BNFSymbol>>> productionRules;
    private final String namePattern = "<.*?>";
    private final String stringPattern = "(?<!\\\\)\".*?(?<!\\\\)\"";

    //Used to store, for each rule, its parent(s) and position in the corresponding production
    private final HashMap<String, HashMap<String, List<Integer>>> productionParentRules = new HashMap<>();

    private final List<String> nullableProductionRules = new ArrayList<>();
    //String = name of the rule
    //BNFSymbol = symbol producing the firsts (used to create the parsing table properly)
    //List<BNFSymbol> = list of all the firsts symbols
    private final Map<String, List<BNFSymbol>> firsts = new HashMap<>();
    private final Map<String, List<BNFSymbol>> follows = new HashMap<>();
    private final Stack<String> recursionStack = new Stack<>(); //Used to track and prevent endless recursion in recursive functions
    private final HashMap<String, HashMap<BNFSymbol, List<BNFSymbol>>> parsingTable;

    public BNFParser(String path, Boolean case_sensitive) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line = reader.readLine();

        while(line != null) {
            //Todo: Fix comments after line and add multi line comment support
            if (!line.startsWith("//")) { //If it's not a single comment
                Matcher nameMatcher = Pattern.compile(namePattern + "(?=\\s*::=)").matcher(line);
                if (nameMatcher.find()) { // BNF line is valid
                    String currentName = nameMatcher.group().replaceAll("<|>","");
                    String statementContent = line.substring(line.indexOf("::=") + 3).strip();

                    // check if is terminal or not
                    if (isStatementTerminal(statementContent)) {
                        terminalMap.add(Map.entry(currentName, statementContent));
                    } else {
                        nonTerminalMap.add(Map.entry(currentName, statementContent));
                    }
                } else {
                    if (!line.isBlank()) { //If line is not empty
                        System.out.println("\"" + line + "\"");
                        throw new BNFException(line);
                    }
                }
            }
            line = reader.readLine();
        }
        dictionary = createDictionary(case_sensitive);
        productionRules = createProductionRules();
        parsingTable = createParsingTable();
        reader.close();
    }

    /**
     * Create a dictionary of regexes for each terminal statement
     * @param case_sensitive Is the dictionary case-sensitive
     * @return An ordered list containing for each symbol name a matching regex
     */
    private List<Map.Entry<String,Pattern>> createDictionary(Boolean case_sensitive) {
        List<Map.Entry<String,Pattern>> dictionary = new ArrayList<>();

        terminalMap.forEach(terminal -> {
            String name = terminal.getKey();
            StringBuilder statement = new StringBuilder(terminal.getValue());

            List<String> slicedStatement = sliceStatement(statement.toString(), "r?" + stringPattern);

            //We reset the statement value to fill it with the processed statement
            statement = new StringBuilder();

            //For each part of the statement
            for (String group: slicedStatement) {
                if (group.charAt(0) == 'r'){ //If it's a regex
                    group = "(" + group.substring(2, group.length() - 1) + ")";
                } else if (group.charAt(0) == '\"') { //It's a string
                    //We need to escape symbols
                    group = "(" + escapeSymbols(group.substring(1, group.length() - 1)) + ")";
                } else {
                    group = group.replaceAll("(?<!\\\\)[{\\[]", "("); //Replace opening braces and curly
                    group = group.replaceAll("(?<!\\\\)}", ")"); //Replace closing curly
                    group = group.replaceAll("(?<!\\\\)]", ")?"); //Replace closing braces
                }
                statement.append(group); //Append the processed slice to the statement
            }
            if (case_sensitive) {
                dictionary.add(Map.entry(name,Pattern.compile("^("+ statement.toString().strip()+")$")));
            }else {
                dictionary.add(Map.entry(name, Pattern.compile("^("+ statement.toString().strip()+")$", Pattern.CASE_INSENSITIVE)));
            }
        });

        return dictionary;
    }

    public List<Map.Entry<String, Pattern>> getDictionary() {
        return this.dictionary;
    }

    private HashMap<String, HashMap<BNFSymbol, List<BNFSymbol>>> createParsingTable() {
        //see: https://www.youtube.com/watch?v=oOCromcWnfc

        HashMap<String, HashMap<BNFSymbol, List<BNFSymbol>>> parsingTable = new HashMap<>();

        //Get the "firsts" set for each production rule
        //Todo: Check if we can improve this (BIG O(2n)), Do everything inside the same loop, and if geFollow needs a first that doesn't exist yet, call getFirst for it
        productionRules.getValue().forEach((name,rule) -> getFirsts(name));
        productionRules.getValue().forEach((name,rule) -> getFollows(name));

        // Building parsing table structure.
        /*
            Useful video: https://www.youtube.com/watch?v=DT-cbznw9aY
            ISSUE: Because we don't know which non-terminal has generated which set of firsts, we can't properly create the parsing table I guess
        */

        //Refer to here to compare: https://www.cs.princeton.edu/courses/archive/spring20/cos320/LL1/
        /*
            Use the following grammar:

            starting_statement ::= ingredient_statement variable_declaration serving_statement inner_block_statement

            serving_statement ::= serving double_dot
            ingredient_statement ::= ingredient double_dot

            inner_block_statement ::= inner_block_statement#
            inner_block_statement ::= ''
            inner_block_statement# ::= variable_declaration inner_block_statement
            inner_block_statement# ::= arith  inner_block_statement
            inner_block_statement# ::= conditional 'ok' inner_block_statement

            conditional ::= if_clause inner_block_statement else_clause
            if_clause ::= 'if' 'you' 'have' condition 'then'
            condition ::= 'exactly' condition_declaration condition_aux
            condition_declaration ::= id 'ml' 'of' id
            condition_aux ::= 'and' 'you' 'have' condition
            condition_aux ::= ''
            else_clause ::= 'otherwise' inner_block_statement
            else_clause ::= ''

            variable_declaration ::= - id ml of id

            ingredient ::= 'ingredients'
            serving ::= 'serving'
            double_dot ::= ':'

            arith ::= E
            E  ::= K E'
            E'  ::= add K E'
            E' ::= ''
            K  ::= T K'
            K'  ::= strain T K'
            K' ::= ''
            T  ::= J T'
            T'  ::= mix J T'
            T' ::= ''
            J  ::= M J'
            J'  ::= chop M J'
            J' ::= ''
            M  ::= F M'
            M'  ::= mod F M'
            M' ::= ''
            F  ::= id
            F ::= ( E )

        */

        for (Map.Entry<String, List<BNFSymbol>> entry : firsts.entrySet()) {
            List<List<BNFSymbol>> splitProductionRule = new ArrayList<>();
            List<BNFSymbol> followSet = follows.get(entry.getKey());
            List<BNFSymbol> temp = new ArrayList<>();
            HashMap<BNFSymbol, List<BNFSymbol>> parsingTableRow = new HashMap<>();
            for (BNFSymbol symbol: productionRules.getValue().get(entry.getKey())) {
                if (symbol.getType() == BNFSymbol.SymbolType.Spechar && Objects.equals(symbol.getName(), "or")) {
                    splitProductionRule.add(temp);
                    temp = new ArrayList<>();
                } else {
                    temp.add(symbol);
                }
            }
            if (temp.size() > 0) splitProductionRule.add(temp);
            for (List<BNFSymbol> rule : splitProductionRule) {
                if (rule.get(0).isTerminal()) {
                    parsingTableRow.put(rule.get(0), rule);
                    continue;
                }
                int group_index = 0;
                while (true) {
                    if (rule.get(group_index).getType() == BNFSymbol.SymbolType.Symbol) {
                        if (firsts.containsKey(rule.get(group_index).getName())) {
                            firsts.get(rule.get(group_index).getName()).forEach(first -> {
                                if (first.getType() != BNFSymbol.SymbolType.Epsilon) { //If the symbol is an epsilon, we don't add it to the parsing table
                                    parsingTableRow.put(first, rule);
                                }
                            });
                        }
                        //If the non-terminal of the rule contains an epsilon, add the rule for each symbol of the follow set
                        if (nullableProductionRules.contains(rule.get(group_index).getName())) {
                            followSet.forEach(f -> parsingTableRow.put(f, rule));
                            if (rule.size() == group_index+1) { //If we are at the end of the rule, we don't try to get the next symbol
                                break;
                            }
                        } else {
                            break; //We're done filling the parsing table for this part of the rule
                        }
                    } else if (rule.get(group_index).getType() == BNFSymbol.SymbolType.Epsilon) {
                        followSet.forEach(f -> {
                            if (parsingTableRow.keySet().stream().noneMatch(k -> Objects.equals(k.getName(), f.getName()))) parsingTableRow.put(f, rule);
                        });
                        break; //If we've found an epsilon, we're done filling the parsing table for this part of the rule
                    }
                    group_index += 1;
                }
            }
            parsingTable.put(entry.getKey(), parsingTableRow);
        }
        //System.out.println("Parsing table created");
        //ADD THE STARTING RULE
        HashMap<BNFSymbol, List<BNFSymbol>> starting_statement_rules_matching = new HashMap<>();
        List<BNFSymbol> starting_statement_rules = new ArrayList<>();
        starting_statement_rules.add(new BNFSymbol(productionRules.getKey(), null, false, BNFSymbol.SymbolType.Symbol));
        starting_statement_rules.add(new BNFSymbol("$", null, true, BNFSymbol.SymbolType.End));
        firsts.get(productionRules.getKey()).forEach(symbol -> {
            starting_statement_rules_matching.put(symbol, starting_statement_rules);
        });
        parsingTable.put("start", starting_statement_rules_matching);
        return parsingTable;
    }

    public HashMap<String, HashMap<BNFSymbol, List<BNFSymbol>>> getParsingTable() {
        return parsingTable;
    }

    /**
     * Get the "firsts" set for a production rule
     * @param productionRuleName The name of the production rule to create the first set recursively for (usually the starting rule)
     */
    //TODO: check that productionRulesContainingEpsilon may fail if two symbols have the same name but not the same type (probably not but we never know)
    private void getFirsts(String productionRuleName) {
        boolean all_firsts_found = false; //If set to true, we found all the firsts for the current bnfSymbol (gets reseted when an | (or) is found
        List<BNFSymbol> local_firsts = new ArrayList<>();
        List<BNFSymbol> productionRule = productionRules.getValue().get(productionRuleName);

        //Prevent endless recursion (either left or circle) from happening
        if (recursionStack.contains(productionRuleName)) {
            throw new BNFException(productionRuleName, "Endless recursion found in grammar !");
        }
        recursionStack.add(productionRuleName);

        for (BNFSymbol bnfSymbol : productionRule) {
            if (bnfSymbol.getType() == BNFSymbol.SymbolType.Spechar && Objects.equals(bnfSymbol.getName(), "or")) {
                all_firsts_found = false; //If we find the | operator, we reset the flag as we need to find another "first"
                continue;
            }
            if(!all_firsts_found) { //If we need to find another terminal
                if (bnfSymbol.isTerminal()) { //If we find a terminal
                    local_firsts.add(bnfSymbol);
                    all_firsts_found = true;
                } else { //If we find a non-terminal
                    if (bnfSymbol.getType() == BNFSymbol.SymbolType.Epsilon) { //If we find an epsilon, we add it to the first set
                        local_firsts.add(bnfSymbol);
                        nullableProductionRules.add(productionRuleName); //And we mark the rule as containing an epsilon
                    } else if (nullableProductionRules.contains(bnfSymbol.getName())) { //If the rule contains a non-terminal containing an epsilon
                        nullableProductionRules.add(productionRuleName); //We mark the rule as containing an epsilon
                    }
                    if (productionRules.getValue().containsKey(bnfSymbol.getName())) { //If the non-terminal is in the production rules (we exclude epsilon or other non-terminals that can't be expended)
                        if (!firsts.containsKey(bnfSymbol.getName())) { //If we haven't found it already in another production rule, and we need its first set
                            getFirsts(bnfSymbol.getName());
                        }
                        //firsts.get(bnfSymbol.getName()).entrySet().stream().map(s -> s.getValue().stream().filter(l -> !l.getType().equals(BNFSymbol.SymbolType.Epsilon))).toList()

                        local_firsts.addAll(firsts.get(bnfSymbol.getName()).stream().filter(symbol -> symbol.getType() != BNFSymbol.SymbolType.Epsilon).toList());
                        //If the current bnfSymbol (which is a non-terminal) or any sub production rules contains epsilon, we should get the next terminal or symbol's first set as well
                        if (!nullableProductionRules.contains(bnfSymbol.getName())) {
                            all_firsts_found = true;
                        }
                    }
                }
            }
        }
        firsts.put(productionRuleName, local_firsts);
        recursionStack.pop();
    }

    //TODO: complete this function
    //Faire un parent map
    private void getFollows(String productionRuleName) {
        //AtomicBoolean follow_found = new AtomicBoolean(false);
        List<BNFSymbol> local_follows = new ArrayList<>();
        List<BNFSymbol> productionRule = productionRules.getValue().get(productionRuleName);
        recursionStack.add(productionRuleName);
        /* If the rule is the starting one, then add $ to the follows */
        if (Objects.equals(productionRuleName, productionRules.getKey())) {
            local_follows.add(new BNFSymbol("$", null, true, BNFSymbol.SymbolType.End));
        }

        for (Map.Entry<String, List<Integer>> entry: productionParentRules.get(productionRuleName).entrySet()) {
            if (recursionStack.stream().filter(e -> Objects.equals(e, entry.getKey())).count()>=2/*.contains(entry.getKey())*/) continue; //If we've already seen this parent production rule twice (to be sure to get the follows), try another one
            List<BNFSymbol> parentProductionRule = productionRules.getValue().get(entry.getKey());
            entry.getValue().forEach(position -> {
                if (!Objects.equals(entry.getKey(), productionRuleName) && (position == parentProductionRule.size()-1)) {
                    //Rule: If the non-terminal is the last symbol in the production rule,
                    //AND it's not a recursive call, then its follow set is the follow set of its parent
                    if (!follows.containsKey(entry.getKey())) {
                        getFollows(entry.getKey());
                    }
                    local_follows.addAll(follows.get(entry.getKey()));
                } else {
                    BNFSymbol next_symbol = parentProductionRule.get(position + 1);

                    //Rule: If the non-terminal is the last symbol in an or clause (|),
                    //AND it's not a recursive call, then its follow set is the follow set of its parent
                    if (!Objects.equals(entry.getKey(), productionRuleName) &&
                            next_symbol.getType() == BNFSymbol.SymbolType.Spechar &&
                            Objects.equals(next_symbol.getName(), "or")) {
                        if (!follows.containsKey(entry.getKey())) {
                            getFollows(entry.getKey());
                        }
                        local_follows.addAll(follows.get(entry.getKey()));
                    } else {
                        int next_symbol_loop_offset = 1;
                        while (position+next_symbol_loop_offset < parentProductionRule.size()) {
                            BNFSymbol next_symbol_loop = parentProductionRule.get(position + next_symbol_loop_offset);

                             if (next_symbol_loop.getType() != BNFSymbol.SymbolType.Symbol || next_symbol_loop.isTerminal()) {
                                local_follows.add(next_symbol_loop);
                                break;
                            } else {
                                 List<BNFSymbol> next_symbol_loop_first_set = firsts.get(next_symbol_loop.getName());
                                 //We repeat this rule as long as the firsts of the next symbol contains NULL (espilon)
                                 //Rule: If the non-terminal is followed by a symbol, then the first of it (minus NULL (epsilon)) is its follow set
                                 local_follows.addAll(next_symbol_loop_first_set.stream().filter(s -> s.getType() != BNFSymbol.SymbolType.Epsilon).toList());
                                 if (next_symbol_loop_first_set.stream().noneMatch(symbol -> symbol.getType() == BNFSymbol.SymbolType.Epsilon)) break;
                             } /*else if (next_symbol.getType() == BNFSymbol.SymbolType.Spechar && Objects.equals(next_symbol.getName(), "or")) {
                        follow_found.set(false);
                        }*/
                            next_symbol_loop_offset++;
                        }

                        List<BNFSymbol> next_symbol_first_set = firsts.get(next_symbol.getName());
                        //If a non-terminal is followed by a symbol where its first set contains NULL (epsilon), then follow set of the non-terminal is equal to the follow set of its parent
                        if (next_symbol.getType() == BNFSymbol.SymbolType.Symbol && !next_symbol.isTerminal() && next_symbol_first_set.stream().anyMatch(symbol -> symbol.getType() == BNFSymbol.SymbolType.Epsilon)) {  // && !follow_found.get()
                            if (follows.containsKey(entry.getKey())) {
                                local_follows.addAll(follows.get(entry.getKey()));
                                //follow_found.set(true);
                            } else {
                                getFollows(entry.getKey());
                                local_follows.addAll(follows.get(entry.getKey()));
                            }
                        }
                    }
                }
            });
        }

//        ["E": ["T": [0,2], "F": [0]]]
//        <T> ::= <E>|<T>

        follows.put(productionRuleName, local_follows.stream().filter(distinctByKey(t -> t.getName() + t.getType() + t.isTerminal() + t.getScopeId() + t.getValue())).toList());
        recursionStack.pop();
    }

    /**
     * Get distinct follow set by object parameters
     * @param keyExtractor
     * @param <T>
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor)
    {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * Create a data structure containing the starting rule name and a collection of all rules with their associated symbols
     * @return An abstract map containing the starting rule as key, a Hashmap as value
     */
    private AbstractMap.SimpleEntry<String, HashMap<String, List<BNFSymbol>>> createProductionRules() {
        List<String> terminalNameList = terminalMap.stream().map(Map.Entry::getKey).toList();

        for (Map.Entry<String, String> entry: nonTerminalMap) {
            HashMap<String, List<Integer>> temp = new HashMap<String, List<Integer>>();
            productionParentRules.put(entry.getKey(), temp);
        }

        HashMap<String,List<BNFSymbol>> productionRules = new HashMap<>();
        //For each BNF statement, we create the corresponding production rule
        for (Map.Entry<String, String> entry: nonTerminalMap) {
            String statement = entry.getValue();
            List<String> slicedStatement = sliceStatement(statement, "r?" + stringPattern + "|" + namePattern + "|([A-Za-z])+|([^A-Za-z\\s])");

            AtomicInteger count_group_position = new AtomicInteger(-1);
            //Convert string groups to BNF symbols
            List<BNFSymbol> symbolList = new ArrayList<>(slicedStatement.stream().map(group -> {
                count_group_position.getAndIncrement();
                if (group.charAt(0) == 'r') {
                    throw new BNFException(statement, "Can't use regex in non-terminal statement");
                } else if (group.charAt(0) == '\"') {
                    return new BNFSymbol(null, group.substring(1, group.length() - 1), true, BNFSymbol.SymbolType.String);
                } else if (group.charAt(0) == '<') {
                    String groupName = group.substring(1, group.length() - 1);
                    boolean isTerminal;
                    if (terminalNameList.contains(groupName)) {
                        isTerminal = true;
                    } else if (nonTerminalMap.stream().anyMatch(e-> Objects.equals(e.getKey(), groupName))) {
                        isTerminal = false;
                        if (productionParentRules.containsKey(groupName)){
                            HashMap<String, List<Integer>> temp = productionParentRules.get(groupName);
                            List<Integer> positions;
                            if (temp.containsKey(entry.getKey())) {
                                positions = temp.get(entry.getKey());
                            } else {
                                positions = new ArrayList<>();
                            }
                            positions.add(count_group_position.get());
                            temp.put(entry.getKey(),positions);
                            productionParentRules.replace(groupName, temp);
                        }
                    } else {
                        throw new BNFException(statement, "Undefined symbol in statement");
                    }
                    return new BNFSymbol(groupName, null, isTerminal, BNFSymbol.SymbolType.Symbol);
                } else {
                    return switch (group.charAt(0)) {
                        case '|' -> new BNFSymbol("or", null, false, BNFSymbol.SymbolType.Spechar);
                        case '+', '-', '*', '/', '(', ')' ->
                                new BNFSymbol(String.valueOf(group.charAt(0)), null, true, BNFSymbol.SymbolType.Spechar);
                        case 'e' -> new BNFSymbol("epsilon", null, false, BNFSymbol.SymbolType.Epsilon);
                        default -> throw new BNFException(statement, "Unrecognized operator in statement");
                    };
                }
            }).toList());
            productionRules.put(entry.getKey(), symbolList);
        }
        String startingRule = null;

        for (Map.Entry<String, HashMap<String, List<Integer>>> entry : productionParentRules.entrySet()) {
            int number_of_parents = 0;
            for (Map.Entry<String, List<Integer>> entry2: entry.getValue().entrySet()) {
                if (!Objects.equals(entry2.getKey(), entry.getKey())){
                    number_of_parents += entry2.getValue().size();
                }
            }
            if (startingRule == null && number_of_parents == 0) {
                startingRule = entry.getKey();
            } else if (startingRule != null && number_of_parents == 0) {
                throw new BNFException(entry.getKey(), "Dangling rule found !");
            }
        }


        if (startingRule == null) { //No uncalled rule found
            System.out.println("WARNING, NO UNCALLED GRAMMAR RULE FOUND, USING FIRST NON-TERMINAL AS STARTING POINT");
            startingRule = nonTerminalMap.get(0).getKey();
        }
        return new AbstractMap.SimpleEntry<>(startingRule, productionRules);
    }

    /**
     * Checks if a statement contains any sub statement in the form <name>
     * @param statementContent A BNF statement
     * @return True or False
     */
    private boolean isStatementTerminal(String statementContent) {
        List<String> currentLineString = new ArrayList<>();

        Matcher stringMatcher = Pattern.compile(stringPattern).matcher(statementContent);
        while (stringMatcher.find()) {
            currentLineString.add(stringMatcher.group());
        }

        AtomicReference<String> tempStatement = new AtomicReference<>(statementContent);

        currentLineString.forEach(str -> tempStatement.set(tempStatement.get().replace(str, "")));

        return !Pattern.compile(namePattern).matcher(tempStatement.get()).find() && !Objects.equals(tempStatement.get().strip(), "e");
    }

    /**
     * Escape any symbol except '-' and '_' by appending a backslash in front of it
     * @param s Any string
     * @return An escaped string
     */
    private String escapeSymbols(String s) {
        return s.replaceAll("([^A-Za-z0-9-_])", "\\\\$1");
    }

    /**
     * Split a BNF statement in multiple groups using a regex
     * @param statement A BNF statement (anything following the symbol "::=")
     * @param groupRegex A regex used to split the statement in multiple groups
     * @return A list containing all the groups
     */
    private List<String> sliceStatement(String statement, String groupRegex) {
        List<String> slicedStatement = new ArrayList<>();
        while (true) {
            //Find any string or regex
            Matcher stringMatcher = Pattern.compile(groupRegex).matcher(statement);
            if (stringMatcher.find()) {
                int startPos = stringMatcher.start();
                int endPos = stringMatcher.end();

                String before = statement.substring(0,startPos).strip();
                String match = statement.substring(startPos,endPos).strip();
                statement = statement.substring(endPos).strip(); //after

                if (before.length() > 0) {
                    slicedStatement.add(before);
                }
                //Add the string to the list
                slicedStatement.add(match);
            } else { //If we don't find any string, regex or reference to split on
                if (statement.length() > 0) {
                    slicedStatement.add(statement);
                }
                break;
            }
        }
        return slicedStatement;
    }
}

