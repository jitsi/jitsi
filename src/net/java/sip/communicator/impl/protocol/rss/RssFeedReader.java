/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.net.*;
import java.util.*;
import java.text.*;
import java.io.*;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Wrapper class for the ROME functionality used in the RSS implementation in
 * SIP Communicator. 
 * The class provides the means for identifying feed items, formatting and 
 * displaying the actual feed items.
 *
 * @author Jean-Albert Vescovo
 * @author Mihai Balan
 */
public class RssFeedReader
{
    /**
     * The URL of the contact/feed, used to make a TCP query for the XML file
     * containing the actual RSS feed.
     */
    private URL rssURL;

    /**
     * The title of the feed, which will be used as the display name
     * of the contact/feed.
     */
    private String title = null;

    /**
     * The object charged to retrieve the feed incoming from the relevant
     * server.
     */
    private SyndFeed feed = null;

    /**
     * Key identifying the retrieved item in this feed.
     */
    private RssItemKey lastItemKey = null;

    /**
     * An array of <tt>SyndEntry</tt> objects which will contain all the items 
     * retrieved from the feed.
     */
    private SyndEntry[] items = null;

    /**
     * A comparator that we use when sorting the items array.
     */
    private SyndEntryComparator syndEntryComparator = new SyndEntryComparator();

     /**
     * Creates an instance of a RSS reader with the specified string used
     * as an URL for the actual feed.
     *
     * @param contactRssURL the URL of this feed.
     */
    public RssFeedReader(URL contactRssURL)
    {
        this.rssURL = contactRssURL;

        /* TODO should retrieve this from a resource file.*/
        this.title = "No feed avalaible !";
    }

