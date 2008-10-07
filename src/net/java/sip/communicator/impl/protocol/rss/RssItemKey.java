/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import com.sun.syndication.feed.synd.*;

import net.java.sip.communicator.util.*;

import java.text.*;
import java.util.*;

/**
 * The <code>RssItemKey</code> is used to encapsulate information pertaining to
 * the last item retrieved from a RSS feed. It can be used with both feeds that
 * provide and that don't provide a date for their contents.
 * 
 * @author Mihai Balan
 * @author Vincent Lucas
 */
public class RssItemKey
   //implements Comparable
{
    private static final Logger logger =
                Logger.getLogger(OperationSetPersistentPresenceRssImpl.class);

    /***
     * Date of the last show post. If it cannot be used, it's null. 
     */
    private Date itemDate;
    
    /***
     * URI (link) of the last shown post. If it's not used, it's null.
     */
    private String itemUri;
    
    /***
     * Flag to mark whether date is used to mark items (<code>true</code>) or
     * URI (<code>false</code>)
     */
    private boolean usesDate;

    private static SimpleDateFormat formatter = 
       new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
    
    /***
     * Creates a RssContactSettings object that uses date to identify feed
     * items, with the last item date specified by the <tt>lastPostDate</tt>
     * parameter.
     * @param itemDate date/time of the item
     */
    public RssItemKey(SyndEntry entry)
    {
        this.itemUri = entry.getUri();
        this.itemDate = entry.getPublishedDate();
        this.usesDate = (this.itemDate != null);
    }

    public RssItemKey(Date date)
    {
        this.itemUri = null;
        this.itemDate = date;
        this.usesDate = true;
    }

    public RssItemKey(String uri)
    {
        this.itemUri = uri;
        this.itemDate = null;
        this.usesDate = false;
    }
    
    /**
     * Determines if the current key uses <code>Date</code> as a means of
     * identification or not. Usually if true is returned this also implies that
     * <code>getItemUri() == null</code> so this should be used with care.
     * Similarly, if <code>false</code> is returned, one should assume that
     * <code>getItemDate() == null</code>.
     * 
     * @return <code>true</code> if date is used for identification,
     * <code>false</code> otherwise.
     * @see #getItemDate()
     * @see #getItemUri()
     */
    public boolean usesDate()
    {
        return this.usesDate;
    }
    
    /**
     * Returns the date that is used as a key. Note that null can also be
     * returned in case <code>usesDate() == false</code>.
     * 
     * @return date field of the key.
     * @see #usesDate()
     */
    public Date getItemDate()
    {
        return this.itemDate;
    }
    
    /**
     * Returns the URI that is used as a key. Note that null can also be
     * returned in case <code>usesDate() == true</code>.
     * 
     * @return URI field of the key.
     * @see #usesDate()
     */
    public String getItemUri()
    {
        return this.itemUri;
    }

    /***
     * Used for restoring the key information from a textual representation.
     *
     * @param settings textual representation of the stored data
     * @return the result rss item
     */
    public static RssItemKey deserialize(String settings)
    {
        StringTokenizer reader = new StringTokenizer(settings, ";");
        Date date = null;
        String uri = null;
        boolean useInitialized = false;
        boolean isDateUsed = false;

        while (reader.hasMoreTokens())
        {
            String data[] = reader.nextToken().split("=", 2);

            if (data[0].equals("itemDate"))
            {
                if (data.length == 2)
                {
                    try
                    {
                        date = formatter.parse(data[1]);
                    }
                    catch (ParseException e)
                    {
                        logger.error("Failed to deserialize RSS settings. Parse date error: " +
                                settings,
                                e);
                        return null;
                    }
                }
                else
                {
                    logger.error("Failed to deserialize RSS settings. Parse itemDate error: " +
                            settings,
                            new Exception("Parse itemDate error: " + settings));
                    return null;
                }
            }
            else if (data[0].equals("itemUri"))
            {
                if (data.length == 2)
                {
                    uri = data[1];
                }
                else
                {
                    logger.error("Failed to deserialize RSS settings. Parse itemUri error: " +
                            settings,
                            new Exception("Parse itemUri error: " + settings));
                    return null;
                }
            }
            else if (data[0].equals("usesDate"))
            {
                if (data.length == 2)
                {
                    isDateUsed = Boolean.valueOf(data[1]).booleanValue();
                    useInitialized = true;
                }
                else
                {
                    logger.error("Failed to deserialize RSS settings. Parse usesDate error: " +
                            settings,
                            new Exception("Parse usesDate error: " + settings));
                    return null;
                }
            }
        }
        
        if(useInitialized)
        {
            if(isDateUsed && date != null)
            {
                return new RssItemKey(date);
            }
            else if(!isDateUsed && uri != null)
            {
                return new RssItemKey(uri);
            }
        }
        return null;
    }

    /**
     * Serializes current key to a textual representation.
     * 
     * @return String containing the textual representation of the current key.
     */
    public String serialize()
    {
        StringBuffer result = new StringBuffer();
        
        result.append("itemDate=");
        result.append(
                itemDate == null ?
                "" : formatter.format(itemDate));
        result.append(";");

        result.append("itemUri=");
        result.append(
                itemUri == null ?
                "" : itemUri);
        result.append(";");

        result.append("usesDate=");
        result.append(usesDate);
        result.append(";");

        return result.toString();
    }

    /**
     * Returns the textual representation of the settings object. This can
     * be easily de-serialized with a call to <code>deserialize()</code>.
     * 
     * @see #deserialize(String)
     */
    public String toString()
    {
        return this.serialize();
    }
    

    public int compareTo(RssItemKey obj)
    {
        if(obj == null)
        {
            return 1;
        }
        if (this.usesDate())
        {
            if (obj.usesDate())
            {
                return this.itemDate.compareTo(obj.itemDate);
            }
            else
            {
                return 0;
            }
        }
        else
        {
            if(obj.usesDate())
            {
                return 0;
            }
            else
            {
                return this.itemUri.compareToIgnoreCase(obj.itemUri);
            }
        }
    }
}
