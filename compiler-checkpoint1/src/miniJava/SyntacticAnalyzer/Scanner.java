package miniJava.SyntacticAnalyzer;

import java.io.*;
import miniJava.ErrorReporter;

public class Scanner {
  private InputStream inputStream;
	private ErrorReporter reporter;

	private char currentChar;
	private StringBuilder currentSpelling;

  // mutable flag stating if currentChar is in a comment or not
  private enum Comment {NONE, LINEAR /* // comments */, BLOCK}
  private Comment commentMode = Comment.NONE;
	
	// true when end of line is found
	private boolean eot = false; 


	public Scanner(InputStream inputStream, ErrorReporter reporter) {
		this.inputStream = inputStream;
		this.reporter = reporter;

		// initialize scanner state
		readChar();
	}

	/**
	 * Automatically skips whitespace and builds spelling for the current token.
   * If there is any error, first sanity check: print out the Token and determine if the spelling
   * matches the TokenKind.
   * 
	 */
	public Token scan() {

		// skip whitespace 
		while (!eot && isWhitespace(currentChar)) {
      skipIt();
    }

		// start of a token: collect spelling and identify token kind
		currentSpelling = new StringBuilder();
		TokenKind kind = scanToken();
		String spelling = currentSpelling.toString();

    // handle comment, recursively call scan until comments have ended
    if (kind == TokenKind.COMMENT) {
      // System.out.println("finding end of comment..." + commentMode);
      if (commentMode == Comment.LINEAR) {
        while (currentChar != '\n' && currentChar != '\r' && !eot) {
          skipIt();
        }
      } else { // commentMode == Comment.BLOCK
        while (!eot) {
          if (currentChar == '*') {
            skipIt();
            if (currentChar == '/') {
              skipIt();
              commentMode = Comment.NONE;
              break;
            }
          } else {
            skipIt();
          }
        } 
        // edge case where multiline comment hasn't terminated by EOT
        if (eot && commentMode == Comment.BLOCK) {
          scanError("Expected `*/` to close the multiline comment before the end of file.");
        }
      }

      // System.out.println("exiting comment...");
      commentMode = Comment.NONE; // not needed, we 
      return scan();
    }

    if (kind == TokenKind.ERROR) {
      scanError("Attempting to scan '" + spelling + "' but could not find a match.");
    }

		// return new token
    Token t = new Token(kind, spelling);
    // t.print();
		return t;
	}

  /**
   * Reads input stream and determines if it matches any token.
   * @return TokenKind identified
   */
  public TokenKind scanToken() {
    if (eot) {
      return TokenKind.EOT;
    }

    // identifiers and keywords
    if (isLetter(currentChar)) {// in regular Java _varName is allowed, miniJava pa1 says it must start w/ letter
      takeTill( (c) -> { return isLetter(c) || isDigit(c) || c == '_'; });
      return TokenKind.ID;
    }

    switch (currentChar) {
      // for sure syntactic symbols
      case ';':
        takeIt();
        return TokenKind.SEMICOLON;
      case ',':
        takeIt();
        return TokenKind.COMMA;
      case '.':
        takeIt();;
        return TokenKind.PERIOD;

      // brackets
      case '(':
        takeIt();
        return TokenKind.LPAREN;
      case ')':
        takeIt();
        return TokenKind.RPAREN;
      case '{':
        takeIt();
        return TokenKind.LCURLY;
      case '}':
        takeIt();
        return TokenKind.RCURLY;
      case '[':
        takeIt();
        return TokenKind.LSQUARE;
      case ']':
        takeIt();
        return TokenKind.RSQUARE;
        
      // binary and unary operators ( starters(unop) \subset starters(binop) )
      case '+':
        takeIt();
        return TokenKind.ADD;
      case '-':
        takeIt();
        return TokenKind.MINUS;
      case '*':
        takeIt();
        return TokenKind.MULTIPLY;
      case '/':
        takeIt();
        if (currentChar == '*') {
          takeIt();
          commentMode = Comment.BLOCK;
          return TokenKind.COMMENT;
        } else if (currentChar == '/') {
          takeIt();
          commentMode = Comment.LINEAR;
          return TokenKind.COMMENT;
        }
        return TokenKind.DIVIDE;
      case '<':
        takeIt();
        if (currentChar == '=') {
          takeIt();
          return TokenKind.LEQ;
        }
        return TokenKind.LT;
      case '>':
        takeIt();
        if (currentChar == '=') {
          takeIt();
          return TokenKind.GEQ;
        }
        return TokenKind.GT;
      case '=':
        takeIt();
        if (currentChar == '=') {
          takeIt();
          return TokenKind.EQUALS;
        }
        return TokenKind.ASSIGN;
      case '!':
        takeIt();
        if (currentChar == '=') {
          takeIt();
          return TokenKind.NEQ;
        }
        return TokenKind.NOT;
      case '&':
        takeIt();
        if (currentChar == '&') {
          takeIt();
          return TokenKind.AND;
        }
        return TokenKind.ERROR;
      case '|':
        takeIt();
        if (currentChar == '|') {
          takeIt();
          return TokenKind.OR;
        }
        return TokenKind.ERROR;

      // numbers
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        takeTill( (c) -> { return isDigit(c); } );
        return TokenKind.NUM;

      default:
        scanError("Unknown '" + currentChar +"' character was read.");
        return TokenKind.ERROR;
    }

  }

  /*
  It seems like takeMany(amount), takeRest() (until whitespace) are all
  bad ideas as what if it takes too many or too little
  */

  /**
	 * Appends the current char onto the current spelling and goes to the next char in stream.
	 */
  private void takeIt() {
		currentSpelling.append(currentChar);
		nextChar();
	}

  /**
	 * Like takeIt() but takes in a boolean function taking in a char argument to see whether
	 * it should continue 
	 */
	private void takeTill(CharIs function) {
    while (function.isValid(currentChar)) {
      takeIt();
    }
    return;
	}

	private void skipIt() {
		nextChar();
	}

  private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}

  private boolean isLetter(char c) {
    return isLowercase(c) || isUppercase(c);
  }

  private boolean isUppercase(char c) {
    return c >= 'A' && c <= 'Z';
  }

  private boolean isLowercase(char c) {
    return c >= 'a' && c <= 'z';
  }

  private boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
  }

  /**
	 * advance to next char in inputstream
	 * detect end of file or end of line as end of input
	 */
	private void nextChar() {
		if (!eot)
			readChar();
	}

	private void readChar() {
		try {
			int c = inputStream.read();
			currentChar = (char) c;
			if (c == -1) {
				eot = true;
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			eot = true;
		}
	}

  private void scanError(String m) {
		reporter.reportError("Scan Error:  " + m);
	}
}

@FunctionalInterface
interface CharIs {
  boolean isValid(char c);
}
