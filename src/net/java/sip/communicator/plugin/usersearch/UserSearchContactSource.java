/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
