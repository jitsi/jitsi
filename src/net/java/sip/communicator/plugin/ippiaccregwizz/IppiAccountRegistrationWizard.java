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
package net.java.sip.communicator.plugin.ippiaccregwizz;

import java.util.*;

import net.java.sip.communicator.plugin.sipaccregwizz.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.sip.*;

/**
 * The <tt>IppiAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
 * the user to create and configure a new IP Tel SIP account.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class IppiAccountRegistrationWizard
    extends SIPAccountRegistrationWizard
{
    /**
     * A constant pointing to the IP Tel protocol logo image.
     */
    private static final String PROTOCOL_ICON
        = "service.protocol.ippi.IPPI_16x16";

    /**
     * A constant pointing to the IP Tel protocol wizard page image.
     */
    private static final String PAGE_IMAGE
        = "service.protocol.ippi.IPPI_64x64";

    /**
     * The protocol name.
     */
    public static final String PROTOCOL = "ippi";

    /**
     * The create account form.
     */
    CreateIppiAccountForm createAccountForm = new CreateIppiAccountForm();

    /**
     * Creates an instance of <tt>IptelAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container
     */
    public IppiAccountRegistrationWizard(WizardContainer wizardContainer)
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
        SIPAccountRegistration reg = new SIPAccountRegistration();

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
        SIPAccountRegistration reg = new SIPAccountRegistration();

        setPredefinedProperties(reg);

        return getSimpleForm(reg, isCreateAccount);
    }

    /**
     * Sets all predefined properties specific for this account wizard.
     *
     * @param reg the registration object
     */
    private void setPredefinedProperties(SIPAccountRegistration reg)
    {
        // set properties common for ippi
        reg.setServerAddress("ippi.fr");
        reg.setServerPort("5060");
        reg.setProxyAutoConfigure(false);
        reg.setProxy("ippi.fr");
        reg.setProxyPort("5060");
        reg.setPreferredTransport("TCP");
        reg.setDefaultDomain("ippi.fr");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    @Override
    public byte[] getIcon()
    {
        return IppiAccRegWizzActivator.getResources()
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
        return IppiAccRegWizzActivator.getResources()
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
        return Resources.getString(
            "plugin.ippiaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    @Override
    public String getProtocolDescription()
    {
        return Resources.getString(
            "plugin.ippiaccregwizz.PROTOCOL_DESCRIPTION");
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
        return "Ex: myusername or myusername@ippi.fr";
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
        return "resources/images/protocol/ippi";
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    @Override
    public String getAccountIconPath()
    {
        return "resources/images/protocol/ippi/sip32x32.png";
    }

    /**
     * Opens the browser on the page sign up
     */
    @Override
    public void webSignup()
    {
        IppiAccRegWizzActivator.getBrowserLauncher()
            .openURL("https://www.ippi.fr/index.php?page=sp-offer");
    }

    /**
     * Returns the name of the web sign up link.
     * @return the name of the web sign up link
     */
    @Override
    public String getWebSignupLinkName()
    {
        return Resources.getString("plugin.ippiaccregwizz.NEW_ACCOUNT_TITLE");
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    @Override
    protected SIPAccountCreationFormService getCreateAccountService()
    {
        return createAccountForm;
    }

    /**
     * Returns the display label used for the sip id field.
     * @return the sip id display label string.
     */
    @Override
    protected String getUsernameLabel()
    {
        return Resources.getString("plugin.sip2sipaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    @Override
    protected String getExistingAccountLabel()
    {
        return Resources.getString("plugin.ippiaccregwizz.EXISTING_ACCOUNT");
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    @Override
    protected String getCreateAccountLabel()
    {
        return Resources.getString("plugin.ippiaccregwizz.CREATE_ACCOUNT");
    }
}
