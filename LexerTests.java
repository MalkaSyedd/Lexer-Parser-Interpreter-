import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class LexerTests {
    @Test
    public void SimpleLexerTest1() {
        var l = new Lexer("jhdgf cd efshhs gjdjh");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals("jhdgf", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("efshhs", res.get(2).getValue());
            Assertions.assertEquals("gjdjh", res.get(3).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.WORD, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MultilineLexerTest1() {
        var l = new Lexer("jhg hggy huhygv njnyg\ngvyvchgvgbjm\nhbv iuhhj jhghj \n\nuyguvb");
        try {
            var res = l.Lex();
            Assertions.assertEquals(13, res.size());
            Assertions.assertEquals("jhg", res.get(0).getValue());
            Assertions.assertEquals("hggy", res.get(1).getValue());
            Assertions.assertEquals("huhygv", res.get(2).getValue());
            Assertions.assertEquals("njnyg", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());
            Assertions.assertEquals("gvyvchgvgbjm", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(6).getType());
            Assertions.assertEquals("hbv", res.get(7).getValue());
            Assertions.assertEquals("iuhhj", res.get(8).getValue());
            Assertions.assertEquals("jhghj", res.get(9).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(11).getType());
            Assertions.assertEquals("uyguvb", res.get(12).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentTest() {
        var l = new Lexer("jhdgf {cdefshhs} gjdjh");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals("jhdgf", res.get(0).getValue());
            Assertions.assertEquals("gjdjh", res.get(1).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.WORD, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentTest1() {
        var l = new Lexer("jhdgf {cd efsh      h s} gjdjh 9.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(3, res.size());
            Assertions.assertEquals("jhdgf", res.get(0).getValue());
            Assertions.assertEquals("gjdjh", res.get(1).getValue());
            Assertions.assertEquals("9.2", res.get(2).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentSyntaxError() { //NOT FIXED
        var l = new Lexer(" {jnjdubdbbf");
        try {
            String expectedMessage = "Error at line 0 at character 13";
            Assertions.assertThrows(SyntaxErrorException.class, () -> l.Lex());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentSyntaxError2() {
        var l = new Lexer("{jnjdubdbbf} {{}{}{}{}}");
        try {
            var res = l.Lex();
            Assertions.assertEquals(0, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    @Test
    public void CommentSyntaxError3() {
        var l = new Lexer("{{}");
        try {
            Assertions.assertThrows(SyntaxErrorException.class, () -> l.Lex());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentIndentTest() {
        var l = new Lexer(
                "jsh{loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n}"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
            Assertions.assertEquals("jsh", res.get(0).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }
    @Test
    public void QuotedStringMultiLexerTest() {
        var l = new Lexer("test \"hello\nworld\" \"th\nere\" 1.2"); //IS THIS THE MULTILINE TEST TO BE PASSING?
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());

            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("test", res.get(0).getValue());

            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("hello\nworld", res.get(1).getValue());

            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(2).getType());
            Assertions.assertEquals("th\nere", res.get(2).getValue());

            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("1.2", res.get(3).getValue());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void QuotedSyntaxError() { //CHECK IF THIS MAKES SENSE
        var l = new Lexer("\"{{}");
        try {
            Assertions.assertThrows(SyntaxErrorException.class, () -> l.Lex());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentIndentTestEnds() {
        var l = new Lexer(
                "jsh{loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n"
        );
        try {
            Assertions.assertThrows(SyntaxErrorException.class, () -> l.Lex());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentSyntaxError4() {
        var l = new Lexer("{fjushd} {dkfj{{{{{}}{}");
        try {
            Assertions.assertThrows(SyntaxErrorException.class, () -> l.Lex());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void IndentTestTabs() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "\tif n >= 15\n" +
                        "\t\tkeepGoing = false\n"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(16, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void IndentTestTabsSingleSingleLine() {
        var l = new Lexer(
                "loop keepGoing\n\tif n >= 15\n\t\tkeepGoing = false\n"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(16, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }



    @Test
    public void SimpleLexerNumberTest() {
        var l = new Lexer("9 2 3 6");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals("9", res.get(0).getValue());
            Assertions.assertEquals("2", res.get(1).getValue());
            Assertions.assertEquals("3", res.get(2).getValue());
            Assertions.assertEquals("6", res.get(3).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void ComplicatedLexerNumberTest() {
        var l = new Lexer("9.2 3.6");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
            Assertions.assertEquals("9.2", res.get(0).getValue());
            Assertions.assertEquals("3.6", res.get(1).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void LexerParserTest() { //FIX THIS
        var l = new Lexer("interface someName\n" +
                "    square() : number s\n" +
                "class TranExample implements someName\n" +
                "\tnumber m\n" +
                "\t\taccessor:");
        try {
            var res = l.Lex();
            Assertions.assertEquals(26, res.size());
            Assertions.assertEquals(Token.TokenTypes.INTERFACE, res.get(0).getType());
            Assertions.assertEquals("someName", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(3).getType());
            Assertions.assertEquals("square", res.get(4).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(7).getType());
            Assertions.assertEquals("number", res.get(8).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(8).getType());
            Assertions.assertEquals("s", res.get(9).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(9).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(11).getType());
            Assertions.assertEquals(Token.TokenTypes.CLASS, res.get(12).getType());
            Assertions.assertEquals("TranExample", res.get(13).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(13).getType());
            Assertions.assertEquals(Token.TokenTypes.IMPLEMENTS, res.get(14).getType());
            Assertions.assertEquals("someName", res.get(15).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(15).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(16).getType());
            Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(17).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void SyntaxError() {
        var l = new Lexer(" [");
        try {
            Assertions.assertThrows(SyntaxErrorException.class, () -> l.Lex());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }



    @Test
    public void ComplicatedLexerNumberTest3() {
        var l = new Lexer("9.2 3.6.8.8.99 2.5");
        try {
            var res = l.Lex();
            //Assertions.assertEquals(6, res.size());
            Assertions.assertEquals("9.2", res.get(0).getValue());
            Assertions.assertEquals("3.6", res.get(1).getValue());
            Assertions.assertEquals(".8", res.get(2).getValue());
            Assertions.assertEquals(".8", res.get(3).getValue());
            Assertions.assertEquals(".99", res.get(4).getValue());
            Assertions.assertEquals("2.5", res.get(5).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void ComplicatedLexerNumberTest2() {
        var l = new Lexer("9.2 3.6.8");
        try {
            var res = l.Lex();
            Assertions.assertEquals(3, res.size());
            Assertions.assertEquals("9.2", res.get(0).getValue());
            Assertions.assertEquals("3.6", res.get(1).getValue());
            Assertions.assertEquals(".8", res.get(2).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, result.getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void LexingParsing() {
        var l = new Lexer("interface someName\n\tcrazystuff()");
        try {
            var res = l.Lex();
            Assertions.assertEquals(8, res.size());
            Assertions.assertEquals(Token.TokenTypes.INTERFACE, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(7).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void LexingParsing1() {
        var l = new Lexer("interface someName\r\n\tupdateClock()\r\n\tsquare() : number s");
        try {
            var res = l.Lex();
            Assertions.assertEquals(15, res.size());
            Assertions.assertEquals(Token.TokenTypes.INTERFACE, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(7).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(8).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(9).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(10).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(11).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(12).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(13).getType());
            Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(14).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void KeyWordLexerTest2() {
        var l = new Lexer("accessor: accessor mutator: mutator");
        try {
            var res = l.Lex();
            Assertions.assertEquals(6, res.size());
            Assertions.assertEquals(Token.TokenTypes.ACCESSOR, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(2).getType());
            Assertions.assertEquals("accessor", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.MUTATOR, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(5).getType());
            Assertions.assertEquals("mutator", res.get(5).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }


    @Test
    public void ComplicatedLexerMixedTest() {
        var l = new Lexer("x==.5 m=1- zk7");
        try {
            var res = l.Lex();
            Assertions.assertEquals(9, res.size());
            Assertions.assertEquals("x", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("==", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(1).getType());
            Assertions.assertEquals(".5", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(2).getType());
            Assertions.assertEquals("m", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
            Assertions.assertEquals("=", res.get(4).getValue());
            Assertions.assertEquals(Token.TokenTypes.ASSIGN, res.get(4).getType());
            Assertions.assertEquals("1", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(5).getType());
            Assertions.assertEquals("", res.get(6).getValue());
            Assertions.assertEquals(Token.TokenTypes.MINUS, res.get(6).getType());
            Assertions.assertEquals("zk", res.get(7).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(7).getType());
            Assertions.assertEquals("7", res.get(8).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(8).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }
}
