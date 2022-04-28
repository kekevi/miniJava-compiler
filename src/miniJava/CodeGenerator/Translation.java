package miniJava.CodeGenerator;

import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.Predefined;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.TokenKind;
import miniJava.mJAM.Machine;
import miniJava.mJAM.Machine.Op;
import miniJava.mJAM.Machine.Prim;
import miniJava.mJAM.Machine.Reg;

public class Translation implements Visitor<Heap<Integer>, Object> {
  private final static int UNDEFINED = 0; // MethodDecl.offset = 0 initially, but only mainInvoker can be at CB+0 ==> safe to use 0 as undefined method location
  private final static int TBD = -1; // an address that needs to be patched later
  private final static int FRAME_SIZE = 3;
  private final static int OBJECT_SIZE = 2;

  private final static int STATIC_FIELD_INIT = 0; 
  private final static int LOCAL_VAR_INIT_TAG = 1337; // TESTING: should be 0, but for testing purposes this is easier to see
  public final static int UNINIT_OFFSET = -300; // TESTING: to visualize offset mistakes
  private final static int CLASS_ADDR = -3; 
  private final static int ARRAY_ADDR = -2; // should be identical to tag for array

  // types
  private final static Object THIS_REF = new Object();
  private final static Object CLASS_REF = new Object();
  private final static Object INST_FIELD_REF = new Object();
  private final static Object STACK_REF = null;

  private AST ast;
  private ErrorReporter reporter;
  private ArrayList<Patch> toPatch;

  /*
   *
   *  Methods for External Use 
   * 
   */

  public Translation (AST checkedAst, ErrorReporter reporter) {
    this.ast = checkedAst;
    this.reporter = reporter;
    this.toPatch = new ArrayList<Patch>();
  }

  public void translate() {
    Machine.initCodeGen();
    ast.visit(this, null);
    for (Patch patch : toPatch) {
      Machine.patch(patch.line, patch.decl.offset);
    }
  }

  /*
   *
   * Utilities
   * 
   */

  private static String prefix(SourcePosition posn) {
    return "*** line " + posn.start + ": ";
  }

  
  private void mark(int label) {
    Machine.emit(Op.LOADL, label);
    Machine.emit(Op.POP, 1);
  }

  /** Can make patching slightly quicker as it checks if a method has already been processed. */
  private int addMethodPatch(MethodDecl method) {
    if (method.offset == UNDEFINED) {
      toPatch.add(new Patch(Machine.nextInstrAddr(), method));
      return TBD;
    }
    return method.offset;
  }

  private void Machine_emitPushArgList(ExprList args) {
    for (int i = args.size()-1; i >= 0; i--) {
      args.get(i).visit(this, null);
    }
    return;
  }

  private Object Machine_emitMethodInvocation(ExprList arguments, Reference method_reference) {
    MethodDecl method = (MethodDecl) method_reference.getId().decl;

    // push arguments in reverse order on stack (to match with LB-i offset for ith arg)
    Machine_emitPushArgList(arguments);

    // check for built-in methods
    if (Predefined.isPrintLn(method_reference)) { // TypeChecking should block this for assignment
      Machine.emit(Prim.putintnl);
      return null;
    }

    // static methods do not need a reference of 'this'
    if (method.isStatic) {
      toPatch.add(new Patch(Machine.nextInstrAddr(), method));
      Machine.emit(Op.CALL, Reg.CB, TBD);
      return null;
    }

    // instance methods need to find `this`
    if (method_reference.isQualified()) {
      Object refType = ((QualRef) method_reference).ref.visit(this, null);

      if (refType == CLASS_REF) { // cannot be as Class.method <-- must be static
        reporter.reportError(prefix(method_reference.posn) + "somehow a static method got through.");
        return null;
      } else if (refType == THIS_REF) {
        // this ref already on stack -- how nice!
        toPatch.add(new Patch(Machine.nextInstrAddr(), method));
        Machine.emit(Op.CALLI, Reg.CB, TBD);
        return null;
      } else if (refType == INST_FIELD_REF) {
        // eg. Other.x instanceof Timer; x.counter.tick();
        Machine.emit(Prim.fieldref);
        toPatch.add(new Patch(Machine.nextInstrAddr(), method));
        Machine.emit(Op.CALLI, Reg.CB, TBD);
        return null;
      } else { // STACK_REF
        Machine.emit(Op.LOADI);
        toPatch.add(new Patch(Machine.nextInstrAddr(), method));
        Machine.emit(Op.CALLI, Reg.CB, TBD);
        return null;
      }
    }
    
    // implicit this
    Machine_emitThis();
    toPatch.add(new Patch(Machine.nextInstrAddr(), method));
    Machine.emit(Op.CALLI, Reg.CB, TBD);
    return null;
  }

