package miniJava;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.Translation;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.ContextualAnalyzer.TypeChecking;

import java.io.File;
import java.io.FileInputStream;

public class Compiler {
  public static void report(String label, ErrorReporter reporter) {
    if (reporter.hasErrors()) {
      System.out.println("Failed " + label + ".");
      System.exit(4);
    }
  }
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("No file specified in first argument.");
      System.exit(1);
    }
    
    boolean debugMode = false;
    boolean showTree = false;
    if (args.length >= 2) {
      // debugMode = args[1].equals("true");
      showTree = args[1].equals("--showtree");
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
    
    // Parsing and Scanning 
    Scanner scanner = new Scanner(stream, reporter);
    Parser parser = new Parser(scanner, reporter, debugMode);

    Package ast = parser.parse();
    report("parsing", reporter);

    ASTDisplay display = new ASTDisplay();
    if (showTree) display.showTree(ast);

    // Contextual Analysis

    Identification identifier = new Identification(ast, reporter);
    TypeChecking typechecker = new TypeChecking(ast, reporter);

    try {
      identifier.identify();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(4); 
    }
    report("identification", reporter);
    
    typechecker.typeCheck();
    report("type checking", reporter);

    // Code Generation
    Translation translator = new Translation(ast, reporter);

    translator.translate();
    report("code generation", reporter);

    // Writing
    String outputFile = args[0].contains(".") ? args[0].substring(0, args[0].lastIndexOf('.')) + ".mJAM" : args[0] + ".mJAM";
    System.out.println(outputFile);
    ObjectFile objectFile = new ObjectFile(outputFile);
    if (objectFile.write()) {
      System.out.println("Failed at writing object file. (.mJAM)");
      System.exit(4);
    }
    System.exit(0);

  }
}
