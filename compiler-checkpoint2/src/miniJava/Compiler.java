package miniJava;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.ContextualAnalyzer.TypeChecking;

import java.io.File;
import java.io.FileInputStream;

public class Compiler {
  public static void main(String[] args) {
    boolean debugMode = false;
    if (args.length < 1) {
      System.out.println("No file specified in first argument.");
      System.exit(1);
    }

    if (args.length >= 2) {
      debugMode = args[1].equals("true");
    }

    if (debugMode) System.out.println("Reading from " + args[0]);
    FileInputStream stream = null; // just to get rid of the annoying warning
    try {
      stream = new FileInputStream(new File(args[0]));
    } catch (Exception e) {
      System.out.println("File path " + args[0] + " could not be read.");
      System.exit(1);
    }

    ErrorReporter reporter = new ErrorReporter();
    Scanner scanner = new Scanner(stream, reporter);
    Parser parser = new Parser(scanner, reporter, debugMode);
    
    if (debugMode) System.out.println("Syntactic analysis...");
    Package AST = parser.parse();
    ASTDisplay display = new ASTDisplay();
    if (debugMode) System.out.println("Syntactic analysis complete: ");
    if (!reporter.hasErrors()) {
      display.showTree(AST);
    }
    
    if (debugMode) System.out.println("Identification...");
    Identification identifier = new Identification(AST, reporter);
    try {
      identifier.identify();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(4); 
    }
    if (reporter.hasErrors()) {
      System.out.println("Failed Identification");
      System.exit(4);
    }
    if (debugMode) System.out.println("Identification complete ");
    TypeChecking typechecker = new TypeChecking(AST, reporter);
    typechecker.typeCheck();

    if (reporter.hasErrors()) {
			System.out.println("Invalid miniJava program.");
			System.exit(4);
		}
		else {
			if (debugMode) System.out.println("Valid miniJava program.");
      // display.showTree(AST);
			System.exit(0);
		}

  }
}
