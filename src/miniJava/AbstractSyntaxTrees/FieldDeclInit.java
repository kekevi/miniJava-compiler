package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class FieldDeclInit extends FieldDecl {
  public Expression init;

  public FieldDeclInit(boolean isPrivate, boolean isStatic, TypeDenoter t, String name, Expression init, SourcePosition posn) {
    super(isPrivate, isStatic, t, name, posn);
    this.init = init;
  }

  @Override
  public <A, R> R visit(Visitor<A, R> v, A o) {
    return v.visitFieldDeclInit(this, o);
  }
}
