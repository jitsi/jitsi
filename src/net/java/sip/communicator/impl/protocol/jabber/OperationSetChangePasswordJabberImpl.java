/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jivesoftware.smack.*;

/**
 * A jabber implementation of the password change operation set.
 *
 * @author Boris Grozev
 */
public class OperationSetChangePasswordJabberImpl
        implements OperationSetChangePassword
{
    /**
     * The <tt>ProtocolProviderService</tt> whose password we'll change.
     */
    private ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * The logger used by <tt>OperationSetChangePasswordJabberImpl</tt>.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetChangePasswordJabberImpl.class);

    /**
     * Sets the object protocolProvider to the one given.
     * @param protocolProvider the protocolProvider to use.
     */
    OperationSetChangePasswordJabberImpl (
                            ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }
    
    /**
     * Changes the jabber account password of protocolProvider to newPass.
     * @param newPass the new password.
     * @throws IllegalStateException if the account is not registered.
     * @throws OperationFailedException if the server does not support password
     * changes.
     */
    public void changePassword(String newPass)
            throws IllegalStateException, OperationFailedException
    {
        org.jivesoftware.smack.AccountManager accountManager
                = new org.jivesoftware.smack.AccountManager(
                                        protocolProvider.getConnection());
                
        try
        {
            accountManager.changePassword(newPass);
        }
        catch (XMPPException e)
        {
            if(logger.isInfoEnabled())
            {
                logger.info("Tried to change jabber password, but the server "
                        + "does not support inband password changes", e);
            }

            throw new OperationFailedException("In-band password changes not"
                    + " supported", 
                    OperationFailedException.NOT_SUPPORTED_OPERATION,
                    e);
        }
    }
}
