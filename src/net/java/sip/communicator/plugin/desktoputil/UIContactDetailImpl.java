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
package net.java.sip.communicator.plugin.desktoputil;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>UIContactDetail</tt> implementation
 *
 * @author Yana Stamcheva
 */
public class UIContactDetailImpl
    extends UIContactDetail
{
    /**
     * The status icon of this contact detail.
     */
    private ImageIcon statusIcon;

    /**
     * Creates a <tt>UIContactDetailImpl</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     *
     * @param address the contact address
     * @param displayName the contact display name
     * @param statusIcon the status icon of this contact detail
     * @param descriptor the underlying object that this class is wrapping
     */
    public UIContactDetailImpl(
        String address,
        String displayName,
        ImageIcon statusIcon,
        Object descriptor)
    {
        super(  address,
                displayName,
                null,
                null,
                null,
                null,
                descriptor);

        setStatusIcon(statusIcon);
    }

    /**
     * Creates a <tt>UIContactDetailImpl</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     * @param address the contact address
     * @param displayName the contact display name
     * @param category the category of the underlying contact detail
     * @param labels the collection of labels associated with this detail
     * @param statusIcon the status icon of this contact detail
     * @param preferredProviders the preferred protocol providers
     * @param preferredProtocols the preferred protocols if no protocol provider
     * is set
     * @param descriptor the underlying object that this class is wrapping
     */
    public UIContactDetailImpl(
        String address,
        String displayName,
        String category,
        Collection<String> labels,
        ImageIcon statusIcon,
        Map<Class<? extends OperationSet>, ProtocolProviderService>
                                                        preferredProviders,
        Map<Class<? extends OperationSet>, String> preferredProtocols,
        Object descriptor)
    {
        super(address, displayName, category, labels, preferredProviders,
            preferredProtocols, descriptor);

        setStatusIcon(statusIcon);
    }

    /**
     * Sets the given status icon.
     *
     * @param statusIcon the status icon to set
     */
    public void setStatusIcon(ImageIcon statusIcon)
    {
        this.statusIcon = statusIcon;
    }

    /**
     * Returns the status icon of this contact detail.
     *
     * @return the status icon of this contact detail
     */
    public ImageIcon getStatusIcon()
    {
        return statusIcon;
    }

    @Override
    public PresenceStatus getPresenceStatus()
    {
        return null;
    }
}
