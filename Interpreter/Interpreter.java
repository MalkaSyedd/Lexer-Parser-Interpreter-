package Interpreter;

import AST.*;
import AST.MathOpNode.MathOperations;

import java.lang.reflect.Executable;
import java.util.*;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariables;

public class Interpreter {
    private TranNode top;
    private HashMap<String, InterpreterDataType> variables;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;
        ClassNode classNode = new ClassNode();
        variables = new HashMap<String, InterpreterDataType>();

        ConsoleWrite consoleWrite = new ConsoleWrite();

        consoleWrite.isVariadic = true;
        consoleWrite.isShared = true;
        consoleWrite.name = "write";
        classNode.name = "console";
        classNode.methods.add(consoleWrite);
        top.Classes.add(classNode);

    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {
        ClassNode classNode = new ClassNode();
        for(int i=0; i < top.Classes.size(); i++){
            classNode = top.Classes.get(i);
            for(int j = 0; j < classNode.methods.size(); j++){
                if(classNode.methods.get(j).name.equals("start") && !classNode.methods.get(j).isPrivate &&
                        classNode.methods.get(j).isShared && classNode.methods.get(j).parameters.isEmpty()){

                    interpretMethodCall(Optional.empty(), classNode.methods.get(j), new LinkedList<>());
                    return;
                }
            }
        }
        throw new RuntimeException("No valid 'start' method found in the AST.");
    }

    //Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> result = null;
        List<InterpreterDataType> parameters = getParameters(object, locals, mc);

