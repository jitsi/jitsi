/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.text.*;
import java.util.*;

/**
 * The <code>RssItemKey</code> is used to encapsulate information pertaining to
 * the last item retrieved from a RSS feed. It can be used with both feeds that
 * provide and that don't provide a date for their contents.
 * 
 * @author Mihai Balan
 */
public class RssItemKey
   implements Comparable
{
    /***
     * Date of the last show post. If it cannot be used, it's null. 
     */
    private Date itemDate = null;
    
    /***
     * URI (link) of the last shown post. If it's not used, it's null.
     */
    private String itemUri = null;
    
    /***
     * Flag to mark whether date is used to mark items (<code>true</code>) or
     * URI (<code>false</code>)
     */
    private boolean usesDate = true;

    private static SimpleDateFormat formatter = 
       new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");

    /***
     * Creates a RssContactSettings object that by default uses date to 
     * identify feed items, with the last item date the current date/time.
     */
    public RssItemKey()
    {
        itemDate = new Date();
        itemUri = null;
        usesDate = true;
    }
    
    /***
     * Creates a RssContactSettings object that uses date to identify feed
     * items, with the last item date specified by the <tt>lastPostDate</tt>
     * parameter.
     * @param itemDate date/time of the item
     */
    public RssItemKey(Date itemDate)
    {
        this.itemDate = itemDate;
        this.itemUri = null;
        this.usesDate = true;
    }
    
    /***
     * Creates a RssContactSettings object that uses URI to identify feed
     * items, with the last item URI specified by the <tt>lastPostUri</tt>
     * @param itemUri URI/link of the item
     */
    public RssItemKey(String itemUri)
    {
        this.itemUri = itemUri;
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
        RssItemKey result = new RssItemKey();

        while (reader.hasMoreTokens())
        {
            String data[] = reader.nextToken().split("=", 2);
            if (data[0].equals("itemDate"))
            {
                if (data.length == 2)
                {
                    try
                    {
                        result.itemDate = formatter.parse(data[1]);
                    }
                    catch (ParseException e)
                    {
                        result.itemDate = null;
                        //XXX: logger.error("Could not parse date: " + data[1]);
                    }
                }
                else
                    result.itemDate = null;
            }
            
            if (data[0].equals("itemUri"))
            {
                if (data.length == 2)
                    result.itemUri = data[1];
                else
                    result.itemUri = null;
            }
       
            if (data[0].equals("usesDate"))
            {
                if (data.length == 2)
                    result.usesDate = Boolean.valueOf(data[1]).booleanValue();
                else
                    result.usesDate = result.itemDate == null;
            }
        }
        
        if (result.itemDate == null && result.itemUri == null)
            return null;
        else
            return result;
    }

    /***
     * Returns the textual representation of the settings object. This can
     * be easily de-serialized with a call to <code>deserialize()</code>.
     * 
     * @see #deserialize(String)
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        
        result.append("itemDate=");
        result.append(itemDate == null ? 
            "" : formatter.format(itemDate));
        result.append(";");

        result.append("itemUri=");
        result.append(itemUri == null ? "" : itemUri);
        result.append(";");

        result.append("usesDate=");
        result.append(usesDate);
        result.append(";");

        return result.toString();
    }
    
    /***
     * Serializes current key to a textual representation.
     * 
     * @return String containing the textual representation of the current key.
     */
    public String serialize()
    {
        return this.toString();
    }

    public int compareTo(Object o)
    {
        RssItemKey obj = (RssItemKey)o;
   
        if (obj == null)
            throw new ClassCastException("Can only compare item keys.");
   
        if (this.usesDate())
        {
            if (obj.usesDate())
                return this.itemDate.compareTo(obj.itemDate);
            else
                return 0;
        }
        else
        {
            if(obj.usesDate())
                return 0;
            else
                return this.itemUri.compareToIgnoreCase(obj.itemUri);
        }
    }
}