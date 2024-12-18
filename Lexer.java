import java.util.*;


public class Lexer {
    private TextManager textManager;
    public HashMap<String, Token.TokenTypes> Hashmap = new HashMap<>();
    int lineNumber = 1;
    int characterPosition = 0;
    int currentIndentLevel = 0;


    public Lexer(String textEntered) {
        textManager = new TextManager(textEntered);
        Hashmap.put("accessor:", Token.TokenTypes.ACCESSOR);
        Hashmap.put("class", Token.TokenTypes.CLASS);
        Hashmap.put("mutator:", Token.TokenTypes.MUTATOR);
        Hashmap.put("implements", Token.TokenTypes.IMPLEMENTS);
        Hashmap.put("interface", Token.TokenTypes.INTERFACE);
        Hashmap.put("loop", Token.TokenTypes.LOOP);
        Hashmap.put("if", Token.TokenTypes.IF);
        Hashmap.put("shared", Token.TokenTypes.SHARED);
        Hashmap.put("construct", Token.TokenTypes.CONSTRUCT);
        Hashmap.put("new", Token.TokenTypes.NEW);
        Hashmap.put("private", Token.TokenTypes.PRIVATE);
        Hashmap.put("true", Token.TokenTypes.TRUE);
        Hashmap.put("false", Token.TokenTypes.FALSE);
        Hashmap.put("&&", Token.TokenTypes.AND);
        Hashmap.put("||", Token.TokenTypes.OR);
        Hashmap.put("!", Token.TokenTypes.NOT);
        Hashmap.put("not", Token.TokenTypes.NOT);
        Hashmap.put("and", Token.TokenTypes.AND);
        Hashmap.put("or", Token.TokenTypes.OR);
        Hashmap.put("else", Token.TokenTypes.ELSE);
        Hashmap.put(".", Token.TokenTypes.DOT);
        Hashmap.put(":", Token.TokenTypes.COLON);
        Hashmap.put(",", Token.TokenTypes.COMMA);
        Hashmap.put("(", Token.TokenTypes.LPAREN);
        Hashmap.put(")", Token.TokenTypes.RPAREN);
        Hashmap.put("+", Token.TokenTypes.PLUS);
        Hashmap.put("-", Token.TokenTypes.MINUS);
        Hashmap.put("*", Token.TokenTypes.TIMES);
        Hashmap.put("/", Token.TokenTypes.DIVIDE);
        Hashmap.put("!=", Token.TokenTypes.NOTEQUAL);
        Hashmap.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        Hashmap.put(">", Token.TokenTypes.GREATERTHAN);
        Hashmap.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        Hashmap.put("<", Token.TokenTypes.LESSTHAN);
        Hashmap.put("=", Token.TokenTypes.ASSIGN);
        Hashmap.put("==", Token.TokenTypes.EQUAL);
        Hashmap.put("%", Token.TokenTypes.MODULO);
    }

