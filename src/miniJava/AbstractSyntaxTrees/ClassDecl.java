/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.Predefined;
import  miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class ClassDecl extends Declaration {
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
  public ConstructorDecl constructor; // pa5 - we only support one constructor per class currently
  public int instanceSize; // # of fields an instance would have
  private boolean isDefaultConstructor;

  public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, null, posn);
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
    this.constructor = defaultConstructorBuilder(name); 
  }

  // pa5 modification
  public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, ConstructorDecl constructor, SourcePosition posn) {
	  this(cn, fdl, mdl, posn);
    if (constructor != null) {
      isDefaultConstructor = false;
      this.constructor = constructor;
    } 
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }

  public boolean hasDefaultConstructor() {
    return isDefaultConstructor;
  }

  private ConstructorDecl defaultConstructorBuilder(String name) {
    isDefaultConstructor = true;
    return new ConstructorDecl(
      false, 
      new ClassType(new Identifier(new Token(TokenKind.ID, name, posn), this), posn), 
      name, 
      new ParameterDeclList(), 
      new StatementList(), 
      new SourcePosition(Predefined.PREDEFINED_LINE_NO));
  }
}
