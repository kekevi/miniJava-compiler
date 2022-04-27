package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;

// where the argument TypeDenoter is a type that the children need to 
public class TypeChecking implements Visitor<TypeDenoter, TypeDenoter> {
  private AST ast;
  private ErrorReporter reporter;
  private ReturnContext context;

  // expects an *identified* ast!!
  public TypeChecking(AST ast, ErrorReporter reporter) {
    this.ast = ast;
    this.reporter = reporter;
  }

  // main method
  public AST typeCheck() {
    ast.visit(this, null);
    return ast;
  }

  private String prefix(SourcePosition posn) {
    return "*** line " + posn.start + ": ";
  }

  private String suffix(TypeDenoter left, TypeDenoter right) {
    return " (Expected: " + left.toString() + ", Got: " + right.toString() + ")."; 
  }

  private BaseType ErrorType(SourcePosition posn) {
    return new BaseType(TypeKind.ERROR, posn);
  } 

  private boolean check(TypeDenoter left, TypeDenoter right, String err) {
    if (!left.matches(right)) {
      reporter.reportError(err);
      return false;
    }
    return true;
  }

  private TypeDenoter ResultType(TypeDenoter type, SourcePosition posn) {
    switch (type.typeKind) {
      case INT:
      case BOOLEAN:
        return new BaseType(type.typeKind, posn);
      case CLASS: {
        ClassType result = (ClassType) type;
        return new ClassType(result.className, posn);
      }
      case ARRAY: {
        ArrayType result = (ArrayType) type;
        return new ArrayType(result.eltType, posn);
      }
      default:
        return ErrorType(posn);
    }
  }

  @Override
  public TypeDenoter visitPackage(Package prog, TypeDenoter arg) {
    for (ClassDecl clas : prog.classDeclList) {
      clas.visit(this, null);
    }
    return null;
  }

  @Override
  public TypeDenoter visitClassDecl(ClassDecl cd, TypeDenoter arg) {
    for (FieldDecl field : cd.fieldDeclList) {
      field.visit(this, null);
    }
    for (MethodDecl method : cd.methodDeclList) {
      method.visit(this, null);
    }
    return null;
  }

  @Override
  public TypeDenoter visitFieldDecl(FieldDecl fd, TypeDenoter arg) {
    if (fd.type.typeKind == TypeKind.VOID) {
      reporter.reportError(prefix(fd.posn) + "void fields are not a thing. How did you get here?");
    }
    return fd.type;
  }

  @Override
  public TypeDenoter visitMethodDecl(MethodDecl md, TypeDenoter arg) {
    context = new ReturnContext(md);
    for (Statement statement : md.statementList) {
      statement.visit(this, md.type);
    }

    if (!context.hasReturn()) {
      reporter.reportError(prefix(md.posn) + "method '" + md.name + "' needs a return statement of type '" + md.type.toString() + "'.");
    }
    context = null;
    return md.type;
  }

  @Override
  public TypeDenoter visitParameterDecl(ParameterDecl pd, TypeDenoter arg) {
    if (pd.type.typeKind == TypeKind.VOID) {
      reporter.reportError(prefix(pd.posn) + "void params are not a thing. How did you get here?");
    }
    return pd.type;
  }

  @Override
  public TypeDenoter visitVarDecl(VarDecl var, TypeDenoter arg) {
    if (var.type.typeKind == TypeKind.VOID) {
      reporter.reportError(prefix(var.posn) + "void variables are not a thing. How did you get here?");
    }
    return var.type;
  }

  @Override
  public TypeDenoter visitArrayLengthDecl(ArrayLengthDecl decl, TypeDenoter arg) {
    return decl.type;
  }

  @Override
  public TypeDenoter visitBaseType(BaseType type, TypeDenoter arg) {
    return type;
  }

  @Override
  public TypeDenoter visitClassType(ClassType type, TypeDenoter arg) {
    return type;
  }

  @Override
  public TypeDenoter visitArrayType(ArrayType type, TypeDenoter arg) {
    return type;
  }