        String objectName = mc.objectName.get();
        String methodName = mc.methodName;
        if (objectName.isEmpty()) {
            for(int i=0; i < object.get().astNode.methods.size(); i++){
                MethodDeclarationNode methodNode = object.get().astNode.methods.get(i);
                if(doesMatch(methodNode, mc, parameters)){
                    return interpretMethodCall(object, methodNode, parameters);
                }
            }

        } else {
            if(mc.methodName.equals("write")){
                for(int i=0; i < top.Classes.size(); i++){
                    if(top.Classes.get(i).name.equals(objectName)){
                        ClassNode nameOfClass = top.Classes.get(i);
                        for(int t = 0; t < nameOfClass.methods.size(); t++){
                            MethodDeclarationNode methodNode = nameOfClass.methods.get(t);
                            if(doesMatch(methodNode, mc, parameters) && methodNode.isShared){
                                return interpretMethodCall(object, methodNode, parameters);
                            }
                        }
                    }
                }
            }else if(object != null && methodName != null){
                for(int i=0; i < object.get().astNode.methods.size(); i++){
                    if(object.get().astNode.methods.get(i).name.equals(methodName)){
                        MethodDeclarationNode methodNode = object.get().astNode.methods.get(i);
                        if(methodNode.locals.size() != 0){
                            for(int k = 0; k < methodNode.locals.size(); k++){
                                instantiate(methodNode.locals.get(k).type);
                                object.get().members.put(methodNode.locals.get(k).name, instantiate(methodNode.locals.get(k).type));
                            }
                        }
                        if(doesMatch(methodNode, mc, parameters)){
                            return interpretMethodCall(object, methodNode, parameters);
                        }
                    }
                }
            }else {
                ObjectIDT obj = object.orElseGet(null);
                for (int t = 0; t < obj.astNode.methods.size(); t++) {
                    MethodDeclarationNode methodNode = obj.astNode.methods.get(t);
                    if (doesMatch(methodNode, mc, parameters)) {
                        return interpretMethodCall(object, methodNode, parameters);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        var retVal = new LinkedList<InterpreterDataType>();

        if(m.name.equals("start")){
            BuiltInMethodDeclarationNode startMethod = new BuiltInMethodDeclarationNode() {
                @Override
                public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
                    for(AST.VariableDeclarationNode v: m.locals) {
                        variables.put(v.name, instantiate(v.type));
                    }
                    interpretStatementBlock(object, m.statements, variables);

                    return List.of();
                }
            };
            return startMethod.Execute(values);
        }else if(m instanceof ConsoleWrite) {
            return ((ConsoleWrite) m).Execute(values);
        }

        if (m.parameters.size() > values.size()) {
            throw new RuntimeException("Parameter count mismatch for method: " + m.name);
        }

        HashMap<String, InterpreterDataType> returnVariables = new HashMap<>();
        if (m.returns.size() != 0) {
            for(int g = 0; g < m.returns.size(); g++){
                returnVariables.put(m.returns.get(g).name, instantiate(m.returns.get(g).type));
            }
        }
        if (m.returns != null && !m.returns.isEmpty()) {
            for (VariableDeclarationNode returnVarName : m.returns) {
                InterpreterDataType returnValue = returnVariables.get(returnVarName.name);
                if (returnValue != null) {
                    object.get().members.put(returnVarName.name, instantiate(returnVarName.type)); // Add the return value to the list
                } else {
                    throw new RuntimeException("Return variable " + returnVarName + " not found in method: " + m.name);
                }
            }
        }

        interpretStatementBlock(object, m.statements, object.get().members);

        if (m.returns.size() != 0) {
            for(int g = 0; g < m.returns.size(); g++){
                if(object.get().members.containsKey(m.returns.get(g).name)){
                    retVal.add(object.get().members.get(m.returns.get(g).name));
                }
            }
        }

        return retVal;
    }

    // Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        List<InterpreterDataType> parameters = getParameters(callerObj, locals, mc); //populate of IDT
        String className = mc.methodName; // Assuming objectName holds the class name
        Optional<ClassNode> classNodeOpt = getClassByName(className); //find class for constructor

        if (!classNodeOpt.isPresent()) {
            throw new RuntimeException("Class not found: " + className);
        }
        ClassNode classNode = classNodeOpt.get();
        ConstructorNode matchingConstructor = null;
        for (ConstructorNode constructor : classNode.constructors) {
            if (doesConstructorMatch(constructor, mc, parameters)) { //does construcot match
                matchingConstructor = constructor;
                break;
            }
        }
        if (matchingConstructor == null) {
            throw new RuntimeException("No matching constructor found for: " + mc.methodName);
        }
        HashMap<String, InterpreterDataType> localVariables = new HashMap<>();
        for(int i = 0; i < matchingConstructor.parameters.size(); i++){
            InterpreterDataType type = instantiate(matchingConstructor.parameters.get(i).type);
            if(type instanceof StringIDT){
                StringIDT stringIDT = (StringIDT) type;
                stringIDT.Value = mc.parameters.get(i).toString();
                localVariables.put(matchingConstructor.parameters.get(i).name, stringIDT);
            } else if(type instanceof NumberIDT){
                NumberIDT numberIDT = (NumberIDT) type;
                numberIDT.Value = Float.parseFloat(String.valueOf(mc.parameters.get(i)));
                localVariables.put(matchingConstructor.parameters.get(i).name, numberIDT);
            }
        }
        List<InterpreterDataType> parameterValues = new ArrayList<>(localVariables.values());
        parameters.addAll(parameterValues);

        interpretConstructorCall(newOne, matchingConstructor, parameterValues);
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        if (c.parameters.size() != values.size()) { //check number of parameters match
            throw new RuntimeException("Constructor " + c.toString() + " expects " + c.parameters.size()
                    + " parameters, but " + values.size() + " were provided.");
        }

        HashMap<String, InterpreterDataType> constructorLocals = new HashMap<>();

        for (int i = 0; i < c.parameters.size(); i++) {
            String paramName = c.parameters.get(i).name;  // Get the parameter name from the constructor
            InterpreterDataType paramValue = instantiate(c.parameters.get(i).type);// Get the corresponding parameter value
            if(paramValue instanceof StringIDT){
                StringIDT stringIDT = (StringIDT) paramValue;
                stringIDT.Value = values.get(i).toString();
                constructorLocals.put(paramName, stringIDT);
            } else if(paramValue instanceof NumberIDT){
                NumberIDT numberIDT = (NumberIDT) paramValue;
                numberIDT.Value = Float.parseFloat(values.get(i).toString());
                constructorLocals.put(paramName, numberIDT);
            }
        }

        interpretStatementBlock(Optional.ofNullable(object), c.statements, constructorLocals);
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do every statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for(StatementNode s : statements){
            if(s instanceof AssignmentNode) {
                AssignmentNode a = (AssignmentNode) s;
                InterpreterDataType target = findVariable(String.valueOf(a.target.name), locals, object);
                target.Assign(evaluate(locals, object, a.expression));

            }else if(s instanceof MethodCallStatementNode) {
                MethodCallStatementNode m = (MethodCallStatementNode) s;

                Optional<ClassNode> cn = getClassByName(m.objectName.orElse(null));
                if(!cn.isPresent()) {
                    cn = getClassByName(m.methodName);
                }
                ReferenceIDT ref = ((ReferenceIDT) locals.get(m.objectName.orElseGet(null)));
                Optional<ObjectIDT> obj = null;
                if(ref != null){
                    obj = ref.refersTo;
                }else{
                    obj = object;
                }
                List<InterpreterDataType> retVals = new ArrayList<>();

                retVals = findMethodForMethodCallAndRunIt(obj, locals, m);

                for(InterpreterDataType p : retVals){
                    locals.put(p.toString(), p);
                }
            } else if(s instanceof LoopNode) {
                LoopNode loop = (LoopNode) s;
                String value = ((LoopNode) s).expression.toString();
                //FOR ITERATOR
                ExpressionNode name = loop.expression;
                if(name instanceof MethodCallExpressionNode){
                    MethodCallExpressionNode m = (MethodCallExpressionNode) name;
                    if(m.methodName.equals("times")){
                        NumberIDT numberIDT = new NumberIDT(1);
                        HashMap<String, InterpreterDataType> localVariablsIterator = new HashMap<>();
                        for(String local: locals.keySet()){
                            localVariablsIterator.put(local, numberIDT);
                        }
                        NumberIDT interpreterObject = (NumberIDT) evaluate(localVariablsIterator, object, loop.expression);
                        NumberIDT interpretValues = (NumberIDT) locals.get(m.objectName.get());
                        int x = 1;
                        while(interpreterObject.Value < interpretValues.Value){
                            interpreterObject = (NumberIDT) evaluate(localVariablsIterator, object, loop.expression);
                            interpretStatementBlock(object, loop.statements, localVariablsIterator);
                            x = x +1;
                            NumberIDT numberX = new NumberIDT(x);
                            for(String local: locals.keySet()){
                                localVariablsIterator.put(local, numberX);
                            }
                        }
                    }
                }

                if(locals.containsKey(value)){
                    InterpreterDataType valueOfExpression = evaluate(locals, object, loop.expression);
                    if(valueOfExpression instanceof BooleanIDT){
                        value = ((BooleanIDT) valueOfExpression).toString();
                        while(value.equals("true")){
                            interpretStatementBlock(object, loop.statements, locals);
                            valueOfExpression = evaluate(locals, object, loop.expression);
                            value = ((BooleanIDT) valueOfExpression).toString();
                        }
                    }
                }

            } else if(s instanceof IfNode) {
                IfNode ifNode = (IfNode) s;
                BooleanIDT booleantIDT = (BooleanIDT) evaluate(locals, object, ifNode.condition);
                if(booleantIDT.Value){
                    interpretStatementBlock(object, ifNode.statements, locals);
                } else if(!booleantIDT.Value && ifNode.elseStatement.isPresent()){
                    Optional<ElseNode> elseNodeStatements = ifNode.elseStatement;
                    interpretStatementBlock(object, elseNodeStatements.get().statements, locals);
                }
            }
        }

    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        if(expression instanceof NumericLiteralNode) {
            NumericLiteralNode n = (NumericLiteralNode) expression;
            return new NumberIDT(n.value);
        } else if (expression instanceof StringLiteralNode) {
            StringLiteralNode s = (StringLiteralNode) expression;
            return new StringIDT(s.value);
        } else if (expression instanceof VariableReferenceNode) {
            VariableReferenceNode v = (VariableReferenceNode) expression;
            return locals.get(v.name);
        } else if(expression instanceof BooleanOpNode){
            BooleanOpNode m = (BooleanOpNode) expression;
            BooleanIDT left= (BooleanIDT) evaluate(locals, object, m.left);
            if(m.right != null){
                BooleanIDT right= (BooleanIDT) evaluate(locals, object, m.right);
                if(m.op == BooleanOpNode.BooleanOperations.and){
                    return new BooleanIDT(left.Value && right.Value);
                } else if(m.op == BooleanOpNode.BooleanOperations.or){
                    return new BooleanIDT(left.Value || right.Value);
                }
            } else{
                return new BooleanIDT(left.Value);
            }
        }else if(expression instanceof CompareNode){
            CompareNode c = (CompareNode) expression;
            InterpreterDataType typeUnknownLeft = evaluate(locals, object, c.left);
            InterpreterDataType typeUnknownRight = evaluate(locals, object, c.right);
            if(typeUnknownLeft instanceof NumberIDT && typeUnknownRight instanceof NumberIDT){
                NumberIDT numberLeft = (NumberIDT) typeUnknownLeft;
                NumberIDT numberRight = (NumberIDT) typeUnknownRight;
                if(c.op == CompareNode.CompareOperations.eq){
                    return new BooleanIDT(numberLeft.Value == numberRight.Value);
                } else if(c.op == CompareNode.CompareOperations.ne){
                    return new BooleanIDT(numberLeft.Value != numberRight.Value);
                } else if(c.op == CompareNode.CompareOperations.lt){
                    return new BooleanIDT(numberLeft.Value < numberRight.Value);
                } else if(c.op == CompareNode.CompareOperations.le){
                    return new BooleanIDT(numberLeft.Value <= numberRight.Value);
                } else if(c.op == CompareNode.CompareOperations.gt){
                    return new BooleanIDT(numberLeft.Value > numberRight.Value);
                }else if(c.op == CompareNode.CompareOperations.ge){
                    return new BooleanIDT(numberLeft.Value >= numberRight.Value);
                }
            }
        }else if (expression instanceof MathOpNode) {
            MathOpNode m = (MathOpNode) expression;
            NumberIDT l = (NumberIDT) evaluate(locals, object, m.left);
            NumberIDT r = (NumberIDT) evaluate(locals, object, m.right);
            if (m.op == MathOperations.add) {
                return new NumberIDT(l.Value + r.Value);
            }else if (m.op == MathOperations.subtract) {
                return new NumberIDT(l.Value - r.Value);
            }else if (m.op == MathOperations.multiply) {
                return new NumberIDT(l.Value * r.Value);
            }else if (m.op == MathOperations.divide) {
                return new NumberIDT(l.Value / r.Value);
            }else if (m.op == MathOperations.modulo) {
                return new NumberIDT(l.Value % r.Value);
            }
        } else if(expression instanceof NewNode){
            MethodCallStatementNode methodCallStatementNode = new MethodCallStatementNode();
            methodCallStatementNode.methodName = ((NewNode) expression).className;
            methodCallStatementNode.parameters = ((NewNode) expression).parameters;
            Optional<ClassNode> cn = getClassByName(methodCallStatementNode.methodName);
            ObjectIDT obj = new ObjectIDT(cn.orElseGet(null));
            for(int i = 0; i < cn.get().members.size(); i++){
                obj.members.put(cn.get().members.get(i).declaration.name.toString(), instantiate(cn.get().members.get(i).declaration.type.toString()));
            }

            findConstructorAndRunIt(object, locals, methodCallStatementNode, obj);
            return obj;
        } else if(expression instanceof BooleanLiteralNode){
            BooleanLiteralNode b = (BooleanLiteralNode) expression;
            b.value = ((BooleanLiteralNode) expression).value;
            return new BooleanIDT(b.value);
        } else if (expression instanceof MethodCallExpressionNode){
            MethodCallExpressionNode m = (MethodCallExpressionNode) expression;
            if(m.methodName.equals("times")){//means this is an iterator
                NumberIDT interpretVal = (NumberIDT) locals.get(m.objectName.get());

                for(int i = 0; i < top.Classes.size(); i++){
                    if(top.Classes.get(i).name.equals("console")){
                        MethodDeclarationNode md = top.Classes.get(i).methods.get(0);
                        ArrayList<InterpreterDataType> retVals = new ArrayList<InterpreterDataType>();
                        for(int retVal = 0; retVal < md.returns.size(); retVal++){
                            retVals.add(instantiate(md.returns.get(retVal).type.toString()));
                        }
                    }
                }
                return new NumberIDT(interpretVal.Value);
            }



            for(int i = 0; i < object.orElseGet(null).astNode.methods.size(); i++){
                if(object.orElseGet(null).astNode.methods.get(i).name.equals(m.methodName)){
                    MethodDeclarationNode md = object.orElseGet(null).astNode.methods.get(i);
                    ArrayList<InterpreterDataType> retVals = new ArrayList<InterpreterDataType>();
                    for(int retVal = 0; retVal < md.returns.size(); retVal++){
                        retVals.add(instantiate(md.returns.get(retVal).type.toString()));
                    }
                    InterpreterDataType typeFromMethodCall = interpretMethodCall(object, md, retVals).get(0);
                    return typeFromMethodCall;
                }
            }
            return null;
        }
        throw new IllegalArgumentException();
    }


    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this method call?
     * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if (!m.name.equals(mc.methodName)) {
            return false;
        }

        if(m instanceof BuiltInMethodDeclarationNode && ((BuiltInMethodDeclarationNode) m).isVariadic) {
            return true;
        }

        if(parameters.size() < m.parameters.size()) {
            return false;
        }

        for(int i = 0; i < m.parameters.size(); i++) {
            InterpreterDataType declaredParamType = instantiate(m.parameters.get(i).type);
            if(parameters.get(i).getClass() != declaredParamType.getClass()) {
                return false;
            }
        }
        if(parameters.size() > m.parameters.size()) {
            return false;
        }
        return true;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if(mc.parameters.size() != c.parameters.size()){
            return false;
        }

        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> parameters = new ArrayList<>();
        for (ExpressionNode paramExpr : mc.parameters) {
            InterpreterDataType paramValue = evaluate(locals, object, paramExpr);
            parameters.add(paramValue);
        }
        return parameters;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {
        if (idt instanceof BooleanIDT) {
            return "boolean".equals(type);
        } else if (idt instanceof CharIDT) {
            return "char".equals(type);
        } else if (idt instanceof NumberIDT) {
            return "number".equals(type);
        } else if (idt instanceof StringIDT) {
            return "string".equals(type);
        }

        if (idt instanceof ObjectIDT) {
            ObjectIDT objectIDT = (ObjectIDT) idt;
            String className = objectIDT.getClass().getName();  // Retrieve class name
            if (className.equals(type)) {
                return true;  // Class name matches the type
            }

            if (objectIDT.getClass().getInterfaces().equals(type)) {
                return true;
            }
        }

        if (idt instanceof ReferenceIDT) {
            ReferenceIDT referenceIDT = (ReferenceIDT) idt;
            return typeMatchToIDT(type, referenceIDT.refersTo.get());  // Recursively check inner type
        }

        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        ClassNode classNode = object.astNode;  // Assuming getClassNode() returns the class for the object

        List<MethodDeclarationNode> methods = classNode.methods;

        for (MethodDeclarationNode method : methods) {
            if (method.name.equals(mc.methodName)) {
                if (doesMatch(method, mc, parameters)) {
                    return method;
                }
            }
        }
        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        List<ClassNode> classes = top.Classes;// Get the list of classes from the TranNode

        for (int i = 0; i < classes.size(); i++) {
            ClassNode classNode = classes.get(i);
            if (classNode.name.equals(name)) {
                return Optional.of(classNode);
            } else{
                for(int j = 0; j < classNode.methods.size(); j++) {
                    if(classNode.methods.get(j).name.equals(name)) {
                        return Optional.of(classNode);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        if (locals.containsKey(name)) {
            return locals.get(name);
        }

        if (object.isPresent()) {
            ObjectIDT currentObject = object.get();

            List<String> hashMapKeyValues = new ArrayList<>();
            hashMapKeyValues.addAll(object.get().members.keySet());


            for(int i = 0; i < currentObject.members.size(); i++) { //SOMETHING IS NOT WORKING HERE UNABLE TO FIND VARIABLES DECLARED IN CLASS
                if(hashMapKeyValues.contains(name)){
                    InterpreterDataType memberValue = (InterpreterDataType) currentObject.members.get(name);
                    if (memberValue != null) {
                        return memberValue;
                    }
                }
            }

        }
        throw new RuntimeException("Unable to find variable " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        switch (type.toLowerCase()) {
            case "string":
                return new StringIDT("");
            case "number":
                return new NumberIDT(0);
            case "boolean":
                return new BooleanIDT(false);
            case "character":
                return new CharIDT(' ');
            default:
                return new ReferenceIDT();
        }
    }
}
