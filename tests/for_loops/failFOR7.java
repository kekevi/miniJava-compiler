class Test {
  public static void main(String[] args) {
    int i = 0;
    for (i = 0; i == 1; i = i+1)
      int fail; // cannot init a variable in body stmt
  }
}