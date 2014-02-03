/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>OperationSetCusaxUtilsSipImpl</tt> provides utility methods
 * related to the SIP CUSAX implementation.
 *
 * @author Damian Minkov
 */
public class OperationSetCusaxUtilsSipImpl
    implements OperationSetCusaxUtils
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetCusaxUtilsSipImpl.class);

    /**
     * The parent jabber protocol provider.
     */
    private final ProtocolProviderServiceSipImpl provider;

    /**
     * Constructs this operation set.
     * @param provider the parent provider.
     */
    public OperationSetCusaxUtilsSipImpl(
        ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
    }

    /**
     * Returns the linked CUSAX provider for this protocol provider.
     *
     * @return the linked CUSAX provider for this protocol provider or null
     * if such isn't specified
     */
    public ProtocolProviderService getLinkedCusaxProvider()
    {
        String cusaxProviderID = provider.getAccountID()
            .getAccountPropertyString(
                ProtocolProviderFactory.CUSAX_PROVIDER_ACCOUNT_PROP);

        if (cusaxProviderID == null)
            return null;

        AccountID acc
            = ProtocolProviderActivator.getAccountManager()
                .findAccountID(cusaxProviderID);

        if(acc == null)
        {
            logger.warn("No connected cusax account found for "
                + cusaxProviderID);
            return null;
        }
        else
        {
            for (ProtocolProviderService pProvider :
              ProtocolProviderActivator.getProtocolProviders())
            {
                if(pProvider.getAccountID().equals(acc))
                {
                    return pProvider;
                }
            }
        }

        return null;
    }
}
