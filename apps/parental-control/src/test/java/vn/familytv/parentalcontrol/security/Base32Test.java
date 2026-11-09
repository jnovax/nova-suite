package vn.familytv.parentalcontrol.security;

import org.junit.Assert;
import org.junit.Test;
import java.nio.charset.StandardCharsets;

public class Base32Test {
    @Test
    public void testEncodeAndDecodeAscii() {
        byte[] input = "Hello World".getBytes(StandardCharsets.UTF_8);
        String encoded = Base32.encode(input);
        byte[] decoded = Base32.decode(encoded);
        Assert.assertArrayEquals(input, decoded);
    }

    @Test
    public void testRfc4648TestVectors() {
        Assert.assertEquals("", Base32.encode("".getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("MY======", Base32.encode("f".getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("MZXQ====", Base32.encode("fo".getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("MZXW6===", Base32.encode("foo".getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("MZXW6YQ=", Base32.encode("foob".getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("MZXW6YTB", Base32.encode("fooba".getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("MZXW6YTBOI======", Base32.encode("foobar".getBytes(StandardCharsets.UTF_8)));
    }
}
