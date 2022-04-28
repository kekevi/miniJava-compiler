package miniJava.ContextualAnalyzer;

import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.Predefined;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Identification implements Visitor<WatchOut, Object> { // just <ArgType, ReturnType> = <Object, Object> because it shouldn't return anything and it doesn't pass anything (most of the time)
  private AST ast;
  private ErrorReporter reporter;
  protected IdTable env;
  private Context context;
  private boolean hasMain;
  private ArrayList<FieldDeclInit> delayedFieldInit;

  private static final WatchOut none = WatchOut.None;

  public Identification(AST ast, ErrorReporter reporter) {
    this.ast = ast;
    this.reporter = reporter;
    this.env = new IdTable();
    this.context = new Context();
    this.hasMain = false;
    this.delayedFieldInit = new ArrayList<FieldDeclInit>();
  }

  // main method: modifies the passed in AST by decorating it
  public AST identify() {
    ast.visit(this, none);
    return ast;
  }

  //
  // error management
  //
  private String prefix(SourcePosition posn) {
    return "*** line " + posn.start + ": ";
  }

  // private void check(boolean result) {
  //   check(result, "Unspecified Identification Error!");
  // }

  private void check(boolean result, String err) {
    if (!result) {
      reporter.reportError(err);
    }
  }

  private Declaration check(Declaration decl, String err) {
    if (decl == null) {
      reporter.reportError(err);
      return null;
    }
    return decl;
  }

  // helper
  // private boolean isMain(MethodDecl method) { // moved definition to MethodDecl as static
  //   // public static void main(String[] args)
  //   return (
  //     !method.isPrivate // private
  //     && method.isStatic // static
  //     && method.type.typeKind == TypeKind.VOID // void
  //     && method.name.equals("main")  // main
  //     && method.parameterDeclList.size() == 1 // (??? args)
  //     && method.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY // (??[] args)
  //     && ((ArrayType) method.parameterDeclList.get(0).type).eltType.typeKind == TypeKind.CLASS // (?[] args)
  //     && ((ClassType) ((ArrayType) method.parameterDeclList.get(0).type).eltType).className.spelling.equals("String") // (String[] args)
  //   );
  // }

  @Override
  public Object visitPackage(Package prog, WatchOut arg) {
    env.addScope(); // add CLASS scope

    // premade classes
    env.addClassDecl("String", Predefined.string);
    env.addClassDecl("_PrintStream", Predefined._printstream);
    env.addClassDecl("System", Predefined.system);

    // we must pre-add all classes first so ClassIds can be filled
    for (ClassDecl clas : prog.classDeclList) {
      check(env.addClassDecl(clas.name, clas), prefix(clas.posn) + "Duplicate class: '" + clas.name + "' is not allowed @" + clas.posn.toString());
    }

    // actually visit classes
    for (ClassDecl clas : prog.classDeclList) {
      clas.visit(this, WatchOut.None);
    }

    env.removeScope(); // ends CLASS scope

    if (!hasMain) {
      reporter.reportError(prefix(prog.posn) + " each package needs a single main method in miniJava.");
    }
    return null;
  }

  @Override
  public Object visitClassDecl(ClassDecl cd, WatchOut arg) {
    context.inClass(cd);

    env.addScope(); // open MEMBER scope
    // pre-add so all MemberDecls are visible
    for (FieldDecl field : cd.fieldDeclList) {
      check(env.addMemberDecl(field.name, field), prefix(field.posn) + "Duplicate member name: '" + field.name + "' is not allowed @" + field.posn.toString());
    }
    for (MethodDecl method : cd.methodDeclList) {
      check(env.addMemberDecl(method.name, method), prefix(method.posn) + "Duplicate member name: '" + method.name + "' is not allowed @" + method.posn.toString());
    }

    // actually visit to link
    for (FieldDecl field : cd.fieldDeclList) {
      field.visit(this, none);
    }
    for (MethodDecl method : cd.methodDeclList) {
      method.visit(this, none);
      if (MethodDecl.isMain(method)) {
        if (hasMain) { // already has a `main` method
          reporter.reportError(prefix(method.posn) + "main method has already been defined. miniJava only accepts one main method per package.");
        }
        hasMain = true;
      } 
    }
    cd.constructor.visit(this, none);

    env.removeScope(); // ends MEMBER scope
    return null;
  }

  @Override
  public Object visitFieldDecl(FieldDecl fd, WatchOut arg) {
    // no identifier to link to declaration here (cause this is the decl)
    if (!fd.isStatic) {
      context.currentClass().instanceSize += 1; // for PA4 code gen
    }
    fd.type.visit(this, none); // type: TypeDenoter, which each impl of TypeDenoter implements linking
    return null;
  }

  @Override
  public Object visitFieldDeclInit(FieldDeclInit fd, WatchOut arg) {
    if (!fd.isStatic) {
      context.currentClass().instanceSize += 1;
      reporter.reportError(prefix(fd.posn) + "miniJava does not currently support instance field initialization.");
    }
    fd.type.visit(this, none);

    context.inField(fd);
    fd.init.visit(this, none); //TODO: right now I am going to just assume this works
    return null;
  }

  @Override
  public Object visitMethodDecl(MethodDecl md, WatchOut arg) {
    context.inMethod(md);

    md.type.visit(this, none);
    env.addScope(); // open PARAM scope
    for (ParameterDecl param : md.parameterDeclList) {
      param.visit(this, none); // params (and locals) will add themselves to the env, unlike members and classes, which get added by their parent node
    }
    env.addScope(); // open 1st LOCAL scope
    for (Statement statement : md.statementList) {
      context.statementReset();
      statement.visit(this, none);
    }
    env.removeScope(); // ends 1st LOCAL scope
    env.removeScope(); // ends PARAM scope
    return null;
  }

  @Override
  public Object visitConstructorDecl(ConstructorDecl constructor, WatchOut arg) {
    context.inMethod(constructor);
    constructor.type.visit(this, none);

    env.addScope(); // open PARAM scope
    for (ParameterDecl param : constructor.pl) {
      param.visit(this, none);
    }
    env.addScope(); // open 1st LOCAL scope
    for (Statement statement : constructor.sl) {
      context.statementReset();
      statement.visit(this, WatchOut.Constructor);
    }
    env.removeScope(); // ends 1st LOCAL scope
    env.removeScope(); // ends PARAM scope
    return null;
  }

  @Override
  public Object visitParameterDecl(ParameterDecl pd, WatchOut arg) {
    pd.type.visit(this, none); // 1st. check if param type is valid
    check(env.addParamDecl(pd.name, pd), prefix(pd.posn) + "Duplicate parameter '" + pd.name + "' is not allowed @" + pd.posn.toString()); // 2nd. check param isn't duped
    return null;
  }

  @Override
  public Object visitVarDecl(VarDecl var, WatchOut arg) {
    var.type.visit(this, none);
    check(env.addVarDecl(var.name, var), prefix(var.posn) + "Duplicate local variable name: '" + var.name + "' is not allowed @" + var.posn.toString() + "\n\t"
                                          + "NOTE: Java(and miniJava) does not allow local variable & parameter shadowing...");
    return null;
  }

  @Override
  public Object visitArrayLengthDecl(ArrayLengthDecl decl, WatchOut arg) {
    System.out.println("Should not be able to get here.");
    return null;
  }

  @Override
  public Object visitBaseType(BaseType type, WatchOut arg) {
    // primative types are always valid! and there are no more subnodes
    return null;
  }

  @Override
  public Object visitClassType(ClassType type, WatchOut arg) {
    ClassDecl clas = (ClassDecl) check(env.getClassDecl(type.className.spelling), prefix(type.posn) + "Undefined class: Cannot find class '" + type.className.spelling + "' @" + type.posn.toString());
    if (arg == WatchOut.Constructor && clas == context.currentClass()) {
      reporter.reportError(prefix(type.posn) + "cannot recursively call a constructor! (do you want a HeapOverflow?!)");
    }
    type.className.decl = clas; // we could combine this into one line too!
    return null;
  }

  @Override
  public Object visitArrayType(ArrayType type, WatchOut arg) {
    type.eltType.visit(this, none); // arraytypes just point to another type
    return null;
  }

  @Override
  public Object visitBlockStmt(BlockStmt block, WatchOut arg) {
    env.addScope(); // add n+1 LOCAL scope
    
    for (Statement statement : block.sl) {
      context.statementReset();
      statement.visit(this, none);
    }

    env.removeScope(); // ends n+1 LOCAL scope
    return null;
  }

  @Override
  public Object visitVarDeclStmt(VarDeclStmt stmt, WatchOut arg) {
    if (arg != null && arg == WatchOut.VarDeclStmt) {
      reporter.reportError(prefix(stmt.posn) + "Short conditional declaration: Short conditional statements do not allow variable declarations.");
    } 

    stmt.varDecl.visit(this, none);
    context.setDeclaring(stmt.varDecl.name);
    stmt.initExp.visit(this, WatchOut.VarDeclStmt); // even though it makes sense to evaluate expression first, Java doesn't allow `int x = x` even if x is a field variable
    context.setDeclaring(null); // just in case
    // if (stmt.varDecl.name.equals(context.takeDefining())) {
    //   reporter.reportError(prefix(stmt.posn) + "cannot use variable name in the midst of declaring '" + stmt.varDecl.name + "'. You may've forgotten the `this.`");
    // }
    return null;
  }

  @Override
  public Object visitAssignStmt(AssignStmt stmt, WatchOut arg) {
    stmt.val.visit(this, none);
    stmt.ref.visit(this, WatchOut.AssignStmt);
    return null;
  }

  @Override
  public Object visitIxAssignStmt(IxAssignStmt stmt, WatchOut arg) {
    stmt.exp.visit(this, none);
    stmt.ref.visit(this, WatchOut.ArrayAssignStmt); // I don't think the order matters here
    stmt.ix.visit(this, none); // I don't think index checking is done in identification or type checking
    return null;
  }

  @Override
  public Object visitCallStmt(CallStmt stmt, WatchOut arg) {
    for (Expression expr : stmt.argList) {
      expr.visit(this, none);
    }
    stmt.methodRef.visit(this, WatchOut.MethodCall); // TODO: specifically a method reference, might have some special check? potentially pass it as an arg to visit function
    return null;
  }

  @Override
  public Object visitReturnStmt(ReturnStmt stmt, WatchOut arg) {
    if (arg == WatchOut.Constructor) {
      reporter.reportError(prefix(stmt.posn) + "cannot return from a constructor!");
      return null;
    }
    stmt.ofMethod = context.currentMethod();
    if (stmt.returnExpr != null) {
      stmt.returnExpr.visit(this, none); // in type checking: check that return statement must be void if current method returns void!
    }
    return null;
  }

  @Override
  public Object visitIfStmt(IfStmt stmt, WatchOut arg) {
    /* 
      oh wait it makes sense that you can't declare a variable in a oneliner conditional because then you can't guarantee 
      that the variable's location exists or not at compile time
      -- it's better to fail early in this case
    */
    stmt.cond.visit(this, none);
    stmt.thenStmt.visit(this, WatchOut.VarDeclStmt);
    if (stmt.elseStmt != null) {
      stmt.elseStmt.visit(this, WatchOut.VarDeclStmt);
    }
    return null;
  }

  @Override
  public Object visitWhileStmt(WhileStmt stmt, WatchOut arg) {
    stmt.cond.visit(this, none);
    stmt.body.visit(this, WatchOut.VarDeclStmt);
    return null;
  }

  /** checks for valid non-body statements in for loops
   * @param s internal statement of ForStmt
   * @param isInit true if testing forStmt.cond, false if testing forStmt.increment
   */
  private boolean checkValidForLoop(Statement s, boolean isInit) {
    /* 
      init statements can only be:
        in miniJava: VarDeclStmt, AssignStmt, IxAssignStmt, CallStmt
        not in miniJava: NewObjectStmt, In/DecrementStatements

      update statements can only be:
        same as above but cannot be VarDeclStmt!!

      with so many exceptions, I will not use a strict visitor pattern
    */
    boolean valid;
    if (isInit) {
      valid = s instanceof VarDeclStmt || s instanceof AssignStmt || s instanceof IxAssignStmt || s instanceof CallStmt;
      if (!valid) {
        reporter.reportError(prefix(s.posn) + "initialization statement of for loop can only declare a variable, assign to a variable, or call a method.");
      }
    } else {
      valid = s instanceof AssignStmt || s instanceof IxAssignStmt || s instanceof CallStmt;
      if (!valid) {
        reporter.reportError(prefix(s.posn) + "update statement of for loop can only assign to a variable or call a statement.");
      }
    }
    return valid;
  }

  @Override
  public Object visitForStmt(ForStmt stmt, WatchOut arg) {
    // we must wrap for Statement in its own scope
    env.addScope();
    
    // for loops suck: https://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.14
    if (stmt.hasInit()) {
      checkValidForLoop(stmt.init, true);
      stmt.init.visit(this, none);
    }

    if (stmt.hasCond()) {
      stmt.cond.visit(this, none);
    }

    if (stmt.hasUpdate()) {
      // NOTE: below is unneccessary as for loop's update statement is a subset of regular Statement, hence Parser will catch problems
      checkValidForLoop(stmt.update, false);
      stmt.update.visit(this, WatchOut.VarDeclStmt); // don't need WatchOut because `checkValidForLoop` does it
    }
    stmt.body.visit(this, WatchOut.VarDeclStmt);

    env.removeScope();
    return null;
  }

  @Override
  public Object visitUnaryExpr(UnaryExpr expr, WatchOut arg) {
    expr.expr.visit(this, none);
    return null;
  }

  @Override
  public Object visitBinaryExpr(BinaryExpr expr, WatchOut arg) {
    expr.left.visit(this, none);
    expr.right.visit(this, none);
    return null;
  }

  @Override
  public Object visitRefExpr(RefExpr expr, WatchOut arg) {
    expr.ref.visit(this, none);
    return null;
  }

  @Override
  public Object visitIxExpr(IxExpr expr, WatchOut arg) {
    expr.ixExpr.visit(this, none);
    expr.ref.visit(this, WatchOut.Array);
    return null;
  }

  @Override
  public Object visitCallExpr(CallExpr expr, WatchOut arg) {
    for (Expression argExpr : expr.argList) {
      argExpr.visit(this, none);
    }
    expr.functionRef.visit(this, WatchOut.MethodCall);
    return null;
  }

  @Override
  public Object visitLiteralExpr(LiteralExpr expr, WatchOut arg) {
    expr.lit.visit(this, none);
    return null;
  }

  @Override
  public Object visitNewObjectExpr(NewObjectExpr expr, WatchOut arg) {
    for (Expression argExpr : expr.argList) {
      argExpr.visit(this, none);
    }
    expr.classtype.visit(this, arg);
    return null;
  }

  @Override
  public Object visitNewArrayExpr(NewArrayExpr expr, WatchOut arg) {
    expr.eltType.visit(this, none);
    expr.sizeExpr.visit(this, none);
    return null;
  }

  @Override
  public Object visitThisRef(ThisRef ref, WatchOut arg) {
    if (context.isStaticContext()) {
      reporter.reportError(prefix(ref.posn) + "'this' cannot be used in static context!");
    }

    switch (arg) {
      case MethodCall:
      case TypeRef: {
        reporter.reportError(prefix(ref.posn) + "'this()' call can only be used with constructors! (NOT IMPLEMENTED YET) caller:" + arg.name());
        break;
      }
      case ArrayAssignStmt:
      case AssignStmt: {
        reporter.reportError(prefix(ref.posn) + "'this' cannot be assigned to!");
      }
      case QualRef: {
        ref.id = new Identifier(new Token(TokenKind.ID, context.currentClass().name, ref.posn), context.currentClass());
        context.setExternal(context.currentClass());
        break;
      }
      default: { // can only get here through RefExpr?
        ref.id = new Identifier(new Token(TokenKind.ID, context.currentClass().name, ref.posn), context.currentClass());
        context.setExternal(context.currentClass()); // this case happens if we have a statement: `this;`
      }
    }
    return null;
  }

  @Override
  public Object visitIdRef(IdRef ref, WatchOut arg) {
    String symbol = ref.id.spelling;
    switch (arg) {
      case MethodCall: {
        // technically none of these casts are needed
        MethodDecl method = (MethodDecl) check(env.getInternalMethod(symbol), prefix(ref.posn) + "'" + symbol + "'' is not a defined method.");
        ref.id.decl = method;
        context.setExternal(lookupClassType(method.type));
        break;
      }
      // case TypeRef: {
      //   ref.id.decl = (ClassDecl) check(env.getClassDecl(symbol), prefix(ref.posn) + "'" + symbol + "'' is not a defined class.");
      //   break;
      // }
      case QualRef: {
        // may either be a local variable, a field, or a class
          // i don't think we allow for calls in between? eg. Class.method().field  (although you can still set a variable to do this)
        checkDeclarationClash(symbol, ref.posn);
        LocalDecl var = env.getParamOrLocal(symbol);
        if (var != null) {
          ref.id.decl = var;
          context.setExternal(lookupClassType(var.type));
          context.setInstanceEnsured();
          context.checkArray(ref);
          break;
        }

        FieldDecl field = env.getInternalField(symbol);
        if (field != null) {
          if (!field.isStatic && context.isStaticContext()) {
            reporter.reportError(prefix(ref.posn) + "cannot get instance field '" + symbol + "'' in a static context!");
          }
          ref.id.decl = field;
          context.setExternal(lookupClassType(field.type));
          context.setInstanceEnsured();
          context.checkArray(ref);
          break;
        } 

        // finally, check reference to an external class
        ClassDecl external = (ClassDecl) check(env.getClassDecl(symbol), prefix(ref.posn) + "the reference '" + symbol + "' cannot be resolved as a defined class, field, or local variable.");
        ref.id.decl = external;
        context.setExternal(external);
        // ClassDecl clas = env.getClassDecl(symbol);
        // ref.id.decl = clas;
        // if (clas == null) {
        //   reporter.reportError(message);
        // }
        break;
      }
      default: {
        checkDeclarationClash(symbol, ref.posn);
        LocalDecl var = env.getParamOrLocal(symbol); // prefix(ref.posn) + "'" + symbol + "' is not defined local variable or instance field.");
        if (var != null) {
          ref.id.decl = var;
          context.setExternal(lookupClassType(var.type));
          break;
        }

        // we must also check if there is an implict 'this' to check the internal field if local doesn't exist
        FieldDecl field = (FieldDecl) check(env.getInternalField(symbol), prefix(ref.posn) + "the reference '" + symbol + "' cannot be resolved to a field or local variable.");
        if (field != null && !field.isStatic && context.isStaticContext()) {
          reporter.reportError(prefix(ref.posn) + "cannot get instance field '" + symbol + "'' in a static context!");
        } 
        ref.id.decl = check(field, prefix(ref.posn) + "cannot find symbol '" + symbol + "' as a field or local variable.");
        context.setExternal(lookupClassType(field.type));
        break;
      }
    }
    if ((arg == WatchOut.Array || arg == WatchOut.ArrayAssignStmt) && ref.id.decl.type.typeKind != TypeKind.ARRAY) {
      reporter.reportError(prefix(ref.posn) + " is invalid as symbol '" + symbol + "' is not an array.");
    }
    return null;
  }

  @Override
  public Object visitQualRef(QualRef ref, WatchOut arg) {
    // children references are where this reference comes from (ie. left of a chain A.B.C)
    ref.ref.visit(this, WatchOut.QualRef);
    String symbol = ref.id.spelling;
    if (symbol.equals("length") && context.isLastRefArray()) {
      ref.id.decl = new ArrayLengthDecl(context.takeArrayRef(), ref.posn);
      if (arg == WatchOut.AssignStmt) {
        reporter.reportError(prefix(ref.posn) + "length of an array cannot be assigned to.");
      }
      return null; // valid length arg
    }
    switch (arg) {
      case MethodCall: {
        ClassDecl external = context.takeExternal();
        if (external == null) {
          reporter.reportError(prefix(ref.posn) + "method '" + symbol + "' is not defined on a primative result.");
        }
        ref.id.decl = (MethodDecl) check(env.getExternalMethod(symbol, external), prefix(ref.posn) + "the method '" + symbol + "' is not defined with respect to class '" + external.name + "'.");
        break;
      }
      // case TypeRef: {
      //   break;
      // }
      case QualRef: 
      default: {
        ClassDecl external = context.takeExternal();
        if (external == null) {
          reporter.reportError(prefix(ref.posn) + "could not read field '" + symbol + "'.");
        }

        FieldDecl field = (FieldDecl) check(env.getExternalField(symbol, external), prefix(ref.posn) + "cannot find the field '" + symbol + "' of class '" + external == null ? "UNKNOWN" : external.name + "'.");
        if (field != null) {
          if (isUnvisible(external, field)) {
            reporter.reportError(prefix(ref.posn) + "cannot get private field '" + symbol + "'.");
          }
          if (field.isStatic) {
            context.setInstanceEnsured();
            // reporter.reportError(prefix(ref.posn) + "cannot get instance field '" + symbol + "'' in a static context! (referenced as if external)");
          }
          if (context.isInstanceEnsured() == false && !field.isStatic && context.isStaticContext()) {
            reporter.reportError(prefix(ref.posn) + "cannot get instance field '" + symbol + "'' in a static context! (referenced as if external)");
          }
          ref.id.decl = field;
          context.setExternal(lookupClassType(field.type));
          context.checkArray(ref);
          break;
        } 
        break;
      }
      // default: { // end of the chain ~~
      
      // }
    }
    return null;
  }

  @Override
  public Object visitIdentifier(miniJava.AbstractSyntaxTrees.Identifier id, WatchOut arg) {
    System.err.println("[as of PA3:Identification], We should never visit an Identifier: " + id.spelling);
    return null;
  }

  @Override
  public Object visitOperator(Operator op, WatchOut arg) {
    // nothing to check
    return null;
  }

  @Override
  public Object visitIntLiteral(IntLiteral num, WatchOut arg) {
    // nothing to check
    return null;
  }

  @Override
  public Object visitBooleanLiteral(BooleanLiteral bool, WatchOut arg) {
    // nothing to check
    return null;
  }

  @Override
  public Object visitNullLiteral(NullLiteral nulllit, WatchOut arg) {
    // nothing to check
    return null;
  }

  /**
   * checks if the type of some declaration's type field is a class type, if so gets the type
   * @param type
   * @return null if the declaration's type is a primative
   */
  private ClassDecl lookupClassType(TypeDenoter type) {
    if (type.typeKind == TypeKind.CLASS) {
      return env.getClassDecl(((ClassType) type).className.spelling);
    }
    return null;
  }

  /**
   * Used to see if a field/method is accessible in a foreign/qualified reference
   * @param external - returned type of left qualifier
   * @param member - field of left qualifier that we must check to see if our current context can access it
   * @return
   */
  private boolean isUnvisible(ClassDecl external, MemberDecl member) {
    return member.isPrivate && external != context.currentClass(); // TODO: maybe check if we should check by String instead?
  }

  private void checkDeclarationClash(String symbol, SourcePosition refposn) {
    String defining = context.getDeclaring();
    if (symbol.equals(defining)) {
      reporter.reportError(prefix(refposn) + "cannot use variable name in the midst of declaring '" + symbol + "'. You may've forgotten the `this.`");
    } 
  }
}

