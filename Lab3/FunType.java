import java.util.LinkedList;
import CPP.Absyn.Type;

public class FunType {
    public LinkedList<Type> intyps;
    public Type outtyp;

    public FunType(LinkedList<Type> intyps, Type outtyp) {
        this.intyps = intyps;
        this.outtyp = outtyp;
    };
}

