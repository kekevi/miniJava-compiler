/**
 * NullLiteral added for pa3
 * @author kekevi
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class NullLiteral extends Terminal {
  public NullLiteral(Token t) {
    super(t);
  }
 
  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitNullLiteral(this, o);
  }
}
