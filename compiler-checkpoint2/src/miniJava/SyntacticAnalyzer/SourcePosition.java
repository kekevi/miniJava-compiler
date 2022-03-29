package miniJava.SyntacticAnalyzer;

public class SourcePosition {
  public int start, finish;

  public SourcePosition (int s, int f) {
    start = s;
    finish = f;
  }

  public SourcePosition (int sf) {
    this(sf, sf);
  }
  
  public SourcePosition () {
    this(0, 0);
  }
  
  public String toString() {
    return "(" + start + ", " + finish + ")";
  }
}
