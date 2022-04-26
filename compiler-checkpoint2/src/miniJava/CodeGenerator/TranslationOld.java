package miniJava.CodeGenerator;

import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.Predefined;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.mJAM.Machine;
import miniJava.mJAM.Machine.Op;
import miniJava.mJAM.Machine.Prim;
import miniJava.mJAM.Machine.Reg;

public class TranslationOld implements Visitor<Object, Object> {
  private final static int UNDEFINED = 0; // MethodDecl.offset = 0 initially, but only mainInvoker can be at CB+0 ==> safe to use 0 as undefined method location
  private final static int TBD = -1; // an address that needs to be patched later
  private final static int FRAME_SIZE = 3;
  private final static int OBJECT_SIZE = 2;

  private AST ast;
  private ErrorReporter reporter;
  private int mainAddr;
  private ArrayList<Patch> toPatch;

  /* 
    NOTE: if you want to implement doubles, (multi-block data structures) there are many strategies
    * add a `* factor` everywhere you need
    * define doubles similar to a class, but with a size on the stack rather than the heap
  */

  /*
    TODO: refactor References so you don't have to pass Address around
    TODO: refactor CallStmt and CallExpr so they call a void helper function
    TODO: we can also refactor BlockStmt and that part of MethodDecl, but this is way less beneficial
    TODO: make patch constructor automatically call Machine.nextInstrAddr
    TODO: extract Pretranslation actions from identification

    BIG TODO: check for missing return stmt
  */

  public TranslationOld (AST checkedAst, ErrorReporter reporter) {
    this.ast = checkedAst;
    this.reporter = reporter;
    this.toPatch = new ArrayList<Patch>();
  }

  /**
   * Main method of Translation
   */
  public void translate() {
    Machine.initCodeGen();
    ast.visit(this, null);
    // tell machine to patch all the addresses that have yet to be made
  }

  private static String prefix(SourcePosition posn) {
    return "*** line " + posn.start + ": ";
  }

  @Override
  public Object visitPackage(Package prog, Object arg) {
    // allocate main
    Machine.emit(Op.LOADL, 0);
    Machine.emit(Prim.newarr);
    mainAddr = Machine.nextInstrAddr();
    Machine.emit(Op.CALL, Reg.CB, TBD);
    Machine.emit(Op.HALT, 0, 0, 0);

    for (ClassDecl clas : prog.classDeclList) {
      clas.visit(this, null);
    }

    for (Patch patch : toPatch) {
      Machine.patch(patch.line, patch.decl.offset);
    }
    return null;
  }

  @Override
  public Object visitClassDecl(ClassDecl cd, Object arg) {
    // fields should be loaded before any statements are visited
    // cd.instanceSize = 0; // unnecessary, but initialize just in case NOTE: done in Identification now
    int ith_instance = 0;
    for (FieldDecl field : cd.fieldDeclList) {
      // field.visit(this, cd.size); // old visit
      if (field.isStatic) { // allocate in static segment
        Machine.emit(Op.PUSH, 1); // PUSH increments the data store by 1 (stack)
        field.offset = nextStaticFieldAddr(); // should = Machine.ST
      } else {
        field.offset = ith_instance + OBJECT_SIZE; // we are not passing or storing class decls
        ith_instance += 1;
      }
    }

    for (MethodDecl method : cd.methodDeclList) {
      method.visit(this, null);
    }

    return null;
  }

  private int staticSegmentOffset = 0; // annoyingly there is no static address manager in Machine
  private int nextStaticFieldAddr() {
    return staticSegmentOffset++;
  }
  @Override
  public Object visitFieldDecl(FieldDecl fd, Object arg) {
    // if (fd.isStatic) { // allocate in static segment
    //   Machine.emit(Op.PUSH, 1); // PUSH increments the data store by 1 (stack)
    //   fd.offset = nextStaticFieldAddr(); // should = Machine.ST
    // } else {
    //   fd.offset = (Integer) arg; // offset from OB
    // }
    if (fd.isStatic) {
      return new Address(Reg.SB, fd.offset);
    }

    // TODO:
    return new Address(Reg.SB, fd.offset);
  }

