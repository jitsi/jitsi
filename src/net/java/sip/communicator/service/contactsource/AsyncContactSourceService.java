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
package net.java.sip.communicator.service.contactsource;

import java.util.regex.*;

/**
 * Declares the interface of a <tt>ContactSourceService</tt> which performs
 * <tt>ContactQuery</tt>s in a separate <tt>Thread</tt>.
 *
 * @author Lyubomir Marinov
 */
public abstract class AsyncContactSourceService
    implements ExtendedContactSourceService
{
    /**
     * Creates query that searches for <tt>SourceContact</tt>s
     * which match a specific <tt>query</tt> <tt>String</tt>.
     *
     * @param query the <tt>String</tt> which this <tt>ContactSourceService</tt>
     * is being queried for
     * @return a <tt>ContactQuery</tt> which represents the query of this
     * <tt>ContactSourceService</tt> implementation for the specified
     * <tt>String</tt> and via which the matching <tt>SourceContact</tt>s (if
     * any) will be returned
     * @see ContactSourceService#queryContactSource(String)
     */
    public ContactQuery createContactQuery(String query)
    {
        return createContactQuery(
            Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
    }

    /**
     * Creates query that searches for <tt>SourceContact</tt>s
     * which match a specific <tt>query</tt> <tt>String</tt>.
     *
     * @param query the <tt>String</tt> which this <tt>ContactSourceService</tt>
     * is being queried for
     * @param contactCount the maximum count of result contacts
     * @return a <tt>ContactQuery</tt> which represents the query of this
     * <tt>ContactSourceService</tt> implementation for the specified
     * <tt>String</tt> and via which the matching <tt>SourceContact</tt>s (if
     * any) will be returned
     * @see ContactSourceService#queryContactSource(String)
     */
    public ContactQuery createContactQuery(String query, int contactCount)
    {
        return createContactQuery(
            Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
    }

    /**
     * Stops this <tt>ContactSourceService</tt>.
     */
    public abstract void stop();

    /**
     * Defines whether using this contact source service (Outlook or MacOSX
     * Contacs) can be used as result for the search field. This is
     * useful when an external plugin looks for result of this contact source
     * service, but want to display the search field result from its own (avoid
     * duplicate results).
     *
     * @return True if this contact source service can be used to perform search
     * for contacts. False otherwise.
     */
    public abstract boolean canBeUsedToSearchContacts();
}
