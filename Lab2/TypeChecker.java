import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import CPP.PrettyPrinter;
import CPP.Absyn.*;

public class TypeChecker {

    private static class FunType {
        public LinkedList<Type> intyps;
        public Type outtyp;

        public FunType(LinkedList<Type> intyps, Type outtyp) {
            this.intyps = intyps;
            this.outtyp = outtyp;
        }
    }

    private static class Env {
        // functions
        public HashMap<String, FunType> signature = new HashMap<String, FunType>();
        // current variables
        public LinkedList<HashMap<String,Type>> contexts = new LinkedList<HashMap<String,Type>>();

        private Env() { }

        public static Env empty() {
            Env env = new Env();
            return env;
        }

        public FunType lookupFun(String id) {
            if (signature.containsKey(id))
                return signature.get(id);
            else
                throw new TypeException("Function "+ id +" not defined");
        }

        public Type lookupVar(String id) {
            // search from current to earlier contexts
            ListIterator<HashMap<String, Type>> listIterator = contexts.listIterator(contexts.size());

            while(listIterator.hasPrevious()) {
                HashMap<String, Type> context = listIterator.previous();
                if (context.containsKey(id))
                    return context.get(id);
            }
            throw new TypeException("Var "+ id +" not declared in any context.");
        }

        public Env updateVar(String id, Type typ) {
            if (contexts.getLast().containsKey(id)) {
                throw new TypeException("Var "+ id +" already declared");
            } else {
                contexts.getLast().put(id, typ);
                return this;
            }
        }

        public Env updateFun(String id, FunType funtyp) {
            if (signature.containsKey(id)) {
                throw new TypeException("Function "+ id +" already declared");
            } else {
                signature.put(id, funtyp);
                return this;
            }
        }

        public Env newBlock() {
            contexts.add(new HashMap<String,Type>());
            return this;
        }

        public Env exitBlock() {
            contexts.pollLast();
            return this;
        }
    }


    public void typecheck(Program p) {
        PDefs defs = (PDefs)p;
        Env env = Env.empty();
        LinkedList<Type> args = new LinkedList<Type>();

        // predefined functions
        env.updateFun("readInt", new FunType(new LinkedList<Type>(args), new Type_int()));
        args.add(new Type_int());
        env.updateFun("printInt", new FunType(new LinkedList<Type>(args), new Type_void()));
        args.clear();
        env.updateFun("readDouble", new FunType(new LinkedList<Type>(args), new Type_double()));
        args.add(new Type_double());
        env.updateFun("printDouble", new FunType(new LinkedList<Type>(args), new Type_void()));

        for(Def f: defs.listdef_) {
            DFun df = (DFun)f;
            LinkedList<Type> funArgs = new LinkedList<Type>();

            for(Arg arg: df.listarg_) {
                ADecl decl = (ADecl)arg;
                funArgs.add(decl.type_);
            }
            env.updateFun(df.id_, new FunType(funArgs,df.type_));
        }

        // check for valid main method
        FunType main = env.lookupFun("main");
        if (!(main.outtyp instanceof Type_int) || !main.intyps.isEmpty()) {
            throw new TypeException("Main method has incorrect format");
        }

        for(Def f: defs.listdef_) {
            DFun df = (DFun)f;

            env.newBlock();
            env.updateVar("return", df.type_);
            for(Arg arg: df.listarg_) {
                ADecl decl = (ADecl)arg;
                env.updateVar(decl.id_,decl.type_);
            }
            typecheckStms(env, df.liststm_);
            env.exitBlock();
        }
    }

    public void typecheckStms(Env env, ListStm stms) {
        for(Stm stm:stms) {
            typecheckStm(env, stm);
        }
    }

    public void typecheckStm(Env env, Stm stm) {
        stm.accept(new StmChecker(), env);
    }

    private class StmChecker implements Stm.Visitor<Void, Env> {
        public Void visit(SExp p, Env env) {
            p.exp_.accept(new ExpInferer(), env);
            return null;
        }

        public Void visit(SDecls p, Env env) {
            for(String id:p.listid_) {
                env.updateVar(id, p.type_);
            }
            return null;
        }

        public Void visit(SInit p, Env env) {
            Type type = p.exp_.accept(new ExpInferer(), env);
            if(!p.type_.equals(type)) {
                throw new TypeException("can't init "+p.id_+" of type "+p.type_+" with given type "+type);
            }
            env.updateVar(p.id_, p.type_);
            return null;
        }

        public Void visit(SReturn p, Env env) {
            Type retType = env.lookupVar("return");
            //void
                Type expType = p.exp_.accept(new ExpInferer(),env);
                if (!retType.equals(expType)) {
                    throw new TypeException("Returned type doesn't match declared one.");
                }
            return null;
        }

        public Void visit(SWhile p, Env env) {
            Type expType = p.exp_.accept(new ExpInferer(),env);
            if(!expType.equals(new Type_bool())) {
                throw new TypeException("Expression in while loop is not of type bool.");
            } else {
                env.newBlock();
                p.stm_.accept(this, env);
                env.exitBlock();
            }
            return null;
        }

        public Void visit(SBlock p, Env env) {
            env.newBlock();
            for(Stm stm:p.liststm_) {
                stm.accept(this, env);
            }
            env.exitBlock();
            return null;
        }

