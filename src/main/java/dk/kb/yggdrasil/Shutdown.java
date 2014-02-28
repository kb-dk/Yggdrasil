package dk.kb.yggdrasil;

import java.io.File;
import java.io.IOException;

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
     */
    public static void main(String[] args) throws IOException, YggdrasilException {
      File configDir = Main.getConfigDir();
      File rabbitmqConfigFile = new File(configDir, Main.RABBITMQ_CONF_FILENAME);
      if (!rabbitmqConfigFile.exists()) {
          throw new YggdrasilException(
                  "Unable to shutdown Yggdrasil. Rabbitmq configuration is missing. "
          		+ "Looked here: " +  rabbitmqConfigFile.getAbsolutePath());
      }
      RabbitMqSettings rabbitMqSettings = new RabbitMqSettings(rabbitmqConfigFile);
      MQ mq = new MQ(rabbitMqSettings);
      String message = "Shutdown Yggdrasil, please";
      mq.publishOnQueue(rabbitMqSettings.getPreservationDestination(), 
              message.getBytes(), MQ.SHUTDOWN_MESSAGE_TYPE);
      mq.cleanup();
    }

}
