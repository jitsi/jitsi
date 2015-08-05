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

import org.jitsi.service.protocol.event.*;

/**
 * CallPeerSecurityListener interface extends EventListener. This is the
 * listener interface used to handle an event related with a change in security
 * status.
 *
 * The change in security status is triggered at the protocol level, which
 * signal security state changes to the GUI. This modifies the current security
 * status indicator for the call sessions.
 *
 * @author Werner Dittmann
 * @author Yana Stamcheva
 */
public interface CallPeerSecurityListener
    extends EventListener
{
    /**
     * The handler for the security event received. The security event
     * represents an indication of change in the security status.
     *
     * @param securityEvent
     *            the security event received
     */
    public void securityOn(
        CallPeerSecurityOnEvent securityEvent);

    /**
     * The handler for the security event received. The security event
     * represents an indication of change in the security status.
     *
     * @param securityEvent
     *            the security event received
     */
    public void securityOff(
        CallPeerSecurityOffEvent securityEvent);

    /**
     * The handler for the security event received. The security event
     * represents a timeout trying to establish a secure connection.
     * Most probably the other peer doesn't support it.
     *
     * @param securityTimeoutEvent
     *            the security timeout event received
     */
    public void securityTimeout(
        CallPeerSecurityTimeoutEvent securityTimeoutEvent);

    /**
     * The handler of the security message event.
     *
     * @param event the security message event.
     */
    public void securityMessageRecieved(
        CallPeerSecurityMessageEvent event);

    /**
     * The handler for the security event received. The security event
     * for starting establish a secure connection.
     *
     * @param securityStartedEvent
     *            the security started event received
     */
    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent securityStartedEvent);
}
