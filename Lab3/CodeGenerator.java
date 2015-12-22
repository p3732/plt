import java.nio.file.*

import java.util.LinkedList;

import CPP.PrettyPrinter;
import CPP.Absyn.*;

public class CodeGenerator {
    Env env;
    String filename;
    PrintWriter file;
    LinkedList<String> emitBuffer = new LinkedList<String>();
    int maxLocals=0;
    int maxStackSize=0;
    boolean returnStm = false;

    public CodeGenerator(Env env) {
        this.env = env;
    }

    private void emit(String s) {
        // store temp until stack calculations done
        emitBuffer.add(s);
    }

    private void functionDone(String fnName) {
        file.write("");
        file.write(".method public static" + fnName);
        if(maxLocals > 0)
            file.write("  .limit locals " + maxLocals);
        if(maxStackSize > 0)
            file.write("  .limit stack " + maxStackSize);

        // flush buffer
        for(String s:emitBuffer) {
            file.write(s);
        }

        file.write("");
        file.write(".end method");
        emitBuffer.clear();
        maxLocals=0;
        maxStackSize=0;
        file.flush();
    }

    private void startEmiting() {
        // open file
        file = new PrintWriter(new FileWriter(filename+".j"));

	    // header
        file.write(".class public "+filename);
        file.write(".super java/lang/Object");
        file.write("");
        file.write(".method public static main([Ljava/lang/String;)V");
        file.write("");
        file.write(".method public <init>()V");
        file.write("aload_0");
        file.write("invokespecial java/lang/Object/<init>()V");
        file.write("return");
        file.write(".end method");
        file.write(".method public static main([Ljava/lang/String;)V");
        file.write(".limit locals 1");
        file.write("invokestatic good03/main()I");
        file.write("pop");
        file.write("return");
        file.write(".end method");
        file.write(";;########################");
        file.write(";;# end boilerplate code #");
        file.write(";;########################");
    }

    private void stopEmiting() {
        file.close();
    }

    private String jvmFunType(FunType ft) {
        String str="";
        return str;
     }

     private String jvmType(FunType ft) {
        String str="";
        return str;
     }

    private Env env;

    public void generateCode(Program p) {
        // TODO call jasmin afterwards
        PDefs defs = (PDefs) p;

        LinkedList<Type> args = new LinkedList<Type>();
        args.add(new Type_int());
        env.updateFun("printInt", new FunType("Runtime/printInt", args, new Type_void()));

        //TODO user def functions;
        for(Def f:defs.listdef_) {
            DFun df = (DFun)f;

            // skip creating local block and adding fun args
            generateStms(df.liststm_);

            // take care of main fun special return
            if(!returnStm && df.id_=="main") {
                emit("iconst_0");
                emit("ireturn");
            }

            functionDone(df.id_);
        }
    }

    private void generateStms(ListStms stms) {
        for(Stm stm:stms) {
            generateStm(stm);
        }
    }

    private void generateStm(Stm stm) {
        stm.accept(new StmGenerator(), null);
    }

    private class StmGenerator implements Stm.Visitor<Void,Void> {
        public Void visit(SExp s, Void _) {
            generate(s.exp_);
            emit("pop"); // no double (never)

            return null;
        }
    }

    private class ExpGenerator implements Exp.Visitor<Void,Void> {
        // basic
        public Type visit(ETrue e, Void v) { emit(""); return null; }
        public Type visit(EFalse e, Void v) { emit(""); return null; }
        public Type visit(EInt e, Void v) { emit("ldc " + e.integer_); return null; }
        public Type visit(EDouble e, Void v) { throw new RuntimeExcemit(""); return null; }

        // var, function
        public Type visit(EId e, Void v) { return env.lookupVar(e.id_); }
        //TODO parameter types
        public Type visit(EApp e, Void v) {
            for(Exp arg: e.listexp_) {
                generateExp(arg);
            }
            FunType ft = env.lookupFun(e.id_);
            emit("invokestatic" + ft.javaRef + jvmFunType(ft));

            switch(typeCode(ft.outtyp)) {
                case VOID:
                    emit("bipush 0"); break;
            }
            return null;
        }

        //++ -- (implicit variable via parser)
        public Type visit(EPostIncr e, Void v) { return numberType(e.exp_, env, "post++"); }
        public Type visit(EPostDecr e, Void v) { return numberType(e.exp_, env, "post--"); }
        public Type visit(EPreIncr e, Void v) { return numberType(e.exp_, env, "++pre"); }
        public Type visit(EPreDecr e, Void v) { return numberType(e.exp_, env, "--pre"); }

        // * / + - assignment(implicit variable via parser)
        public Type visit(ETimes e, Void v) { return intDoubleType(e.exp_1, e.exp_2, env, "*"); }
        public Type visit(EDiv e, Void v) { return intDoubleType(e.exp_1, e.exp_2, env, "/"); }

        public Type visit(EPlus e, Void v) {
            accept(e.exp_1, e.exp_2)
            emit("iadd");
            return null;
        }

        //
        public Type visit(EMinus e, Void v) { return intDoubleType(e.exp_1, e.exp_2, env, "-"); }
        public Type visit(EAss e, Void v) { return sameType(e.exp_1, e.exp_2, env, "assignment"); }

        // < > >= ... && ||
        public Type visit(ELt e, Void v) { return boolType(e.exp_1, e.exp_2, env, "<"); }
        public Type visit(EGt e, Void v) { return boolType(e.exp_1, e.exp_2, env, ">"); }
        public Type visit(ELtEq e, Void v) { return boolType(e.exp_1, e.exp_2, env, "<="); }
        public Type visit(EGtEq e, Void v) { return boolType(e.exp_1, e.exp_2, env, ">="); }
        public Type visit(EEq e, Void v) { return boolType(e.exp_1, e.exp_2, env, "=="); }
        public Type visit(ENEq e, Void v) { return boolType(e.exp_1, e.exp_2, env, "!="); }
        public Type visit(EAnd e, Void v) { return boolType(e.exp_1, e.exp_2, env, "&&"); }
        public Type visit(EOr e, Void v) { return boolType(e.exp_1, e.exp_2, env, "||"); }

        private void accept(Exp e1, Exp e2) {
            e1.accept(this, null);
            e2.accept(this, null);
        }
    }
}
