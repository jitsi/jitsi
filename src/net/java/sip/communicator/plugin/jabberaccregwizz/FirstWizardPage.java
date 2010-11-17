/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user
 * ID and the password of the account.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage
{
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private final AccountPanel accountPanel;

    private final ConnectionPanel connectionPanel;

    private final IceConfigPanel iceConfigPanel;

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private final JabberAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    private boolean isServerOverridden = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(JabberAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        accountPanel = new AccountPanel(this);
        connectionPanel = new ConnectionPanel(wizard);
        iceConfigPanel = new IceConfigPanel();

        this.init();
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        JTabbedPane tabbedPane = new JTabbedPane();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tabbedPane.addTab(  Resources.getString("service.gui.ACCOUNT"),
                            accountPanel);
        tabbedPane.addTab(  Resources.getString("service.gui.CONNECTION"),
                            connectionPanel);
        tabbedPane.addTab(  Resources.getString("service.gui.ADVANCED"),
                            iceConfigPanel);

        add(tabbedPane, BorderLayout.NORTH);
        tabbedPane.setSelectedIndex(0);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     *
     * @return the id of the first wizard page.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     *
     * @return the id of the next wizard page.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier. In this case it's null because this is the first
     * wizard page.
     *
     * @return the identifier of the previous wizard page
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     *
     * @return this wizard page.
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the User ID field is empty.
     */
    public void pageShowing()
    {
        this.setNextButtonAccordingToUserIDAndResource();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        JabberAccountRegistration registration = wizard.getRegistration();

        String userID = accountPanel.getUsername();

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);
        registration.setPassword(new String(accountPanel.getPassword()));
        registration.setRememberPassword(accountPanel.isRememberPassword());

        registration.setServerAddress(connectionPanel.getServerAddress());
        registration.setSendKeepAlive(connectionPanel.isSendKeepAlive());
        registration.setGmailNotificationEnabled(
            connectionPanel.isGmailNotificationsEnabled());
        registration.setResource(connectionPanel.getResource());

        String serverPort = connectionPanel.getServerPort();
        if (serverPort != null)
            registration.setPort(Integer.parseInt(serverPort));

        String priority = connectionPanel.getPriority();
        if (priority != null)
            registration.setPriority(Integer.parseInt(priority));

        registration.setUseIce(iceConfigPanel.isUseIce());
        registration.setAutoDiscoverStun(iceConfigPanel.isAutoDiscoverStun());

        //we will be reentering all stun servers so let's make sure we clear
        //the servers vector in case we already did that with a "Next".
        registration.getAdditionalStunServers().clear();

        List<StunServerDescriptor> stunServers
            = iceConfigPanel.getAdditionalStunServers();

        for (StunServerDescriptor descriptor : stunServers)
            registration.addStunServer(descriptor);

        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;

        this.isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * UserID field is empty.
     */
    void setNextButtonAccordingToUserIDAndResource()
    {
        boolean nextFinishButtonEnabled = false;

        String userID = accountPanel.getUsername();
        if ((userID != null) && !userID.equals(""))
        {
            String resource = connectionPanel.getResource();
            if ((resource != null) && !resource.equals(""))
            {
                nextFinishButtonEnabled = true;
            }
        }

        wizard
            .getWizardContainer()
                .setNextFinishButtonEnabled(nextFinishButtonEnabled);
    }

    /**
     * Dummy implementation
     */
    public void pageHiding() {}

    /**
     * Dummy implementation
     */
    public void pageShown() {}

    /**
     * Dummy implementation
     */
    public void pageBack() {}

    /**
     * Fills the User ID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();
        Map<String, String> accountProperties
            = accountID.getAccountProperties();

        String password
            = accountProperties.get(ProtocolProviderFactory.PASSWORD);

        accountPanel.setRememberPassword(false);
        accountPanel.setUsername(accountID.getUserID());

        if (password != null)
        {
            accountPanel.setPassword(password);
            accountPanel.setRememberPassword(true);
        }

        String serverAddress
            = accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        connectionPanel.setServerAddress(serverAddress);

        String serverPort
            = accountProperties.get(ProtocolProviderFactory.SERVER_PORT);

        connectionPanel.setServerPort(serverPort);

        boolean keepAlive
            = Boolean.parseBoolean(accountProperties.get("SEND_KEEP_ALIVE"));

        connectionPanel.setSendKeepAlive(keepAlive);

        boolean gmailNotificationEnabled
            = Boolean.parseBoolean(
                    accountProperties.get("GMAIL_NOTIFICATIONS_ENABLED"));

        connectionPanel.setGmailNotificationsEnabled(gmailNotificationEnabled);

        String resource
            = accountProperties.get(ProtocolProviderFactory.RESOURCE);

        connectionPanel.setResource(resource);

        String priority
            = accountProperties.get(ProtocolProviderFactory.RESOURCE_PRIORITY);

        connectionPanel.setPriority(priority);

        String useIce =
            accountProperties.get(ProtocolProviderFactory.IS_USE_ICE);
        boolean isUseIce = Boolean.parseBoolean(
                (useIce != null && useIce.length() != 0) ? useIce : "true");

        iceConfigPanel.setUseIce(isUseIce);

        boolean isAutoDiscoverStun
            = Boolean.parseBoolean(
                accountProperties.get(
                    ProtocolProviderFactory.AUTO_DISCOVER_STUN));

        iceConfigPanel.setAutoDiscoverStun(isAutoDiscoverStun);

        for (int i = 0; i < StunServerDescriptor.MAX_STUN_SERVER_COUNT; i ++)
        {
            StunServerDescriptor stunServer
                = StunServerDescriptor.loadDescriptor(
                    accountProperties, ProtocolProviderFactory.STUN_PREFIX + i);



            // If we don't find a stun server with the given index, it means
            // that there're no more servers left i nthe table so we've nothing
            // more to do here.
            if (stunServer == null)
                break;

            iceConfigPanel.addStunServer(stunServer);
        }

        this.isServerOverridden
            = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    false);
    }

    /**
     * Returns the simple form.
     * @return the simple form
     */
    public Object getSimpleForm()
    {
        return accountPanel;
    }

    /**
     * Indicates if this page has been already committed.
     * @return <tt>true</tt> if this page has been already committed,
     * <tt>false</tt> - otherwise
     */
    public boolean isCommitted()
    {
        return isCommitted;
    }

    /**
     * Parse the server part from the jabber id and set it to server as default
     * value. If the server was overriden do nothing.
     * @param username the username from which to extract the server address
     */
    void setServerFieldAccordingToUsername(String username)
    {
        if (!wizard.isModification() || !isServerOverridden)
        {
            connectionPanel.setServerAddress(
                wizard.getServerFromUserName(username));
        }
    }

    /**
     * Returns the contained connection panel.
     * @return the contained connection panel
     */
    ConnectionPanel getConnectionPanel()
    {
        return connectionPanel;
    }
}
