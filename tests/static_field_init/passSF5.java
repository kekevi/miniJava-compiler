class Main {
  public static void main(String[] args) {
    o.o = 5;
    System.out.println(o.o);
  }
  
  static Other o = new Other();
}

class Other {
  int o;
}