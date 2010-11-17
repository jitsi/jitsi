/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>UIContactDetail</tt> corresponds to a particular contact detail,
 * phone number, IM identifier, email, etc. which has it's preferred mode of
 * transport <tt>ProtocolProviderService</tt>.
 *
 * @author Yana Stamcheva
 */
public abstract class UIContactDetail
{
    /**
     * The address of this detail.
     */
    private final String address;

    /**
     * The display name of this detail.
     */
    private final String displayName;

    /**
     * The status icon of this contact detail.
     */
    private final ImageIcon statusIcon;

    /**
     * The <tt>ProtocolProviderService</tt> corresponding to this detail.
     */
    private final ProtocolProviderService protocolProvider;

    /**
     * The protocol to be used for this contact detail if no protocol provider
     * is set.
     */
    private final String preferredProtocol;

    /**
     * Creates a <tt>UIContactDetail</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     * @param address the contact address
     * @param displayName the contact display name
     * @param statusIcon the status icon of this contact detail
     * @param preferredProvider the preferred protocol provider
     * @param preferredProtocol the preferred protocol if no protocol provider
     * is set
     */
    public UIContactDetail(
        String address,
        String displayName,
        ImageIcon statusIcon,
        ProtocolProviderService preferredProvider,
        String preferredProtocol)
    {
        this.address = address;
        this.displayName = displayName;
        this.statusIcon = statusIcon;
        this.protocolProvider = preferredProvider;
        this.preferredProtocol = preferredProtocol;
    }

    /**
     * Returns the display name of this detail.
     * @return the display name of this detail
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the address of this detail.
     * @return the address of this detail
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Returns the status icon of this contact detail.
     *
     * @return the status icon of this contact detail
     */
    public ImageIcon getStatusIcon()
    {
        return statusIcon;
    }

    /**
     * Returns the protocol provider preferred for contacting this detail for
     * the given <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class for which we're looking
     * for provider
     * @return the protocol provider preferred for contacting this detail
     */
    public ProtocolProviderService getPreferredProtocolProvider(
        Class<? extends OperationSet> opSetClass)
    {
        return protocolProvider;
    }

    /**
     * Returns the name of the protocol preferred for contacting this detail for
     * the given <tt>OperationSet</tt> class if no preferred protocol provider
     * is set.
     * @param opSetClass the <tt>OperationSet</tt> class for which we're looking
     * for protocol
     * @return the name of the protocol preferred for contacting this detail
     */
    public String getPreferredProtocol(Class<? extends OperationSet> opSetClass)
    {
        return preferredProtocol;
    }
    /**
     * Returns the <tt>PresenceStatus</tt> of this <tt>ContactDetail</tt> or
     * null if the detail doesn't support presence.
     * @return the <tt>PresenceStatus</tt> of this <tt>ContactDetail</tt> or
     * null if the detail doesn't support presence
     */
    public abstract PresenceStatus getPresenceStatus();
}