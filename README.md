# COOKING PROGRAMMING LANGUAGE DEFINITION				

⚠  *The language is **NOT** <ins>case sensitive</ins>.* We **CAN'T** create any kind of collection (arrays, tuples, etc.)
Empty lines (white space or tabulation only) are allowed.

## Data types

|Type|Full name|Corresponding C type|Accepted Range|
|:---:|:---:|:---:|:---:|
|`ml`|Milliliter|`int`|`-32768` to `+32767`|
|`g`|Gram|`float`|`3.4*10`<sup>`-38`</sup> to `3.4*10`<sup>`+38`</sup>|
|`temp`|Temperature|`bool`|`hot` (true) or `cold` (false)|

## Ascii symbols dictionnary

|Char|Char name|C equivalent|Meaning|
|:---:|:---:|:---:|:---:|
|`\n` (0xD)|Carriage return|`;` (0x3B)|Terminate a line
|`\t` (0x9)|Tabulation|Kinda like the `{` and `}`|Works more like the python tabulation to indicate scopes change.|
|`,` (0x2C)|Comma|`,`|Used to separate arguments after the `using` keyword to receive multiple args.|
|`+` (0x2B)|Plus|`printf("%s", char[])` or `strcat()`|Concat two things together (*e.g. string + var, string + string, etc.*)|
|`(` and `)`(0x28 and 0x29)|Parenthesis|Same|Operation priority definition (order of execution).|


## Arithmetic operations

Any arithmetic operation between floats and ints is allowed and the result will be a float. Any other arithmetic operation between two different data types is **NOT** allowed.

⚠  *The language does **NOT** implement either a lower or equal (<=) or greater or equal (>=) symbol.*

|Keyword|Name|C equivalent|Meaning|
|:---:|:---:|:---:|:---:|
|`add`|Addition|`+`|Addition|
|`strain` *(from a strainer)*|Subtraction|`-`|Subtraction|
|`chop` *(from a chopping knife)*|Division|`/`|Division|
|`mod`|Modulo|`%`|Returns the rest of an euclidean division (modulo).|
|`mix`|Multiply|`*`|Multiply|

## Logical operations

Only variables or values of the same type can be compared together. We can't compare int to float, or float to bool, etc. Doing a logical operation returns a `hot` or `cold` (temperature data type) value like in c.

