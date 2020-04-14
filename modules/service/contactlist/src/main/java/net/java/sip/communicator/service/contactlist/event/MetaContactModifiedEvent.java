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
 * Indicates that a meta contact has chaned.
 * @author Damian Minkov
 */
public class MetaContactModifiedEvent
    extends MetaContactPropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Name of the modification.
     */
    private String modificationName;

    /**
     * Creates an instance of this event using the specified arguments.
     * @param source the <tt>MetaContact</tt> that this event is about.
     * @param modificationName name of the modification
     * @param oldValue the new value for the modification of this meta contact.
     * @param newValue the old value for the modification of this meta contact.
     */
    public MetaContactModifiedEvent(MetaContact source,
                                   String modificationName,
                                   Object oldValue,
                                   Object newValue)
    {
        super(source, META_CONTACT_MODIFIED, oldValue, newValue);
        this.modificationName = modificationName;
    }

    /**
     * Returns the modification name of the source meta contact.
     * @return the modification name for the meta contact.
     */
    public String getModificationName()
    {
        return modificationName;
    }
}
