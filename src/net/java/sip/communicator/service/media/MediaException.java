/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media;

/**
 * Thrown when errors occur in the media package.
 *
 * @author Emil Ivov
 */
public class MediaException
    extends Exception
{
    /**
     * Set when no other error code can describe the exception that occurred.
     */
    public static final int GENERAL_ERROR = 1;

    /**
     * Set when command fails due to a failure in network communications or
     * a transport error.
     */
    public static final int NETWORK_ERROR = 2;

    /**
     * Set to indicate that the service implementation needs to be started
     * (initialized) before calling the method that threw the exception.
     */
    public static final int SERVICE_NOT_STARTED = 3;

    /**
     * Set when an operation fails for implementation specific reasons.
     */
    public static final int INTERNAL_ERROR = 4;

    /**
     * Set when an operation fails because of an input/output error.
     */
    public static final int IO_ERROR = 4;

    /**
     * Set when the media service is requested to receive or transmit in a
     * format set that is not supported by the implementation.
     */
    public static final int UNSUPPORTED_FORMAT_SET_ERROR = 5;

    /**
     * The error code of the exception
     */
    private int errorCode = GENERAL_ERROR;

    /**
     * Creates an exception with the specified error message and error code.
     * @param message A message containing details on the error that caused the
     * exception
     * @param errorCode the error code of the exception (one of the error code
     * fields of this class)
     */
    public MediaException(String message, int errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with the specified message, errorCode and cause.
     * @param message A message containing details on the error that caused the
     * exception
     * @param errorCode the error code of the exception (one of the error code
     * fields of this class)
     * @param cause the error that caused this exception
     */
    public MediaException(String message, int errorCode, Throwable cause)
    {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Obtain the error code value.
     *
     * @return the error code for the exception.
     */
    public int getErrorCode()
    {
        return errorCode;
    }
}
