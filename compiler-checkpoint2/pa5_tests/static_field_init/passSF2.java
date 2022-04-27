class Test {
  public static void main(String[] args) {
    t.i = 2;
    System.out.println(t.i);
    t = new Test();
    System.out.println(t.i + 3);
  }
  
  static Test t = new Test();
  int i;
}