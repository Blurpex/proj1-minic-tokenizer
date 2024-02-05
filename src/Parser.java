enum TokenName {
    OP, RELOP, LPAREN, RPAREN, SEMI, COMMA, ASSIGN, TYPEOF, NUM, ID, INT, PRINT, VAR, FUNC, IF, THEN, ELSE, WHILE, VOID, BEGIN, END
}

public class Parser {

    public static final int OP         = 10;    // +  -  *  /
    public static final int RELOP      = 11;    // <  >  <=  >=  ...
    public static final int LPAREN     = 12;    // (
    public static final int RPAREN     = 13;    // )
    public static final int SEMI       = 14;    // ;
    public static final int COMMA      = 15;    // ,
    public static final int ASSIGN     = 16;    // :=
    public static final int TYPEOF     = 17;    // ::
    public static final int NUM        = 18;    // number
    public static final int ID         = 19;    // identifier
    public static final int INT        = 20;    // int
    public static final int PRINT      = 21;    // print
    public static final int VAR        = 22;    // var
    public static final int FUNC       = 23;    // func
    public static final int IF         = 24;    // if
    public static final int THEN       = 25;    // then
    public static final int ELSE       = 26;    // else
    public static final int WHILE      = 27;    // while
    public static final int VOID       = 28;    // void
    public static final int BEGIN      = 29;    // begin
    public static final int END        = 30;    // end

    Compiler compiler;
    Lexer lexer;                // lexer.yylex() returns token-name
    public ParserVal yylval;    // yylval contains token-attribute

    public Parser(java.io.Reader r, Compiler compiler) throws Exception {
        this.compiler = compiler;
        this.lexer = new Lexer(r, this);
    }

    private String getTokenName(int token) {
        String tokenName = "EOF";
        if(token >= 10 && token <= 30)
            tokenName = TokenName.values()[token - 10].name();
        return tokenName;
    }

    // 1. parser call lexer.yylex that should return (token-name, token-attribute)
    // 2. lexer
    //    a. assign token-attribute to yyparser.yylval
    //       token attribute can be lexeme, line number, colume, etc.
    //    b. return token-id defined in Parser as a token-name
    // 3. parser print the token on console
    //    if there was an error (-1) in lexer, then print error message
    // 4. repeat until EOF (0) is reached
    public int yyparse() throws Exception {

        while(true) {
            int token = lexer.yylex();  // get next token-name
            Object attr = yylval.obj;   // get token-attribute
            String tokenName = getTokenName(token);

            if(token == 0) {
                // EOF is reached
                System.out.println("Success!");
                return 0;
            }
            if(token == -1) {
                // lexical error is found
                System.out.println("Error! There is a lexical error at " + lexer.lineno + ":" + lexer.column + ".");
                return -1;
            }

            System.out.println("<" + tokenName + ", token-attr:\"" + attr + "\", " + lexer.lineno + ":" + lexer.column + ">");
//            lexer.updateLineAndColumn((String) attr);
        }
    }
}
