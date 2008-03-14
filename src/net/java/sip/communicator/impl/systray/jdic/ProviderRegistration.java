/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.systray.jdic;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.gui.*;
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
    implements SecurityAuthority
{
    /**
     * The protocol provider to whom we want to register
     */
    private ProtocolProviderService protocolProvider;

    private boolean isUserNameEditable = false;

    /**
     * The logger for this class.
     */
    private Logger logger
        = Logger.getLogger(ProviderRegistration.class.getName());

    /**
     * Creates an instance of <tt>ProviderRegistration</tt>.
     *
     * @param protocolProvider the provider we want to register
     */
    public ProviderRegistration(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Start the thread which will register to the provider
     */
    public void run()
    {
        try {
            protocolProvider.register(this);
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

    /**
     * Used to login to the protocol providers
     *
     * @param realm the realm that the credentials are needed for
     * @param userCredentials the values to propose the user by default
     * @param reasonCode the reason for which we're asking for credentials
     * @return The Credentials associated with the speciefied realm
     */
    public UserCredentials obtainCredentials(
            String realm,
            UserCredentials userCredentials,
            int reasonCode)
    {
        ExportedWindow loginWindow
            = SystrayActivator.getUIService()
                .getAuthenticationWindow(protocolProvider,
                                        realm,
                                        userCredentials,
                                        isUserNameEditable);

        loginWindow.setVisible(true);

        return userCredentials;
    }

    /**
     * Used to login to the protocol providers
     *
     * @param realm the realm that the credentials are needed for
     * @param userCredentials the values to propose the user by default
     * @return The Credentials associated with the speciefied realm
     */
    public UserCredentials obtainCredentials(
            String realm,
            UserCredentials userCredentials)
    {
        return obtainCredentials(   realm,
                                    userCredentials,
                                    SecurityAuthority.AUTHENTICATION_REQUIRED);
    }
    /**
     * Sets the userNameEditable property, which should indicate to the
     * implementations of this interface if the user name could be changed by
     * user or not.
     * 
     * @param isUserNameEditable indicates if the user name could be changed by
     * user in the implementation of this interface.
     */
    public void setUserNameEditable(boolean isUserNameEditable)
    {
        this.isUserNameEditable = isUserNameEditable;
    }

    /**
     * Indicates if the user name is currently editable, i.e. could be changed
     * by user or not.
     * 
     * @return <code>true</code> if the user name could be changed,
     * <code>false</code> - otherwise.
     */
    public boolean isUserNameEditable()
    {
        return isUserNameEditable;
    }
 }
