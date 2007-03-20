/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.systray.jdic;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ProviderRegistration</tt> is used by the systray plugin 
 * to make the registration to a protocol provider. This operation 
 * is implemented within a thread, so that sip-communicator can 
 * continue its execution during this operation.
 * 
 * @author Nicolas Chamouard
 */

public class ProviderRegistration
    extends Thread
{
	/**
	 * The protocol provider to whom we want to register
	 */
    ProtocolProviderService protocolProvider;
    /* temporary */
    SecurityAuthority secAuth;
    
    /**
     * The logger for this class.
     */
    private Logger logger = Logger.getLogger(ProviderRegistration.class.getName());
    
    /**
     * Creates an instance of <tt>ProviderRegistration</tt>.
     * @param protocolProvider the provider we want to register
     * @param secAuth temporary, will be changed
     */
    ProviderRegistration(ProtocolProviderService protocolProvider,
        SecurityAuthority secAuth)
    {
        this.protocolProvider = protocolProvider;
        this.secAuth = secAuth;
    }

    /**
     * Start the thread which will register to the provider
     */
    public void run()
    {
        try {
            protocolProvider.register(secAuth);
        }
        catch (OperationFailedException ex) 
        {
            int errorCode = ex.getErrorCode();
            if (errorCode == OperationFailedException.GENERAL_ERROR) 
            {
                logger.error("Provider could not be registered"
                    + " due to the following general error: ", ex);
            }
            else if (errorCode == OperationFailedException.INTERNAL_ERROR) 
            {
                logger.error("Provider could not be registered"
                    + " due to the following internal error: ", ex);
            }
            else if (errorCode == OperationFailedException.NETWORK_FAILURE) 
            {
                logger.error("Provider could not be registered"
                        + " due to a network failure: " + ex);
            }
            else if (errorCode == OperationFailedException
                    .INVALID_ACCOUNT_PROPERTIES) 
            {
                logger.error("Provider could not be registered"
                    + " due to an invalid account property: ", ex);
            }
            else
            {
                logger.error("Provider could not be registered.", ex);
            }
        }
    }
 }