/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.net.*;
import java.util.*;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The class used for using the Informa Library into the RSS protocol
 *
 * @author Jean-Albert Vescovo
 */
public class RssFeedReader
{
    /**
     * The URL of the contact/feed, used to make a tcp query toward
     * the .xml file containing the items of the feed.
     */
    private URL rssURL;

    /**
     * The title of the feed, which will be used as the displayname
     * of the contact/feed.
     */
    private String title = null;

    /**
     * The object charged to retrieve the feed incoming from the relavant
     * server.
     */
    private SyndFeed feed = null;

    /**
     * The last update date of this feed.
     */
    private Date lastItemPubDate = new Date(0l);

    /**
     * An array of SyndEntry which will contain all the items retrieved from
     * the feed.
     */
    private SyndEntry[] items = null;

    /**
     * A comparator that we use when sorting the items array.
     */
    private SyndEntryComparator syndEntryComparator = new SyndEntryComparator();

     /**
     * Creates an instance of a rss feed with the specified string used
     * as an url to contact the relevant server.
     *
     * @param contactRssURL the url of this feed.
     */
    public RssFeedReader(URL contactRssURL)
    {
        this.rssURL = contactRssURL;

        /** @todo should retrieve this from a resource file.*/
        this.title = "No feed avalaible !";
    }

    /**
     * Refreshes the rss feed associated with this reader, sorts all items by
     * reverse chronological order and stores them locally so that they could
     * be retrieved by the getPrintedFeed() method.
     *
     * @throws OperationFailedException with code ILLEGAL_ARGUMENT
     */
    public void retrieveFlow()
        throws OperationFailedException
    {
        //the most important thing in this protocol: we parse the rss feed
        //using the Rome library
        SyndFeedInput input = new SyndFeedInput();

        try
        {
            this.feed = input.build(new XmlReader(rssURL));
        }
        catch (Exception ex)
        {
            throw new OperationFailedException(
                "Failed to create and XmlReader for url: " + rssURL
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }

        feed.getEntries();

        this.title = this.feed.getTitle();

        //we retrieve the items and sort them by reverse
        //chronological order
        items = (SyndEntry[]) (this.feed.getEntries()
                               .toArray(new SyndEntry[0]));
        sortItems();

        //if we don't understand the date format we don't want to handle
        //this feed
        if( items[items.length -1].getPublishedDate() == null)
        {
            throw new OperationFailedException(
                "We can't retrieve dates for RSS flow \""
                + title
                + "\" ("
                +rssURL
                +")"
                , OperationFailedException.GENERAL_ERROR);
        }


        //store the date of the most recent item
        setLastItemPubDate( items[items.length -1].getPublishedDate() );
    }

    /**
     * Returns a String containing the message to send to the user after
     * a successful query on a rss server:
     *<p>
     * - if we have no items, we return "No items found on this feed !"<br>
     * - if we can't read a date in these items, we return the last 10 items
     *   of the feed<br>
     * - if we can read a date, we just return the items which have a date
     *   earlier than the lastQueryDate, and "No new articles in your feed
     *   since last update." if it isn't new item since lastQueryDate.
     *</p><p>
     * We signal to the user ("Send anything to refresh this feed...") that he
     * can send anything to refresh the present contact/feed.
     *</p>
     * @param latestRetrievedItemDate the date to compare with that of the items
     * retrieved.
     *
     * @return a String containing all messages published on this rss source
     * since lastQueryDate or a message stating lack of new messages.
     */
    public synchronized String feedToString(Date latestRetrievedItemDate)
    {
        StringBuffer printedFeed = new StringBuffer();

        if (items.length == 0)
            return "No items currently available for this feed !";

        //go through the items list in reverse order so that we could stop
        //as soon as we reach items that we've already shown to the user.
        for (int i = items.length - 1; i >= 0; i--)
        {
            if (items[i].getPublishedDate()
                .compareTo(latestRetrievedItemDate) > 0)
            {
                printedFeed.insert(
                    0
                    , "\nAt " + items[i].getPublishedDate()
                    + " - " + items[i].getTitle()
                    + "\nLink: " + items[i].getLink() + "\n\n");
            }
            else
            {
                if (i == items.length - 1)
                {
                    printedFeed
                        .append("\n\nNo new articles in your feed since"
                                + " last update.");
                }
                break;
            }
        }

        printedFeed.append ("\n\nSend anything to refresh this feed...");
        return printedFeed.toString();
    }

    /**
     * Sorts the items retrieved from the rss contact/feed associated with this
     * reader. The method uses a bubble sort algorithm.
     */
    private void sortItems()
    {
        Arrays.sort(items, syndEntryComparator);
    }

    /**
     * Returns a Date that can be used to know the most recent item in a
     * retrieved feed.
     *
     * @return the feed's Date representing the nearest item's date never
     * retrieved on this feed.
     */
    public Date getLastItemPubDate()
    {
        return this.lastItemPubDate;
    }

    /**
     * Returns a Date that can be used to know the most recent item in a
     * retrieved feed.
     * <p>
     * This method just gives the date of the first element of the array of
     * ItemIF previously sorted.
     *
     * @param date the publish date of the latest item retrieved with this
     * reader.
     */
    private void setLastItemPubDate(Date date)
    {
        this.lastItemPubDate = date;
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
        return rssURL.toString();
    }

    private class SyndEntryComparator implements Comparator
    {
        /**
         * Compares its two items for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument has a date
         * which preceeds, is equal or is greater the second.
         * <p>
         * @param o1 the first item to be compared.
         * @param o2 the second item to be compared.
         * @return a negative integer, zero, or a positive integer as the
         *         first item has a date that is before is equal to or is
         *         after the swcond.
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
