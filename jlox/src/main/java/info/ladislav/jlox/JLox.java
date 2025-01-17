package info.ladislav.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import info.ladislav.jlox.lexer.*;
import info.ladislav.jlox.parser.*;
/**
 * JLox interpreter initial version.
 *
 */
public class JLox 
{
    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    public static void main( String[] args ) throws IOException
    {
        if(args.length > 1){
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        }else if( args.length == 1){
            runFile(args[0]);
        }else{
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));        
        run(new String(bytes, Charset.defaultCharset()));

        //indicate error in exit code
        if (hadError) System.exit(65);        
        if (hadRuntimeError) System.exit(70);
      }         

      private static void runPrompt() throws IOException {
        //TODO implement possibility to execute expressions with REPL
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    
        for (;;) { 
          System.out.print("> ");                                  
          run(reader.readLine());
          hadError = false;                                  
        }              
      }                
      
      private static void run(String source) {    
        // For now, just print the tokens.
        List<Token> tokens =  new Scanner(source).scanTokens();
        Parser parser = new Parser(tokens);               
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.                   
        if (hadError) return;              

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        
        // Stop if there was a resolution error.     
        if(hadError) return;

        interpreter.interpret(statements);
      }              
      
      public static void error(int line, String message) {                       
        report(line, "", message);                                        
      }

      public static void error(Token token, String message) {              
        if (token.type == TokenType.EOF) {                          
          report(token.line, " at end", message);                   
        } else {                                                    
          report(token.line, " at '" + token.lexeme + "'", message);
        }                                                           
      }                    
    
      private static void report(int line, String where, String message) {
        System.err.println(                                               
            "[line " + line + "] Error" + where + ": " + message);        
        hadError = true;                                                  
      }

      public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
            "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
      }
}