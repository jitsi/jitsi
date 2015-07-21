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
package net.java.sip.communicator.service.protocol.event;

import java.util.EventObject;

import net.java.sip.communicator.service.protocol.*;

/**
 * Notifies <tt>UserSearchProviderListener</tt> that a provider that supports
 * user search is added or removed.
 * @author Hristo Terezov
 */
public class UserSearchProviderEvent
    extends EventObject
{
    /**
     * The serial ID.
     */
    private static final long serialVersionUID = -1285649707213476360L;

    /**
     * A type that indicates that the provider is added.
     */
    public static int PROVIDER_ADDED = 0;

    /**
     * A type that indicates that the provider is removed.
     */
    public static int PROVIDER_REMOVED = 1;

    /**
     * The type of the event.
     */
    private final int type;

    /**
     * Constructs new <tt>UserSearchProviderEvent</tt> event.
     * @param provider the provider.
     * @param type the type of the event.
     */
    public UserSearchProviderEvent(ProtocolProviderService provider, int type)
    {
        super(provider);
        this.type = type;
    }

    /**
     * Returns the provider associated with the event.
     * @return the provider associated with the event.
     */
    public ProtocolProviderService getProvider()
    {
        return (ProtocolProviderService) getSource();
    }

    /**
     * Returns the type of the event.
     * @return the type of the event.
     */
    public int getType()
    {
        return type;
    }

}
