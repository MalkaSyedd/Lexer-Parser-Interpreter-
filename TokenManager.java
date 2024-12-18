import java.util.List;
import java.util.Optional;

public class TokenManager {
    private List<Token> tokens;
    public int currentIndexofToken = 0;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean done() {
        return currentIndexofToken >= tokens.size();
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        Optional<Token> nextToken = peek(currentIndexofToken);
        if(nextToken.isPresent() && nextToken.get().getType() == t) {
            tokens.remove(0);
            return nextToken;
        }
        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        if(currentIndexofToken < tokens.size()) {
            return Optional.of(tokens.get(i));
        }
        return Optional.empty();
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second){
        if(tokens.size() < 2){
            return false;
        }
        Optional<Token> firstToken = peek(0);
        Optional<Token> secondToken = peek(1);
        if(firstToken.isPresent() && secondToken.isPresent()){
            if(firstToken.get().getType() == first && secondToken.get().getType() == second) {
                return true;
            }
        }
        return false;
    }

    public int getCurrentLine(){
        return tokens.get(currentIndexofToken).getLineNumber();
    }

    public int getCurrentColumnNumber(){
        return tokens.get(currentIndexofToken).getColumnNumber();
    }
}