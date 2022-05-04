# miniJava-compiler
A compiler for miniJava for COMP 520 Spring 2022 at UNC.

## How to Use
* Clone this GitHub repository to your computer. Compile the src files into a bin folder in project root.
* Write some miniJava in a file, save it with `.java`. You can change the extension for the make and debug scripts inside the file.
* In the terminal, type `./make.sh filename.java`. This will run the program and create assembly code (`.asm`) and an object file (`.mJAM`).
* Type `./debug.sh filename.java` to run the debugger.

## Modified Grammar

The grammar after adding in the PA5 extra credit is a bit different compared to how it was described in PA1:

ClassDeclaration ::= **class** *id* **{** (FieldDeclaration | MethodDeclaration | ConstructorDeclaration)* **}**

FieldDeclaration ::= Visibility Access Type *id* ( **=** Expression )? **;**

ConstructorDeclaration ::= Visibility *id* **(** ParamList? **)** **{** Statement* **}**

Statement (ForStmt) ::= 
  ... |
  **for** **(** (Statement | **;**) (Expression? **;**) ForUpdateStatement **)** Statement 
  | ...

ForUpdateStatement ::= \[AssignStmts, IxAssignStmts, CallStmts without the **;** ending\]

Expression (NewObjectExpr) ::= **new** *id* **(** ArgList? **)**

Below is a copy of `guide.txt`, which is used for the PA5 submission.
# miniJava Compiler

## Scope of Project
- basic PA1-PA4 functionality
- static field initialization (+2)
- parameterized class constructors (+2)
- for loops (+3)

## Summary of AST Class Changes
- All References have an identifier to access their declaration, set during identification.
- Added custom toString() to Type and Token enums to facilitate error reporter messages and testing.
- Added a bunch of Abstract Syntax Trees like ForStmt, ConstructorDecl, etc. for the extra features.
- Things like ASTDisplay and Vistor was modified accordingly to additions.
- Some classes like ClassDecl had additional constructors added just to save a few lines of code.
- Decls also all have an `offset` field that tells how many addresses away from a base should
  the code generator go to to find the instruction/data it's looking for.

## Description of Tests
- the `tests` directory contains a `general_test.java` and a modified `PA4Test.java`. These
  should print 1 2 3, and 1 2 3 ... 18 999.
- there are three folders `for_loops`, `parameterized_constructors`, and `static_field_init`
  which correspond to the three additional features I've implemented. So long as positive values
  are printed from the *pass* files, the compiler is functional. For the *fail* files, it should
  not compile. Most of them have reasons why explained as comments within the file.
