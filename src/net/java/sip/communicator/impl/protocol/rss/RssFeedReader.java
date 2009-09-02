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
import net.java.sip.communicator.util.*;

/**
 * Wrapper class for the ROME functionality used in the RSS implementation in
 * SIP Communicator. 
 * The class provides the means for identifying feed items, formatting and 
 * displaying the actual feed items.
 *
 * @author Jean-Albert Vescovo
 * @author Mihai Balan
 * @author Vincent Lucas
 */
public class RssFeedReader
{
    private static final Logger logger
        = Logger.getLogger(ContactRssImpl.class);
    /**
     * The URL of the contact/feed, used to make a TCP query for the XML file
     * containing the actual RSS feed.
     */
    private URL rssURL;

    /**
     * The title of the feed, which will be used as the display name
     * of the contact/feed.
     */
    private String title = "No feed avalaible !";

    /**
     * The object charged to retrieve the feed incoming from the relevant
     * server.
     */
    private SyndFeed feed = null;

    /**
     * Key identifying the retrieved item in this feed.
     */
    private RssItemKey lastItemKey;

    /**
     * An array of <tt>SyndEntry</tt> objects which will contain all the items 
     * retrieved from the feed.
     */
    private SyndEntry[] items = null;

    /**
     * Tells us if the feeds is available or not. In other words, if the feed is
     * ONLINE or OFFLINE.
     */
    private boolean isFeedJoinable = false;

     /**
     * Creates an instance of a RSS reader with the specified string used
     * as an URL for the actual feed.
     *
     * @param contactRssURL the URL of this feed.
     */
    public RssFeedReader(URL contactRssURL)
        throws OperationFailedException, FileNotFoundException
    {
        this.rssURL = contactRssURL;
        this.lastItemKey  = null;
        // Try to retrieve the feed and to complete this instanciation.
        this.retrieveFlow();
    }

    /**
     * Refreshes the RSS feed associated with this reader, and does not store
     * the feed items (see getNewFeeds for this).
     *
     * @throws OperationFailedException with code ILLEGAL_ARGUMENT
     * @throws FileNotFoundException if the feed does not exist any more.
     */
    private void retrieveFlow()
        throws OperationFailedException, FileNotFoundException
    {

        SyndFeedInput input = new SyndFeedInput();
        
        try
        {
            this.feed = input.build(new XmlReader(rssURL));
        } 
        catch (FileNotFoundException ex)
        {
            this.isFeedJoinable = false;
            //We are handling that in OpSetBasicInstantMessaging as it indicates
            //a feed that has most likely been removed
            throw ex;
        }
        catch (IOException ex)
        {
            this.isFeedJoinable = false;
            throw new OperationFailedException(
                "Failed to create and XmlReader for url: " + rssURL
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }
        catch(FeedException fex)
        {
            this.isFeedJoinable = false;
            throw new OperationFailedException(
                "Failed to create and XmlReader for url: " + rssURL
                , OperationFailedException.GENERAL_ERROR
                , fex);
        }
        this.isFeedJoinable = true;

        this.feed.getEntries();

        this.title = this.feed.getTitle();

        // retrieve items
        this.items = (SyndEntry[]) this.feed.getEntries().toArray(new SyndEntry[0]);
        Arrays.sort(items, new SyndEntryComparator());
    }

    /**
     * Returns the textual representation of the feed's items with regard to the
     * key of the last item shown to the user. The items are sorted in reverse
     * chronological order, if possible.
     * @return textual representation of the feed items.
     */
    public synchronized String getNewFeeds()
        throws OperationFailedException, FileNotFoundException
    {
        String newsAbstract = null;
        StringBuffer printedFeed = new StringBuffer();
        
        int i;
        boolean hasSomeNews = false;

        // Try to retrieve the feed and to complete this instanciation.
        this.retrieveFlow();
        
        for (i = items.length - 1;
                i >= 0 &&  (new RssItemKey(items[i])).compareTo(lastItemKey) != 0;
                --i)
        {
            hasSomeNews = true;
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
        if (!hasSomeNews)
        {
            return null;
        }
        lastItemKey = new RssItemKey(items[items.length - 1]);
        printedFeed
            .append ("<em>Send anything to refresh this feed...</em><br>\n");
        return printedFeed.toString();
    }

    public String getNoNewFeedString()
    {
        return "<strong>No new articles in your feed since"
            + " last update.</strong><br>"
            + "<em>Send anything to refresh this feed...</em><br>\n";
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
    public String getURL()
    {
        return rssURL.toString();
    }

    private static class SyndEntryComparator implements Comparator<SyndEntry>
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
        public int compare(SyndEntry o1, SyndEntry o2)
        {
            Date date1 = o1.getPublishedDate();
            Date date2 = o2.getPublishedDate();

            if (date1 == null || date2 == null)
            {
                return 0;
            }
            return date1.compareTo(date2);
        }
    }    

    public String serialize()
    {
        StringBuffer result = new StringBuffer();

        if(this.lastItemKey != null)
        {
            result.append(lastItemKey.serialize());
        }
        result.append("displayName=");
        result.append(this.title);
        result.append(";");

        return result.toString();
    }

    public static RssFeedReader deserialize(URL contactRssURL, String settings)
        throws OperationFailedException, FileNotFoundException
    {
        StringTokenizer reader = new StringTokenizer(settings, ";");
        String tmpTitle = null;

        while (reader.hasMoreTokens())
        {
            String data[] = reader.nextToken().split("=", 2);

            if (data[0].equals("displayName"))
            {
                if (data.length == 2)
                {
                    tmpTitle = data[1];
                }
                else
                {
                    logger.error("Failed to deserialize RSS settings. Parse displayName error: " +
                            settings,
                            new Exception("Parse itemUri error: " + settings));
                    return null;
                }
            }
        }
        RssItemKey tmpKey = RssItemKey.deserialize(settings);
        RssFeedReader rssFeedReader = new RssFeedReader(contactRssURL);
        rssFeedReader.lastItemKey  = tmpKey;
        rssFeedReader.title = tmpTitle;

        return rssFeedReader;
    }
}
