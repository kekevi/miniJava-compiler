/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {
    
    public TypeDenoter(TypeKind type, SourcePosition posn){
        super(posn);
        typeKind = type;
    }
    
    public TypeKind typeKind;
    
    public boolean matches(TypeDenoter other) {
        if (this.typeKind == TypeKind.ERROR || other.typeKind == TypeKind.ERROR) { // ERROR aux-type is equal to any type
            return true;
        }
        if (this.typeKind == TypeKind.UNSUPPORTED || other.typeKind == TypeKind.UNSUPPORTED) { // UNSUPPORTED aux-type doesn't match any other type 
            return false;
        }
        return this.typeKind == other.typeKind;
    }

    public String toString() {
        return this.typeKind.toString();
    }
}

        