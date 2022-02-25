package miniJava.SyntacticAnalyzer;

// keywords are to have their own tokens TODO: use enumSets to categorize these tokens further
public enum TokenKind {
  // auxillary tokens
  EOT,              // end of text
  COMMENT,
  ERROR,            // invalid token

  // spelled tokens
  NUM,
  ID,               // variable, function, class, and member names

  // syntactual symbols
  SEMICOLON,
  COMMA,            // for lists
  PERIOD,           // for accessing class members
  ASSIGN,           // *assignment* equals (not the equality boolean operator)

  // bracket tokens
  LPAREN,
  RPAREN,
  LCURLY,
  RCURLY,
  LSQUARE,
  RSQUARE,

  // binary operators (unsure if separated from tokenkind=binop would be better or not)
  ADD,            // binary operators (+, -, *, /, {relational}, ==, !=, {logical})
  MULTIPLY,
  DIVIDE,
  LT,
  GT,
  LEQ,            // <=
  GEQ,            // >=
  EQUALS,
  NEQ,            // !=
  AND,
  OR,

  // annoying operators, let the parser handle the logic
  MINUS,       // -

  // unary operators
  NOT,            // !

  // primatives / typing
  INT,              // primitive type
  BOOLEAN,          // primitive type
  VOID,
  CLASS,
  
  // keywords
  THIS,
  NEW,
  STATIC,         // access
  TRUE,
  FALSE,
  
  // keywords - visibility
  PUBLIC,
  PRIVATE,
  
  // keywords - control flow
  RETURN,
  WHILE,
  IF,
  ELSE
}
