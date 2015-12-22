import java.util.List;

import CPP.Absyn.DFun;
import CPP.Absyn.ListStm;

public class Function {
	public final List<String> cParameters;
	public final ListStm cStatements;
	public final DFun cFunDecl;


	public Function(List<String> params, ListStm stmts, DFun df) {
		cParameters = params;
		cStatements = stmts;
		cFunDecl = df;
	}
}
