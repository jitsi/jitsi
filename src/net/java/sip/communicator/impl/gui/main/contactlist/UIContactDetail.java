/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

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
     * The prefix to be used when calling this contact detail.
     */
    private String prefix;

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
     * The collection of labels associated with this detail.
     */
    private final Collection<String> labels;

    /**
     * The category of the underlying contact detail.
     */
    private final String category;

    /**
     * Creates a <tt>UIContactDetail</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     * @param address the contact address
     * @param displayName the contact display name
     * @param category the category of the underlying contact detail
     * @param labels the collection of labels associated with this detail
     * @param statusIcon the status icon of this contact detail
     * @param preferredProvider the preferred protocol provider
     * @param preferredProtocol the preferred protocol if no protocol provider
     * is set
     */
    public UIContactDetail(
        String address,
        String displayName,
        String category,
        Collection<String> labels,
        ImageIcon statusIcon,
        ProtocolProviderService preferredProvider,
        String preferredProtocol)
    {
        this.address = address;
        this.displayName = displayName;
        this.category = category;
        this.labels = labels;
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
        if (prefix != null && prefix.trim().length() >= 0)
            return prefix + address;

        return address;
    }

    /**
     * Returns the category of the underlying detail.
     *
     * @return the category of the underlying detail
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Returns an iterator over the collection of labels associated with this
     * detail.
     *
     * @return an iterator over the collection of labels associated with this
     * detail
     */
    public Iterator<String> getLabels()
    {
        if (labels != null)
            return labels.iterator();

        return null;
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
     * Returns the prefix to be used when calling this contact detail.
     *
     * @return the prefix to be used when calling this contact detail
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * Sets the prefix to be used when calling this contact detail.
     *
     * @param the prefix to be used when calling this contact detail
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Returns the <tt>PresenceStatus</tt> of this <tt>ContactDetail</tt> or
     * null if the detail doesn't support presence.
     * @return the <tt>PresenceStatus</tt> of this <tt>ContactDetail</tt> or
     * null if the detail doesn't support presence
     */
    public abstract PresenceStatus getPresenceStatus();
}