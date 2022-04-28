package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ConstructorDecl extends MemberDecl {
  public ParameterDeclList pl;
  public StatementList sl;

  /** @param type should be a ClassType */ 
  public ConstructorDecl(boolean isPrivate, TypeDenoter type, String className, ParameterDeclList pl, StatementList sl, SourcePosition posn) {
    super(isPrivate, false, type, className, posn);
    this.pl = pl;
    this.sl = sl;
  }

  @Override
  public <A, R> R visit(Visitor<A, R> v, A o) {
    return v.visitConstructorDecl(this, o);
  }

  
}
