/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.login;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>SecurityAuthorityImpl</tt> is an implementation of the
 * <tt>SecurityAuthority</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class SecurityAuthorityImpl implements SecurityAuthority {

    private MainFrame mainFrame;

    private ProtocolProviderService protocolProvider;

    private boolean isUserNameEditable = false;

    /**
     * Creates an instance of <tt>SecurityAuthorityImpl</tt>.
     * @param mainFrame The parent window of the <tt>AuthenticationWIndow</tt>
     * created in this class.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> for this
     * <tt>SecurityAuthority</tt>.
     */
    public SecurityAuthorityImpl(MainFrame mainFrame,
            ProtocolProviderService protocolProvider)
    {
        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;
    }

    /**
     * Implements the <code>SecurityAuthority.obtainCredentials</code> method.
     * Creates and show an <tt>AuthenticationWindow</tt>, where user could enter
     * its password.
     * @param realm The realm that the credentials are needed for.
     * @param userCredentials the values to propose the user by default
     * @param reasonCode indicates the reason for which we're obtaining the
     * credentials.
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(
            String realm,
            UserCredentials userCredentials,
            int reasonCode)
    {
        String errorMessage = null;

        if (reasonCode == WRONG_PASSWORD)
        {
            errorMessage
                = Messages.getI18NString("authenticationFailed",
                    new String[]{   userCredentials.getUserName(),
                                    realm}).getText();
        }
        else if (reasonCode == WRONG_USERNAME)
        {
            errorMessage
                = Messages.getI18NString("authenticationFailed",
                    new String[]{   userCredentials.getUserName(),
                                    realm}).getText();
        }

        AuthenticationWindow loginWindow = null;
        
        if (errorMessage == null)
            loginWindow = new AuthenticationWindow( mainFrame,
                                        protocolProvider,
                                        realm,
                                        userCredentials,
                                        isUserNameEditable);
        else
            loginWindow = new AuthenticationWindow( mainFrame,
                protocolProvider,
                realm,
                userCredentials,
                isUserNameEditable,
                errorMessage);

        loginWindow.setVisible(true);

        return userCredentials;
    }

    /**
     * Implements the <code>SecurityAuthority.obtainCredentials</code> method.
     * Creates and show an <tt>AuthenticationWindow</tt>, where user could enter
     * its password.
     * @param realm The realm that the credentials are needed for.
     * @param userCredentials the values to propose the user by default
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(
            String realm,
            UserCredentials userCredentials)
    {
        return this.obtainCredentials(realm, userCredentials,
            SecurityAuthority.AUTHENTICATION_REQUIRED);
    }

    /**
     * Sets the userNameEditable property, which indicates if the user name
     * could be changed by user or not.
     * 
     * @param isUserNameEditable indicates if the user name could be changed by
     * user
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
