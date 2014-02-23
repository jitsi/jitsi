/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.usersearch;

import net.java.sip.communicator.service.contactsource.*;

/**
 * The user search contact source.
 *
 * @author Hristo Terezov
 */
public class UserSearchContactSource
    implements ContactSourceService
{

    /**
     * The time to wait before actually start the searching. If the query is
     * canceled before the timeout the query won't be started.
     */
    public static final long QUERY_DELAY = 500;

    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    public int getType()
    {
        return SEARCH_TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return UserSearchActivator.getResources().getI18NString(
            "plugin.usersearch.USER_SEARCH");
    }

    @Override
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, -1);
    }

    @Override
    public ContactQuery createContactQuery(String queryString, int contactCount)
    {
        if(queryString == null)
            queryString = "";

        UserSearchQuery query = new UserSearchQuery(queryString, this);
        return query;
    }

    @Override
    public int getIndex()
    {
        return 0;
    }

}
