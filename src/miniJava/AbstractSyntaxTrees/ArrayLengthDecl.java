package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayLengthDecl extends Declaration {

  public NamedRef arrayRef; // reference to the entire array object

  public ArrayLengthDecl(NamedRef arrayRef, SourcePosition posn) {
    super("length", new BaseType(TypeKind.INT, posn), posn);
    this.arrayRef = arrayRef;
  }

  @Override
  public <A, R> R visit(Visitor<A, R> v, A o) {
    return v.visitArrayLengthDecl(this, o);
  }
  
}
