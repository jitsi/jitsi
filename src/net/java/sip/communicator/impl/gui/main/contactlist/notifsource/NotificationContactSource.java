/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.notifsource;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetMessageWaiting.MessageType;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>NotificationContactSource</tt> represents a contact source that would
 * listen for message waiting notifications and would display them in the
 * history view of the contact list.
 *
 * @author Yana Stamcheva
 */
public class NotificationContactSource
    implements MessageWaitingListener
{
    /**
     * A mapping attaching to each <tt>NotificationGroup</tt> the
     * corresponding <tt>MessageType</tt>, for which notifications
     * are received.
     */
    private final Hashtable<String, NotificationGroup> groups
        = new Hashtable<String, NotificationGroup>();

    /**
     * The list of action buttons for this meta contact.
     */
    private static Map<ContactAction<NotificationMessage>, SIPCommButton>
                                                            customActionButtons;

    private static NotificationContact customActionContact;

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(NotificationContactSource.class);

    /**
     * Adds the received waiting message to the corresponding group and contact.
     * Also adds it the <tt>UINotificationManager</tt> that would take care of
     * notifying the user.
     *
     * @param evt the notification event.
     */
    public void messageWaitingNotify(MessageWaitingEvent evt)
    {
        MessageType type = evt.getMessageType();

        NotificationGroup group = groups.get(type.toString());

        if (group == null)
        {
            String displayName;
            if (type.equals(MessageType.VOICE))
                displayName = GuiActivator.getResources()
                    .getI18NString("service.gui.VOICEMAIL_TITLE");
            else
                displayName = type.toString();

            group = new NotificationGroup(displayName);
            groups.put(type.toString(), group);
        }

        // mark it as global box by not providing list of messages.
        group.messageWaitingNotify(
            new MessageWaitingEvent(evt.getSourceProvider(),
                                    evt.getMessageType(),
                                    evt.getAccount(),
                                    evt.getUnreadMessages(),
                                    evt.getReadMessages(),
                                    evt.getUnreadUrgentMessages(),
                                    evt.getReadUrgentMessages()));

        Iterator<NotificationMessage> messages = evt.getMessages();

        if (messages != null)
        {
            while (messages.hasNext())
            {
                NotificationMessage message = messages.next();

                String messageGroupName = message.getMessageGroup();

                NotificationGroup messageGroup = groups.get(messageGroupName);

                if (messageGroup == null)
                {
                    messageGroup
                        = new NotificationGroup(messageGroupName);

                    groups.put(messageGroupName, messageGroup);
                }

                messageGroup.messageWaitingNotify(evt);
            }
        }
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all notification groups
     * contained in this source.
     *
     * @return an <tt>Iterator</tt> over a list of all notification groups
     * contained in this source
     */
    public Iterator<? extends UIGroup> getNotificationGroups()
    {
        return groups.values().iterator();
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all notification contacts
     * contained in the given group.
     *
     * @param group the group, which notification contacts we're looking for
     * @return an <tt>Iterator</tt> over a list of all notification contacts
     * contained in the given group
     */
    public Iterator<? extends UIContact> getNotifications(UIGroup group)
    {
        if (!(group instanceof NotificationGroup))
            return null;

        NotificationGroup notifGroup = (NotificationGroup) group;

        return notifGroup.getNotifications();
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public static Collection<SIPCommButton> getContactCustomActionButtons(
            final NotificationContact notificationContact)
    {
        customActionContact = notificationContact;

        if (customActionButtons == null)
            initCustomActionButtons();

        return customActionButtons.values();
    }

    /**
     * Initializes custom action buttons.
     */
    private static void initCustomActionButtons()
    {
        customActionButtons = new LinkedHashMap
                                        <ContactAction<NotificationMessage>,
                                         SIPCommButton>();

        for (CustomContactActionsService<NotificationMessage> ccas
                : getNotificationActionsServices())
        {
            Iterator<ContactAction<NotificationMessage>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<NotificationMessage>
                    ca = actionIterator.next();

                SIPCommButton actionButton = customActionButtons.get(ca);

                if (actionButton == null)
                {
                    actionButton = new SIPCommButton(
                        new ImageIcon(ca.getIcon()).getImage(),
                        new ImageIcon(ca.getPressedIcon()).getImage(),
                        null);

                    actionButton.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            List<UIContactDetail> contactDetails
                                = customActionContact.getContactDetails();

                            UIContactDetailCustomAction contactAction
                                = new UIContactDetailCustomAction(ca);

                            if (contactDetails.size() > 1)
                            {
                                ChooseUIContactDetailPopupMenu
                                    detailsPopupMenu
                                        = new ChooseUIContactDetailPopupMenu(
                                            (JButton) e.getSource(),
                                            customActionContact
                                                .getContactDetails(),
                                            contactAction);

                                detailsPopupMenu.showPopupMenu();
                            }
                            else if (contactDetails.size() == 1)
                            {
                                JButton button = (JButton) e.getSource();
                                Point location = new Point(button.getX(),
                                    button.getY() + button.getHeight());

                                SwingUtilities.convertPointToScreen(
                                    location, GuiActivator.getContactList());

                                location.y = location.y
                                    + GuiActivator.getContactList()
                                        .getPathBounds(
                                            GuiActivator.getContactList()
                                            .getSelectionPath()).y;

                                contactAction.actionPerformed(
                                    contactDetails.get(0),
                                    location.x,
                                    location.y);
                            }
                        }
                    });

                    customActionButtons.put(ca, actionButton);
                }
            }
        }
    }

    /**
     * An implementation of <tt>UIContactDetail</tt> for a custom action.
     */
    private static class UIContactDetailCustomAction
        implements UIContactDetailAction
    {
        /**
         * The contact action.
         */
        private final ContactAction<NotificationMessage> contactAction;

        /**
         * Creates an instance of <tt>UIContactDetailCustomAction</tt>.
         */
        public UIContactDetailCustomAction(
            ContactAction<NotificationMessage> contactAction)
        {
            this.contactAction = contactAction;
        }

        /**
         * Performs the action on button click.
         */
        public void actionPerformed(UIContactDetail contactDetail, int x, int y)
        {
            try
            {
                contactAction.actionPerformed(
                    (NotificationMessage) contactDetail.getDescriptor(), x, y);
            }
            catch (OperationFailedException e)
            {
                new ErrorDialog(null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    e.getMessage());
            }
        }
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private static List<CustomContactActionsService<NotificationMessage>>
        getNotificationActionsServices()
    {
        List<CustomContactActionsService<NotificationMessage>>
            contactActionsServices
                = new ArrayList<CustomContactActionsService
                                    <NotificationMessage>>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                    CustomContactActionsService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("NotificationContactSource : " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                CustomContactActionsService<?> customActionService
                    = (CustomContactActionsService<?>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(NotificationMessage.class))
                {
                    contactActionsServices.add(
                        (CustomContactActionsService<NotificationMessage>)
                            customActionService);
                }
            }
        }
        return contactActionsServices;
    }
}
