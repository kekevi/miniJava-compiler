/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassType extends TypeDenoter
{
    public ClassType(Identifier cn, SourcePosition posn){
        super(TypeKind.CLASS, posn);
        className = cn;
    }
            
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitClassType(this, o);
    }

    public Identifier className;

    public boolean matches(TypeDenoter other) {
        return super.matches(other) && other.typeKind != TypeKind.ERROR ? this.className.decl == ((ClassType) other).className.decl : super.matches(other); 
    }

    public String toString() {
        return this.className.spelling + "(class)";
    }
}
