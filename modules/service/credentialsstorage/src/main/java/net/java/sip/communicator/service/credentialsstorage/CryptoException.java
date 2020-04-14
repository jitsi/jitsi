/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
