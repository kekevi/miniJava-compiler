class Main {
  int i;
  int j;

  public Main(int i, int another) {
    this.i = i;
    j = another;
  }
  public static void main(String[] args) {
    Main m = new Main(4, 5); 
    System.out.println(m.i);
    System.out.println(m.j);
  }
}
