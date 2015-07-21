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
package net.java.sip.communicator.plugin.simpleaccreg;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>NoAccountFoundPage</tt> is the page shown in the account
 * registration wizard shown in the beginning of the program, when no registered
 * accounts are found.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class InitialAccountRegistrationFrame
    extends SIPCommFrame
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private ConfigurationService configurationService;

    /**
     * The <tt>Logger</tt> used by this <tt>InitialAccountRegistrationFrame</tt>
     * for logging output.
     */
    private final Logger logger
        = Logger.getLogger(InitialAccountRegistrationFrame.class);

    private final TransparentPanel mainAccountsPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private final TransparentPanel accountsPanel
        = new TransparentPanel(new GridLayout(0, 2, 10, 10));

    private final TransparentPanel southPanel
        = new TransparentPanel(new BorderLayout());

    private final JButton signinButton
        = new JButton(Resources.getString("service.gui.SIGN_IN"));

    private final Collection<AccountRegistrationPanel> registrationForms =
        new Vector<AccountRegistrationPanel>();

    /**
     * Creates an instance of <tt>NoAccountFoundPage</tt>.
     */
    public InitialAccountRegistrationFrame()
    {
        super(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(buttonPanel, BorderLayout.EAST);
        JButton cancelButton
            = new JButton(Resources.getString("service.gui.CANCEL"));

        this.setTitle(Resources.getString("service.gui.SIGN_IN"));

        this.setIconImage(
            Resources.getImage("service.gui.SIP_COMMUNICATOR_LOGO").getImage());

        // In order to have the same icon when using option panes
        JOptionPane.getRootFrame().setIconImage(
            Resources.getImage("service.gui.SIP_COMMUNICATOR_LOGO").getImage());

        mainAccountsPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.getContentPane().add(mainPanel);

        mainPanel.add(createTitleComponent(), BorderLayout.NORTH);
        JScrollPane scroller = new JScrollPane(mainAccountsPanel);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        mainPanel.add(scroller, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        mainAccountsPanel.add(accountsPanel, BorderLayout.CENTER);

        initProvisioningPanel();

        mainAccountsPanel.setOpaque(false);
        accountsPanel.setOpaque(false);
        buttonPanel.setOpaque(false);

        SigninActionListener actionListener = new SigninActionListener();

        signinButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);

        buttonPanel.add(signinButton);
        buttonPanel.add(cancelButton);

        this.getRootPane().setDefaultButton(signinButton);

        this.initAccountWizards();

        // Create the default group
        String groupName
            = Resources.getApplicationProperty("impl.gui.DEFAULT_GROUP_NAME");

        if(groupName != null && groupName.length() > 0)
        {
            MetaContactListService contactList =
                SimpleAccountRegistrationActivator.getContactList();
            Iterator<MetaContactGroup> iter
                = contactList.getRoot().getSubgroups();
            while (iter.hasNext())
            {
                MetaContactGroup gr = iter.next();
                if (groupName.equals(gr.getGroupName()))
                    return;
            }

            contactList
                .createMetaContactGroup(contactList.getRoot(), groupName);

            getConfigurationService()
                .setProperty(
                    "net.java.sip.communicator.impl.gui.addcontact.lastContactParent",
                    groupName);
        }

        this.getRootPane().validate();
        this.pack();

        // if the screen height is sufficiently large, expand the window size
        // so that no scrolling is needed
        if (scroller.getViewport().getHeight()
            < Toolkit.getDefaultToolkit().getScreenSize().getHeight() - 230)
        {
            this.setSize(scroller.getViewport().getWidth() + 100,
                scroller.getViewport().getHeight() + 150);
        }
        else
        {
            // otherwise add some width so that no horizontal scrolling is
            // needed
            this.setSize(this.getSize().width + 20,
                this.getSize().height - 10);
        }
    }

    /**
     * Initializes the provisioning panel.
     */
    private void initProvisioningPanel()
    {
        String isInitialProv = SimpleAccountRegistrationActivator.getResources()
            .getSettingsString(
                "plugin.provisioning.IS_INITIAL_PROVISIONING_LINK");

        if (isInitialProv != null && isInitialProv.length() > 0
            && !Boolean.parseBoolean(isInitialProv))
            return;

        String useProvisioningString = SimpleAccountRegistrationActivator
            .getResources().getI18NString("service.gui.USE_PROVISIONING");

        final JLabel provisioningLabel =
            new JLabel("<html><a href=''>"
                + useProvisioningString
                + "</a></html>");

        provisioningLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        provisioningLabel.setToolTipText(useProvisioningString);

        provisioningLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                ConfigurationContainer configContainer
                    = SimpleAccountRegistrationActivator.getUIService()
                        .getConfigurationContainer();

                ConfigurationForm advancedConfigForm
                    = SimpleAccountRegistrationActivator.getAdvancedConfigForm();

                if (advancedConfigForm != null)
                {
                    configContainer.setSelected(advancedConfigForm);

                    if (advancedConfigForm instanceof ConfigurationContainer)
                    {
                        ConfigurationForm provisioningForm
                            = SimpleAccountRegistrationActivator
                                .getProvisioningConfigForm();

                        if (provisioningForm != null)
                        {
                            ((ConfigurationContainer) advancedConfigForm)
                                .setSelected(provisioningForm);
                        }
                    }
                }

                configContainer.setVisible(true);
            }
        });

        southPanel.add(provisioningLabel, BorderLayout.WEST);
    }

    private void initAccountWizards()
    {
        String simpleWizards
            = SimpleAccountRegistrationActivator.getConfigService()
                .getString("plugin.simpleaccreg.PROTOCOL_ORDER");

        StringTokenizer tokenizer = new StringTokenizer(simpleWizards, "|");

        ServiceReference[] serviceRefs = null;
        while (tokenizer.hasMoreTokens())
        {
            String protocolToken = tokenizer.nextToken();

            String osgiFilter = "("
                + ProtocolProviderFactory.PROTOCOL
                + "="+protocolToken+")";

            try
            {
                serviceRefs = SimpleAccountRegistrationActivator.bundleContext
                    .getServiceReferences(
                        AccountRegistrationWizard.class.getName(), osgiFilter);

                if (serviceRefs != null && serviceRefs.length > 0)
                {
                    AccountRegistrationWizard wizard
                        = (AccountRegistrationWizard)

                        SimpleAccountRegistrationActivator
                        .bundleContext.getService(serviceRefs[0]);

                    this.addAccountRegistrationForm(wizard);
                }
            }
            catch (InvalidSyntaxException ex)
            {
                logger.error("GuiActivator : ", ex);
            }
        }
    }

    /**
     * The account registration panel.
     */
    private class AccountRegistrationPanel
        extends JPanel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private JLabel usernameLabel
            = new JLabel(Resources.getString(
                "plugin.simpleaccregwizz.LOGIN_USERNAME"));

        private JLabel passwordLabel
            = new JLabel(Resources.getString("service.gui.PASSWORD"));

        private JTextField usernameField = new JTextField();

        private JLabel usernameExampleLabel = new JLabel();

        private JPasswordField passwordField = new JPasswordField();

        private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 5, 0));

        private JPanel fieldsPanel = new JPanel(new GridLayout(0, 1, 5, 0));

        private JPanel emptyPanel = new JPanel();

        private JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        private JPanel iconDescriptionPanel = new JPanel(new BorderLayout());

        private JTextArea descriptionArea = new JTextArea();

        private final AccountRegistrationWizard wizard;

        public AccountRegistrationPanel(
            AccountRegistrationWizard accountWizard,
            boolean isPreferredWizard)
        {
            super(new BorderLayout(5, 5));

            this.wizard = accountWizard;

            // Obtain the simple form in order to initialize all predefined
            // values.
            // TODO: Use the simple form instead of creating new fields and
            // panels here.
            wizard.getSimpleForm(false);

            JLabel protocolLabel = new JLabel();
            JPanel inputRegisterPanel = new JPanel(new BorderLayout());

            this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            this.setOpaque(false);

            this.inputPanel.setOpaque(false);
            this.labelsPanel.setOpaque(false);
            this.fieldsPanel.setOpaque(false);
            this.emptyPanel.setOpaque(false);
            inputRegisterPanel.setOpaque(false);
            this.iconDescriptionPanel.setOpaque(false);

            this.add(inputRegisterPanel, BorderLayout.CENTER);

            inputRegisterPanel.add(inputPanel, BorderLayout.NORTH);

            if (((wizard instanceof ExtendedAccountRegistrationWizard)
                    && ((ExtendedAccountRegistrationWizard) wizard)
                        .isSignupSupported()) || wizard.isWebSignupSupported())
            {
                String textKey =
                    isPreferredWizard ? "plugin.simpleaccregwizz.SPECIAL_SIGNUP"
                        : "plugin.simpleaccregwizz.SIGNUP";
                JLabel signupLabel =
                    new JLabel("<html><a href=''>"
                        + Resources.getString(textKey) + "</a></html>",
                        JLabel.RIGHT);

                signupLabel.setFont(signupLabel.getFont().deriveFont(10f));
                signupLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                signupLabel.setToolTipText(
                        Resources.getString(
                        "plugin.simpleaccregwizz.SPECIAL_SIGNUP"));
                signupLabel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        try
                        {
                            if ((wizard
                                    instanceof ExtendedAccountRegistrationWizard)
                                && ((ExtendedAccountRegistrationWizard) wizard)
                                    .isSignupSupported())
                            {
                                showCreateAccountWindow(wizard);
                                dispose();
                            }
                            else if (wizard.isWebSignupSupported())
                            {
                                wizard.webSignup();
                            }
                        }
                        catch (UnsupportedOperationException ex)
                        {
                            // This should not happen, because we check if the
                            // operation is supported, before adding the sign
                            // up.
                            logger.error("The web sign up is not supported.",
                                ex);
                        }
                    }
                });

                inputRegisterPanel.add(signupLabel, BorderLayout.SOUTH);
            }

            this.inputPanel.add(labelsPanel, BorderLayout.WEST);

            this.inputPanel.add(fieldsPanel, BorderLayout.CENTER);

            this.iconDescriptionPanel.add(
                protocolLabel, BorderLayout.NORTH);

            protocolLabel.setFont(
                protocolLabel.getFont().deriveFont(Font.BOLD, 14f));
            this.usernameExampleLabel.setForeground(Color.DARK_GRAY);
            this.usernameExampleLabel.setFont(
                usernameExampleLabel.getFont().deriveFont(8f));
            this.emptyPanel.setMaximumSize(new Dimension(40, 25));

            this.labelsPanel.add(usernameLabel);
            this.labelsPanel.add(emptyPanel);
            this.labelsPanel.add(passwordLabel);

            this.fieldsPanel.add(usernameField);
            this.fieldsPanel.add(usernameExampleLabel);
            this.fieldsPanel.add(passwordField);

            this.usernameExampleLabel.setText(wizard.getUserNameExample());

            protocolLabel.setText(wizard.getProtocolName());

            Image image = null;
            try
            {
                image = ImageIO.read(
                    new ByteArrayInputStream(wizard.getPageImage()));
            }
            catch (IOException e)
            {
                logger.error("Unable to load image.", e);
            }

            if (image != null)
            {
                image = image.getScaledInstance(28, 28, Image.SCALE_SMOOTH);

                protocolLabel.setIcon(new ImageIcon(image));
            }

            if (isPreferredWizard)
            {
                descriptionArea.setBorder(BorderFactory
                    .createEmptyBorder(10, 0, 0, 0));

                descriptionArea.setFont(
                    descriptionArea.getFont().deriveFont(10f));
                descriptionArea.setPreferredSize(new Dimension(220, 50));
                descriptionArea.setLineWrap(true);
                descriptionArea.setWrapStyleWord(true);
                descriptionArea.setText(wizard.getProtocolDescription());
                descriptionArea.setEditable(false);
                descriptionArea.setOpaque(false);

                this.iconDescriptionPanel.add(
                    descriptionArea, BorderLayout.CENTER);

                this.add(iconDescriptionPanel, BorderLayout.WEST);
            }
            else
            {
                this.add(iconDescriptionPanel, BorderLayout.NORTH);
            }
        }

        @Override
        public void paintComponent(Graphics g)
        {
            // do the superclass behavior first
            super.paintComponent(g);

            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                Graphics2D g2d = (Graphics2D) g;

                // paint the background with the chosen color
                g2d.setColor(Color.GRAY);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
            }
            finally
            {
                g.dispose();
            }
        }

        public boolean isFilled()
        {
            String username = usernameField.getText();
            return (username != null) && (username.length() > 0);
        }

        public void signin() throws OperationFailedException
        {
            ProtocolProviderService protocolProvider =
                wizard.signin(usernameField.getText(), new String(passwordField
                    .getPassword()));

            saveAccountWizard(protocolProvider, wizard);
        }
    }

    /**
     * Adds a simple account registration form corresponding to the given
     * <tt>AccountRegistrationWizard</tt>.
     *
     * @param wizard the <tt>AccountRegistrationWizard</tt>, which gives us
     * information to fill our simple form.
     */
    private void addAccountRegistrationForm(AccountRegistrationWizard wizard)
    {
        // We don't need to add wizards that are not interested in a
        // simple sign in form.
        if (!wizard.isSimpleFormEnabled())
            return;

        String preferredWizardName
            = Resources.getLoginProperty("impl.gui.PREFERRED_ACCOUNT_WIZARD");

        AccountRegistrationPanel accountPanel;

        if (preferredWizardName != null
            && preferredWizardName.equals(wizard.getClass().getName()))
        {
            accountPanel = new AccountRegistrationPanel(wizard, true);

            mainAccountsPanel.add(
                accountPanel,
                BorderLayout.NORTH);

            mainAccountsPanel.validate();
        }
        else
        {
            accountPanel = new AccountRegistrationPanel(wizard, false);

            accountsPanel.add(accountPanel);

            accountsPanel.validate();
        }

        this.registrationForms.add(accountPanel);
    }

    /**
     * Handles the event triggered by the "service.gui.SIGN_IN" button.
     */
    private class SigninActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            JButton button = (JButton) evt.getSource();

            if (button.equals(signinButton))
            {
                Iterator<AccountRegistrationPanel> regIterator =
                    registrationForms.iterator();

                if (regIterator.hasNext())
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                while(regIterator.hasNext())
                {
                    AccountRegistrationPanel regForm = regIterator.next();

                    try
                    {
                        if (regForm.isFilled())
                            regForm.signin();
                    }
                    catch (OperationFailedException e)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("The sign in operation has failed.");

                        PopupDialog popupDialog
                            = SimpleAccountRegistrationActivator.getUIService()
                                .getPopupDialog();

                        if (e.getErrorCode()
                            == OperationFailedException.ILLEGAL_ARGUMENT)
                        {
                            popupDialog.showMessagePopupDialog(
                                Resources.getString(
                                    "service.gui.USERNAME_NULL"),
                                Resources.getString(
                                    "service.gui.ERROR"),
                                PopupDialog.ERROR_MESSAGE);
                        }
                        else if (e.getErrorCode()
                                    == OperationFailedException
                                        .IDENTIFICATION_CONFLICT)
                        {
                            popupDialog.showMessagePopupDialog(
                                Resources.getString(
                                    "service.gui.USER_EXISTS_ERROR"),
                                Resources.getString(
                                    "service.gui.ERROR"),
                                PopupDialog.ERROR_MESSAGE);
                        }
                        else if (e.getErrorCode()
                                    == OperationFailedException
                                        .SERVER_NOT_SPECIFIED)
                        {
                            popupDialog.showMessagePopupDialog(
                                Resources.getString(
                                    "service.gui.SPECIFY_SERVER"),
                                Resources.getString(
                                    "service.gui.ERROR"),
                                PopupDialog.ERROR_MESSAGE);
                        }
                    }
                }
            }

            InitialAccountRegistrationFrame initialAccountRegistrationFrame =
                InitialAccountRegistrationFrame.this;
            initialAccountRegistrationFrame.setVisible(false);
            initialAccountRegistrationFrame.dispose();
        }
    }

    /**
     * Saves the (protocol provider, wizard) pair in through the
     * <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider to save
     * @param wizard the wizard to save
     */
    private void saveAccountWizard(ProtocolProviderService protocolProvider,
        AccountRegistrationWizard wizard)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        ConfigurationService configService = getConfigurationService();

        List<String> accounts = configService.getPropertyNamesByPrefix(prefix, true);
        boolean savedAccount = false;

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID()
                .getAccountUniqueID()))
            {

                configService.setProperty(accountRootPropName + ".wizard",
                    wizard.getClass().getName().replace('.', '_'));

                savedAccount = true;
            }
        }

        if (!savedAccount)
        {
            String accNodeName =
                "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage =
                "net.java.sip.communicator.impl.gui.accounts." + accNodeName;

            configService.setProperty(accountPackage, protocolProvider
                .getAccountID().getAccountUniqueID());

            configService.setProperty(accountPackage + ".wizard", wizard);
        }
    }

    /**
     * Returns the <tt>ConfigurationService</tt>.
     *
     * @return the <tt>ConfigurationService</tt>
     */
    public ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            BundleContext bundleContext =
                SimpleAccountRegistrationActivator.bundleContext;
            ServiceReference configReference =
                bundleContext.getServiceReference(ConfigurationService.class
                    .getName());

            configurationService =
                (ConfigurationService) bundleContext
                    .getService(configReference);
        }
        return configurationService;
    }

    /**
     * Creates the title component.
     * @return the newly created title component
     */
    private JComponent createTitleComponent()
    {
        JTextPane titlePane = new JTextPane();
        SimpleAttributeSet aSet = new SimpleAttributeSet();
        StyleConstants.setAlignment(aSet, StyleConstants.ALIGN_CENTER);
        StyleConstants.setBold(aSet, true);
        StyleConstants.setFontFamily(aSet, titlePane.getFont().getFamily());

        titlePane.setParagraphAttributes(aSet, true);
        titlePane.setEditable(false);
        titlePane.setText(Resources.getString(
            "plugin.simpleaccregwizz.INITIAL_ACCOUNT_REGISTRATION"));
        titlePane.setOpaque(false);

        return titlePane;
    }

    /**
     * Show the create account window for the given wizard.
     *
     * @param wizard the <tt>AccountRegistrationWizard</tt>, for which we're
     * opening the window
     */
    private void showCreateAccountWindow(AccountRegistrationWizard wizard)
    {
        CreateAccountWindow createAccountWindow
            = SimpleAccountRegistrationActivator
                .getUIService().getCreateAccountWindow();

        createAccountWindow.setSelectedWizard(wizard, true);

        if (wizard instanceof ExtendedAccountRegistrationWizard)
        {
            ((ExtendedAccountRegistrationWizard) wizard).setCreateAccountView();
        }

        createAccountWindow.setVisible(true);
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }
}
