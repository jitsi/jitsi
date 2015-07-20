/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.login;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Utility class that can be used in cases where components other than the main
 * user interface may need to launch provider registration. At the time I am
 * writing this, the <tt>DefaultSecurityAuthority</tt> is being used by the
 * systray and uri handlers.
 *
 * @author Yana Stamcheva
 * @author Emil Ivov
 */
public class DefaultSecurityAuthority
    implements SecurityAuthority
{
    private ProtocolProviderService protocolProvider;

    private boolean isUserNameEditable = false;

    /**
     * Creates an instance of <tt>SecurityAuthorityImpl</tt>.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> for this
     * <tt>SecurityAuthority</tt>.
     */
    public DefaultSecurityAuthority(ProtocolProviderService protocolProvider)
    {
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

        if (reasonCode == WRONG_PASSWORD
            || reasonCode == WRONG_USERNAME)
        {
            errorMessage
                = GuiActivator.getResources().getI18NString(
                    "service.gui.AUTHENTICATION_FAILED", new String[]{realm});
        }

        AuthenticationWindow loginWindow = null;

        String userName = userCredentials.getUserName();
        char[] password = userCredentials.getPassword();
        ImageIcon icon
            = AuthenticationWindow.getAuthenticationWindowIcon(protocolProvider);

        if (errorMessage == null)
            loginWindow = new AuthenticationWindow(
                userName,
                password,
                realm,
                isUserNameEditable,
                icon);
        else
            loginWindow = new AuthenticationWindow(
                userName,
                password,
                realm,
                isUserNameEditable,
                icon,
                errorMessage);

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
