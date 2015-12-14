
public class CodeGenerator {
    private class Env {
        private HashMap<String, FunType
        
        
    String emitBuffer = new StrinBuilder();
    private void emit() {
        // store temp until stack calculations done
        // emitBuffer;
    }
     
     
     private String jvmFunType(FunType ft) {
        String str="";
        return str;
     }
     
     private String jvmType(FunType ft) {
        String str="";
        return str;
     }
     
     /* .class public Test
        .super java/lang/Object
        
        .method public static main([Ljava/lang/String;)V
        .limit stack 2 TODO
        
        
        .end method
        
        
        TODO call jasmin afterwards
     */
     
    private Env env;
    
    public void generateCode(Program p) {
        env = new Env();
        
        PDefs defs = (PDefs) p;
        
        LinkedList<Type> args = new LinkedList<Type>();
        args.add(new Type_int());
        env.updateFun("printInt", new FunType("runtime/printInt", args, new Type_void()))
        //TODO user def functions;
        
        for(Def f:defs.listdef_) {
            DFun df = (DFun)f;
            
            // skip creating local block and adding fun args
            
            generateStms(df.liststm_);
        }
        
    }
    
    private void generateStms(ListStms stms) {
        for(Stm stm:stms) {
            generateStm(stm);
        }
    }
    
    private void generateStm(Stm stm) {
        //take care of main fun special return
        stm.accept(new StmGenerator(), null);
    }
    
    private class StmGenerator implements Stm.Visitor<Void,Void> {
        public Void visit(SExp s, Void _) {
            generate(s.exp_);
            emit("pop"); // no double (never)
            
            return null;
        }
        
        //EInt "ldc " + e.integer_
        
        // EPlus e.exp_1.accept(this, _);
        // e.exp_2.accept(this, _);
        //emit("iadd");
        
        // EApp 
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
    
}
