/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.dictaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the host,
 * port and the strategy of the account.
 * 
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class FirstWizardPage
    extends JPanel
    implements WizardPage, DocumentListener, ActionListener
{
    private static Logger logger = Logger.getLogger(FirstWizardPage.class);

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel hostPortPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new JPanel();

    private JPanel valuesPanel = new JPanel();

    private JLabel hostLabel = new JLabel("Host");

    private JPanel emptyPanel = new JPanel();

    private JLabel hostExampleLabel = new JLabel("Ex: dict.org");

    private JLabel portLabel = new JLabel("Port");

    private JLabel existingAccountLabel =
        new JLabel(Resources.getString("existingAccount"));

    private JTextField hostField = new JTextField();

    private JTextField portField = new JTextField("2628");
    
    
    private JPanel strategyPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel strategyTitleBloc = new JPanel(new BorderLayout());
    
    private JLabel strategyTitle = new JLabel(Resources.getString("strategyList"));
    
    private JButton strategyLoader = new JButton(Resources.getString("strategyActu"));
    
    private Vector<String> strategyList;
    private JScrollPane jScrollPane;
    private JList strategyBox;
    private JTextArea strategyDescription = new JTextArea(Resources.getString("strategyDesc"));
    private JLabel strategyMessage;
    private boolean strategyMessInstall = false;

    private JPanel mainPanel = new JPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private DictAccountRegistrationWizard wizard;
    
    private String initstrategy = "";
    
    private ArrayList<String> strategiesAssoc = new ArrayList<String>();
    
    private StrategyThread populateThread = null;
    
    private boolean firstAccount = false;
    
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
        
        this.populateThread = new StrategyThread(this);

        this.firstAccount = !this.hasAccount();
        
        if (this.firstAccount) 
        {
            this.initFirstAccount();
        }
        else
        {
            this.init();
        }
        
        this.populateThread = new StrategyThread(this);
        this.populateThread.start();

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
        
        // Server informations
        this.existingAccountLabel.setForeground(Color.RED);

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

        hostPortPanel.setBorder(BorderFactory.createTitledBorder("Server informations"));
        
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
        
        // Strategies
        this.strategyList = new Vector<String>();
        this.strategyBox = new JList(this.strategyList);
        
        for (int i=0; i<20; i++)
        {
            this.strategyList.add("Elem "+i);
        }
        
        this.strategyBox.setVisibleRowCount(6);
        
        this.strategyBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        this.jScrollPane = new JScrollPane();
        this.jScrollPane.getViewport().add(this.strategyBox);
        this.strategyPanel.add(this.jScrollPane);
        
        // Strategy title + button
        this.strategyTitleBloc.add(this.strategyTitle, BorderLayout.WEST);
        this.strategyTitleBloc.add(this.strategyLoader, BorderLayout.EAST);
        
        // Button action listener
        this.strategyLoader.setActionCommand("populateList");
        this.strategyLoader.addActionListener(this);
        
        // South Panel
        JPanel sSouthPanel = new JPanel(new BorderLayout());
        
        // Description
        this.strategyDescription.setLineWrap(true);
        this.strategyDescription.setLineWrap(true);
        this.strategyDescription.setRows(4);
        this.strategyDescription.setWrapStyleWord(true);
        this.strategyDescription.setAutoscrolls(false);
        sSouthPanel.add(this.strategyDescription);
        
        // Message
        this.strategyMessage = new JLabel(" ");        
        sSouthPanel.add(this.strategyMessage, BorderLayout.SOUTH);
        
        this.strategyPanel.add(sSouthPanel, BorderLayout.SOUTH);
        
        this.strategyPanel.add(this.strategyTitleBloc, BorderLayout.NORTH);
        this.strategyPanel.setBorder(BorderFactory.createTitledBorder("Strategy selection"));
        mainPanel.add(this.strategyPanel);

        this.add(mainPanel, BorderLayout.NORTH);
    }
    
    private void initFirstAccount()
    {
        // Data init
        this.hostField = new JTextField("dict.org");
        this.portField = new JTextField("2628");
        
        // Init strategy box
        this.strategyList = new Vector<String>();
        this.strategyBox = new JList(this.strategyList);
        this.strategyMessage = new JLabel(" ");
        
        
        JPanel infoTitlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JTextArea firstDescription = new JTextArea(Resources.getString("firstAccount"));
        JLabel title = new JLabel(Resources.getString("dictAccountInfoTitle"));
        
        // Title
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14.0f));
        infoTitlePanel.add(title);
        this.add(infoTitlePanel, BorderLayout.NORTH);
        this.add(this.strategyMessage, BorderLayout.SOUTH);
        
        // Description
        firstDescription.setLineWrap(true);
        firstDescription.setLineWrap(true);
        firstDescription.setRows(6);
        firstDescription.setWrapStyleWord(true);
        firstDescription.setAutoscrolls(false);
        this.add(firstDescription);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     *
     * @return Returns the identifier of the current (the first) page of the wizard.
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
     * the next back identifier - the default page.
     *
     * @return Returns the identifier of the previous page of the wizard.
     */
    public Object getBackPageIdentifier()
    {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
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
    public void pageNext()
    {
        //*
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        int stPos;
        boolean isModified = false;
        
        if (this.initAccountID instanceof AccountID)
        {   // We check if there is modifications to the server
            String accHost = (String) this.initAccountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_ADDRESS);
            int accPort = Integer.parseInt((String) this.initAccountID.getAccountProperties()
                .get(ProtocolProviderFactory.SERVER_PORT));
            
            if (accHost != host || accPort != port)
            {
                isModified = true;
            }
        }
        
        // We check if a strategy has been selected
        if (this.strategyList.size() == 0)
        {   // No Strategy, we get them
            this.populateStrategies();
            
            while (this.populateThread.isRunning()) {;}
        }

        if (this.strategyList.size() == 0)
        {
            // No strategy, maybe not connected
            // Information message is already on the wizard
            nextPageIdentifier = FIRST_PAGE_IDENTIFIER;
            this.revalidate();
        }
        else if ((!wizard.isModification() && isExistingAccount(host, port)) 
               || (isModified && isExistingAccount(host, port)))
        {
            nextPageIdentifier = FIRST_PAGE_IDENTIFIER;
            hostPortPanel.add(existingAccountLabel, BorderLayout.NORTH);
            this.revalidate();
        }
        else
        {
            nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
            hostPortPanel.remove(existingAccountLabel);

            DictAccountRegistration registration = wizard.getRegistration();

            registration.setHost(host);
            registration.setPort(port);
            
            stPos = this.strategyBox.getSelectedIndex();
            registration.setStrategyCode(this.strategiesAssoc.get(stPos));
            registration.setStrategy(this.strategyBox.getSelectedValue().toString());
        }
        //*/
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the UIN
     * field is empty.
     */
    private void setNextButtonEnabled()
    {
        boolean hostOK = DictAdapter.isUrl(hostField.getText());
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
            this.strategyList.clear();
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
        String host = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_ADDRESS);
        String port = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_PORT);
        String strategy = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactory.STRATEGY);
        
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

        ArrayList registeredAccounts = factory.getRegisteredAccounts();
        return !registeredAccounts.isEmpty();
    }
    
    /**
     * Checks if an acount with the given account already exists.
     * 
     * @param host the host of the account to check
     * @param port the port of the account to check
     * @return TRUE, if an account with the given name already exists, FALSE -
     *         otherwise
     */
    private boolean isExistingAccount(String host, int port)
    {
        //*
        ProtocolProviderFactory factory =
            DictAccRegWizzActivator.getDictProtocolProviderFactory();

        ArrayList registeredAccounts = factory.getRegisteredAccounts();
        
        String accHost;
        int accPort;

        for (int i = 0; i < registeredAccounts.size(); i++)
        {
            AccountID accountID = (AccountID) registeredAccounts.get(i);
            accHost = (String) accountID.getAccountProperties()
                .get(ProtocolProviderFactory.SERVER_ADDRESS);
            
            if (host.equalsIgnoreCase(accHost))
            {
                // We check the port, only if there is an account with the same host
                accPort = Integer.parseInt((String) accountID.getAccountProperties()
                    .get(ProtocolProviderFactory.SERVER_PORT));
                
                if (port == accPort)
                {
                    return true;
                }
            }
        }
        //*/
        return false;
    }
       
    /**
     * Start the thread which will populate the Strategy List
     */
    public void populateStrategies()
    {
        // Clear ArrayLists
        this.strategiesAssoc.clear();
        this.strategyList.clear();
        
        
        //this.populateThread = new StrategyThread(this);
        this.populateThread.setHost(this.hostField.getText())
                           .setPort(Integer.parseInt(this.portField.getText()))
                           .sendProcessRequest();
    }
    
    /**
     * Called by the thread, display a message
     * @param message a message
     */
    public void threadMessage(String message)
    {
        this.strategyMessage.setText(message);
    }
    
    /**
     * Called by the thread, remove the special message section
     */
    public void threadRemoveMessage()
    {
        this.strategyMessage.setText(" ");
    }
    
    /**
     * Called by the thread, add a strategy in the list
     * @param code The strategy code
     * @param description The strategy description
     */
    public void threadAddStrategy(String code, String description)
    {
        this.strategiesAssoc.add(code);
        this.strategyList.add(description);
        this.strategyBox.setListData(this.strategyList);
    }
    
    /**
     * Automatic selection of a strategy
     */
    public void autoSelectStrategy()
    {
        int index = -1;
        
        if (this.initstrategy.length() > 0)
        {   // saved strategy
            index = this.strategiesAssoc.indexOf(this.initstrategy);
            this.initstrategy = "";
        }
        if (index < 0)
        {
            // First case : levenstein distance
            index = this.strategiesAssoc.indexOf("lev");
        }
        if (index < 0)
        {
            // Second case : soundex
            index = this.strategiesAssoc.indexOf("soundex");
        }
        if (index < 0)
        {
            // Last case : prefix
            index = this.strategiesAssoc.indexOf("prefix");
        }
        
        // If the index is still < 0, we select the first index
        if (index < 0)
        {
            index = 0;
        }
        if (index < this.strategyBox.getVisibleRowCount())
        {
            // If the index is visible row, we don't need to scroll
            this.strategyBox.setSelectedIndex(index);
        }
        else
        {
            // Otherwise, we scroll to the selected value 
            this.strategyBox.setSelectedValue(this.strategyList.get(index), true);
        }
        
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
}
