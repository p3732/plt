import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import CPP.PrettyPrinter;
import CPP.Absyn.*;

public class Interpreter {

    private BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    private static class Env {
        // functions
        public HashMap<String, DFun> signatures = new HashMap<String, DFun>();
        // current variables for Interpreter
        private LinkedList<HashMap<String,Object>> contexts = new LinkedList<HashMap<String,Object>>();

        // base environment with all declared functions. used as singleton.
        private static Env baseEnv = null;

        private Env() { }

        private Env(Env env) {
            this.signatures = new HashMap(env.signatures);
            this.contexts = new LinkedList();
        }

        public static Env empty() {
            Env env;
            if (baseEnv==null)
                env = new Env();
            else
                env = new Env(baseEnv);
            env.newBlock();
            return env;
        }

        public static void setAsBaseEnv(Env env) {
            baseEnv = env;
        }

        public DFun lookupFunction(String id) {
            return signatures.get(id);
        }

        public Object lookupVar(String id) {
        	// search from current to earlier contexts
            ListIterator<HashMap<String, Object>> listIterator = contexts.listIterator(contexts.size());

            while(listIterator.hasPrevious()) {
                HashMap<String, Object> context = listIterator.previous();
                if (context.containsKey(id))
                    return context.get(id);
            }
            return null;
        }

        public void updateVar(String id, Object varValue) {
        	// search from current to earlier contexts
            ListIterator<HashMap<String, Object>> listIterator = contexts.listIterator(contexts.size());

            while(listIterator.hasPrevious()) {
                HashMap<String, Object> context = listIterator.previous();
                if (context.containsKey(id)) {
                    context.put(id, varValue);
                    break;
                }
            }
        }

        public void declareVar(String id, Object varValue) {
        	contexts.getLast().put(id, varValue);
        }

        public void declareFunction(String id, DFun f) {
           	signatures.put(id, f);
        }

        public void newBlock() {
            contexts.add(new HashMap<String,Object>());
        }

        public void exitBlock() {
            contexts.pollLast();
        }
    }

    public void interpret(Program p) {
    	PDefs defs = (PDefs)p;
        Env baseEnv = Env.empty();

        // Iterate over all function declarations
        for(Def f: defs.listdef_) {
            DFun df = (DFun)f;
	    baseEnv.declareFunction(df.id_, df);
        }
        Env.setAsBaseEnv(baseEnv);

        // interpret function main()
        Env env = Env.empty();
        DFun f = env.lookupFunction("main");
        f.accept(new FunctionInterpreter(), env);
    }

    private class FunctionInterpreter implements Def.Visitor<Object, Env> {
        public Object visit(DFun df, Env env) {
            Object ret = null;
            for(Stm stm : df.liststm_) {
                ret = stm.accept(new StmEval(), env);
                if (ret != null)
                    break;
            }
            return ret;
        }
    }

    private class StmEval implements Stm.Visitor<Object, Env> {
        public Object visit(SExp expr, Env env) {
            expr.exp_.accept(new ExpEval(), env);
            return null;
        }

        public Object visit(SDecls df, Env env) {
            for(String id:df.listid_) {
                env.declareVar(id,null);
            }
            return null;
        }

        public Object visit(SInit df, Env env) {
        	Object varValue = df.exp_.accept(new ExpEval(), env);
        	env.declareVar(df.id_, varValue);
            return null;
        }

        public Object visit(SReturn df, Env env) {
        	return df.exp_.accept(new ExpEval(), env);
        }

        public Object visit(SWhile df, Env env) {
       	    Object ret = null;
       	    env.newBlock();
       	    while ((boolean) df.exp_.accept(new ExpEval(), env)) {
       	        ret = df.stm_.accept(new StmEval(), env);
       	        if (ret!= null)
       	            break;
       	    }
       	    env.exitBlock();
       	    return ret;
        }

        public Object visit(SBlock df, Env env) {
        	Object ret = null;
        	env.newBlock();
        	for(Stm stm : df.liststm_) {
                ret = stm.accept(new StmEval(), env);
                if (ret!= null)
                    break;
            }
        	env.exitBlock();
            return ret;
        }


