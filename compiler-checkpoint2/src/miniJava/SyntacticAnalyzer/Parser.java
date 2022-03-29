package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

import java.util.EnumSet;

public class Parser {
  private boolean debugMode = false;

  private Scanner scanner;
  private ErrorReporter reporter;
  private Token token; // current token to parse
  private int startpos; // current token.posn (automatically updated by accepting functions)
  private int finishpos; // last token parsed
  private boolean trace = true;
  
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
    int pkg_start = startpos;
    try {
      // should be okay since parseProgram will run first, then finishpos will already be updated --> apparently this is not guaranteed in C
      // also this is what decorators are for! for *extending* functions with extra functionality!
      return new Package(parseProgram(), new SourcePosition(pkg_start, finishpos)); 
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
    int class_start = startpos;
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
    return new ClassDecl(classname, fields, methods, new SourcePosition(class_start, finishpos));
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
    int classitem_start = startpos;
    boolean isPrivate = parseVisibility();
    boolean isStatic = parseAccess();

    // void MethodDecl
    if (acceptCheck(TokenKind.VOID)) {
      int void_pos = finishpos;
      String methodname = accept(TokenKind.ID).spelling;
      accept(TokenKind.LPAREN);

      ParameterDeclList params = new ParameterDeclList(); // empty by default
      if (token.kind != TokenKind.RPAREN) { // easier than: if (StarterSets.TypeStarters.contains(token.kind)) { // starters(Type) = starters(ParameterList)
        params = parseParamList();
      }
      accept(TokenKind.RPAREN);
      accept(TokenKind.LCURLY);
      StatementList statements = new StatementList();
      while (token.kind != TokenKind.RCURLY) {
        statements.add(parseStatement());
      }
      accept(TokenKind.RCURLY);
      return new MemberSpecifier(new MethodDecl(new FieldDecl(isPrivate, isStatic, new BaseType(TypeKind.VOID, new SourcePosition(void_pos)), methodname, new SourcePosition(classitem_start, finishpos)), params, statements, new SourcePosition(classitem_start, finishpos)), false);
    }

    TypeDenoter type = parseType();
    String name = accept(TokenKind.ID).spelling;

    // (typed) FieldDecl
    if (token.kind == TokenKind.SEMICOLON) {
      acceptIt();
      return new MemberSpecifier(new FieldDecl(isPrivate, isStatic, type, name, new SourcePosition(classitem_start, finishpos)), true);
    }

    // typed MethodDecl
    accept(TokenKind.LPAREN);
    ParameterDeclList params = new ParameterDeclList();
    if (token.kind != TokenKind.RPAREN) { // easier than: if (StarterSets.TypeStarters.contains(token.kind)) { // starters(Type) = starters(ParameterList)
      params = parseParamList();
    }
    accept(TokenKind.RPAREN);
    accept(TokenKind.LCURLY);
    StatementList statements = new StatementList();
    while (token.kind != TokenKind.RCURLY) {
      statements.add(parseStatement());
    }
    accept(TokenKind.RCURLY);
    return new MemberSpecifier(new MethodDecl(new FieldDecl(isPrivate, isStatic, type, name, new SourcePosition(classitem_start, finishpos)), params, statements, new SourcePosition(classitem_start, finishpos)), false);
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
    SourcePosition type_pos = new SourcePosition(startpos);
    switch (prev.kind) {
      case BOOLEAN:
        return new BaseType(TypeKind.BOOLEAN, type_pos);
      case INT:
      case ID:
        boolean isInt = prev.kind == TokenKind.INT;
        if (token.kind == TokenKind.LSQUARE) {
          accept(TokenKind.LSQUARE);
          accept(TokenKind.RSQUARE);
          return new ArrayType(isInt ? new BaseType(TypeKind.INT, type_pos) : new ClassType(new Identifier(prev), type_pos), type_pos);
        }
        return isInt ? new BaseType(TypeKind.INT, type_pos) : new ClassType(new Identifier(prev), type_pos);
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

    do {
      int param_start = startpos;
      TypeDenoter type = parseType();
      String name = accept(TokenKind.ID).spelling;
      params.add(new ParameterDecl(type, name, new SourcePosition(param_start, finishpos)));
    } while (acceptCheck(TokenKind.COMMA));

    return params;
  }
  
  private ExprList parseArgList() throws SyntaxError {
    ExprList expressions = new ExprList();

    do {
      expressions.add(parseExpression());
    } while (acceptCheck(TokenKind.COMMA));
    return expressions;
  }

  // REVIEW: (1) if you change `parseType` or `parseReference`, you should change `parseStatement`
  private Reference parseReference(Reference innerRef) throws SyntaxError {
    SourcePosition ref_pos = new SourcePosition(startpos);
    Reference left;
    // if (! (acceptCheck(TokenKind.THIS) || acceptCheck(TokenKind.ID))) { error! }
    if (token.kind == TokenKind.THIS && innerRef == null) {
      acceptIt();
      left = new ThisRef(ref_pos);
    } else if (token.kind == TokenKind.THIS && innerRef != null) {
      parseError("Invalid Reference: `this` cannot be an attribute of another Reference. (inside parseReference)");    
      return null;
    } else if (innerRef != null && token.kind != TokenKind.THIS) {
      left = new QualRef(innerRef, new Identifier(accept(TokenKind.ID)), ref_pos);
    } else { // innnerRef == null && token.kind != THIS
      left = new IdRef(new Identifier(accept(TokenKind.ID)), ref_pos);
    }

    Reference right;
    while (acceptCheck(TokenKind.PERIOD)) {
      right = new QualRef(left, new Identifier(accept(TokenKind.ID)), new SourcePosition(finishpos)); // righter/specific-er ref is higher on AST
      left = right;
    }

    return left;
  }

  private Reference parseReference() throws SyntaxError {
    return parseReference(null);
  }

  private Statement parseStatement() throws SyntaxError {
    int statement_start = startpos;
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
        return new ReturnStmt(toReturn, new SourcePosition(statement_start, finishpos));

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
        return new IfStmt(ifCondition, ifBlock, elseBlock, new SourcePosition(statement_start, finishpos));

      // WhileStmt
      case WHILE:
        acceptIt();
        accept(TokenKind.LPAREN);
        Expression whileCondition = parseExpression();
        accept(TokenKind.RPAREN);
        Statement whileBlock = parseStatement();
        return new WhileStmt(whileCondition, whileBlock, new SourcePosition(statement_start, finishpos));

      // BlockStmt
      case LCURLY:
        acceptIt();
        StatementList statements = new StatementList();
        while (token.kind != TokenKind.RCURLY) {
          statements.add(parseStatement());
        }
        accept(TokenKind.RCURLY);
        return new BlockStmt(statements, new SourcePosition(statement_start, finishpos));

      case THIS:
        Reference left = parseReference();
        Statement refStatement = parseStatement_Reference(left, statement_start);
        return refStatement;
    }

    // oh no! both TypeStarters and ReferenceStarters can include ID
    // we parse an additional char before determining Type- or Reference- Statements
    // REVIEW: (1) if you change `parseType` or `parseReference`, you should change this
    if (token.kind == TokenKind.ID) {
      int tbd_start = startpos;
      Token firstId = acceptIt();
      // Reference starting statement will only have PERIOD or ASSIGN after first id
      if (acceptCheck(TokenKind.PERIOD)) { // Reference with an attribute
        if (token.kind == TokenKind.THIS) {
          // check added to parseReference(), but kept anyways
          parseError("Invalid Reference (in parseStatement): `this` cannot be an attribute of another reference.");
        }
        Reference fullRef = parseReference(new IdRef(new Identifier(firstId), new SourcePosition(tbd_start, finishpos))); // guaranteed as current token chain is ID.ID(.ID)* 
        return parseStatement_Reference(fullRef, statement_start);
      } else if (token.kind == TokenKind.ID) { // two consecutive IDs must be a Statement->Type
        // if (acceptCheck(TokenKind.LSQUARE)) {
        //   accept(TokenKind.RSQUARE);
        // }
        return parseStatement_Type(firstId, statement_start); 
      } else if (acceptCheck(TokenKind.LSQUARE)) { // by LSQUARE, it is still ambiguous whether it is a Type or Reference
        // NOTE: LL(3) Exception!!, it is much easier to parse Type and Reference on spot!
        if (acceptCheck(TokenKind.RSQUARE)) { 
          // must be Type ~ id [ ] varname = ... special case
          TypeDenoter type = new ArrayType(new ClassType(new Identifier(firstId), new SourcePosition(finishpos)), new SourcePosition(tbd_start, finishpos));
          return parseStatement_Type(type, statement_start);
        }  
        // must be Reference ~ id [ Expr ] = ... special case
        Reference reference = new IdRef(new Identifier(firstId), new SourcePosition(tbd_start, finishpos));
        return parseStatement_Reference_LSQUARE(reference, statement_start);

      } else { // must be Reference --> (ASSIGN | LPAREN)
        return parseStatement_Reference(new IdRef(new Identifier(firstId), new SourcePosition(tbd_start, finishpos)), statement_start);
      }
    }

    if (StarterSets.TypeStarters.contains(token.kind)) {
      TypeDenoter type = parseType();
      return parseStatement_Type(type, statement_start);
    }

    if(StarterSets.ReferenceStarters.contains(token.kind)) {
      Reference reference = parseReference();
      return parseStatement_Reference(reference, statement_start);
    }

    parseError("Invalid Statement - expected valid command but instead got: " + token.kind);
    return null;
  }
  
