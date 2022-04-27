package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ForStmt extends Statement {
  public Statement init;
  public Expression cond;
  public Statement update;
  public Statement body; 

  public ForStmt(Statement init, Expression cond, Statement update, Statement body, SourcePosition posn) {
    super(posn);
    this.init = init;
    this.cond = cond;
    this.update = update;
    this.body = body;
  }

  @Override
  public <A, R> R visit(Visitor<A, R> v, A o) {
    return v.visitForStmt(this, o);
  }

  public boolean hasInit() {
    return init != null;
  }

  public boolean hasCond() {
    return cond != null;
  }

  public boolean hasUpdate() {
    return update != null;
  }

}
