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
package net.java.sip.communicator.plugin.phonenumbercontactsource;

import net.java.sip.communicator.service.contactsource.*;

/**
 * The <tt>PhoneNumberContactSource</tt> is a source of phone numbers coming
 * from the server stored contact info of all contacts for all protocol
 * providers.
 *
 * @author Yana Stamcheva
 */
public class PhoneNumberContactSource
    implements ContactSourceService
{
    /**
     * Returns DEFAULT_TYPE to indicate that this contact source is a default
     * source.
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
        return PNContactSourceActivator.getResources().getI18NString(
            "plugin.phonenumbercontactsource.DISPLAY_NAME");
    }

    /**
     *  Creates query for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, -1);
    }

    /**
     *  Creates query for the given <tt>queryString</tt>.
     *
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    public ContactQuery createContactQuery( String queryString,
                                            int contactCount)
    {
        if (queryString == null)
            queryString = "";

        PhoneNumberContactQuery contactQuery
            = new PhoneNumberContactQuery(this, queryString, contactCount);

        return contactQuery;
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return -1;
    }
}
