/*
  https://stackoverflow.com/questions/61418866/static-variable-initialization-that-reference-each-other
  any recursive field references should just be given up

  in this file:
  - Main.main() is called ==> initialize Main class
  - in trying to initialize Main.i, Other.i is called ==> initialize Other class
  - in trying to initialize Other.i, Main.i is called (! Main is in initialization stack !) 
      ==> ignore it as default, hence Other.i = 4
  - going back to Main, we do Main.i = 1+Other.i = 1+4 = 5 
*/
class Main {
  public static void main(String[] args) {
    System.out.println(Other.i);
    System.out.println(Main.i);
  }
  
  static int i = 1 + Other.i; // should be 5
}

class Other {
  static int i = 4 + Main.i; // should be 4
}