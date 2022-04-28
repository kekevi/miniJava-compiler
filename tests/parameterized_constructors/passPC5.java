class A {
  boolean a;
  int b;
  Main c;

  A (boolean a, int b, Main c) {
    this.a = a;
    this.b = b;
    this.c = c;
    c.i = 3;
    c.hasBeenInit = true;
  }
}

class Main {
  public static boolean hasBeenInit;
  int i;
  public static void main(String[] args) {
    Main m = new Main();
    A a = m.createUselessA();
    
    if (hasBeenInit == true) {
      if (m.i == 3)
        System.out.println(5);
      else
        System.out.println(-1);
    } else
      System.out.println(-2);

    if (m.i == 3)
      System.out.println(6);

    if (hasBeenInit)
      System.out.println(7);

    if (true == true) 
      System.out.println(8);
  }

  A createUselessA() {
    return new A(true, 12, this);
  }

}
