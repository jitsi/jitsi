/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.account;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>AccountStatusUtils</tt> provides utility methods for account status
 * management.
 *
 * @author Yana Stamcheva
 */
public class AccountStatusUtils
{
    private static GlobalStatusService globalStatusService;

    /**
     * If the protocol provider supports presence operation set searches the
     * last status which was selected, otherwise returns null.
     *
     * @param protocolProvider the protocol provider we're interested in.
     * @return the last protocol provider presence status, or null if this
     * provider doesn't support presence operation set
     */
    public static Object getProtocolProviderLastStatus(
            ProtocolProviderService protocolProvider)
    {
        if(getProtocolPresenceOpSet(protocolProvider) != null)
            return getLastPresenceStatus(protocolProvider);
        else
            return getGlobalStatusService()
                    .getLastStatusString(protocolProvider);
    }

    /**
     * Returns the presence operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the
     * presence operation set is searched.
     * @return the presence operation set for the given protocol provider.
     */
    public static OperationSetPresence getProtocolPresenceOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        return
            (opSet instanceof OperationSetPresence)
                ? (OperationSetPresence) opSet
                : null;
    }

    /**
     * Returns the last status that was stored in the configuration xml for the
     * given protocol provider.
     *
     * @param protocolProvider the protocol provider
     * @return the last status that was stored in the configuration xml for the
     *         given protocol provider
     */
    public static PresenceStatus getLastPresenceStatus(
        ProtocolProviderService protocolProvider)
    {
        if (getGlobalStatusService() != null)
            return getGlobalStatusService().getLastPresenceStatus(
                protocolProvider);

        return null;
    }

    /**
     * Returns the last contact status saved in the configuration.
     *
     * @param protocolProvider the protocol provider to which the status
     *            corresponds
     * @return the last contact status saved in the configuration.
     */
    public String getLastStatusString(ProtocolProviderService protocolProvider)
    {
        return getGlobalStatusService().getLastStatusString(protocolProvider);
    }

    /**
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle
     * context.
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle
     * context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (globalStatusService == null)
        {
            globalStatusService
                = ServiceUtils.getService(
                        UtilActivator.bundleContext,
                        GlobalStatusService.class);
        }

        return globalStatusService;
    }
}