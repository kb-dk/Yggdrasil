package dk.kb.yggdrasil.exceptions;

public class YggdrasilException extends Exception {


    /**
     * Constructs new YggdrasilException with the specified detail message.
     * @param message The detail message
     */
      public YggdrasilException(String message) {
          super(message);
      }

    /**
     * Constructs new YggdrasilException with the specified
     * detail message and cause.
     * @param message The detail message
     * @param cause The cause
     */
      public YggdrasilException(String message, Throwable cause) {
          super(message, cause);
      }
    
}