        public Void visit(SIfElse p, Env env) {
            Type expType = p.exp_.accept(new ExpInferer(),env);
            if(!expType.equals(new Type_bool())) {
                throw new TypeException("Expression in while loop is not of type bool.");
            } else {
                env.newBlock();
                p.stm_1.accept(this, env);
                env.exitBlock();
                env.newBlock();
                p.stm_2.accept(this, env);
                env.exitBlock();
            }
            return null;
        }
    }

    private class ExpInferer implements Exp.Visitor<Type, Env> {
        Type_void v = new Type_void();
        Type_int i = new Type_int();
        Type_double d = new Type_double();
        Type_bool b = new Type_bool();

        // basic
        public Type visit(ETrue e, Env env) { return b; }
        public Type visit(EFalse e, Env env) { return b; }
        public Type visit(EInt e, Env env) { return i; }
        public Type visit(EDouble e, Env env) { return d; }

        // var, function
        public Type visit(EId e, Env env) { return env.lookupVar(e.id_); }
        //TODO parameter types
        public Type visit(EApp e, Env env) {
            FunType ftype = env.lookupFun(e.id_);
            // check amount of arguments
            if(ftype.intyps.size() != e.listexp_.size()) {
                throw new TypeException("Arguments for function call of " + e.id_ +
                    " has wrong amount of arguments " + ftype.intyps + " " + e.listexp_.size());
            }

            // check for each argument whether it's type is what it's supposed to be
            ListIterator<Type> intyps = ftype.intyps.listIterator();
            for(Exp exp:e.listexp_) {
                Type type = exp.accept(this, env);
                if(!intyps.next().equals(type)) {
                    throw new TypeException("Argument for function call to " + e.id_ +
                        " has wrong type " + type + " at position " + intyps.previousIndex());
                }
            }

            return env.lookupFun(e.id_).outtyp;
        }

        //++ -- (implicit variable via parser)
        public Type visit(EPostIncr e, Env env) { return numberType(e.exp_, env, "post++"); }
        public Type visit(EPostDecr e, Env env) { return numberType(e.exp_, env, "post--"); }
        public Type visit(EPreIncr e, Env env) { return numberType(e.exp_, env, "++pre"); }
        public Type visit(EPreDecr e, Env env) { return numberType(e.exp_, env, "--pre"); }

        // * / + - assignment(implicit variable via parser)
        public Type visit(ETimes e, Env env) { return intDoubleType(e.exp_1, e.exp_2, env, "*"); }
        public Type visit(EDiv e, Env env) { return intDoubleType(e.exp_1, e.exp_2, env, "/"); }
        public Type visit(EPlus e, Env env) { return intDoubleType(e.exp_1, e.exp_2, env, "+"); }
        public Type visit(EMinus e, Env env) { return intDoubleType(e.exp_1, e.exp_2, env, "-"); }
        public Type visit(EAss e, Env env) { return sameType(e.exp_1, e.exp_2, env, "assignment"); }

        // < > >= ... && ||
        public Type visit(ELt e, Env env) { return boolType(e.exp_1, e.exp_2, env, "<"); }
        public Type visit(EGt e, Env env) { return boolType(e.exp_1, e.exp_2, env, ">"); }
        public Type visit(ELtEq e, Env env) { return boolType(e.exp_1, e.exp_2, env, "<="); }
        public Type visit(EGtEq e, Env env) { return boolType(e.exp_1, e.exp_2, env, ">="); }
        public Type visit(EEq e, Env env) { return boolType(e.exp_1, e.exp_2, env, "=="); }
        public Type visit(ENEq e, Env env) { return boolType(e.exp_1, e.exp_2, env, "!="); }
        public Type visit(EAnd e, Env env) { return boolType(e.exp_1, e.exp_2, env, "&&"); }
        public Type visit(EOr e, Env env) { return boolType(e.exp_1, e.exp_2, env, "||"); }

        private Type numberType(Exp e, Env env, String symbol) {
            Type type = e.accept(this, env);
            if(type.equals(d) || type.equals(i)) {
                return type;
            } else {
                throw new TypeException("Couldn't apply " + symbol + " to variable not of type double or int.");
            }
        }

        private Type sameType(Exp e1, Exp e2, Env env, String symbol) {
            Type type1 = e1.accept(this, env);
            Type type2 = e2.accept(this, env);
            if(!type1.equals(type2)) {
                throw new TypeException("Arguments for "+ symbol +
                " don't match (" + type1 + ", " + type2 + ")");
            }
            return type1;
        }

        private Type intDoubleType(Exp e1, Exp e2, Env env, String symbol) {
            Type type = sameType(e1, e2, env, symbol);
            if(!type.equals(i) && !type.equals(d)) {
                throw new TypeException(symbol + " not applicable for " + type);
            }
            return type;
        }

        private Type boolType(Exp e1, Exp e2, Env env, String symbol) {
            Type type = sameType(e1, e2, env, symbol);
            if(!type.equals(b) && !type.equals(i) && !type.equals(d)) {
                throw new TypeException("Comparison " + symbol +
                " not possible, given arguments are not comparable.");
            }
            return b;
        }
    }
}

