/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.credentialsstorage;

/**
 * Exception thrown by the Crypto encrypt/decrypt interface methods.
 *
 * @author Dmitri Melnikov
 */
public class CryptoException
    extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -5424208764356198091L;

    /**
     * Set when encryption fails.
     */
    public static final int ENCRYPTION_ERROR = 1;

    /**
     * Set when decryption fails.
     */
    public static final int DECRYPTION_ERROR = 2;

    /**
     * Set when a decryption fail is caused by the wrong key.
     */
    public static final int WRONG_KEY = 3;

    /**
     * The error code of this exception.
     */
    private final int errorCode;

    /**
     * Constructs the crypto exception.
     *
     * @param code the error code
     * @param cause the original exception that this instance wraps
     */
    public CryptoException(int code, Exception cause)
    {
        super(cause);

        this.errorCode = code;
    }

    /**
     * @return the error code for the exception.
     */
    public int getErrorCode()
    {
        return errorCode;
    }
}
