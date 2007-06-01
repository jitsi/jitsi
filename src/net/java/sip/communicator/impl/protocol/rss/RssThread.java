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

/**
 * Instant messaging functionalites for the Rss protocol.
 *
 * @author Jean-Albert Vescovo
 */
public class RssThread 
    extends Thread
{
     private OperationSetBasicInstantMessagingRssImpl opSet;
     private ContactRssImpl rssFeed = null;
     private boolean newContact = false;
     private boolean aloneUpdate = false;
     
    /** Creates a new instance of RssThread 
     * @param opSet the OperationSetBasicInstantMessagingRssImpl instance that
     * is managing the rss protocol.
     */
    public RssThread(OperationSetBasicInstantMessagingRssImpl opSet) 
    {
        this.opSet = opSet;
        this.start();
    }
    
    /** Creates a new instance of RssThread 
     * @param opSet the OperationSetBasicInstantMessagingRssImpl instance that
     * is managing the rss protocol.
     * @param rssFeed the contact that the thread is going to do a query
     * @param newContact newContact
     * @param aloneUpdate aloneUpdate
     */
    public RssThread(OperationSetBasicInstantMessagingRssImpl opSet,
                     ContactRssImpl rssFeed,
                     boolean newContact,
                     boolean aloneUpdate)
    {
        this.opSet = opSet;
        this.rssFeed = rssFeed;
        this.newContact = newContact;
        this.aloneUpdate = aloneUpdate;
        this.start();
    }
     
    /**
     * The task executed by the thread
     * If no rss contact given as parameter, the query is launched for all contacts
     */
    public void run()
    {
        try
        {
            if(this.rssFeed == null)
                this.opSet.refreshRssFeed();
            else
                this.opSet.refreshRssFeed(this.rssFeed,this.newContact,this.aloneUpdate);
        }
        catch(Exception exc)
        {
            exc.printStackTrace();
        }
    }
}
