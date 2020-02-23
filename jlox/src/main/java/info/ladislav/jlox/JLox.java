package info.ladislav.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * JLox interpreter initial version.
 *
 */
public class JLox 
{
    static boolean hadError = false;
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
      }         

      private static void runPrompt() throws IOException {         
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
        tokens.forEach(System.out::println);                                         
      }              
      
      static void error(int line, String message) {                       
        report(line, "", message);                                        
      }
    
      private static void report(int line, String where, String message) {
        System.err.println(                                               
            "[line " + line + "] Error" + where + ": " + message);        
        hadError = true;                                                  
      }                        
}