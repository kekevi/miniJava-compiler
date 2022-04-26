package miniJava;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.*;

public class Predefined {
  private final static int PREDEFINED_LINE_NO = -1;
  public static ClassDecl string;
  public static ClassDecl _printstream;
  public static ClassDecl system;
  static { 
    // each wrapped in blocks so we can collapse them in IDE
    {string = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), new SourcePosition(PREDEFINED_LINE_NO));}

    {_printstream = new ClassDecl(
      "_PrintStream",
      new FieldDeclList(), 
      new MethodDeclList(
        new MethodDecl(
          new FieldDecl(
            false, 
            false, 
            new BaseType(
              TypeKind.VOID, 
              new SourcePosition(PREDEFINED_LINE_NO)
            ), 
            "println",
            new SourcePosition(PREDEFINED_LINE_NO)
          ), 
          new ParameterDeclList(
            new ParameterDecl(
              new BaseType(
                TypeKind.INT, 
                new SourcePosition(PREDEFINED_LINE_NO)
              ), "n", new SourcePosition(PREDEFINED_LINE_NO)
            )
          ), 
          new StatementList(), 
          new SourcePosition(PREDEFINED_LINE_NO)
        )
      ), 
      new SourcePosition(PREDEFINED_LINE_NO)
    );}

    {system = new ClassDecl(
      "System", 
      new FieldDeclList(
        new FieldDecl(
          false, 
          true, 
          new ClassType(
            new Identifier(
              new Token(
                TokenKind.ID, 
                "_PrintStream", 
                new SourcePosition(PREDEFINED_LINE_NO)
              ),
              _printstream), new SourcePosition(PREDEFINED_LINE_NO)
          ), 
          "out", 
          new SourcePosition(PREDEFINED_LINE_NO))
      ), 
      new MethodDeclList(), 
      new SourcePosition(PREDEFINED_LINE_NO)
    );}
  }

    static boolean p(boolean b) {
      System.out.println(b);
      return b;
    }
    public static boolean isPrintLn(Reference printlnRef) {
      MethodDecl method = (MethodDecl) printlnRef.getId().decl;

      return (
        !method.isPrivate
        && !method.isStatic
        && method.type.typeKind == TypeKind.VOID
        && method.name.equals("println")
        && method.parameterDeclList.size() == 1
        && method.parameterDeclList.get(0).type.typeKind == TypeKind.INT
        && printlnRef.isQualified()
        && ((QualRef) printlnRef).ref.getId().spelling.equals("out")
        && ((QualRef) printlnRef).ref.getId().decl == system.fieldDeclList.get(0)
        && ((QualRef) printlnRef).ref.isQualified()
        && ((QualRef) (((QualRef) printlnRef).ref)).ref.getId().spelling.equals("System")
        && ((QualRef) (((QualRef) printlnRef).ref)).ref.getId().decl == system
      );
    }
  
}


