// not actually a for loop test, but I realized I didn't pass return type checking into a while body
class Test {
  public static void main(String[] args) {
    int i = 0;
    while (i == 0) 
      return;
  }
}
