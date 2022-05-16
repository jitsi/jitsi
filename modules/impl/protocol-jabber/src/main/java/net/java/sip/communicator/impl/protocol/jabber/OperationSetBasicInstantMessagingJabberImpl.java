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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.carbons.CarbonCopyReceivedListener;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension.Direction;
import org.jivesoftware.smackx.delay.*;
import org.jivesoftware.smackx.message_correct.element.*;
import org.jivesoftware.smackx.message_correct.provider.*;
import org.jivesoftware.smackx.xevent.*;
import org.jivesoftware.smackx.xhtmlim.*;
import org.jivesoftware.smackx.xhtmlim.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.stringprep.*;

import static org.jivesoftware.smack.packet.StanzaError.Condition.*;

/**
 * A straightforward implementation of the basic instant messaging operation
 * set.
 *
 * @author Damian Minkov
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 * @author Emil Ivov
 * @author Hristo Terezov
 */
public class OperationSetBasicInstantMessagingJabberImpl
    extends AbstractOperationSetBasicInstantMessaging
    implements OperationSetMessageCorrection
{
    /**
     * Our class logger
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetBasicInstantMessagingJabberImpl.class);

    /**
     * A table mapping contact addresses to full jids that can be used to
     * target a specific resource (rather than sending a message to all logged
     * instances of a user).
     */
    private final Map<BareJid, StoredThreadID> jids
        = new Hashtable<>();

    /**
     * The most recent full JID used for the contact address.
     */
    private Map<BareJid, Jid> recentJIDForAddress
        = new Hashtable<>();
    /**
     * The smackMessageListener instance listens for incoming messages.
     * Keep a reference of it so if anything goes wrong we don't add
     * two different instances.
     */
    private SmackMessageListener smackMessageListener = null;

    /**
     * Contains the complete jid of a specific user and the time that it was
     * last used so that we could remove it after a certain point.
     */
    public static class StoredThreadID
    {
        /** The time that we last sent or received a message from this jid */
        long lastUpdatedTime;

        /** The last chat used, this way we will reuse the thread-id */
        String threadID;
    }

    /**
     * A prefix helps to make sure that thread ID's are unique across mutliple
     * instances.
     */
    private static String prefix = RandomStringUtils.random(5);

    /**
     * Keeps track of the current increment, which is appended to the prefix to
     * forum a unique thread ID.
     */
    private static long id = 0;

    /**
     * The number of milliseconds that we preserve threads with no traffic
     * before considering them dead.
     */
    private static final long JID_INACTIVITY_TIMEOUT = 10*60*1000;//10 min.

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * The html namespace used as feature
     * XHTMLManager.namespace
     */
    private final static String HTML_NAMESPACE =
        "http://jabber.org/protocol/xhtml-im";

    /**
     * List of filters to be used to filter which messages to handle
     * current Operation Set.
     */
    private List<StanzaFilter> packetFilters = new ArrayList<StanzaFilter>();

    /**
     * Whether carbon is enabled or not.
     */
    private boolean isCarbonEnabled = false;

    /**
     * Creates an instance of this operation set.
     * @param provider a reference to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetBasicInstantMessagingJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;

        packetFilters.add(new GroupMessagePacketFilter());
        packetFilters.add(
            new StanzaTypeFilter(org.jivesoftware.smack.packet.Message.class));

        provider.addRegistrationStateChangeListener(
                        new RegistrationStateListener());

        MessageCorrectProvider extProvider =
                new MessageCorrectProvider();
        ProviderManager.addExtensionProvider(
            MessageCorrectExtension.ELEMENT,
            MessageCorrectExtension.NAMESPACE,
                extProvider);
    }

    /**
     * Create a Message instance with the specified UID, content type
     * and a default encoding.
     * This method can be useful when message correction is required. One can
     * construct the corrected message to have the same UID as the message
     * before correction.
     *
     * @param messageText the string content of the message.
     * @param contentType the MIME-type for <tt>content</tt>
     * @param messageUID the unique identifier of this message.
     * @return Message the newly created message
     */
    public Message createMessageWithUID(
        String messageText, String contentType, String messageUID)
    {
        return new MessageJabberImpl(messageText, contentType,
            DEFAULT_MIME_ENCODING, null, messageUID);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @return the newly created message.
     */
    public Message createMessage(String content, String contentType)
    {
        return createMessage(content, contentType, DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param subject the Subject of the message that we'd like to create.
     * @param encoding the enconding of the message that we will be sending.
     *
     * @return the newly created message.
     */
    @Override
    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        return new MessageJabberImpl(content, contentType, encoding, subject);
    }

    Message createMessage(String content, String contentType,
            String messageUID)
    {
        return new MessageJabberImpl(content, contentType,
                DEFAULT_MIME_ENCODING, null, messageUID);
    }

    /**
     * Determines wheter the protocol provider (or the protocol itself) support
     * sending and receiving offline messages. Most often this method would
     * return true for protocols that support offline messages and false for
     * those that don't. It is however possible for a protocol to support these
     * messages and yet have a particular account that does not (i.e. feature
     * not enabled on the protocol server). In cases like this it is possible
     * for this method to return true even when offline messaging is not
     * supported, and then have the sendMessage method throw an
     * OperationFailedException with code - OFFLINE_MESSAGES_NOT_SUPPORTED.
     *
     * @return <tt>true</tt> if the protocol supports offline messages and
     * <tt>false</tt> otherwise.
     */
    public boolean isOfflineMessagingSupported()
    {
        return true;
    }

    /**
     * Determines wheter the protocol supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        return
            (contentType.equals(DEFAULT_MIME_TYPE)
                || contentType.equals(HTML_MIME_TYPE));
    }

    /**
     * Determines whether the protocol supports the supplied content type
     * for the given contact.
     *
     * @param contentType the type we want to check
     * @param contact contact which is checked for supported contentType
     * @return <tt>true</tt> if the contact supports it and
     * <tt>false</tt> otherwise.
     */
    @Override
    public boolean isContentTypeSupported(String contentType, Contact contact)
    {
        // by default we support default mime type, for other mimetypes
        // method must be overriden
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else if(contentType.equals(HTML_MIME_TYPE))
        {
            Jid contactJid;
            try
            {
                contactJid = JidCreate.from(contact.getAddress());
            }
            catch (XmppStringprepException e)
            {
                return false;
            }

            Jid toJID = recentJIDForAddress.get(contactJid);
            if (toJID == null)
                toJID = contactJid;

            return jabberProvider.isFeatureListSupported(
                        toJID,
                        HTML_NAMESPACE);
        }

        return false;
    }

    /**
     * Remove from our <tt>jids</tt> map all entries that have not seen any
     * activity (i.e. neither outgoing nor incoming messags) for more than
     * JID_INACTIVITY_TIMEOUT. Note that this method is not synchronous and that
     * it is only meant for use by the {@link #getThreadIDForAddress(BareJid)} and
     * {@link #putJidForAddress(Jid, String)}
     */
    private void purgeOldJids()
    {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<BareJid, StoredThreadID>> entries
            = jids.entrySet().iterator();


        while( entries.hasNext() )
        {
            Map.Entry<BareJid, StoredThreadID> entry = entries.next();
            StoredThreadID target = entry.getValue();

            if (currentTime - target.lastUpdatedTime
                            > JID_INACTIVITY_TIMEOUT)
                entries.remove();
        }
    }

    /**
     * Returns the last jid that the party with the specified <tt>address</tt>
     * contacted us from or <tt>null</tt>(or bare jid) if we don't have a jid
     * for the specified <tt>address</tt> yet. The method would also purge all
     * entries that haven't seen any activity (i.e. no one has tried to get or
     * remap it) for a delay longer than <tt>JID_INACTIVITY_TIMEOUT</tt>.
     *
     * @param jid the <tt>jid</tt> that we'd like to obtain a threadID for.
     *
     * @return the last jid that the party with the specified <tt>address</tt>
     * contacted us from or <tt>null</tt> if we don't have a jid for the
     * specified <tt>address</tt> yet.
     */
    String getThreadIDForAddress(BareJid jid)
    {
        synchronized(jids)
        {
            purgeOldJids();
            StoredThreadID ta = jids.get(jid);

            if (ta == null)
                return null;

            ta.lastUpdatedTime = System.currentTimeMillis();

            return ta.threadID;
        }
    }

    /**
     * Maps the specified <tt>address</tt> to <tt>jid</tt>. The point of this
     * method is to allow us to send all messages destined to the contact with
     * the specified <tt>address</tt> to the <tt>jid</tt> that they last
     * contacted us from.
     *
     * @param threadID the threadID of conversation.
     * @param jid the jid (i.e. address/resource) that the contact with the
     * specified <tt>address</tt> last contacted us from.
     */
    private void putJidForAddress(Jid jid, String threadID)
    {
        synchronized(jids)
        {
            purgeOldJids();

            StoredThreadID ta = jids.get(jid.asBareJid());

            if (ta == null)
            {
                ta = new StoredThreadID();
                jids.put(jid.asBareJid(), ta);
            }

            recentJIDForAddress.put(jid.asBareJid(), jid);

            ta.lastUpdatedTime = System.currentTimeMillis();
            ta.threadID = threadID;
        }
    }

    /**
     * Helper function used to send a message to a contact, with the given
     * extensions attached.
     *
     * @param to The contact to send the message to.
     * @param toResource The resource to send the message to or null if no
     * resource has been specified
     * @param message The message to send.
     * @param extensions The XMPP extensions that should be attached to the
     * message before sending.
     * @return The MessageDeliveryEvent that resulted after attempting to
     * send this message, so the calling function can modify it if needed.
     */
    private MessageDeliveredEvent sendMessage(  Contact to,
                                                ContactResource toResource,
                                                Message message,
                                                ExtensionElement[] extensions)
        throws OperationFailedException
    {
        if( !(to instanceof ContactJabberImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not a Jabber contact."
               + to);

        assertConnected();

        MessageBuilder builder = jabberProvider
            .getConnection()
            .getStanzaFactory()
            .buildMessageStanza();

        Jid toJID = null;
        if (toResource != null)
        {
            if(toResource.equals(ContactResource.BASE_RESOURCE))
            {
                toJID = ((ContactJabberImpl) to).getAddressAsJid();
            }
            else
                toJID =
                    ((ContactResourceJabberImpl) toResource).getFullJid();
        }

        if (toJID == null)
        {
            toJID = ((ContactJabberImpl) to).getAddressAsJid();
        }

        //TODO stanza-id is generated automatically. is this ok or is required
        // somewhere else to have it as the Jitsi-internal message id?
        //msg.setStanzaId(message.getMessageUID());
        builder.to(toJID);

        for (ExtensionElement ext : extensions)
        {
            builder.addExtension(ext);
        }

        if (logger.isTraceEnabled())
            logger.trace("Will send a message to:" + toJID
                        + " chat.jid=" + toJID);

        MessageDeliveredEvent msgDeliveryPendingEvt
            = new MessageDeliveredEvent(message, to, toResource);

        MessageDeliveredEvent[] transformedEvents
            = messageDeliveryPendingTransform(msgDeliveryPendingEvt);

        if (transformedEvents == null || transformedEvents.length == 0)
            return null;

        for (MessageDeliveredEvent event : transformedEvents)
        {
            String content = event.getSourceMessage().getContent();

            if (message.getContentType().equals(HTML_MIME_TYPE))
            {
                builder.setBody(Html2Text.extractText(content));

                // Check if the other user supports XHTML messages
                // make sure we use our discovery manager as it caches calls
                if (jabberProvider
                    .isFeatureListSupported(toJID, HTML_NAMESPACE))
                {
                    // Add the XHTML text to the message
                    XHTMLManager.addBody(builder, new XHTMLText(null, "en")
                        .append(content).appendCloseBodyTag());
                }
            }
            else
            {
                // this is plain text so keep it as it is.
                builder.setBody(content);
            }

            // msg.addExtension(new Version());

            if (event.isMessageEncrypted() && isCarbonEnabled)
            {
                CarbonExtension.Private.addTo(builder);
            }

            String threadID = getThreadIDForAddress(toJID.asBareJid());
            if (threadID == null)
                threadID = nextThreadID();

            builder.setThread(threadID)
                .ofType(org.jivesoftware.smack.packet.Message.Type.chat)
                .from(jabberProvider.getConnection().getUser());

            org.jivesoftware.smack.packet.Message msg = builder.build();
            MessageEventManager.addNotificationsRequests(msg, true, false,
                false, true);
            try
            {
                jabberProvider.getConnection().sendStanza(msg);
            }
            catch (NotConnectedException | InterruptedException e)
            {
                throw new OperationFailedException(
                    "Could not send message",
                    OperationFailedException.GENERAL_ERROR,
                    e);
            }

            putJidForAddress(toJID, threadID);
        }

        return new MessageDeliveredEvent(message, to, toResource);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance of ContactImpl.
     */
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException,
               IllegalArgumentException,
               OperationFailedException
    {
        sendInstantMessage(to, null, message);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt>. Provides a default implementation of this method.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param toResource the resource to which the message should be send
     * @param message the <tt>Message</tt> to send.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     * @throws java.lang.IllegalArgumentException if <tt>to</tt> is not an
     * instance belonging to the underlying implementation.
     */
    @Override
    public void sendInstantMessage( Contact to,
                                    ContactResource toResource,
                                    Message message)
        throws IllegalStateException,
               IllegalArgumentException, OperationFailedException
    {
        MessageDeliveredEvent msgDelivered =
            sendMessage(to, toResource, message, new ExtensionElement[0]);

        fireMessageEvent(msgDelivered);
    }

    /**
     * Replaces the message with ID <tt>correctedMessageUID</tt> sent to
     * the contact <tt>to</tt> with the message <tt>message</tt>
     *
     * @param to The contact to send the message to.
     * @param message The new message.
     * @param correctedMessageUID The ID of the message being replaced.
     */
    public void correctMessage(
        Contact to, ContactResource resource,
        Message message, String correctedMessageUID)
        throws OperationFailedException
    {
        ExtensionElement[] exts = new ExtensionElement[1];
        exts[0] = new MessageCorrectExtension(correctedMessageUID);
        MessageDeliveredEvent msgDelivered
            = sendMessage(to, resource, message, exts);
        msgDelivered.setCorrectedMessageUID(correctedMessageUID);
        fireMessageEvent(msgDelivered);
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected()
        throws IllegalStateException
    {
        if (opSetPersPresence == null)
        {
            throw
                new IllegalStateException(
                        "The provider must be signed on the service before"
                            + " being able to communicate.");
        }
        else
            opSetPersPresence.assertConnected();
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERING)
            {
                opSetPersPresence
                    = (OperationSetPersistentPresenceJabberImpl)
                        jabberProvider.getOperationSet(
                                OperationSetPersistentPresence.class);

                if(smackMessageListener == null)
                {
                    smackMessageListener = new SmackMessageListener();
                }
                else
                {
                    // make sure this listener is not already installed in this
                    // connection
                    jabberProvider.getConnection()
                        .removeAsyncStanzaListener(smackMessageListener);
                }

                jabberProvider.getConnection().addAsyncStanzaListener(
                        smackMessageListener,
                        new AndFilter(
                            packetFilters.toArray(
                                new StanzaFilter[packetFilters.size()])));
            }
            else if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        initAdditionalServices();
                    }
                }).start();
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                || evt.getNewState() == RegistrationState.CONNECTION_FAILED
                || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED)
            {
                if(jabberProvider.getConnection() != null)
                {
                    if(smackMessageListener != null)
                        jabberProvider.getConnection().removeAsyncStanzaListener(
                            smackMessageListener);
                }

                smackMessageListener = null;
            }
        }
    }

    /**
     * Initialize carbons.
     */
    private void initAdditionalServices()
    {
        CarbonManager cm = CarbonManager.getInstanceFor(
            jabberProvider.getConnection());
        boolean enableCarbon
            = isCarbonSupported() && !jabberProvider.getAccountID()
            .getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_CARBON_DISABLED,
                false);
        if(enableCarbon)
        {
            try
            {
                cm.enableCarbons();
                cm.addCarbonCopyReceivedListener(new CarbonCopyReceivedListener()
                {

                    @Override
                    public void onCarbonCopyReceived(Direction direction,
                        org.jivesoftware.smack.packet.Message carbonCopy,
                        org.jivesoftware.smack.packet.Message wrappingMessage)
                    {
                        processMessage(carbonCopy, direction == Direction.sent);
                    }
                });
            }
            catch (XMPPException | SmackException | InterruptedException e)
            {
                isCarbonEnabled = false;
                logger.warn("Could not enable carbons", e);
            }
        }
        else
        {
            isCarbonEnabled = false;
        }
    }

    /**
     * Checks whether the carbon is supported by the server or not.
     * @return <tt>true</tt> if carbon is supported by the server and
     * <tt>false</tt> if not.
     */
    private boolean isCarbonSupported()
    {
        try
        {
            return CarbonManager.getInstanceFor(jabberProvider.getConnection())
                .isSupportedByServer();
        }
        catch (XMPPException
            | InterruptedException
            | NoResponseException
            | NotConnectedException e)
        {
           logger.warn("Failed to retrieve carbon support", e);
        }

        return false;
    }

    /**
     * The listener that we use in order to handle incoming messages.
     */
    private class SmackMessageListener
        implements StanzaListener
    {
        /**
         * Handles incoming messages and dispatches whatever events are
         * necessary.
         * @param packet the packet that we need to handle (if it is a message).
         */
        public void processStanza(Stanza packet)
        {
            if(!(packet instanceof org.jivesoftware.smack.packet.Message))
                return;

            org.jivesoftware.smack.packet.Message msg =
                (org.jivesoftware.smack.packet.Message)packet;
            processMessage(msg, false);
        }
    }

    private void processMessage(org.jivesoftware.smack.packet.Message msg,
        boolean isForwardedSentMessage)
    {
        if (msg.getBody() == null)
        {
            return;
        }

        Object multiChatExtension =
            msg.getExtensionElement("x", "http://jabber.org/protocol/muc#user");

        // its not for us
        if(multiChatExtension != null)
            return;

        Jid userFullId
            = isForwardedSentMessage? msg.getTo() : msg.getFrom();

        BareJid userBareID = userFullId.asBareJid();

        boolean isPrivateMessaging = false;
        ChatRoom privateContactRoom = null;
        OperationSetMultiUserChatJabberImpl mucOpSet =
            (OperationSetMultiUserChatJabberImpl)jabberProvider
                .getOperationSet(OperationSetMultiUserChat.class);
        if(mucOpSet != null)
            privateContactRoom = mucOpSet.getChatRoom(userBareID);

        if(privateContactRoom != null)
        {
            isPrivateMessaging = true;
        }

        if(logger.isDebugEnabled())
        {
            if (logger.isDebugEnabled())
                logger.debug("Received from "
                         + userBareID
                         + " the message "
                         + msg.toXML());
        }

        Message newMessage = createMessage(msg.getBody(),
                DEFAULT_MIME_TYPE, msg.getStanzaId());

        //check if the message is available in xhtml
        ExtensionElement ext = msg.getExtension(
                        "http://jabber.org/protocol/xhtml-im");

        if(ext != null)
        {
            XHTMLExtension xhtmlExt
                = (XHTMLExtension)ext;

            //parse all bodies
            StringBuilder messageBuff = new StringBuilder();
            for (CharSequence body : xhtmlExt.getBodies())
            {
                messageBuff.append(body);
            }

            if (messageBuff.length() > 0)
            {
                // we remove body tags around message cause their
                // end body tag is breaking
                // the visualization as html in the UI
                String receivedMessage =
                    messageBuff.toString()
                    // removes body start tag
                    .replaceAll("\\<[bB][oO][dD][yY].*?>","")
                    // removes body end tag
                    .replaceAll("\\</[bB][oO][dD][yY].*?>","");

                // for some reason &apos; is not rendered correctly
                // from our ui, lets use its equivalent. Other
                // similar chars(< > & ") seem ok.
                receivedMessage =
                        receivedMessage.replaceAll("&apos;", "&#39;");

                newMessage = createMessage(receivedMessage,
                        HTML_MIME_TYPE, msg.getStanzaId());
            }
        }

        ExtensionElement correctionExtension =
                msg.getExtension(MessageCorrectExtension.NAMESPACE);
        String correctedMessageUID = null;
        if (correctionExtension != null)
        {
            correctedMessageUID = ((MessageCorrectExtension)
                    correctionExtension).getIdInitialMessage();
        }

        Contact sourceContact
            = opSetPersPresence.findContactByID(
                (isPrivateMessaging? userFullId : userBareID));
        if(msg.getType()
                        == org.jivesoftware.smack.packet.Message.Type.error)
        {
            // error which is multichat and we don't know about the contact
            // is a muc message error which is missing muc extension
            // and is coming from the room, when we try to send message to
            // room which was deleted or offline on the server
            if(isPrivateMessaging && sourceContact == null)
            {
                StanzaError error = msg.getError();
                int errorResultCode
                    = ChatRoomMessageDeliveryFailedEvent.UNKNOWN_ERROR;

                if(error != null && error.getCondition() == forbidden)
                {
                    errorResultCode
                        = ChatRoomMessageDeliveryFailedEvent.FORBIDDEN;
                }

                String errorReason = error.getConditionText();
                ChatRoomMessageDeliveryFailedEvent evt =
                    new ChatRoomMessageDeliveryFailedEvent(
                        privateContactRoom,
                        null,
                        errorResultCode,
                        errorReason,
                        new Date(),
                        newMessage);
                ((ChatRoomJabberImpl)privateContactRoom)
                    .fireMessageEvent(evt);

                return;
            }

            if (logger.isInfoEnabled())
                logger.info("Message error received from " + userBareID);

            int errorResultCode = MessageDeliveryFailedEvent.UNKNOWN_ERROR;
            if (msg.getError() != null)
            {
                if(msg.getError().getCondition() == service_unavailable)
                {
                    org.jivesoftware.smackx.xevent.packet.MessageEvent msgEvent =
                        msg.getExtension("x", "jabber:x:event");
                    if(msgEvent != null && msgEvent.isOffline())
                    {
                        errorResultCode =
                            MessageDeliveryFailedEvent
                                .OFFLINE_MESSAGES_NOT_SUPPORTED;
                    }
                }
            }

            if (sourceContact == null)
            {
                sourceContact = opSetPersPresence.createVolatileContact(
                    userFullId, isPrivateMessaging);
            }

            MessageDeliveryFailedEvent ev
                = new MessageDeliveryFailedEvent(newMessage,
                                                 sourceContact,
                                                 correctedMessageUID,
                                                 errorResultCode);

            // ev = messageDeliveryFailedTransform(ev);

            fireMessageEvent(ev);
            return;
        }
        putJidForAddress(userFullId, msg.getThread());

        // In the second condition we filter all group chat messages,
        // because they are managed by the multi user chat operation set.
        if(sourceContact == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("received a message from an unknown contact: "
                               + userBareID);
            //create the volatile contact
            sourceContact = opSetPersPresence
                .createVolatileContact(
                    userFullId,
                    isPrivateMessaging);
        }

        Date timestamp
            = DelayInformationManager.getDelayTimestamp(msg);
        if (timestamp == null)
        {
            timestamp = new Date();
        }

        ContactResource resource = ((ContactJabberImpl) sourceContact)
                .getResourceFromJid(userFullId);

        EventObject msgEvt = null;
        if(!isForwardedSentMessage)
            msgEvt
                = new MessageReceivedEvent( newMessage,
                                            sourceContact,
                                            resource,
                                            timestamp,
                                            correctedMessageUID,
                                            isPrivateMessaging,
                                            privateContactRoom);
        else
            msgEvt = new MessageDeliveredEvent(newMessage, sourceContact, timestamp);
        // msgReceivedEvt = messageReceivedTransform(msgReceivedEvt);
        fireMessageEvent(msgEvt);
    }

    /**
     * A filter that prevents this operation set from handling multi user chat
     * messages.
     */
    private static class GroupMessagePacketFilter implements StanzaFilter
    {
        /**
         * Returns <tt>true</tt> if <tt>packet</tt> is a <tt>Message</tt> and
         * false otherwise.
         *
         * @param packet the packet that we need to check.
         *
         * @return  <tt>true</tt> if <tt>packet</tt> is a <tt>Message</tt> and
         * false otherwise.
         */
        @Override
        public boolean accept(Stanza packet)
        {
            if(!(packet instanceof org.jivesoftware.smack.packet.Message))
                return false;

            org.jivesoftware.smack.packet.Message msg
                = (org.jivesoftware.smack.packet.Message) packet;

            return
                !msg.getType().equals(
                        org.jivesoftware.smack.packet.Message.Type.groupchat);
        }
    }

    public Jid getRecentJIDForAddress(BareJid address)
    {
        return recentJIDForAddress.get(address);
    }

    /**
     * Returns the inactivity timeout in milliseconds.
     *
     * @return The inactivity timeout in milliseconds. Or -1 if undefined
     */
    public long getInactivityTimeout()
    {
        return JID_INACTIVITY_TIMEOUT;
    }

    /**
     * Adds additional filters for incoming messages. To be able to skip some
     * messages.
     * @param filter to add
     */
    public void addMessageFilters(StanzaFilter filter)
    {
        this.packetFilters.add(filter);
    }

    /**
     * Returns the next unique thread id. Each thread id made up of a short
     * alphanumeric prefix along with a unique numeric value.
     *
     * @return the next thread id.
     */
    public static synchronized String nextThreadID()
    {
        return prefix + Long.toString(id++);
    }
}
