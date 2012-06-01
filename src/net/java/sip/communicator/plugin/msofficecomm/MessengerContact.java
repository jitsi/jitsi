/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msofficecomm;

/**
 * Represents the Java counterpart of a native <tt>IMessengerContact</tt>
 * implementation.
 *
 * @author Lyubomir Marinov
 */
public class MessengerContact
{
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
     * Gets the connection/presence status of the contact associated with this
     * instance in the form of a <tt>MISTATUS</tt> value.
     *
     * @return a <tt>MISTATUS</tt> value which specifies the connection/presence
     * status of the contact associated with this instance
     */
    public int getStatus()
    {
        return Messenger.getStatus(this);
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