  /*
   *
   * Declarations - only used for setup, in rest of generation, they should be accessed through ids (do not visit)
   * 
   */

  private int func_main_addr;
  private int ith_statfield = 0;
  @Override
  public Object visitPackage(Package prog, Heap<Integer> arg) {

    // allocate static fields for all classes (no code generated)
    for (ClassDecl clas : prog.classDeclList) {
      int ith_instance = 0;
      for (FieldDecl field : clas.fieldDeclList) {
        if (field.isStatic) {
          Machine.emit(Op.LOADL, STATIC_FIELD_INIT);
          // ^ NOTE: if Machine.nullRep != 0, then you need to change this base on the field's type! (int vs. bool vs. object, etc.)
          field.offset = ith_statfield++;
        } else {
          field.offset = ith_instance++;
        }
      }
    }

    // static_initialization (I am not doing strict Java init order for recursive field refs, it is initialized in order classes appear in file)
    for (ClassDecl clas : prog.classDeclList) {
      for (FieldDecl field : clas.fieldDeclList) {
        field.visit(this, null); // regular FieldDecls should do nothing
      }
    }

    // main_invoker:
    Machine.emit(Op.LOADL, 0);
    Machine.emit(Prim.newarr);
    func_main_addr = Machine.nextInstrAddr();
    Machine.emit(Op.CALL, Reg.CB, TBD);
    Machine.emit(Op.HALT, 0, 0, 0);
    
    // generate code for all methods
    for (ClassDecl clas : prog.classDeclList) {
      clas.visit(this, null);
    }

    return null;
  }

  @Override
  public Object visitClassDecl(ClassDecl cd, Heap<Integer> arg) {
    for (MethodDecl method : cd.methodDeclList) {
      method.visit(this, null);
    }

    if (!cd.hasDefaultConstructor()) { // saves on some code space
      cd.constructor.visit(this, null);
    }

    return null;
  }

  @Override
  public Object visitFieldDecl(FieldDecl fd, Heap<Integer> arg) {
    return null;
  }

  @Override
  public Object visitFieldDeclInit(FieldDeclInit fd, Heap<Integer> arg) {
    fd.init.visit(this, null);
    Machine.emit(Op.STORE, Reg.SB, fd.offset);
    return null;
  }

  @Override
  public Object visitMethodDecl(MethodDecl md, Heap<Integer> arg) {
    md.offset = Machine.nextInstrAddr();
    if (MethodDecl.isMain(md)) { // guaranteed to be 1
      Machine.patch(func_main_addr, md.offset);
    }

    for (int ith_param = 0; ith_param < md.parameterDeclList.size(); ith_param++) {
      // set i=0/first param to LB-1, i=1/second param to LB-2, etc...
      md.parameterDeclList.get(ith_param).offset = -(ith_param+1); 
    }

    Heap<Integer> ith_local = new Heap<Integer>(FRAME_SIZE);
    for (Statement stmt : md.statementList) {
      // it's stmt's role to increment ith_local if creating a new local variable
      stmt.visit(this, ith_local); 
    }

    // if return stmt is dropped in void:, otherwise, it'll jump due to the return right before this
    // we could check for md.returnType.VOID, but it should be okay
    Machine.emit(Op.RETURN, 0, Reg.ZR, md.parameterDeclList.size());
    return null;
  }

  @Override
  public Object visitConstructorDecl(ConstructorDecl cons, Heap<Integer> arg) {
    cons.offset = Machine.nextInstrAddr();

    for (int ith_param = 0; ith_param < cons.pl.size(); ith_param++) {
      // set i=0/first param to LB-1, i=1/second param to LB-2, etc...
      cons.pl.get(ith_param).offset = -(ith_param+1); 
    }

    Heap<Integer> ith_local = new Heap<Integer>(FRAME_SIZE);
    for (Statement stmt : cons.sl) {
      // it's stmt's role to increment ith_local if creating a new local variable
      stmt.visit(this, ith_local); 
    }

    // NOTE: when calling the constructor, please use CALLI and pass the new "this"
    Machine.emit(Op.RETURN, 0, Reg.ZR, cons.pl.size());
    return null;
  }

