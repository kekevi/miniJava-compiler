class Test {
  static int i;
  public static void main(String[] args) {
    for (i = 0; ; i = i + 1) {
      System.out.println(i);
      if (i > 5) {
        return; 
      }
    }
  }
}