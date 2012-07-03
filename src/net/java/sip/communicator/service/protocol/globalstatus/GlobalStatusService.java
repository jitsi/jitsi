/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
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
     * @param rememberStatus whether to remember the status for future restore.
     */
    public void publishStatus(
            ProtocolProviderService protocolProvider,
            PresenceStatus status,
            boolean rememberStatus);
}
