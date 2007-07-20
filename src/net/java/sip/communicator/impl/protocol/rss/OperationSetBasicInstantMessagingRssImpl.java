/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import net.java.sip.communicator.util.*;

/**
 * Instant messaging functionalites for the Rss protocol.
 *
 * @author Jean-Albert Vescovo
 * @author Mihai Balan
 */
public class OperationSetBasicInstantMessagingRssImpl
    implements OperationSetBasicInstantMessaging,
               RegistrationStateChangeListener
{
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicInstantMessagingRssImpl.class);
    /**
     * Currently registered message listeners.
     */
    private Vector messageListeners = new Vector();

    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceRssImpl opSetPersPresence = null;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceRssImpl parentProvider = null;

    /**
     * The timer used in order to refresh one or more rss feeds
     */
    private Timer timer = null;

    /**
     * The value corresponding to the time in ms
     * of the rss refreshing period (here 5min)
     */
    final int PERIOD_REFRESH_RSS = 300000;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     * @param opSetPersPresence the currently valid
     * <tt>OperationSetPersistentPresenceRssImpl</tt> instance.
     */
    public OperationSetBasicInstantMessagingRssImpl(
                ProtocolProviderServiceRssImpl        provider,
                OperationSetPersistentPresenceRssImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;

        parentProvider.addRegistrationStateChangeListener(this);
        if(parentProvider.isRegistered())
        {
            createTimer();
        }
    }

    /**
     * Registers a MessageListener with this operation set so that it gets
     * notifications of successful message delivery, failure or reception of
     * incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to register.
     */
    public void addMessageListener(MessageListener listener)
    {
        if(!messageListeners.contains(listener))
            messageListeners.add(listener);
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *   subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageRssImpl(new String(content),
                                  contentType,
                                  contentEncoding,
                                  subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return new MessageRssImpl(messageText,
                                  "text/html",
                                  DEFAULT_MIME_ENCODING,
                                  null);
    }

    /**
     * Unregisteres <tt>listener</tt> so that it won't receive any further
     * notifications upon successful message delivery, failure or reception
     * of incoming messages..
     *
     * @param listener the <tt>MessageListener</tt> to unregister.
     */
    public void removeMessageListener(MessageListener listener)
    {
        messageListeners.remove(listener);
    }

    /**
     * Updates the rss feed associated with rssContact. If the update has been
     * requested by the user the method would fire a message received event
     * even if there are no new items.
     *
     * @param rssContact the <tt>contact</tt> to send query
     * @param userRequestedUpdate indicates whether the query is triggered by
     * the user or by a scheduled timer task.
     */
    private void submitRssQuery(ContactRssImpl rssContact,
                                boolean userRequestedUpdate)
    {
        Message msg;
        boolean newName = false;
        boolean newDate = false;
        boolean update = false;
        String newDisplayName = new String();
        String oldDisplayName = new String();

        RssFeedReader rssFeed = rssContact.getRssFeedReader();
        try
        {
            rssFeed.retrieveFlow();
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to retrieve RSS flow. Error was: "
                         + ex.getMessage()
                         , ex);
            return;
        }


        //we recover the feed's old name
        oldDisplayName = rssContact.getDisplayName();

        //we change the contact's displayName according to the feed's title
        newDisplayName = rssFeed.getTitle();
        if (! (newDisplayName.equals(oldDisplayName)))
        {
            newName = true;
        }
        rssContact.setDisplayName(newDisplayName);

        //we create the message containing the new items retrieved
        msg = createMessage(rssFeed.feedToString(rssContact.getLastItemDate()));

        //if a newer date is available for the current feed/contact looking
        //the date of each item of the feed retrieved, we update this date
        if (rssFeed.getLastItemPubDate()
                .compareTo(rssContact.getLastItemDate()) > 0)
        {
            rssContact.setDate(rssFeed.getLastItemPubDate());
            newDate = true;
            update = true;
        }

        //if we have a new date or a new name on this feed/contact, we fire
        //that the contact has his properties modified in order to save it
        if (newName || newDate)
            this.opSetPersPresence.fireContactPropertyChangeEvent(
                ContactPropertyChangeEvent.
                PROPERTY_DISPLAY_NAME, rssContact,
                oldDisplayName, newDisplayName);

        //if the feed has been updated or if the user made a request on a
        //specific feed/contact, we fire a new message containing the new items
        //to the user
        if(update || userRequestedUpdate)
            fireMessageReceived(msg, rssContact);
    }

    /**
     * To refresh all rss feeds registered as contacts
     */
    public void refreshAllRssFeeds()
    {
         Vector rssContactList = new Vector();
         rssContactList = opSetPersPresence.getContactListRoot().
             getRssURLList(rssContactList);
         Iterator rssContact = rssContactList.iterator();
         while(rssContact.hasNext())
         {
            submitRssQuery((ContactRssImpl)rssContact.next(), false);
         }
    }

    /**
     * To refresh a specific rss feed specified as param.
     *
     * @param rssURL the <tt>contact</tt> to be refreshed
     */
    public void refreshRssFeed( ContactRssImpl rssURL)
    {
        submitRssQuery(rssURL, true);
    }

    /**
     * Creating the timer permitting the refresh of rss feeds
     */
    public void createTimer()
    {
        if (timer != null )
            return;

        logger.trace("Creating rss timer and task.");
        RssTimerRefreshFeed refresh = new RssTimerRefreshFeed(this);
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(refresh, 100, PERIOD_REFRESH_RSS);

        logger.trace("Done.");
    }

     /**
     * Cancel the timer if the user switch to the OFFLINE status
     */
    public void stopTimer(){
        this.timer.cancel();
    }

    /**
     * Retrieve the feeds for a new Rss Feed just added as persistent contact
     *
     * @param contact the <tt>Contact</tt> added
     */
    public void threadedContactFeedUpdate(ContactRssImpl contact)
    {
        RssThread rssThr = new RssThread(this, contact);
        rssThr.start();
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param to the <tt>Contact</tt> to send <tt>message</tt> to
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying ICQ stack is not
     *   registered and initialized.
     * @throws IllegalArgumentException if <tt>to</tt> is not an instance
     *   belonging to the underlying implementation.
     */
    public void sendInstantMessage(Contact to, Message message)
        throws  IllegalStateException,
                IllegalArgumentException
    {
        if( !(to instanceof ContactRssImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not a Rss contact."
               + to);

        //refresh the present rssFeed "to"
        Message msg = new MessageRssImpl("Refreshing feed...",
            DEFAULT_MIME_TYPE, DEFAULT_MIME_ENCODING, null);
        
        fireMessageDelivered(msg,to);
            
        threadedContactFeedUpdate((ContactRssImpl)to);
    }


    /**
     * Notifies all registered message listeners that a message has been
     * delivered successfully to its addressee..
     *
     * @param message the <tt>Message</tt> that has been delivered.
     * @param to the <tt>Contact</tt> that <tt>message</tt> was delivered to.
     */
    private void fireMessageDelivered(Message message, Contact to)
    {
        MessageDeliveredEvent evt
            = new MessageDeliveredEvent(message, to, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageDelivered(evt);
        }
    }

    /**
     * Notifies all registered message listeners that a message has been
     * received.
     *
     * @param message the <tt>Message</tt> that has been received.
     * @param from the <tt>Contact</tt> that <tt>message</tt> was received from.
     */
    private void fireMessageReceived(Message message, Contact from)
    {
        MessageReceivedEvent evt
            = new MessageReceivedEvent(message, from, new Date());

        Iterator listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList(messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MessageListener listener
                = (MessageListener) listeners.next();

            listener.messageReceived(evt);
        }
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
        return false;
    }

    /**
     * Determines whether the protocol supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the protocol supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else if(contentType.equals("text/html"))
            return true;
        else
           return false;
    }

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderServiceRssImpl</tt>
     * instance that this operation set belongs to.
     */
    public ProtocolProviderServiceRssImpl getParentProvider()
    {
        return this.parentProvider;
    }

    /**
     * Returns a reference to the presence operation set instance used by our
     * source provider.
     *
     * @return a reference to the <tt>OperationSetPersistentPresenceRssImpl</tt>
     * instance used by this provider.
     */
    public OperationSetPersistentPresenceRssImpl getOpSetPersPresence()
    {
        return this.opSetPersPresence;
    }

    /**
     * The method is called by the ProtocolProvider whenver a change in the
     * registration state of the corresponding provider has occurred. We use
     * it to start and stop the timer that periodically checks for rss updates.
     *
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            if (timer == null)
            {
                createTimer();
            }
       }
       else if(timer != null)
       {
            timer.cancel();
            timer = null;
       }
    }

}
