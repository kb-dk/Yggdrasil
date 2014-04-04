package dk.kb.yggdrasil.warc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jwat.warc.WarcDigest;

import dk.kb.yggdrasil.exceptions.ArgumentCheck;
import dk.kb.yggdrasil.exceptions.YggdrasilException;

public class Digest {

    private final MessageDigest md; 
    private final String digestType;

    /** The maximal size of the byte array for digest.*/
    private static final int BYTE_ARRAY_SIZE_FOR_DIGEST = 4096;

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
        
        try {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileToDigest);
                byte[] checksumBytes = calculateChecksumWithMessageDigest(fis);    	
                String checksum = decodeBase16(checksumBytes);
                return createWarcDigest(checksum);
            } finally {
                if(fis != null) {
                    fis.close();
                }
            }
        } catch (IOException e) {
            throw new YggdrasilException("Could not calculate checksum of file '" + fileToDigest + "'", e);
        }
    }

    /**
     * Create a WarcDigest based on the given bytes.
     * @param bytesToDigest The bytes to digest
     * @return a WarcDigest based on the given file.
     * @throws YggdrasilException 
     */
    public WarcDigest getDigestOfBytes(byte[] bytesToDigest) throws YggdrasilException {
        ArgumentCheck.checkNotNullOrEmpty(bytesToDigest, "byte[] bytesToDigest");
        ByteArrayInputStream bais = new ByteArrayInputStream(bytesToDigest);
        byte[] checksumBytes = calculateChecksumWithMessageDigest(bais);    	
        String checksum = decodeBase16(checksumBytes);
        return createWarcDigest(checksum);
    }

    /**
     * Calculates the checksum of an InputStream.
     * @param content The content to calculate the checksum of.
     * @return The 16-bit encoded checksum. 
     * @throws YggdrasilException If something goes wrong.
     */
    private byte[] calculateChecksumWithMessageDigest(InputStream content) throws YggdrasilException {
        byte[] bytes = new byte[BYTE_ARRAY_SIZE_FOR_DIGEST];
        int bytesRead;
        try {
            md.reset();
            while ((bytesRead = content.read(bytes)) > 0) {
                md.update(bytes, 0, bytesRead);
            }
            return md.digest();
        } catch (Exception e) {
            throw new YggdrasilException("Cannot calculate the checksum.", e);
        }
    }

    /**
     * Decodes a Base16 encoded byte set into a human readable string.
     * @param data The data to decode.
     * @return The decoded data, or null if a null is given.
     */
    private static String decodeBase16(byte[] data) {
        if(data == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++){
            int v = data[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    /**
     * Creates the WarcDigest for the checksum.
     * @param checksum The checksum for the warc-digest.
     * @return The WarcDigest with the algorithm for this instance and the given checksum.
     */
    private WarcDigest createWarcDigest(String checksum) {
        return WarcDigest.parseWarcDigest(digestType + ":" + checksum);
    }
}