        public Object visit(SIfElse df, Env env) {
        	Object ret;
        	env.newBlock();
        	if ((boolean) df.exp_.accept(new ExpEval(), env)) {
        		ret = df.stm_1.accept(new StmEval(), env);
        	} else {
        		ret = df.stm_2.accept(new StmEval(), env);
        	}
        	env.exitBlock();
            return ret;
        }
    }

    private class ExpEval implements Exp.Visitor<Object, Env> {
        // basic
        public Boolean visit(ETrue e, Env env) { return true; }
        public Boolean visit(EFalse e, Env env) { return false; }
        public Integer visit(EInt e, Env env) { return e.integer_; }
        public Double visit(EDouble e, Env env) { return e.double_; }

        // var, function
        public Object visit(EId e, Env env) {
            Object v = env.lookupVar(e.id_);
            if (v==null) {
                throw new RuntimeException(e.id_ + " was used uninitialized!");
            }
            return v;
        }
        public Object visit(EApp e, Env env) { return evaluateFunction(e, env); }

        //++ --
        public Object visit(EPostIncr e, Env env) {
        	String var = ((EId) e.exp_).id_;
        	Object oldValue = env.lookupVar(var);
        	if (oldValue instanceof Integer) {
        		env.updateVar(var, ((Integer) oldValue).intValue() + 1);
        	} else if (oldValue instanceof Double) {
        		env.updateVar(var, ((Double) oldValue).doubleValue() + 1.0);
        	}
        	return oldValue;
        }
        public Object visit(EPostDecr e, Env env) {
        	String var = ((EId) e.exp_).id_;
        	Object oldValue = env.lookupVar(var);
        	if (oldValue instanceof Integer) {
        		env.updateVar(var, ((Integer) oldValue).intValue() - 1);
        	} else if (oldValue instanceof Double) {
        		env.updateVar(var, ((Double) oldValue).doubleValue() - 1.0);
        	}

        	return oldValue;
        }
        public Object visit(EPreIncr e, Env env) {
        	String var = ((EId) e.exp_).id_;
        	Object oldValue = env.lookupVar(var);
        	if (oldValue instanceof Integer) {
        		Integer newValue = ((Integer) oldValue).intValue() + 1;
        		env.updateVar(var, newValue);
        		return newValue;
        	} else if (oldValue instanceof Double) {
        		Double newValue = ((Double) oldValue).doubleValue() + 1.0;
        		env.updateVar(var, newValue);
        		return newValue;
        	} else return null;
        }
        public Object visit(EPreDecr e, Env env) {
        	String var = ((EId) e.exp_).id_;
        	Object oldValue = env.lookupVar(var);
        	if (oldValue instanceof Integer) {
        		Integer newValue = ((Integer) oldValue).intValue() - 1;
        		env.updateVar(var, newValue);
        		return newValue;
        	} else if (oldValue instanceof Double) {
        		Double newValue = ((Double) oldValue).doubleValue() - 1.0;
        		env.updateVar(var, newValue);
        		return newValue;
        	} else return null;
        }

