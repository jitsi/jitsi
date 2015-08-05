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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.authorization.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.SubscriptionStatus;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;
import net.java.sip.communicator.util.skin.*;

import org.osgi.framework.*;

/**
 * The ContactRightButtonMenu is the menu, opened when user clicks with the
 * user could add a subcontact, remove a contact, send message, etc.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class MetaContactRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                ContactListListener,
                Skinnable
{
    /**
     * An eclipse generated serial version unique ID
     */
    private static final long serialVersionUID = 3033031652970285857L;

    /**
     * The logger of this class.
     */
    private final Logger logger
        = Logger.getLogger(MetaContactRightButtonMenu.class);

    /**
     * The string shown over menu items indicating that an operation should be
     * done for all contained contacts.
     */
    private static final String allContactsString
        = GuiActivator.getResources().getI18NString("service.gui.ALL_CONTACTS");

    /**
     * String for call menu items.
     */
    private static final String callString
        = GuiActivator.getResources().getI18NString("service.gui.CALL");

    /**
     * String for remove contact menu items.
     */
    private static final String removeString = GuiActivator
        .getResources().getI18NString("service.gui.REMOVE_CONTACT");

    /**
     * The menu responsible for moving a contact to another group.
     */
    private final SIPCommMenu moveToMenu
        = new SIPCommMenu(GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_TO_GROUP"));

    /**
     * The menu responsible for moving a containing protocol contact to another
     * group.
     */
    private final SIPCommMenu moveSubcontactMenu
        = new SIPCommMenu(GuiActivator.getResources()
            .getI18NString("service.gui.MOVE_SUBCONTACT"));

    /**
     * The menu responsible for removing a contact.
     */
    private final SIPCommMenu removeContactMenu = new SIPCommMenu(removeString);

    /**
     * The menu responsible for calling a contact.
     */
    private final SIPCommMenu callContactMenu = new SIPCommMenu(callString);

    /**
     * The menu responsible for adding a contact.
     */
    private final JMenuItem addContactItem = new JMenuItem();

    /**
     * The menu item responsible for calling a contact.
     */
    private final JMenuItem callItem = new JMenuItem(callString);

    /**
     * The video call menu item.
     */
    private final JMenuItem videoCallItem = new JMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.VIDEO_CALL"));

    /**
     * The menu responsible for calling a contact with video.
     */
    private final SIPCommMenu videoCallMenu = new SIPCommMenu(
        GuiActivator.getResources().getI18NString(
            "service.gui.VIDEO_CALL"));

    /**
     * The menu responsible for full screen sharing when more than one contact
     * is available for sharing.
     */
    private final JMenuItem fullShareMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_FULL_SCREEN"));

    /**
     * The menu responsible for region screen sharing when more than one contact
     * is available for sharing.
     */
    private final JMenuItem regionShareMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_REGION"));

    /**
     * The menu responsible for full screen sharing when more than one contact
     * is available for sharing.
     */
    private final SIPCommMenu multiContactFullShareMenu = new SIPCommMenu(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_FULL_SCREEN"));

    /**
     * The menu responsible for region screen sharing when more than one contact
     * is available for sharing.
     */
    private final SIPCommMenu multiContactRegionShareMenu = new SIPCommMenu(
        GuiActivator.getResources().getI18NString(
            "service.gui.SHARE_REGION"));

    /**
     * The send message menu item.
     */
    private final JMenuItem sendMessageItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SEND_MESSAGE"));

    /**
     * The send file menu item.
     */
    private final JMenuItem sendFileItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SEND_FILE"));

    /**
     * The send SMS menu item.
     */
    private final JMenuItem sendSmsItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SEND_SMS"));

    /**
     * The rename contact menu item.
     */
    private final JMenuItem renameContactItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.RENAME_CONTACT"));

    /**
     * The view history menu item.
     */
    private final JMenuItem viewHistoryItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.VIEW_HISTORY"));

    /**
     * Multi protocol contact authorization request menu.
     */
    private final SIPCommMenu multiContactRequestAuthMenu
        = new SIPCommMenu(GuiActivator.getResources()
            .getI18NString("service.gui.RE_REQUEST_AUTHORIZATION"));

    /**
     * Authorization request menu item.
     */
    private final JMenuItem requestAuthMenuItem
        = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.RE_REQUEST_AUTHORIZATION"));

    /**
     * The <tt>MetaContact</tt> over which the right button was pressed.
     */
    private final MetaContact metaContact;

    /**
     * The prefix for the move menu.
     */
    private static final String moveToPrefix = "moveTo:";

    /**
     * The prefix for remove contact menu.
     */
    private static final String removeContactPrefix = "removeContact:";

    /**
     * The prefix for remove protocol contact menu.
     */
    private static final String moveSubcontactPrefix = "moveSubcontact:";

    /**
     * The prefix for call contact menu.
     */
    private static final String callContactPrefix = "callContact:";

    /**
     * The prefix for call phone menu.
     */
    private static final String callPhonePrefix = "callPhone:";

    /**
     * The prefix for video call contact menu.
     */
    private static final String videoCallPrefix = "videoCall:";

    /**
     * The prefix for full screen desktop sharing menu.
     */
    private static final String fullDesktopSharingPrefix = "shareFullScreen:";

    /**
     * The prefix for region screen desktop sharing menu.
     */
    private static final String regionDesktopSharingPrefix =
        "shareRegionScreen:";

    /**
     * The prefix for full screen desktop sharing menu.
     */
    private static final String requestAuthPrefix = "requestAuth:";

    /**
     * The contact to move when the move menu has been chosen.
     */
    private Contact contactToMove;

    /**
     * Indicates if all contacts should be moved when the move to menu is
     * pressed.
     */
    private boolean moveAllContacts = false;

    /**
     * The move dialog.
     */
    private MoveSubcontactMessageDialog moveDialog;

    /**
     * The main window.
     */
    private final MainFrame mainFrame;

    /**
     * The contact list component.
     */
    private final TreeContactList contactList;

    /**
     * The first unsubscribed contact we found.
     */
    private Contact firstUnsubscribedContact = null;

    /**
     * The phone util we use to check whether to enable/disable buttons.
     */
    private MetaContactPhoneUtil contactPhoneUtil;

    /**
     * Indicates if a separator should be added at the end of the menu
     * initialization.
     */
    private boolean separator = false;

    /**
     * Creates an instance of ContactRightButtonMenu.
     * @param metaContact The MetaContact for which the menu is opened
     */
    public MetaContactRightButtonMenu(  MetaContact metaContact)
    {
        super();

        this.mainFrame = GuiActivator.getUIService().getMainFrame();
        this.contactList = GuiActivator.getContactList();

        this.metaContact = metaContact;

        this.setLocation(getLocation());

        this.init();

        this.initMnemonics();

        loadSkin();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init()
    {
        addContactItem.setText(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT_TO")
                + " " + metaContact.getDisplayName());

        //Initialize moveTo menu.
        Iterator<MetaContactGroup> groups
            = GuiActivator.getContactListService().getRoot().getSubgroups();

        if(groups.hasNext())
        {
            JLabel infoLabel = new JLabel(
                GuiActivator.getResources()
                    .getI18NString("service.gui.SELECT_GROUP"));

            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));

            this.moveToMenu.add(infoLabel);
            this.moveToMenu.addSeparator();
        }

        if(GuiActivator.getContactList().getRootUIGroup() != null)
        {
            // adds contacts group if it is visible
            JMenuItem menuItem = new JMenuItem(
                GuiActivator.getContactList().getRootUIGroup()
                    .getDisplayName());
            menuItem.setName(moveToPrefix
                + GuiActivator.getContactListService().getRoot().getMetaUID());
            menuItem.addActionListener(this);

            this.moveToMenu.add(menuItem);
        }

        while (groups.hasNext())
        {
            MetaContactGroup group = groups.next();

            if (!group.isPersistent())
                continue;

            JMenuItem menuItem = new JMenuItem(group.getGroupName());

            menuItem.setName(moveToPrefix + group.getMetaUID());
            menuItem.addActionListener(this);

            this.moveToMenu.add(menuItem);
        }

        boolean hasOnlyReadonlyContacts = true;
        boolean hasAnyReadonlyContact = false;
        Iterator<Contact> iter = metaContact.getContacts();
        while(iter.hasNext())
        {
            Contact c = iter.next();
            OperationSetPersistentPresencePermissions opsetPermissions =
                c.getProtocolProvider()
                    .getOperationSet(
                        OperationSetPersistentPresencePermissions.class);

            if( opsetPermissions == null
                || !opsetPermissions.isReadOnly(c))
            {
                hasOnlyReadonlyContacts = false;
            }

            if(opsetPermissions != null
                && opsetPermissions.isReadOnly(c))
            {
                hasAnyReadonlyContact = true;
            }
        }

        //Initialize removeContact menu.
        Iterator<Contact> contacts = metaContact.getContacts();

        if (metaContact.getContactCount() > 1)
        {
            Icon deleteIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.DELETE_16x16_ICON));
            JMenuItem allItem = createMenuItem(
                allContactsString,
                removeContactPrefix + "allContacts",
                deleteIcon);
            JMenuItem allItem1 = new JMenuItem(allContactsString);

            allItem1.addActionListener(this);

            allItem1.setName(moveSubcontactPrefix + "allContacts");

            if(!hasAnyReadonlyContact)
            {
                this.removeContactMenu.add(allItem);
                this.moveSubcontactMenu.add(allItem1);
                this.removeContactMenu.addSeparator();
                this.moveSubcontactMenu.addSeparator();
            }
        }

        contactPhoneUtil = MetaContactPhoneUtil.getPhoneUtil(metaContact);

        boolean hasPersistableAddress = false;
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            ProtocolProviderService protocolProvider
                = contact.getProtocolProvider();

            String contactPersistableAddress = contact.getPersistableAddress();
            String contactAddress = contact.getAddress();

            Icon protocolIcon = new ImageIcon(
                    createContactStatusImage(contact));

            boolean isContactReadonly = false;

            OperationSetPersistentPresencePermissions opsetPermissions =
                protocolProvider
                    .getOperationSet(
                        OperationSetPersistentPresencePermissions.class);
            if(opsetPermissions != null
               && opsetPermissions.isReadOnly(contact))
                isContactReadonly = true;

            if(!isContactReadonly)
                this.removeContactMenu.add(
                    new ContactMenuItem(contact,
                                        contactAddress,
                                        removeContactPrefix,
                                        protocolIcon));

            if(contactPersistableAddress != null)
            {
                hasPersistableAddress = true;

                if(!isContactReadonly)
                    this.moveSubcontactMenu.add(
                        new ContactMenuItem(contact,
                                            contactPersistableAddress,
                                            moveSubcontactPrefix,
                                            protocolIcon));
            }

            List<String> phones = contactPhoneUtil.getPhones(contact);

            // add all the contacts that support telephony to the call menu
            if (metaContact.getContactCount() > 1 || phones.size() > 0)
            {
                if (contactPhoneUtil.isCallEnabled(contact))
                {
                    addCallMenuContact(contact, protocolIcon);

                    separator = true;
                }

                if (contactPhoneUtil.isVideoCallEnabled(contact))
                {
                    videoCallMenu.add(
                        new ContactMenuItem(contact,
                                            contactAddress,
                                            videoCallPrefix,
                                            protocolIcon));
                }

                if (contactPhoneUtil.isDesktopSharingEnabled(contact))
                {
                    multiContactFullShareMenu.add(
                        new ContactMenuItem(contact,
                                            contactAddress,
                                            fullDesktopSharingPrefix,
                                            protocolIcon));

                    multiContactRegionShareMenu.add(
                        new ContactMenuItem(contact,
                                            contactAddress,
                                            regionDesktopSharingPrefix,
                                            protocolIcon));
                }

                OperationSetExtendedAuthorizations authOpSet
                    = protocolProvider.getOperationSet(
                        OperationSetExtendedAuthorizations.class);
                
                OperationSetMultiUserChat opSetMUC
                    = protocolProvider.getOperationSet(
                        OperationSetMultiUserChat.class);

                if (authOpSet != null
                    && authOpSet.getSubscriptionStatus(contact) != null
                    && !authOpSet.getSubscriptionStatus(contact)
                        .equals(SubscriptionStatus.Subscribed)
                    && (opSetMUC == null
                        || !opSetMUC.isPrivateMessagingContact(contactAddress)))
                {
                    if (firstUnsubscribedContact == null)
                        firstUnsubscribedContact = contact;

                    multiContactRequestAuthMenu.add(
                        new ContactMenuItem(contact,
                                            contactAddress,
                                            requestAuthPrefix,
                                            protocolIcon));
                }
            }

            addCallMenuPhones(phones);

            addVideoMenuPhones(contact);

            if(separator && contacts.hasNext())
            {
                callContactMenu.addSeparator();
                separator = false;
            }
        }

        // if a separator is the last item, remove it
        Component c = null;

        if(callContactMenu.getMenuComponentCount() > 0)
            c = callContactMenu.getMenuComponent(
                callContactMenu.getMenuComponentCount() - 1);

        if(c != null && (c instanceof JSeparator))
        {
            callContactMenu.remove(c);
        }

        this.add(sendMessageItem);

        if (metaContact.getDefaultContact(
            OperationSetSmsMessaging.class) != null)
        {
            this.add(sendSmsItem);
            sendSmsItem.addActionListener(this);
            sendSmsItem.setName("sendSms");
        }

        if (callContactMenu.getItemCount() > 1)
        {
            this.add(callContactMenu);
        }
        else
        {
            if(callContactMenu.getItemCount() > 0)
            {
                JMenuItem item = callContactMenu.getItem(0);
                this.callItem.setName(item.getName());
            }
            else
            {
                this.callItem.setName("call");
            }

            this.callItem.addActionListener(this);
            this.add(callItem);
        }

        if (videoCallMenu.getItemCount() > 1)
        {
            this.add(videoCallMenu);
        }
        else
        {
            if(videoCallMenu.getItemCount() > 0)
            {
                JMenuItem item = videoCallMenu.getItem(0);
                this.videoCallItem.setName(item.getName());
            }
            else
            {
                this.videoCallItem.setName("videoCall");
            }

            this.videoCallItem.addActionListener(this);
            this.add(videoCallItem);
        }

        if (multiContactFullShareMenu.getItemCount() > 1)
        {
            add(multiContactFullShareMenu);
            add(multiContactRegionShareMenu);
        }
        else
        {
            if(multiContactFullShareMenu.getItemCount() > 0)
            {
                JMenuItem item = multiContactFullShareMenu.getItem(0);
                this.fullShareMenuItem.setName(item.getName());

                JMenuItem ritem = multiContactRegionShareMenu.getItem(0);
                this.regionShareMenuItem.setName(ritem.getName());
            }
            else
            {
                this.fullShareMenuItem.setName("shareFullScreen");
                this.regionShareMenuItem.setName("shareRegion");
            }

            this.fullShareMenuItem.addActionListener(this);
            this.regionShareMenuItem.addActionListener(this);
            this.add(fullShareMenuItem);
            this.add(regionShareMenuItem);
        }

        add(sendFileItem);

        addSeparator();

        if (!ConfigurationUtils.isContactMoveDisabled() &&
            !ConfigurationUtils.isCreateGroupDisabled() &&
            hasPersistableAddress)
        {
            boolean addSeparator = false;

            if(!hasAnyReadonlyContact)
            {
                add(moveToMenu);
                addSeparator = true;
            }

            if(moveSubcontactMenu.getItemCount() > 0)
            {
                add(moveSubcontactMenu);
                addSeparator = true;
            }

            if(addSeparator)
                addSeparator();
        }

        if (!ConfigurationUtils.isAddContactDisabled()
            && !ConfigurationUtils.isMergeContactDisabled()
            && !hasAnyReadonlyContact)
        {
            add(addContactItem);
            addSeparator();
        }

        separator = false;
        if (!ConfigurationUtils.isRemoveContactDisabled())
        {
            if (metaContact.getContactCount() > 1)
            {
                add(removeContactMenu);
                separator = true;
            }
            else if(!hasOnlyReadonlyContacts)
            {
                // There is only one contact, so a submenu is unnecessary -
                // just add a single menu item.  It masquerades as an item to
                // delete all contacts as that way we don't have to specify
                // the contact's address.
                Icon deleteIcon = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.DELETE_16x16_ICON));
                JMenuItem removeContactItem = createMenuItem(
                    removeString,
                    removeContactPrefix + "allContacts",
                    deleteIcon);

                add(removeContactItem);
                separator = true;
            }
        }

        if (!ConfigurationUtils.isContactRenameDisabled())
        {
            add(renameContactItem);
            separator = true;
        }

        if(separator)
        {
            addSeparator();
        }

        add(viewHistoryItem);

        addSeparator();

        Contact defaultContact = metaContact.getDefaultContact();
        int authRequestItemCount = multiContactRequestAuthMenu.getItemCount();
        OperationSetMultiUserChat opSetMUC
            = defaultContact.getProtocolProvider().getOperationSet(
                OperationSetMultiUserChat.class);
        
        // If we have more than one request to make.
        if (authRequestItemCount > 1)
        {
            this.add(multiContactRequestAuthMenu);
        }
        // If we have more than one protocol contacts and only one need
        // authorization or we have only one contact that needs authorization.
        else if (authRequestItemCount == 1
            || (metaContact.getContactCount() == 1
                && defaultContact.getProtocolProvider()
                    .getOperationSet(OperationSetExtendedAuthorizations.class)
                        != null
                && (opSetMUC == null || !opSetMUC.isPrivateMessagingContact(
                        defaultContact.getAddress())))
                && !SubscriptionStatus.Subscribed
                        .equals(defaultContact.getProtocolProvider()
                                    .getOperationSet(
                                        OperationSetExtendedAuthorizations.class)
                                        .getSubscriptionStatus(defaultContact)))
        {
            this.add(requestAuthMenuItem);
            this.requestAuthMenuItem.setName("requestAuth");
            this.requestAuthMenuItem.addActionListener(this);
        }

        initPluginComponents();

        sendMessageItem.setName("sendMessage");
        sendFileItem.setName("sendFile");
        moveToMenu.setName("moveToGroup");
        addContactItem.setName("addContact");
        renameContactItem.setName("renameContact");
        viewHistoryItem.setName("viewHistory");

        sendMessageItem.addActionListener(this);
        sendFileItem.addActionListener(this);
        renameContactItem.addActionListener(this);
        viewHistoryItem.addActionListener(this);
        addContactItem.addActionListener(this);

        // Disable all menu items that do nothing.
        if (metaContact.getDefaultContact(
            OperationSetFileTransfer.class) == null)
            this.sendFileItem.setEnabled(false);

        if (!contactPhoneUtil.isCallEnabled())
        {
            this.callItem.setEnabled(false);
        }

        if (!contactPhoneUtil.isVideoCallEnabled())
        {
            this.videoCallItem.setEnabled(false);
        }

        if (!contactPhoneUtil.isDesktopSharingEnabled())
        {
            fullShareMenuItem.setEnabled(false);
            regionShareMenuItem.setEnabled(false);
        }

        if (metaContact.getDefaultContact(
            OperationSetBasicInstantMessaging.class) == null)
            this.sendMessageItem.setEnabled(false);
    }

    /**
     * Adds call menu phone entries.
     *
     * @param phones the list of phones to add to menu
     */
    private void addCallMenuPhones(List<String> phones)
    {
        List<ProtocolProviderService> providers =
            CallManager.getTelephonyProviders();

        for(String phone : phones)
        {
            String p = phone.substring(0, phone.lastIndexOf("(") - 1);
            if(providers.size() > 0)
            {
                JMenuItem menuItem = createMenuItem( phone,
                    callPhonePrefix + p,
                    GuiActivator.getResources().getImage(
                        "service.gui.icons.EXTERNAL_PHONE"));
                menuItem.setBorder(
                    BorderFactory.createEmptyBorder(0, 20, 0, 0));

                callContactMenu.add(menuItem);

                separator = true;
            }
        }
    }

    /**
     * Adds contact resources to call menu.
     *
     * @param contact the <tt>Contact</tt>, which resources to add
     * @param protocolIcon the protocol icon
     */
    private void addCallMenuContact(Contact contact, Icon protocolIcon)
    {
        if (!contact.supportResources())
            return;

        Collection<ContactResource> resources = contact.getResources();

        if (resources == null)
            return;

        String contactAddress = contact.getAddress();

        if (contact.getResources().size() > 1)
        {
            callContactMenu.add(new ContactMenuItem(contact,
                                                    null,
                                                    contactAddress,
                                                    callContactPrefix,
                                                    protocolIcon,
                                                    true));
        }

        Iterator<ContactResource> resourceIter
            = contact.getResources().iterator();

        while (resourceIter.hasNext())
        {
            ContactResource resource = resourceIter.next();

            String resourceName;
            boolean isBold = false;
            Icon resourceIcon;
            if (contact.getResources().size() > 1)
            {
                resourceName = resource.getResourceName();
                resourceIcon
                    = ImageLoader.getIndexedProtocolIcon(
                        ImageUtils.getBytesInImage(
                        resource.getPresenceStatus().getStatusIcon()),
                        contact.getProtocolProvider());
            }
            else
            {
                resourceName = contact.getAddress()
                                + " " + resource.getResourceName();
                resourceIcon
                    = ImageLoader.getIndexedProtocolIcon(
                        ImageUtils.getBytesInImage(
                        contact.getPresenceStatus().getStatusIcon()),
                        contact.getProtocolProvider());
                // If the resource is only one we don't want to pass it to
                // call operations.
                resource = null;
                isBold = true;
            }

            JMenuItem menuItem = new ContactMenuItem(
                                                contact,
                                                resource,
                                                resourceName,
                                                callContactPrefix,
                                                resourceIcon,
                                                isBold);

            if (contact.getResources().size() > 1)
                menuItem.setBorder(
                    BorderFactory.createEmptyBorder(0, 20, 0, 0));

            callContactMenu.add(menuItem);
        }
    }

    /**
     * Adds video related call menu phone entries.
     *
     * @param contact the contact, which phones we're adding
     */
    private void addVideoMenuPhones(Contact contact)
    {
        List<ProtocolProviderService> providers =
            CallManager.getTelephonyProviders();

        List<String> videoPhones
            = contactPhoneUtil.getVideoPhones(contact, null);
        for(String vphone : videoPhones)
        {
            String p = vphone.substring(0, vphone.lastIndexOf("(") - 1);
            if(providers.size() > 0)
            {
                JMenuItem vmenu
                    = createMenuItem(   vphone,
                                        videoCallPrefix + p,
                                        null);

                videoCallMenu.add(vmenu);

                JMenuItem shdmenu
                    = createMenuItem( vphone,
                                      fullDesktopSharingPrefix + p,
                                      null);

                multiContactFullShareMenu.add(shdmenu);

                JMenuItem rshdmenu
                    = createMenuItem(   vphone,
                                        regionDesktopSharingPrefix + p,
                                        null);
                multiContactRegionShareMenu.add(rshdmenu);

                separator = true;
            }
        }
    }

    /**
     * Initializes the call menu items.
     *
     * @param displayName the display name of the menu item
     * @param name the name of the menu item, used to distinguish it in action
     * events
     * @param icon the icon of the protocol
     *
     * @return the created menu item
     */
    private JMenuItem createMenuItem(   String displayName,
                                        String name,
                                        Icon icon)
    {
        JMenuItem menuItem = new JMenuItem(displayName);

        menuItem.setIcon(icon);
        menuItem.setName(name);
        menuItem.addActionListener(this);

        return menuItem;
    }

    /**
     * Initializes plug-in components for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponentFactory.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponentFactory factory =
                    (PluginComponentFactory) GuiActivator
                        .bundleContext.getService(serRefs[i]);

                PluginComponent component =
                    factory.getPluginComponentInstance(this);

                component.setCurrentContact(metaContact);

                if (component.getComponent() == null)
                    continue;

                if(factory.getPositionIndex() != -1)
                    this.add((Component)component.getComponent(),
                        factory.getPositionIndex());
                else
                    this.add((Component)component.getComponent());

            }
        }
        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Initializes menu items mnemonics.
     */
    private void initMnemonics()
    {
        this.sendMessageItem.setMnemonic(
            GuiActivator.getResources()
                .getI18nMnemonic("service.gui.SEND_MESSAGE"));

        char callMnemonic = GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CALL");

        if (callContactMenu.getItemCount() > 1)
        {
            this.callContactMenu.setMnemonic(callMnemonic);
        }
        else
        {
            this.callItem.setMnemonic(callMnemonic);
        }

        char videoCallMnemonic = GuiActivator.getResources()
            .getI18nMnemonic("service.gui.VIDEO_CALL");

        if (videoCallMenu.getItemCount() > 1)
        {
            this.videoCallMenu.setMnemonic(videoCallMnemonic);
        }
        else
        {
            this.videoCallItem.setMnemonic(videoCallMnemonic);
        }

        this.sendSmsItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.SEND_SMS"));
        this.sendFileItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.SEND_FILE"));
        this.moveToMenu.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.MOVE_TO_GROUP"));
        this.addContactItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_CONTACT"));
        this.removeContactMenu.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.REMOVE_CONTACT"));
        this.renameContactItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.RENAME_CONTACT"));
        this.viewHistoryItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.VIEW_HISTORY"));
        this.moveSubcontactMenu.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.MOVE_SUBCONTACT"));
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and performs the appropriate operations.
     * @param e the <tt>ActionEvent</tt>, which notified us of the action
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        Contact contact;

        if (itemName.equals(addContactItem.getName()))
        {
            AddContactDialog dialog
                = new AddContactDialog(mainFrame, metaContact);

            // Try to select the preferred protocol provider.
            ProtocolProviderService protocolProvider
                = GuiActivator.getPreferredAccount();

            if (protocolProvider != null)
                dialog.setSelectedAccount(protocolProvider);

            dialog.setVisible(true);
        }
        else if (itemName.equalsIgnoreCase("sendMessage"))
        {
            GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact);
        }
        else if (itemName.equalsIgnoreCase("sendSms"))
        {
            Contact defaultSmsContact
                = metaContact.getDefaultContact(OperationSetSmsMessaging.class);

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact, defaultSmsContact, true);
        }
        else if (itemName.equals("call"))
        {
            call(false, false, false, null);
        }
        else if (itemName.equals("videoCall"))
        {
            call(true, false, false, null);
        }
        else if (itemName.equals("shareFullScreen"))
        {
            call(true, true, false, null);
        }
        else if (itemName.equals("shareRegion"))
        {
            call(true, true, true, null);
        }
        else if (itemName.equals("sendFile"))
        {
            SipCommFileChooser scfc = GenericFileDialog.create(
                null, "Send file...",
                SipCommFileChooser.LOAD_FILE_OPERATION,
                ConfigurationUtils.getSendFileLastDir());
            File selectedFile = scfc.getFileFromDialog();
            if(selectedFile != null)
            {
                ConfigurationUtils.setSendFileLastDir(
                        selectedFile.getParent());

                // Obtain the corresponding chat panel.
                ChatPanel chatPanel
                    = GuiActivator.getUIService().
                        getChatWindowManager().getContactChat(metaContact, true);

                chatPanel.sendFile(selectedFile);

                GuiActivator.getUIService().
                    getChatWindowManager().openChat(chatPanel, true);
            }

            GuiActivator.getUIService().getChatWindowManager()
                .startChat(metaContact);
        }
        else if (itemName.equals("renameContact"))
        {
            RenameContactDialog dialog = new RenameContactDialog(
                    mainFrame, metaContact);

            dialog.setVisible(true);

            dialog.requestFocusInFiled();
        }
        else if (itemName.equals("viewHistory"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            if(historyWindowManager
                .containsHistoryWindowForContact(metaContact))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(metaContact);

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else
            {
                history = new HistoryWindow(this.metaContact);

                historyWindowManager
                    .addHistoryWindowForContact(metaContact, history);

                history.setVisible(true);
            }
        }
        else if (itemName.equals("requestAuth"))
        {
            // If we have more than one protocol contacts, but just one of them
            // needs authorization.
            if (firstUnsubscribedContact != null)
                contact = firstUnsubscribedContact;
            // If we have only one protocol contact and it needs authorization.
            else
                contact = metaContact.getDefaultContact();

            requestAuthorization(contact);
        }
        else if (itemName.startsWith(moveToPrefix))
        {
            MetaContactListManager.moveMetaContactToGroup(
                metaContact, itemName.substring(moveToPrefix.length()));
        }
        else if (itemName.startsWith(removeContactPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                MetaContactListManager.removeContact(
                    ((ContactMenuItem) menuItem).getContact());
            }
            else
            {
                MetaContactListManager.removeMetaContact(metaContact);
            }
        }
        else if(itemName.startsWith(moveSubcontactPrefix))
        {
            contactList.addContactListListener(this);
            contactList.setGroupClickConsumed(true);

            // FIXME: set the special cursor while moving a subcontact
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            this.moveDialog = new MoveSubcontactMessageDialog(mainFrame, this);

            // Be sure we allow open/close groups in the contactlist if
            // user cancels the action
            this.moveDialog.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        contactList.setGroupClickConsumed(false);
                    }
                });
            this.moveDialog.setVisible(true);

            if(menuItem instanceof ContactMenuItem)
            {
                this.contactToMove = ((ContactMenuItem) menuItem).getContact();
            }
            else
            {
                this.moveAllContacts = true;
            }
        }
        else if (itemName.startsWith(callContactPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                ContactMenuItem contactItem = (ContactMenuItem) menuItem;

                call(false, false, false,
                    contactItem.getContact(), contactItem.getContactResource());
            }
            else
                call(false, false, false,
                    itemName.substring(callContactPrefix.length()));
        }
        else if (itemName.startsWith(videoCallPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                ContactMenuItem contactItem = (ContactMenuItem) menuItem;

                call(true, false, false, contactItem.getContact(),
                       contactItem.getContactResource());
            }
            else
                call(true, false, false,
                            itemName.substring(videoCallPrefix.length()));
        }
        else if (itemName.startsWith(fullDesktopSharingPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                ContactMenuItem contactItem = (ContactMenuItem) menuItem;

                call(true, true, false, contactItem.getContact(),
                    contactItem.getContactResource());
            }
            else
                call(true, true, false,
                        itemName.substring(fullDesktopSharingPrefix.length()));

        }
        else if (itemName.startsWith(regionDesktopSharingPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                ContactMenuItem contactItem = (ContactMenuItem) menuItem;

                call(true, true, true, contactItem.getContact(),
                    contactItem.getContactResource());
            }
            else
                call(true, true, true,
                    itemName.substring(regionDesktopSharingPrefix.length()));

        }
        else if (itemName.startsWith(requestAuthPrefix))
        {
            if(menuItem instanceof ContactMenuItem)
            {
                contact = ((ContactMenuItem) menuItem).getContact();
            }
            else
                contact = getContactFromMetaContact(
                    itemName.substring(requestAuthPrefix.length()));

            requestAuthorization(contact);
        }
        else if (itemName.startsWith(callPhonePrefix))
        {
            String phone = itemName.substring(callPhonePrefix.length());

            call(false, false, false, phone);
        }
    }

    /**
     * Obtains the <tt>Contact</tt> corresponding to the given address
     * identifier.
     *
     * @param itemID The address of the <tt>Contact</tt>.
     * @return the <tt>Contact</tt> corresponding to the given address
     * identifier.
     */
    private Contact getContactFromMetaContact(String itemID)
    {
        Iterator<Contact> i = metaContact.getContacts();

        while (i.hasNext())
        {
            Contact contact = i.next();

            String id = contact.getAddress()
                    + contact.getProtocolProvider().getProtocolName();

            if (itemID.equals(id))
            {
                return contact;
            }
        }
        return null;
    }

    /**
     * Indicates that a group has been selected during a move operation. Moves
     * the selected contact to the selected group.
     * @param evt the <tt>ContactListEvent</tt> has
     */
    public void groupClicked(ContactListEvent evt)
    {
        this.moveDialog.dispose();

        UIGroup sourceGroup = evt.getSourceGroup();

        // TODO: may be show a warning message to tell the user that she should
        // select another group.
        if (!(sourceGroup instanceof MetaUIGroup))
            return;

        MetaContactGroup metaGroup
            = (MetaContactGroup) sourceGroup.getDescriptor();

        contactList.removeContactListListener(this);

        // FIXME: unset the special cursor after a subcontact has been moved
        //guiContactList.setCursor(
        //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if(moveAllContacts)
        {
            MetaContactListManager
                .moveMetaContactToGroup(metaContact, metaGroup);
        }
        else if(contactToMove != null)
        {
            MetaContactListManager
                .moveContactToGroup(contactToMove, metaGroup);
        }

        contactList.setGroupClickConsumed(false);
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the chosen sub-contact when a meta contact is selected.
     * @param evt the <tt>ContactListEvent</tt> that notified us
     */
    public void contactClicked(ContactListEvent evt)
    {
        UIContact descriptor = evt.getSourceContact();
        // We're only interested in MetaContacts here.
        if (!(descriptor instanceof MetaUIContact))
            return;

        this.moveContact((MetaContact) descriptor.getDescriptor());
    }

    /**
     * We're not interested in group selection events here.
     */
    public void groupSelected(ContactListEvent evt) {}

    /**
     * We're not interested in contact selection events here.
     */
    public void contactSelected(ContactListEvent evt) {}

    /**
     * Moves the previously chosen sub-contact in the given toMetaContact.
     *
     * @param toMetaContact the MetaContact, where to move the previously
     * chosen sub-contact.
     */
    private void moveContact(MetaContact toMetaContact)
    {
        this.moveDialog.dispose();

        if(toMetaContact.equals(metaContact))
        {
            new ErrorDialog(this.mainFrame,
                GuiActivator.getResources()
                    .getI18NString("service.gui.MOVE_SUBCONTACT"),
                GuiActivator.getResources()
                    .getI18NString("service.gui.MOVE_SUBCONTACT_FAILED"),
                ErrorDialog.WARNING)
                    .showDialog();
        }
        else
        {
            contactList.removeContactListListener(this);

            // FIXME: unset the special cursor after a subcontact has been moved
            //guiContactList.setCursor(
            //        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if(moveAllContacts)
            {
                MetaContactListManager
                    .moveMetaContactToMetaContact(metaContact, toMetaContact);
            }
            else if(contactToMove != null)
            {
                MetaContactListManager
                    .moveContactToMetaContact(contactToMove, toMetaContact);
            }
        }
    }

    /**
     * Adds the according plug-in component to this container.
     * @param event received event
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if(!factory.getContainer()
                .equals(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
            return;

        Object constraints
            = UIServiceImpl.getBorderLayoutConstraintsFromContainer(
                factory.getConstraints());

        PluginComponent c = factory.getPluginComponentInstance(this);
        if (c.getComponent() == null)
            return;

        int ix = factory.getPositionIndex();

        if (constraints == null)
        {
            if(ix != -1)
                this.add((Component) c.getComponent(), ix);
            else
                this.add((Component) c.getComponent());
        }
        else
        {
            if(ix != -1)
                this.add((Component) c.getComponent(), constraints, ix);
            else
                this.add((Component) c.getComponent(), constraints);
        }


        c.setCurrentContact(metaContact);

        this.repaint();
    }

    /**
     * Removes the according plug-in component from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if(factory.getContainer()
                .equals(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {
            this.remove((Component)factory.getPluginComponentInstance(this)
                .getComponent());
        }
    }

    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param protoContact the protocol contact for which to create the image
     * @return the indexed status image
     */
    public Image createContactStatusImage(Contact protoContact)
    {
        return
            ImageLoader.getIndexedProtocolImage(
                ImageUtils.getBytesInImage(
                    protoContact.getPresenceStatus().getStatusIcon()),
                protoContact.getProtocolProvider());
    }

    /**
     * Reloads skin related information.
     */
    public void loadSkin()
    {
        callItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));

        videoCallItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.VIDEO_CALL)));

        videoCallMenu.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.VIDEO_CALL)));

        fullShareMenuItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.DESKTOP_SHARING)));

        regionShareMenuItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.REGION_DESKTOP_SHARING)));

        multiContactFullShareMenu.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.DESKTOP_SHARING)));

        multiContactRegionShareMenu.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.REGION_DESKTOP_SHARING)));

        sendMessageItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

        sendFileItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.SEND_FILE_16x16_ICON)));

        sendSmsItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

        renameContactItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.RENAME_16x16_ICON)));

        viewHistoryItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HISTORY_16x16_ICON)));

        moveToMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MOVE_TO_GROUP_16x16_ICON)));

        addContactItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        removeContactMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.DELETE_16x16_ICON)));

        moveSubcontactMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MOVE_CONTACT_ICON)));

        callContactMenu.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));

        requestAuthMenuItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.UNAUTHORIZED_CONTACT_16x16)));

        multiContactRequestAuthMenu.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.UNAUTHORIZED_CONTACT_16x16)));
    }

    /**
     * Requests authorization for contact.
     *
     * @param contact the contact for which we request authorization
     */
    private void requestAuthorization(final Contact contact)
    {
        final OperationSetExtendedAuthorizations authOpSet
            = contact.getProtocolProvider().getOperationSet(
                OperationSetExtendedAuthorizations.class);

        if (authOpSet == null)
            return;

        final AuthorizationRequest request = new AuthorizationRequest();

        final RequestAuthorizationDialog dialog
            = new RequestAuthorizationDialog(mainFrame, contact, request);
        dialog.showDialog();

        new Thread()
        {
            @Override
            public void run()
            {
                int returnCode = dialog.getReturnCode();

                if(returnCode == RequestAuthorizationDialog.OK_RETURN_CODE)
                {
                    request.setReason(dialog.getRequestReason());

                    try
                    {
                        authOpSet.reRequestAuthorization(request, contact);
                    }
                    catch (OperationFailedException e)
                    {
                        new ErrorDialog(mainFrame,
                            GuiActivator.getResources()
                                .getI18NString(
                                    "service.gui.RE_REQUEST_AUTHORIZATION"),
                            e.getMessage(),
                            ErrorDialog.WARNING)
                                .showDialog();
                    }
                }
            }
        }.start();
    }

    /**
     * Calls using the CallManager
     * @param isVideo whether video button is pressed
     * @param isDesktopSharing whether the share desktop button is used
     * @param shareRegion whether the user want to share region from the desktop
     * @param contact the contact to call
     * @param contactResource the specific contact resource to call
     */
    private void call(boolean isVideo,
                      boolean isDesktopSharing,
                      boolean shareRegion,
                      Contact contact,
                      ContactResource contactResource)
    {
        if (contactResource != null)
            CallManager.call(
                contact, contactResource,
                isVideo, isDesktopSharing, shareRegion);
        else
            CallManager.call(
                contact, isVideo, isDesktopSharing, shareRegion);
    }

    /**
     * Calls using the CallManager
     * @param isVideo whether video button is pressed
     * @param isDesktopSharing whether the share desktop button is used
     * @param shareRegion whether the user want to share region from the desktop
     * @param contactName the phone number to call or the contact name
     *                    selected (normally when using prefix), if null
     *                    will call the metacontact
     */
    private void call(boolean isVideo,
                      boolean isDesktopSharing,
                      boolean shareRegion,
                      String contactName)
    {
        if(contactName != null)
        {
            Contact contact = getContactFromMetaContact(contactName);

            // we want to call particular contact
            if(contact != null)
            {
                call(isVideo, isDesktopSharing, shareRegion, contact, null);
                return;
            }
            else
            {
                // we want to call a phoneNumber
                CallManager.call(
                    contactName, MetaContactListSource.getUIContact(metaContact),
                    isVideo, isDesktopSharing, shareRegion);
                return;
            }
        }

        // just call the metacontact
        CallManager.call(metaContact, isVideo, isDesktopSharing, shareRegion);
    }

    /**
     * A JMenuItem corresponding to a specific protocol <tt>Contact</tt>.
     */
    private class ContactMenuItem
        extends JMenuItem
    {
        /**
         * The associated contact.
         */
        private final Contact contact;

        /**
         * The associated contact resource.
         */
        private ContactResource contactResource;

        /**
         * Creates an instance of <tt>ContactMenuItem</tt>.
         *
         * @param contact the associated protocol <tt>Contact</tt>
         * @param displayName the text to display on the menu
         * @param menuName the name of the menu, used by action listeners
         * @param icon the icon associated by this menu item
         */
        public ContactMenuItem( Contact contact,
                                String displayName,
                                String menuName,
                                Icon icon)
        {
            this(contact, null, displayName, menuName, icon, false);
        }

        /**
         * Creates an instance of <tt>ContactMenuItem</tt>.
         *
         * @param contact the associated protocol <tt>Contact</tt>
         * @param contactResource the associated <tt>ContactResource</tt>
         * @param displayName the text to display on the menu
         * @param menuName the name of the menu, used by action listeners
         * @param icon the icon associated by this menu item
         * @param isBold indicates if the menu should be shown in bold
         */
        public ContactMenuItem( Contact contact,
                                ContactResource contactResource,
                                String displayName,
                                String menuName,
                                Icon icon,
                                boolean isBold)
        {
            super(displayName);

            this.contact = contact;
            this.contactResource = contactResource;

            setIcon(icon);
            setName(menuName);
            if (isBold)
                setFont(getFont().deriveFont(Font.BOLD));
            addActionListener(MetaContactRightButtonMenu.this);
        }

        /**
         * Returns the protocol <tt>Contact</tt> associated with this menu item.
         *
         * @return the protocol <tt>Contact</tt> associated with this menu item
         */
        Contact getContact()
        {
            return contact;
        }

        /**
         * Returns the <tt>ContactResource</tt> associated with this menu item
         * if such exists otherwise returns null.
         *
         * @return the <tt>ContactResource</tt> associated with this menu item
         * if such exists otherwise returns null
         */
        ContactResource getContactResource()
        {
            return contactResource;
        }
    }
}
