/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

public enum TypeKind {
        VOID,
		INT,
        BOOLEAN,
        CLASS,
        ARRAY,
        UNSUPPORTED,
        ERROR;

    public static boolean isObject(TypeKind kind) {
        return kind == CLASS || kind == ARRAY;
    }
}
