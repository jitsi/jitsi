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
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * An operation set that allows plugins to register filters which could
 * intercept instant messages and determine whether or not they should be
 * dispatched to regular listeners. <tt>EventFilter</tt>-s allow implementating
 * features that use standard instant messaging channels to exchange
 *
 * @author Keio Kraaner
 */
public interface OperationSetInstantMessageFiltering
    extends OperationSet
{
    /**
     * Registeres an <tt>EventFilter</tt> with this operation set so that
     * events, that do not need processing, are filtered out.
     *
     * @param filter the <tt>EventFilter</tt> to register.
     */
    public void addEventFilter(EventFilter filter);

    /**
     * Unregisteres an <tt>EventFilter</tt> so that it won't check any more
     * if an event should be filtered out.
     *
     * @param filter the <tt>EventFilter</tt> to unregister.
     */
    public void removeEventFilter(EventFilter filter);
}
