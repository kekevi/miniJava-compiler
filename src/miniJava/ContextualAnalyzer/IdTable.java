package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.LocalDecl;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.VarDecl;

public class IdTable {
  private final static int CLASS = 0;
  private final static int MEMBER = 1;
  private final static int PARAM = 2;
  private ArrayList<HashMap<String, Declaration>> env;
  private HashMap<String, Boolean> isMethod;
  public IdTable() {
    env = new ArrayList<>();
  }

  //
  // Helpers
  // 

  private int top() {
    return env.size() - 1;
  }

  private HashMap<String, Declaration> topScope() {
    return env.get(top());
  }

  //
  // Basic Methods
  //

  public void addScope() {
    env.add(new HashMap<>());
    if (top() == MEMBER) {
      isMethod = new HashMap<>();
    }
  }

  public void removeScope() {
    if (top() == MEMBER) {
      isMethod = null;
    }
    env.remove(top());
  }

  private boolean hasDeclaration(int level, String symbol) {
    return env.get(level).containsKey(symbol);
  }

  /**
   * @param level
   * @param symbol
   * @return null if declaration if the symbol does not exist on that level
   * *There should not be any null declarations passed into the IdTable
   */
  private Declaration getDeclaration(int level, String symbol) {
    return env.get(level).get(symbol);
  }

  private boolean addDeclaration(int level, String symbol, Declaration decl) {
    if (hasDeclaration(level, symbol)) {
      return false;
    }
    env.get(level).put(symbol, decl);
    return true;
  }

  /**
   * @param symbol
   * @return false if symbol isn't in table (null) or symbol is a field
   */
  private boolean isMethod(String symbol) {
    if (isMethod.get(symbol) == null) {
      return false;
    }
    return isMethod.get(symbol);
  }

  //
  // Specific Methods for AST Items
  //

    //
    // Putters
    // @returns {Boolean} false if cannot be added (already exists/duplicate key)

  /**
   * 
   * @param symbol
   * @param clas
   * @return true if successfully added, false if symbol/key already exists
   */
  public boolean addClassDecl(String symbol, ClassDecl clas) {
    return addDeclaration(CLASS, symbol, clas);
  }

  private boolean addMemberDecl(String symbol, MemberDecl member) {
    return addDeclaration(MEMBER, symbol, member);
  }

  public boolean addMemberDecl(String symbol, FieldDecl field) {
    isMethod.put(symbol, false);
    return addMemberDecl(symbol, (MemberDecl) field);
  }

  public boolean addMemberDecl(String symbol, MethodDecl method) {
    isMethod.put(symbol, true);
    return addMemberDecl(symbol, (MemberDecl) method);
  }

  public boolean addParamDecl(String symbol, ParameterDecl param) {
    return addDeclaration(PARAM, symbol, param);
  }

  public boolean addVarDecl(String symbol, VarDecl local) {
    // SPECIAL CASE since vars of same name cannot be redeclared even if declaration occurs in higher scope
    if (getParamOrLocal(symbol) != null) {
      return false;
    }
    return addDeclaration(top(), symbol, local);
  }

    //
    // Getters
    // @returns {Declaration} null if cannot be gotten

  public ClassDecl getClassDecl(String symbol) {
    return (ClassDecl) getDeclaration(CLASS, symbol);
  }

  public LocalDecl getParamOrLocal(String symbol) {
    int level = top();
    while (level >= PARAM) {
      // should always be a local decl
      LocalDecl localDecl = (LocalDecl) getDeclaration(level, symbol);
      if (localDecl != null) {
        return localDecl;
      }
      level--;
    }
    return null;
  }

  public MethodDecl getInternalMethod(String symbol) {
    if (isMethod(symbol)) {
      return (MethodDecl) getDeclaration(MEMBER, symbol);
    }
    return null;
  }

  public FieldDecl getInternalField(String symbol) {
    if (isMethod(symbol)) {
      return null;
    }
    return (FieldDecl) getDeclaration(MEMBER, symbol);
  }

  // TODO: ClassDecls should store methods and fields as a hashmap (really all DeclLists should be stored as a hashmap too)
  // maybe we should check if external is in our declared classes?
  public MethodDecl getExternalMethod(String symbol, ClassDecl external) {
    for (MethodDecl method : external.methodDeclList) {
      if (method.name.equals(symbol)) { 
        return method;
      }
    }
    return null;
  }

  public FieldDecl getExternalField(String symbol, ClassDecl external) {
    for (FieldDecl field : external.fieldDeclList) {
      if (field.name.equals(symbol)) {
        return field;
      }
    }
    return null;
  }
}
