package dk.kb.yggdrasil.warc;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.jwat.common.Base32;
import org.jwat.warc.WarcDigest;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

public class Digest {
    
    private final MessageDigest md; 
    private final String digestType;
    
    /**
     * Constructor.
     * @param digestType Type of Digest used (SHA-1 or similar)
     * @throws YggdrasilException If the given Algorithm is unrecognized
     */
    public Digest(String digestType) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(digestType, "String digestType");
        this.digestType = digestType;
        try {
            md = MessageDigest.getInstance(digestType);
        } catch (NoSuchAlgorithmException e) {
            throw new YggdrasilException("The digestType '" + digestType + "' is unrecognized", e);
        } 
    }
    
    /**
     * Create a WarcDigest based on the given file.
     * @param fileToDigest The file to digest
     * @return a WarcDigest based on the given file.
     */
    public WarcDigest getDigestOfFile(File fileToDigest) throws YggdrasilException {
        ArgumentCheck.checkExistsNormalFile(fileToDigest, "File fileToDigest");
        WarcDigest res = null;
        md.reset();
        byte[] digestBytes;
        
        try {
            digestBytes = FileUtils.readFileToByteArray(fileToDigest);
            res = WarcDigest.createWarcDigest(digestType, digestBytes, 
                    "Base32", Base32.encodeArray(digestBytes));
        } catch (IOException e) {
            throw new YggdrasilException("Failed to generate digest for resource '"
                    + fileToDigest.getAbsolutePath() + "'", e);
        }
        
        return res;
    }
    
    /**
     * Create a WarcDigest based on the given bytes.
     * @param bytesToDigest The bytes to digest
     * @return a WarcDigest based on the given file.
     */
    public WarcDigest getDigestOfBytes(byte[] bytesToDigest) throws YggdrasilException {
        ArgumentCheck.checkNotNull(bytesToDigest, "byte[] bytesToDigest");
        WarcDigest res = null;
        md.reset();
        res = WarcDigest.createWarcDigest(digestType, bytesToDigest, 
                "Base32", Base32.encodeArray(bytesToDigest));
        return res;
    }
    
    
    
}
