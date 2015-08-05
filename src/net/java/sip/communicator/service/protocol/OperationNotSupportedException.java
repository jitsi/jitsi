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
package net.java.sip.communicator.service.protocol;

/**
 * The OperationNotSupportedException is used by telephony providers as an
 * indication that a requested operation is not supported or implemented.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class OperationNotSupportedException
    extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Initializes a new <code>OperationNotSupportedException</code> instance
     * which does not give a human-readable explanation why the operation is
     * not supported.
     */
    public OperationNotSupportedException()
    {
        this(null);
    }

    /**
     * Creates an OperationNotSupportedException instance with the specified
     * reason phrase.
     *
     * @param message
     *            a detailed message explaining any particular details as to why
     *            is not the specified operation supported or null if no
     *            particular details exist.
     */
    public OperationNotSupportedException(String message)
    {
        super(message);
    }
}
