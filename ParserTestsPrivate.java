import AST.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
//THESE WERE TESTS WRITTEN TO TEST MY CODE
public class ParserTestsPrivate {
    private TranNode LexAndParse(String input, int tokenCount) throws Exception {
        var l = new Lexer(input);
        var tokens = l.Lex();
        Assertions.assertEquals(tokenCount, tokens.size());
        var tran = new TranNode();
        var p = new Parser(tran, tokens);
        p.Tran();
        return tran;
    }

    //SELF WRITTEN
    @Test
    public void Interfacetest1() throws Exception {
        var t = LexAndParse("interface someName\n\tcrazystuff()", 8);
        Assertions.assertEquals(1, t.Interfaces.size());
    }

    @Test
    public void Interfacetest2() throws Exception {
        var t = LexAndParse("interface someName\n\tcrazystuff(number i)", 10);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(1, t.Interfaces.getFirst().methods.size());
    }

    @Test
    public void Interfacetest3() throws Exception {
        var t = LexAndParse("interface someName\n\tcrazystuff(number i, number j)", 13);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(1, t.Interfaces.getFirst().methods.size());
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.getFirst().parameters.size());
    }



    @Test
    public void Interfacetest4() throws Exception {
        var t = LexAndParse("interface someName\n\tcrazystuff(number i, number j)\n\tmorecrazyStuff()", 17);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.getFirst().parameters.size());
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.size());
    }

    @Test
    public void Interfacetest5() throws Exception {
        var t = LexAndParse("interface someName\n\tcrazystuff(number i, number j)\n\ttooCrazy()\n\tlittleMoreCrazy()", 21);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.getFirst().parameters.size());
        Assertions.assertEquals(3, t.Interfaces.getFirst().methods.size());
    }


    @Test
    public void testInterface6() throws Exception {
        var t = LexAndParse("interface someName\n\tupdateClock()\n\tsquare() : number s", 15);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.size());
    }

    @Test
    public void testInterface7() throws Exception {
        var t = LexAndParse("interface someName\n\tupdateClock()\n\tsquare() : number n\n\ttriangle() : trip k", 22);

        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(3, t.Interfaces.getFirst().methods.size());
        Assertions.assertEquals(0, t.Interfaces.getFirst().methods.get(0).returns.size()); // updateClock() has no return type
        Assertions.assertEquals(1, t.Interfaces.getFirst().methods.get(1).returns.size()); // square() has a return type
        Assertions.assertEquals(1, t.Interfaces.getFirst().methods.get(2).returns.size()); // triangle() has a return type
    }

    @Test
    public void testInterface8() throws Exception {
        var t = LexAndParse("interface someName\n\tupdateClock()\n\tsquare() : number n\n\ttriangle() : trip k, name k", 25);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(3, t.Interfaces.getFirst().methods.size());
        Assertions.assertEquals(0, t.Interfaces.getFirst().methods.get(0).returns.size()); // updateClock() has no return type
        Assertions.assertEquals(1, t.Interfaces.getFirst().methods.get(1).returns.size()); // square() has a return type
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.get(2).returns.size()); // triangle() has a return type
    }

    @Test
    public void testInterface9() throws Exception {
        var t = LexAndParse("interface someName\n\n\n\n\n\n\n\n\n\tupdateClock()\n\tsquare() : number n\n\ttriangle() : trip k, name k\n\trectangle() : length l, width w, height h", 46);
        Assertions.assertEquals(1, t.Interfaces.size());
        Assertions.assertEquals(4, t.Interfaces.getFirst().methods.size());
        Assertions.assertEquals(0, t.Interfaces.getFirst().methods.get(0).returns.size()); // updateClock() has no return type
        Assertions.assertEquals(1, t.Interfaces.getFirst().methods.get(1).returns.size()); // square() has a return type
        Assertions.assertEquals(2, t.Interfaces.getFirst().methods.get(2).returns.size()); // triangle() has a return type
        Assertions.assertEquals(3, t.Interfaces.getFirst().methods.get(3).returns.size()); // triangle() has a return type
    }

}
