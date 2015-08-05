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
package net.java.sip.communicator.impl.gui.main.contactlist.notifsource;

import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetMessageWaiting.*;
import net.java.sip.communicator.service.protocol.event.*;

import net.java.sip.communicator.service.protocol.globalstatus.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>NotificationContact</tt> represents a notification entry shown in
 * the contact list history view. It could represent a voice message entry,
 * email notification or something else.
 *
 * @author Yana Stamcheva
 */
public class NotificationContact
    extends  UIContactImpl
    implements  RegistrationStateChangeListener,
                ProviderPresenceStatusListener
{
    /**
     * The tip explaining to the user how to hear its voice messages.
     */
    private static final String VOICEMAIL_TIP
        = GuiActivator.getResources()
            .getI18NString("service.gui.VOICEMAIL_TIP");

    /**
     * Tooltip for missing account.
     */
    private static final String VOICEMAIL_TIP_NO_ACCOUNT
        = GuiActivator.getResources()
            .getI18NString("service.gui.VOICEMAIL_TIP_NO_ACCOUNT");

    /**
     * The parent contact list group.
     */
    private NotificationGroup parentGroup;

    /**
     * The corresponding protocol provider.
     */
    private final ProtocolProviderService protocolProvider;

    /**
     * The notification message corresponding to the source message.
     */
    private final NotificationMessage notificationMessage;

    /**
     * The corresponding <tt>ContactNode</tt> in the contact list component
     * data model.
     */
    private ContactNode contactNode;

    /**
     * The notification detail.
     */
    private UIContactDetail notificationDetail;

    /**
     * The count of unread urgent messages attached to this notification.
     */
    private int unreadUrgentMessageCount = 0;

    /**
     * The count of unread messages attached to this notification.
     */
    private int unreadMessageCount = 0;

    /**
     * The count of read messages attached to this notification.
     */
    private int readMessageCount = 0;

    /**
     * The type of the message.
     */
    private MessageType messageType;

    /**
     * Creates an instance of <tt>NotificationContact</tt> by specifying the
     * parent group and the corresponding <tt>ProtocolProviderService</tt>.
     *
     * @param group the parent group
     * @param protocolProvider the corresponding protocol provider
     * @param messageType the type of the message
     * @param notificationMessage the actual notification message
     */
    public NotificationContact( NotificationGroup group,
                                ProtocolProviderService protocolProvider,
                                MessageType messageType,
                                NotificationMessage notificationMessage)
    {
        this.parentGroup = group;
        this.protocolProvider = protocolProvider;
        this.messageType = messageType;
        this.notificationMessage = notificationMessage;

        protocolProvider.addRegistrationStateChangeListener(this);

        OperationSetPresence presenceOpSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        if (presenceOpSet != null)
            presenceOpSet.addProviderPresenceStatusListener(this);
    }

    /**
     * Returns the descriptor of this contact.
     *
     * @return the descriptor of this contact
     */
    @Override
    public Object getDescriptor()
    {
        return protocolProvider;
    }

    /**
     * Returns the display name of this contact.
     *
     * @return the display name of this contact
     */
    @Override
    public String getDisplayName()
    {
        if (notificationMessage != null)
            return notificationMessage.getFromContact();

        return GuiActivator.getUIService().getMainFrame()
                .getAccountDisplayName(protocolProvider);
    }

    /**
     * Returns the display details of this contact. These would be shown
     * whenever the contact is selected. In the <tt>NotificationContact</tt>
     * these contain information about unread and read messages.
     *
     * @return the display details of this contact
     */
    @Override
    public String getDisplayDetails()
    {
        String displayDetails;

        if (notificationMessage != null)
        {
            return notificationMessage.getMessageDetails();
        }

        ResourceManagementService resources = GuiActivator.getResources();

        if ((unreadUrgentMessageCount > 0) &&
            (unreadMessageCount > 0) &&
            (readMessageCount > 0))
        {
            displayDetails = resources.getI18NString(
                "service.gui.VOICEMAIL_NEW_URGENT_OLD_RECEIVED",
                new String[]{   Integer.toString(unreadMessageCount),
                                Integer.toString(unreadUrgentMessageCount),
                                Integer.toString(readMessageCount)});
        }
        else if ((unreadMessageCount > 0) && (readMessageCount > 0))
        {
            displayDetails = resources.getI18NString(
                "service.gui.VOICEMAIL_NEW_OLD_RECEIVED",
                new String[]{   Integer.toString(unreadMessageCount),
                                Integer.toString(readMessageCount)});
        }
        else if ((unreadUrgentMessageCount > 0) && (unreadMessageCount > 0))
        {
            displayDetails = resources.getI18NString(
                "service.gui.VOICEMAIL_NEW_URGENT_RECEIVED",
                new String[]{   Integer.toString(unreadMessageCount),
                                Integer.toString(unreadUrgentMessageCount)});
        }
        else if (unreadMessageCount > 0)
        {
            displayDetails = resources.getI18NString(
                "service.gui.VOICEMAIL_NEW_RECEIVED",
                new String[]{   Integer.toString(unreadMessageCount)});
        }
        else if (readMessageCount > 0)
        {
            displayDetails = resources.getI18NString(
                "service.gui.VOICEMAIL_OLD_RECEIVED",
                new String[]{   Integer.toString(readMessageCount)});
        }
        else
        {
            displayDetails = resources.getI18NString(
                "service.gui.VOICEMAIL_NO_MESSAGES");
        }

        return displayDetails;
    }

    /**
     * Returns the index of this contact in its source.
     *
     * @return the source index
     */
    @Override
    public int getSourceIndex()
    {
        return -1;
    }

    /**
     * Returns the icon indicating that this is a notification contact.
     *
     * @param isSelected indicates if the contact is selected
     * @param width the width of the avatar
     * @param height the height of the avatar
     * @return  the avatar of this contact
     */
    @Override
    public ImageIcon getScaledAvatar(boolean isSelected, int width, int height)
    {
        ImageIcon avatarIcon = null;
        if (messageType.equals(MessageType.VOICE))
        {
            avatarIcon = GuiActivator.getResources().getImage(
                                "service.gui.icons.VOICEMAIL");
        }

        return avatarIcon;
    }

    /**
     * Returns the status icon of this contact or null if no status is
     * available.
     *
     * @return the status icon of this contact or null if no status is
     * available
     */
    @Override
    public ImageIcon getStatusIcon()
    {
        OperationSetPresence presence = protocolProvider
            .getOperationSet(OperationSetPresence.class);

        if (presence != null)
        {
            return new ImageIcon(
                Constants.getStatusIcon(presence.getPresenceStatus()));
        }
        else if (protocolProvider.isRegistered())
        {
            return new ImageIcon(GlobalStatusEnum.ONLINE.getStatusIcon());
        }

        return new ImageIcon(GlobalStatusEnum.OFFLINE.getStatusIcon());
    }

    /**
     * Creates a tool tip for this contact. If such tooltip is
     * provided it would be shown on mouse over over this <tt>UIContact</tt>.
     *
     * @return the tool tip for this contact descriptor
     */
    /**
     * Returns the tool tip opened on mouse over.
     * @return the tool tip opened on mouse over
     */
    @Override
    public ExtendedTooltip getToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(true);

        ImageIcon avatarImage = getScaledAvatar(true, 64, 64);

        if (avatarImage != null)
            tip.setImage(avatarImage);

        tip.setTitle(protocolProvider.getAccountID().getDisplayName());

        tip.addLine(new JLabel[]{new JLabel(getDisplayDetails())});
        tip.addLine(null, " ");

        if(notificationDetail != null && notificationDetail.getAddress() != null)
            tip.setBottomText(VOICEMAIL_TIP);
        else
            tip.setBottomText(VOICEMAIL_TIP_NO_ACCOUNT);

        return tip;
    }

    /**
     * Returns null to indicate that no right button menu is provided for this
     * contact.
     *
     * @return null
     */
    @Override
    public JPopupMenu getRightButtonMenu()
    {
        return null;
    }

    /**
     * Returns the parent group.
     *
     * @return the parent group
     */
    @Override
    public UIGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Sets the given <tt>UIGroup</tt> to be the parent group of this
     * <tt>UIContact</tt>.
     *
     * @param parentGroup the parent <tt>UIGroup</tt> of this contact
     */
    @Override
    public void setParentGroup(UIGroup parentGroup)
    {
        if (!(parentGroup instanceof NotificationGroup))
            return;

        this.parentGroup = (NotificationGroup) parentGroup;
    }

    /**
     * No search strings are provided for this contact.
     *
     * @return null
     */
    @Override
    public Iterator<String> getSearchStrings()
    {
        return null;
    }

    /**
     * Returns the corresponding <tt>ContactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @return the corresponding <tt>ContactNode</tt>
     */
    @Override
    public ContactNode getContactNode()
    {
        return contactNode;
    }

    /**
     * Sets the given <tt>contactNode</tt>. The <tt>ContactNode</tt>
     * is the real node that is stored in the contact list component data model.
     *
     * @param contactNode the <tt>ContactNode</tt> that corresponds to this
     * <tt>UIGroup</tt>
     */
    @Override
    public void setContactNode(ContactNode contactNode)
    {
        this.contactNode = contactNode;
    }

    /**
     * Returns the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class
     */
    @Override
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        if (opSetClass.equals(OperationSetBasicTelephony.class))
            return notificationDetail;

        return null;
    }

    /**
     * Returns a list of all contained <tt>UIContactDetail</tt>s.
     *
     * @return a list of all contained <tt>UIContactDetail</tt>s
     */
    @Override
    public List<UIContactDetail> getContactDetails()
    {
        List<UIContactDetail> resultList = new LinkedList<UIContactDetail>();

        resultList.add(notificationDetail);

        return resultList;
    }

    /**
     * Returns a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    @Override
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> resultList = new LinkedList<UIContactDetail>();

        if (opSetClass.equals(OperationSetBasicTelephony.class))
            resultList.add(notificationDetail);

        return resultList;
    }

    /**
     * Sets the corresponding message account.
     *
     * @param messageAccount the message account corresponding to the contained
     * notification
     */
    public void setMessageAccount(String messageAccount)
    {
        notificationDetail
            = new MessageWaitingDetail(protocolProvider, messageAccount);
    }

    /**
     * Sets the number of urgent unread messages, this notification is about.
     *
     * @param count the number of urgent unread messages, this notification is
     * about
     */
    public void setUnreadUrgentMessageCount(int count)
    {
        this.unreadUrgentMessageCount = count;
    }

    /**
     * Returns the number of urgent unread messages, this notification is
     * about.
     *
     * @return the number of urgent unread messages, this notification is about
     */
    public int getUrgentUnreadMessageCount()
    {
        return unreadUrgentMessageCount;
    }

    /**
     * Sets the number of unread messages, this notification is about.
     *
     * @param count the number of unread messages, this notification is about
     */
    public void setUnreadMessageCount(int count)
    {
        this.unreadMessageCount = count;
    }

    /**
     * Returns the number of unread messages, this notification is about.
     *
     * @return the number of unread messages, this notification is about
     */
    public int getUnreadMessageCount()
    {
        return unreadMessageCount;
    }

    /**
     * Sets the number of read messages, this notification is about.
     *
     * @param count the number of read messages, this notification is about
     */
    public void setReadMessageCount(int count)
    {
        this.readMessageCount = count;
    }

    /**
     * Returns the notification message corresponding to this notification
     * contact.
     *
     * @return the <tt>NotificationMessage</tt> corresponding to this
     * <tt>NotificationContact</tt>
     */
    public NotificationMessage getNotificationMessage()
    {
        return notificationMessage;
    }

    /**
     * The implementation of the <tt>UIContactDetail</tt> interface for the
     * external source <tt>ContactDetail</tt>s.
     */
    private class MessageWaitingDetail
        extends UIContactDetailImpl
    {
        /**
         * Creates an instance of <tt>SourceContactDetail</tt> by specifying
         * the underlying <tt>detail</tt> and the <tt>OperationSet</tt> class
         * for it.
         *
         * @param protocolProvider the protocol provider corresponding to this
         * detail
         * @param messageAccount the message account corresponding to this
         * detail
         */
        public MessageWaitingDetail(ProtocolProviderService protocolProvider,
                                    String messageAccount)
        {
            super(  messageAccount,
                    messageAccount,
                    ImageLoader.getAccountStatusImage(protocolProvider),
                    notificationMessage);

            Iterator<Class<? extends OperationSet>> opSetClasses
                = protocolProvider.getSupportedOperationSetClasses().iterator();

            while (opSetClasses.hasNext())
            {
                Class<? extends OperationSet> opSetClass = opSetClasses.next();

                addPreferredProtocolProvider(opSetClass, protocolProvider);
                addPreferredProtocol(opSetClass,
                    protocolProvider.getProtocolName());
            }
        }

        /**
         * Returns null to indicate that this detail doesn't support presence.
         * @return null
         */
        @Override
        public PresenceStatus getPresenceStatus()
        {
            return null;
        }
    }

    /**
     * Refresh the notification contact corresponding the the attached provider
     * in order to better reflect its state.
     *
     * @param evt the <tt>ProviderPresenceStatusChangeEvent</tt> that has
     * notified us of the state change
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();
        if (newState.equals(RegistrationState.UNREGISTERED)
            || newState.equals(RegistrationState.REGISTERED))
        {
            TreeContactList contactList = GuiActivator.getContactList();

            contactList.refreshContact(this);
        }
    }

    /**
     * Refresh the notification contact corresponding the the attached provider
     * in order to better reflect its status.
     *
     * @param evt the <tt>ProviderPresenceStatusChangeEvent</tt> that has
     * notified us of the status change
     */
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {
        TreeContactList contactList = GuiActivator.getContactList();

        contactList.refreshContact(this);
    }

    /**
     * Returns all custom action buttons for this notification contact.
     *
     * @return a list of all custom action buttons for this notification contact
     */
    @Override
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        if (notificationMessage != null)
            return NotificationContactSource
                    .getContactCustomActionButtons(this);

        return null;
    }

    public void providerStatusMessageChanged(PropertyChangeEvent evt) {}
}
