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

import java.util.*;

/**
 * The <tt>ContactReceivedEvent</tt> indicates that a
 * <tt>SourceContact</tt> has been received as a result of a
 * <tt>ContactQuery</tt>.
 * @author Yana Stamcheva
 */
public class ContactReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The contact that has been received.
     */
    private final SourceContact contact;
    
    /**
     * Indicates whether show more label should be shown or not.
     */
    private final boolean showMoreEnabled;

    /**
     * Creates a <tt>ContactReceivedEvent</tt> by specifying the contact search
     * source and the received <tt>searchContact</tt>.
     * @param source the source that triggered this event
     * @param contact the received contact
     */
    public ContactReceivedEvent(ContactQuery source,
                                SourceContact contact)
    {
        super(source);

        this.contact = contact;
        
        showMoreEnabled = true;
    }
    
    /**
     * Creates a <tt>ContactReceivedEvent</tt> by specifying the contact search
     * source and the received <tt>searchContact</tt>.
     * @param source the source that triggered this event
     * @param contact the received contact
     * @param showMoreEnabled indicates whether show more label should be shown 
     * or not.
     */
    public ContactReceivedEvent(ContactQuery source,
                                SourceContact contact,
                                boolean showMoreEnabled)
    {
        super(source);

        this.contact = contact;
        
        this.showMoreEnabled = showMoreEnabled;
    }

    /**
     * Returns the <tt>ContactQuery</tt> that triggered this event.
     * @return the <tt>ContactQuery</tt> that triggered this event
     */
    public ContactQuery getQuerySource()
    {
        return (ContactQuery) source;
    }

    /**
     * Returns the received contact.
     * @return the received contact
     */
    public SourceContact getContact()
    {
        return contact;
    }
    
    /**
     * Returns <tt>true</tt> if show more label should be shown and 
     * <tt>false</tt> if not.
     * @return <tt>true</tt> if show more label should be shown and 
     * <tt>false</tt> if not.
     */
    public boolean isShowMoreEnabled()
    {
        return showMoreEnabled;
    }
}
