package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

import java.util.EnumSet;

public class Parser {
  private boolean debugMode = false;

  private Scanner scanner;
  private ErrorReporter reporter;
  private Token token;
  private boolean trace = true;
  // TODO: replace all nullpos with actual source positions
  private static final SourcePosition nullpos = null;
  
  public Parser(Scanner scanner, ErrorReporter reporter, boolean debugMode) {
    this.scanner = scanner;
    this.reporter = reporter;
    this.debugMode = debugMode;
  }
  
  public Parser(Scanner scanner, ErrorReporter reporter) {
    this(scanner, reporter, false);
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
  public Package parse() {
    token = scanner.scan();
    try {
      return new Package(parseProgram(), nullpos);
    }
    catch (SyntaxError e) {
      return null;
    }
  }

  // parse Program ::= (ClassDeclaration)* (eot)
  private ClassDeclList parseProgram() throws SyntaxError {
    ClassDeclList classes = new ClassDeclList();
    while (token.kind != TokenKind.EOT) {
      classes.add(parseClassDeclaration());
    }
    accept(TokenKind.EOT);
    return classes;
  }

  private ClassDecl parseClassDeclaration() throws SyntaxError {
    accept(TokenKind.CLASS);
    String classname = accept(TokenKind.ID).spelling;
    accept(TokenKind.LCURLY);

    FieldDeclList fields = new FieldDeclList();
    MethodDeclList methods = new MethodDeclList();
    // while ("starter set of ClassItem") {
    while (token.kind != TokenKind.RCURLY) {
      MemberSpecifier classItem = parseClassItem();

      if (classItem.isFieldDecl) {
        fields.add((FieldDecl) classItem.classItem);
      } else {
        methods.add((MethodDecl) classItem.classItem);
      }
    }

    accept(TokenKind.RCURLY);
    return new ClassDecl(classname, fields, methods, nullpos);
  }

  /** Auxiliary struct to return a MemberDecl along with the specific type */
  class MemberSpecifier {
    MemberDecl classItem;
    boolean isFieldDecl;
    public MemberSpecifier(MemberDecl classItem, boolean isFieldDecl) {
      this.classItem = classItem;
      this.isFieldDecl = isFieldDecl;
    }
  }

  private MemberSpecifier parseClassItem() throws SyntaxError {
    boolean isPrivate = parseVisibility();
    boolean isStatic = parseAccess();

    // void MethodDecl
    if (acceptCheck(TokenKind.VOID)) {
      String methodname = accept(TokenKind.ID).spelling;
      accept(TokenKind.LPAREN);

      ParameterDeclList params = new ParameterDeclList(); // empty by default
      if (token.kind != TokenKind.RPAREN) { // easier than: if (StarterSets.TypeStarters.contains(token.kind)) { // starters(Type) = starters(ParameterList)
        params = parseParamList();
      }
      accept(TokenKind.RPAREN);
      accept(TokenKind.LCURLY);
      // TODO: figure out how to check if Statement exists: // can I do (token.kind != RCURLY), then hope parseStatement() will eventually throw an error?
      // while ("starterSet of Statement that hopefully is disjoint with RCURLY") {
      StatementList statements = new StatementList();
      while (token.kind != TokenKind.RCURLY) {
        statements.add(parseStatement());
      }
      accept(TokenKind.RCURLY);
      return new MemberSpecifier(new MethodDecl(new FieldDecl(isPrivate, isStatic, new BaseType(TypeKind.VOID, nullpos), methodname, nullpos), params, statements, nullpos), false);
    }

    TypeDenoter type = parseType();
    String name = accept(TokenKind.ID).spelling;

    // (typed) FieldDecl
    if (token.kind == TokenKind.SEMICOLON) {
      acceptIt();
      return new MemberSpecifier(new FieldDecl(isPrivate, isStatic, type, name, nullpos), true);
    }

    // typed MethodDecl
    accept(TokenKind.LPAREN);
    ParameterDeclList params = new ParameterDeclList();
    if (token.kind != TokenKind.RPAREN) { // easier than: if (StarterSets.TypeStarters.contains(token.kind)) { // starters(Type) = starters(ParameterList)
      params = parseParamList();
    }
    accept(TokenKind.RPAREN);
    accept(TokenKind.LCURLY);
    // TODO: figure out how to check if Statement exists:
    // while ("starterSet of Statment that hopefully is disjoint with RCURLY") {
    StatementList statements = new StatementList();
    while (token.kind != TokenKind.RCURLY) {
      statements.add(parseStatement());
    }
    accept(TokenKind.RCURLY);
    return new MemberSpecifier(new MethodDecl(new FieldDecl(isPrivate, isStatic, type, name, nullpos), params, statements, nullpos), false);
  }

  /**
   * @return true if the visibility is private, false if protected or public
   * @throws SyntaxError
   */
  private boolean parseVisibility() throws SyntaxError {
    if (token.kind == TokenKind.PUBLIC) {
      acceptIt();
      return false;
    } else if (token.kind == TokenKind.PRIVATE) {
      acceptIt();
      return true;
    }
    if (debugMode) System.out.println(":: Visibility: Epsilon");
    return false;
  }

  /**
   * 
   * @return true if access level is static, false otherwise
   * @throws SyntaxError
   */
  private boolean parseAccess() throws SyntaxError {
    if (token.kind == TokenKind.STATIC) {
      acceptIt();
      return true;
    } 
    if (debugMode) System.out.println(":: Access: Epsilon");
    return false;
  }

  // REVIEW: (1) if you change `parseType` or `parseReference`, you should change `parseStatement`
  private TypeDenoter parseType(Token prev) throws SyntaxError {
    switch (prev.kind) {
      case BOOLEAN:
        return new BaseType(TypeKind.BOOLEAN, nullpos);
      case INT:
      case ID:
        boolean isInt = prev.kind == TokenKind.INT;
        if (token.kind == TokenKind.LSQUARE) {
          accept(TokenKind.LSQUARE);
          accept(TokenKind.RSQUARE);
          return new ArrayType(isInt ? new BaseType(TypeKind.INT, nullpos) : new ClassType(new Identifier(prev), nullpos), nullpos);
        }
        return isInt ? new BaseType(TypeKind.INT, nullpos) : new ClassType(new Identifier(prev), nullpos);
      default:
        parseError("Invalid Type: Expecting int, boolean, identifier, or (int|id)[], but found: " + prev.kind);
        return null;
    }
  }

  private TypeDenoter parseType() throws SyntaxError {
    return parseType(acceptIt());
  }

  private ParameterDeclList parseParamList() throws SyntaxError {
    ParameterDeclList params = new ParameterDeclList();

    TypeDenoter first_type =  parseType();
    String first_name = accept(TokenKind.ID).spelling;
    params.add(new ParameterDecl(first_type, first_name, nullpos));

    while (acceptCheck(TokenKind.COMMA)) {
      TypeDenoter other_type = parseType();
      String other_name = accept(TokenKind.ID).spelling;
      params.add(new ParameterDecl(other_type, other_name, nullpos));
    }

    return params;
  }
  
  private ExprList parseArgList() throws SyntaxError {
    ExprList expressions = new ExprList();

    expressions.add(parseExpression());
    while (acceptCheck(TokenKind.COMMA)) {
      expressions.add(parseExpression());
    }
    return expressions;
  }

  // REVIEW: (1) if you change `parseType` or `parseReference`, you should change `parseStatement`
  private Reference parseReference(Reference innerRef) throws SyntaxError {
    Reference left;
    // if (! (acceptCheck(TokenKind.THIS) || acceptCheck(TokenKind.ID))) { error! }
    if (token.kind == TokenKind.THIS && innerRef == null) {
      acceptIt();
      left = new ThisRef(nullpos);
    } else if (token.kind == TokenKind.THIS && innerRef != null) {
      parseError("Invalid Reference: `this` cannot be an attribute of another Reference. (inside parseReference)");    
      return null;
    } else if (innerRef != null && token.kind != TokenKind.THIS) {
      left = new QualRef(innerRef, new Identifier(accept(TokenKind.ID)), nullpos);
    } else { // innnerRef == null && token.kind != THIS
      left = new IdRef(new Identifier(accept(TokenKind.ID)), nullpos);
    }

    Reference right;
    while (acceptCheck(TokenKind.PERIOD)) {
      right = new QualRef(left, new Identifier(accept(TokenKind.ID)), nullpos); // righter/specific-er ref is higher on AST
      left = right;
    }

    return left;
  }

  private Reference parseReference() throws SyntaxError {
    return parseReference(null);
  }

  private Statement parseStatement() throws SyntaxError {
    // checks RETURN, IF, WHILE, and LCURLY cases
    switch (token.kind) {
      // ReturnStmt
      case RETURN:
        acceptIt();
        Expression toReturn = null;
        if (!acceptCheck(TokenKind.SEMICOLON)) {
          toReturn = parseExpression();
          accept(TokenKind.SEMICOLON);
        }
        return new ReturnStmt(toReturn, nullpos);

      // IfStmt
      case IF:
        acceptIt();
        accept(TokenKind.LPAREN);
        Expression ifCondition = parseExpression();
        accept(TokenKind.RPAREN);
        Statement ifBlock = parseStatement();
        Statement elseBlock = null;
        if (acceptCheck(TokenKind.ELSE)) {
          elseBlock = parseStatement();
        }
        return new IfStmt(ifCondition, ifBlock, elseBlock, nullpos);

      // WhileStmt
      case WHILE:
        acceptIt();
        accept(TokenKind.LPAREN);
        Expression whileCondition = parseExpression();
        accept(TokenKind.RPAREN);
        Statement whileBlock = parseStatement();
        return new WhileStmt(whileCondition, whileBlock, nullpos);

      // BlockStmt
      case LCURLY:
        acceptIt();
        StatementList statements = new StatementList();
        while (token.kind != TokenKind.RCURLY) {
          statements.add(parseStatement());
        }
        accept(TokenKind.RCURLY);
        return new BlockStmt(statements, nullpos);

      case THIS:
        Reference left = parseReference();
        Statement refStatement = parseStatement_Reference(left);
        return refStatement;
    }

    // oh no! both TypeStarters and ReferenceStarters can include ID
    // we parse an additional char before determining Type- or Reference- Statements
    // REVIEW: (1) if you change `parseType` or `parseReference`, you should change this
    if (token.kind == TokenKind.ID) {
      Token firstId = acceptIt();
      // Reference starting statement will only have PERIOD or ASSIGN after first id
      if (acceptCheck(TokenKind.PERIOD)) { // Reference with an attribute
        if (token.kind == TokenKind.THIS) {
          // check added to parseReference(), but kept anyways
          parseError("Invalid Reference (in parseStatement): `this` cannot be an attribute of another reference.");
        }
        Reference fullRef = parseReference(new IdRef(new Identifier(firstId), nullpos)); // guaranteed as current token chain is ID.ID(.ID)* 
        return parseStatement_Reference(fullRef);
      } else if (token.kind == TokenKind.ID) { // two consecutive IDs must be a Statement->Type
        // if (acceptCheck(TokenKind.LSQUARE)) {
        //   accept(TokenKind.RSQUARE);
        // }
        return parseStatement_Type(firstId); 
      } else if (acceptCheck(TokenKind.LSQUARE)) { // by LSQUARE, it is still ambiguous whether it is a Type or Reference
        // NOTE: LL(3) Exception!!, it is much easier to parse Type and Reference on spot!
        if (acceptCheck(TokenKind.RSQUARE)) { 
          // must be Type ~ id [ ] varname = ... special case
          TypeDenoter type = new ArrayType(new ClassType(new Identifier(firstId), nullpos), nullpos);
          return parseStatement_Type(type);
        }  
        // must be Reference ~ id [ Expr ] = ... special case
        Reference reference = new IdRef(new Identifier(firstId), nullpos);
        return parseStatement_Reference_LSQUARE(reference);

      } else { // must be Reference --> (ASSIGN | LPAREN)
        return parseStatement_Reference(new IdRef(new Identifier(firstId), nullpos));
      }
    }

    if (StarterSets.TypeStarters.contains(token.kind)) {
      TypeDenoter type = parseType();
      return parseStatement_Type(type);
    }

    if(StarterSets.ReferenceStarters.contains(token.kind)) {
      Reference reference = parseReference();
      return parseStatement_Reference(reference);
    }

    parseError("Invalid Statement - expected valid command but instead got: " + token.kind);
    return null;
  }
  
  private Statement parseStatement_Type(TypeDenoter type) throws SyntaxError {
    String name = accept(TokenKind.ID).spelling; // second id (varname)
    accept(TokenKind.ASSIGN);
    Expression value = parseExpression();
    accept(TokenKind.SEMICOLON);
    return new VarDeclStmt(new VarDecl(type, name, nullpos), value, nullpos);
  }
  
  private Statement parseStatement_Type(Token typeToken) throws SyntaxError {
    TypeDenoter type = parseType(typeToken);
    return parseStatement_Type(type);
  }

  private Statement parseStatement_Reference(Reference left) throws SyntaxError {
    switch (token.kind) {
      case ASSIGN:
        acceptIt();
        Expression value = parseExpression();
        accept(TokenKind.SEMICOLON);
        return new AssignStmt(left, value, nullpos);
      case LSQUARE:
        acceptIt();
        return parseStatement_Reference_LSQUARE(left);
      case LPAREN:
        acceptIt();
        ExprList args = new ExprList(); // empty default
        if (token.kind != TokenKind.RPAREN) { // EBNF '?'
          args = parseArgList();
        }
        accept(TokenKind.RPAREN);
        accept(TokenKind.SEMICOLON);
        return new CallStmt(left, args, nullpos);
      default:
        parseError("Invalid Statement - After a reference, parser expects ASSIGNS, LSQUARE, or LPAREN");
        return null;
    }
  }

  private Statement parseStatement_Reference_LSQUARE(Reference left) {
    Expression index = parseExpression();
    accept(TokenKind.RSQUARE);
    accept(TokenKind.ASSIGN);
    Expression value = parseExpression();
    accept(TokenKind.SEMICOLON);
    return new IxAssignStmt(left, index, value, nullpos);
  }

  class ExpressionParser {
    // if this was a functional language, I would've generalized these parseE_X_() functions
    // into a list of functions that would try to either parse the next index in the list or
    // return the expression

    public Expression parseE0() throws SyntaxError {
      Expression left = parseE1();
      while (token.kind == TokenKind.OR) {
        // Token op = acceptIt(); Expression right = parseE[i+1]() is integrated below
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE1(), nullpos);
      }
      return left;
    }

    Expression parseE1() throws SyntaxError {
      Expression left = parseE2();
      while (token.kind == TokenKind.AND) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE2(), nullpos);
      }
      return left;
    }

    Expression parseE2() throws SyntaxError {
      Expression left = parseE3();
      while (token.kind == TokenKind.EQUALS || token.kind == TokenKind.NEQ) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE3(), nullpos);
      }
      return left;
    }

    Expression parseE3() throws SyntaxError {
      Expression left = parseE4();
      while (token.kind == TokenKind.LEQ || token.kind == TokenKind.LT
          || token.kind == TokenKind.GEQ || token.kind == TokenKind.GT) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE4(), nullpos);
      }
      return left;
    }

    Expression parseE4() throws SyntaxError {
      Expression left = parseE5();
      while (token.kind == TokenKind.ADD || token.kind == TokenKind.MINUS) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE5(), nullpos);
      }
      return left;
    }

    Expression parseE5() throws SyntaxError {
      Expression left = parseE6();
      while (token.kind == TokenKind.MULTIPLY || token.kind == TokenKind.DIVIDE) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE6(), nullpos);
      }
      return left;
    }

    Expression parseE6() throws SyntaxError { 
      // UnaryExpr
      if (token.kind == TokenKind.NOT || token.kind == TokenKind.MINUS) {
        Token unop = acceptIt();
        return new UnaryExpr(new Operator(unop), parseE6(), nullpos);
      }
      
      return parseE7();
    }

    Expression parseE7() throws SyntaxError { // parens and root
      // (parens) Expression
      if (acceptCheck(TokenKind.LPAREN)) {
        Expression inside = parseE0();
        accept(TokenKind.RPAREN);
        return inside;
      }

      // root expressions:
      return parseRootExpr();
    }

    Expression parseRootExpr() throws SyntaxError {
      // TODO: make sure switch cases don't match starters(Reference)
      boolean matchedSwitch = true;
      switch (token.kind) {
        // LiteralExpr
        //    IntLiteral
        case NUM:
          return new LiteralExpr(new IntLiteral(acceptIt()), nullpos);

        //    BooleanLiteral
        case TRUE:
        case FALSE:
          return new LiteralExpr(new BooleanLiteral(acceptIt()), nullpos);

        // NewExpr
        case NEW:
          acceptIt();

          // NewArrayExpr - Integer
          if (acceptCheck(TokenKind.INT)) {
            accept(TokenKind.LSQUARE);
            Expression arraySize = parseExpression();
            accept(TokenKind.RSQUARE);
            return new NewArrayExpr(new BaseType(TypeKind.INT, nullpos), arraySize, nullpos);
          } 
          
          if (token.kind == TokenKind.ID) {
            Token id = acceptIt();

            // NewObjectExpr
            if (acceptCheck(TokenKind.LPAREN)) {
              accept(TokenKind.RPAREN);
              return new NewObjectExpr(new ClassType(new Identifier(id), nullpos), nullpos);
            // NewArrayExpr - Object
            } else {
              accept(TokenKind.LSQUARE);
              Expression arraySize = parseExpression();
              accept(TokenKind.RSQUARE);
              return new NewArrayExpr(new ClassType(new Identifier(id), nullpos), arraySize, nullpos);
            }
          }
          break;

        // ParenExpr (should be taken by earlier priority)
        // case LPAREN:
        //   acceptIt();
        //   parseExpression();
        //   accept(TokenKind.RPAREN);
        //   break;
        default:
          // check if any of the cases above applied
          matchedSwitch = false;
      }

      if (matchedSwitch) {
        return null; // should not be able to get here...

      // UnaryExpr (should be taken by earlier priority)
      // } else if (StarterSets.Unops.contains(token.kind)) {
      //   acceptIt();
      //   parseExpression();
      } else if (StarterSets.ReferenceStarters.contains(token.kind)) { // parse Reference
        // what if it passes through all the alternations and doesn't get caught??
        // } else { // parse Reference
        Reference ref = parseReference();

        // IxExpr
        if (acceptCheck(TokenKind.LSQUARE)) {
          Expression index = parseExpression();
          accept(TokenKind.RSQUARE);
          return new IxExpr(ref, index, nullpos);

        // CallExpr
        } else if (acceptCheck(TokenKind.LPAREN)) {
          ExprList args = new ExprList();
          if (token.kind != TokenKind.RPAREN) { // EBNF ?
            args = parseArgList();
          }
          accept(TokenKind.RPAREN);
          return new CallExpr(ref, args, nullpos);

        // Ref Expr
        } else { 
          // this may not work if the followers of Expression could be LSQUARE or LPAREN
          // no-op, epsilon case, may need to check followers...
          if (debugMode) System.out.println(":: Expression_Reference : Epsilon");
          return new RefExpr(ref, nullpos);
        }

      } else {
        // none of the required alternations a in Expr := ( a1 | a2 | ... )(binop Expr)* was selected
        parseError("Expecting a valid expression before a binary operator.");
        return null;
      }
    }
  }
  private ExpressionParser ep = new ExpressionParser();

  private Expression parseExpression() throws SyntaxError {
    return ep.parseE0();
  }

  /**
   * accept current token and advance to next token
   */
  private Token acceptIt() throws SyntaxError {
    return accept(token.kind);
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
   * @return the current token
   * @throws SyntaxError  if match fails
   */
  private Token accept(TokenKind expectedTokenKind) throws SyntaxError {
    if (token.kind == expectedTokenKind) {
      if (trace) {
        pTrace();
      }
      Token old = token;
      token = scanner.scan();
      return old;
    }
    else {
      parseError("Expecting '" + expectedTokenKind +
          "' but found '" + token.kind + "' with spelling: " + token.spelling);
      return token;
    }
  }

  /**
   * report parse error and unwind call stack to start of parse
   * @param e  string with error detail
   * @throws SyntaxError
   */
  private void parseError(String e) throws SyntaxError {
    reporter.reportError("Parse error: " + e);
    // TODO: remember to comment this out when submitting
    if (debugMode) for (StackTraceElement stl : Thread.currentThread().getStackTrace()) {
      System.err.println(stl);
    }
    throw new SyntaxError();
  }

  // show parse stack whenever terminal is  accepted
  private void pTrace() {
    StackTraceElement [] stl = Thread.currentThread().getStackTrace();
    if (debugMode) {
      for (int i = stl.length - 1; i > 0 ; i--) {
        if(stl[i].toString().contains("parse"))
          System.out.println(stl[i]);
      }
      System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
      System.out.println();
    }
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