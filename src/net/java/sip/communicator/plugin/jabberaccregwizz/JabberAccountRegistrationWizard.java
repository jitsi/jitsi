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
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * The <tt>JabberAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Jabber protocol. It should allow
 * the user to create and configure a new Jabber account.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistrationWizard
    extends DesktopAccountRegistrationWizard
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(JabberAccountRegistrationWizard.class);

    /**
     * The first wizard page.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The registration object, where all properties related to the account
     * are stored.
     */
    private JabberAccountRegistration registration;

    /**
     * The <tt>ProtocolProviderService</tt> of this account.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * The create account form.
     */
    private JabberAccountCreationForm createAccountService;

    /**
     * Creates an instance of <tt>JabberAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public JabberAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        setWizardContainer(wizardContainer);

        wizardContainer
            .setFinishButtonText(Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    @Override
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
        return Resources.getString("plugin.jabberaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    @Override
    public String getProtocolDescription()
    {
        return Resources
            .getString("plugin.jabberaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        return getPages(new JabberAccountRegistration());
    }

    /**
     * Returns the set of pages contained in this wizard.
     *
     * @param registration the registration object
     * @return Iterator
     */
    public Iterator<WizardPage> getPages(JabberAccountRegistration registration)
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();

        // create new registration, our container needs the pages
        // this means this is a new wizard and we must reset all data
        // it will be invoked and when the wizard cleans and unregister
        // our pages, but this fix don't hurt in this situation.
        this.registration = registration;

        if (firstWizardPage == null)
            firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    @Override
    public Iterator<Map.Entry<String,String>> getSummary()
    {
        Hashtable<String,String> summaryTable = new Hashtable<String,String>();

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.USERNAME"),
            registration.getUserID());

        summaryTable.put(
            Resources.getString("service.gui.REMEMBER_PASSWORD"),
            Boolean.toString(registration.isRememberPassword()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.SERVER"),
            registration.getServerAddress());

        summaryTable.put(
            Resources.getString("service.gui.PORT"),
            String.valueOf(registration.getServerPort()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.ENABLE_KEEP_ALIVE"),
            String.valueOf(registration.isSendKeepAlive()));

        summaryTable.put(
            Resources.getString(
                        "plugin.jabberaccregwizz.ENABLE_GMAIL_NOTIFICATIONS"),
            String.valueOf(registration.isGmailNotificationEnabled()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.RESOURCE"),
            registration.getResource());

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.PRIORITY"),
            String.valueOf(registration.getPriority()));

        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.DTMF_METHOD"),
            registration.getDTMFMethod());

        summaryTable.put(
            Resources.getString(
                "plugin.sipaccregwizz.DTMF_MINIMAL_TONE_DURATION"),
            registration.getDtmfMinimalToneDuration());

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account defined in this wizard.
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    @Override
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return
            firstWizardPage.isCommitted()
                ? signin(registration.getUserID(), registration.getPassword())
                : null;
    }

    /**
     * Installs the account defined in this wizard.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService signin(
            final String userName,
            final String password)
        throws OperationFailedException
    {
        /*
         * If firstWizardPage is null we are requested sign-in from initial
         * account registration form we must init firstWizardPage in order to
         * init default values
         * Pawel: firstWizardPage is never null, and commitPage fails with no
         * user ID provided for simple account wizard. Now userName and password
         * are reentered here.
         */
        final AccountPanel accPanel
            = (AccountPanel) firstWizardPage.getSimpleForm();
        /*
         * XXX Swing is not thread safe! We've experienced deadlocks on OS X
         * upon invoking accPanel's setters. In order to address them, (1)
         * invoke accPanel's setters on the AWT event dispatching thread and (2)
         * do it only if absolutely necessary.
         */
        String accPanelUsername = accPanel.getUsername();
        boolean equals = false;
        final boolean rememberPassword = (password != null);

        if (StringUtils.isEquals(accPanelUsername, userName))
        {
            char[] accPanelPasswordChars = accPanel.getPassword();
            char[] passwordChars
                = (password == null) ? null : password.toCharArray();

            if (accPanelPasswordChars == null)
                equals = ((passwordChars == null) || passwordChars.length == 0);
            else if (passwordChars == null)
                equals = (accPanelPasswordChars.length == 0);
            else
                equals = Arrays.equals(accPanelPasswordChars, passwordChars);
            if (equals)
            {
                boolean accPanelRememberPassword
                    = accPanel.isRememberPassword();

                equals = (accPanelRememberPassword == rememberPassword);
            }
        }
        if (!equals)
        {
            try
            {
                if(SwingUtilities.isEventDispatchThread())
                {
                    accPanel.setUsername(userName);
                    accPanel.setPassword(password);
                    accPanel.setRememberPassword(rememberPassword);
                }
                else
                {
                    SwingUtilities.invokeAndWait(
                            new Runnable()
                            {
                                public void run()
                                {
                                    accPanel.setUsername(userName);
                                    accPanel.setPassword(password);
                                    accPanel.setRememberPassword(
                                        rememberPassword);
                                }
                            });
                }
            }
            catch (Exception e)
            {
                if (e instanceof OperationFailedException)
                {
                    throw (OperationFailedException) e;
                }
                else
                {
                    throw new OperationFailedException(
                            "Failed to set username and password on "
                                + accPanel.getClass().getName(),
                            OperationFailedException.INTERNAL_ERROR,
                            e);
                }
            }
        }

        if(!firstWizardPage.isCommitted())
            firstWizardPage.commitPage();
        if(!firstWizardPage.isCommitted())
        {
            throw new OperationFailedException(
                    "Could not confirm data.",
                    OperationFailedException.GENERAL_ERROR);
        }

        ProtocolProviderFactory factory
            = JabberAccRegWizzActivator.getJabberProtocolProviderFactory();

        return
            installAccount(
                    factory,
                    registration.getUserID(), // The user id may get changed.
                                              // Server part can be added in the
                                              // data commit.
                    password);
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
        if(logger.isTraceEnabled())
        {
            logger.trace("Preparing to install account for user " + userName);
        }
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        String protocolIconPath = getProtocolIconPath();

        String accountIconPath = getAccountIconPath();

        registration.storeProperties(
                userName, passwd,
                protocolIconPath, accountIconPath,
                accountProperties);

        accountProperties.put(ProtocolProviderFactory.IS_PREFERRED_PROTOCOL,
                              Boolean.toString(isPreferredProtocol()));
        accountProperties.put(ProtocolProviderFactory.PROTOCOL, getProtocol());

        if (isModification())
        {
            providerFactory.modifyAccount(  protocolProvider,
                accountProperties);

            setModification(false);

            return protocolProvider;
        }

        try
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Will install account for user " + userName
                             + " with the following properties."
                             + accountProperties);
            }

            AccountID accountID = providerFactory.installAccount(
                userName,
                accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                JabberAccRegWizzActivator.bundleContext
                .getService(serRef);
        }
        catch (IllegalArgumentException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Username, password or server is null.",
                OperationFailedException.ILLEGAL_ARGUMENT);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Account already exists.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Throwable exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Failed to add account.",
                OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Fills the User ID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    @Override
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        setModification(true);

        this.protocolProvider = protocolProvider;

        this.registration = new JabberAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     *
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public JabberAccountRegistration getRegistration()
    {
        if (registration == null)
            registration = new JabberAccountRegistration();

        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    @Override
    public Dimension getSize()
    {
        return new Dimension(300, 480);
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
        return "Ex: johnsmith@jabber.org";
    }

    /**
     * Opens the Gmail signup URI in the OS's default browser.
     */
    @Override
    public void webSignup()
    {
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
        // when creating first wizard page, create and new
        // AccountRegistration to avoid reusing old instances and
        // data left from old registrations
        return getSimpleForm(new JabberAccountRegistration(), isCreateAccount);
    }

    /**
     * Returns the first wizard page.
     *
     * @param registration the registration object
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return the first wizard page.
     */
    public Object getSimpleForm(JabberAccountRegistration registration,
                                boolean isCreateAccount)
    {
        this.registration = registration;

        firstWizardPage = new FirstWizardPage(this);

        return firstWizardPage.getSimpleForm();
    }

    /**
     * Returns the protocol name as listed in "ProtocolNames" or just the name
     * of the service.
     * @return the protocol name
     */
    public String getProtocol()
    {
        return ProtocolNames.JABBER;
    }

    /**
     * Returns the protocol icon path.
     * @return the protocol icon path
     */
    public String getProtocolIconPath()
    {
        return null;
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    public String getAccountIconPath()
    {
        return null;
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    protected JabberAccountCreationFormService getCreateAccountService()
    {
        if (createAccountService == null)
            createAccountService = new JabberAccountCreationForm();

        return createAccountService;
    }

    /**
     * Returns the display label used for the jabber id field.
     * @return the jabber id display label string.
     */
    protected String getUsernameLabel()
    {
        return Resources.getString("plugin.jabberaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getCreateAccountButtonLabel()
    {
        return Resources.getString(
            "plugin.jabberaccregwizz.NEW_ACCOUNT_TITLE");
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return Resources.getString(
            "plugin.jabberaccregwizz.REGISTER_NEW_ACCOUNT_TEXT");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getExistingAccountLabel()
    {
        return Resources.getString("plugin.jabberaccregwizz.EXISTING_ACCOUNT");
    }

    /**
     * Return the string for home page link label.
     * @return the string for home page link label
     */
    protected String getHomeLinkLabel()
    {
        return null;
    }

    /**
     * Return the wizard's protocolProvider, if the wizard modifies an
     * account, null if it creates a new one
     * @return the wizard's protocolProvider
     */
    public ProtocolProviderService getProtocolProvider()
    {
        if(isModification())
            return protocolProvider;
        return null;
    }
}
