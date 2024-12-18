import AST.*;


import javax.swing.text.html.Option;
import java.beans.Expression;
import java.beans.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//Grammar, dealing with class or interface,
public class Parser {
    private TokenManager tokenManager;
    private TranNode tranNode;

    public Parser(TranNode tranNode, List<Token> tokens) {
        this.tranNode = tranNode;
        this.tokenManager = new TokenManager(tokens);
    }

    public void RequireNewLine() throws SyntaxErrorException {
        boolean newLineSeen = false;
        while(tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
            newLineSeen = true;
        }
        if(!newLineSeen){
            throw new SyntaxErrorException("new line needed", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

    //{} means 1 or more
//[] means 0 or 1
    public void Tran() throws SyntaxErrorException{
        while(!tokenManager.done()){
            if(tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE).isPresent()){
                InterfaceNode interfaceName = Interface();
                if(interfaceName != null){
                    tranNode.Interfaces.add(interfaceName);
                }
            }else if(tokenManager.matchAndRemove(Token.TokenTypes.CLASS).isPresent()){
                ClassNode className = Class();
                if(className != null){
                    tranNode.Classes.add(className);
                }
            }else if(tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){

            }else if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()); //ADDED
        }
    }

    private InterfaceNode Interface() throws SyntaxErrorException {
        InterfaceNode interfaceNode = new InterfaceNode(); //create interfaceNode

        Optional<Token> interfaceName = tokenManager.matchAndRemove(Token.TokenTypes.WORD); //get interface name
        if(interfaceName.isEmpty()){
            throw new SyntaxErrorException("interface must be named", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        RequireNewLine(); //require new line

        if(!tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){ //make sure indent is present
            throw new SyntaxErrorException("must declare methods with indent", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        interfaceNode.name = interfaceName.get().getValue(); //assign name to interface

        interfaceNode.methods.add(MethodHeader()); //must have atleast one method header

        while(!tokenManager.done()){
            if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                return interfaceNode;
            } else{
                while(tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){//loops if new lines
                    if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                        return interfaceNode;
                    } else{
                        interfaceNode.methods.add(MethodHeader());
                    }
                }
            }
        }
        return interfaceNode;
    }

    private MethodHeaderNode MethodHeader() throws SyntaxErrorException {
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();

        Optional<Token> methodHeaderName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(methodHeaderName.isEmpty()){
            throw new SyntaxErrorException("need method header name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        methodHeaderNode.name = methodHeaderName.get().getValue();

        if(!tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()){ //search for LPAREN
            throw new SyntaxErrorException("need left parenthesis after name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        Optional<Token> searchRparen = tokenManager.matchAndRemove(Token.TokenTypes.RPAREN); //search for RPAREN

        while(searchRparen.isEmpty()){ //if no RPAREN FOUND
            methodHeaderNode.parameters.add(VariableDeclaration()); //MUST HAVE ATLEAST ONE VARIABLE DECLARATION
            Optional<Token> searchComma = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
            if(searchComma.isEmpty()){
                searchRparen = tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
                if(searchRparen.isEmpty()){
                    throw new SyntaxErrorException("missing RPAREN", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }
        }

        Optional<Token> searchColon = tokenManager.matchAndRemove(Token.TokenTypes.COLON);
        if(searchColon.isEmpty()){
            return methodHeaderNode;
        } else{
            methodHeaderNode.returns.add(VariableDeclaration());
            Optional<Token> searchCommanReturns = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);

            while(!searchCommanReturns.isEmpty()){
                methodHeaderNode.returns.add(VariableDeclaration());
                searchCommanReturns = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
            }
        }
        return methodHeaderNode;
    }

    private VariableDeclarationNode VariableDeclaration() throws SyntaxErrorException {
        VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();  //instance of variable declaration

        Optional<Token> methodVariableDeclarationType = tokenManager.matchAndRemove(Token.TokenTypes.WORD); //find word
        if(methodVariableDeclarationType.isEmpty()){ //if word not seen throw exception
            //return null;
            throw new SyntaxErrorException("declare variable type or put RPAREN", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        variableDeclarationNode.type = methodVariableDeclarationType.get().getValue(); //word found, set it to variavle type

        Optional<Token> methodVariableDeclarationName = tokenManager.matchAndRemove(Token.TokenTypes.WORD); //search for second word
        if(methodVariableDeclarationName.isEmpty()){ //if second word not there throw error
            throw new SyntaxErrorException("please declare variable name (word)", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        variableDeclarationNode.name = methodVariableDeclarationName.get().getValue(); //second word found, set it to variable name
        return variableDeclarationNode;
    }

    private ClassNode Class() throws SyntaxErrorException {
        ClassNode classNode = new ClassNode();
        Optional<Token> className = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (className.isEmpty()) {
            throw new SyntaxErrorException("Class must be named", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        classNode.name = className.get().getValue();

        if(tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()){
            Optional<Token> interfaceImplented = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if(!interfaceImplented.isPresent()){
                throw new SyntaxErrorException("interface being implemented must be named", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            } else{
                classNode.interfaces.add(interfaceImplented.get().getValue());
            }
            Optional<Token> searchComma = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);

            while(!searchComma.isEmpty()){
                Optional<Token> interfaceImplented2 = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                classNode.interfaces.add(interfaceImplented2.get().getValue());
                searchComma = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
            }
        }
        RequireNewLine();
        if(!tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
            throw new SyntaxErrorException("indent needed", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // Parse constructors, methods, and members
        while (true) {
            if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isPresent()) {
                classNode.constructors.add(Constructor());
                Optional<Token> maybeNewLine = tokenManager.peek(0); //PART ADDED BROOOOOOOOO
                if(maybeNewLine.isPresent() && maybeNewLine.get().getType() == Token.TokenTypes.NEWLINE){
                    RequireNewLine();
                }
            } else if(tokenManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent()){ //DELETE if WRONG
                MethodDeclarationNode methodDeclarationNode = MethodDeclaration();
                methodDeclarationNode.isShared = true;
                classNode.methods.add(methodDeclarationNode);
                if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                    if(tokenManager.done()){ //THIS PART WAS ADDED
                        break;
                    }
                }else{
                    RequireNewLine();
                }
            }else if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
                classNode.methods.add(MethodDeclaration());
                if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                    Optional<Token> maybeNewLine = tokenManager.peek(0); //PART ADDED BROOOOOOOOO
                    if(maybeNewLine.isPresent() && maybeNewLine.get().getType() == Token.TokenTypes.NEWLINE){
                        RequireNewLine();
                    }
                    if(tokenManager.done()){ //THIS PART WAS ADDED
                        break;
                    }
                }else{
                    RequireNewLine();
                }
            } else if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){
                classNode.members.add(Member());
                if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
                    Optional<Token> maybeNewLine = tokenManager.peek(0); //PART ADDED BROOOOOOOOO
                    if(maybeNewLine.isPresent() && maybeNewLine.get().getType() == Token.TokenTypes.NEWLINE){
                        RequireNewLine();
                    }
                    break;
                }else{
                    RequireNewLine();
                }
            } else{
                break;
            }
        }
        return classNode;
    }

    private ConstructorNode Constructor() throws SyntaxErrorException {

        if (!tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            throw new SyntaxErrorException("Expected '(' after constructor name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        ConstructorNode constructorNode = new ConstructorNode();

        while(!tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()){
            constructorNode.parameters.add(VariableDeclaration());
            Optional<Token> searchCommanReturns = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);

            while(!searchCommanReturns.isEmpty()){
                constructorNode.parameters.add(VariableDeclaration());
                searchCommanReturns = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
            }
        }

        RequireNewLine();

        if (!tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            throw new SyntaxErrorException("Expected indent for constructor body", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        while(!tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()){
            constructorNode.statements.add(Statement());
        }

        return constructorNode; // Return the populated constructor node
    }

    private MethodDeclarationNode MethodDeclaration() throws SyntaxErrorException {
        MethodDeclarationNode methodNode = new MethodDeclarationNode();

        if (tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent()) {
            methodNode.isPrivate = true;
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent()) {
            methodNode.isShared = true;
        }

        MethodHeaderNode methodHeader = MethodHeader();
        if (methodHeader == null) {
            throw new SyntaxErrorException("Expected method header", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        methodNode.name = methodHeader.name;
        methodNode.returns = methodHeader.returns;
        RequireNewLine();

        if(tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
            boolean searchDedent = false;

            while(!searchDedent){
                if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
                    Optional<Token> searchNewline;
                    do {
                        VariableDeclarationNode variableDeclaration = VariableDeclaration();
                        methodNode.locals.add(variableDeclaration);
                        RequireNewLine();
                        //searchNewline = tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
                    } while (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD));// && searchNewline.isPresent());
                }

                while(true){
                    if(tokenManager.peek(0).get().getType() == Token.TokenTypes.DEDENT){ //CHECK IF DEDENT IS PRESENT
                        searchDedent = true;
                        break;
                    }
                    Optional<StatementNode> statement = Optional.ofNullable(Statement());
                    if(statement.isPresent()){
                        methodNode.statements.add(statement.get());
                    } else{
                        break;
                    }
                }
            }
        }
        return methodNode;
    }

    private MemberNode Member() throws SyntaxErrorException {
        MemberNode memberNode = new MemberNode();

        VariableDeclarationNode variableDeclaration = VariableDeclaration();
        if (variableDeclaration == null) {
            throw new SyntaxErrorException("Expected variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        memberNode.declaration = variableDeclaration;


        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
            if (tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()) {
                if (!tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
                    throw new SyntaxErrorException("Expected ':' after 'accessor'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }

            if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                if (!tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
                    throw new SyntaxErrorException("Expected ':' after 'mutator'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }
        }
        return memberNode;
    }

    private StatementNode Statement() throws SyntaxErrorException {
        if(tokenManager.matchAndRemove(Token.TokenTypes.IF).isPresent()){
            return If();
        } else if(tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isPresent()){
            return Loop();
        } else{
            Optional<StatementNode> result = disambiguate();
            if (result.isPresent()) {
                if(tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
                    RequireNewLine();
                }
                return result.get();
            } else {
                if(tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isPresent()){
                    return Loop();
                }
                return null;
            }
        }
    }

    private MethodCallStatementNode MethodCall() throws SyntaxErrorException {
        MethodCallStatementNode methodCallNode = new MethodCallStatementNode();
        methodCallNode.returnValues.add(VariableReference());

        Optional<Token> searchComma = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
        while(searchComma.isPresent()){
            methodCallNode.returnValues.add(VariableReference());
            searchComma = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
        }
        if(!tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()){
            throw new SyntaxErrorException("Expected assignment", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        methodCallNode.methodName = String.valueOf(MethodCallExpression().get().methodName);
        return methodCallNode;
    }

    private AssignmentNode Assignment() throws SyntaxErrorException {
        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.expression = Expression();
        return assignmentNode;
    }

    private Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        Optional<MethodCallExpressionNode> methodCallExpressionNode = MethodCallExpression();
        if(methodCallExpressionNode.isPresent()){ //IDK HOW TO SET
            MethodCallStatementNode methodCallStatement = new MethodCallStatementNode();
            methodCallStatement.objectName = methodCallExpressionNode.get().objectName;
            methodCallStatement.parameters = methodCallExpressionNode.get().parameters;
            methodCallStatement.methodName = methodCallExpressionNode.get().methodName;
            return Optional.of(methodCallStatement);
        }

        Optional<VariableReferenceNode> variableReferenceNode = Optional.of(VariableReference());
        if(!variableReferenceNode.isPresent()){
            return Optional.empty();
        }

        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.ASSIGN){
            if(tokenManager.peek(1).get().getType() == Token.TokenTypes.WORD) {
                if(tokenManager.peek(2).get().getType() == Token.TokenTypes.LPAREN){
                    tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
                    methodCallExpressionNode = MethodCallExpression();
                    if(methodCallExpressionNode.isPresent()){
                        MethodCallStatementNode methodCallStatement = new MethodCallStatementNode();
                        methodCallStatement.methodName = methodCallExpressionNode.get().methodName;
                        if(variableReferenceNode.isPresent()){
                            methodCallStatement.returnValues.add(variableReferenceNode.get());
                        }
                        return Optional.of(methodCallStatement);
                    }
                }
            }
        }



        if (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            MethodCallStatementNode methodCallStatement = new MethodCallStatementNode();
            methodCallStatement = MethodCall();
            methodCallStatement.returnValues.add(0, variableReferenceNode.get());
            return Optional.of(methodCallStatement);
        } else {
            if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
                if(tokenManager.peek(0).get().getType() == Token.TokenTypes.LOOP){
                    return Optional.empty();
                }
                AssignmentNode assignmentNode = Assignment();
                assignmentNode.target = variableReferenceNode.get();
                return Optional.of(assignmentNode);
            } else {
                Optional<StatementNode> methodCallResult = Optional.ofNullable(MethodCall());
                if (methodCallResult.isPresent()) {
                    return methodCallResult;
                }
            }
        }
        return Optional.empty();
    }

    //{} means 1 or more
    //[] means 0 or 1
    private BooleanOpNode BoolExpTerm() throws SyntaxErrorException {
        BooleanOpNode booleanOpNode = new BooleanOpNode();
        if(tokenManager.matchAndRemove(Token.TokenTypes.NOT).isPresent()){
            BoolExpTerm();
        }

        booleanOpNode.left = BoolExpFactor();

        if(tokenManager.matchAndRemove(Token.TokenTypes.AND).isPresent()){
            BooleanOpNode andNode = new BooleanOpNode();
            andNode.left = booleanOpNode.left;
            andNode.op = BooleanOpNode.BooleanOperations.and;
            andNode.right = BoolExpFactor();
            booleanOpNode.left = andNode;
        }


        while(true){
            Optional<Token> searchAnd = tokenManager.matchAndRemove(Token.TokenTypes.AND);
            Optional<Token> searchOr = tokenManager.matchAndRemove(Token.TokenTypes.OR);

            if (searchAnd.isPresent()) {
                booleanOpNode.op = BooleanOpNode.BooleanOperations.and;
                booleanOpNode.right = BoolExpTerm();
            } else if (searchOr.isPresent()) {
                booleanOpNode.op = BooleanOpNode.BooleanOperations.or;
                booleanOpNode.right = BoolExpFactor();
            }else {
                break; // No more "and" or "or" found, exit the loop
            }
        }
        return booleanOpNode;
    }

    private ExpressionNode BoolExpFactor() throws SyntaxErrorException {
        CompareNode compareNode = new CompareNode();

        Optional<MethodCallExpressionNode> methodCallExpression = MethodCallExpression();
        if(methodCallExpression.isPresent()){
            return methodCallExpression.get();
        }

        Optional<VariableReferenceNode> variableReference = Optional.of(VariableReference());
        if(variableReference.isPresent()){

            if(tokenManager.matchAndRemove(Token.TokenTypes.EQUAL).isPresent()) {
                compareNode.left = variableReference.get();
                compareNode.op = CompareNode.CompareOperations.eq;
                compareNode.right = Expression();
            } else if(tokenManager.matchAndRemove(Token.TokenTypes.LESSTHAN).isPresent()) {
                compareNode.left = variableReference.get();
                compareNode.op = CompareNode.CompareOperations.lt;
                compareNode.right = Expression();
            } else if(tokenManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL).isPresent()) {
                compareNode.left = variableReference.get();
                compareNode.op = CompareNode.CompareOperations.le;
                compareNode.right = Expression();
            } else if(tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHAN).isPresent()) {
                compareNode.left = variableReference.get();
                compareNode.op = CompareNode.CompareOperations.gt;
                compareNode.right = Expression();
            }else if(tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL).isPresent()) {
                compareNode.left = variableReference.get();
                compareNode.op = CompareNode.CompareOperations.ge;
                compareNode.right = Expression();
            }else if(tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL).isPresent()) {
                compareNode.left = variableReference.get();
                compareNode.op = CompareNode.CompareOperations.ne;
                compareNode.right = Expression();
            } else{
                return variableReference.get();
            }
        }

        return compareNode;
    }


    private VariableReferenceNode VariableReference() throws SyntaxErrorException {
        VariableReferenceNode variableReferenceNode = new VariableReferenceNode();
        Optional<Token> variableName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        Optional<Token> variableName2 = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
        Optional<Token> variableName3 = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);
        Optional<Token> variableName4 = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);
        if(!variableName.isPresent() && !variableName2.isPresent() && !variableName3.isPresent() && !variableName4.isPresent()) {
            throw new SyntaxErrorException("Expected variable reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        if(variableName.isPresent()){
            variableReferenceNode.name = variableName.get().getValue();
        } else if(variableName2.isPresent()){
            variableReferenceNode.name = variableName2.get().getValue();
        } else if(variableName3.isPresent()){
            //CharLiteralNode charLiteralNode = new CharLiteralNode();
            //charLiteralNode.value = variableName3.get().getValue().charAt(0);
            variableReferenceNode.name = variableName3.get().getValue();
            //return charLiteralNode;
        } else if(variableName4.isPresent()){
            //StringLiteralNode stringLiteralNode = new StringLiteralNode();
            //stringLiteralNode.value = variableName4.get().getValue();
            //return stringLiteralNode;
            variableReferenceNode.name = variableName4.get().getValue();
        }
        return variableReferenceNode;
    }



    private ExpressionNode Expression() throws SyntaxErrorException {
        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.expression = Term();

        Optional<Token> searchPlus = tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
        Optional<Token> searchMinus = tokenManager.matchAndRemove(Token.TokenTypes.MINUS);

        while(searchPlus.isPresent() || searchMinus.isPresent()){ //finish implementing this
            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = assignmentNode.expression;
            if(searchPlus.isPresent()){
                mathOpNode.op = MathOpNode.MathOperations.add;
            } else if(searchMinus.isPresent()){
                mathOpNode.op = MathOpNode.MathOperations.subtract;
            }

            mathOpNode.right = Term();

            assignmentNode.expression = mathOpNode;
            searchPlus = tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
            searchMinus = tokenManager.matchAndRemove(Token.TokenTypes.MINUS);
        }
        return assignmentNode.expression;
    }


    private ExpressionNode Term() throws SyntaxErrorException {
        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.expression = Factor();
        Optional<Token> searchTimes = tokenManager.matchAndRemove(Token.TokenTypes.TIMES);
        Optional<Token> searchModulo = tokenManager.matchAndRemove(Token.TokenTypes.MODULO);
        Optional<Token> searchDivide = tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE);
        while(searchDivide.isPresent() || searchModulo.isPresent() || searchTimes.isPresent()){
            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = assignmentNode.expression;
            if(searchTimes.isPresent()){
                mathOpNode.op = MathOpNode.MathOperations.multiply;
            }else if(searchModulo.isPresent()){
                mathOpNode.op = MathOpNode.MathOperations.modulo;
            }else if(searchDivide.isPresent()){
                mathOpNode.op = MathOpNode.MathOperations.divide;
            }
            mathOpNode.right = Factor();
            assignmentNode.expression = mathOpNode;

            searchTimes = tokenManager.matchAndRemove(Token.TokenTypes.TIMES);
            searchModulo = tokenManager.matchAndRemove(Token.TokenTypes.MODULO);
            searchDivide = tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE);
        }
        return assignmentNode.expression;
    }

    private ExpressionNode Factor() throws SyntaxErrorException {

        Optional<Token> searchTrue= tokenManager.matchAndRemove(Token.TokenTypes.TRUE);
        if(searchTrue.isPresent()){
            BooleanLiteralNode booleanLiteralNode = new BooleanLiteralNode(true);
            booleanLiteralNode.value = Boolean.parseBoolean(searchTrue.get().getValue());
            return booleanLiteralNode;
        }

        Optional<Token> searchFalse= tokenManager.matchAndRemove(Token.TokenTypes.FALSE);
        if(searchFalse.isPresent()){
            BooleanLiteralNode booleanLiteralNode = new BooleanLiteralNode(false);
            booleanLiteralNode.value = Boolean.parseBoolean(searchFalse.get().getValue());
            return booleanLiteralNode;
        }

        Optional<Token> searchNew = tokenManager.matchAndRemove(Token.TokenTypes.NEW); //finish implementing
        if(searchNew.isPresent()){
            NewNode newNode = new NewNode();
            newNode.className = tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            if(!tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()){
                throw new SyntaxErrorException("Expected paren", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            while(!tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()){
                newNode.parameters.add(VariableReference());
                Optional<Token> searchCommanReturns = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                while(!searchCommanReturns.isEmpty()){
                    newNode.parameters.add(VariableReference());
                    searchCommanReturns = tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                }
            }
            return newNode;
        }
        Optional<Token> Lparen = tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        if(Lparen.isPresent()){
           ExpressionNode expressionNode = Expression();
           if(!tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()){
               throw new SyntaxErrorException("need RParen", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
           }
           return expressionNode;
        }
        Optional<Token> searchNumber = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
        if (searchNumber.isPresent()){
            NumericLiteralNode numericLiteralNode = new NumericLiteralNode();
            numericLiteralNode.value = Integer.parseInt(searchNumber.get().getValue());
            return numericLiteralNode;
        }
        Optional<Token> searchQuoted = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);
        if(searchQuoted.isPresent()){
            StringLiteralNode stringLiteralNode = new StringLiteralNode();
            stringLiteralNode.value = searchQuoted.get().getValue();
            return stringLiteralNode;
        }
        Optional<Token> searchQuotedChar = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);
        if(searchQuotedChar.isPresent()){
            CharLiteralNode charLiteralNode = new CharLiteralNode();
            String value = searchQuotedChar.get().getValue(); // Get the string value
            charLiteralNode.value = value.charAt(0);
            return charLiteralNode;
        }

        Optional<MethodCallExpressionNode> methodCallExpression = MethodCallExpression();
        if(methodCallExpression.isPresent()){
            return methodCallExpression.get();
        }

        Optional<VariableReferenceNode> variableReference = Optional.of(VariableReference());
        if(variableReference.isPresent()){
            return variableReference.get();
        }
        return null;
    }


    private Optional<MethodCallExpressionNode> MethodCallExpression() throws SyntaxErrorException {
        MethodCallExpressionNode methodCallExpressionNode = new MethodCallExpressionNode();
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD || tokenManager.peek(0).get().getType() == Token.TokenTypes.NUMBER){
            if(tokenManager.peek(1).get().getType() == Token.TokenTypes.ASSIGN){
                if(tokenManager.peek(2).get().getType() == Token.TokenTypes.WORD && (tokenManager.peek(3).get().getType() != Token.TokenTypes.DOT)
                || tokenManager.peek(3).get().getType() != Token.TokenTypes.LPAREN){
                    return Optional.empty();
                }
            }else if(tokenManager.peek(1).get().getType() != Token.TokenTypes.DOT){
                methodCallExpressionNode.objectName = Optional.empty();
                if(tokenManager.peek(1).get().getType() != Token.TokenTypes.LPAREN){
                    return Optional.empty();
                }
            }
        }

        Optional<Token> variableName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        Optional<Token> variableName2 = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
        if(!variableName.isPresent() && !variableName2.isPresent()) {
            throw new SyntaxErrorException("Expected variable", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        } else if(variableName.isPresent()){
            methodCallExpressionNode.methodName = variableName.get().getValue();
        } else if(variableName2.isPresent()){
            methodCallExpressionNode.methodName = variableName2.get().getValue();
        }

        if(tokenManager.matchAndRemove(Token.TokenTypes.DOT).isPresent()){
            methodCallExpressionNode.objectName = methodCallExpressionNode.methodName.describeConstable();
            Optional<Token> identifier = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            Optional<Token> identifier2 = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
            if(!identifier.isPresent() && !identifier2.isPresent()) {
                throw new SyntaxErrorException("Expected identifier after dot", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            } else if(identifier.isPresent()){
                methodCallExpressionNode.methodName = identifier.get().getValue();
            } else if(identifier2.isPresent()){
                methodCallExpressionNode.methodName = identifier2.get().getValue();
            }
        }


        if(!tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()){
            return Optional.of(methodCallExpressionNode);
        } else{

            while(!tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()){
                methodCallExpressionNode.parameters.add(Expression());
                Optional<Token> maybeComma = tokenManager.peek(0); //PART ADDED BROOOOOOOOO
                if(maybeComma.isPresent() && maybeComma.get().getType() == Token.TokenTypes.COMMA){
                    tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
                }
            }
        }

        return Optional.of(methodCallExpressionNode);
    }



    private IfNode If() throws SyntaxErrorException {
        IfNode ifNode = new IfNode();
        ifNode.statements = new ArrayList<>();

        ifNode.condition = BoolExpTerm();
        RequireNewLine();
        if(!tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
            throw new SyntaxErrorException("need indent for statements", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        ifNode.statements.add(Statement()); //atleast one statement
        Optional<Token> searchDedent = tokenManager.matchAndRemove(Token.TokenTypes.DEDENT); //can have multiple statements
        while(!searchDedent.isPresent()){
            ifNode.statements.add(Statement());
            searchDedent = tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        }


        Optional<Token> searchElse = tokenManager.matchAndRemove(Token.TokenTypes.ELSE);
        if(searchElse.isPresent()) {
            RequireNewLine();
            if(!tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
                throw new SyntaxErrorException("need indent for else statements", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            ElseNode elseNode = new ElseNode();
            elseNode.statements = new ArrayList<>();
            elseNode.statements.add(Statement());
            Optional<Token> searchDedent2 = tokenManager.matchAndRemove(Token.TokenTypes.DEDENT); //can have multiple statements
            while(!searchDedent2.isPresent()){
                elseNode.statements.add(Statement());
                searchDedent2 = tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
            }
            ifNode.elseStatement = Optional.of(elseNode);

        } else{
            ifNode.elseStatement = Optional.empty();
        }
        return ifNode;
    }


    private LoopNode Loop() throws SyntaxErrorException {
        LoopNode loopNode = new LoopNode();
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD || tokenManager.peek(0).get().getType() == Token.TokenTypes.NUMBER){
            if(tokenManager.peek(1).get().getType() == Token.TokenTypes.ASSIGN){
                loopNode.assignment = Optional.of(VariableReference());
                tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
            } else{
                loopNode.assignment = Optional.empty();
            }
        }

        Optional<MethodCallExpressionNode> methodCallExpression = MethodCallExpression();
        if(methodCallExpression.isPresent()){
            loopNode.expression = methodCallExpression.get();
        } else{
            BooleanOpNode booleanOpNode =  BoolExpTerm();
            if(booleanOpNode.op == null && booleanOpNode.right == null){
                CompareNode compareNode = new CompareNode();
                compareNode.left = booleanOpNode.left;
                loopNode.expression = compareNode.left;
                //loopNode.expression = compareNode;
            }
        }


        if(tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()){
            Optional<MethodCallExpressionNode> methodCallExpressionNode = MethodCallExpression();
            if(methodCallExpressionNode.isPresent()){
                loopNode.expression = methodCallExpressionNode.get();
            } else{
                loopNode.expression = BoolExpTerm();
            }
        }

        RequireNewLine();
        if(!tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()){
            throw new SyntaxErrorException("need indent for loop statements", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        loopNode.statements.add(Statement());
        Optional<Token> searchDedent = tokenManager.matchAndRemove(Token.TokenTypes.DEDENT); //can have multiple statements
        while(!searchDedent.isPresent()){
            loopNode.statements.add(Statement());
            searchDedent = tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        }

        return loopNode;
    }
}