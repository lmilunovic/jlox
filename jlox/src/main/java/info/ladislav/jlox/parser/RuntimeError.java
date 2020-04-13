
package info.ladislav.jlox.parser;

import info.ladislav.jlox.lexer.Token;

public class RuntimeError extends RuntimeException {
    public final Token token;

    RuntimeError(Token token, String message) {
      super("RuntimeError: " + message);
      this.token = token;
    }
  }