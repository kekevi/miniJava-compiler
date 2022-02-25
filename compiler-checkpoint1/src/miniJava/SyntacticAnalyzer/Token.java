package miniJava.SyntacticAnalyzer;

public class Token {
  public TokenKind kind;
  public String spelling;

  public Token(TokenKind kind, String spelling) {
    this.kind = checkKeyword(kind, spelling);
    this.spelling = spelling;
  }

  public void print() {
    System.out.println(kind.name() + " : " + spelling);
  }

  private static TokenKind checkKeyword(TokenKind k, String s) {
    if (k != TokenKind.ID) {
      return k;
    }

    switch (s) {
      // primatives/typing
      case "int":
        return TokenKind.INT;
      case "boolean":
        return TokenKind.BOOLEAN;
      case "void":
        return TokenKind.VOID;
      case "class":
        return TokenKind.CLASS;

      // keywords
      case "this":
        return TokenKind.THIS;
      case "new":
        return TokenKind.NEW;
      case "static":
        return TokenKind.STATIC;
      case "true":
        return TokenKind.TRUE;
      case "false":
        return TokenKind.FALSE;

      // keywords - visibility
      case "public":
        return TokenKind.PUBLIC;
      case "private":
        return TokenKind.PRIVATE;

      // keywords - control flow
      case "return":
        return TokenKind.RETURN;
      case "while":
        return TokenKind.WHILE;
      case "if":
        return TokenKind.IF;
      case "else":
        return TokenKind.ELSE;

      default:
        return TokenKind.ID;
    }
  }

}
