package miniJava;

import java.io.InputStream;

import miniJava.SyntacticAnalyzer.Scanner;

public class Recognizer {
  public static void main(String[] args) {
    System.out.print("Paste the code to parse in here: ");
    InputStream inputStream = System.in;

    ErrorReporter reporter = new ErrorReporter();
    Scanner scanner = new Scanner(inputStream, reporter);

    int scanCounts = 0;
    while (scanCounts++ < 1000) {
      scanner.scan().print();
    }
  }
}
