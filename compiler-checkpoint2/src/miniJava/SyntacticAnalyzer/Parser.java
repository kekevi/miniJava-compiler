  package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

import java.util.EnumSet;

public class Parser {

  private Scanner scanner;
  private ErrorReporter reporter;
  private Token token;
  private boolean trace = true;

  public Parser(Scanner scanner, ErrorReporter reporter) {
    this.scanner = scanner;
    this.reporter = reporter;
  }


  /**
   * SyntaxError is used to unwind parse stack when parse fails
   *
   */
  class SyntaxError extends Error {
    private static final long serialVersionUID = 1L;  
  }

  /**
   *  parse input, catch possible parse error
   */
  public void parse() {
    token = scanner.scan();
    try {
      parseProgram();
    }
    catch (SyntaxError e) { }
  }

  // parse Program ::= (ClassDeclaration)* (eot)
  private void parseProgram() throws SyntaxError {
    while (token.kind != TokenKind.EOT) {
      parseClassDeclaration();
    }
    accept(TokenKind.EOT);
  }

  private void parseClassDeclaration() throws SyntaxError {
    accept(TokenKind.CLASS);
    accept(TokenKind.ID);
    accept(TokenKind.LCURLY);
    if (token.kind == TokenKind.RCURLY) { // empty class declaration, quick end
      acceptIt(); 
      return;
    } 

    // TODO:
    // while ("starter set of ClassItem") {
    while (token.kind != TokenKind.RCURLY) {
      parseClassItem();
    }

    accept(TokenKind.RCURLY);
  }

  private void parseClassItem() throws SyntaxError {
    parseVisibility();
    parseAccess();

    if (acceptCheck(TokenKind.VOID)) {
      accept(TokenKind.ID);
      accept(TokenKind.LPAREN);
      if (token.kind != TokenKind.RPAREN) { // easier than: if (StarterSets.TypeStarters.contains(token.kind)) { // starters(Type) = starters(ParameterList)
        parseParamList();
      }
      accept(TokenKind.RPAREN);
      accept(TokenKind.LCURLY);
      // TODO: figure out how to check if Statement exists: // can I do (token.kind != RCURLY), then hope parseStatement() will eventually throw an error?
      // while ("starterSet of Statement that hopefully is disjoint with RCURLY") {
      while (token.kind != TokenKind.RCURLY) {
        parseStatement();
      }
      accept(TokenKind.RCURLY);
      return;
    }

    parseType();
    accept(TokenKind.ID);

    if (token.kind == TokenKind.SEMICOLON) {
      acceptIt();
      return;
    }

    accept(TokenKind.LPAREN);
    if (token.kind != TokenKind.RPAREN) { // easier than: if (StarterSets.TypeStarters.contains(token.kind)) { // starters(Type) = starters(ParameterList)
      parseParamList();
    }
    accept(TokenKind.RPAREN);
    accept(TokenKind.LCURLY);
    // TODO: figure out how to check if Statement exists:
    // while ("starterSet of Statment that hopefully is disjoint with RCURLY") {
    while (token.kind != TokenKind.RCURLY) {
      parseStatement();
    }
    accept(TokenKind.RCURLY);
    return;
  }

  private void parseVisibility() throws SyntaxError {
    if (token.kind == TokenKind.PUBLIC) {
      acceptIt();
      return;
    } else if (token.kind == TokenKind.PRIVATE) {
      acceptIt();
      return;
    }
    System.out.println(":: Visibility: Epsilon");
  }

  private void parseAccess() throws SyntaxError {
    if (token.kind == TokenKind.STATIC) {
      acceptIt();
      return;
    } 
    System.out.println(":: Access: Epsilon");
  }

  // REVIEW: (1) if you change `parseType` or `parseReference`, you should change `parseStatement`
  private void parseType() throws SyntaxError {
    switch (token.kind) {
      case BOOLEAN:
        acceptIt();
        return;
      case INT:
      case ID:
        acceptIt();
        if (token.kind == TokenKind.LSQUARE) {
          accept(TokenKind.LSQUARE);
          accept(TokenKind.RSQUARE);
        }
        return;
      default:
        parseError("Invalid Type: Expecting int, boolean, identifier, or (int|id)[], but found: " + token.kind);
    }
  }

  private void parseParamList() throws SyntaxError {
    parseType();
    accept(TokenKind.ID);
    while (acceptCheck(TokenKind.COMMA)) {
      parseType();
      accept(TokenKind.ID);
    }
  }
  
  private void parseArgList() throws SyntaxError {
    parseExpression();
    while (acceptCheck(TokenKind.COMMA)) {
      parseExpression();
    }
  }

