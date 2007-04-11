/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.login;

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

    /**
     * Creates an instance of <tt>SecurityAuthorityImpl</tt>.
     * @param mainFrame The parent window of the <tt>AuthenticationWIndow</tt>
     * created in this class.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> for this
     * <tt>SecurityAuthority</tt>.
     */
    public SecurityAuthorityImpl(MainFrame mainFrame,
            ProtocolProviderService protocolProvider) {
        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;
    }

    /**
     * Implements the <code>SecurityAuthority.obtainCredentials</code> method.
     * Creates and show an <tt>AuthenticationWindow</tt>, where user could enter
     * its password.
     */
    public UserCredentials obtainCredentials(String realm,
            UserCredentials userCredentials)
    {
        AuthenticationWindow loginWindow = new AuthenticationWindow(
                mainFrame, protocolProvider, realm, userCredentials);
        
        loginWindow.setVisible(true);
        
        return userCredentials;
    }
}
