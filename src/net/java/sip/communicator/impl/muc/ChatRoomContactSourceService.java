/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.muc;

import java.util.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * Contact source service for chat rooms.
 * 
 * @author Hristo Terezov
 */
public class ChatRoomContactSourceService
    implements ContactSourceService
{
    /**
     * The <tt>List</tt> of <tt>ChatRoomQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<ChatRoomQuery> queries
        = new LinkedList<ChatRoomQuery>();
    
    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return DEFAULT_TYPE;
    }

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    public String getDisplayName()
    {
        return MUCActivator.getResources().getI18NString(
            "service.gui.CHAT_ROOMS");
    }

    /**
     * Queries this contact source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    @Override
    public ContactQuery queryContactSource(String queryString)
    {
        return queryContactSource(queryString, -1);
    }

    /**
     * Queries this contact source for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    @Override
    public ContactQuery queryContactSource(String queryString, int contactCount)
    {
        if (queryString == null)
            queryString = "";
        ChatRoomQuery contactQuery
            = new ChatRoomQuery(queryString, contactCount, this);

        synchronized (queries)
        {
            queries.add(contactQuery);
        }
    
        boolean queryHasStarted = false;
    
        try
        {
            contactQuery.start();
            queryHasStarted = true;
        }
        finally
        {
            if (!queryHasStarted)
            {
                synchronized (queries)
                {
                    if (queries.remove(contactQuery))
                        queries.notify();
                }
            }
        }
        return contactQuery;
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    @Override
    public int getIndex()
    {
        return -1;
    }

}
