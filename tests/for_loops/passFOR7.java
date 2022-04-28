/*
  Sieve of Eratosthenes
  * why don't we have boolean arrays? so annoying
*/

class Main {
  static int[] sieve(int n) {
    int[] prime = new int[n+1];
    for (int i = 0; i <= n; i=i+1) {
      prime[i] = 1;
    }

    for (int p = 2; p*p <= n; p = p+1) {
      if (prime[p] == 1) {
        for (int i = p*p; i <= n; i = i + p) {
          prime[i] = 0;
        }
      }
    }
    
    return prime;
  }

  public static void main(String[] args) {
    int[] primes = sieve(999);
    for (int i = 0; i < 50; i=i+1) {
      if (primes[i] == 1)
      System.out.println(i);
    }
  }
}
