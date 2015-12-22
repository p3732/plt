import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import CPP.PrettyPrinter;
import CPP.Absyn.*;

public class Env {
	public enum Flow  {NORMAL, RETURN};
    // functions
    public HashMap<String, FunType> signature = new HashMap<String, FunType>();
    // current variables for TypeChecker
    public LinkedList<HashMap<String,Type>> contexts = new LinkedList<HashMap<String,Type>>();
    
    // current variables for Interpreter
    private LinkedList<HashMap<String,Object>> varContexts = new LinkedList<HashMap<String,Object>>();
    // functions
    private HashMap<String, Function> declFunctions = new HashMap<String, Function>();
    
    private Flow mFlowOfTheProgram = Flow.NORMAL;
    
   

	private final String cFunctionOfThisEnv; 
    
    private Env() {cFunctionOfThisEnv = "NoFunctionGivenForThisContext"; }
    
    private Env (String methodOfThisEnv) {
    	cFunctionOfThisEnv = methodOfThisEnv;
    }
    
    public static Env empty() {
        return new Env();
    }
    

    public FunType lookupFun(String id) {
        if (signature.containsKey(id))
            return signature.get(id);
        else
            throw new TypeException("Function "+ id +" not defined");
    }
    
    // used for Interpreter
    public Function lookupFunction(String id) {
//    	System.out.println("lookupFunction(" + id + ") called.");
//    	System.out.println("Printing available functions");
//    	System.out.println(declFunctions.keySet());
        if (declFunctions.containsKey(id))
            return declFunctions.get(id);
        else
            throw new RuntimeException("Function "+ id +" not defined");
    }
    
    

    public Type getTypeOfVar(String id) {
        // search from current to earlier contexts
        ListIterator<HashMap<String, Type>> listIterator = contexts.listIterator(contexts.size());

        while(listIterator.hasPrevious()) {
            HashMap<String, Type> context = listIterator.previous();
            if (context.containsKey(id))
                return context.get(id);
        }
        throw new TypeException("Var "+ id +" not declared in any context.");
    }
 // used for Interpreter
    public Object lookupVar(String id) {
    	// search from current to earlier contexts
        ListIterator<HashMap<String, Object>> listIterator = varContexts.listIterator(varContexts.size());

        while(listIterator.hasPrevious()) {
            HashMap<String, Object> context = listIterator.previous();
            if (context.containsKey(id))
                return context.get(id);
        }
        throw new RuntimeException("Var "+ id +" not declared in any context.");
    }
    public Env declareVar(String id, Object varValue) {
    	 if (varContexts.getLast().containsKey(id)) {
             throw new RuntimeException("Var "+ id +" already declared");
         } else {
         	varContexts.getLast().put(id, varValue);
             return this;
         }
    }
    
    public Env updateVar(String id, Type typ) {
        if (contexts.getLast().containsKey(id)) {
            throw new TypeException("Var "+ id +" already declared");
        } else {
            contexts.getLast().put(id, typ);
            return this;
        }
    }
 // used for Interpreter
    public Env updateVar(String id, Object varValue) {
    	// search from current to earlier contexts
        ListIterator<HashMap<String, Object>> listIterator = varContexts.listIterator(varContexts.size());

        while(listIterator.hasPrevious()) {
            HashMap<String, Object> context = listIterator.previous();
            if (context.containsKey(id)) {
            	context.put(id, varValue);
            	return this;
            }
            
        }
        throw new RuntimeException("Var "+ id +" not declared in any context.");
        
    }

    public Env updateFun(String id, FunType funtyp) {
        if (signature.containsKey(id)) {
            throw new TypeException("Function "+ id +" already declared");
        } else {
            signature.put(id, funtyp);
            return this;
        }
    }
    
    public Env updateFunction(String id, Function f) {
        if (declFunctions.containsKey(id)) {
            throw new TypeException("Function "+ id +" already declared");
        } else {
        	declFunctions.put(id, f);
//        	System.out.println(id + " declared.");
            return this;
        }
    }
    public Env newFunction(String funcName) {
    	Env newEnv = new Env(funcName);
    	newEnv.declFunctions = this.declFunctions;
    	newEnv.newBlock();
    	return newEnv;
    }

    public Env newBlock() {
        contexts.add(new HashMap<String,Type>());
        varContexts.add(new HashMap<String,Object>());
        return this;
    }

    public Env exitBlock() {
        contexts.pollLast();
        varContexts.pollLast();
        return this;
    }
    

	public Flow getFlow() {
		return mFlowOfTheProgram;
	}

	public void setFlow(Flow flowOfTheProgram) {
		this.mFlowOfTheProgram = flowOfTheProgram;
	}
}

