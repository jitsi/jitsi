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
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.dict4j.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the host,
 * port and the strategy of the account.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class FirstWizardPage
    extends TransparentPanel
    implements WizardPage, DocumentListener, ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel hostPortPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JLabel hostLabel
        = new JLabel(Resources.getString("plugin.dictaccregwizz.HOST"));

    private JPanel emptyPanel = new TransparentPanel();

    private JLabel hostExampleLabel = new JLabel("Ex: dict.org");

    private JLabel portLabel
        = new JLabel(Resources.getString("service.gui.PORT"));

    private JTextField hostField = new JTextField();

    private JTextField portField = new JTextField("2628");

    private JPanel strategyPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel strategyTitleBloc = new TransparentPanel(new BorderLayout());

    private JLabel strategyTitle = new JLabel(Resources.getString(
            "plugin.dictaccregwizz.STRATEGY_LIST"));

    private JButton strategyLoader
        = new JButton(Resources.getString(
            "plugin.dictaccregwizz.SEARCH_STRATEGIES"));

    private StrategiesList strategiesList;

    private JTextArea strategyDescription
        = new JTextArea(Resources.getString(
            "plugin.dictaccregwizz.STRATEGY_DESCRIPTION"));

    private ProgressPanel searchProgressPanel;

    private JPanel mainPanel = new TransparentPanel(new BorderLayout());

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private DictAccountRegistrationWizard wizard;

    private String initstrategy = "";

    private ThreadManager searchThread = null;

    private boolean firstAccount = false;

    private boolean isPageCommitted = false;

    /**
     * Initial AccountID (null if new account)
     * Used to check if there are modifications to the account
     */
    private AccountID initAccountID = null;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(DictAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        this.setPreferredSize(new Dimension(300, 150));

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.searchThread = new ThreadManager(this);
        this.searchProgressPanel = new ProgressPanel(this.searchThread);

        this.firstAccount = !this.hasAccount();

        if (this.firstAccount)
        {
            this.initFirstAccount();
        }
        else
        {
            this.init();
        }

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        // Host and port Field
        this.hostField = new JTextField();
        this.portField = new JTextField("2628");

        this.hostField.getDocument().addDocumentListener(this);
        this.portField.getDocument().addDocumentListener(this);

        this.hostExampleLabel.setForeground(Color.GRAY);
        this.hostExampleLabel.setFont(hostExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.hostExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8,
            0));

        labelsPanel.add(hostLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(portLabel);

        valuesPanel.add(hostField);
        valuesPanel.add(hostExampleLabel);
        valuesPanel.add(portField);

        hostPortPanel.add(labelsPanel, BorderLayout.WEST);
        hostPortPanel.add(valuesPanel, BorderLayout.CENTER);

        hostPortPanel.setBorder(BorderFactory.createTitledBorder(
                Resources.getString("plugin.dictaccregwizz.SERVER_INFO")));

        this.labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
        this.valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));

        mainPanel.add(hostPortPanel);

        this.portField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent evt)
            {
                // If evt isn't a digit, we don't add it
                if (!Character.isDigit(evt.getKeyChar()))
                {
                    evt.consume();
                }
            }

            // Not used
            public void keyPressed(KeyEvent evt) {;}
            public void keyReleased(KeyEvent evt) {;}
        });

        // Strategies list
        this.strategiesList = new StrategiesList();

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(this.strategiesList);
        this.strategyPanel.add(scrollPane);

        // Strategy title + button
        this.strategyTitleBloc.add(this.strategyTitle, BorderLayout.WEST);
        this.strategyTitleBloc.add(this.strategyLoader, BorderLayout.EAST);

        // Button action listener
        this.strategyLoader.setActionCommand("populateList");
        this.strategyLoader.addActionListener(this);

        // South Panel
        JPanel sSouthPanel = new TransparentPanel(new BorderLayout());

        // Description
        this.strategyDescription.setLineWrap(true);
        this.strategyDescription.setLineWrap(true);
        this.strategyDescription.setRows(4);
        this.strategyDescription.setWrapStyleWord(true);
        this.strategyDescription.setAutoscrolls(false);
        sSouthPanel.add(this.strategyDescription);

        // Message
        sSouthPanel.add(this.searchProgressPanel, BorderLayout.SOUTH);

        this.strategyPanel.add(sSouthPanel, BorderLayout.SOUTH);

        this.strategyPanel.add(this.strategyTitleBloc, BorderLayout.NORTH);
        this.strategyPanel.setBorder(BorderFactory.createTitledBorder(
                Resources.getString("plugin.dictaccregwizz.STRATEGY_SELECTION")));
        mainPanel.add(this.strategyPanel);

        this.add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Initialize the UI for the first account
     */
    private void initFirstAccount()
    {
        // Data init
        this.hostField = new JTextField("dict.org");
        this.portField = new JTextField("2628");

        // Init strategies list
        this.strategiesList = new StrategiesList();

        this.mainPanel = new TransparentPanel(new BorderLayout());

        JPanel infoTitlePanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        JTextArea firstDescription
            = new JTextArea(Resources.getString(
                "plugin.dictaccregwizz.FIRST_ACCOUNT"));
        JLabel title
            = new JLabel(Resources.getString(
                "plugin.dictaccregwizz.ACCOUNT_INFO_TITLE"));

        // Title
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14.0f));
        infoTitlePanel.add(title);
        this.mainPanel.add(infoTitlePanel, BorderLayout.NORTH);
        this.mainPanel.add(this.searchProgressPanel, BorderLayout.SOUTH);

        // Description
        firstDescription.setLineWrap(true);
        firstDescription.setEditable(false);
        firstDescription.setOpaque(false);
        firstDescription.setRows(6);
        firstDescription.setWrapStyleWord(true);
        firstDescription.setAutoscrolls(false);
        this.mainPanel.add(firstDescription);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     *
     * @return Returns the identifier of the current (the first) page of the
     * wizard.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     *
     * @return Returns the identifier of the next page of the wizard.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier, which is null as this is the first wizard page.
     *
     * @return the identifier of the previous page of the wizard.
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     * @return Returns this form of the wizard.
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the UIN field is empty.
     */
    public void pageShowing()
    {
        this.setNextButtonEnabled();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        boolean isModified = false;

        if (this.initAccountID != null)
        { // We check if there are modifications to the server
            String accHost =
                this.initAccountID.getAccountPropertyString(
                    ProtocolProviderFactory.SERVER_ADDRESS);
            int accPort =
                Integer.parseInt(this.initAccountID
                    .getAccountPropertyString(ProtocolProviderFactory.SERVER_PORT));

            if (((accHost == null) ? (host != null) : !accHost.equals(host))
                    || (accPort != port))
            {
                isModified = true;
            }
        }

        // We check if a strategy has been selected
        if (this.strategiesList.getModel().getSize() == 0)
        {   // No Strategy, we get them
            this.populateStrategies();

            if (!this.searchThread.waitThread())
            {
                // TODO error dialog : thread interrupted ? no thread ?
                this.strategiesList.clear();
            }
        }

        if (this.strategiesList.getModel().getSize() == 0)
        {
            // No strategy, maybe not connected
            // Information message is already on the wizard
            nextPageIdentifier = FIRST_PAGE_IDENTIFIER;
            this.revalidate();
        }
        else
        {
            nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;

            DictAccountRegistration registration = wizard.getRegistration();

            registration.setHost(host);
            registration.setPort(port);
            registration.setStrategy(
                (Strategy) this.strategiesList.getSelectedValue());
        }

        isPageCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the UIN
     * field is empty.
     */
    private void setNextButtonEnabled()
    {
        boolean hostOK = DictConnection.isUrl(hostField.getText());
        boolean portOK = (this.portField.getText().length() != 0)
            && Integer.parseInt(this.portField.getText()) > 10;

        if (this.firstAccount)
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(true);
        }
        else if (hostOK && portOK)
        {
            this.strategyLoader.setEnabled(true);
            wizard.getWizardContainer().setNextFinishButtonEnabled(true);
        }
        else
        {
            // Disable the finish button
            wizard.getWizardContainer().setNextFinishButtonEnabled(false);

            // Clear the list and disable the button
            this.strategiesList.clear();
            this.strategyLoader.setEnabled(false);
        }
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the UIN
     * field. Enables or disables the "Next" wizard button according to whether
     * the UIN field is empty.
     *
     * @param e the <tt>DocumentEvent</tt> triggered when user types in the UIN
     * field.
     */
    public void insertUpdate(DocumentEvent e)
    {
        this.setNextButtonEnabled();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UIN field. Enables or disables the "Next" wizard button
     * according to whether the UIN field is empty.
     *
     * @param e The <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UIN field.
     */
    public void removeUpdate(DocumentEvent e)
    {
        this.setNextButtonEnabled();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user changes an
     * attribute or set of attributes from the UIN field.
     * Currently this notification has no effect and is just here to implement
     * the DocumentListener interface.
     *
     * @param e The <tt>DocumentEvent</tt> triggered when an attribute or set of
     * attributes changed from the UIN field.
     */
    public void changedUpdate(DocumentEvent e)
    {
    }

    /**
     * Invoked when this WizardPage will be hidden eighter because the user has
     * clicked "Back" or "Next".
     * This function has no effect.
     */
    public void pageHiding()
    {
    }

    /**
     * Invoked when this WizardPage will be shown eighter because the user has
     * clicked "Back" on the next wizard page or "Next" on the previous one.
     * This function has no effect.
     */
    public void pageShown()
    {
    }

    /**
     * Invoked when user clicks on the "Back" wizard button.
     * This function has no effect.
     */
    public void pageBack()
    {
    }

    /**
     * Fills the Host, Port and Strategy fields in this panel with the data comming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();
        String host =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.SERVER_ADDRESS);
        String port =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.SERVER_PORT);
        String strategy =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.STRATEGY);

        this.initAccountID = accountID;

        // Host field
        this.hostField.setText(host);

        // Port Field
        this.portField.setText(port);

        // Load strategies
        this.initstrategy = strategy;
        this.populateStrategies();
    }

    /**
     * Handles the action of the button.
     *
     * @param e The event generated when the button is pressed.
     */
    public void actionPerformed(ActionEvent e)
    {
        // Button action -> populate the list
        if (e.getActionCommand().equals("populateList"))
        {
            this.populateStrategies();
        }
    }

    /**
     * Checks if an account is stored for this protocol
     * @return TRUE, if an account is stored - FALSE otherwise
     */
    private boolean hasAccount()
    {
        ProtocolProviderFactory factory =
            DictAccRegWizzActivator.getDictProtocolProviderFactory();

        return !factory.getRegisteredAccounts().isEmpty();
    }

    /**
     * Start the thread which will populate the Strategies List
     */
    public void populateStrategies()
    {
        // Clear ArrayList
        this.strategiesList.clear();

        boolean ok = this.searchThread.submitRequest(this.hostField.getText(),
                                    Integer.parseInt(this.portField.getText()));

        if (!ok)
        {
            // TODO Display error
        }
    }

    /**
     * Automatic selection of a strategy
     */
    public void autoSelectStrategy()
    {
        this.strategiesList.autoSelectStrategy(this.initstrategy);
    }

    /**
     *
     * @param strategies
     */
    public void setStrategies(List<Strategy> strategies)
    {
        this.strategiesList.setStrategies(strategies);
    }

    /**
     * Informs the user of the current status of the search
     * Should only be called by the thread
     * @param message Search status
     */
    public void progressMessage(String message)
    {
        this.searchProgressPanel.nextStep(message);
    }

    /**
     * Informs the wizard that the search of the strategies is complete.
     * Should only be called by the thread
     */
    public void strategiesSearchComplete()
    {
        setStrategyButtonEnable(true);
        this.searchProgressPanel.finish();
    }

    /**
     * Informs the wizard that the search of the strategies is a failure
     * Should only be called by the thread
     * @param reason Reason message
     * @param de Exception thrown
     */
    public void strategiesSearchFailure(String reason, DictException de)
    {
        strategiesSearchComplete();
        // TODO SHOW ERROR MESSAGE
    }

    /**
     * Enables or disable the Next Button and the Strategy Button
     * @param e TRUE enables - FALSE disables
     */
    public void setStrategyButtonEnable(boolean e)
    {
        // During all the process the buttons and the fieldsset are in the same state

        this.hostField.setEnabled(e);
        this.portField.setEnabled(e);

        this.strategyLoader.setEnabled(e);
        wizard.getWizardContainer().setNextFinishButtonEnabled(e);
    }

    public Object getSimpleForm()
    {
        return mainPanel;
    }

    /**
     * Indicates if this is the first dict account
     *
     * @return TRUE if this is the first dict account - FALSE otherwise
     */
    public boolean isFirstAccount()
    {
        return this.firstAccount;
    }

    public boolean isCommitted()
    {
        return isPageCommitted;
    }
}
