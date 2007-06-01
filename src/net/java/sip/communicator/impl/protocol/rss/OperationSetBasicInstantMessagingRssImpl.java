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

import java.awt.*;
import java.awt.event.*;

/**
 * Instant messaging functionalites for the Rss protocol.
 *
 * @author Jean-Albert Vescovo
 */
public class OperationSetBasicInstantMessagingRssImpl
    implements OperationSetBasicInstantMessaging
{    
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
     * of the rss refreshing period (here 10min)
     */
    final int PERIOD_REFRESH_RSS = 600000;
            
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
        return new MessageRssImpl(new String(content), contentType
                                       , contentEncoding, subject);
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
        
        return new MessageRssImpl(messageText, DEFAULT_MIME_TYPE
                                        , DEFAULT_MIME_ENCODING, null);
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
     * Looks for a RSS feed specified as contact
     *
     * @param rssContact the <tt>contact</tt> to send query
     * @param newContact the <tt>boolean</tt> to now if it's a new feed/contact
     * @param aloneUpdate the <tt>boolean</tt> to know if it's 
     *          a query just for one feed/contact
     */
    private void submitRssQuery(ContactRssImpl rssContact, 
                                boolean newContact, 
                                boolean aloneUpdate)
    {
        Message msg;
        boolean newName = false;
        boolean newDate = false;
        boolean update = false;
        Date lastQueryDate = null;        
        String newDisplayName = new String();
        String oldDisplayName = new String();
        
        //we instantiate a new RssFeedReader which will contain the feed retrieved
        RssFeedReader rssFeed = new RssFeedReader(rssContact.getAddress());        
        
        //we parse the feed/contact
        rssFeed.recupFlux();                
        
        if(rssFeed.getFeed() == null)
        {
            msg = createMessage("No RSS feed available at URL "+ rssContact.getAddress());
        }else
        {
            //we recover the feed's old name 
            if(newContact)
                oldDisplayName = rssContact.getDisplayName();
            else
                oldDisplayName = rssFeed.getTitle();
            
            //we change the contact's displayName according to the feed's title            
            newDisplayName = rssFeed.getTitle();
            if(!(newDisplayName.equals(oldDisplayName)))
            {
                newName = true;                
            }
            rssContact.setDisplayName(newDisplayName);
            
            //Looking for a date representing the last item retrieved on this feed
            //we look after a date saving in the contact's parameters (i.e. in the
            // file contactlist.xml)
            if(rssContact.getDate() != null)
                lastQueryDate = rssContact.getDate();
            
            //we create the message containing the new items retrieved
            msg = createMessage(rssFeed.getPrintedFeed(lastQueryDate));            
                               
            //if a newer date is avalaible for the current feed/contact looking the
            // date of each item of the feed retrieved, we update this date
            if(rssFeed.getUltimateItemDate() != null)
            {
                if(lastQueryDate != null)
                {
                    if(rssFeed.getUltimateItemDate().compareTo(lastQueryDate)>0)
                    {
                        rssContact.setDate(rssFeed.getUltimateItemDate());
                        newDate = true;
                        update = true;
                    }
                }
                else
                {
                    rssContact.setDate(rssFeed.getUltimateItemDate());
                    newDate = true;
                    update = true;
                }
            }
            else
                update = true;
           
            //if we have a new date or a new name on this feed/contact, we fire that
            // the contact has his properties modified in order to save it
            if(newName || newDate)
                this.opSetPersPresence.fireContactPropertyChangeEvent(
                                ContactPropertyChangeEvent.
                                PROPERTY_DISPLAY_NAME, rssContact,
                                oldDisplayName, newDisplayName);
        }
        
        //if the feed has been updated or if the user made a request on a specific
        //feed/contact, we fire a new message containing the new items to the user
        if(update || aloneUpdate)
            fireMessageReceived(msg, rssContact);
    }
      
    /**
     * To refresh all rss feeds registered as contacts
     */
    public void refreshRssFeed()
    {
         Vector rssContactList = new Vector();
         rssContactList = opSetPersPresence.getContactListRoot().
             getRssURLList(rssContactList);
         Iterator rssContact = rssContactList.iterator();
         while(rssContact.hasNext())
         {
            submitRssQuery((ContactRssImpl)rssContact.next(), false, false);
         }
    }
    
    /**
     * To refresh a specific rss feed specified as param
     *
     * @param rssURL the <tt>contact</tt> to be refreshed
     * @param newContact 
     * @param aloneUpdate 
     */
    public void refreshRssFeed( ContactRssImpl rssURL, 
                                boolean newContact, 
                                boolean aloneUpdate)
    {
        submitRssQuery(rssURL, newContact, aloneUpdate);
    }

    /**
     * Creating the timer permitting the refresh of rss feeds
     */
    public void createTimer()
    {
        RssTimerRefreshFeed refresh = new RssTimerRefreshFeed(this);
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(refresh, 0, PERIOD_REFRESH_RSS);
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
    public void newContact(ContactRssImpl contact)
    {
        RssThread rssThr = new RssThread(this,contact, true, true);
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
        
        MessageDeliveredEvent msgDeliveredEvt
            = new MessageDeliveredEvent(message, to, new Date());

        //refresh the present rssFeed "to"        
        fireMessageDelivered(message,to);
        RssThread rssThr = new RssThread(this, (ContactRssImpl)to, false, true);
    }
    
    /**
     * In case the to the <tt>to</tt> Contact corresponds to another rss
     * protocol provider registered with SIP Communicator, we deliver
     * the message to them, in case the <tt>to</tt> Contact represents us, we
     * fire a <tt>MessageReceivedEvent</tt>, and if <tt>to</tt> is simply
     * a contact in our contact list, then we simply echo the message.
     *
     * @param message the <tt>Message</tt> the message to deliver.
     * @param to the <tt>Contact</tt> that we should deliver the message to.
     */
    private void deliverMessage(Message message, ContactRssImpl to)
    {
        String userID = to.getAddress();

        //if the user id is owr own id, then this message is being routed to us
        //from another instance of the rss provider.
        if (userID.equals(this.parentProvider.getAccountID().getUserID()))
        {
            //check who is the provider sending the message
            String sourceUserID
                = to.getProtocolProvider().getAccountID().getUserID();

            //check whether they are in our contact list
            Contact from = opSetPersPresence.findContactByID(sourceUserID);


            //and if not - add them there as volatile.
            if(from == null)
            {
                from = opSetPersPresence.createVolatileContact(sourceUserID);
            }

            //and now fire the message received event.
            fireMessageReceived(message, from);
        }
        else
        {
            //if userID is not our own, try an check whether another provider
            //has that id and if yes - deliver the message to them.
            ProtocolProviderServiceRssImpl rssProvider
                = this.opSetPersPresence.findProviderForRssUserID(userID);
            if(rssProvider != null)
            {
                OperationSetBasicInstantMessagingRssImpl opSetIM
                    = (OperationSetBasicInstantMessagingRssImpl)
                        rssProvider.getOperationSet(
                            OperationSetBasicInstantMessaging.class);
                opSetIM.deliverMessage(message, to);
            }
            else
            {
                //if we got here then "to" is simply someone in our contact
                //list so let's just echo the message.
                fireMessageReceived(message, to);
            }
        }
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
        return true;
    }
    
    public ProtocolProviderServiceRssImpl getParentProvider(){
        return this.parentProvider;
    }
    
    public OperationSetPersistentPresenceRssImpl getOpSetPersPresence(){
        return this.opSetPersPresence;
    }
}
