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

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a notifying event fired by a specific {@link AccountManager}.
 *
 * @author Lubomir Marinov
 */
public class AccountManagerEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The type of event notifying that the loading of the stored accounts of a
     * specific <code>ProtocolProviderFactory</code> has finished.
     */
    public static final int STORED_ACCOUNTS_LOADED = 1;

    /**
     * The <code>ProtocolProviderFactory</code> being worked on at the time this
     * event has been fired.
     */
    private final ProtocolProviderFactory factory;

    /**
     * The (detail) type of this event which is one of
     * {@link #STORED_ACCOUNTS_LOADED}.
     */
    private final int type;

    /**
     * Initializes a new <code>AccountManagerEvent</code> instance fired by a
     * specific <code>AccountManager</code> in order to notify of an event of a
     * specific type occurring while working on a specific
     * <code>ProtocolProviderFactory</code>.
     *
     * @param accountManager the <code>AccountManager</code> issuing the
     *            notification i.e. the source of the event
     * @param type the type of the event which is one of
     * {@link #STORED_ACCOUNTS_LOADED}
     * @param factory the <code>ProtocolProviderFactory</code> being worked on
     *            at the time this event has been fired
     */
    public AccountManagerEvent(AccountManager accountManager, int type,
        ProtocolProviderFactory factory)
    {
        super(accountManager);

        this.type = type;
        this.factory = factory;
    }

    /**
     * Gets the <code>ProtocolProviderFactory</code> being worked on at the time
     * this event has been fired.
     *
     * @return the <code>ProtocolProviderFactory</code> being worked on at the
     *         time this event has been fired
     */
    public ProtocolProviderFactory getFactory()
    {
        return factory;
    }

    /**
     * Gets the (detail) type of this event which is one of
     * <code>STORED_ACCOUNTS_LOADED</code>.
     *
     * @return the (detail) type of this event which is one of
     *         <code>STORED_ACCOUNTS_LOADED</code>
     */
    public int getType()
    {
        return type;
    }
}
