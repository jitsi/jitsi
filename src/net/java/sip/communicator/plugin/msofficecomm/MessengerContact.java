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
package net.java.sip.communicator.plugin.msofficecomm;

import net.java.sip.communicator.util.*;

/**
 * Represents the Java counterpart of a native <tt>IMessengerContact</tt>
 * implementation.
 *
 * @author Lyubomir Marinov
 */
public class MessengerContact
{
    /**
     * The <tt>Logger</tt> used by the <tt>MessengerContact</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MessengerContact.class);

    /**
     * The sign-in name associated with the native <tt>IMessengerContact</tt>
     * implementation represented by this instance.
     */
    public final String signinName;

    /**
     * Initializes a new <tt>MessengerContact</tt> instance which is to
     * represent the Java counterpart of a native <tt>IMessengerContact</tt>
     * implementation associated with a specific sign-in name.
     *
     * @param signinName the sign-in name associated with the native
     * <tt>IMessengerContact</tt> implementation which is to be represented by
     * the new instance
     */
    public MessengerContact(String signinName)
    {
        this.signinName = signinName;
    }

    /**
     * Gets the phone number information of the contact associated with this
     * instance.
     *
     * @param a member of the <tt>MPHONE_TYPE</tt> enumerated type which
     * specifies the type of the phone number information to be retrieved
     * @return the phone number information of the contact associated with this
     * instance
     */
    public String getPhoneNumber(int type)
    {
        try
        {
            return Messenger.getPhoneNumber(this, type);
        }
        catch (Throwable t)
        {
            /*
             * The native counterpart will swallow any exception. Even if it
             * didn't, it would still not use a Logger instance to describe the
             * exception. So describe it on the Java side and rethrow it.
             */
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else if (t instanceof OutOfMemoryError)
                throw (OutOfMemoryError) t;
            else
            {
                logger.error(
                        "Failed to retrieve the phone number information of an"
                            + " IMessengerContact with sign-in name: "
                            + signinName,
                        t);
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * Gets the connection/presence status of the contact associated with this
     * instance in the form of a <tt>MISTATUS</tt> value.
     *
     * @return a <tt>MISTATUS</tt> value which specifies the connection/presence
     * status of the contact associated with this instance
     */
    public int getStatus()
    {
        try
        {
            return Messenger.getStatus(this);
        }
        catch (Throwable t)
        {
            /*
             * The native counterpart will swallow any exception. Even if it
             * didn't, it would still not use a Logger instance to describe the
             * exception. So describe it on the Java side and rethrow it.
             */
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else if (t instanceof OutOfMemoryError)
                throw (OutOfMemoryError) t;
            else
            {
                logger.error(
                        "Failed to determine the status of an IMessengerContact"
                            + " with sign-in name: "
                            + signinName,
                        t);
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * Gets the indicator which determines whether this
     * <tt>MessengerContact</tt> is the same user as the current client user.
     *
     * @return <tt>true</tt> if this <tt>MessengerContact</tt> is the same user
     * as the current client user; otherwise, <tt>false</tt>
     */
    public boolean isSelf()
    {
        return Messenger.isSelf(this);
    }
}