  // REVIEW: (1) if you change `parseType` or `parseReference`, you should change `parseStatement`
  private void parseReference() throws SyntaxError {
    // if (! (acceptCheck(TokenKind.THIS) || acceptCheck(TokenKind.ID))) { error! }
    if (token.kind == TokenKind.THIS) {
      acceptIt();
    } else {
      accept(TokenKind.ID);
    }
    while (acceptCheck(TokenKind.PERIOD)) {
      accept(TokenKind.ID);
    }
  }

  private void parseStatement() throws SyntaxError {
    // checks RETURN, IF, WHILE, and LCURLY cases
    switch (token.kind) {
      case RETURN:
        acceptIt();
        if (!acceptCheck(TokenKind.SEMICOLON)) {
          parseExpression();
          accept(TokenKind.SEMICOLON);
        }
        return;
      case IF:
        acceptIt();
        accept(TokenKind.LPAREN);
        parseExpression();
        accept(TokenKind.RPAREN);
        parseStatement();
        if (acceptCheck(TokenKind.ELSE)) {
          parseStatement();
        }
        return;
      case WHILE:
        acceptIt();
        accept(TokenKind.LPAREN);
        parseExpression();
        accept(TokenKind.RPAREN);
        parseStatement();
        return;
      case LCURLY:
        acceptIt();
        while (token.kind != TokenKind.RCURLY) {
          parseStatement();
        }
        accept(TokenKind.RCURLY);
        return;
      case THIS:
        parseReference();
        parseStatement_Reference();
        return;
    }

    // oh no! both TypeStarters and ReferenceStarters can include ID
    // we parse an additional char before determining Type- or Reference- Statements
    // REVIEW: (1) if you change `parseType` or `parseReference`, you should change this
    if (acceptCheck(TokenKind.ID)) {
      // Reference starting statement will only have PERIOD or ASSIGN after first id
      if (acceptCheck(TokenKind.PERIOD)) { 
        if (token.kind == TokenKind.THIS) {
          parseError("Invalid Reference (in parseStatement): `this` cannot be a attribute of another reference.");
        }
        parseReference(); // should loop within
        parseStatement_Reference();
        return;
      } else if (token.kind == TokenKind.ID) { // two consecutive IDs must be a Statement->Type
        // if (acceptCheck(TokenKind.LSQUARE)) {
        //   accept(TokenKind.RSQUARE);
        // }
        parseStatement_Type(); 
        return;
      } else if (acceptCheck(TokenKind.LSQUARE)) { // by LSQUARE, it is still ambiguous whether it is a Type or Reference
        if (acceptCheck(TokenKind.RSQUARE)) { // must be Type
          parseStatement_Type();
          return;
        }  // must be Reference
        parseStatement_Reference_LSQUARE();
        return;

      } else { // must be Reference --> (ASSIGN | LPAREN)
        parseStatement_Reference();
        return;
      }
    }

    if (StarterSets.TypeStarters.contains(token.kind)) {
      parseType();
      parseStatement_Type();
      return;
    }

    if(StarterSets.ReferenceStarters.contains(token.kind)) {
      parseReference();
      parseStatement_Reference();
      return;
    }

    parseError("Invalid Statement - expected valid command but instead got: " + token.kind);
  }

  private void parseStatement_Type() throws SyntaxError {
    accept(TokenKind.ID);
    accept(TokenKind.ASSIGN);
    parseExpression();
    accept(TokenKind.SEMICOLON);
    return;
  }

  private void parseStatement_Reference() throws SyntaxError {
    switch (token.kind) {
      case ASSIGN:
        acceptIt();
        parseExpression();
        accept(TokenKind.SEMICOLON);
        return;
      case LSQUARE:
        acceptIt();
        parseStatement_Reference_LSQUARE();
        return;
      case LPAREN:
        acceptIt();
        if (token.kind != TokenKind.RPAREN) {
          parseArgList();
        }
        accept(TokenKind.RPAREN);
        accept(TokenKind.SEMICOLON);
        return;
      default:
        parseError("Invalid Statement - After a reference, parser expects ASSIGNS, LSQUARE, or LPAREN");
    }
  }

  private void parseStatement_Reference_LSQUARE() {
    parseExpression();
    accept(TokenKind.RSQUARE);
    accept(TokenKind.ASSIGN);
    parseExpression();
    accept(TokenKind.SEMICOLON);
    return;
  }

