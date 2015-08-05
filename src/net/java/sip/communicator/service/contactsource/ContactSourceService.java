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

/**
 * The <tt>ContactSourceService</tt> interface is meant to be implemented
 * by modules supporting large lists of contacts and wanting to enable searching
 * from other modules.
 *
 * @author Yana Stamcheva
 */
public interface ContactSourceService
{
    /**
     * Type of a default source.
     */
    public static final int DEFAULT_TYPE = 0;

    /**
     * Type of a search source. Queried only when searches are performed.
     */
    public static final int SEARCH_TYPE = 1;

    /**
     * Type of a history source. Queries only when history should be shown.
     */
    public static final int HISTORY_TYPE = 2;
    
    /**
     * Type of a contact list source. Queries to be shown in the contact list.
     */
    public static final int CONTACT_LIST_TYPE = 3;

    /**
     * Returns the type of this contact source.
     *
     * @return the type of this contact source
     */
    public int getType();

    /**
     * Returns a user-friendly string that identifies this contact source.
     *
     * @return the display name of this contact source
     */
    public String getDisplayName();

    /**
     * Creates and returns new <tt>ContactQuery</tt> instance.
     * 
     * @param queryString the string to search for
     * 
     * @return new <tt>ContactQuery</tt> instance.
     */
    public ContactQuery createContactQuery(String queryString);
    
    /**
     * Creates and returns new <tt>ContactQuery</tt> instance.
     * 
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return new <tt>ContactQuery</tt> instance.
     */
    public ContactQuery createContactQuery(String queryString,
      int contactCount);

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex();
}
