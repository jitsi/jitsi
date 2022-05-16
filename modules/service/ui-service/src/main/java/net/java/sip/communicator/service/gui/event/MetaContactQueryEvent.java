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
package net.java.sip.communicator.service.gui.event;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>MetaContactQueryEvent</tt> is triggered each time a
 * <tt>MetaContact</tt> is received as a result of a <tt>MetaContactQuery</tt>.
 *
 * @author Yana Stamcheva
 */
public class MetaContactQueryEvent
    extends EventObject
{
    /**
     * The <tt>MetaContact</tt> this event is about.
     */
    private final MetaContact metaContact;

    /**
     * Creates an instance of <tt>MetaGroupQueryEvent</tt> by specifying the
     * <tt>source</tt> query this event comes from and the <tt>metaContact</tt>
     * this event is about.
     *
     * @param source the <tt>MetaContactQuery</tt> that triggered this event
     * @param metaContact the <tt>MetaContact</tt> this event is about
     */
    public MetaContactQueryEvent(   MetaContactQuery source,
                                    MetaContact metaContact)
    {
        super(source);
        this.metaContact = metaContact;
    }

    /**
     * Returns the <tt>MetaContactQuery</tt> that triggered this event.
     * @return the <tt>MetaContactQuery</tt> that triggered this event
     */
    public MetaContactQuery getQuerySource()
    {
        return (MetaContactQuery) source;
    }

    /**
     * Returns the <tt>MetaContact</tt> this event is about.
     * @return the <tt>MetaContact</tt> this event is about
     */
    public MetaContact getMetaContact()
    {
        return metaContact;
    }
}
