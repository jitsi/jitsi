/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sip2sipaccregwizz;

import net.java.sip.communicator.plugin.sipaccregwizz.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>Sip2SipAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
 * the user to create and configure a new IP Tel SIP account.
 *
 * @author Yana Stamcheva
 */
public class Sip2SipAccountRegistrationWizard
    extends SIPAccountRegistrationWizard
{
    /**
     * A constant pointing to the IP Tel protocol logo image.
     */
    private static final String PROTOCOL_ICON
        = "service.protocol.sip2sip.SIP2SIP_16x16";

    /**
     * A constant pointing to the IP Tel protocol wizard page image.
     */
    private static final String PAGE_IMAGE
        = "service.protocol.sip2sip.SIP2SIP_64x64";

    /**
     * The protocol name.
     */
    public static final String PROTOCOL = "sip2sip.info";

    /**
     * The create account form.
     */
    CreateSip2SipAccountForm createAccountForm = new CreateSip2SipAccountForm();

    /**
     * Creates an instance of <tt>IptelAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container
     */
    public Sip2SipAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        super(wizardContainer);

        // set default proxy, common for sip2sip
        getRegistration().setProxy("proxy.sipthor.net");
        getRegistration().setKeepAliveMethod("NONE");
        getRegistration().setDefaultKeepAliveMethod("NONE");
        getRegistration().setDefaultDomain("sip2sip.info");
        getRegistration().setXCapEnable(true);
        getRegistration().setXCapServerUri(
                "https://xcap.sipthor.net/xcap-root@sip2sip.info");
        getRegistration().setXCapUseSipCredetials(true);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon()
    {
        return Sip2SipAccRegWizzActivator.getResources()
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
        return Sip2SipAccRegWizzActivator.getResources()
            .getImageInBytes(PAGE_IMAGE);
    }


    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * @return String
     */
    public String getProtocolName()
    {
        return Resources.getString(
            "plugin.sip2sipaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources.getString(
            "plugin.sip2sipaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return "Ex: myusername or myusername@sip2sip.info";
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
        return "resources/images/protocol/sip2sip";
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    public String getAccountIconPath()
    {
        return "resources/images/protocol/sip2sip/sip32x32.png";
    }

    /**
     * Opens the browser on the page sign up
     */
    public void webSignup()
    {
        Sip2SipAccRegWizzActivator.getBrowserLauncher()
            .openURL("http://wiki.sip2sip.info");
    }

    /**
     * Returns the name of the web sign up link.
     * @return the name of the web sign up link
     */
    public String getWebSignupLinkName()
    {
        return Resources.getString("plugin.sip2sipaccregwizz.NEW_ACCOUNT_TITLE");
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    protected CreateAccountService getCreateAccountService()
    {
        return createAccountForm;
    }

    /**
     * Returns the display label used for the sip id field.
     * @return the sip id display label string.
     */
    protected String getUsernameLabel()
    {
        return Resources.getString("plugin.sip2sipaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getExistingAccountLabel()
    {
        return Resources.getString("plugin.sip2sipaccregwizz.EXISTING_ACCOUNT");
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return Resources.getString("plugin.sip2sipaccregwizz.CREATE_ACCOUNT");
    }
}