  private Statement parseStatement_Type(TypeDenoter type, int statement_start) throws SyntaxError {
    Token name_token = accept(TokenKind.ID); // second id (varname)
    String name = name_token.spelling;
    int name_pos = name_token.posn.start;
    accept(TokenKind.ASSIGN);
    Expression value = parseExpression();
    accept(TokenKind.SEMICOLON);
    return new VarDeclStmt(new VarDecl(type, name, new SourcePosition(name_pos)), value, new SourcePosition(statement_start, finishpos));
  }
  
  private Statement parseStatement_Type(Token typeToken, int statement_start) throws SyntaxError {
    TypeDenoter type = parseType(typeToken);
    return parseStatement_Type(type, statement_start);
  }

  private Statement parseStatement_Reference(Reference left, int statement_start) throws SyntaxError {
    switch (token.kind) {
      case ASSIGN:
        acceptIt();
        Expression value = parseExpression();
        accept(TokenKind.SEMICOLON);
        return new AssignStmt(left, value, new SourcePosition(statement_start, finishpos));
      case LSQUARE:
        acceptIt();
        return parseStatement_Reference_LSQUARE(left, statement_start);
      case LPAREN:
        acceptIt();
        ExprList args = new ExprList(); // empty default
        if (token.kind != TokenKind.RPAREN) { // EBNF '?'
          args = parseArgList();
        }
        accept(TokenKind.RPAREN);
        accept(TokenKind.SEMICOLON);
        return new CallStmt(left, args, new SourcePosition(statement_start, finishpos));
      default:
        parseError("Invalid Statement - After a reference, parser expects ASSIGNS, LSQUARE, or LPAREN");
        return null;
    }
  }

