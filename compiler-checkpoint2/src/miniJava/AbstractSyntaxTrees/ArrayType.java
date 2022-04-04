/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends TypeDenoter {

	    public ArrayType(TypeDenoter eltType, SourcePosition posn){
	        super(TypeKind.ARRAY, posn);
	        this.eltType = eltType;
	    }
	        
	    public <A,R> R visit(Visitor<A,R> v, A o) {
	        return v.visitArrayType(this, o);
	    }

	    public TypeDenoter eltType;

			public boolean matches(TypeDenoter other) {
				return super.matches(other) && other.typeKind != TypeKind.ERROR ? this.eltType.matches(((ArrayType) other).eltType) : super.matches(other);
			}

			public String toString() {
				return "Array of " + this.eltType.toString();
			}
	}

