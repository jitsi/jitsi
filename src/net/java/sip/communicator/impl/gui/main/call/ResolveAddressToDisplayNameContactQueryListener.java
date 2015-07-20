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
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.contactsource.*;
import org.jitsi.util.*;

/**
 * Listener for a contact query, used in order to resolve a contact address
 * into a display name. If contact and its name is found, its image also is
 * available.
 *
 * @author Vincent Lucas
 */
public class ResolveAddressToDisplayNameContactQueryListener
    implements ContactQueryListener
{
    /**
     * The query we are looking for events.
     */
    private ContactQuery query;

    /**
     * The display name corresponding to the contact address.
     */
    private String resolvedName;

    /**
     * The image corresponding to the contact address.
     */
    private byte[] resolvedImage;

    /**
     * Creates a new ResolvedContactQueryListener.
     */
    public ResolveAddressToDisplayNameContactQueryListener(ContactQuery query)
    {
        this.resolvedName = null;
        this.query = query;
        if(this.query != null)
        {
            this.query.addContactQueryListener(this);
        }
    }

    /**
     * Indicates that a contact has been updated after a  search.
     */
    public void contactChanged(ContactChangedEvent event)
    {
        // NOT USED
    }

    /**
     * Indicates that a new contact has been received for a search.
     */
    public void contactReceived(ContactReceivedEvent event)
    {
        SourceContact contact = event.getContact();
        if(contact != null)
        {
            if(!isFoundName())
            {
                this.resolvedName = contact.getDisplayName();
            }

            if(!isFoundImage())
            {
                this.resolvedImage = contact.getImage();
            }

            if(isFoundName() && isFoundImage())
            {
                this.stop();
            }
        }
    }

    /**
     * Indicates that a contact has been removed after a search.
     */
    public void contactRemoved(ContactRemovedEvent event)
    {
        // NOT USED
    }

    /**
     * Indicates that the status of a search has been changed.
     */
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        this.stop();
    }

    /**
     * Tells if the query is still running.
     *
     * @return True if the query is still running. False otherwise.
     */
    public boolean isRunning()
    {
        return this.query != null;
    }

    /**
     * Stops this ResolvedContactQueryListener.
     */
    public synchronized void stop()
    {
        if(this.query != null)
        {
            this.query.removeContactQueryListener(this);
            this.query.cancel();
            this.query = null;
        }
    }

    /**
     * Tells if the query has found a match to resolve the contact address.
     *
     * @return True if the query has found a match to resolve the contact
     * address. False otherwise.
     */
    public boolean isFoundName()
    {
        return !StringUtils.isNullOrEmpty(resolvedName);
    }

    /**
     * Tells if the query has found a match to resolve the contact address.
     *
     * @return True if the query has found a match to resolve the contact
     * address. False otherwise.
     */
    public boolean isFoundImage()
    {
        return resolvedImage != null;
    }

    /**
     * Returns the display name corresponding to the contact address.
     *
     * @return The display name corresponding to the contact address. Null
     * or empty string if not found.
     */
    public String getResolvedName()
    {
        return this.resolvedName;
    }

    /**
     * Returns the image corresponding to the contact address.
     *
     * @return The image corresponding to the contact address. Null
     * if not found or missing.
     */
    public byte[] getResolvedImage()
    {
        return this.resolvedImage;
    }
}
