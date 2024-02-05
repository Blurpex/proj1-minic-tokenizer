public class Lexer {

    private static final char EOF = 0;

    private Parser yyparser;
    private java.io.Reader reader;
    public int lineno;
    public int column;

    private final char[] bufferOne = new char[10];
    private final char[] bufferTwo = new char[10];
    private int bufferPosition = 10;
    private boolean mutex = false;      // true for bufferOne, false for bufferTwo

    public Lexer(java.io.Reader reader, Parser yyparser) throws Exception {
        this.reader = reader;
        this.yyparser = yyparser;
        lineno = 1;
        column = 1;
    }

    public char nextChar() throws Exception {
        // http://tutorials.jenkov.com/java-io/readers-writers.html
        int data = reader.read();
        if(data == -1) return EOF;
        return (char)data;
    }

    public int fail() {
        return -1;
    }

    private char[] manageDoubleBuffer() {
        if(bufferPosition == 10) {
            bufferPosition = 0;
            mutex = !mutex;
        }
        return mutex ? bufferOne : bufferTwo;
    }

    private char[] readBuffer() {
        char[] buffer = manageDoubleBuffer();
        try {
            for(int i = 0; i < 10; i++) {
                char curr = nextChar();
                // check for null
                if(curr == '\u0000') {
                    buffer[i] = EOF;
                    break;
                }
                buffer[i] = curr;
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        return buffer;
    }

    private char getNextChar() {
        char[] buffer = mutex ? bufferOne : bufferTwo;
        if(bufferPosition == 10)
            buffer = readBuffer();
        char nextChar = buffer[bufferPosition];
        this.bufferPosition++;
        updateLineAndColumn(String.valueOf(nextChar));
        return nextChar;
    }

    public void updateLineAndColumn(String token) {
        if(token.equals("\n")) {
            lineno++;
            column = 1;
        } else {
            column += token.length();
        }
    }

    /*
     * If yylex reach to the end of file, return  0
     * If there is a lexical error found, return -1
     * If a proper lexeme is determined, return token <token-id, token-attribute> as follows:
     *   1. set token-attribute into yyparser.yylval
     *   2. return token-id defined in Parser
     *   token attribute can be lexeme, line number, column, etc
     */
    public int yylex() throws Exception {

        int state = 0;
        String lexeme = "";
        char c, nextChar;

        while(true) {
            switch(state) {
                case 0:
                    c = getNextChar();
                    // ignore newlines and whitespace
                    if(Character.isWhitespace(c)) continue;

                    if(c == ';') {
                        state = 1;
                        continue;
                    } else if(c == '+' || c == '-' || c == '*' || c == '/') {
                        lexeme = String.valueOf(c);
                        state = 2;
                        continue;
                    } else if(c == '(') {
                        state = 3;
                        continue;
                    } else if(c == ')') {
                        state = 4;
                        continue;
                    } else if(c == ',') {
                        state = 5;
                        continue;
                    } else if(c == '<') {
                        state = 6;
                        continue;
                    } else if(c == '>') {
                        state = 7;
                        continue;
                    } else if(c == ':') {
                        state = 8;
                        continue;
                    } else if(c == '=') {
                        yyparser.yylval = new ParserVal((Object) "=");
                        return Parser.RELOP;
                    } else if(Character.isLetter(c)) {
                        return identifyKeywords(c);
                    } else if(Character.isDigit(c)) {
                        return identifyNumber(c);
                    } else if(c == EOF) {
                        state = 9999;
                        continue;
                    }
                    return fail();
                case 1:     // ;
                    yyparser.yylval = new ParserVal((Object)";");
                    return Parser.SEMI;
                case 2:     // +, -, *, /
                    yyparser.yylval = new ParserVal((Object) lexeme);
                    return Parser.OP;
                case 3:     // (
                    yyparser.yylval = new ParserVal((Object) "(");
                    return Parser.LPAREN;
                case 4:     // )
                    yyparser.yylval = new ParserVal((Object) ")");
                    return Parser.RPAREN;
                case 5:     // ,
                    yyparser.yylval = new ParserVal((Object) ",");
                    return Parser.COMMA;
                case 6:     // <
                    nextChar = getNextChar();
                    if(nextChar == '=') {
                        state = 9;
                        continue;
                    } else if(nextChar == '>') {
                        state = 11;
                        continue;
                    } else {
                        this.bufferPosition--;      // retract
                        yyparser.yylval = new ParserVal((Object) "<");
                        return Parser.RELOP;
                    }
                case 7:     // >
                    nextChar = getNextChar();
                    if(nextChar == '=') {
                        state = 10;
                        continue;
                    } else {
                        this.bufferPosition--;      // retract
                        yyparser.yylval = new ParserVal((Object) ">");
                        return Parser.RELOP;
                    }
                case 8:     // :
                    nextChar = getNextChar();
                    if(nextChar == '=') {
                        state = 12;
                        continue;
                    } else if(nextChar == ':') {
                        state = 13;
                        continue;
                    } else {
                        state = 999;
                        continue;
                    }
                case 9:     // <=
                    yyparser.yylval = new ParserVal((Object) "<=");
                    return Parser.RELOP;
                case 10:    // >=
                    yyparser.yylval = new ParserVal((Object) ">=");
                    return Parser.RELOP;
                case 11:    // <>
                    yyparser.yylval = new ParserVal((Object) "<>");
                    return Parser.RELOP;
                case 12:    // :=
                    yyparser.yylval = new ParserVal((Object) ":=");
                    return Parser.ASSIGN;
                case 13:    // ::
                    yyparser.yylval = new ParserVal((Object) "::");
                    return Parser.TYPEOF;
                case 999:
                    yyparser.yylval = new ParserVal(-1);
                    return fail();
                case 9999:
                    return EOF;
            }
        }
    }

    private int identifyKeywords(char c) {
        StringBuilder sb = new StringBuilder();
        while(Character.isLetterOrDigit(c) || c == '_') {
            sb.append(c);
            c = getNextChar();
        }

        bufferPosition--;   // retract
        yyparser.yylval = new ParserVal((Object) sb.toString());
        int token = Parser.ID;

        // identify keywords
        String[] keywords = {"int", "print", "var", "func", "if", "then", "else", "while", "void", "begin", "end"};
        for(String keyword : keywords) {
            if (sb.toString().equals(keyword)) {
                token = TokenName.valueOf(keyword.toUpperCase()).ordinal() + 10;
                break;
            }
        }

        return token;
    }

    private int identifyNumber(char c) {
        StringBuilder sb = new StringBuilder();
        yyparser.yylval = new ParserVal();

        boolean containsDot = false;
        boolean digitAfterDot = true;

        while(Character.isDigit(c) || c == '.') {

            if(c == '.' && containsDot) break;
            sb.append(c);

            // check whether there is a digit after dot
            if(!digitAfterDot) digitAfterDot = true;

            // check for multiple dots
            if(c == '.') {
                containsDot = true;
                digitAfterDot = false;
            }

            c = getNextChar();
        }

        bufferPosition--;   // retract
        if(!digitAfterDot) return -1;
        yyparser.yylval = new ParserVal((Object) sb.toString());
        return Parser.NUM;
    }
}
