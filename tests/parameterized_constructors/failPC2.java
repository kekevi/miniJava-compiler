class Main {

  public Main(int i) {}
  public static void main(String[] args) {
    Main m = new Main(); // default constructor should not work anymore.
    getSomeNum(4); // this shouldn't work either
  }

  private static int getSomeNum() {
    return -1;
  }
}
