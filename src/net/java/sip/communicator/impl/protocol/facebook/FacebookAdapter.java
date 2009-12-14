/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.apache.http.*;
import org.json.*;

/**
 * Adapter for the Facebook protocol. This class works as a bridge between the
 * Facebook specific classes and the sip communication interfaces. It manages
 * the lifecycle of the classes responsible of accessing Facebook servers and
 * also manage the necessary {@link Thread threads} used to update the state
 * with the latest server changes.
 * 
 * @author Dai Zhiwei
 * @author Lubomir Marinov
 * @author Edgar Poce
 */
public class FacebookAdapter
    implements FacebookSessionListener
{
    private static Logger logger = Logger.getLogger(FacebookAdapter.class);

    private final OperationSetBasicInstantMessagingFacebookImpl
        basicInstantMessaging;

    /**
     * Parent service provider
     */
    private final ProtocolProviderServiceFacebookImpl parentProvider;

    private final OperationSetPersistentPresenceFacebookImpl persistentPresence;

    /**
     * The Facebook session
     */
    private FacebookSession session;

    private final OperationSetTypingNotificationsFacebookImpl
        typingNotifications;

    /**
     * Cache of typing notifications
     */
    private final Map<String, TypingNotificationRecord> typingNotificationRecord
        = new HashMap<String, TypingNotificationRecord>();

    /**
     * Adapter for each Facebook Chat account.
     * 
     * @param parentProvider the parent service provider
     * @param persistentPresence
     * @param basicInstantMessaging
     * @param typingNotifications
     */
    public FacebookAdapter(
        ProtocolProviderServiceFacebookImpl parentProvider,
        OperationSetPersistentPresenceFacebookImpl persistentPresence,
        OperationSetBasicInstantMessagingFacebookImpl basicInstantMessaging,
        OperationSetTypingNotificationsFacebookImpl typingNotifications)
    {
        this.parentProvider = parentProvider;
        this.persistentPresence = persistentPresence;
        this.basicInstantMessaging = basicInstantMessaging;
        this.typingNotifications = typingNotifications;
    }

    /**
     * Get the facebook id of this account
     * 
     * @return the facebook id of this account
     */
    public String getUID()
    {
        return this.session.getUid();
    }

    /**
     * Get the parent service provider
     * 
     * @return parent service provider
     */
    public ProtocolProviderServiceFacebookImpl getParentProvider()
    {
        return parentProvider;
    }

    /**
     * Initializes the {@link FacebookSession},<br>
     * Initializes the {@link Thread}s to update the buddy list and to poll
     * messages
     * 
     * @param email
     * @param password
     * @return true if the user is logged in
     * @throws IOException
     * @throws BrokenFacebookProtocolException
     */
    public synchronized boolean initialize(
            final String email,
            final String password)
    throws OperationFailedException
    {
        if (this.session != null && this.session.isLoggedIn())
            return true;

        logger.info("initializing facebook adapter. account");
        try
        {
            this.session = new FacebookSession();
            boolean loggedIn = session.login(email, password);
            if (loggedIn)
                session.addListener(this);
            return loggedIn;
        }
        catch (Exception e)
        {
            throw
                new OperationFailedException(
                        "unable to initialize adapter",
                        FacebookErrorException.kError_Login_GenericError,
                        e);
        }
    }

    /**
     * Post typing notification to the given contact.
     * 
     * @param notifiedContact
     *            the contact we want to notify
     * @param typingState
     *            our current typing state(SC)
     * @throws HttpException
     *             the http exception
     * @throws IOException
     *             IO exception
     * @throws JSONException
     *             JSON parsing exception
     * @throws Exception
     *             the general exception
     */
    public void postTypingNotification(Contact notifiedContact, int typingState)
        throws HttpException,
               IOException,
               JSONException,
               Exception
    {
        TypingNotificationRecord record
            = typingNotificationRecord.get(notifiedContact.getAddress());
        if (record == null)
        {
            record = new TypingNotificationRecord(-1l, -1);
            synchronized (typingNotificationRecord)
            {
                typingNotificationRecord
                    .put(notifiedContact.getAddress(), record);
            }
        }

        if (record.getTime() < System.currentTimeMillis() - 1000)
        {
            FacebookOutgoingTypingNotification msg
                = new FacebookOutgoingTypingNotification(session);
            msg.setAddress(notifiedContact.getAddress());
            msg.setTypingState(typingState);
            msg.send();
            record.setTime(System.currentTimeMillis());
            record.setType(typingState);
        }
    }

    public void setStatusMessage(String statusMessage)
            throws OperationFailedException
    {
        try
        {
            this.session.setStatusMessage(statusMessage);
        }
        catch (IOException e)
        {
            throw
                new OperationFailedException(
                        "unable to change facebook status message",
                        -1,
                        e);
        }
    }

    public synchronized void shutdown()
    {
        if (session != null)
        {
            logger.info("shutting down facebook adapter");
            session.logout();
        }
    }

    /**
     * Post message to someone.
     *
     * @param message message to post
     * @param to contact
     * @return null if postMessage succeed, <tt>MessageDeliveryFailedEvent</tt>
     * otherwise
     */
    public synchronized MessageDeliveryFailedEvent postMessage(
            Message message,
            Contact to)
    {
        FacebookOutgoingMailboxMessage msg
            = new FacebookOutgoingMailboxMessage(session);
        msg.setAddress(to.getAddress());
        msg.setContent(message.getContent());
        msg.setSubject(message.getSubject());
        msg.setUid(message.getMessageUID());
        try
        {
            msg.send();
            return null;
        }
        catch (BrokenFacebookProtocolException e)
        {
            logger.error(e);
            return new MessageDeliveryFailedEvent(message, to, -1, System
                    .currentTimeMillis(), e.getMessage());
        }
        catch (IOException e)
        {
            logger.warn(e);
            return new MessageDeliveryFailedEvent(message, to, -1, System
                    .currentTimeMillis(), e.getMessage());
        }
        catch (FacebookErrorException e)
        {
            return new MessageDeliveryFailedEvent(message, to, e.getCode());
        }
    }

    public MessageDeliveryFailedEvent postFacebookChatMessage(
            Message message,
            Contact to)
    {
        FacebookOutgoingChatMessage msg
            = new FacebookOutgoingChatMessage(session);
        msg.setAddress(to.getAddress());
        msg.setContent(message.getContent());
        msg.setMessageUid(message.getMessageUID());
        try
        {
            msg.send();
            return null;
        }
        catch (BrokenFacebookProtocolException e)
        {
            logger.error(e);
            return new MessageDeliveryFailedEvent(message, to, -1, System
                    .currentTimeMillis(), e.getMessage());
        }
        catch (IOException e)
        {
            logger.warn(e);
            return new MessageDeliveryFailedEvent(message, to, -1, System
                    .currentTimeMillis(), e.getMessage());
        }
        catch (FacebookErrorException e)
        {
            return new MessageDeliveryFailedEvent(message, to, e.getCode());
        }
    }

    /**
     * Promotes the incoming message to the GUI
     * 
     * @see FacebookSessionListener#onIncomingChatMessage(FacebookMessage)
     */
    public void onIncomingChatMessage(FacebookMessage msg)
    {
        if (!msg.getFrom().equals(this.session.getUid()))
            basicInstantMessaging.receivedInstantMessage(msg);
    }

    /**
     * Promotes the incoming notification to the GUI
     * 
     * @see FacebookSessionListener#onIncomingTypingNotification(String, int)
     */
    public void onIncomingTypingNotification(String buddyUid, int state)
    {
        if (!buddyUid.equals(this.session.getUid()))
        {
            Contact fromContact = persistentPresence.findContactByID(buddyUid);
            if (fromContact == null)
                fromContact
                    = persistentPresence.createVolatileContact(buddyUid);

            int typingState = OperationSetTypingNotifications.STATE_UNKNOWN;
            switch (state)
            {
            case 1:
                typingState = OperationSetTypingNotifications.STATE_TYPING;
                break;
            case 0:
                typingState = OperationSetTypingNotifications.STATE_STOPPED;
                break;
            default:
                typingState = OperationSetTypingNotifications.STATE_UNKNOWN;
            }

            typingNotifications
                .receivedTypingNotification(fromContact, typingState);
        }
    }

    /**
     * notifies SC that the connection is lost
     */
    public void onFacebookConnectionLost() {
        if (parentProvider.isRegistered())
        {
            try
            {
                parentProvider.unregister();
            }
            catch (OperationFailedException e)
            {
                logger.error("unable to unregister", e);
            }
        }

        // tag all the buddies as offline
        persistentPresence
            .setPresenceStatusForAllContacts(FacebookStatusEnum.OFFLINE);
    }

    public void onBuddyListUpdated()
    {
        for (FacebookUser user : this.session.getBuddyList().getBuddies())
        {
            PresenceStatus newStatus;
            if (user.isOnline && user.isIdle)
                newStatus = FacebookStatusEnum.IDLE;
            else if (user.isOnline)
                newStatus = FacebookStatusEnum.ONLINE;
            else
                newStatus = FacebookStatusEnum.OFFLINE;

            persistentPresence.setPresenceStatusForContact(user.uid, newStatus);
        }
    }

    public FacebookSession getSession()
    {
        return session;
    }

    private static class TypingNotificationRecord
    {
        private long time;
        private int type;

        public TypingNotificationRecord(long time, int type)
        {
            this.time = time;
            this.type = type;
        }

        public long getTime()
        {
            return time;
        }

        public int getType()
        {
            return type;
        }

        public void setTime(long time)
        {
            this.time = time;
        }

        public void setType(int type)
        {
            this.type = type;
        }
    }
}
