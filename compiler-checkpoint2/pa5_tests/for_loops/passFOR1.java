class Test {
  public static void main(String[] args) {
    int[] arr = new int[5];
    arr[0] = 0;
    arr[1] = 1;
    arr[2] = 0;
    arr[3] = 0;
    arr[4] = 0;

    // standard short form filter
    for (int i = 0; i < arr.length; i=i+1) 
      if (arr[i] > 0) 
        System.out.println(i);
  }
}
