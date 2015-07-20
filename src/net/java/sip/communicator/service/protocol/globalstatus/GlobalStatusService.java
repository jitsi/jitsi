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
package net.java.sip.communicator.service.protocol.globalstatus;

import net.java.sip.communicator.service.protocol.*;

/**
 * Service managing global statuses, publishing status for
 * global statuses and fo individual protocol providers, saving its last
 * state for future restore .
 *
 * @author Damian Minkov
 */
public interface GlobalStatusService
{
    /**
     * Returns the global presence status.
     *
     * @return the current global presence status
     */
    public PresenceStatus getGlobalPresenceStatus();

    /**
     * Returns the last status that was stored in the configuration for the
     * given protocol provider.
     *
     * @param protocolProvider the protocol provider
     * @return the last status that was stored in the configuration for the
     *         given protocol provider
     */
    public PresenceStatus getLastPresenceStatus(
        ProtocolProviderService protocolProvider);

    /**
     * Returns the last contact status saved in the configuration.
     *
     * @param protocolProvider the protocol provider to which the status
     *            corresponds
     * @return the last contact status saved in the configuration.
     */
    public String getLastStatusString(ProtocolProviderService protocolProvider);

    /**
     * Publish present global status, changing the appropriate status on all
     * protocols.
     *
     * @param status the status to change to.
     */
    public void publishStatus(GlobalStatusEnum status);

    /**
     * Publish present status. We search for the highest,
     *
     * @param protocolProvider the protocol provider to which we
     * change the status.
     * @param status the status to publish.
     */
    public void publishStatus(
            ProtocolProviderService protocolProvider,
            PresenceStatus status);
}
