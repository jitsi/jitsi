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
package net.java.sip.communicator.plugin.googletalkaccregwizz;

import java.util.*;

import net.java.sip.communicator.plugin.jabberaccregwizz.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.jabber.*;

/**
 * The <tt>GoogleTalkAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Google Talk protocol. It should
 * allow the user to create and configure a new Google Talk account.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class GoogleTalkAccountRegistrationWizard
    extends JabberAccountRegistrationWizard
{
    /**
     * The Google Talk protocol name.
     */
    public static final String PROTOCOL = "Google Talk";

    /**
     * A constant pointing to the Google Talk protocol logo image.
     */
    public static final String PROTOCOL_ICON
        = "service.protocol.googletalk.GTALK_16x16";

    /**
     * A constant pointing to the Aim protocol wizard page image.
     */
    public static final String PAGE_IMAGE
        = "service.protocol.googletalk.GTALK_64x64";

    /**
     * Creates an instance of <tt>GoogleTalkAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public GoogleTalkAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        super(wizardContainer);
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        JabberAccountRegistration reg = new JabberAccountRegistration();

        setPredefinedProperties(reg);

        return getPages(reg);
    }

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     *
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return a simple account registration form
     */
    @Override
    public Object getSimpleForm(boolean isCreateAccount)
    {
        JabberAccountRegistration reg = new JabberAccountRegistration();

        setPredefinedProperties(reg);

        return getSimpleForm(reg, isCreateAccount);
    }

    /**
     * Sets all google talk specific properties.
     *
     * @param reg the registration object
     */
    private void setPredefinedProperties(JabberAccountRegistration reg)
    {
        reg.setDefaultUserSufix("gmail.com");
        reg.setServerAddress("talk.google.com");

        reg.setServerOverridden(true);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    @Override
    public byte[] getIcon()
    {
        return GoogleTalkAccRegWizzActivator.getResources()
            .getImageInBytes(PROTOCOL_ICON);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code> method.
     * Returns the image used to decorate the wizard page
     *
     * @return byte[] the image used to decorate the wizard page
     */
    @Override
    public byte[] getPageImage()
    {
        return GoogleTalkAccRegWizzActivator.getResources()
                .getImageInBytes(PAGE_IMAGE);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * @return String
     */
    @Override
    public String getProtocolName()
    {
        return GoogleTalkAccRegWizzActivator.getResources()
                .getI18NString("plugin.googletalkaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    @Override
    public String getProtocolDescription()
    {
        return GoogleTalkAccRegWizzActivator.getResources()
            .getI18NString("plugin.googletalkaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    @Override
    public String getUserNameExample()
    {
        return "Ex: johnsmith@gmail.com or johnsmith";
    }

    /**
     * Returns the display label used for the sip id field.
     * @return the sip id display label string.
     */
    @Override
    protected String getUsernameLabel()
    {
        return GoogleTalkAccRegWizzActivator.getResources()
            .getI18NString("plugin.googletalkaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    @Override
    protected String getCreateAccountButtonLabel()
    {
        return GoogleTalkAccRegWizzActivator.getResources().getI18NString(
            "plugin.googletalkaccregwizz.NEW_ACCOUNT_TITLE");
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    @Override
    protected String getCreateAccountLabel()
    {
        return GoogleTalkAccRegWizzActivator.getResources().getI18NString(
            "plugin.googletalkaccregwizz.REGISTER_NEW_ACCOUNT_TEXT");
    }

    /**
     * Returns the protocol name as listed in "ProtocolNames" or just the name
     * of the service.
     * @return the protocol name
     */
    @Override
    public String getProtocol()
    {
        return PROTOCOL;
    }

    /**
     * Returns the protocol icon path.
     * @return the protocol icon path
     */
    @Override
    public String getProtocolIconPath()
    {
        return "resources/images/protocol/googletalk";
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    @Override
    public String getAccountIconPath()
    {
        return "resources/images/protocol/googletalk/logo32x32.png";
    }

    /**
     * Opens a browser on the sign up page.
     */
    @Override
    public void webSignup()
    {
        GoogleTalkAccRegWizzActivator.getBrowserLauncher()
            .openURL("https://www.google.com/accounts/NewAccount");
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    @Override
    public boolean isWebSignupSupported()
    {
        return true;
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    @Override
    protected JabberAccountCreationFormService getCreateAccountService()
    {
        return null;
    }
}
