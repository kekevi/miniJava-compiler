package miniJava;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

import java.io.File;
import java.io.FileInputStream;

public class Compiler {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("No file specified in first argument.");
      System.exit(1);
    }
    System.out.println("Reading from " + args[0]);
    FileInputStream stream = null; // just to get rid of the annoying warning
    try {
      stream = new FileInputStream(new File(args[0]));
    } catch (Exception e) {
      System.out.println("File path " + args[0] + " could not be read.");
      System.exit(1);
    }

    ErrorReporter reporter = new ErrorReporter();
    Scanner scanner = new Scanner(stream, reporter);
    Parser parser = new Parser(scanner, reporter);

    System.out.println("Syntactic analysis...");
    parser.parse();
    System.out.println("Syntactic analysis complete: ");
    if (reporter.hasErrors()) {
			System.out.println("Invalid miniJava program.");
			System.exit(4);
		}
		else {
			System.out.println("Valid miniJava program.");
			System.exit(0);
		}

  }
}
