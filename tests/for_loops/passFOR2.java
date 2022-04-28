// checks properly skipping condition
class Test {
  public static void main(String[] args) {
    for (int i = 0; i < 0; i = i+1) {
      System.out.println(-1);
    }
    System.out.println(2);
  }
}