  @Override
  public Object visitParameterDecl(ParameterDecl pd, Heap<Integer> arg) {
    return null;
  }

  @Override
  public Object visitVarDecl(VarDecl decl, Heap<Integer> arg) {
    return null;
  }

  @Override
  public Object visitArrayLengthDecl(ArrayLengthDecl decl, Heap<Integer> arg) {
    return null;
  }

  /*
   *
   * Statements - after each statement, the expression stack should be cleaned up (unless ofc a variable slot is declared)
   *  
   */

  @Override
  public Object visitBlockStmt(BlockStmt stmt, Heap<Integer> arg) {
    Heap<Integer> ith_local = arg;
    int num_prev_locals = ith_local.val; // standard procedure to start and stop scope
    for (Statement statement : stmt.sl) {
      statement.visit(this, ith_local);
    }
    
    /* you need to pop off the local scope so you don't run out of space during looping */
    // ith_local's value should be changed internally
    Machine.emit(Op.POP, ith_local.val-num_prev_locals);
    ith_local.val = num_prev_locals;
    return null;
  }

  @Override
  public Object visitVarDeclStmt(VarDeclStmt stmt, Heap<Integer> arg) {
    Heap<Integer> ith_local = arg;
    stmt.varDecl.offset = ith_local.val++;

    // Machine.emit(Op.PUSH, 1); // uninitialized
    Machine.emit(Op.LOADL, LOCAL_VAR_INIT_TAG); // initialized
    stmt.initExp.visit(this, null);
    Machine.emit(Op.STORE, Reg.LB, stmt.varDecl.offset);
    return null;
  }

  @Override
  public Object visitAssignStmt(AssignStmt stmt, Heap<Integer> arg) {
    // should not be able to assign to a CLASS_REF or THIS_REF anyways
    stmt.val.visit(this, null);
    Object refType = stmt.ref.visit(this, null);
    if (refType == INST_FIELD_REF) { 
      // stmt.val.visit(this, null); <-- do not do (bad because you call it twice!!! --> newobj cannot be called twice)
      Machine.emit(Op.LOAD, Reg.ST, -3); // we push (a copy of) value back on top 
      Machine.emit(Prim.fieldupd);
      Machine.emit(Op.POP, 1); // to remove the first expr val
      return null;
    // } else if (refType == THIS_REF) { // storing current this val
      
    } else { // STACK_REF || THIS_REF (works as OB should be treated as a value)
      Machine.emit(Op.STOREI);
      return null;
    }
  }

  @Override
  public Object visitIxAssignStmt(IxAssignStmt stmt, Heap<Integer> arg) {
    // should not be assigned to a CLASS_REF or THIS_REF anyways
    Object refType = stmt.ref.visit(this, null);
    if (refType == INST_FIELD_REF) { // we must first get the address of the array
      Machine.emit(Prim.fieldref);
    } else {
      Machine.emit(Op.LOADI);
    }
    stmt.ix.visit(this, null);
    stmt.exp.visit(this, null);
    Machine.emit(Prim.arrayupd);

    return null;
  }

  @Override
  public Object visitCallStmt(CallStmt stmt, Heap<Integer> arg) {
    Machine_emitMethodInvocation(stmt.argList, stmt.methodRef);
    if (stmt.methodRef.getId().decl.type.typeKind != TypeKind.VOID) {
      Machine.emit(Op.POP, 1); // remove return value if non-void method
    }
    return null;
  }

  @Override
  public Object visitReturnStmt(ReturnStmt stmt, Heap<Integer> arg) {
    // Op.RETURN must also clear the arguments put onto the stack
    int num_args = stmt.ofMethod.parameterDeclList.size();

    // non-void
    if (stmt.returnExpr != null) {
      stmt.returnExpr.visit(this, null);
      Machine.emit(Op.RETURN, 1, Reg.ZR, num_args);
    }

    // void
    Machine.emit(Op.RETURN, 0, Reg.ZR, num_args);
    return null;
  }

  @Override
  public Object visitIfStmt(IfStmt stmt, Heap<Integer> arg) {
    Heap<Integer> ith_local = arg;

    stmt.cond.visit(this, null);
    int jumpToElseOrEnd = Machine.nextInstrAddr();
    Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, TBD); // jump to else: or cont:

    stmt.thenStmt.visit(this, ith_local);
    if (stmt.elseStmt == null) {
      Machine.patch(jumpToElseOrEnd, Machine.nextInstrAddr());
      return null;
    } 

