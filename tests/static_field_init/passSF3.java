class Test {
  public static void main(String[] args) {
    System.out.println(i);
    Test t = new Test();
    System.out.println(t.i + i);
    System.out.println(Other.o);
  }
  
  static int i = 1;
}

class Other {
  static int o = 3;
  int oo;
}