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
package net.java.sip.communicator.plugin.iptelaccregwizz;

import java.util.*;

import net.java.sip.communicator.plugin.sipaccregwizz.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.sip.*;

/**
 * The <tt>IptelAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
 * the user to create and configure a new SIP account.
 *
 * @author Yana Stamcheva
 */
public class IptelAccountRegistrationWizard
    extends SIPAccountRegistrationWizard
{
    /**
     * A constant pointing to the IP Tel protocol logo image.
     */
    private static final String PROTOCOL_ICON
        = "service.protocol.iptel.IPTEL_16x16";

    /**
     * A constant pointing to the IP Tel protocol wizard page image.
     */
    private static final String PAGE_IMAGE
        = "service.protocol.iptel.IPTEL_64x64";

    /**
     * The protocol name.
     */
    public static final String PROTOCOL = "iptel.org";

    /**
     * Creates an instance of <tt>IptelAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container
     */
    public IptelAccountRegistrationWizard(WizardContainer wizardContainer)
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
     * Sets all iptel specific properties.
     *
     * @param reg the registration object
     */
    private void setPredefinedProperties(SIPAccountRegistration reg)
    {
        reg.setDefaultDomain("iptel.org");
        reg.setPreferredTransport("TCP");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    @Override
    public byte[] getIcon()
    {
        return IptelAccRegWizzActivator.getResources()
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
        return IptelAccRegWizzActivator.getResources()
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
            "plugin.iptelaccregwizz.PROTOCOL_NAME");
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
            "plugin.iptelaccregwizz.PROTOCOL_DESCRIPTION");
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
        return "Ex: myusername or myusername@iptel.org";
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
        return "resources/images/protocol/iptel";
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    @Override
    public String getAccountIconPath()
    {
        return "resources/images/protocol/iptel/sip32x32.png";
    }

    /**
     * Opens the browser on the page sign up
     */
    @Override
    public void webSignup()
    {
        IptelAccRegWizzActivator.getBrowserLauncher()
            .openURL("https://serweb.iptel.org/user/reg/index.php");
    }

    /**
     * Returns the name of the web sign up link.
     * @return the name of the web sign up link
     */
    @Override
    public String getWebSignupLinkName()
    {
        return IptelAccRegWizzActivator.getResources().getI18NString(
            "plugin.iptelaccregwizz.NEW_ACCOUNT_TITLE");
    }
}