    int jumpPastElse = Machine.nextInstrAddr();
    Machine.emit(Op.JUMP, Reg.CB, TBD); // skip over else section if true already activates
    Machine.patch(jumpToElseOrEnd, Machine.nextInstrAddr());
    stmt.elseStmt.visit(this, ith_local); 

    Machine.patch(jumpPastElse, Machine.nextInstrAddr()); 
     
    return null;
  }

  @Override
  public Object visitWhileStmt(WhileStmt stmt, Heap<Integer> arg) {
    Heap<Integer> ith_local = arg;

    int condLine = Machine.nextInstrAddr();
    stmt.cond.visit(this, null);
    int skipBody = Machine.nextInstrAddr();
    Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, TBD);
    stmt.body.visit(this, ith_local);
    Machine.emit(Op.JUMP, Reg.CB, condLine);
    Machine.patch(skipBody, Machine.nextInstrAddr());
    return null;
  }

  @Override
  public Object visitForStmt(ForStmt stmt, Heap<Integer> arg) {
    Heap<Integer> ith_local = arg;
    int num_prev_locals = ith_local.val; // standard procedure to start and stop scope

    if (stmt.hasInit()) {
      stmt.init.visit(this, ith_local);
    }

    int condLine = Machine.nextInstrAddr();
    int skipBodyLine = -1000;
    if (stmt.hasCond()) {
      stmt.cond.visit(this, null);
      skipBodyLine = Machine.nextInstrAddr();
      Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, TBD);
    }
    stmt.body.visit(this, ith_local);
    if (stmt.hasUpdate()) {
      stmt.update.visit(this, null); // we do not pass ith_local as you can't declare a local here anyways
    }
    Machine.emit(Op.JUMP, Reg.CB, condLine);
    Machine.patch(skipBodyLine, Machine.nextInstrAddr());

    Machine.emit(Op.POP, ith_local.val-num_prev_locals); // reduce an instruction by removing POP if 0 vars created
    ith_local.val = num_prev_locals;
    return null;
  }

  /*
   *
   * References - adds the **address** of where a local/param/field is located to the stack
   * NOTE: method references should be gotten through the identifier
   * 
   */

  /** loads Reg.OB for the current frame's `this` reference. */
  private Object Machine_emitThis() {
    Machine.emit(Op.LOADA, Reg.OB, 0);
    return THIS_REF;
  }

  @Override
  public Object visitThisRef(ThisRef ref, Heap<Integer> arg) {
    // Machine.emit(Op.LOADA, Reg.OB, 0);
    return Machine_emitThis();
    // return null;
  }

  @Override
  public Object visitIdRef(IdRef ref, Heap<Integer> arg) {
    if (ref.id.decl instanceof ClassDecl) {
      // eg. Other.staticfield
      // Machine.emit(Op.LOAD, Reg.SB, ref.id.decl.offset);
      // NOTE: the referencing should be done from the QualRef
      return CLASS_REF;
    } else if (ref.id.decl instanceof LocalDecl) {
      // eg. localx
      Machine.emit(Op.LOADA, Reg.LB, ref.id.decl.offset);
      return null;
    } else if (ref.id.decl instanceof FieldDecl) { /* Internal Field Ref */
      // eg. (implicit this.) field (essentially a QualRef)
      if (((FieldDecl) ref.id.decl).isStatic) {
        // eg. (implicit this.) staticfield
        // we don't need object reference for static fields
        Machine.emit(Op.LOADA, Reg.SB, ref.id.decl.offset);
      } else {
        // eg. (implicit this.) instfield
        Machine_emitThis();
        Machine.emit(Op.LOADL, ref.id.decl.offset);
        return INST_FIELD_REF; // (we went into internal field to get an external field)
      }
    }

    return null;
  }

  private boolean Machine_emitStaticField(FieldDecl field) {
    if (field.isStatic) {
      Machine.emit(Op.POP, 1);
      Machine.emit(Op.LOADA, Reg.SB, field.offset);
      return true;
    }
    return false;
  } 

  @Override
  public Object visitQualRef(QualRef ref, Heap<Integer> arg) {
    if (ref.id.decl instanceof ArrayLengthDecl) {
      // as length can only be from RefExpr, we can just return the address of the array (which should be the reference to the left)
      return ref.ref.visit(this, null);
    }

    Object leftRefKind = ref.ref.visit(this, null);
    if (leftRefKind == THIS_REF) {
      if (((FieldDecl) ref.getId().decl).isStatic) { /* Internal Field Ref */
        Machine.emit(Op.POP, 1); // we don't need the object reference for static fields
        Machine.emit(Op.LOADA, Reg.SB, ref.id.decl.offset);
      } else {
        Machine.emit(Op.LOADL, ref.id.decl.offset);
        return INST_FIELD_REF;
      }
    } else if (leftRefKind == INST_FIELD_REF) { // eg. (this.)field.inst <--
      Machine.emit(Prim.fieldref); // this gets value, but what if this is a primative? --> well it's ready to be gotten!
      if (((FieldDecl) ref.getId().decl).isStatic) {
        Machine.emit(Op.POP, 1);
        Machine.emit(Op.LOADA, Reg.SB, ref.id.decl.offset);
        return null;
      } else {
        Machine.emit(Op.LOADL, ref.id.decl.offset);
        return INST_FIELD_REF;
      }
    } else if (leftRefKind == CLASS_REF) {
      // eg. Other.staticfield (typechecking should only allow static)
      Machine.emit(Op.LOADA, Reg.SB, ref.id.decl.offset);
    } else { // local or static address (stack) is on stack
      if (((FieldDecl) ref.getId().decl).isStatic) {
        Machine.emit(Op.POP, 1); // we don't need the prior reference as we can get the static field straight from this ref
        Machine.emit(Op.LOADA, Reg.SB, ref.id.decl.offset);
      } else {
        Machine.emit(Op.LOADI);
        Machine.emit(Op.LOADL, ref.id.decl.offset);
        return INST_FIELD_REF;
      }
    }

    return null;
  }

  /*
   *
   * Expressions - adds/modifies values at the stack top, statements must clean these up/use them!
   * 
   */

  @Override
  public Object visitUnaryExpr(UnaryExpr expr, Heap<Integer> arg) {
    expr.expr.visit(this, null);
    switch (expr.operator.kind) {
      case NOT:
        Machine.emit(Prim.not);
        break;
      case MINUS:
        Machine.emit(Prim.neg);
        break;
      default:
        reporter.reportError(prefix(expr.operator.posn) + "unknown binary operator '" + expr.operator.spelling + "'.");
        break;
    }
    return null;
  }

  @Override
  public Object visitBinaryExpr(BinaryExpr expr, Heap<Integer> arg) {
    expr.left.visit(this, null);
    TokenKind opKind = expr.operator.kind;

    // short circuiting operators
    if (opKind == TokenKind.AND || opKind == TokenKind.OR) {
      int shortCircuitJumpLine = Machine.nextInstrAddr();
      switch (opKind) {
        case AND:
          Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, TBD);
          expr.right.visit(this, null);
          Machine.emit(Prim.and);
          break;
        case OR:
          Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, TBD);
          expr.right.visit(this, null);
          Machine.emit(Prim.or);
          break;
        default:
          reporter.reportError(prefix(expr.operator.posn) + "not a short circuiting operator.");
      }
      
      // set where short circuit skip should be:
      Machine.patch(shortCircuitJumpLine, Machine.nextInstrAddr());

      return null;
    }

    expr.right.visit(this, null);
    switch (expr.operator.kind) {
      case ADD:
        Machine.emit(Prim.add);
        break;
      case MINUS:
        Machine.emit(Prim.sub);
        break;
      case MULTIPLY:
        Machine.emit(Prim.mult);
        break;
      case DIVIDE:
        Machine.emit(Prim.div);
        break;
      case LT:
        Machine.emit(Prim.lt);
        break;
      case GT:
        Machine.emit(Prim.gt);
        break;
      case LEQ:
        Machine.emit(Prim.le);
        break;
      case GEQ:
        Machine.emit(Prim.ge);
        break;
      case EQUALS:
        Machine.emit(Prim.eq);
        break;
      case NEQ:
        Machine.emit(Prim.ne);
        break;
      default:
        reporter.reportError(prefix(expr.operator.posn) + "unknown binary operator '" + expr.operator.spelling + "'.");
        break;
    }
    return null;
  }

  @Override
  public Object visitRefExpr(RefExpr expr, Heap<Integer> arg) {
    // loads the value at that address
    
    Object refType = expr.ref.visit(this, null);

    // special <Array>.length form
    if (expr.ref.getId().decl instanceof ArrayLengthDecl) {
      /* decided to implement QualRef just getting the array's length */
      if (refType == INST_FIELD_REF) {
        Machine.emit(Prim.fieldref);
      } else { // STACK_REF
        Machine.emit(Op.LOADI);
      }
      Machine.emit(Prim.arraylen);
      return null;
      // cannot ever get `this.length` (as if `this` was an array)
    }

    if (refType == INST_FIELD_REF) {
      Machine.emit(Prim.fieldref);
      return null;
    } else if (refType == THIS_REF) {
      // we just want OB as a value
      return null;
    } else { // STACK_REF
      Machine.emit(Op.LOADI);
    }

    return null;
  }

  @Override
  public Object visitIxExpr(IxExpr expr, Heap<Integer> arg) {
    Object refType = expr.ref.visit(this, null);
    if (refType == INST_FIELD_REF) {
      Machine.emit(Prim.fieldref);
    } else {
      Machine.emit(Op.LOADI);
    }
    expr.ixExpr.visit(this, null);
    Machine.emit(Prim.arrayref);
    return null;
  }

  @Override
  public Object visitCallExpr(CallExpr expr, Heap<Integer> arg) {
    Machine_emitMethodInvocation(expr.argList, expr.functionRef);
    // typechecking should prevent a void method call (==> prevent System.out.println)
    return null;
  }

  @Override
  public Object visitLiteralExpr(LiteralExpr expr, Heap<Integer> arg) {
    int val = 0;
    switch (expr.lit.kind) {
      case NUM:
        val = Integer.parseInt(expr.lit.spelling);
        break;
      case TRUE:
        val = Machine.trueRep;
        break;
      case FALSE:
        val = Machine.falseRep;
        break;
      case NULL:
        val = Machine.nullRep;
        break;
      default:
        reporter.reportError(prefix(expr.posn) + "unsupported literal token of '" + expr.lit.toString() + "'");
        break;
    }
    Machine.emit(Op.LOADL, val);
    return null;
  }

  @Override
  public Object visitNewObjectExpr(NewObjectExpr expr, Heap<Integer> arg) {
    ClassDecl calling = (ClassDecl) expr.classtype.className.decl;

    Machine.emit(Op.LOADL, CLASS_ADDR); // we do not save the Class object in memory, for our miniJava, classes are entirely a compilation entity
    Machine.emit(Op.LOADL, ((ClassDecl) expr.classtype.className.decl).instanceSize); // instanceSize must be set first!
    Machine.emit(Prim.newobj);

    if (!calling.hasDefaultConstructor()) {
      Machine_emitPushArgList(expr.argList);
      Machine.emit(Op.LOAD, Reg.ST, -(1 + expr.argList.size())); // make a copy of object reference
      toPatch.add(new Patch(Machine.nextInstrAddr(), calling.constructor));
      Machine.emit(Op.CALLI, Reg.CB, calling.constructor.offset);
    }
    return null;
  }

  @Override
  public Object visitNewArrayExpr(NewArrayExpr expr, Heap<Integer> arg) {
    expr.sizeExpr.visit(this, null);
    Machine.emit(Prim.newarr);
    return null;
  }

  
  /*
  * 
  * Useless Visitors 
  * 
  */
  
  @Override
  public Object visitIdentifier(Identifier id, Heap<Integer> arg) {
    return null;
  }
  
  @Override
  public Object visitOperator(Operator op, Heap<Integer> arg) {
    return null;
  }
  
  @Override
  public Object visitIntLiteral(IntLiteral num, Heap<Integer> arg) {
    return null;
  }
  
  @Override
  public Object visitBooleanLiteral(BooleanLiteral bool, Heap<Integer> arg) {
    return null;
  }
  
  @Override
  public Object visitNullLiteral(NullLiteral nulllit, Heap<Integer> arg) {
    return null;
  }

  @Override
  public Object visitBaseType(BaseType type, Heap<Integer> arg) {
    return null;
  }

  @Override
  public Object visitClassType(ClassType type, Heap<Integer> arg) {
    return null;
  }

  @Override
  public Object visitArrayType(ArrayType type, Heap<Integer> arg) {
    return null;
  }
}

class Patch {
  public int line;
  public Declaration decl;
  public Patch(int line, Declaration decl) {
    this.line = line;
    this.decl = decl;
  }
}

// allows you to pass a primative by reference
class Heap<T> {
  public T val;
  public Heap (T init) {
    this.val = init;
  }
}