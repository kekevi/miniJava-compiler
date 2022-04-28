/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NewObjectExpr extends NewExpr
{
    // pa5 - now required to have an argList
    public NewObjectExpr(ClassType ct, ExprList argList, SourcePosition posn){
        super(posn);
        classtype = ct;
        this.argList = argList;
    }
        
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitNewObjectExpr(this, o);
    }
    
    public ClassType classtype;
    public ExprList argList;
}
