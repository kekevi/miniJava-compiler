class Main {
  public static void main(String[] args) {
    int[] y = new int[3];
    int x = Other.arr.length;
    // Other.arr.length = 4;
    int z = y.length + 3;
  }
}

class Other {
  public static int[] arr;
}