  @Override
  public Object visitMethodDecl(MethodDecl md, Object arg) {
    md.offset =  Machine.nextInstrAddr();
    if (MethodDecl.isMain(md)) {
      Machine.patch(mainAddr, md.offset);
    }

    for (int ith_param = 0; ith_param < md.parameterDeclList.size(); ith_param++) {
      ParameterDecl param = md.parameterDeclList.get(ith_param);
      param.offset = -(ith_param+1); // could alternatively skip visitPD, and just do this in visitMD
    }

    Heap<Integer> ith_local = new Heap<Integer>(FRAME_SIZE);    
    // Heap<Integer> ith_local = new Heap<Integer>(md.isStatic ? );    
    for (Statement statement : md.statementList) {
      if (statement.visit(this, ith_local) == null ? false : true) {
        ith_local.val += 1;
      }
    }

    // if return stmt is dropped in void:, otherwise, it'll jump due to the return right before this
    // we could check for md.returnType.VOID, but it should be okay
    Machine.emit(Op.RETURN, 0, Reg.ZR, md.parameterDeclList.size());
    return new Address(Reg.CB, md.offset);
  }

  @Override
  public Object visitParameterDecl(ParameterDecl pd, Object arg) {
    // pd.offset = -((Integer) arg); // could alternatively skip visitPD, and just do this in visitMD
    // args should be placed above LB (negative offset)
    // Machine.emit(Op.LOAD, Reg...) <-- should be emitted during ProcCall
    // return null;
    return new Address(Reg.LB, pd.offset); // maybe? or should the ref just do this
  }

  @Override
  public Object visitVarDecl(VarDecl decl, Object arg) {
    // decl.offset = (Integer) arg;
    // if (TypeKind.isObject(decl.type.typeKind)) {

    // }
    
    return new Address(Reg.LB, decl.offset);
  }

  @Override
  public Object visitArrayLengthDecl(ArrayLengthDecl decl, Object arg) {
    Address array_addr = (Address) decl.arrayRef.getId().decl.visit(this, null);
    return array_addr;
  }

  @Override
  public Object visitBaseType(BaseType type, Object arg) {
    return null;
  }

  @Override
  public Object visitClassType(ClassType type, Object arg) {
    return null;
  }

  @Override
  public Object visitArrayType(ArrayType type, Object arg) {
    return null;
  }

  @Override
  public Object visitBlockStmt(BlockStmt stmt, Object arg) {
    /*
    int n_prev_locals = (Integer) arg;
    int ith_local = 0 + n_prev_locals;
    for (Statement statement : stmt.sl) {
      if (statement.visit(this, ith_local) == null ? false : true) {
        ith_local += 1;
      }
    }

    // technically I don't even need to pop these off, since identification should already treat them as missing
    //  and when the call frame pops, then this will all get popped off -- the only thing is that I need a consistent local count
    Machine.emit(Op.POP, 0, Reg.ZR, ith_local - n_prev_locals); // n is 0 because we don't want anything put back onto the stack, POP ignores register
    */
    Heap<Integer> ith_local = ((Heap<Integer>) arg);
    for (Statement statement : stmt.sl) {
      if (statement.visit(this, ith_local) == null ? false : true) {
        ith_local.val += 1;
      }
    }
    return null;
  }

  @Override
  public Object visitVarDeclStmt(VarDeclStmt stmt, Object arg) {
    int ith_local = ((Heap<Integer>) arg).val; // where arg is the ith local of this frame
    stmt.varDecl.offset = ith_local;
    Machine.emit(Op.PUSH, 1);
    stmt.initExp.visit(this, null);
    Machine.emit(Op.STORE, Reg.LB, ith_local);
    return true;
  }

  @Override
  public Object visitAssignStmt(AssignStmt stmt, Object arg) {
    Reference idkref = stmt.ref;
    System.out.println(idkref.getId().spelling);
    if (idkref.isQualified()) {
      Reference leftRef = ((QualRef) stmt.ref).ref;
      Address left_addr = (Address) leftRef.getId().decl.visit(this, null);
      Machine.emit(Op.LOAD, 1, left_addr.base, left_addr.offset); // push the heap address onto stack
      Machine.emit(Op.LOADL, stmt.ref.getId().decl.offset); // get the ith field
      stmt.val.visit(this, null);
      Machine.emit(Prim.fieldupd);
      return null;
    }
    
    stmt.val.visit(this, null);
    Address toLoad = (Address) stmt.ref.getId().decl.visit(this, null);
    // stmt.ref.visit(this, null);
    Machine.emit(Op.STORE, 1, toLoad.base, toLoad.offset);
    return null;
  }

