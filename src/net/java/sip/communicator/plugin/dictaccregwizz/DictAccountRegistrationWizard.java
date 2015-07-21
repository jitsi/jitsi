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
package net.java.sip.communicator.plugin.dictaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>DictAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Dict protocol. It should allow
 * the user to create and configure a new Dict account.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class DictAccountRegistrationWizard
    extends DesktopAccountRegistrationWizard
{
    private final Logger logger
        = Logger.getLogger(DictAccountRegistrationWizard.class);

    /**
     * The reference to the first page of the wizard.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The registration of the DICT account.
     */
    private DictAccountRegistration registration = new DictAccountRegistration();

    /**
     * The protocole provider.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>DictAccountRegistrationWizard</tt>.
     *
     * @param wizardContainer the wizard container, where this wizard is added
     */
    public DictAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        setWizardContainer(wizardContainer);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * @return Returns the icon to be used for this wizard.
     */
    @Override
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.DICT_LOGO);
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
     * method.
     * @return Returns the protocol name for this wizard.
     */
    @Override
    public String getProtocolName()
    {
        return Resources.getString("plugin.dictaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method.
     * @return Returns the description of the protocol for this wizard.
     */
    @Override
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.dictaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     *
     * @return Returns the set of pages contained in this wizard.
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
        this.firstWizardPage = new FirstWizardPage(this);
        pages.add(this.firstWizardPage);
        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Returns the set of data that user has entered through this wizard.
     */
    @Override
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        Map<String, String> summaryTable = new LinkedHashMap<String, String>();

        summaryTable.put("Host", registration.getHost());
        summaryTable.put("Port", String.valueOf(registration.getPort()));
        summaryTable.put("Strategy", registration.getStrategy().getName());

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

        return signin(registration.getUserID(), null);
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
            = DictAccRegWizzActivator.getDictProtocolProviderFactory();

        return this.installAccount(factory, registration.getHost(),
                                   registration.getPort(),
                                   registration.getStrategy().getCode());
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create the
     * account.
     * @param host The hostname of the DICT server.
     * @param port The port used by the DICT server.
     * @param strategy The strategy choosen for matching words in the
     * dictionnaries.
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String host,
        int port,
        String strategy)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(  ProtocolProviderFactory.ACCOUNT_ICON_PATH,
            "resources/images/protocol/dict/dict-32x32.png");

        // Set this property to indicate that Dict account does not require
        // authentication.
        accountProperties.put(
            ProtocolProviderFactory.NO_PASSWORD_REQUIRED,
            new Boolean(true).toString());

        // Save host
        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS, host);
        // Save port
        accountProperties.put(  ProtocolProviderFactory.SERVER_PORT,
                                String.valueOf(port));
        // Save strategy
        accountProperties.put(ProtocolProviderFactory.STRATEGY, strategy);

        if (isModification())
        {
            providerFactory.uninstallAccount(protocolProvider.getAccountID());
            this.protocolProvider = null;
            setModification(false);
        }

        try
        {
            String uid = this.generateUID();
            AccountID accountID =
                providerFactory.installAccount(uid, accountProperties);

            ServiceReference serRef =
                providerFactory.getProviderForAccount(accountID);

            protocolProvider =
                (ProtocolProviderService) DictAccRegWizzActivator.bundleContext
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

        this.registration = new DictAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     *
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public DictAccountRegistration getRegistration()
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
        return new Dimension(300, 150);
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
     * Generate the UID for the acount
     * @return the new UID
     */
    private String generateUID()
    {
        String uid;
        int nbAccounts = this.getNumberOfAccounts();
        String host = this.registration.getHost();
        int nbAccountsForHost = this.getNbAccountForHost(host);

        if (nbAccounts == 0 || (this.isModification() && nbAccounts == 1) ||
                nbAccountsForHost == 0
                || (this.isModification() && nbAccountsForHost == 1))
        {
            // We create the first account or we edit the onlyone
            // Or we create the first account for this server or edit the onlyone
            uid = host;
        }
        else
        {
            uid = host + ":" + this.registration.getPort();
        }

        return uid;
    }

    /**
     * Returns the number of accounts stored for the protocol
     * @return the number of accounts stored for the protocol
     */
    private int getNumberOfAccounts()
    {
        ProtocolProviderFactory factory =
            DictAccRegWizzActivator.getDictProtocolProviderFactory();

        return factory.getRegisteredAccounts().size();
    }

    /**
     * Returns the number of account for a given host
     * @param hostName the host
     * @return the number of account for a given host
     */
    private int getNbAccountForHost(String host)
    {
        ProtocolProviderFactory factory =
            DictAccRegWizzActivator.getDictProtocolProviderFactory();

        ArrayList<AccountID> registeredAccounts
            = factory.getRegisteredAccounts();
        int total = 0;

        for (int i = 0; i < registeredAccounts.size(); i++)
        {
            AccountID accountID = registeredAccounts.get(i);

            // The host is always stored at the start
            if (accountID.getUserID().startsWith(host.toLowerCase()))
            {
                total++;
            }
        }
        return total;
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
        firstWizardPage = new FirstWizardPage(this);

        return firstWizardPage.getSimpleForm();
    }
}
