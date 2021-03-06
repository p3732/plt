package CPP.Absyn; // Java Package generated by the BNF Converter.

public abstract class Exp implements java.io.Serializable {
  public abstract <R,A> R accept(Exp.Visitor<R,A> v, A arg);
  public interface Visitor <R,A> {
    public R visit(CPP.Absyn.ETrue p, A arg);
    public R visit(CPP.Absyn.EFalse p, A arg);
    public R visit(CPP.Absyn.EInt p, A arg);
    public R visit(CPP.Absyn.EDouble p, A arg);
    public R visit(CPP.Absyn.EId p, A arg);
    public R visit(CPP.Absyn.EApp p, A arg);
    public R visit(CPP.Absyn.EPostIncr p, A arg);
    public R visit(CPP.Absyn.EPostDecr p, A arg);
    public R visit(CPP.Absyn.EPreIncr p, A arg);
    public R visit(CPP.Absyn.EPreDecr p, A arg);
    public R visit(CPP.Absyn.ETimes p, A arg);
    public R visit(CPP.Absyn.EDiv p, A arg);
    public R visit(CPP.Absyn.EPlus p, A arg);
    public R visit(CPP.Absyn.EMinus p, A arg);
    public R visit(CPP.Absyn.ELt p, A arg);
    public R visit(CPP.Absyn.EGt p, A arg);
    public R visit(CPP.Absyn.ELtEq p, A arg);
    public R visit(CPP.Absyn.EGtEq p, A arg);
    public R visit(CPP.Absyn.EEq p, A arg);
    public R visit(CPP.Absyn.ENEq p, A arg);
    public R visit(CPP.Absyn.EAnd p, A arg);
    public R visit(CPP.Absyn.EOr p, A arg);
    public R visit(CPP.Absyn.EAss p, A arg);

  }

}
