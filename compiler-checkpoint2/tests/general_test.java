class Test {
  static int pi;
  public static void main(String[] args) {
    Other a = new Other();
    a.build();
    // int x = a.add(2).increment(); // thank god miniJava doesn't allow this, I would've had no idea how
    Integer x_temp = a.add(2);
    System.out.println(a.num.i);
    int xi = x_temp.i;
    System.out.println(xi);
    // System.out.println(x_temp.num);
    int x = x_temp.increment();
    System.out.println(x);
    String hey = new String();
  }

  static int foo() {
    int i = 1;
    i = i+1;
    return i;
  }
}

class Other {
  public Integer num;

  public Integer add(int v) {
    num.i = num.i + v;
    return num;
  }

  public void build() {
    num = new Integer();
  }
}

class Integer {
  public int i;
  public int increment() {
    return i+1;
  }
}