        // * / + - assignment
        public Object visit(ETimes e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);

        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) * ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) * ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) * ((int)v2);
        	return ((double)v1) * ((double)v2);
        }
        public Object visit(EDiv e, Env env) { 
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);

        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) / ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) / ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) / ((int)v2);
        	return ((double)v1) / ((double)v2);
        }
        public Object visit(EPlus e, Env env) { 
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);

        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) + ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) + ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) + ((int)v2);
        	return ((double)v1) + ((double)v2);
        }
        public Object visit(EMinus e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);

        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) - ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) - ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) - ((int)v2);
        	return ((double)v1) - ((double)v2);
        }
        public Object visit(EAss e, Env env) {
        	String id = ((EId) e.exp_1).id_;
        	Object v2 = e.exp_2.accept(this, env);
        	env.updateVar(id, v2);
        	return v2;
        }

        // < > >= ... && ||
        public Boolean visit(ELt e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);
        	
        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) < ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) < ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) < ((int)v2);
        	return ((double)v1) < ((double)v2);
        }
        public Boolean visit(EGt e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);
        	
        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) > ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) > ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) > ((int)v2);
        	return ((double)v1) > ((double)v2);
        }
        public Boolean visit(ELtEq e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);
        	
        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) <= ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) <= ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) <= ((int)v2);
        	return ((double)v1) <= ((double)v2);
        }
        public Boolean visit(EGtEq e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);
        	
        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) >= ((int)v2);
        	else if ((v1 instanceof Integer) && (v2 instanceof Double)) return ((int)v1) >= ((double)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Integer)) return ((double)v1) >= ((int)v2);
        	return ((double)v1) >= ((double)v2);
        }
        public Boolean visit(EEq e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);
        	
        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) { return ((int)v1) == ((int)v2); }
        	else if ((v1 instanceof Double) && (v2 instanceof Double)) return ((double)v1) == ((double)v2);
        	else if ((v1 instanceof Boolean) && (v2 instanceof Boolean)) return ((boolean)v1) == ((boolean)v2);
        	// TODO: Which other types may be compared for equality
        	else throw new RuntimeException("You cannot compare two objects of different types for equality.");
        }
        public Boolean visit(ENEq e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	Object v2 = e.exp_2.accept(this, env);
        	
        	if ((v1 instanceof Integer) && (v2 instanceof Integer)) return ((int)v1) != ((int)v2);
        	else if ((v1 instanceof Double) && (v2 instanceof Double)) return ((double)v1) != ((double)v2);
           	else if ((v1 instanceof Boolean) && (v2 instanceof Boolean)) return ((boolean)v1) != ((boolean)v2);
        	// TODO: Which other types may be compared for inequality
        	else throw new RuntimeException("You cannot compare two objects of different types for inequality.");
        }
        public Boolean visit(EAnd e, Env env) { 
        	Object v1 = e.exp_1.accept(this, env);
        	if (v1 instanceof Boolean) {
        		if (!((boolean) v1)) return false;
        	}
        	if (v1 instanceof Integer) {
        		if (((int)v1) != 1 ) {
        			return false;
        		}
        	}
        	Object v2 = e.exp_2.accept(this, env);

        	if (v2 instanceof Boolean) return (boolean) v2;
        	else {
        		return (((int)v2) == 1 ? true : false);
        	}
        }
        public Boolean visit(EOr e, Env env) {
        	Object v1 = e.exp_1.accept(this, env);
        	if (v1 instanceof Boolean) {
        		if (((boolean) v1)) return true;
        	}
        	if (v1 instanceof Integer) {
        		if (((int)v1) == 1 ) return true;
        	}
        	Object v2 = e.exp_2.accept(this, env);

        	if (v2 instanceof Boolean) return (boolean) v2;
        	else {
        		return (((int)v2) == 1 ? true : false);
        	}
        }
        		
        private Object evaluateFunction(EApp e, Env env) {
        	// Evaluate the arguments
        	LinkedList<Object> argVals = new LinkedList<Object>();
        	for (Exp exp: e.listexp_) {
        		argVals.add(exp.accept(this, env));
        	}

        	//// built-in methods
        	switch (e.id_) {
            	case "printInt": {
            		Integer i = (Integer) argVals.getFirst();
            		System.out.println(i);
            		return null;
            	}
            	case "printDouble": {
            		Double d = (Double) argVals.getFirst();
            		System.out.println(d);
            		return null;
            	}

            	case "readInt":
            	case "readDouble": {
            		String line = null;
            		try {
            				line = input.readLine();
				    } catch (IOException e1) {
					    System.err.println("There was a problem readin a line from stdin");
					    System.err.println(e1.getMessage());
					    throw new RuntimeException("Input Error.");
				    }
            		if (line != null) {
            			if (e.id_ == "readInt") {
            				return Integer.parseInt(line);
            			} else {
            				return Double.parseDouble(line);
            			}
            		} else {
				        throw new RuntimeException("Could not read the number from standard-input, the string was \"null\"");
            		}
            	}
            }
        	//// handle program-specific functions
        	DFun df = env.lookupFunction(e.id_);

        	// create new env with args
        	Env newEnv = Env.empty();
            ListIterator<Arg> argIds = df.listarg_.listIterator();
        	for(Object argVal:argVals) {
                ADecl ad = (ADecl)argIds.next();
        	    newEnv.declareVar(ad.id_,argVal);
        	}

        	return df.accept(new FunctionInterpreter(), newEnv);
        }
    }
}