|Keyword|C equivalent|Meaning|
|:---:|:---:|:---:|
|`if`|`if`|An if statement.|
|`then`|None (equivalent to `:` in python's ifs)|Marks the end of an if's condition (i.e. `if you have exactly 0 ml of foo then` ).|
|`otherwise`|`else`|Used to create an `else` or `else if` (e.i. `otherwise if`).|
|`is`|`==`|Compare two booleans together.|
|`exactly`|`==`|Compare for equality.|
|`less`|`<`|Compare for less than.|
|`more`|`>`|Compare for more than.|
|`and`|`&&`|AND logical operator.|
|`or`|`\|\|`|OR logical operator.|
|`not`|`!`|NOT logical operator.|

## Loops

|Keyword|C equivalent|Meaning|
|:---:|:---:|:---:|
|`bake for`|`for`|A for loop.|
|`stop oven`|`break`|A break to quit a loop.|

## Keywords (dictionnary)

|Keyword|C equivalent|Meaning|
|:---:|:---:|:---:|
|`referring to recipe`|`#include`|Import any other library or piece of code to the current program.|
|`ingredients:`|None|Part of the code, at the beginning of the program, where we define the variables used in the 'main' (`serving`) part. Must **ALWAYS** be the first keyword of the program except for the comments and `referring to recipe` statements.|
|`steps:`|None|Part of the code, after the `ingredients:` section, where we declare the functions of the program. Each function is a recipe "step". Steps start at number 1. Steps must be defined in order (no step skip allowed).|
|`serving:`|`function main()`|The main function (entrypoint) of the program. Each line can be prefixed with a `-` token.|
|`set aside`|`return`|A return statement.|
|`set aside with`|`return`|A return statement with a variable.|
|`equals`|`=`|Assign a value to a function.|
|`using`|None|Indicates that the statement following is a parameter of the function.|
|`EOF` (end of file)|None|End of the main function (`serving:`)|

## Keyword aliases

|Keyword|Aliases|
|---|---|
|`bake for`|`heat for`, `grill for`, `cook for`|

## Functions

|Keyword|C equivalent|Meaning|
|:---:|:---:|:---:|
|`post-it`|`print()`|Print something to screen (STDIO)|

## Useless tokens

Theses tokens exists for the sole purpose of formating the code to look like an english recipe (natural language).

|Token|
|:---:|
|`than`|
|`-` (0x2D)|
|`you`|
|`have`|
|`time`|
|`times`|

During the pre-compiling part of the "compiler", we get rid of these tokens (just like we would do with comments) and then we don't care about them in the grammar (parsing).

## Grammar definition

Refer to https://stackoverflow.com/a/8643425 for questions concerning the indent issue.

@Todo: Add the regexes to the grammar.

|Grammar rule name|Regexes|
|:---:|:---:|
|starting_statement| ingredients:\n<ingredient_declaration>steps:\n<steps_declaration>serving:\n<inner_block_statement>EOF|
|ingredient_declaration|(\d* <data_type> of \w*\n)+|
|steps_declaration|(<type_of_function>)+|
|type_of_function|<simple_function> or <returning_function> or <parameter_function> or <parameter_returning_function>|
|simple_function|\d+. \w+\n<inner_block_statement>set aside\n|
|returning_function|\d+. \w+ prepares <data_type> \w+\n<inner_block_statement>set aside with \w+\n|
|parameter_function|\d+. \w+ using <parameters>\n<inner_block_statement>set aside\n|
|parameter_returning_function|\d+. \w+ prepares <data_type> of \w+ using <parameters>\n<inner_block_statement>set aside with \w+\n|
|parameters|<data_type> of \w+ <extra_parameter>|
|extra_parameter|( , <parameters>)*+|
|data_type|ml or g or temp|
|inner_block_statement|(\t<inner_block_statement_prime>)+|
|inner_block_statement_prime|<variable_declaration> <inner_block_statement> or <for_loop> <inner_block_statement> or <conditional> <inner_block_statement> or <arith_operation> <inner_block_statement>or <display_on_screen> <inner_block_statement> or <see_direction> <inner_block_statement>|
|variable_declaration|<value> <data_type> of \w+\n or \w+ equals <variable_declaration_aux>\n|
|value|<integer> or <float> or <boolean>|
|integer| |
|variable_declaration_aux||




### Starting Statement (We always define ingredients and steps even if they are empty, to preserve the recipe template) IVAN
```
<starting_statement> ::= ingredients: <new_line> <ingredient_declaration> steps: <new_line> <steps_declaration> serving: <inner_block_statement> EOF
```

###  Inside Starting Statement NonTerminals - Ingredient Declaration IVAN
```
<ingredient_declaration> ::= <value> <datatype> of varName <new_line> <ingredient_declaration> | E
<value> ::= value value*
```

###  Inside Starting Statement NonTerminals - Steps Declaration IVAN
```
<steps_declaration>  ::= <type_of_function> <steps_declaration> | E
<type_of_function> ::= <simple_function> | <returning_function> | <parameter_function> | <parameter_returning_function>
```

### New Line and Tab IVAN
```
<new_line> ::= \n
<tab> ::= \t
<logic_operator> ::= or | and | not
```

### Inner Block Statement IVAN
``` 
<inner_block_statement> ::= <tab> <inner_block_statement_prime> | E
<inner_block_statement_prime> ::= <variable_declaration> <inner_block_statement> | <for_loop> <inner_block_statement> | <conditional> <inner_block_statement> | <arith_operation> <inner_block_statement>
| <display_on_screen> <inner_block_statement> | <see_direction> <inner_block_statement>
```  

### Variable Declaration ARNAU
```
<variable_declaration> ::= <value> <datatype> of varName <new_line> | varName equals <variable_declaration_aux> <new_line> 
<variable_declaration_aux> ::= <see_direction> | <arithmetic_operation> 
<datatype> ::= g | ml | temp
<value> ::= <integer> | <floating_point> | <boolean>
```

### For Loop ARNAU
```
<for_loop> ::= bake for value minutes varName starting at value <new_line> <tab>
```

### Conditional ARNAU
```
<conditional> ::= <if_clause> <inner_block_statement> <else_clause>
<if_clause> ::= if you have <condition> then <new_line> <tab>
<condition> ::= <comparing_operators> <condition_declaration> <condition_aux>
<comparing_operators> ::= exactly | more than | less than
<condition_declaration> ::= value <datatype> of varName
<condition_aux> ::= <logic_operator> you have <condition> | E
<else_clause> ::= otherwise <new_line> <tab> <inner_block_statement> | E
```

### Arithmetic Operation (REVIEW) ARNAU
```
<arith_operation> ::= id <arith_operator> id2 <new_line>
<arith_operation> ::= varName <arith_operator> varName2 <new_line> | varName <arith_operator> value <new_line> | value <arith_operator> value2 <new_line>
<arith_operator> ::= add | strain | chop | mod | mix
```

### Display on Screen ARNAU
```
<display_on_screen> ::= Post-It <post_it_content> <new_line>
<post_it_content> "content_id" + varName <post_it_content_aux> | varName + "content_id" <post_it_content_aux> | varName <post_it_content_aux> | "content_id" <post_it_content_aux>
<post_it_content_aux> ::= + <post_it_content> | E
```


### Function Call GODDY
```
<see_direction> ::= See functionName <specify_direction_type> <new_line>
<specify_direction_type> ::= with varName | E
```

### Function grammar GODDY
```
<simple_function> ::= number. functionName <new_line> <inner_block_statement> set aside <new_line>
```

### Functions grammar returning value GODDY
```
<returning_function> ::= number. functionName prepares <data_type> varName <new_line> <inner_block_statement> set aside with varName <new_line>
```

### Functions grammar with parameters GODDY

```
<parameter_function> ::= number. functionName using <parameters> <new_line> <inner_block_statement> set aside <new_line>
<parameters> ::= <data_type> of variableName <extra_parameter>
<extra_parameter> ::= , <parameters> | E (note the whitespace before the comma).
```

### Functions grammar with parameters and returning value GODDY
```
<parameter_returning_function> ::= number. functionName prepares <data_type> of varName using <parameters> <new_line> <inner_block_statement> set aside with varName <new_line>
```

### Functions grammar with parameters and returning value

## Code example

Bellow are example of codes using the cooking programming language.

### Non-recursive fibonacctea

```js
Ingredients:
9 ml of water

Steps:
1. Fibonacctea using ml of n
    0 ml of milk
    1 ml of tea
    0 ml of water
    2 ml of honey

    if you have exactly 0 ml of n then
			set aside (tea strain 1 time)

    Heat for n minutes, Stopwatch starting at 2
        water equals (milk add tea)
        milk equals tea
        tea equals water
    set aside tea

Serving:
	Post-It "Result is: " + See Fibonacctea using water
```

### Recursive fibonnacctea

```js
Ingredients:
9 mL of n

Steps:
1. boil_fibonacctea using mL of yummy_thing
    if you have less than 1 mL of yummy_thing or you have exactly 1 mL of yummy_thing then
        set aside yummy_thing
    otherwise
        set aside see boil_fibonacctea using (yummy_thing strain 1 time) add see boil_fibonacctea using (yummy_thing strain 2 times)

Serving:
    post-it "The result is " + see boil_fibonacctea using n
    post-it "The result is " + see 1. using n
```