enum WatchOut {
  None, // equivalent to VarRef too
  VarDeclStmt,
  MethodCall,
  TypeRef,
  QualRef, // (external ref, or this)
  AssignStmt,
  Array,
  ArrayAssignStmt,
  Constructor
}

class Context {
  // about current class
  private ClassDecl self;
  // about current method
  private FieldDecl field;
  private MethodDecl method;
  private ClassDecl external;
  private String defining;
  private boolean isInstanceEnsured;
  private boolean isLastRefArray;
  private NamedRef arrayRef;
  
  public Context() {
    // does nothing, must use methods!
  } 

  public void inClass(ClassDecl clas) {
    this.self = clas;
    this.method = null;
  }
  
  // only ever use either inMethod or inField at a time!
  public void inMethod(MethodDecl method) {
    this.field = null;
    this.method = method;
  }
  public void inMethod(ConstructorDecl constructor) {
    this.field = null;
    this.method = new MethodDecl(constructor, constructor.pl, constructor.sl, constructor.posn); // a pseudo method
  } 
  public void inField(FieldDecl field) {
    this.method = null;
    this.field = field;
  }

  public boolean isStaticContext() {
    if (method == null) {
      return field.isStatic;
    }
    return method.isStatic;
  }

  public ClassDecl currentClass() {
    return this.self;
  }

  public MethodDecl currentMethod() { // used for ReturnStmt
    return this.method;
  }

  public void setExternal(ClassDecl external) {
    this.external = external;
  }

  public ClassDecl takeExternal() {
    ClassDecl external = this.external;
    this.external = null;
    return external;
  }

  public void statementReset() {
    isInstanceEnsured = false;
    isLastRefArray = false;
  }

  public void setInstanceEnsured() {
    isInstanceEnsured = true;
  }

  public boolean isInstanceEnsured() {
    return isInstanceEnsured;
  }

  public void setDeclaring(String name) {
    this.defining = name;
  }

  public String getDeclaring() {
    return this.defining;
  }

  public void checkArray(NamedRef ref) {
    isLastRefArray = ref.getId().decl.type.typeKind == TypeKind.ARRAY;
    if (isLastRefArray) {
      arrayRef = ref;
    }
  }

  public NamedRef takeArrayRef() {
    NamedRef ref = arrayRef;
    arrayRef = null;
    return ref;
  }

  public boolean isLastRefArray() {
    return isLastRefArray;
  }
}