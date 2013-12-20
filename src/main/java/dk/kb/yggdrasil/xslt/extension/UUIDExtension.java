package dk.kb.yggdrasil.xslt.extension;

import java.util.UUID;

/**
 * UUID XSLT extensions.
 */
public class UUIDExtension {

    /**
     * Private constructor to prevent instantiation of extension class.
     */
    private UUIDExtension() {
    }

    /**
     * @return generate and return a random UUID
     */
    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

}
