/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.facebookaccregwizz;

import java.util.*;

import net.java.sip.communicator.plugin.jabberaccregwizz.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FacebookAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Facebook Chat protocol. It allows
 * the user to create and configure a new Facebook account.
 *
 * @author Dai Zhiwei
 * @author Yana Stamcheva
 */
public class FacebookAccountRegistrationWizard
    extends JabberAccountRegistrationWizard
{
    /**
     * The protocol name.
     */
    private static final String PROTOCOL = "Facebook";

    /**
     * A constant pointing to the Facebook protocol logo icon.
     */
    private static final String PROTOCOL_ICON
        = "service.protocol.facebook.FACEBOOK_16x16";

    /**
     * A constant pointing to the Facebook protocol wizard page image.
     */
    private static final String PAGE_IMAGE
        = "service.protocol.facebook.FACEBOOK_48x48";

    private static final String SERVER_ADDRESS = "chat.facebook.com";

    /**
     * Creates an instance of <tt>FacebookAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public FacebookAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        super(wizardContainer);
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages()
    {
        JabberAccountRegistration reg = new JabberAccountRegistration();

        setPredefinedProperties(reg);

        return getPages(reg);
    }

    /**
     * Returns the first wizard page.
     *
     * @return the first wizard page.
     */
    public Object getSimpleForm()
    {
        JabberAccountRegistration reg = new JabberAccountRegistration();

        setPredefinedProperties(reg);

        return getSimpleForm(reg);
    }

    /**
     * Sets all facebook specific properties.
     *
     * @param reg the registration object
     */
    private void setPredefinedProperties(JabberAccountRegistration reg)
    {
        reg.setServerAddress(SERVER_ADDRESS);
        reg.setSendKeepAlive(true);

        reg.setServerOverridden(true);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon()
    {
        return FacebookAccRegWizzActivator.getResources()
            .getImageInBytes(PROTOCOL_ICON);
    }
    
    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code> method.
     * Returns the image used to decorate the wizard page
     * 
     * @return byte[] the image used to decorate the wizard page
     */
    public byte[] getPageImage()
    {
        return FacebookAccRegWizzActivator.getResources()
                .getImageInBytes(PAGE_IMAGE);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * @return String
     */
    public String getProtocolName()
    {
        return FacebookAccRegWizzActivator.getResources()
                .getI18NString("plugin.facebookaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return FacebookAccRegWizzActivator.getResources()
            .getI18NString("plugin.facebookaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return "Ex: username";
    }

    /**
     * Returns the display label used for the sip id field.
     * @return the sip id display label string.
     */
    protected String getUsernameLabel()
    {
        return FacebookAccRegWizzActivator.getResources()
            .getI18NString("plugin.facebookaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getCreateAccountButtonLabel()
    {
        return null;
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return FacebookAccRegWizzActivator.getResources().getI18NString(
            "plugin.facebookaccregwizz.DESCRIPTION");
    }

    /**
     * Returns the protocol name as listed in "ProtocolNames" or just the name
     * of the service.
     * @return the protocol name
     */
    public String getProtocol()
    {
        return PROTOCOL;
    }

    /**
     * Returns the protocol icon path.
     * @return the protocol icon path
     */
    public String getProtocolIconPath()
    {
        return "resources/images/protocol/facebook";
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    public String getAccountIconPath()
    {
        return "resources/images/protocol/facebook/logo32x32.png";
    }

    /**
     * Opens a browser on the sign up page. 
     */
    public void webSignup() {}

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return false;
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param userName the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException if the operation didn't succeed
     */
    protected ProtocolProviderService installAccount(
                                        ProtocolProviderFactory providerFactory,
                                        String userName,
                                        String passwd)
                                        throws OperationFailedException
    {
        // add server part to username
        if(userName.indexOf("@") == -1)
            userName += "@" + SERVER_ADDRESS;

        return super.installAccount(providerFactory, userName, passwd);
    }
}