  private Statement parseStatement_Reference_LSQUARE(Reference left, int statement_start) {
    Expression index = parseExpression();
    accept(TokenKind.RSQUARE);
    accept(TokenKind.ASSIGN);
    Expression value = parseExpression();
    accept(TokenKind.SEMICOLON);
    return new IxAssignStmt(left, index, value, new SourcePosition(statement_start, finishpos));
  }

  class ExpressionParser {
    // if this was a functional language, I would've generalized these parseE_X_() functions
    // into a list of functions that would try to either parse the next index in the list or
    // return the expression
    private int expr_start;
    ExpressionParser(int expr_start) {
      this.expr_start = expr_start;
    }

    public Expression parseE0() throws SyntaxError {
      Expression left = parseE1();
      while (token.kind == TokenKind.OR) {
        // Token op = acceptIt(); Expression right = parseE[i+1]() is integrated below
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE1(), new SourcePosition(expr_start, finishpos));
      }
      return left;
    }

    Expression parseE1() throws SyntaxError {
      Expression left = parseE2();
      while (token.kind == TokenKind.AND) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE2(), new SourcePosition(expr_start, finishpos));
      }
      return left;
    }

    Expression parseE2() throws SyntaxError {
      Expression left = parseE3();
      while (token.kind == TokenKind.EQUALS || token.kind == TokenKind.NEQ) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE3(), new SourcePosition(expr_start, finishpos));
      }
      return left;
    }

    Expression parseE3() throws SyntaxError {
      Expression left = parseE4();
      while (token.kind == TokenKind.LEQ || token.kind == TokenKind.LT
          || token.kind == TokenKind.GEQ || token.kind == TokenKind.GT) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE4(), new SourcePosition(expr_start, finishpos));
      }
      return left;
    }

    Expression parseE4() throws SyntaxError {
      Expression left = parseE5();
      while (token.kind == TokenKind.ADD || token.kind == TokenKind.MINUS) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE5(), new SourcePosition(expr_start, finishpos));
      }
      return left;
    }

    Expression parseE5() throws SyntaxError {
      Expression left = parseE6();
      while (token.kind == TokenKind.MULTIPLY || token.kind == TokenKind.DIVIDE) {
        left = new BinaryExpr(new Operator(acceptIt()), left, parseE6(), new SourcePosition(expr_start, finishpos));
      }
      return left;
    }

    Expression parseE6() throws SyntaxError { 
      // UnaryExpr
      if (token.kind == TokenKind.NOT || token.kind == TokenKind.MINUS) {
        Token unop = acceptIt();
        return new UnaryExpr(new Operator(unop), parseE6(), new SourcePosition(expr_start, finishpos));
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
      int expr_start = startpos;
      // TODO: make sure switch cases don't match starters(Reference)
      boolean matchedSwitch = true;
      switch (token.kind) {
        // LiteralExpr
        //    IntLiteral
        case NUM:
          return new LiteralExpr(new IntLiteral(acceptIt()), new SourcePosition(expr_start, finishpos));

        //    BooleanLiteral
        case TRUE:
        case FALSE:
          return new LiteralExpr(new BooleanLiteral(acceptIt()), new SourcePosition(expr_start, finishpos));

        //    NullLiteral
        case NULL:
          return new LiteralExpr(new NullLiteral(acceptIt()), new SourcePosition(expr_start, finishpos));

        // NewExpr
        case NEW:
          acceptIt();

          // NewArrayExpr - Integer
          int type_pos = startpos;
          if (acceptCheck(TokenKind.INT)) {
            accept(TokenKind.LSQUARE);
            Expression arraySize = parseExpression();
            accept(TokenKind.RSQUARE);
            return new NewArrayExpr(new BaseType(TypeKind.INT, new SourcePosition(startpos)), arraySize, new SourcePosition(expr_start, finishpos));
          } 
          
          if (token.kind == TokenKind.ID) {
            Token id = acceptIt();

            // NewObjectExpr
            if (acceptCheck(TokenKind.LPAREN)) {
              accept(TokenKind.RPAREN);
              return new NewObjectExpr(new ClassType(new Identifier(id), new SourcePosition(type_pos)), new SourcePosition(expr_start, finishpos));
            // NewArrayExpr - Object
            } else {
              accept(TokenKind.LSQUARE);
              Expression arraySize = parseExpression();
              accept(TokenKind.RSQUARE);
              return new NewArrayExpr(new ClassType(new Identifier(id), new SourcePosition(type_pos)), arraySize, new SourcePosition(expr_start, finishpos));
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
          return new IxExpr(ref, index, new SourcePosition(expr_start, finishpos));

        // CallExpr
        } else if (acceptCheck(TokenKind.LPAREN)) {
          ExprList args = new ExprList();
          if (token.kind != TokenKind.RPAREN) { // EBNF ?
            args = parseArgList();
          }
          accept(TokenKind.RPAREN);
          return new CallExpr(ref, args, new SourcePosition(expr_start, finishpos));

        // Ref Expr
        } else { 
          // this may not work if the followers of Expression could be LSQUARE or LPAREN
          // no-op, epsilon case, may need to check followers...
          if (debugMode) System.out.println(":: Expression_Reference : Epsilon");
          return new RefExpr(ref, new SourcePosition(expr_start, finishpos));
        }

      } else {
        // none of the required alternations a in Expr := ( a1 | a2 | ... )(binop Expr)* was selected
        parseError("Expecting a valid expression before a binary operator.");
        return null;
      }
    }
  }

  private Expression parseExpression() throws SyntaxError {
    return (new ExpressionParser(startpos)).parseE0();
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
   * SIDE EFFECTS: startpos and finishpos are updated
   */
  private Token accept(TokenKind expectedTokenKind) throws SyntaxError {
    if (token.kind == expectedTokenKind) {
      if (trace) {
        pTrace();
      }
      Token old = token;
      finishpos = old.posn == null ? finishpos : old.posn.finish;
      token = scanner.scan();
      startpos = token.posn == null ? startpos : token.posn.start;
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