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
package net.java.sip.communicator.service.contactlist.event;

import net.java.sip.communicator.service.contactlist.*;

/**
 * Indicates that a meta contact has chaned its display name.
 * @author Emil Ivov
 */
public class MetaContactRenamedEvent
    extends MetaContactPropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of this event using the specified arguments.
     * @param source the <tt>MetaContact</tt> that this event is about.
     * @param oldDisplayName the new display name of this meta contact.
     * @param newDisplayName the old display name of this meta contact.
     */
    public MetaContactRenamedEvent(MetaContact source,
                                   String oldDisplayName,
                                   String newDisplayName)
    {
        super(source, META_CONTACT_RENAMED, oldDisplayName, newDisplayName);
    }

    /**
     * Returns the display name of the source meta contact as it is now, after
     * the change.
     * @return the new display name of the meta contact.
     */
    public String getNewDisplayName()
    {
        return (String)getNewValue();
    }

    /**
     * Returns the display name of the source meta contact as it was now, before
     * the change.
     * @return the meta contact name as it was before the change.
     */
    public String getOldDisplayName()
    {
        return (String)getOldValue();
    }
}
