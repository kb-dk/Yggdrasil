package dk.kb.yggdrasil.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide the hostname of the machine on which the program is running.
 * @return the hostname as a {@link String}
 */
public class HostName {
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(HostName.class.getName());
    
    public String getHostName () throws UnknownHostException {
        String hostName;
        try {
            //Trying to get hostname through InetAddress
            InetAddress iAddress = InetAddress.getLocalHost();
            hostName = iAddress.getHostName();
            
            //Trying to do better and get Canonical hostname
            String canonicalHostName = iAddress.getCanonicalHostName();         
            hostName = canonicalHostName;
            
            if (StringUtils.isNotEmpty(hostName)) {
                logger.info("Local hostname (provided  by iAddress): " + hostName);
                return hostName;
            }
            
        } catch (UnknownHostException  e) {
            // Failed the standard Java way, trying alternative ways.
        }
        
        // Trying to get hostname through environment properties.
        //      
        hostName = System.getenv("COMPUTERNAME");
        if (hostName != null) {
            logger.info("Local hostname (provided by System.getenv COMPUTERNAME): " + hostName);
            return hostName;
        }
        hostName = System.getenv("HOSTNAME");
        if (hostName != null) {
            logger.info("Local hostname (provided by System.getenv HOSTNAME): " + hostName);
            return hostName;
        }
        // Nothing worked, hostname undetermined.
        logger.warn("Hostname undetermined");
        throw new UnknownHostException();
    }
}