  @Override
  public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
    Address toLoad = (Address) stmt.ref.getId().decl.visit(this, null);
    System.out.println("off" + toLoad.offset);
    Machine.emit(Op.LOAD, 1, toLoad.base, toLoad.offset);
    stmt.ix.visit(this, null);
    stmt.exp.visit(this, null);
    // arrayupd expects an address of an array object, hence we need to push the heap address; the index; and the new value
    Machine.emit(Prim.arrayupd);
    return null;
  }

  @Override
  public Object visitCallStmt(CallStmt stmt, Object arg) {
    // we go backwards so the 1st arg can be @ LB-1, the 2nd arg can be @ LB-2, etc...
    for (int i = stmt.argList.size()-1; i >= 0; i--) {
      stmt.argList.get(i).visit(this, null);
    }

    Reference procRef = stmt.methodRef; // method references do not need to be visited
    MethodDecl procedure = ((MethodDecl) procRef.getId().decl);

    // check for built-in methods
    if (Predefined.isPrintLn(procRef)) {
      Machine.emit(Prim.putintnl);
      // Machine.emit(Op.POP, 0, Reg.ZR, 1); // I don't think putinnl actually removes the value
      return null;
    }

    if (procedure.isStatic) {
      int codeAddr = procedure.offset;
      if (codeAddr == UNDEFINED) {
        toPatch.add(new Patch(Machine.nextInstrAddr(), procedure));
        codeAddr = TBD;
      }
      Machine.emit(Op.CALL, Reg.CB, codeAddr);

    } else { // instance invocation
      if (procRef.isQualified()) { // either 'this' or IdRef of a class (as miniJava does not allow method chaining, eg. `x.foo().bar()` )
        Reference instanceRef = ((QualRef) procRef).ref;
        Address instance_addr = (Address) instanceRef.visit(this, null); 
        Machine.emit(Op.LOADA, instance_addr.base, instance_addr.offset); //TODO: LOAD or LOADA? 
      } else {// otherwise, implicit 'this'
        Machine.emit(Op.LOADA, Reg.OB, 0); // NOTE: should be same as "this"
      }
      int codeAddr = procedure.offset;
      if (codeAddr == UNDEFINED) {
        toPatch.add(new Patch(Machine.nextInstrAddr(), procedure));
        codeAddr = TBD;
      }
      Machine.emit(Op.CALLI, Reg.CB, codeAddr);
    }

    // handle return? or returnStmt will handle?
    return null;
  }

  @Override
  public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
    int nArgs = stmt.ofMethod.parameterDeclList.size();

    // non-void 
    if (stmt.returnExpr != null) {
      stmt.returnExpr.visit(this, null); 
      Machine.emit(Op.RETURN, 1, Reg.ZR, nArgs);
      return null;
    }

    // void
    Machine.emit(Op.RETURN, 0, Reg.ZR, nArgs);
    return null;
  }

  @Override
  public Object visitIfStmt(IfStmt stmt, Object ith_local_tracker) {
    stmt.cond.visit(this, null);
    int jumpToElseOrEnd = Machine.nextInstrAddr();
    Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, TBD); // jump to else: or cont:

    stmt.thenStmt.visit(this, ith_local_tracker);
    if (stmt.elseStmt == null) {
      Machine.patch(jumpToElseOrEnd, Machine.nextInstrAddr());
      return null;
    } 

    int jumpPastElse = Machine.nextInstrAddr();
    Machine.emit(Op.JUMP, Reg.CB, TBD); // skip over else section if true already activates
    Machine.patch(jumpToElseOrEnd, Machine.nextInstrAddr());
    stmt.elseStmt.visit(this, ith_local_tracker); 

    Machine.patch(jumpPastElse, Machine.nextInstrAddr()); 
     
    return null;
  }

  @Override
  public Object visitWhileStmt(WhileStmt stmt, Object arg) {
    /*
      while_check: (get condition expr)
      JUMPIF (cond == false ) --> cont:
      ...body...
      JUMP --> while_check:
      cont: ...
    */
    int condLine = Machine.nextInstrAddr();
    stmt.cond.visit(this, null);
    int skipBody = Machine.nextInstrAddr();
    Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, TBD);
    stmt.body.visit(this, null);
    Machine.emit(Op.JUMP, Reg.CB, condLine);
    Machine.patch(skipBody, Machine.nextInstrAddr());
    return null;
  }

  @Override
  public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
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
  public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
    expr.left.visit(this, null);
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
      case AND:
        Machine.emit(Prim.and);
        break;
      case OR:
        Machine.emit(Prim.or);
        break;
      default:
        reporter.reportError(prefix(expr.operator.posn) + "unknown binary operator '" + expr.operator.spelling + "'.");
        break;
    }
    return null;
  }

  @Override
  public Object visitRefExpr(RefExpr expr, Object arg) {
    if (expr.ref.getId().decl instanceof ArrayLengthDecl) {
      Machine.emit(Prim.arraylen);
      return null;
    }
    expr.ref.visit(this, null);
    return null;
  }

  @Override
  public Object visitIxExpr(IxExpr expr, Object arg) {
    expr.ref.visit(this, null);
    expr.ixExpr.visit(this, null);
    Machine.emit(Prim.arrayref);
    return null;
  }

  @Override
  public Object visitCallExpr(CallExpr expr, Object arg) {
    for (int i = expr.argList.size()-1; i >= 0; i--) {
      expr.argList.get(i).visit(this, null);
    }

    // I don't think an Expr can be void so we don't have to check for println

    MethodDecl method = (MethodDecl) expr.functionRef.getId().decl;
    Reference methodRef = expr.functionRef;
    if (method.isStatic) {
      int codeAddr = method.offset;
      if (codeAddr == UNDEFINED) {
        toPatch.add(new Patch(Machine.nextInstrAddr(), method));
        codeAddr = TBD;
      }
      Machine.emit(Op.CALL, Reg.CB, codeAddr);
    } else {
      if (methodRef.isQualified()) { // either 'this' or IdRef of a class (as miniJava does not allow method chaining, eg. `x.foo().bar()` )
        Reference instanceRef = ((QualRef) methodRef).ref;
        Address instance_addr = (Address) instanceRef.visit(this, null); 
        Machine.emit(Op.LOADA, instance_addr.base, instance_addr.offset); //TODO: LOAD or LOADA? 
      } else {// otherwise, implicit 'this'
        Machine.emit(Op.LOADA, Reg.OB, 0); // NOTE: should be same as "this"
      }
      int codeAddr = method.offset;
      if (codeAddr == UNDEFINED) {
        toPatch.add(new Patch(Machine.nextInstrAddr(), method));
        codeAddr = TBD;
      }
      Machine.emit(Op.CALLI, Reg.CB, codeAddr);
    }

    return null;
  }

  @Override
  public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
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
  public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
    Machine.emit(Op.LOADL, Machine.nullRep); // we do not save the Class object in memory, for our miniJava, classes are entirely a compilation entity
    Machine.emit(Op.LOADL, ((ClassDecl) expr.classtype.className.decl).instanceSize); // instanceSize must be set first!
    Machine.emit(Prim.newobj);
    return null;
  }

  @Override
  public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
    expr.sizeExpr.visit(this, null);
    Machine.emit(Prim.newarr);
    return null;
  }

  // ***
  // visiting a reference will add their position onto the execution stack
  // eg. ThisRef.visit(...) will push location of "this"'s instance in heap
  // eg. IdRef of local var `x` will push location of 'x' on stack
  // ^^^ TODO: or we may want to make this optional based on a passed in arg
  // ***

  @Override
  public Object visitThisRef(ThisRef ref, Object arg) {
    // TODO Auto-generated method stub
    Machine.emit(Op.LOADA, Reg.OB, 0);
    return new Address(Reg.OB, 0);
  }

  @Override
  public Object visitIdRef(IdRef ref, Object arg) {
    Address addr = (Address) ref.id.decl.visit(this, null);
    Machine.emit(Op.LOAD, addr.base, addr.offset);
    return addr;
  }

  @Override
  public Object visitQualRef(QualRef ref, Object arg) {
    System.out.print(ref.ref.getId().spelling + ".");
    System.out.println(ref.id.spelling);

    // checking length built in
    if (ref.id.spelling.equals("length") && ref.ref.getId().decl.type.typeKind == TypeKind.ARRAY) {
      Address array_addr = (Address) ref.id.decl.visit(this, null);
      Machine.emit(Op.LOAD, array_addr.base, array_addr.offset);
      return null; 
    }

    System.out.println(ref.ref.getId().decl);
    if (ref.ref.getId().decl instanceof VarDecl) {
      Address local = (Address) ref.ref.getId().decl.visit(this, null); // base is LB
      Machine.emit(Op.LOAD, 1, local.base, local.offset); // push the heap address onto stack
      Machine.emit(Op.LOADL, ref.id.decl.offset); // get the ith field
      Machine.emit(Prim.fieldref);
      return null;
    }
    Address addr = (Address) ref.id.decl.visit(this, null);
    Machine.emit(Op.LOAD, addr.base, addr.offset);
    return addr;
  }

  @Override
  public Object visitIdentifier(Identifier id, Object arg) {
    // idk why we need identifiers anyway, can't we just refer to the decl from the ref?
    return null;
  }

  @Override
  public Object visitOperator(Operator op, Object arg) {
    // just get it from visitUnary/BinExpr
    return null;
  }

  // *** ignore visitXLiteral, it's easier just to switch case the the `lit.kind` than casting
  @Override
  public Object visitIntLiteral(IntLiteral num, Object arg) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object visitNullLiteral(NullLiteral nulllit, Object arg) {
    // TODO Auto-generated method stub
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