    /**
     * Refreshes the RSS feed associated with this reader, stores the feed items
     * and updates item identification information so that items that were 
     * displayed once aren't displayed again.
     *
     * @throws OperationFailedException with code ILLEGAL_ARGUMENT
     * @throws FileNotFoundException if the feed does not exist any more.
     */
    public void retrieveFlow()
        throws OperationFailedException, FileNotFoundException
    {
        SyndFeedInput input = new SyndFeedInput();
        
        try
        {
            this.feed = input.build(new XmlReader(rssURL));
        } 
        catch (FileNotFoundException ex)
        {
            //We are handling that in OpSetBasicInstantMessaging as it indicates
            //a feed that has most likely been removed
            throw ex;
        }
        catch (IOException ex)
        {
            throw new OperationFailedException(
                "Failed to create and XmlReader for url: " + rssURL
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }
        catch(FeedException fex)
        {
            throw new OperationFailedException(
                "Failed to create and XmlReader for url: " + rssURL
                , OperationFailedException.GENERAL_ERROR
                , fex);
        }


        feed.getEntries();

        this.title = this.feed.getTitle();

        // retrieve items
        items = (SyndEntry[]) this.feed.getEntries().toArray(new SyndEntry[0]);
        
        if (items.length == 0)
        {
            lastItemKey = new RssItemKey(new Date(0));
            return;
        }

        if (items[items.length - 1].getPublishedDate() != null)
        {
            Arrays.sort(items, syndEntryComparator);
            lastItemKey =
                new RssItemKey(items[items.length - 1].getPublishedDate()); 
        }
        else
        {
            lastItemKey = new RssItemKey(items[0].getLink());
        }
    }

    /**
     * Returns the textual representation of the feed's items with regard to the
     * key of the last item shown to the user. The items are sorted in reverse
     * chronological order, if possible.
     * @param itemKey key identifying the last item retrieved.
     * @return textual representation of the feed items.
     */
    public synchronized String feedToString(RssItemKey itemKey)
    {
        String newsAbstract = null;
        StringBuffer printedFeed = new StringBuffer();

        // used for performance reasons
        Date itemDate = itemKey.getItemDate();
        String itemUri = itemKey.getItemUri();
        
        int i, markerPosition = -1;
        
        // TODO move this message in a resources file.
        if (items.length == 0)
            return "<b>No items currently available for this feed !</b><br>";

        if (lastItemKey.usesDate())
        {
            for (i = items.length - 1; i >= 0; i--)
            {
                if (items[i].getPublishedDate()
                    .compareTo(itemDate) > 0)
                {
                    // Get the abstract of the news.
                    newsAbstract = getNewsAbstract(items[i]);
                    // Forge the news text to be displayed.
                    printedFeed.insert(0,
                        "<a href=\""+items[i].getLink()+"\">"
                        + "<strong>"+ items[i].getTitle() + "</strong>"
                        + "</a>"
                        + "<br>"
                        + newsAbstract
                        + "<hr>");
                }
                else
                {
                    if (i == items.length - 1)
                    {
                        printedFeed
                            .append("<strong>No new articles in your feed since"
                                + " last update.</strong><br>");
                    }
                    break;
                }
            }
            
            printedFeed
                .append ("<em>Send anything to refresh this feed...</em><br>\n");
            
            return printedFeed.toString();
            }
            else
            {
                for(i = 0; i < items.length; i++)
                {
                    if(itemUri != null && 
                        itemUri.equalsIgnoreCase(items[i].getLink()))
                    {
                        markerPosition = i;
                        break;
                    }
                }
                
                if (markerPosition == -1)
                    markerPosition = items.length;                    
                
                // the main assumption here is that even in case the items don't present
                // a date, they are usually sorted by the publishing date, the most
                // recent first. This way, if the last displayed item is the first in
                // the feed, we infer that all the feed has been previously displayed.
                if (markerPosition != 0)
                {
                    for(i = 0; i < markerPosition; i++)
                    {
                        // Get the abstract of the news.
                        newsAbstract = getNewsAbstract(items[i]);
                        // Forge the news text to be displayed.
                        printedFeed.insert(0,
                            "<a href=\""+items[i].getLink()+"\">"
                            + "<strong>"+ items[i].getTitle() + "</strong>"
                            + "</a>"
                            + "<br>"
                            + newsAbstract
                            + "<hr>");
                    }
                }
                else
                {
                    printedFeed
                        .append("<strong>No new articles in your feed since"
                            + " last update.</strong><br>");
                }
                printedFeed
                    .append ("<em>Send anything to refresh this feed...</em><br>\n");
                
                return printedFeed.toString();
        }
    }
    
    /**
     * The function retrieves the abstract (textual description) of a feed item
     * or an empty string otherwise. It takes care of all format specific data
     * and returns a nicely formatted <tt>String</tt>
     * 
     * @param syndEntry - Feed entry for which to retrieve the abstract (text)
     * @return String representation of the news abstract or an empty string if
     * no such data could be found.
     */
    private String getNewsAbstract(SyndEntry syndEntry)
    {
        StringBuffer newsAbstract = new StringBuffer();
        List contents;
        
        // get item contents
        contents = syndEntry.getContents();
        if (!contents.isEmpty())
        {
            Iterator it = contents.iterator();
            while (it.hasNext())
            {
                newsAbstract.append(((SyndContent)it.next()).getValue());
            }
        }
        
        // format the contents
        if (newsAbstract.toString().length() != 0)
            return newsAbstract.toString();
        else
        {
            if (syndEntry.getDescription() != null)
            {
                if(syndEntry.getDescription().getValue() != null)
                    newsAbstract.append(syndEntry.getDescription().getValue());
            }
        }
        return newsAbstract.toString();
    }

    /**
     * Return the key for the last item retrieved.
     * 
     * @return key of the last item retrieved.
     */
    public RssItemKey getLastItemKey()
    {
        return this.lastItemKey;
    }

    /**
     * Assigns a new key as the key of the last retrieved item.
     * 
     * @param key new key for the last retrieved item.
     */
    public void setLastItemKey(RssItemKey key)
    {
        this.lastItemKey = key;
    }

    /**
     * Returns a ChannelIF that can be used to know if a feed exists indeed.
     *
     * @return a ChannelIF containing the result of a query on a RSS server.
     */
    public SyndFeed getFeed()
    {
        return this.feed;
    }

    /**
     * Returns a Date giving the publication date of the feed on the relevant
     * server.
     *
     * In most case, this date doesn't exist on the server. Not used at this
     * time in this implementation.
     *
     * @return a Date representing the publication date of the feed.
     */
    public Date getPubDate()
    {
        return this.feed.getPublishedDate();
    }

    /**
     * Returns a String used as a display name.
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
     * We'll prefer to use the title of the feed as display name.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress()
    {
        return rssURL.toString();
    }

    private class SyndEntryComparator implements Comparator
    {
        /**
         * Compares its two items for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument has a date
         * which precedes, is equal or is greater the second.
         * <p>
         * @param o1 the first item to be compared.
         * @param o2 the second item to be compared.
         * @return a negative integer, zero, or a positive integer as the
         *         first item has a date that is before is equal to or is
         *         after the second.
         * @throws ClassCastException if one of the objects is not a
         *         SyndEntry instance.
         */
        public int compare(Object o1, Object o2)
        {
            Date date1 = ( (SyndEntry) o1).getPublishedDate();
            if (date1 == null)
                date1 = new Date();

            Date date2 = ( (SyndEntry) o2).getPublishedDate();
            if (date2 == null)
                date2 = new Date();

            return date1.compareTo(date2);
        }
    }    
}