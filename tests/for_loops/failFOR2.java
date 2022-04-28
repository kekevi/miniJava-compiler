class Test {
  public static void main(String[] args) {
    int i = 0;
    for (i = 0; i; i = i + 1) {
      // type checking, not a boolean expr
    }
  }
}
