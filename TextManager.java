public class TextManager {
    private String text;
    private int position = 0;

    public TextManager(String text) {
        this.text = text;
    }

    public char getCharacter(){
        return text.charAt(position++);
    }

    public boolean isAtEnd() {
        if (position == text.length()) {
            return true;
        }
        return false;
    }

    public char peekCharacter() {
        return text.charAt(position);
    }

    public char peekCharacter(int distance) {
        return text.charAt(distance);
    }

    public int getPosition() {
        return position;
    }



}

