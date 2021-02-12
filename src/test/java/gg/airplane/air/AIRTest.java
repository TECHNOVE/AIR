package gg.airplane.air;

import gg.technove.air.AIR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// todo way more comment & section tests
public class AIRTest {
    @Test
    public void simpleParseTest() throws IOException {
        String contents = "# Hello, World\n" +
                "[_head]\n" +
                "\n" +
                "[foo]\n" +
                "bar = \"wow\"";

        AIR parser = new AIR(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals(parser.get("foo.bar", "nonexistent"), "wow");
        Assertions.assertEquals(parser.get("foo.bar2", "nonexistent"), "nonexistent");
    }

    @Test
    public void simpleWriteTest() throws IOException {
        AIR parser = new AIR();

        Assertions.assertEquals(parser.get("foo.bar", "hello"), "hello");
        Assertions.assertEquals(parser.get("foo.bar", "goodbye"), "hello");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        parser.save(outputStream);
        String conf = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        Assertions.assertEquals("[foo]\n  bar = \"hello\"\n\n", conf);
    }
}
