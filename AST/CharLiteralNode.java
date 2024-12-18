package AST;

public class CharLiteralNode implements ExpressionNode {
    public Character value;
    @Override
    public String toString() {
        return "'" + value + "'";
    }
}
