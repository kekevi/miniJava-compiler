/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class MethodDecl extends MemberDecl {
  
  public MethodDecl(MemberDecl md, ParameterDeclList pl, StatementList sl, SourcePosition posn) {
    super(md,posn);
    parameterDeclList = pl;
    statementList = sl;
  }

  // why was this not defined originally??
  public MethodDecl(boolean isPrivate, boolean isStatic, TypeDenoter t, String name, ParameterDeclList pl, StatementList sl, SourcePosition posn) {
    super(isPrivate, isStatic, t, name, posn);
    parameterDeclList = pl;
    statementList = sl;
  }
  
  public <A, R> R visit(Visitor<A, R> v, A o) {
    return v.visitMethodDecl(this, o);
  }
  
  public ParameterDeclList parameterDeclList;
  public StatementList statementList;

  // added in PA4, used in Identification and Translation
  public static boolean isMain(MethodDecl method) {
    // public static void main(String[] args)
    return (
      !method.isPrivate // private
      && method.isStatic // static
      && method.type.typeKind == TypeKind.VOID // void
      && method.name.equals("main")  // main
      && method.parameterDeclList.size() == 1 // (??? args)
      && method.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY // (??[] args)
      && ((ArrayType) method.parameterDeclList.get(0).type).eltType.typeKind == TypeKind.CLASS // (?[] args)
      && ((ClassType) ((ArrayType) method.parameterDeclList.get(0).type).eltType).className.spelling.equals("String") // (String[] args)
    );
  }
}
