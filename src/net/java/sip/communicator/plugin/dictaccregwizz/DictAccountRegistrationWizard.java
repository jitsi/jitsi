/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
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
    implements AccountRegistrationWizard
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
     * The container of the wizard.
     */
    private WizardContainer wizardContainer;

    /**
     * The protocole provider.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * Tells us if the is a modification wiazrd or not.
     */
    private boolean isModification;

    /**
     * Creates an instance of <tt>DictAccountRegistrationWizard</tt>.
     * 
     * @param wizardContainer the wizard container, where this wizard is added
     */
    public DictAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * @return Returns the icon to be used for this wizard.
     */
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
    public byte[] getPageImage()
    {
        return Resources.getImage(Resources.PAGE_IMAGE);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method.
     * @return Returns the protocol name for this wizard.
     */
    public String getProtocolName()
    {
        return Resources.getString("plugin.dictaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method.
     * @return Returns the description of the protocol for this wizard.
     */
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.dictaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     *
     * @return Returns the set of pages contained in this wizard.
     */
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
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        Map<String, String> summaryTable = new LinkedHashMap<String, String>();

        summaryTable.put("Host", registration.getHost());
        summaryTable.put("Port", String.valueOf(registration.getPort()));
        summaryTable.put("Strategy", registration.getStrategy().getName());

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     */
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return signin(registration.getUserID(), null);
    }

    /**
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     */
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

        if (isModification)
        {
            providerFactory.uninstallAccount(protocolProvider.getAccountID());
            this.protocolProvider = null;
            this.isModification  = false;
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
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new DictAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Indicates if this wizard is opened for modification or for creating a
     * new account.
     * 
     * @return <code>true</code> if this wizard is opened for modification and
     * <code>false</code> otherwise.
     */
    public boolean isModification()
    {
        return isModification;
    }
    
    /**
     * Sets if this wizard is opened for modification or for creating a
     * new account.
     * 
     * @param b <code>True</code> if this wizard is opened for modification and
     * <code>false</code> otherwise.
     */
    public void setModification(boolean b)
    {
        this.isModification = b;
    }

    /**
     * Returns the wizard container, where all pages are added.
     * 
     * @return the wizard container, where all pages are added
     */
    public WizardContainer getWizardContainer()
    {
        return wizardContainer;
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
    public Dimension getSize()
    {
        return new Dimension(300, 150);
    }
    
    /**
     * Returns the identifier of the page to show first in the wizard.
     * @return the identifier of the page to show first in the wizard.
     */
    public Object getFirstPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Returns the identifier of the page to show last in the wizard.
     * @return the identifier of the page to show last in the wizard.
     */
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
                nbAccountsForHost == 0 || (this.isModification() && nbAccountsForHost == 1))
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
    public String getUserNameExample()
    {
        return null;
    }

    /**
     * Disables the simple "Sign in" form.
     */
    public boolean isSimpleFormEnabled()
    {
        return false;
    }

    /**
     * Nothing to do here in the case of dictionary.
     */
    public void webSignup()
    {
        throw new UnsupportedOperationException(
            "The web sign up is not supproted by the dictionary wizard.");
    }

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

    public Object getSimpleForm()
    {
        firstWizardPage = new FirstWizardPage(this);

        return firstWizardPage.getSimpleForm();
    }
}