  private void parseExpression() throws SyntaxError {
    // DO NOT return; UNTIL END (binop Expression)* regex loop is checked
    // TODO: make sure switch cases don't match starters(Reference)
    boolean matchedSwitch = true;
    switch (token.kind) {
      case NUM:
      case TRUE:
      case FALSE:
        acceptIt();
        break;
      case NEW:
        acceptIt();
        if (acceptCheck(TokenKind.INT)) {
          accept(TokenKind.LSQUARE);
          parseExpression();
          accept(TokenKind.RSQUARE);
        } 
        
        if (acceptCheck(TokenKind.ID)) {
          if (acceptCheck(TokenKind.LPAREN)) {
            accept(TokenKind.RPAREN);
          } else {
            accept(TokenKind.LSQUARE);
            parseExpression();
            accept(TokenKind.RSQUARE);
          }
        }
        break;
      case LPAREN:
        acceptIt();
        parseExpression();
        accept(TokenKind.RPAREN);
        break;
      default:
        // check if any of the cases above applied
        matchedSwitch = false;
    }

    if (matchedSwitch) {

    } else if (StarterSets.Unops.contains(token.kind)) {
      acceptIt();
      parseExpression();
    } else if (StarterSets.ReferenceStarters.contains(token.kind)) { // parse Reference
      // what if it passes through all the alternations and doesn't get caught??
      // } else { // parse Reference
      parseReference();
      if (acceptCheck(TokenKind.LSQUARE)) {
        parseExpression();
        accept(TokenKind.RSQUARE);
      } else if (acceptCheck(TokenKind.LPAREN)) {
        if (token.kind != TokenKind.RPAREN) {
          parseArgList();
        }
        accept(TokenKind.RPAREN);
      } else { 
        // this may not work if the followers of Expression could be LSQUARE or LPAREN
        // no-op, epsilon case, may need to check followers...
        System.out.println(":: Expression_Reference : Epsilon");
      }
    } else {
      // none of the required alternations a in Expr := ( a1 | a2 | ... )(binop Expr)* was selected
      parseError("Expecting a valid expression before a binary operator.");
    }

    while (StarterSets.Binops.contains(token.kind)) {
      acceptIt();
      parseExpression();
    }

    return;
  }

  /**
   * accept current token and advance to next token
   */
  private void acceptIt() throws SyntaxError {
    accept(token.kind);
  }

  /**
   * Accepts the token if current token matched the expected one.
   * @param tokenkind
   * @return 
   *  *true* <-- matches and able to accept ; 
   *  *false* <-- does not match (doesn't accept either)
   * @throws SyntaxError I don't think this is possible...
   */
  private boolean acceptCheck(TokenKind tokenkind) throws SyntaxError {
    if (token.kind == tokenkind) {
      acceptIt();
      return true;
    } else {
      return false;
    }
  }

  /**
   * verify that current token in input matches expected token and advance to next token
   * @param expectedToken
   * @throws SyntaxError  if match fails
   */
  private void accept(TokenKind expectedTokenKind) throws SyntaxError {
    if (token.kind == expectedTokenKind) {
      if (trace)
        pTrace();
      token = scanner.scan();
    }
    else
      parseError("Expecting '" + expectedTokenKind +
          "' but found '" + token.kind + "' with spelling: " + token.spelling);
  }

  /**
   * report parse error and unwind call stack to start of parse
   * @param e  string with error detail
   * @throws SyntaxError
   */
  private void parseError(String e) throws SyntaxError {
    reporter.reportError("Parse error: " + e);
    // TODO: remember to comment this out when submitting
    for (StackTraceElement stl : Thread.currentThread().getStackTrace()) {
      System.err.println(stl);
    }
    throw new SyntaxError();
  }

  // show parse stack whenever terminal is  accepted
  private void pTrace() {
    StackTraceElement [] stl = Thread.currentThread().getStackTrace();
    for (int i = stl.length - 1; i > 0 ; i--) {
      if(stl[i].toString().contains("parse"))
        System.out.println(stl[i]);
    }
    System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
    System.out.println();
  }

}

class StarterSets {
  static final EnumSet<TokenKind> TypeStarters = EnumSet.of(TokenKind.INT, 
                                                            TokenKind.BOOLEAN, 
                                                            TokenKind.ID);
  static final EnumSet<TokenKind> ReferenceStarters = EnumSet.of(TokenKind.ID, 
                                                                 TokenKind.THIS);
  static final EnumSet<TokenKind> Binops = EnumSet.of(TokenKind.ADD, 
                                                      TokenKind.MULTIPLY, 
                                                      TokenKind.DIVIDE,
                                                      TokenKind.LT,
                                                      TokenKind.GT,
                                                      TokenKind.LEQ,
                                                      TokenKind.GEQ,
                                                      TokenKind.EQUALS,
                                                      TokenKind.NEQ,
                                                      TokenKind.AND,
                                                      TokenKind.OR,
                                                      TokenKind.MINUS);
  static final EnumSet<TokenKind> Unops = EnumSet.of(TokenKind.MINUS,
                                                     TokenKind.NOT);
  static EnumSet<TokenKind> initExpressionFollowers() {
    EnumSet<TokenKind> set = EnumSet.of(TokenKind.COMMA,
                                        TokenKind.SEMICOLON,
                                        TokenKind.RSQUARE);
    // set.addAll(StarterSets.Binops); // actually, idk if binops would be in here if Expr must be followed by a RSQUARE
    return set;
  }
  static final EnumSet<TokenKind> ExpressionFollowers = initExpressionFollowers();
}