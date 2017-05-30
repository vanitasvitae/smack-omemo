package org.jivesoftware.smackx.hash;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.StringUtils;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static junit.framework.TestCase.assertEquals;

/**
 * Test HashUtil functionality.
 */
public class HashUtilTest extends SmackTestSuite {

    public static final String testString = "Hello World!";

    private byte[] array() {
        try {
            return testString.getBytes(StringUtils.UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF8 MUST be supported.");
        }
    }

    @Test
    public void md5Test() {
        String expected = "ed076287532e86365e841e92bfc50d8c";
        String actual = HashUtil.hex(HashUtil.md5(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha1Test() {
        String expected = "2ef7bde608ce5404e97d5f042f95f89f1c232871";
        String actual = HashUtil.hex(HashUtil.sha_1(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha224Test() {
        String expected = "4575bb4ec129df6380cedde6d71217fe0536f8ffc4e18bca530a7a1b";
        String actual = HashUtil.hex(HashUtil.sha_224(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha256Test() {
        String expected = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069";
        String actual = HashUtil.hex(HashUtil.sha_256(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha384Test() {
        String expected = "bfd76c0ebbd006fee583410547c1887b0292be76d582d96c242d2a792723e3fd6fd061f9d5cfd13b8f961358e6adba4a";
        String actual = HashUtil.hex(HashUtil.sha_384(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha512Test() {
        String expected = "861844d6704e8573fec34d967e20bcfef3d424cf48be04e6dc08f2bd58c729743371015ead891cc3cf1c9d34b49264b510751b1ff9e537937bc46b5d6ff4ecc8";
        String actual = HashUtil.hex(HashUtil.sha_512(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha3_224Test() {
        String expected = "716596afadfa17cd1cb35133829a02b03e4eed398ce029ce78a2161d";
        String actual = HashUtil.hex(HashUtil.sha3_224(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha3_256Test() {
        String expected = "d0e47486bbf4c16acac26f8b653592973c1362909f90262877089f9c8a4536af";
        String actual = HashUtil.hex(HashUtil.sha3_256(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha3_384Test() {
        String expected = "f324cbd421326a2abaedf6f395d1a51e189d4a71c755f531289e519f079b224664961e385afcc37da348bd859f34fd1c";
        String actual = HashUtil.hex(HashUtil.sha3_384(array()));
        assertEquals(expected, actual);
    }

    @Test
    public void sha3_512Test() {
        String expected = "32400b5e89822de254e8d5d94252c52bdcb27a3562ca593e980364d9848b8041b98eabe16c1a6797484941d2376864a1b0e248b0f7af8b1555a778c336a5bf48";
        String actual = HashUtil.hex(HashUtil.sha3_512(array()));
        assertEquals(expected, actual);
    }

    public void id_blake2b160Test() {
        String expected = "e7338d05e5aa2b5e4943389f9475fce2525b92f2";
        String actual = HashUtil.hex(HashUtil.id_blake2b160(array()));
        assertEquals(expected, actual);
    }

    public void id_blake2b256Test() {
        String expected = "bf56c0728fd4e9cf64bfaf6dabab81554103298cdee5cc4d580433aa25e98b00";
        String actual = HashUtil.hex(HashUtil.id_blake2b256(array()));
        assertEquals(expected, actual);
    }

    public void id_blake2b384Test() {
        String expected = "53fd759520545fe93270e61bac03b243b686af32ed39a4aa635555be47a89004851d6a13ece00d95b7bdf9910cb71071";
        String actual = HashUtil.hex(HashUtil.id_blake2b384(array()));
        assertEquals(expected, actual);
    }

    public void id_blake2b512Test() {
        String expected = "54b113f499799d2f3c0711da174e3bc724737ad18f63feb286184f0597e1466436705d6c8e8c7d3d3b88f5a22e83496e0043c44a3c2b1700e0e02259f8ac468e";
        String actual = HashUtil.hex(HashUtil.id_blake2b512(array()));
        assertEquals(expected, actual);
    }
}
