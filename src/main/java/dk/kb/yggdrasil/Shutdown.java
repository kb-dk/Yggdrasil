package dk.kb.yggdrasil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import dk.kb.yggdrasil.config.Config;
import dk.kb.yggdrasil.config.RabbitMqSettings;
import dk.kb.yggdrasil.exceptions.RabbitException;
import dk.kb.yggdrasil.exceptions.YggdrasilException;
import dk.kb.yggdrasil.messaging.MQ;

/** 
 * Class for shutting down the yggdrasil by
 * sending a shutdown message to the preservation destination.
 */
public class Shutdown {

    /**
     * This program takes no arguments. Supposed to be called from the shutdown.sh
     * so it knows where the config files are.
     * @param args Not used 
     * @throws YggdrasilException If we couldn't find the file {Main.RABBITMQ_CONF_FILENAME}
     * @throws IOException 
     * @throws RabbitException 
     */
    public static void main(String[] args) throws IOException, YggdrasilException, RabbitException {
        Config config = new Config();
        MQ mq = new MQ(config.getMqSettings());
        String message = "Shutdown Yggdrasil, please";
        mq.publishOnQueue(config.getMqSettings().getPreservationDestination(), 
                message.getBytes(Charset.defaultCharset()), MQ.SHUTDOWN_MESSAGE_TYPE);
        mq.close();
    }

}
