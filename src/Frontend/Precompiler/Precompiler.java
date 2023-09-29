package Frontend.Precompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Precompiler {
    private final Scanner source_code;
    private String single_line_comment_pattern;

    private String next_line;

    public Precompiler(String path, String instruction_delimiter) throws FileNotFoundException {
        this.source_code = new Scanner(new File(path));
        this.source_code.useDelimiter(instruction_delimiter); //Regex pattern used to end an instruction (e.g. ';' in C & C++ or '\r\n')
        //this.single_line_comment_pattern = single_line_comment_pattern;
    }

    //Allow usage of single line comments following a pattern
    public void allowSingleLineComment(String single_line_comment_pattern) {
        this.single_line_comment_pattern = single_line_comment_pattern;
    }

    public boolean hasNext() {
        if (this.next_line == null) { //If we have consumed the previous line
            while (true) {
                if (source_code.hasNext()) { //If we can find more line in the source code
                    String temp_line = source_code.next(); //Get the line
                    //Skip line if it's a single line comments and we allow them
                    /*
                    Todo:
                        -Implement multi-line comments
                        -Implement single line comments after instruction (check for the comment symbol but not in strings)
                     */
                    if (single_line_comment_pattern != null && !temp_line.strip().startsWith(single_line_comment_pattern)) {
                        this.next_line = temp_line;
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return true; //If we haven't consumed the previous line yet by calling next()
    }

    public String next() {
        String temp_line = this.next_line; //Keep the line in a buffer
        this.next_line = null; //Remove it from the property to consume it
        return temp_line;
    }
}
