/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * An implementation of the <tt>ChatSession</tt> interface that represents a
 * user-to-user chat session.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * 
 */
public class MetaContactChatSession
    extends  ChatSession
    implements MetaContactListListener
{
    private final MetaContact metaContact;

    private final MetaContactListService metaContactListService;

    private ChatTransport currentChatTransport;

    private final ChatSessionRenderer sessionRenderer;

    private final java.util.List<ChatSessionChangeListener>
            chatTransportChangeListeners
                = new Vector<ChatSessionChangeListener>();

    /**
     * Creates an instance of <tt>MetaContactChatSession</tt> by specifying the
     * renderer, which gives the connection with the UI, the meta contact
     * corresponding to the session and the protocol contact to be used as
     * transport.
     *
     * @param sessionRenderer the renderer, which gives the connection with
     * the UI.
     * @param metaContact the meta contact corresponding to the session and the
     * protocol contact.
     * @param protocolContact the protocol contact to be used as transport.
     */
    public MetaContactChatSession(  ChatSessionRenderer sessionRenderer,
                                    MetaContact metaContact,
                                    Contact protocolContact)
    {
        this.sessionRenderer = sessionRenderer;
        this.metaContact = metaContact;

        ChatContact chatContact = new MetaContactChatContact(metaContact);

        chatParticipants.add(chatContact);

        this.initChatTransports(protocolContact);

        // Obtain the MetaContactListService and add this class to it as a
        // listener of all events concerning the contact list.
        metaContactListService = GuiActivator.getContactListService();

        if (metaContactListService != null)
            metaContactListService.addMetaContactListListener(this);
    }

    /**
     * Returns the name of this chat.
     *
     * @return the name of this chat
     */
    public String getChatName()
    {
        String displayName = metaContact.getDisplayName();

        if (displayName != null && displayName.length() > 0)
            return metaContact.getDisplayName();

        return GuiActivator.getResources().getI18NString("service.gui.UNKNOWN");
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     *
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistory(int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findLast(
            chatHistoryFilter,
            metaContact,
            ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     *
     * @param date The date up to which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistoryBeforeDate(Date date, int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findLastMessagesBefore(
            chatHistoryFilter,
            metaContact, date, ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     *
     * @param date The date from which we're looking for messages.
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistoryAfterDate(Date date, int count)
    {
        final MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findFirstMessagesAfter(
            chatHistoryFilter,
            metaContact, date, ConfigurationManager.getChatHistorySize());
    }

    /**
     * Returns the start date of the history of this chat session.
     *
     * @return the start date of the history of this chat session.
     */
    public long getHistoryStartDate()
    {
        long startHistoryDate = 0;

        MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return startHistoryDate;

        Collection<Object> firstMessage = metaHistory
            .findFirstMessagesAfter(
                chatHistoryFilter, metaContact, new Date(0), 1);

        if(firstMessage.size() > 0)
        {
            Iterator<Object> i = firstMessage.iterator();

            Object o = i.next();

            if(o instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent) o;

                startHistoryDate = evt.getTimestamp();
            }
            else if(o instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent) o;

                startHistoryDate = evt.getTimestamp();
            }
            else if (o instanceof FileRecord)
            {
                FileRecord fileRecord = (FileRecord) o;

                startHistoryDate = fileRecord.getDate();
            }
        }

        return startHistoryDate;
    }

    /**
     * Returns the end date of the history of this chat session.
     *
     * @return the end date of the history of this chat session.
     */
    public long getHistoryEndDate()
    {
        long endHistoryDate = 0;

        MetaHistoryService metaHistory
            = GuiActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return endHistoryDate;

        Collection<Object> lastMessage = metaHistory
            .findLastMessagesBefore(
                chatHistoryFilter, metaContact, new Date(Long.MAX_VALUE), 1);

        if(lastMessage.size() > 0)
        {
            Iterator<Object> i1 = lastMessage.iterator();

            Object o1 = i1.next();

            if(o1 instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent evt
                    = (MessageDeliveredEvent) o1;

                endHistoryDate = evt.getTimestamp();
            }
            else if(o1 instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent evt = (MessageReceivedEvent) o1;

                endHistoryDate = evt.getTimestamp();
            }
            else if (o1 instanceof FileRecord)
            {
                FileRecord fileRecord = (FileRecord) o1;

                endHistoryDate = fileRecord.getDate();
            }
        }

        return endHistoryDate;
    }

    /**
     * Returns the default mobile number used to send sms-es in this session.
     *
     * @return the default mobile number used to send sms-es in this session.
     */
    public String getDefaultSmsNumber()
    {
        String smsNumber = null;

        List<String> detailsList = metaContact.getDetails("mobile");

        if (detailsList != null && detailsList.size() > 0)
        {
            smsNumber = detailsList.iterator().next();
        }

        return smsNumber;
    }

    /**
     * Sets the default mobile number used to send sms-es in this session.
     *
     * @param smsPhoneNumber The default mobile number used to send sms-es in
     * this session.
     */
    public void setDefaultSmsNumber(String smsPhoneNumber)
    {
        metaContact.addDetail("mobile", smsPhoneNumber);
    }

    /**
     * Initializes all chat transports for this chat session.
     *
     * @param protocolContact the <tt>Contact</tt> which is to be selected into
     * this instance as the current i.e. its <tt>ChatTransport</tt> is to be
     * selected as <tt>currentChatTransport</tt>
     */
    private void initChatTransports(Contact protocolContact)
    {
        Iterator<Contact> protocolContacts = metaContact.getContacts();

        while (protocolContacts.hasNext())
        {
            Contact contact = protocolContacts.next();

            MetaContactChatTransport chatTransport
                = new MetaContactChatTransport(this, contact);

            chatTransports.add(chatTransport);

            if (contact.equals(protocolContact))
                currentChatTransport = chatTransport;
        }
    }

    /**
     * Returns the currently used transport for all operation within this chat
     * session.
     *
     * @return the currently used transport for all operation within this chat
     * session.
     */
    public ChatTransport getCurrentChatTransport()
    {
        return currentChatTransport;
    }

    /**
     * Sets the transport that will be used for all operations within this chat
     * session.
     *
     * @param chatTransport The transport to set as a default transport for this
     * session.
     */
    public void setCurrentChatTransport(ChatTransport chatTransport)
    {
        this.currentChatTransport = chatTransport;

        for (ChatSessionChangeListener l : chatTransportChangeListeners)
            l.currentChatTransportChanged(this);
    }

    public void childContactsReordered(MetaContactGroupEvent evt)
    {}

    public void metaContactAdded(MetaContactEvent evt)
    {}

    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {}

    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {}

    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {}

    public void metaContactModified(MetaContactModifiedEvent evt)
    {}

    public void metaContactMoved(MetaContactMovedEvent evt)
    {}

    public void metaContactRemoved(MetaContactEvent evt)
    {}

    public void metaContactAvatarUpdated(MetaContactAvatarUpdateEvent evt) {}

    /**
     * Implements <tt>MetaContactListListener.metaContactRenamed</tt> method.
     * When a meta contact is renamed, updates all related labels in this
     * chat panel.
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        String newName = evt.getNewDisplayName();

        if(evt.getSourceMetaContact().equals(metaContact))
        {
            ChatContact chatContact
                = findChatContactByMetaContact(evt.getSourceMetaContact());

            sessionRenderer.setContactName(chatContact, newName);
        }
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactAdded</tt> method.
     * When a proto contact is added, updates the "send via" selector box.
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        if (evt.getNewParent().equals(metaContact))
        {
            MetaContactChatTransport chatTransport
                = new MetaContactChatTransport( this,
                                                evt.getProtoContact());

            sessionRenderer.addChatTransport(chatTransport);
        }
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactMoved</tt> method.
     * When a proto contact is moved, updates the "send via" selector box.
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        MetaContactChatTransport chatTransport = null;

        if (evt.getOldParent().equals(metaContact))
        {
            protoContactRemoved(evt);
        }
        else if (evt.getNewParent().equals(metaContact))
        {
            chatTransport
                = new MetaContactChatTransport( this,
                                                evt.getProtoContact());

            sessionRenderer.addChatTransport(chatTransport);
        }
    }

    /**
     * Implements <tt>MetaContactListListener.protoContactRemoved</tt> method.
     * When a proto contact is removed, updates the "send via" selector box.
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        if (evt.getOldParent().equals(metaContact))
        {
            Contact protoContact = evt.getProtoContact();

            for (ChatTransport chatTransport : chatTransports)
            {
                if(((MetaContactChatTransport) chatTransport).getContact()
                        .equals(protoContact))
                {
                    sessionRenderer.removeChatTransport(chatTransport);
                    break;
                }
            }
        }
    }

    /**
     * Returns the <tt>ChatContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt> to search for
     * @return the <tt>ChatContact</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     */
    private ChatContact findChatContactByMetaContact(MetaContact metaContact)
    {
        for (ChatContact chatContact : chatParticipants)
        {
            Object chatSourceContact = chatContact.getDescriptor();
            if (chatSourceContact instanceof MetaContact)
            {
                MetaContact metaChatContact = (MetaContact) chatSourceContact;
                if (metaChatContact.equals(metaContact))
                    return chatContact;
            }
            else
            {
                assert chatSourceContact instanceof ChatRoomMember;
                ChatRoomMember metaChatContact = (ChatRoomMember) chatSourceContact;
                Contact contact = metaChatContact.getContact();
                MetaContact parentMetaContact
                        = GuiActivator.getContactListService()
                        .findMetaContactByContact(contact);
                if(parentMetaContact != null
                    && parentMetaContact.equals(metaContact))
                return chatContact;
            }
        }

        return null;
    }

    /**
     * Disposes this chat session.
     */
    public void dispose()
    {
        if (metaContactListService != null)
            metaContactListService.removeMetaContactListListener(this);

        for (ChatTransport chatTransport : chatTransports)
            chatTransport.dispose();
    }

    /**
     * Returns the <tt>ChatSessionRenderer</tt> that provides the connection
     * between this chat session and its UI.
     *
     * @return The <tt>ChatSessionRenderer</tt>.
     */
    public ChatSessionRenderer getChatSessionRenderer()
    {
        return sessionRenderer;
    }

    /**
     * Returns the descriptor of this chat session.
     *
     * @return the descriptor of this chat session.
     */
    public Object getDescriptor()
    {
        return metaContact;
    }

    /**
     * Returns <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if this contact is persistent, otherwise
     * returns <code>false</code>.
     */
    public boolean isDescriptorPersistent()
    {
        if(metaContact == null)
            return false;

        Contact defaultContact = metaContact.getDefaultContact(
                        OperationSetBasicInstantMessaging.class);

        if(defaultContact == null)
            return false;

        ContactGroup parent = defaultContact.getParentContactGroup();

        boolean isParentPersist = true;
        boolean isParentResolved = true;
        if(parent != null)
        {
            isParentPersist = parent.isPersistent();
            isParentResolved = parent.isResolved();
        }

        if(!defaultContact.isPersistent() &&
           !defaultContact.isResolved() &&
           !isParentPersist &&
           !isParentResolved)
        {
           return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Implements the <tt>ChatPanel.getChatStatusIcon</tt> method.
     *
     * @return the status icon corresponding to this chat room
     */
    public ImageIcon getChatStatusIcon()
    {
        PresenceStatus status
            = this.metaContact.getDefaultContact().getPresenceStatus();

        return new ImageIcon(Constants.getStatusIcon(status));
    }

    /**
     * Returns the avatar icon of this chat session.
     *
     * @return the avatar icon of this chat session.
     */
    public byte[] getChatAvatar()
    {
        return metaContact.getAvatar();
    }

    public void protoContactModified(ProtoContactEvent evt)
    {}

    /**
     *  Implements ChatSession#isContactListSupported().
     */
    public boolean isContactListSupported()
    {
        return false;
    }

    public void addChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            if (!chatTransportChangeListeners.contains(l))
                chatTransportChangeListeners.add(l);
        }
    }

    public void removeChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (chatTransportChangeListeners)
        {
            chatTransportChangeListeners.remove(l);
        }
    }
}
