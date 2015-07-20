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
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;

/**
 * The <tt>MetaUIContact</tt> is the implementation of the UIContact interface
 * for the <tt>MetaContactListService</tt>. This implementation is based on the
 * <tt>MetaContact</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaUIContact
    extends UIContactImpl
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
    @Override
    public Object getDescriptor()
    {
        return metaContact;
    }

    /**
     * Returns the display name of this <tt>MetaUIContact</tt>.
     * @return the display name of this <tt>MetaUIContact</tt>
     */
    @Override
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
    @Override
    public int getSourceIndex()
    {
        MetaContactGroup parentMetaContactGroup =
                metaContact.getParentMetaContactGroup();
        int groupSourceIndex = 0;
        if (parentMetaContactGroup == null)
            return -1;
        MetaContactGroup parentGroup 
            = parentMetaContactGroup.getParentMetaContactGroup();
       
        if(parentGroup != null)
            groupSourceIndex = parentGroup.indexOf(parentMetaContactGroup) 
                * UIGroup.MAX_CONTACTS;
        return GuiActivator.getContactList()
                .getMetaContactListSource().getIndex()
            * UIGroup.MAX_GROUPS + groupSourceIndex +
            parentMetaContactGroup.indexOf(metaContact) + 1;
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of strings, which can be used
     * to find this contact.
     * @return an <tt>Iterator</tt> over a list of search strings
     */
    @Override
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
    @Override
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
    @Override
    public UIGroup getParentGroup()
    {
        return parentUIGroup;
    }

    /**
     * Sets the given <tt>parentGroup</tt> to be the parent <tt>UIGroup</tt>
     * of this <tt>MetaUIContact</tt>.
     * @param parentGroup the parent <tt>UIGroup</tt> to set
     */
    @Override
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
    @Override
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
    @Override
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
     * Returns a list of all <tt>UIContactDetail</tt>s within this
     * <tt>UIContact</tt>.
     *
     * @return a list of all <tt>UIContactDetail</tt>s within this
     * <tt>UIContact</tt>
     */
    @Override
    public List<UIContactDetail> getContactDetails()
    {
        List<UIContactDetail> resultList
            = new LinkedList<UIContactDetail>();

        Iterator<Contact> contacts = metaContact.getContacts();

        while (contacts.hasNext())
        {
            resultList.add(new MetaContactDetail(contacts.next()));
        }
        return resultList;
    }

    /**
     * Gets the avatar of a specific <tt>UIContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @return a byte array representing the avatar of this <tt>UIContact</tt>
     */
    @Override
    public byte[] getAvatar()
    {
        return metaContact.getAvatar();
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
    @Override
    public ImageIcon getScaledAvatar(
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
    @Override
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
    @Override
    public ExtendedTooltip getToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(true);

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

        MetaContactPhoneUtil contactPhoneUtil =
            MetaContactPhoneUtil.getPhoneUtil(metaContact);

        String statusMessage = null;
        Contact protocolContact;
        boolean isLoading = false;
        while (i.hasNext())
        {
            protocolContact = i.next();

            // Set the first found status message.
            if (statusMessage == null)
            {
                statusMessage = protocolContact.getStatusMessage();
                if ((statusMessage != null) && (statusMessage.length() == 0))
                    statusMessage = null;
            }

            if(ConfigurationUtils.isHideAccountStatusSelectorsEnabled())
                break;

            ImageIcon protocolStatusIcon
                = getContactPresenceStatusIcon(protocolContact);

            if (protocolStatusIcon != null)
            {
                protocolStatusIcon
                    = ImageLoader.getIndexedProtocolIcon(
                            protocolStatusIcon.getImage(),
                            protocolContact.getProtocolProvider());
            }

            String contactAddress = protocolContact.getAddress();
            //String statusMessage = protocolContact.getStatusMessage();

            tip.addLine(
                protocolStatusIcon,
                filterAddressDisplay(contactAddress));

            addContactResourceTooltipLines(tip, protocolContact);

            if(!protocolContact.getProtocolProvider().isRegistered())
                continue;

            List<String> phones = contactPhoneUtil
                    .getPhones( protocolContact,
                        new OperationSetServerStoredContactInfo
                                .DetailsResponseListener()
                        {
                            public void detailsRetrieved(
                                final Iterator<GenericDetail> details)
                            {
                                if(!SwingUtilities.isEventDispatchThread())
                                {
                                    SwingUtilities.invokeLater(new Runnable()
                                    {
                                        public void run()
                                        {
                                            detailsRetrieved(details);
                                        }
                                    });
                                    return;
                                }

                                // remove previously shown information
                                // as it contains "Loading..." text
                                tip.removeAllLines();

                                // load it again
                                loadTooltip(tip);
                            }
                        }, true);

            if(phones != null)
            {
                addPhoneTooltipLines(tip, phones.iterator());
            }
            else
                isLoading = true;
        }

        if(isLoading)
            tip.addLine(null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.LOADING"));

        if (statusMessage != null)
            tip.setBottomText(statusMessage);
    }

    /**
     * Adds resources for contact.
     *
     * @param tip the tool tip
     * @param protocolContact the protocol contact, which resources we're
     * looking for
     */
    private void addContactResourceTooltipLines(
        ExtendedTooltip tip,
        Contact protocolContact)
    {
        Collection<ContactResource> contactResources
            = protocolContact.getResources();

        if (contactResources == null)
            return;

        Iterator<ContactResource> resourcesIter = contactResources.iterator();

        while (resourcesIter.hasNext())
        {
            ContactResource contactResource = resourcesIter.next();

            // We only add the status icon if we have more than one resources,
            // otherwise it will always be identical to the contact status icon.
            ImageIcon protocolStatusIcon = null;
            if (contactResources.size() > 1)
            {
                protocolStatusIcon
                    = ImageLoader.getIndexedProtocolIcon(
                        ImageUtils.getBytesInImage(
                            contactResource.getPresenceStatus().getStatusIcon()),
                            protocolContact.getProtocolProvider());
            }

            String resourceName = (contactResource.getPriority() >= 0)
                                    ? contactResource.getResourceName()
                                    + " (" + contactResource.getPriority() + ")"
                                    : contactResource.getResourceName();

            if(contactResource.isMobile())
            {
                resourceName += " " + GuiActivator.getResources()
                    .getI18NString("service.gui.ON_MOBILE_TOOLTIP");
            }

            if (protocolStatusIcon == null)
                tip.addSubLine( protocolStatusIcon,
                                resourceName,
                                27);
            else
                tip.addSubLine( protocolStatusIcon,
                                resourceName,
                                20);
        }

        tip.revalidate();
        tip.repaint();
    }

    /**
     * Fills the tooltip with details.
     * @param tip the tooltip to fill
     * @param phones the available phone details.
     */
    private void addPhoneTooltipLines(ExtendedTooltip tip,
                                      Iterator<String> phones)
    {
        while(phones.hasNext())
        {
            tip.addSubLine(null, filterAddressDisplay(phones.next()), 27);
        }

        tip.revalidate();
        tip.repaint();
    }

    /**
     * Returns the corresponding <tt>ContactNode</tt> in the contact list
     * component data model.
     * @return the corresponding <tt>ContactNode</tt>
     */
    @Override
    public ContactNode getContactNode()
    {
        return contactNode;
    }

    /**
     * Sets the corresponding <tt>ContactNode</tt>.
     * @param contactNode the corresponding <tt>ContactNode</tt> in the contact
     * list component data model
     */
    @Override
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
    private class MetaContactDetail
        extends UIContactDetailImpl
    {
        /**
         * The underlying protocol contact.
         */
        private Contact contact;

        /**
         * Creates an instance of <tt>MetaContactDetail</tt> by specifying the
         * underlying protocol <tt>Contact</tt>.
         *
         * @param contact the protocol contact, on which this implementation
         * is based
         */
        public MetaContactDetail(Contact contact)
        {
            super(  contact.getAddress(),
                    contact.getDisplayName(),
                    getContactPresenceStatusIcon(contact),
                    contact);

            this.contact = contact;

            ProtocolProviderService parentProvider
                = contact.getProtocolProvider();
            Iterator<Class<? extends OperationSet>> opSetClasses
                = parentProvider.getSupportedOperationSetClasses().iterator();

            while (opSetClasses.hasNext())
            {
                Class<? extends OperationSet> opSetClass = opSetClasses.next();

                addPreferredProtocolProvider(opSetClass, parentProvider);
                addPreferredProtocol(opSetClass,
                                    parentProvider.getProtocolName());
            }
        }

        /**
         * Returns the presence status of the underlying protocol
         * <tt>Contact</tt>.
         * @return the presence status of the underlying protocol
         * <tt>Contact</tt>
         */
        @Override
        public PresenceStatus getPresenceStatus()
        {
            return contact.getPresenceStatus();
        }
    }

    /**
     * Returns the right button menu component.
     * @return the right button menu component
     */
    @Override
    public JPopupMenu getRightButtonMenu()
    {
        return new MetaContactRightButtonMenu(metaContact);
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    @Override
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        return MetaContactListSource.getContactCustomActionButtons(this);
    }

    private static ImageIcon getContactPresenceStatusIcon(Contact contact)
    {
        byte[] bytes = contact.getPresenceStatus().getStatusIcon();

        return (bytes == null) ? null : new ImageIcon(bytes);
    }
}
