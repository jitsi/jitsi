/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * SimpleException indicates an exception that occurred in the API.
 *
 * <p> SimpleException contains an error code that gives more information on the
 * exception. The application can obtain the error code using
 * SimpleException.getErrorCode. The error code values are defined in the
 * SimpleException fields.</p>
 *
 * @author Emil Ivov
 */
public class OperationFailedException
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
    public static final int NETWORK_FAILURE = 2;

    /**
     * Set to indicate that a provider needs to be registered or signed on
     * a public service before calling the method that threw the exception.
     */
    public static final int PROVIDER_NOT_REGISTERED = 3;

    /**
     * Set when an operation fails for implementation specific reasons.
     */
    public static final int INTERNAL_ERROR = 4;

    /**
     * Indicates that a user has tried to subscribe to a contact that already
     * had an active subscription.
     */
    public static final int SUBSCRIPTION_ALREADY_EXISTS = 5;


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
    public OperationFailedException(String message, int errorCode)
    {
        super(message);
    }

    /**
     * Creates an exception with the specified message, errorCode and cause.
     * @param message A message containing details on the error that caused the
     * exception
     * @param errorCode the error code of the exception (one of the error code
     * fields of this class)
     * @param cause the error that caused this exception
     */
    public OperationFailedException(String message, int errorCode,
                                    Throwable cause)
    {
        super(message, cause);
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
