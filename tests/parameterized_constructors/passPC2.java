class Main {
  int i;
  public static void main(String[] args) {
    Main m = new Main(); // default constructor should still work
    m.i = 2;
    System.out.println(m.i);
  }
}
