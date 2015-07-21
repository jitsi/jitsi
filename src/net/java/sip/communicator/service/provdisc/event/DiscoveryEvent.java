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
package net.java.sip.communicator.service.provdisc.event;

import java.util.*;

/**
 * Event representing that a provisioning URL has been retrieved.
 *
 * @author Sebastien Vincent
 */
public class DiscoveryEvent extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Provisioning URL.
     */
    private String url = null;

    /**
     * Constructor.
     *
     * @param source object that have created this event
     * @param url provisioning URL
     */
    public DiscoveryEvent(Object source, String url)
    {
        super(source);
        this.url = url;
    }

    /**
     * Get the provisioning URL.
     *
     * @return provisioning URL
     */
    public String getProvisioningURL()
    {
        return url;
    }
}
