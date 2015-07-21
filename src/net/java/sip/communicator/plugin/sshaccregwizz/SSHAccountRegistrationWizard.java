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

package net.java.sip.communicator.plugin.sshaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.ssh.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>SSHAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SSH protocol. It allows
 * the user to create and configure a new SSH account.
 *
 * @author Shobhit Jindal
 */
public class SSHAccountRegistrationWizard
        extends DesktopAccountRegistrationWizard
{
    private final Logger logger
        = Logger.getLogger(SSHAccountRegistrationWizard.class);

    /**
     * The first page of the ssh account registration wizard.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The object that we use to store details on an account that we will be
     * creating.
     */
    private SSHAccountRegistration registration
            = new SSHAccountRegistration();

    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>SSHAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public SSHAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        setWizardContainer(wizardContainer);

        wizardContainer.setFinishButtonText(
            Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    @Override
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.SSH_LOGO);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code>
     *  method.
     * Returns the image used to decorate the wizard page
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
     * @return String
     */
    @Override
    public String getProtocolName()
    {
        return Resources.getString("plugin.sshaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    @Override
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.sshaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
        firstWizardPage
            = new FirstWizardPage(registration, getWizardContainer());

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    @Override
    public Iterator<Map.Entry<String, String>> getSummary() {
        Hashtable<String, String> summaryTable
            = new Hashtable<String, String>();

        /*
         * Hashtable arranges the entries alphabetically so the order
         * of appearance is
         * - Computer Name / IP
         * - Port
         * - User ID
         */
        summaryTable.put("Account ID", registration.getAccountID());
        summaryTable.put("Known Hosts", registration.getKnownHostsFile());
        summaryTable.put("Identity", registration.getIdentityFile());

        return summaryTable.entrySet().iterator();
    }

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    @Override
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return signin(registration.getAccountID(), null);
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
        ProtocolProviderFactory factory
                = SSHAccRegWizzActivator.getSSHProtocolProviderFactory();

        return this.installAccount( factory,
                                    userName);
    }

    /**
     * Creates an account for the given Account ID, Identity File and Known
     *  Hosts File
     *
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param user the user identifier
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    public ProtocolProviderService installAccount(
            ProtocolProviderFactory providerFactory,
            String user)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
            "resources/images/protocol/ssh/ssh32x32.png");

        accountProperties.put(
            ProtocolProviderFactory.NO_PASSWORD_REQUIRED,
            new Boolean(true).toString());

        accountProperties.put(ProtocolProviderFactorySSHImpl.IDENTITY_FILE,
                registration.getIdentityFile());

        accountProperties.put(ProtocolProviderFactorySSHImpl.KNOWN_HOSTS_FILE,
                String.valueOf(registration.getKnownHostsFile()));

        try
        {
            AccountID accountID = providerFactory.installAccount(
                    user, accountProperties);

            ServiceReference serRef = providerFactory
                    .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
            SSHAccRegWizzActivator.bundleContext
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
     * Fills the UserID and Password fields in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    @Override
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;

        this.firstWizardPage.loadAccount(protocolProvider);

        setModification(true);
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
     * Returns the identifier of the first account registration wizard page.
     * This method is meant to be used by the wizard container to determine,
     * which is the first page to show to the user.
     *
     * @return the identifier of the first account registration wizard page
     */
    @Override
    public Object getFirstPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

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
        return null;
    }

    /**
     * Indicates whether this wizard enables the simple "sign in" form shown
     * when the user opens the application for the first time. The simple
     * "sign in" form allows user to configure her account in one click, just
     * specifying her username and password and leaving any other configuration
     * as by default.
     * @return <code>true</code> if the simple "Sign in" form is enabled or
     * <code>false</code> otherwise.
     */
    @Override
    public boolean isSimpleFormEnabled()
    {
        return false;
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
        firstWizardPage = new FirstWizardPage(registration, getWizardContainer());
        return firstWizardPage.getSimpleForm();
    }
}
