package dk.kb.yggdrasil.exceptions;

/**
 * A locally defined RabbitMQ expected to throw when RabbitMQ connection error
 * occurs.
 */
public class RabbitException extends Exception {
    /**
     * Constructs new YggdrasilException with the specified detail message.
     * @param message The detail message
     */
    public RabbitException(String message) {
        super(message);
    }

    /**
     * Constructs new YggdrasilException with the specified
     * detail message and cause.
     * @param message The detail message
     * @param cause The cause
     */
    public RabbitException(String message, Throwable cause) {
        super(message, cause);
    }

    
}
