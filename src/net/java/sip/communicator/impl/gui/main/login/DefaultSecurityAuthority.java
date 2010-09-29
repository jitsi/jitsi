/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.login;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.AuthenticationWindow;

/**
 * Utility class that can be used in cases where components other than the main
 * user interface may need to launch provider registration. At the time I am
 * writing this, the <tt>DefaultSecurityAuthority</tt> is being used by the
 * systray and
 *
 * @author Emil Ivov
 */
public class DefaultSecurityAuthority
    implements SecurityAuthority
{
    private boolean isUserNameEditable = false;

    /**
     * The provider that this authority would be responsible for.
     */
    private ProtocolProviderService provider = null;

    /**
     * Creates this authority for a particular provider.
     *
     * @param provider the protocol provider, for which this security authority
     * is about
     */
    public DefaultSecurityAuthority(ProtocolProviderService provider)
    {
        this.provider = provider;
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
        String userName = userCredentials.getUserName();
        char[] password = userCredentials.getPassword();
        ImageIcon icon = ImageLoader.getAuthenticationWindowIcon(provider);

        AuthenticationWindow loginWindow
            = new AuthenticationWindow( userName,
                                        password,
                                        realm,
                                        isUserNameEditable,
                                        icon);

        loginWindow.pack();
        loginWindow.setVisible(true);

        if (!loginWindow.isCanceled())
        {
            userCredentials.setUserName(loginWindow.getUserName());
            userCredentials.setPassword(loginWindow.getPassword());
            userCredentials.setPasswordPersistent(
                loginWindow.isRememberPassword());
        }
        else
        {
            userCredentials.setUserName(null);
            userCredentials = null;
        }

        return userCredentials;
    }


    /**
     * Sets the userNameEditable property, which should indicate to the
     * implementations of this interface if the user name could be changed
     * by user or not.
     *
     * @param isUserNameEditable indicates if the user name could be changed
     * by user in the implementation of this interface.
     */
    public void setUserNameEditable(boolean isUserNameEditable)
    {
        this.isUserNameEditable = isUserNameEditable;
    }

    /**
     * Indicates if the user name is currently editable, i.e. could be
     * changed by user or not.
     *
     * @return <tt>true</tt> if the user name could be changed and
     * <tt>false</tt> otherwise.
     */
    public boolean isUserNameEditable()
    {
        return isUserNameEditable;
    }
}
