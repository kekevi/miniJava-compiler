package miniJava.CodeGenerator;

import miniJava.mJAM.Machine.Reg;

public class Address {
  public Reg base;
  public int offset;
  public Address(Reg base, int offset) {
    this.base = base;
    this.offset = offset;
  }
}
