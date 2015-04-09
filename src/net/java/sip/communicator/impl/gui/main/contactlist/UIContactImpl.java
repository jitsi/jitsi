/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>UIContactImpl</tt> class extends the <tt>UIContact</tt> in order to
 * add some more methods specific the UI implementation.
 *
 * @author Yana Stamcheva
 */
public abstract class UIContactImpl
    extends UIContact
{
    /**
     * Whether we should filter all addresses shown in tooltips
     * and to remove the domain part.
     */
    private static final String FILTER_DOMAIN_IN_TIP_ADDRESSES
        = "net.java.sip.communicator.impl.gui.main.contactlist" +
            ".FILTER_DOMAIN_IN_TIP_ADDRESSES";

    /**
     * Returns the corresponding <tt>ContactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @return the corresponding <tt>ContactNode</tt>
     */
    public abstract ContactNode getContactNode();

    /**
     * Sets the given <tt>contactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @param contactNode the <tt>ContactNode</tt> that corresponds to this
     * <tt>UIGroup</tt>
     */
    public abstract void setContactNode(ContactNode contactNode);

    /**
     * Returns the general status icon of the given UIContact.
     *
     * @return PresenceStatus the most "available" status from all
     * sub-contact statuses.
     */
    public abstract ImageIcon getStatusIcon();

    /**
     * Gets the avatar of a specific <tt>UIContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @param isSelected indicates if the contact is selected
     * @param width the desired icon width
     * @param height the desired icon height
     * @return an <tt>ImageIcon</tt> which represents the avatar of the
     * specified <tt>MetaContact</tt>
     */
    public abstract ImageIcon getScaledAvatar(
        boolean isSelected, int width, int height);

    /**
     * Gets the avatar of a specific <tt>UIContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @return a byte array representing the avatar of this <tt>UIContact</tt>
     */
    public byte[] getAvatar()
    {
        return null;
    }

    /**
     * Returns the display name of this <tt>UIContact</tt>.
     *
     * @return the display name of this <tt>UIContact</tt>
     */
    @Override
    public abstract String getDisplayName();

    /**
     * Filter address display if enabled will remove domain part of the
     * addresses to show.
     *
     * @param addressToDisplay the address to change
     * @return if enabled the address with removed domain part
     */
    protected String filterAddressDisplay(String addressToDisplay)
    {
        if(!GuiActivator.getConfigurationService()
            .getBoolean(FILTER_DOMAIN_IN_TIP_ADDRESSES, false))
            return addressToDisplay;

        int ix = addressToDisplay.indexOf("@");
        int typeIx = addressToDisplay.indexOf("(");

        if(ix != -1)
        {
            if(typeIx != -1)
                addressToDisplay =
                    addressToDisplay.substring(0, ix)
                    + " "
                    + addressToDisplay.substring(
                            typeIx, addressToDisplay.length());
            else
                addressToDisplay = addressToDisplay.substring(0, ix);
        }
        return addressToDisplay;
    }
}
