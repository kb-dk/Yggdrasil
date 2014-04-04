package dk.kb.yggdrasil.warc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.warc.WarcDigest;

import dk.kb.yggdrasil.exceptions.YggdrasilException;

@RunWith(JUnit4.class)
public class TestDigest {

    @Test
    public void testDigestBytesMD5() throws YggdrasilException {
        String testString = "Yggdrasil\n";
        String expectedChecksum = "c1d6d0fbe801dbe092ee1987ae16fb74";

        Digest digest = new Digest("MD5");

        WarcDigest res = digest.getDigestOfBytes(testString.getBytes());

        Assert.assertNotNull(res);
        Assert.assertEquals("md5", res.algorithm);
        Assert.assertEquals(expectedChecksum, res.digestString);
    }

    @Test
    public void testDigestFileMD5() throws YggdrasilException, IOException {
        File file = File.createTempFile("Digest", "test");
        String testString = "Yggdrasil\n";
        String expectedChecksum = "c1d6d0fbe801dbe092ee1987ae16fb74";

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(testString.getBytes());
            fos.flush();
            fos.close();

            Digest digest = new Digest("MD5");

            WarcDigest res = digest.getDigestOfFile(file);

            Assert.assertNotNull(res);
            Assert.assertEquals("md5", res.algorithm);
            Assert.assertEquals(expectedChecksum, res.digestString);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testDigestBytesSHA1() throws YggdrasilException {
        String testString = "Yggdrasil\n";
        String expectedChecksum = "e46d62fd8ed195a644ce7151c55ebbf37e5e17d2";

        Digest digest = new Digest("SHA1");

        WarcDigest res = digest.getDigestOfBytes(testString.getBytes());

        Assert.assertNotNull(res);
        Assert.assertEquals("sha1", res.algorithm);
        Assert.assertEquals(expectedChecksum, res.digestString);
    }

    @Test
    public void testDigestFileSHA1() throws YggdrasilException, IOException {
        File file = File.createTempFile("Digest", "test");
        String testString = "Yggdrasil\n";
        String expectedChecksum = "e46d62fd8ed195a644ce7151c55ebbf37e5e17d2";

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(testString.getBytes());
            fos.flush();
            fos.close();

            Digest digest = new Digest("SHA1");

            WarcDigest res = digest.getDigestOfFile(file);

            Assert.assertNotNull(res);
            Assert.assertEquals("sha1", res.algorithm);
            Assert.assertEquals(expectedChecksum, res.digestString);
        } finally {
            file.delete();
        }
    }
}
