/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.googletalkaccregwizz;

import java.util.*;

import net.java.sip.communicator.plugin.jabberaccregwizz.*;
import net.java.sip.communicator.service.gui.*;

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
     * Sets all google talk specific properties.
     *
     * @param reg the registration object
     */
    private void setPredefinedProperties(JabberAccountRegistration reg)
    {
        reg.setServerAddress("talk.google.com");

        reg.setServerOverridden(true);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
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
    public String getUserNameExample()
    {
        return "Ex: johnsmith@gmail.com";
    }

    /**
     * Returns the display label used for the sip id field.
     * @return the sip id display label string.
     */
    protected String getUsernameLabel()
    {
        return GoogleTalkAccRegWizzActivator.getResources()
            .getI18NString("plugin.googletalkaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getCreateAccountButtonLabel()
    {
        return GoogleTalkAccRegWizzActivator.getResources().getI18NString(
            "plugin.googletalkaccregwizz.NEW_ACCOUNT_TITLE");
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
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
        return "resources/images/protocol/googletalk";
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    public String getAccountIconPath()
    {
        return "resources/images/protocol/googletalk/logo32x32.png";
    }

    /**
     * Opens a browser on the sign up page. 
     */
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
    public boolean isWebSignupSupported()
    {
        return true;
    }
}
