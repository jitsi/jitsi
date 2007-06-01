/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The class used for using the Informa Library into the RSS protocol
 *
 * @author Jean-Albert Vescovo
 */
public class RssFeedReader
{
    
    /**
     * The id of the contact/feed, used to make a tcp query toward
     * the .xml file containing the items of the feed.
     */
    private String address;
    
    /**
     * The title of the feed, which will be used as the displayname
     * of the contact/feed.
     */
    private String title;
    
    /**
     * The object charged to retrieve the feed incoming from the relavant server.
     */
    private SyndFeed feed;
    
    /**
     * The last update date of this feed.
     */
    private Date ultimateItemDate = null;
    
    /**
     * An array of SyndEntry which will contain all the items retrieved from the feed.
     */
    private SyndEntry[] items;
    
     /**
     * Creates an instance of a rss feed with the specified string used
     * as an url to contact the relevant server.
     *
     * @param address the url of this feed.
     */
    public RssFeedReader(String address)
    {
        this.address = address;
        this.feed = null;
        this.title = "No feed avalaible !";        
    }
    
    /**
     * To refresh this rss contact/feed registered as contact
     * Moreover, we sort the items by reverse chronological order after
     * insert them into an Array
     */
    public void recupFlux()
    {
        try
        {
            URL rssURL = new URL(this.address);
            
            //the most important thing in this protocol: we parse the rss feed
            //using the Rome library
            SyndFeedInput input = new SyndFeedInput();
            this.feed = input.build(new XmlReader(rssURL));
            this.title = this.feed.getTitle(); 
            
            //we retrieve the items and sort them by reverse chronological order
            items = (SyndEntry[])(this.feed.getEntries().toArray(new SyndEntry[0]));
            sortItems();
            
            //we retrieve the date of the most recent item
            this.ultimateItemDate = findUltimateItemDate();
        }
        catch(Exception ex) 
        {
            ex.printStackTrace();
            System.out.println("ERROR: "+ex.getMessage());
        }     
    }
    
    /**
     * Returns a String containing the message to send to the user after 
     * a successful query on a rss server:
     *
     * - if we have no items, we return "No items found on this feed !"
     * - if we can't read a date in these items, we return the last 10 items of the feed
     * - if we can read a date, we just return the items which have a date earlier than
     *   the lastQueryDate, and "No new articles in your feed since last update." if it isn't
     *   new item since lastQueryDate.
     *
     * We signal to the user ("Send anything to refresh this feed...") that he can send anything
     * to refresh the present contact/feed.
     *
     * @param lastQueryDate the date to compare with that of the items retrieved.
     * @return String string
     */
    public String getPrintedFeed(Date lastQueryDate)
    {
        boolean more = true;
        int i=0,nbNewItem = 0;
        String printedFeed = new String();
                
        if(items.length > 0)
        {
            while((i<items.length)&&more)
            {
                if((items[i].getPublishedDate() != null) && (lastQueryDate != null))
                {
                    if(items[i].getPublishedDate().compareTo(lastQueryDate)>0)
                    {
                        printedFeed += "\nAt " + items[i].getPublishedDate()+" - " +
                                items[i].getTitle() +
                                "\nLink: " + items[i].getLink() + "\n\n";
                        nbNewItem++;
                    }
                    else{
                        more = false;
                        if(nbNewItem == 0) printedFeed += 
                            "\n\nNo new articles in your feed since last update.";
                    }
                }
                else{
                    if(items[i].getPublishedDate() != null)
                        printedFeed += "\nAt " + items[i].getPublishedDate();
                    
                    printedFeed += "\n" + items[i].getTitle() + 
                        "\nLink: "+items[i].getLink()+"\n\n";
                    
                    if(i == 10) more = false;
                }
                i++;
            }
            printedFeed += ("\n\nSend anything to refresh this feed...");
        }
        else
        {
            printedFeed += "No items found on this feed !";
        }
        return printedFeed;
    }    
    
    /**
     * To sort the items retrieved from the rss contact/feed registered as contact
     * We use for that a bubble sort algorithm
     */
    public void sortItems()
    {
        int i;
        int size = items.length;
        SyndEntry temp;
        boolean inversion;        
        do
        {
            inversion=false;
            for(i = 0; i < size - 1; i++)
            {
                if((items[i].getPublishedDate() != null) && (items[i+1].getPublishedDate()!=null))
                    if(items[i].getPublishedDate().compareTo(items[i+1].getPublishedDate())<0)
                    {
                        temp = items[i];
                        items[i] = items[i+1];
                        items[i+1] = temp;
                        inversion=true;
                    }
            }
            size--;
        }while(inversion);
    }
    
    /**
     * Returns a Date that can be used to know the most recent item in a retrieved feed.
     *
     * @return the feed's Date representing the nearest item's date never retrieved on this feed.
     */
    public Date getUltimateItemDate()
    {
        return this.ultimateItemDate;
    }
    
    /**
     * Returns a Date that can be used to know the most recent item in a retrieved feed.
     * 
     * This method just gives the date of the first element of the array of ItemIF previously
     * sorted.
     *
     * @return a Date representing the nearest item's date.
     */
    private Date findUltimateItemDate()
    {
        if(items[0].getPublishedDate() != null)
            this.ultimateItemDate = items[0].getPublishedDate();
        return this.ultimateItemDate;
    }
    
    /**
     * Returns a ChannelIF that can be used to know if a feed exists indeed.
     *
     * @return a ChannelIF containing the result of a query on a rss server.
     */
    public SyndFeed getFeed() 
    {
        return this.feed;
    }
    
    /**
     * Returns a Date giving the publication date of the feed on the relevant server.
     * 
     * In most case, this date doesn't exist on the server. Not used at this time in this
     * implementation.
     *
     * @return a Date representing the publication date of the feed.
     */
    public Date getPubDate() 
    {
        return this.feed.getPublishedDate();
    }
    
    /**
     * Returns a String used as a displayname.
     *
     * @return a String title representing the feed/contact.
     */
    public String getTitle() 
    {
        return this.title;
    }
    
    /**
     * Returns a String that can be used for identifying the contact.
     * 
     * We'll prefer to use the title of the feed as displayname.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress() 
    {
        return this.address;
    }    
}