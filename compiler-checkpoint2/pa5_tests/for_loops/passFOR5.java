class Test {
  public static void main(String[] args) {
    for (int i = 0; ; i = i + 1) {
      System.out.println(i);
      if (i > 4) {
        return; 
      }
    }
  }
}