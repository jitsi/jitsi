/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2003-2005 Arthur van Hoff Rick Blair
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
package net.java.sip.communicator.impl.protocol.zeroconf.jmdns;

// REMIND: Listener should follow Java idiom for listener or have a different
//         name.

/**
 * DNSListener.
 * Listener for record updates.
 *
 * @author Werner Randelshofer, Rick Blair
 * @version 1.0  May 22, 2004  Created.
 */
public interface DNSListener
{
    /**
     * Update a DNS record.
     * @param jmdns
     * @param now
     * @param record
     */
    public void updateRecord(JmDNS jmdns, long now, DNSRecord record);
}
