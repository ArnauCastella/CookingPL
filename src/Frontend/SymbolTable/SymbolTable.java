package Frontend.SymbolTable;

import java.util.Hashtable;

public class SymbolTable {
    private final Hashtable<String, Symbol> table;

    public SymbolTable() {
        table = new Hashtable<String, Symbol>();
    }

    public void put(String name, Symbol symbol) {
        table.put(name, symbol);
    }

    public boolean contains(String name) {
        return table.containsKey(name);
    }

    public void setType(String name, String type) {
        table.get(name).setType(type);
    }

    public void setLabel(String name, String label) {
        table.get(name).setInfoLabel(label);
    }

    public void setAddressOffset(String name, int offset) {
        table.get(name).setAddressOffset(offset);
    }

    public Symbol get(String name) {
        return table.get(name);
    }

    public Hashtable<String, Symbol> getTable() {
        return this.table;
    }

    /*
        It is utilized in the compiler's different phases, as shown below:
        Lexical Analysis: New table entries are created in the table, For example, entries about tokens.
        Syntax Analysis: Adds the information about attribute type, dimension, scope line of reference, use, etc in the table.
        Frontend.Semantic Analysis: Checks for semantics in the table, i.e., verifies that expressions and assignments are semantically accurate (type checking) and updates the table appropriately.
        Intermediate Code generation: The symbol table is used to determine how much and what type of run-time is allocated, as well as to add temporary variable data.
        Code Optimization: Uses information from the symbol table for machine-dependent optimization.
        Target Code generation: Uses the address information of the identifier in the table to generate code.

        Structure for Frontend.SymbolTable Approach 1
        Arraylist with hashmap?
            [] -> [token: [datatype, memory location, value...], token2: [datatype, memory location, value...]]
            [] ->
            [] ->
            [] ->
            [] ->
        Every [] is a new scope?
        It gets generated when for loop, if conditional, ingredients:, serving:, and so on?

        Structure for Frontend.SymbolTable Approach 2
        Int + hashmap in Tree format?
            [int1] -> [token: [Symbol], token2: [Symbol], token3: [Symbol]
            [int2] -> (parent is int1)
            [int3] -> (parent is int1)
            [int4] ->
            [int5] ->
        Every [intX] is a new scope?
        It gets generated when for loop, if conditional, ingredients:, serving:, and so on?

        Doubts: Do we actually care about the parent scope?

        The core operations of a symbol table are Allocate, free, insert, lookup, set attribute, and get attribute.
        The allocation operation creates n empty symbol table.
        The free operation is used to remove all records and free the storage of a symbol table.
        As the name implies, the insert operation puts a name into a symbol table and returns a pointer to its entry.
        The lookup function looks up a name and returns a reference to the corresponding entry.
        The set and get attributes associate an attribute with a given entry and get an attribute associated with a provided.
        Other steps may be introduced depending upon the requirements.
        For example, a delete action deletes a previously entered name.

     */

}