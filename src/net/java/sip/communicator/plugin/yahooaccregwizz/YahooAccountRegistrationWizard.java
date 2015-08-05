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
package net.java.sip.communicator.plugin.yahooaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>YahooAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Yahoo protocol. It should allow
 * the user to create and configure a new Yahoo account.
 *
 * @author Yana Stamcheva
 */
public class YahooAccountRegistrationWizard
    extends DesktopAccountRegistrationWizard
{
    private final Logger logger
        = Logger.getLogger(YahooAccountRegistrationWizard.class);

    private FirstWizardPage firstWizardPage;

    private YahooAccountRegistration registration =
        new YahooAccountRegistration();

    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>YahooAccountRegistrationWizard</tt>.
     *
     * @param wizardContainer the wizard container, where this wizard is added
     */
    public YahooAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        setWizardContainer(wizardContainer);

        wizardContainer
            .setFinishButtonText(Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     *
     * @return byte[]
     */
    @Override
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.YAHOO_LOGO);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code>
     * method. Returns the image used to decorate the wizard page
     *
     * @return byte[] the image used to decorate the wizard page
     */
    @Override
    public byte[] getPageImage()
    {
        return Resources.getImage(Resources.PAGE_IMAGE);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     *
     * @return String
     */
    @Override
    public String getProtocolName()
    {
        return Resources.getString("plugin.yahooaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code>
     * method. Returns the description of the protocol for this wizard.
     *
     * @return String
     */
    @Override
    public String getProtocolDescription()
    {
        return Resources
            .getString("plugin.yahooaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     *
     * @return Iterator
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
        firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     *
     * @return Iterator
     */
    @Override
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        Hashtable<String, String> summaryTable
            = new Hashtable<String, String>();

        summaryTable.put(
            Resources.getString("plugin.yahooaccregwizz.USERNAME"),
            registration.getUin());
        summaryTable.put(
            Resources.getString("service.gui.REMEMBER_PASSWORD"),
            Boolean.toString(registration.isRememberPassword()));

        return summaryTable.entrySet().iterator();
    }

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     *
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    @Override
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return signin(  registration.getUin(),
                        registration.getPassword());
    }

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    @Override
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory =
            YahooAccRegWizzActivator.getYahooProtocolProviderFactory();

        return this.installAccount( factory,
                                    userName,
                                    password);
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create the
     *            account
     * @param user the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory, String user, String passwd)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
            "resources/images/protocol/yahoo/yahoo32x32.png");

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        if (isModification())
        {
            providerFactory.modifyAccount(  protocolProvider,
                accountProperties);

            setModification(false);

            return protocolProvider;
        }

        try
        {
            AccountID accountID =
                providerFactory.installAccount(user, accountProperties);

            ServiceReference serRef =
                providerFactory.getProviderForAccount(accountID);

            protocolProvider =
                (ProtocolProviderService) YahooAccRegWizzActivator
                    .bundleContext.getService(serRef);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Account already exists.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Exception exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Failed to add account",
                OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Fills the UIN and Password fields in this panel with the data coming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    @Override
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        setModification(true);

        this.protocolProvider = protocolProvider;

        this.registration = new YahooAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     *
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public YahooAccountRegistration getRegistration()
    {
        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    @Override
    public Dimension getSize()
    {
        return new Dimension(600, 500);
    }

    /**
     * Returns the identifier of the page to show first in the wizard.
     * @return the identifier of the page to show first in the wizard.
     */
    @Override
    public Object getFirstPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Returns the identifier of the page to show last in the wizard.
     * @return the identifier of the page to show last in the wizard.
     */
    @Override
    public Object getLastPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
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
        return FirstWizardPage.USER_NAME_EXAMPLE;
    }

    /**
     * Defines the operation that will be executed when user clicks on the
     * "Sign up" link.
     * @throws UnsupportedOperationException if the web sign up operation is
     * not supported by the current implementation.
     */
    @Override
    public void webSignup()
    {
        YahooAccRegWizzActivator.getBrowserLauncher()
            .openURL("https://edit.yahoo.com/registration");
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
        firstWizardPage = new FirstWizardPage(this);

        return firstWizardPage.getSimpleForm();
    }

    /**
     * Whether the advanced configuration is enabled. Gives an option
     * to disable/hide advanced config button.
     * @return whether the advanced configuration is enabled.
     */
    public boolean isAdvancedConfigurationEnabled()
    {
        return false;
    }
}