    public List<Token> Lex() throws Exception {
        var retVal = new LinkedList<Token>();


        while(!textManager.isAtEnd()){
            char c = textManager.peekCharacter();
            if (Character.isLetter(c) || c == '\n') {
                retVal.addAll(parseWord());
            } else if (Character.isDigit(c)) {
                retVal.addAll(parseNumber());
            }
            else if (c == '.') {
                if(Character.isLetter(textManager.peekCharacter())){
                    retVal.addAll(parsePunctuation());
                } else{
                    retVal.addAll(parseNumber());
                }
            } else if(c == ' ' || c == '\r') {
                textManager.getCharacter();
                characterPosition++; //ADDED
            } else if(c == '\"'){
                textManager.getCharacter();
                characterPosition++;
                retVal.addAll(parseQuoted());
            } else if(c == '\''){
                textManager.getCharacter();
                characterPosition++;
                retVal.addAll(parseQuotedChar());
            }
            else {
                retVal.addAll(parsePunctuation());
            }
        }
        if(currentIndentLevel != 0){
            while(currentIndentLevel > 0){
                retVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition ));
                currentIndentLevel--;
            }
        }
        return retVal;
    }
    public List<Token> parseQuotedChar() throws Exception{
        String currentWord = "";
        var retVal = new LinkedList<Token>();
        int slashCount = 0;
        slashCount++;


        while(!textManager.isAtEnd()){
            char c = textManager.peekCharacter();
            if(c != '\'' && c != ' ' || c =='\n'){
                currentWord += textManager.getCharacter();
                characterPosition++;
                if(c == '\n'){
                    lineNumber++;
                    characterPosition = 0;
                }
            } else if(Character.isWhitespace(c)){
                currentWord += textManager.getCharacter();
                characterPosition++;
            } else if(c == '\''){
                slashCount--;
                c = textManager.getCharacter();
                characterPosition++;
                if(currentWord.length() == 1){
                    retVal.add(new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, characterPosition , currentWord.toString()));
                } else if( currentWord.length() > 1){
                    throw new SyntaxErrorException("Can only have one letter in character", lineNumber, characterPosition);
                }
                break;
            } else {
                break;
            }
        }
        if(slashCount != 0){
            throw new SyntaxErrorException("Syntax Error", lineNumber, characterPosition);
        }
        return retVal;
    }


    public List<Token> parseQuoted() throws Exception {
        String currentWord = "";
        var retVal = new LinkedList<Token>();
        int slashCount = 0;
        slashCount++;


        while(!textManager.isAtEnd()){
            char c = textManager.peekCharacter();
            if(c != '\"' && c != ' ' || c =='\n'){
                currentWord += textManager.getCharacter();
                characterPosition++;
                if(c == '\n'){
                    lineNumber++;
                    characterPosition = 0;
                }
            } else if(Character.isWhitespace(c)){
                currentWord += textManager.getCharacter();
                characterPosition++;
            } else if(c == '\"'){
                slashCount--;
                c = textManager.getCharacter();
                characterPosition++;
                if(currentWord.length() == 1 || currentWord.length() == 0){ //CHANGED THIS
                    retVal.add(new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, characterPosition , currentWord.toString()));
                } else if( currentWord.length() > 1){
                    retVal.add(new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, characterPosition , currentWord.toString()));
                }
                break;
            } else {
                break;
            }
        }
        if(slashCount != 0){
            throw new SyntaxErrorException("Syntax Error", lineNumber, characterPosition);
        }
        return retVal;
    }

    public List<Token> parseWord() throws Exception {
        String currentWord = "";
        var retVal = new LinkedList<Token>();

        while (!textManager.isAtEnd()) {
            char c = textManager.peekCharacter();
            {
                if (Character.isLetter(c)) {
                    currentWord += textManager.getCharacter();
                    characterPosition++; //ADDED
                } else if (c == '\n') {
                    if (!currentWord.isEmpty()) {
                        if(Hashmap.containsKey(currentWord)){
                            retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition, currentWord.toString()));
                        }
                        else{
                            retVal.add(new Token(Token.TokenTypes.WORD, lineNumber, characterPosition , currentWord.toString()));
                        }
                    }
                    c = textManager.getCharacter();
                    lineNumber++;
                    characterPosition = 0;
                    retVal.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                    if(!textManager.isAtEnd()){
                        c = textManager.peekCharacter();
                        while(c == '\n' && !textManager.isAtEnd()){
                            lineNumber++;
                            characterPosition = 0;
                            retVal.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                            c = textManager.getCharacter();
                            if(!textManager.isAtEnd()){
                                c = textManager.peekCharacter();
                            }
                        }
                    }
                    currentWord = "";
                    parseIndentation(retVal);

                } else if (c == ':') {
                    if(currentWord.equals("accessor") || currentWord.equals("mutator")){
                        currentWord += textManager.getCharacter();
                        characterPosition++;
                    }
                    break;
                } else if (c == ' ' || !Character.isLetter(c)) { //ADDED || !Character.isLetter(c)
                    break;
                } else {
                    if (!currentWord.isEmpty()) {
                        if(Hashmap.containsKey(currentWord)){
                            retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition, currentWord.toString()));
                        }
                        else{
                            retVal.add(new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, currentWord.toString()));
                        }
                        currentWord = "";
                    }
                }
            }
        }
        if (!currentWord.isEmpty()) {
            if(Hashmap.containsKey(currentWord)){
                if(currentWord.equals("accessor:") || currentWord.equals("mutator:")){
                    retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition));
                    retVal.add(new Token(Token.TokenTypes.COLON, lineNumber, characterPosition, ":"));
                } else{
                    retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition));
                }
            }
            else{
                retVal.add(new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, currentWord.toString()));
            }
        }
        return retVal;
    }

    public List<Token> parseNumber() throws Exception{
        return parseNumber("");
    }

    public List<Token> parseNumber(String currentWord) throws Exception {
        var retVal = new LinkedList<Token>();
        Boolean pointSeen = false;

        while (!textManager.isAtEnd()) {
            char c = textManager.peekCharacter();
            if (Character.isDigit(c)) {
                currentWord += textManager.getCharacter(); //changed this currentWord += c
                characterPosition++; //ADDED
            } else if (c == '.' && pointSeen) {
                if (!currentWord.isEmpty()) {
                    retVal.add(new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentWord.toString()));
                    currentWord = ".";
                    c = textManager.getCharacter(); //added this line
                    characterPosition++; //ADDED
                    pointSeen = true;
                }
            } else if(c == '.') {
                pointSeen = true;
                currentWord += textManager.getCharacter();
                characterPosition++; //ADDED
            } else if (!Character.isDigit(c)) {
                if (currentWord.equals(".")) { //DELETE THIS PART IF IT DONT WORK.
                    retVal.add(new Token(Token.TokenTypes.DOT, lineNumber, characterPosition, ""));
                    currentWord = "";
                    pointSeen = false;
                }
                break;
            }
            else{
                if (!currentWord.isEmpty()) {
                    retVal.add(new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentWord.toString()));
                    currentWord = "";
                    pointSeen = false;
                }
            }
        }
        if (!currentWord.isEmpty()) {
            retVal.add(new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentWord.toString()));
            pointSeen = false;
        }
        return retVal;
    }

    public void parseIndentation(LinkedList<Token> retVal) throws Exception{
        String currentWord = "";
        int numWhiteSpaces = 0;
        int numTabs = 0;

        while(!textManager.isAtEnd()) {
            char c = textManager.peekCharacter();
            if(!Character.isWhitespace(c) || c == '\n'){
                if(numWhiteSpaces % 4 == 0){ //FIX
                    int numWhiteSpacesGroupFour = numWhiteSpaces/4;
                    if(currentIndentLevel < numWhiteSpacesGroupFour){
                        while( currentIndentLevel < numWhiteSpacesGroupFour){
                            retVal.add(new Token(Token.TokenTypes.INDENT, lineNumber, characterPosition, currentWord.toString()));
                            currentIndentLevel++;
                        }
                        break;
                    } else if(currentIndentLevel > numWhiteSpacesGroupFour){
                        while(currentIndentLevel > numWhiteSpacesGroupFour){
                            retVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition, currentWord.toString()));
                            currentIndentLevel--;
                        }
                    }
                }
                break;
            }
            while(c == ' '){
                c = textManager.getCharacter();
                characterPosition++;
                numWhiteSpaces++;
                c = textManager.peekCharacter();
            }
            while(c == '\t'){ //ADDED
                c = textManager.getCharacter();
                characterPosition = characterPosition + 4; //because each tab consumes 4 spaces
                numTabs++;
                int numWhiteSpaces2 = numTabs * 4;
                c = textManager.peekCharacter();
                if(c != '\t'){
                    numWhiteSpaces = numWhiteSpaces + numWhiteSpaces2;
                }
            }
            if(numWhiteSpaces % 4 == 0){
                int numWhiteSpacesGroupFour = numWhiteSpaces/4;
                if(currentIndentLevel < numWhiteSpacesGroupFour){
                    while( currentIndentLevel < numWhiteSpacesGroupFour){
                        retVal.add(new Token(Token.TokenTypes.INDENT, lineNumber, characterPosition, currentWord.toString()));
                        currentIndentLevel++;
                    }
                    break;
                } else if(currentIndentLevel > numWhiteSpacesGroupFour){
                    while(currentIndentLevel > numWhiteSpacesGroupFour){
                        retVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition, currentWord.toString()));
                        currentIndentLevel--;
                    }
                }
            } else {
                break;
            }
        }
    };

    public List<Token> parsePunctuation() throws SyntaxErrorException {
        String currentWord = "";
        var retVal = new LinkedList<Token>();
        int numBrackets = 0;

        while(!textManager.isAtEnd()) {
            char c = textManager.getCharacter();
            characterPosition++; //ADDED
            switch(c){
                case '.':
                    currentWord += c;
                    break;
                case ':':
                    currentWord += c;
                    break;
                case '(':
                    currentWord += c;
                    break;
                case ')':
                    currentWord += c;
                    break;
                case '+':
                    currentWord += c;
                    break;
                case '*':
                    currentWord += c;
                    break;
                case '-':
                    currentWord += c;
                    break;
                case '/':
                    currentWord += c;
                    break;
                case ',':
                    currentWord += c;
                    break;
                case '>':
                    currentWord += c;
                    if(textManager.peekCharacter() == '='){
                        c = textManager.getCharacter();
                        characterPosition++; //ADDED
                        currentWord += c;
                    }
                    break;
                case '<':
                    currentWord += c;
                    if(textManager.peekCharacter() == '='){
                        c = textManager.getCharacter();
                        characterPosition++; //ADDED
                        currentWord += c;
                    }
                    break;
                case '=':
                    currentWord += c;
                    if(textManager.peekCharacter() == '='){
                        c = textManager.getCharacter();
                        characterPosition++; //ADDED
                        currentWord += c;
                    }
                    break;
                case '!':
                    currentWord += c;
                    if(textManager.peekCharacter() == '='){
                        c = textManager.getCharacter();
                        characterPosition++; //ADDED
                        currentWord += c;
                    }
                    break;
                case '&':
                    currentWord += c;
                    if(textManager.peekCharacter() == '&'){
                        c = textManager.getCharacter();
                        characterPosition++;
                        currentWord += c;
                    } else{
                        throw new SyntaxErrorException("Need another & for and", textManager.getCharacter(), textManager.getPosition());
                    }
                    break;
                case '|':
                    currentWord += c;
                    if(textManager.peekCharacter() == '|'){
                        c = textManager.getCharacter();
                        characterPosition++;
                        currentWord += c;
                    }else{
                        throw new SyntaxErrorException("Need another | and", textManager.getCharacter(), textManager.getPosition());
                    }
                    break;
                case '{':
                    numBrackets++;
                    while(!textManager.isAtEnd()){
                        c = textManager.peekCharacter();
                        if(c != '}' && c != '{'){
                            c = textManager.getCharacter();
                            characterPosition++; //ADDED
                        } else if (c == '}'){
                            numBrackets--;
                            c = textManager.getCharacter();
                            characterPosition++; //ADDED
                            if(numBrackets == 0){
                                break;
                            }
                        } else if(c == '{'){
                            numBrackets++;
                            c = textManager.getCharacter();
                            characterPosition++;
                            if(numBrackets == 0){
                                break;
                            }
                        }
                    }
                    if(numBrackets != 0){
                        throw new SyntaxErrorException("Syntax Error", lineNumber, characterPosition);
                    }
                    break;
                default:
                    if (!currentWord.isEmpty()) {
                        if(Hashmap.containsKey(currentWord)){
                            retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition, currentWord.toString()));
                            currentWord = "";
                        }
                    }
                    throw new SyntaxErrorException("Syntax Error", lineNumber, characterPosition);
            }
            if(Hashmap.containsKey(currentWord)){
                if(currentWord.equals("-")){
                    retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition, ""));
                } else{
                    retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition, currentWord.toString()));
                }
                //retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition, currentWord.toString()));
                currentWord = "";
            }
            break;
        }
        if (!currentWord.isEmpty()) {
            if(Hashmap.containsKey(currentWord)){
                retVal.add(new Token(Hashmap.get(currentWord), lineNumber, characterPosition, currentWord.toString()));
                currentWord = "";
            } else{
                retVal.add(new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, currentWord.toString()));
            }
        }
        return retVal;
    }
};