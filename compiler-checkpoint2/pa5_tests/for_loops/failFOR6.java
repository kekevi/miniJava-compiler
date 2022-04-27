class Test {
  public static void main(String[] args) {
    int i = 0;
    for (return; i = 1; i = i+1) // cannot have return in for's `()` stmts
      System.out.println(-1);
  }
}
