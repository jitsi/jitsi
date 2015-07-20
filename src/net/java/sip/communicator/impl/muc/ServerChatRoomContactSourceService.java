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
package net.java.sip.communicator.impl.muc;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.muc.*;

/**
 * Contact source service for the existing chat rooms on the server.
 * 
 * @author Hristo Terezov
 */
public class ServerChatRoomContactSourceService
    implements ContactSourceService
{
    private ChatRoomProviderWrapper provider = null;
    public ServerChatRoomContactSourceService(ChatRoomProviderWrapper pps)
    {
        provider = pps;
    }

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
            "service.gui.SERVER_CHAT_ROOMS");
    }

    /**
     * Creates query for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    @Override
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, -1);
    }

    /**
     * Creates query for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    @Override
    public ContactQuery createContactQuery(String queryString, int contactCount)
    {
        if (queryString == null)
            queryString = "";
        
        ServerChatRoomQuery contactQuery
            = new ServerChatRoomQuery(queryString, this, provider);
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
