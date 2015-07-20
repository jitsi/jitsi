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
package net.java.sip.communicator.plugin.aimaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>AimAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the AIM protocol. It should allow
 * the user to create and configure a new AIM account.
 *
 * @author Yana Stamcheva
 */
public class AimAccountRegistrationWizard
    extends DesktopAccountRegistrationWizard
{
    /**
     * The logger.
     */
    private final Logger logger
        = Logger.getLogger(AimAccountRegistrationWizard.class);

    private FirstWizardPage firstWizardPage;

    private final AimAccountRegistration registration
        = new AimAccountRegistration();

    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>AimAccountRegistrationWizard</tt>.
     *
     * @param wizardContainer the wizard container, where this wizard is added
     */
    public AimAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        setWizardContainer(wizardContainer);

        wizardContainer.setFinishButtonText(
            Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Returns the protocol icon that will be shown on the left of the protocol
     * name in the list, where user will choose the protocol to register to.
     *
     * @return a short description of the protocol.
     */
    @Override
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.AIM_LOGO);
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
     * Returns the protocol name that will be shown in the list, where user
     * will choose the protocol to register to.
     *
     * @return the protocol name.
     */
    @Override
    public String getProtocolName()
    {
        return Resources.getString("plugin.aimaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Returns a short description of the protocol that will be shown on the
     * right of the protocol name in the list, where user will choose the
     * protocol to register to.
     *
     * @return a short description of the protocol.
     */
    @Override
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.aimaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of <tt>WizardPage</tt>-s for this
     * wizard.
     *
     * @return the set of <tt>WizardPage</tt>-s for this
     * wizard.
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();

        // If the first wizard page was already created
        if (firstWizardPage == null)
            firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns a set of key-value pairs that will represent the summary for
     * this wizard.
     *
     * @return a set of key-value pairs that will represent the summary for
     * this wizard.
     */
    @Override
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        Hashtable<String, String> summaryTable
            = new Hashtable<String, String>();

        summaryTable.put(Resources.getString("plugin.aimaccregwizz.USERNAME"),
            registration.getUin());
        summaryTable.put(Resources.getString("service.gui.REMEMBER_PASSWORD"),
                Boolean.toString(registration.isRememberPassword()));

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * @return the <tt>ProtocolProviderService</tt> for the newly created
     * account.
     * @throws OperationFailedException
     */
    @Override
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return this.signin(registration.getUin(), registration.getPassword());
    }

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "service.gui.SIGN_IN" button.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException
     */
    @Override
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory =
            AimAccRegWizzActivator.getAimProtocolProviderFactory();

        return this.installAccount(factory, userName, password);
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create the
     *            account
     * @param user the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory, String user, String passwd)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(  ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                                "resources/images/protocol/aim/aim32x32.png");

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        if (isModification())
        {
            providerFactory.modifyAccount(protocolProvider, accountProperties);

            setModification(false);

            return protocolProvider;
        }

        try
        {
            AccountID accountID
                = providerFactory.installAccount(user, accountProperties);

            ServiceReference serRef
                = providerFactory.getProviderForAccount(accountID);

            protocolProvider
                = (ProtocolProviderService) AimAccRegWizzActivator.bundleContext
                    .getService(serRef);
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
     * Fills the UIN and Password fields in this panel with the data comming
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

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     *
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public AimAccountRegistration getRegistration()
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
        AimAccRegWizzActivator
            .getBrowserLauncher().openURL("https://new.aol.com");
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
}
