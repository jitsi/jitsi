/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>AdHocConferenceChatContact</tt> represents a <tt>ChatContact</tt> in
 * an ad-hoc conference chat.
 *
 * @author Valentin Martinet
 * @author Lubomir Marinov
 */
public class AdHocConferenceChatContact
    extends ChatContact<Contact>
{

    /**
     * Creates an instance of <tt>AdHocConferenceChatContact</tt> by passing to 
     * it the <tt>Contact</tt> for which it is created.
     *
     * @param participant the <tt>Contact</tt> for which this
     * <tt>AdHocConferenceChatContact</tt> is created.
     */
    public AdHocConferenceChatContact(Contact participant)
    {
        super(participant);
    }

    protected byte[] getAvatarBytes()
    {
        return descriptor.getImage();
    }

    /**
     * Returns the contact name.
     *
     * @return the contact name
     */
    public String getName()
    {
        String name = descriptor.getDisplayName();

        if (name == null || name.length() < 1)
            name = GuiActivator.getResources().getI18NString(
                    "service.gui.UNKNOWN");

        return name;
    }

    /*
     * Implements ChatContact#getUID(). Delegates to
     * Contact#getAddress() because it's supposed to be unique.
     */
    public String getUID()
    {
        return descriptor.getAddress();
    }
}
