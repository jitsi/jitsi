/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.SubscriptionStatus;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MetaUIContact</tt> is the implementation of the UIContact interface
 * for the <tt>MetaContactListService</tt>. This implementation is based on the
 * <tt>MetaContact</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaUIContact
    implements UIContact
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
     * The subscription status of this meta contact. It will be turned to true
     * when all the contact details are checked.
     */
    boolean subscribed = false;

    /**
     * Creates an instance of <tt>MetaUIContact</tt> by specifying the
     * underlying <tt>MetaContact</tt>, on which it's based.
     * @param metaContact the <tt>MetaContact</tt>, on which this implementation
     * is based
     */
    public MetaUIContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;

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
        String displayName = metaContact.getDisplayName();

        /*
         * If the MetaContact doesn't tell us a display name, make up a display
         * name so that we don't end up with "Unknown user".
         */
        if ((displayName == null) || (displayName.trim().length() == 0))
        {
            /*
             * Try to get a display name from one of the Contacts of the
             * MetaContact. If that doesn't cut it, use the address of a
             * Contact. Because it's not really clear which address to display
             * when there are multiple Contacts, use the address only when
             * there's a single Contact in the MetaContact.
             */
            Iterator<Contact> contactIter = metaContact.getContacts();
            int contactCount = 0;
            String address = null;

            while (contactIter.hasNext())
            {
                Contact contact = contactIter.next();

                contactCount++;

                displayName = contact.getDisplayName();
                if ((displayName == null) || (displayName.trim().length() == 0))
                {
                    /*
                     * As said earlier, only use an address if there's a single
                     * Contact in the MetaContact.
                     */
                    address = (contactCount == 1) ? contact.getAddress() : null;
                }
                else
                    break;
            }
            if ((address != null)
                    && (address.trim().length() != 0)
                    && ((displayName == null)
                            || (displayName.trim().length() == 0)))
                displayName = address;
        }
        return displayName;
    }

    /**
     * Returns the index of the underlying <tt>MetaContact</tt> in its
     * <tt>MetaContactListService</tt> parent group.
     * @return the source index of the underlying <tt>MetaContact</tt>
     */
    public int getSourceIndex()
    {
        MetaContactGroup parentMetaContactGroup =
                metaContact.getParentMetaContactGroup();
        if (parentMetaContactGroup == null)
            return -1;
        return parentMetaContactGroup.indexOf(metaContact);
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
     * Sets the given <tt>parentGroup</tt> to be the parent <tt>UIGroup</tt>
     * of this <tt>MetaUIContact</tt>.
     * @param parentGroup the parent <tt>UIGroup</tt> to set
     */
    public void setParentGroup(UIGroup parentGroup)
    {
        parentUIGroup = parentGroup;
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

        return (details != null && !details.isEmpty()) ? details.get(0) : null;
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

        // If there's no avatar we have nothing more to do here.
        if((avatarBytes == null) || (avatarBytes.length <= 0))
        {
            if (!subscribed)
            {
                return ImageUtils.getScaledRoundedIcon(
                    ImageLoader.getImage(ImageLoader.UNAUTHORIZED_CONTACT_PHOTO),
                    width, height);
            }

            return null;
        }

        // If the cell is selected we return a zoomed version of the avatar
        // image.
        if (isSelected)
            return ImageUtils.getScaledRoundedIcon(avatarBytes, width, height);

        // In any other case try to get the avatar from the cache.
        Object[] avatarCache
            = (Object[]) metaContact.getData(AVATAR_DATA_KEY);
        ImageIcon avatar = null;

        if ((avatarCache != null) && (avatarCache[0] == avatarBytes))
            avatar = (ImageIcon) avatarCache[1];

        // If the avatar isn't available or it's not up-to-date, create it.
        if (avatar == null)
        {
            avatar = ImageUtils.getScaledRoundedIcon(avatarBytes, width, height);
        }

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
        String displayDetails = null;

        Iterator<Contact> protoContacts = metaContact.getContacts();

        String subscriptionDetails = null;

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetExtendedAuthorizations authOpSet
                = protoContact.getProtocolProvider()
                    .getOperationSet(OperationSetExtendedAuthorizations.class);

            if (authOpSet != null
                && authOpSet.getSubscriptionStatus(protoContact) != null
                && !authOpSet.getSubscriptionStatus(protoContact)
                    .equals(SubscriptionStatus.Subscribed))
            {
                SubscriptionStatus status
                    = authOpSet.getSubscriptionStatus(protoContact);

                if (status.equals(SubscriptionStatus.SubscriptionPending))
                    subscriptionDetails = GuiActivator.getResources()
                        .getI18NString("service.gui.WAITING_AUTHORIZATION");
                else if (status.equals(SubscriptionStatus.NotSubscribed))
                    subscriptionDetails = GuiActivator.getResources()
                        .getI18NString("service.gui.NOT_AUTHORIZED");
            }
            else if (protoContact.getStatusMessage() != null
                && protoContact.getStatusMessage().length() > 0)
            {
                subscribed = true;
                displayDetails = protoContact.getStatusMessage();
                break;
            }
            else
            {
                subscribed = true;
            }
        }

        if ((displayDetails == null
            || displayDetails.length() <= 0)
            && !subscribed
            && subscriptionDetails != null
            && subscriptionDetails.length() > 0)
            displayDetails = subscriptionDetails;

        return displayDetails;
    }

    /**
     * Returns the tool tip opened on mouse over.
     * @return the tool tip opened on mouse over
     */
    public ExtendedTooltip getToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(
            GuiActivator.getUIService().getMainFrame(), true);

        byte[] avatarImage = metaContact.getAvatar();

        if (avatarImage != null && avatarImage.length > 0)
            tip.setImage(new ImageIcon(avatarImage));

        tip.setTitle(metaContact.getDisplayName());

        loadTooltip(tip);

        return tip;
    }

    /**
     * Loads the tooltip with the data for current metacontact.
     * @param tip the tooltip to fill.
     */
    private void loadTooltip(final ExtendedTooltip tip)
    {
        Iterator<Contact> i = metaContact.getContacts();

        String statusMessage = null;
        Contact protocolContact;
        boolean isLoading = false;
        while (i.hasNext())
        {
            protocolContact = i.next();

            ImageIcon protocolStatusIcon
                = ImageLoader.getIndexedProtocolIcon(
                        ImageUtils.getBytesInImage(
                            protocolContact.getPresenceStatus().getStatusIcon()),
                        protocolContact.getProtocolProvider());

            String contactAddress = protocolContact.getAddress();
            //String statusMessage = protocolContact.getStatusMessage();

            tip.addLine(protocolStatusIcon, contactAddress);
            OperationSetServerStoredContactInfo infoOpSet =
                protocolContact.getProtocolProvider().getOperationSet(
                    OperationSetServerStoredContactInfo.class);

            if(infoOpSet != null
                && protocolContact.getProtocolProvider().isRegistered())
            {
                Iterator<GenericDetail> details =
                    infoOpSet.requestAllDetailsForContact(protocolContact,
                        new OperationSetServerStoredContactInfo
                                .DetailsResponseListener()
                        {
                            public void detailsRetrieved(
                                Iterator<GenericDetail> details)
                            {
                                // remove previously shown information
                                // as it contains "Loading..." text
                                tip.removeAllLines();

                                // load it again
                                loadTooltip(tip);
                            }
                        });

                if(details != null)
                    fillTooltipLines(tip, details);
                else
                    isLoading = true;
            }

            // Set the first found status message.
            if (statusMessage == null
                && protocolContact.getStatusMessage() != null
                && protocolContact.getStatusMessage().length() > 0)
                statusMessage = protocolContact.getStatusMessage();
        }

        if(isLoading)
            tip.addLine(null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.LOADING"));

        if (statusMessage != null)
            tip.setBottomText(statusMessage);
    }

    /**
     * Fills the tooltip with details.
     * @param tip the tooltip to fill
     * @param details the available details.
     */
    private void fillTooltipLines(ExtendedTooltip tip,
                                  Iterator<GenericDetail> details)
    {
        while(details.hasNext())
        {
            GenericDetail d = details.next();
            if(d instanceof PhoneNumberDetail &&
                !(d instanceof FaxDetail) &&
                !(d instanceof PagerDetail))
            {
                PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                if(pnd.getNumber() != null &&
                    pnd.getNumber().length() > 0)
                {
                    String localizedType = null;

                    if(d instanceof WorkPhoneDetail)
                    {
                        localizedType =
                            GuiActivator.getResources().
                                getI18NString(
                                    "service.gui.WORK_PHONE");
                    }
                    else if(d instanceof MobilePhoneDetail)
                    {
                        localizedType =
                            GuiActivator.getResources().
                                getI18NString(
                                    "service.gui.MOBILE_PHONE");
                    }
                    else
                    {
                        localizedType =
                            GuiActivator.getResources().
                                getI18NString(
                                    "service.gui.PHONE");
                    }

                    tip.addLine(null, (pnd.getNumber() +
                        " (" + localizedType + ")"));
                }
             }
        }

        tip.revalidate();
        tip.repaint();
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
                    null,
                    null,
                    new ImageIcon(
                        contact.getPresenceStatus().getStatusIcon()),
                    contact.getProtocolProvider(),
                    contact.getProtocolProvider().getProtocolName());

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
