/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaUIContact</tt> is the implementation of the UIContact interface
 * for the <tt>MetaContactListService</tt>. This implementation is based on the
 * <tt>MetaContact</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaUIContact
    implements  UIContact
{
    /**
     * The key of the user data in <tt>MetaContact</tt> which specifies
     * the avatar cached from previous invocations.
     */
    private static final String AVATAR_DATA_KEY
        = MetaUIContact.class.getName() + ".avatar";

    /**
     * A list of all search strings available for the underlying
     * <tt>MetaContact</tt>.
     */
    private final List<String> searchStrings = new LinkedList<String>();

    /**
     * The <tt>MetaContact</tt>, on which this implementation is based.
     */
    private MetaContact metaContact;

    /**
     * The corresponding <tt>ContactNode</tt> in the contact list component
     * data model.
     */
    private ContactNode contactNode;

    /**
     * The parent <tt>UIGroup</tt> of this contact.
     */
    private UIGroup parentUIGroup;

    /**
     * Creates an instance of <tt>MetaUIContact</tt> by specifying the
     * underlying <tt>MetaContact</tt>, on which it's based.
     * @param metaContact the <tt>MetaContact</tt>, on which this implementation
     * is based
     * @param group the parent <tt>UIGroup</tt>
     */
    public MetaUIContact(MetaContact metaContact, UIGroup group)
    {
        this.metaContact = metaContact;
        this.parentUIGroup = group;

        initSearchStrings();
    }

    /**
     * Returns the underlying <tt>MetaContact</tt>.
     * @return the underlying <tt>MetaContact</tt>
     */
    public Object getDescriptor()
    {
        return metaContact;
    }

    /**
     * Returns the display name of this <tt>MetaUIContact</tt>.
     * @return the display name of this <tt>MetaUIContact</tt>
     */
    public String getDisplayName()
    {
        return metaContact.getDisplayName();
    }

    /**
     * Returns the index of the underlying <tt>MetaContact</tt> in its
     * <tt>MetaContactListService</tt> parent group.
     * @return the source index of the underlying <tt>MetaContact</tt>
     */
    public int getSourceIndex()
    {
        return metaContact.getParentMetaContactGroup().indexOf(metaContact);
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of strings, which can be used
     * to find this contact.
     * @return an <tt>Iterator</tt> over a list of search strings
     */
    public Iterator<String> getSearchStrings()
    {
        return searchStrings.iterator();
    }

    /**
     * Returns the general status icon of the given MetaContact. Detects the
     * status using the priority status table. The priority is defined on
     * the "availability" factor and here the most "available" status is
     * returned.
     *
     * @return PresenceStatus The most "available" status from all
     * sub-contact statuses.
     */
    public ImageIcon getStatusIcon()
    {
        PresenceStatus status = null;
        Iterator<Contact> i = metaContact.getContacts();
        while (i.hasNext()) {
            Contact protoContact = i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (status == null)
                status = contactStatus;
            else
                status = (contactStatus.compareTo(status) > 0)
                        ? contactStatus
                        : status;
        }

        if (status != null)
            return new ImageIcon(Constants.getStatusIcon(status));

        return null;
    }

    /**
     * Returns the parent <tt>UIGroup</tt>.
     * @return the parent <tt>UIGroup</tt>
     */
    public UIGroup getParentGroup()
    {
        return parentUIGroup;
    }

    /**
     * Returns the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class
     */
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> details
            = getContactDetailsForOperationSet(opSetClass);

        if (details != null && !details.isEmpty())
            return details.get(0);

        return null;
    }

    /**
     * Returns a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> resultList
            = new LinkedList<UIContactDetail>();

        Iterator<Contact> contacts
            = metaContact.getContactsForOperationSet(opSetClass).iterator();

        while (contacts.hasNext())
        {
            resultList.add(new MetaContactDetail(contacts.next()));
        }
        return resultList;
    }

    /**
     * Gets the avatar of a specific <tt>MetaContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     * 
     * @param isSelected indicates if the contact is selected
     * @param width the desired icon width
     * @param height the desired icon height
     * @return an <tt>ImageIcon</tt> which represents the avatar of the
     * specified <tt>MetaContact</tt>
     */
    public ImageIcon getAvatar(
        boolean isSelected, int width, int height)
    {
        byte[] avatarBytes = metaContact.getAvatar(true);
        ImageIcon avatar = null;

        // If there'rs no avatar we have nothing more to do here.
        if((avatarBytes == null) || (avatarBytes.length <= 0))
            return null;

        // If the cell is selected we return a zoomed version of the avatar
        // image.
        if (isSelected)
            return ImageUtils.getScaledRoundedIcon(
                avatarBytes,
                width,
                height);

        // In any other case try to get the avatar from the cache.
        Object[] avatarCache
            = (Object[]) metaContact.getData(AVATAR_DATA_KEY);

        if ((avatarCache != null) && (avatarCache[0] == avatarBytes)) 
            avatar = (ImageIcon) avatarCache[1];

        // Just 
        int avatarWidth = width;
        int avatarHeight = height;

        // If the avatar isn't available or it's not up-to-date, create it.
        if (avatar == null)
            avatar = ImageUtils.getScaledRoundedIcon(
                        avatarBytes,
                        avatarWidth,
                        avatarHeight);

        // Cache the avatar in case it has changed.
        if (avatarCache == null)
        {
            if (avatar != null)
                metaContact.setData(
                    AVATAR_DATA_KEY,
                    new Object[] { avatarBytes, avatar });
        }
        else
        {
            avatarCache[0] = avatarBytes;
            avatarCache[1] = avatar;
        }

        return avatar;
    }

    /**
     * Returns the display details for the underlying <tt>MetaContact</tt>.
     * @return the display details for the underlying <tt>MetaContact</tt>
     */
    public String getDisplayDetails()
    {
        String statusMessage = null;
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            statusMessage = protoContact.getStatusMessage();
            if (statusMessage != null && statusMessage.length() > 0)
                break;
        }

        return statusMessage;
    }

    /**
     * Returns the tool tip opened on mouse over.
     * @return the tool tip opened on mouse over
     */
    public ExtendedTooltip getToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(true);

        byte[] avatarImage = metaContact.getAvatar();

        if (avatarImage != null && avatarImage.length > 0)
            tip.setImage(new ImageIcon(avatarImage));

        tip.setTitle(metaContact.getDisplayName());

        Iterator<Contact> i = metaContact.getContacts();

        String statusMessage = null;
        Contact protocolContact;
        while (i.hasNext())
        {
            protocolContact = i.next();

            ImageIcon protocolStatusIcon
                = new ImageIcon(
                    protocolContact.getPresenceStatus().getStatusIcon());

            String contactAddress = protocolContact.getAddress();
            //String statusMessage = protocolContact.getStatusMessage();

            tip.addLine(protocolStatusIcon, contactAddress);

            // Set the first found status message.
            if (statusMessage == null
                && protocolContact.getStatusMessage() != null
                && protocolContact.getStatusMessage().length() > 0)
                statusMessage = protocolContact.getStatusMessage();
        }

        if (statusMessage != null)
            tip.setBottomText(statusMessage);

        return tip;
    }

    /**
     * Returns the corresponding <tt>ContactNode</tt> in the contact list
     * component data model.
     * @return the corresponding <tt>ContactNode</tt>
     */
    public ContactNode getContactNode()
    {
        return contactNode;
    }

    /**
     * Sets the corresponding <tt>ContactNode</tt>.
     * @param contactNode the corresponding <tt>ContactNode</tt> in the contact
     * list component data model
     */
    public void setContactNode(ContactNode contactNode)
    {
        this.contactNode = contactNode;
        if (contactNode == null)
            MetaContactListSource.removeUIContact(metaContact);
    }

    /**
     * Initializes all search strings for this <tt>MetaUIGroup</tt>.
     */
    private void initSearchStrings()
    {
        searchStrings.add(metaContact.getDisplayName());

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            searchStrings.add(contact.getDisplayName());
            searchStrings.add(contact.getAddress());
        }
    }

    /**
     * The implementation of the <tt>UIContactDetail</tt> interface for the
     * <tt>MetaContactListService</tt>.
     */
    private class MetaContactDetail extends UIContactDetail
    {
        /**
         * The underlying protocol contact.
         */
        private Contact contact;

        /**
         * Creates an instance of <tt>MetaContactDetail</tt> by specifying the
         * underlying protocol <tt>Contact</tt>.
         * @param contact the protocol contact, on which this implementation
         * is based
         */
        public MetaContactDetail(Contact contact)
        {
            super(  contact.getAddress(),
                    contact.getDisplayName(),
                    contact.getProtocolProvider());

            this.contact = contact;
        }

        /**
         * Returns the presence status of the underlying protocol
         * <tt>Contact</tt>.
         * @return the presence status of the underlying protocol
         * <tt>Contact</tt>
         */
        public PresenceStatus getPresenceStatus()
        {
            return contact.getPresenceStatus();
        }
    }

    /**
     * Returns the right button menu component.
     * @return the right button menu component
     */
    public JPopupMenu getRightButtonMenu()
    {
        return new MetaContactRightButtonMenu(metaContact);
    }
}