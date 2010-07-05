/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.facebookaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The <tt>FacebookAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Facebook Chat protocol. It allows
 * the user to create and configure a new Facebook account.
 *
 * @author Dai Zhiwei
 */
public class FacebookAccountRegistrationWizard
    implements AccountRegistrationWizard
{

    /**
     * The first page of the facebook account registration wizard.
     */
    private FirstWizardPage firstWizardPage;

    public static final String SERVER_ADDRESS = "chat.facebook.com";

    /**
     * The object that we use to store details on an account that we will be
     * creating.
     */
    private FacebookAccountRegistration registration
        = new FacebookAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private boolean isModification;

    /**
     * Creates an instance of <tt>FacebookAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public FacebookAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.wizardContainer
                .setFinishButtonText(
                    Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.PROTOCOL_ICON);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code> method.
     * Returns the image used to decorate the wizard page
     * 
     * @return byte[] the image used to decorate the wizard page
     */
    public byte[] getPageImage()
    {
        return Resources.getImage(Resources.PAGE_IMAGE);
    }
    
    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * @return String
     */
    public String getProtocolName()
    {
        return Resources.getString("plugin.facebookaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return
            Resources.getString(
                "plugin.facebookaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();

        pages.add(firstWizardPage);
        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        Map<String, String> summaryTable = new Hashtable<String, String>();

        summaryTable.put("User ID", registration.getUsername());
        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     */
    public ProtocolProviderService signin()
    {
        if (!firstWizardPage.isCommitted())
            firstWizardPage.commitPage();

        return signin(registration.getUsername(), null);
    }

    /**
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     */
    public ProtocolProviderService signin(String userName, String password)
    {
        firstWizardPage = null;
        ProtocolProviderFactory factory
            = FacebookAccRegWizzActivator.getFacebookProtocolProviderFactory();

        return this.installAccount(factory,
                                   userName);
    }

    /**
     * Creates an account for the given user and password.
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param user the user identifier 
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String user)
    {
        Map<String, String> accountProperties = new Hashtable<String, String>();

        /* Make the account use the resources specific to Facebook. */
        accountProperties.put(ProtocolProviderFactory.PROTOCOL, 
            ProtocolNames.FACEBOOK);
        accountProperties
            .put(ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                "resources/images/protocol/facebook");

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                "resources/images/protocol/facebook/logo32x32.png");

        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
            SERVER_ADDRESS);

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT, "5222");

        accountProperties.put(ProtocolProviderFactory.RESOURCE, "sip-comm");

        accountProperties.put(ProtocolProviderFactory.RESOURCE_PRIORITY, "10");

        if (registration.isRememberPassword())
        {
            accountProperties.put(  ProtocolProviderFactory.PASSWORD,
                                    registration.getPassword());
        }

        accountProperties.put("SEND_KEEP_ALIVE", Boolean.TRUE.toString());

        if (isModification)
        {
            providerFactory.modifyAccount(protocolProvider, accountProperties);

            this.isModification  = false;

            return protocolProvider;
        }

        Throwable exception = null;

        try
        {
            AccountID accountID = providerFactory.installAccount(
                user, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                FacebookAccRegWizzActivator.bundleContext
                .getService(serRef);
        }
        catch (IllegalArgumentException exc)
        {
            exception = exc;
        }
        catch (IllegalStateException exc)
        {
            exception = exc;
        }

        if (exception != null)
            FacebookAccRegWizzActivator
                .getUIService()
                    .getPopupDialog()
                        .showMessagePopupDialog(
                            exception.getMessage(),
                            Resources.getString("service.gui.ERROR"),
                            PopupDialog.ERROR_MESSAGE);

        return protocolProvider;
    }

    /**
     * Fills the UserID and Password fields in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new FacebookAccountRegistration();

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
    public FacebookAccountRegistration getRegistration()
    {
        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    public Dimension getSize()
    {
        return new Dimension(600, 500);
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
     * Sets the modification property to indicate if this wizard is opened for
     * a modification.
     * 
     * @param isModification indicates if this wizard is opened for modification
     * or for creating a new account. 
     */
    public void setModification(boolean isModification)
    {
        this.isModification = isModification;
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return FirstWizardPage.USER_NAME_EXAMPLE;
    }
    

    /**
     * Disables the simple "Sign in" form.
     */
    public boolean isSimpleFormEnabled()
    {
        return false;
    }

    /**
     * Nothing to do here in the case of Facebook.
     */
    public void webSignup()
    {
        throw new UnsupportedOperationException(
            "The web sign up is not supproted by the facebook wizard.");
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