  @Override
  public TypeDenoter visitBlockStmt(BlockStmt stmt, TypeDenoter arg) {
    for (Statement statement : stmt.sl) {
      statement.visit(this, arg);
    }
    return null;
  }

  @Override
  public TypeDenoter visitVarDeclStmt(VarDeclStmt stmt, TypeDenoter arg) {
    TypeDenoter right = stmt.initExp.visit(this, null);
    TypeDenoter left = stmt.varDecl.visit(this, null);
    check(left, right, prefix(stmt.posn) + "expression does not match declaration's type" + suffix(left, right));
    return null;
  }

  @Override
  public TypeDenoter visitAssignStmt(AssignStmt stmt, TypeDenoter arg) {
    TypeDenoter right = stmt.val.visit(this, null);
    TypeDenoter left = stmt.ref.visit(this, null);
    check(left, right, prefix(stmt.posn) + "assignment types do not match" + suffix(left, right));
    return null;
  }

  @Override
  public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, TypeDenoter arg) {
    TypeDenoter indexType = stmt.ix.visit(this, null);
    if (indexType.typeKind != TypeKind.INT) {
      reporter.reportError(prefix(stmt.posn) + "array index must be an integer.");
    }

    TypeDenoter right = stmt.exp.visit(this, null);
    TypeDenoter leftMaybeArr = stmt.ref.visit(this, null);
    if (leftMaybeArr.typeKind == TypeKind.ARRAY) {
      TypeDenoter left = ((ArrayType) leftMaybeArr).eltType;  
      check(left, right, prefix(stmt.posn) + "assignment (into array) types do not match" + suffix(left, right));
      return null;
    }
    reporter.reportError(prefix(stmt.posn) + "cannot index into a non-array, let alone assign to it.");
    return ErrorType(stmt.posn); // TODO: should be null?
  }

  @Override
  public TypeDenoter visitCallStmt(CallStmt stmt, TypeDenoter arg) {
    MethodDecl method = (MethodDecl) ((NamedRef) stmt.methodRef).getId().decl; 
    if (method.parameterDeclList.size() != stmt.argList.size()) {
      reporter.reportError(prefix(stmt.posn) + "invalid number of arguments. Type checking for arguments is impossible.");
      return null;
    }

    for (int i = 0; i < method.parameterDeclList.size(); i++) {
      TypeDenoter left = method.parameterDeclList.get(i).type;
      TypeDenoter right = stmt.argList.get(i).visit(this, null); // maybe pass the expected type too?
      check(left, right, prefix(stmt.argList.get(i).posn) + "argument " + i + " has an incorrect type" + suffix(left, right));
    }

    return null;
  }

  @Override
  public TypeDenoter visitReturnStmt(ReturnStmt stmt, TypeDenoter methodType) {
    if (methodType.typeKind == TypeKind.VOID) {
      if (stmt.returnExpr == null) {
        return null; // good
      } 
      reporter.reportError(prefix(stmt.posn) + "should return nothing but something is trying to be returned.");
      return null; // bad
    }
    TypeDenoter right = stmt.returnExpr.visit(this, null);
    TypeDenoter left = methodType; // left <==> expected
    check(left, right, prefix(stmt.posn) + "return value does not match method declaration" + suffix(left, right));
    context.recordReturn();
    return null;
  }

  @Override
  public TypeDenoter visitIfStmt(IfStmt stmt, TypeDenoter arg) {
    if (stmt.cond.visit(this, null).typeKind != TypeKind.BOOLEAN) {
      reporter.reportError(prefix(stmt.posn) + "condition must be a boolean.");
    }

    context.inConditional();
    stmt.thenStmt.visit(this, arg);
    context.outConditional();

    if (stmt.elseStmt != null) {
      stmt.elseStmt.visit(this, arg);
    }
    return null;
  }

  @Override
  public TypeDenoter visitWhileStmt(WhileStmt stmt, TypeDenoter arg) {
    if (stmt.cond.visit(this, null).typeKind != TypeKind.BOOLEAN) {
      reporter.reportError(prefix(stmt.posn) + "condition must be a boolean.");
    }
    context.inConditional();

    stmt.body.visit(this, arg);

    context.outConditional();
    return null;
  }

  @Override
  public TypeDenoter visitForStmt(ForStmt stmt, TypeDenoter arg) {
    if (stmt.hasInit()) {
      stmt.init.visit(this, null);
    }

    if (stmt.hasCond()) {
      if (stmt.cond.visit(this, null).typeKind != TypeKind.BOOLEAN) {
        reporter.reportError(prefix(stmt.cond.posn) + "condition must be a boolean.");
      }
    }

    if (stmt.hasUpdate()) {
      stmt.update.visit(this, null);
    }

    context.inConditional();
    stmt.body.visit(this, arg);
    context.outConditional();

    return null;
  }

  @Override
  public TypeDenoter visitUnaryExpr(UnaryExpr expr, TypeDenoter arg) {
    TypeDenoter right = expr.expr.visit(this, null);
    switch (expr.operator.kind) {
      case NOT: {
        if (right.typeKind != TypeKind.BOOLEAN) {
          reporter.reportError(prefix(expr.posn) + "'!' operator only works with boolean. Instead got " + right.toString() + ".");
          return ErrorType(expr.posn);
        }
        return new BaseType(TypeKind.BOOLEAN, expr.posn);
      }
      case MINUS: {
        if (right.typeKind != TypeKind.INT) {
          reporter.reportError(prefix(expr.posn) + "(unary)'-' operator only works with int. Instead got " + right.toString() + ".");
          return ErrorType(expr.posn);
        }
        return new BaseType(TypeKind.INT, expr.posn);
      }
      default:
        reporter.reportError(prefix(expr.posn) + "parser shouldn't've let you got here.");
    }
    return null;
  }

  @Override
  public TypeDenoter visitBinaryExpr(BinaryExpr expr, TypeDenoter arg) {
    TypeDenoter left = expr.left.visit(this, null);
    TypeDenoter right = expr.right.visit(this, null);
    switch (expr.operator.kind) {
      case GT: // int * int -> bool
      case GEQ:
      case LT:
      case LEQ: {
        if (left.typeKind != TypeKind.INT || right.typeKind != TypeKind.INT) {
          reporter.reportError(prefix(expr.posn) + "<, <=, >=, > operators only works with int operands. Instead got" + suffix(left, right));
          return ErrorType(expr.posn);
        }
        return new BaseType(TypeKind.BOOLEAN, expr.posn);
      }
      case ADD: // int * int -> int
      case MINUS:
      case MULTIPLY:
      case DIVIDE: {
        if (left.typeKind != TypeKind.INT || right.typeKind != TypeKind.INT) {
          reporter.reportError(prefix(expr.posn) + "+, -, *, / operators only works with int operands. Instead got" + suffix(left, right));
          return ErrorType(expr.posn);
        }
        return new BaseType(TypeKind.INT, expr.posn);
      }
      case AND: // bool * bool -> bool
      case OR:
      case NOT: {
        if (left.typeKind != TypeKind.BOOLEAN || right.typeKind != TypeKind.BOOLEAN) {
          reporter.reportError(prefix(expr.posn) + "<, <=, >=, >, +, -, *, / operators only works with int operands. Instead got" + suffix(left, right));
          return ErrorType(expr.posn);
        }
        return new BaseType(TypeKind.BOOLEAN, expr.posn);
      }
      case EQUALS: // 'a * 'a -> bool
      case NEQ: {
        check(left, right, prefix(expr.posn) + "equality/inequality operands must be of same type" + suffix(left, right));
        return new BaseType(TypeKind.BOOLEAN, expr.posn);
      }
      default:
        reporter.reportError(prefix(expr.posn) + "parser shouldn't've let you got here.");
    }
    
    return null;
  }

  @Override
  public TypeDenoter visitRefExpr(RefExpr expr, TypeDenoter arg) {
    return expr.ref.visit(this, null);
  }

  @Override
  public TypeDenoter visitIxExpr(IxExpr expr, TypeDenoter arg) {
    TypeDenoter indexType = expr.ixExpr.visit(this, null);
    if (indexType.typeKind != TypeKind.INT) {
      reporter.reportError(prefix(expr.posn) + "array index must be an integer.");
    }

    TypeDenoter maybeArr = expr.ref.visit(this, null);
    if (maybeArr.typeKind == TypeKind.ARRAY) {
      TypeDenoter inner = ((ArrayType) maybeArr).eltType;
      return ResultType(inner, expr.posn);
    }
    reporter.reportError(prefix(expr.posn) + "cannot index into a non-array.");
    return ErrorType(expr.posn);
  }

  @Override
  public TypeDenoter visitCallExpr(CallExpr expr, TypeDenoter arg) {
    MethodDecl method = (MethodDecl) ((NamedRef) expr.functionRef).getId().decl; 
    if (method.parameterDeclList.size() != expr.argList.size()) {
      reporter.reportError(prefix(expr.posn) + "invalid number of arguments. Type checking for arguments is impossible.");
      return ErrorType(expr.posn);
    }

    for (int i = 0; i < method.parameterDeclList.size(); i++) {
      TypeDenoter left = method.parameterDeclList.get(i).type;
      TypeDenoter right = expr.argList.get(i).visit(this, null); // maybe pass the expected type too?
      check(left, right, prefix(expr.argList.get(i).posn) + "argument " + i + " has an incorrect type" + suffix(left, right));
    }

    return method.type;
  }

  @Override
  public TypeDenoter visitLiteralExpr(LiteralExpr expr, TypeDenoter arg) {
    return expr.lit.visit(this, null);
  }

  @Override
  public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, TypeDenoter arg) {
    return expr.classtype.visit(this, null);
  }

  @Override
  public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, TypeDenoter arg) {
    TypeDenoter sizeType = expr.sizeExpr.visit(this, null);
    if (sizeType.typeKind != TypeKind.INT) {
      reporter.reportError(prefix(expr.posn) + "size of new array must be an int, instead got " + sizeType.toString());
    }
    return new ArrayType(expr.eltType, expr.posn);
  }

  @Override
  public TypeDenoter visitThisRef(ThisRef ref, TypeDenoter arg) {
    return new ClassType(ref.id, ref.posn);
  }

  @Override
  public TypeDenoter visitIdRef(IdRef ref, TypeDenoter arg) {
    return ref.id.decl.type;
  }

  @Override
  public TypeDenoter visitQualRef(QualRef ref, TypeDenoter arg) {
    return ref.id.decl.type;
  }

  @Override
  public TypeDenoter visitIdentifier(Identifier id, TypeDenoter arg) {
    // shouldn't get here...?
    System.out.println("visited Identifier " + id.spelling + id.posn.toString());
    return id.decl.type;
  }

  @Override
  public TypeDenoter visitOperator(Operator op, TypeDenoter arg) {
    // maybe I should've generated the ResultType here and have the OpExpr visit here instead to get it
    return null;
  }

  @Override
  public TypeDenoter visitIntLiteral(IntLiteral num, TypeDenoter arg) {
    return new BaseType(TypeKind.INT, num.posn);
  }

  @Override
  public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, TypeDenoter arg) {
    return new BaseType(TypeKind.BOOLEAN, bool.posn);
  }

  @Override
  public TypeDenoter visitNullLiteral(NullLiteral nulllit, TypeDenoter arg) {
    return ErrorType(nulllit.posn);
  }
}

class ReturnContext {
  private boolean satisfied;
  // private boolean pauseChecking; // we pause for if statement checking, cause even if `if (true) {... return}`, it can't ensure return
  //  ^ the problem w/ this is that an `else` inside of an `if` would reenable checking, hence we need a semaphore type lock
  private int condLevel; // the solution!

  public ReturnContext (MethodDecl sig) {
    if (sig.type.typeKind == TypeKind.VOID) {
      this.satisfied = true;
    }
    condLevel = 0;
  }

  private boolean isCheckable() {
    return condLevel == 0;
  }

  public void inConditional() {
    condLevel += 1;
  }

  public void outConditional() {
    condLevel -= 1;
  }

  /** Call only if a return statement is valid (has right typing) */
  public void recordReturn() {
    if (isCheckable()) {
      satisfied = true;
    }
  }

  public boolean hasReturn() {
    return satisfied;
  }
}
