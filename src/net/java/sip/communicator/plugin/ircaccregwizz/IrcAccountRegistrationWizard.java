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
package net.java.sip.communicator.plugin.ircaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>IrcAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the IRC protocol. It allows the user
 * to create and configure a new IRC account.
 *
 * @author Lionel Ferreira & Michael Tarantino
 * @author Danny van Heumen
 */
public class IrcAccountRegistrationWizard
    extends DesktopAccountRegistrationWizard
{
    private static final int WIZARD_DIALOG_HEIGHT = 500;

    private static final int WIZARD_DIALOG_WIDTH = 600;

    /**
     * Repeated contact presence task configuration key from
     * ProtocolProviderFactoryIrcImpl here to avoid having to import irc
     * protocol package directly. See
     * ProtocolProviderFactoryIrcImpl.CONTACT_PRESENCE_TASK.
     */
    public static final String CONTACT_PRESENCE_TASK = "CONTACT_PRESENCE_TASK";

    /**
     * Repeated chat room presence task configuration key from
     * ProtocolProviderFactoryIrcImpl here to avoid having to import irc
     * protocol package directly. See
     * ProtocolProviderFactoryIrcImpl.CHAT_ROOM_PRESENCE_TASK.
     */
    public static final String CHAT_ROOM_PRESENCE_TASK =
        "CHAT_ROOM_PRESENCE_TASK";

    /**
     * Property indicating SASL is enabled.
     */
    public static final String SASL_ENABLED = "SASL_ENABLED";

    /**
     * Property name for SASL user.
     */
    public static final String SASL_USERNAME = "SASL_USERNAME";

    /**
     * Property for SASL authorization role.
     */
    public static final String SASL_ROLE = "SASL_ROLE";

    /**
     * Property for resolving DNS names through configured proxy server.
     */
    public static final String RESOLVE_DNS_THROUGH_PROXY =
        "RESOLVE_DNS_THROUGH_PROXY";

    /**
     * Logger.
     */
    private final Logger logger
        = Logger.getLogger(IrcAccountRegistrationWizard.class);

    /**
     * The first page of the IRC account registration wizard.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The object that we use to store details on an account that we will be
     * creating.
     */
    private IrcAccountRegistration registration
        = new IrcAccountRegistration();

    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>IrcAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public IrcAccountRegistrationWizard(final WizardContainer wizardContainer)
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
        return Resources.getImage(Resources.IRC_LOGO);
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
     * @return String
     */
    @Override
    public String getProtocolName()
    {
        return Resources.getString("plugin.ircaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    @Override
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.ircaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
        String userId = "";
        String server = "";
        if (firstWizardPage != null)
        {
            userId = firstWizardPage.getCurrentUserId();
            server = firstWizardPage.getCurrentServer();
        }
        firstWizardPage = new FirstWizardPage(this, userId, server);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    @Override
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        LinkedHashMap<String, String> summaryTable =
            new LinkedHashMap<String, String>();
        String pass = new String();
        String port = new String();

        if (registration.isRequiredPassword())
        {
            pass = "required";
        }
        else
        {
            pass = "not required";
        }

        port = registration.getPort();
        if (!port.equals(""))
        {
            port = ":" + port;
        }
        
        final String yes = Resources.getString("service.gui.YES");
        final String no = Resources.getString("service.gui.NO");

        summaryTable.put(Resources.getString("plugin.ircaccregwizz.USERNAME"),
            registration.getUserID());
        summaryTable.put(Resources.getString("service.gui.PASSWORD"), pass);
        summaryTable.put(
            Resources.getString("plugin.ircaccregwizz.IRC_SERVER"),
            registration.getServer() + port);
        summaryTable.put(
            Resources.getString("plugin.ircaccregwizz.USE_SECURE_CONNECTION"),
            registration.isSecureConnection() ? yes : no);
        summaryTable.put(Resources
            .getString("plugin.ircaccregwizz.RESOLVE_DNS_THROUGH_PROXY"),
            registration.isResolveDnsThroughProxy() ? yes : no);
        summaryTable
            .put(Resources
                .getString("plugin.ircaccregwizz.ENABLE_CONTACT_PRESENCE"),
                registration.isContactPresenceTaskEnabled() ? yes : no);
        summaryTable.put(Resources
            .getString("plugin.ircaccregwizz.ENABLE_CHAT_ROOM_PRESENCE"),
            registration.isChatRoomPresenceTaskEnabled() ? yes : no);
        summaryTable.put(Resources
            .getString("plugin.ircaccregwizz.ENABLE_SASL_AUTHENTICATION"),
            registration.isSaslEnabled() ? yes : no);
        summaryTable.put(
            "SASL " + Resources.getString("plugin.ircaccregwizz.USERNAME"),
            registration.getSaslUser());
        summaryTable.put("SASL "
                + Resources.getString("plugin.ircaccregwizz.SASL_AUTHZ_ROLE"),
            registration.getSaslRole());

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

        String password = null;
        if (registration.isRememberPassword()
                && registration.isRequiredPassword())
        {
            password = registration.getPassword();
        }

        return this.signin(registration.getUserID(), password);
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
    public ProtocolProviderService signin(final String userName,
        final String password) throws OperationFailedException
    {
        ProtocolProviderFactory factory
            = IrcAccRegWizzActivator.getIrcProtocolProviderFactory();
        return this.installAccount(factory, userName, password);
    }

    /**
     * Creates an account for the given user and password.
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param user the user identifier
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    private ProtocolProviderService installAccount(
        final ProtocolProviderFactory providerFactory, final String user,
        final String password) throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
            registration.getServer());

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                "resources/images/protocol/irc/irc32x32.png");

        if (password != null && !password.equals(""))
        {
            accountProperties.put(
                ProtocolProviderFactory.PASSWORD, registration.getPassword());
        }

        if (!registration.getPort().equals(""))
        {
            accountProperties.put(
                ProtocolProviderFactory.SERVER_PORT, registration.getPort());
        }

        accountProperties.put(
                ProtocolProviderFactory.AUTO_CHANGE_USER_NAME,
                Boolean.toString(registration.isAutoChangeNick()));

        accountProperties.put(
            IrcAccountRegistrationWizard.RESOLVE_DNS_THROUGH_PROXY,
            Boolean.toString(registration.isResolveDnsThroughProxy()));

        accountProperties.put(
                ProtocolProviderFactory.NO_PASSWORD_REQUIRED,
                Boolean.toString(!registration.isRequiredPassword()));

        accountProperties.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION,
            Boolean.toString(registration.isSecureConnection()).toString());

        // Presence-based background tasks
        accountProperties.put(CONTACT_PRESENCE_TASK,
            Boolean.toString(registration.isContactPresenceTaskEnabled()));
        accountProperties.put(CHAT_ROOM_PRESENCE_TASK,
            Boolean.toString(registration.isChatRoomPresenceTaskEnabled()));

        // SASL properties
        accountProperties.put(SASL_ENABLED,
            Boolean.toString(registration.isSaslEnabled()));
        accountProperties.put(SASL_USERNAME, registration.getSaslUser());
        accountProperties.put(SASL_ROLE, registration.getSaslRole());

        if (isModification())
        {
            providerFactory.modifyAccount(this.protocolProvider,
                accountProperties);
            setModification(false);
            return this.protocolProvider;
        }

        try
        {
            AccountID accountID = providerFactory.installAccount(
                user, accountProperties);

            ServiceReference<ProtocolProviderService> serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                IrcAccRegWizzActivator.bundleContext
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
     * Fills the UserID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    @Override
    public void loadAccount(final ProtocolProviderService protocolProvider)
    {
        setModification(true);

        this.protocolProvider = protocolProvider;

        this.registration = new IrcAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     *
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public IrcAccountRegistration getRegistration()
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
        return new Dimension(WIZARD_DIALOG_WIDTH, WIZARD_DIALOG_HEIGHT);
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
    public Object getSimpleForm(final boolean isCreateAccount)
    {
        firstWizardPage = new FirstWizardPage(this, "", "");

        return firstWizardPage.getSimpleForm();
    }
}
