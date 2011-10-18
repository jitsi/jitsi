/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.osdependent.jdic;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ProviderUnRegistration</tt> is used by the systray plugin 
 * to make the unregistration to a protocol provider. This operation 
 * is implemented within a thread, so that sip-communicator can 
 * continue its execution smoothly.
 * 
 * @author Nicolas Chamouard
 */
public class ProviderUnRegistration
    extends Thread
{
    /**
     * The protocol provider to whom we want to unregister
     */
    ProtocolProviderService protocolProvider;
    /**
     * The logger for this class.
     */
    private Logger logger
        = Logger.getLogger(ProviderUnRegistration.class.getName());

    /**
     * Creates an instance of <tt>ProviderUnRegistration</tt>.
     * @param protocolProvider the provider we want to unregister
     */
    ProviderUnRegistration(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Start the thread which will unregister to the provider
     */
    public void run()
    {
        try 
        {
            protocolProvider.unregister();
        }
        catch (OperationFailedException ex) 
        {
            int errorCode = ex.getErrorCode();
            if (errorCode == OperationFailedException.GENERAL_ERROR) 
            {
                logger.error("Provider could not be unregistered"
                    + " due to the following general error: ", ex);
            }
            else if (errorCode == OperationFailedException.INTERNAL_ERROR) 
            {
                logger.error("Provider could not be unregistered"
                    + " due to the following internal error: ", ex);
            }
            else if (errorCode == OperationFailedException.NETWORK_FAILURE) 
            {
                logger.error("Provider could not be unregistered"
                        + " due to a network failure: " + ex);
            }
            else if (errorCode == OperationFailedException
                    .INVALID_ACCOUNT_PROPERTIES) 
            {
                logger.error("Provider could not be unregistered"
                    + " due to an invalid account property: ", ex);
            }
            else
            {
                logger.error("Provider could not be unregistered.", ex);
            }
        }
    }
 }