/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.jivesoftware.smack.*;

/**
 * Dialog for adding a new Jabber account.
 * 
 * @author Nicolas Grandclaude
 */
public class JabberNewAccountDialog
    extends SIPCommDialog
    implements DocumentListener
{
    private static final Logger logger = Logger
        .getLogger(JabberNewAccountDialog.class);

    private JabberServerChooserDialog jabberServerChooserDialog;

    // Panels
    private JPanel userIDPassPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel serverPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel buttonPanel = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private Box buttonBox = new Box(BoxLayout.X_AXIS);

    private JPanel mainPanel = new TransparentPanel(new BorderLayout());

    private JPanel westPanel = new TransparentPanel(new BorderLayout(10, 10));

    // Labels
    private JLabel serverLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.CSERVER"));

    private JLabel userIDLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.USERNAME"));

    private JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private JLabel pass2Label
        = new JLabel(Resources.getString(
            "plugin.jabberaccregwizz.PASSWORD_CONFIRM"));

    private JLabel portLabel
        = new JLabel(Resources.getString("service.gui.PORT"));

    private JLabel westIconLabel = new JLabel();

    // Textfield
    private JTextField serverField = new JTextField();

    private JTextField userIDField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JPasswordField pass2Field = new JPasswordField();

    private JTextField portField = new JTextField("5222");

    // Button
    private JButton chooseButton = new JButton();

    private JButton okButton = new JButton();

    private JButton cancelButton = new JButton();

    // Smack objects
    private XMPPConnection xmppConnection = null;

    private AccountManager accountManager = null;

    // Variables for FirstWizardPage
    public boolean isOK = false;

    public String userID = null;

    public String password = null;

    public String server = null;

    public String port = null;

    private LoadingAccountGlassPane loadingAccountGlassPane
        = new LoadingAccountGlassPane();
    
    /**
     * Creates an instance of <tt>JabberNewAccountDialog</tt>.
     */
    public JabberNewAccountDialog()
    {
        this.setSize(new Dimension(450, 250));
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle(
            Resources.getString("plugin.jabberaccregwizz.NEW_ACCOUNT_TITLE"));
        this.setModal(true);
        this.setGlassPane(loadingAccountGlassPane);

        // Place the window in the screen center
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(screenSize.width / 2 - this.getWidth() / 2,
            screenSize.height / 2 - this.getHeight() / 2);

        this.init();
    }

    /**
     * Connect to the choose server and create a new account with Smack
     * 
     * @param server the server domain
     * @param port TCP port to connect
     * @param username username
     * @param password password
     */
    private boolean addNewAccount(String server, int port, String username,
        String password)
    {
        try
        {
            ConnectionConfiguration config = new ConnectionConfiguration(
                server, port);

            xmppConnection = new XMPPConnection(config);
            xmppConnection.connect();

            accountManager = new AccountManager(xmppConnection);
            accountManager.createAccount(username, password);
            return true;
        }
        catch (XMPPException exc)
        {
            if (exc.getXMPPError().getCode() == 409)
            {
                JOptionPane.showMessageDialog(
                    null,
                    Resources.getString(
                        "plugin.jabberaccregwizz.USER_EXISTS_ERROR"),
                    Resources.getString(
                        "plugin.jabberaccregwizz.XMPP_ERROR"),
                    JOptionPane.ERROR_MESSAGE);

                logger.error(
                    "Error when created a new Jabber account :" +
                    " user already exist");
            }
            else
            {
                JOptionPane.showMessageDialog(
                    null,
                    Resources.getString(
                        "plugin.jabberaccregwizz.UNKNOWN_XMPP_ERROR"),
                    Resources.getString(
                        "plugin.jabberaccregwizz.XMPP_ERROR"),
                    JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {

        labelsPanel.add(serverLabel);
        labelsPanel.add(userIDLabel);
        labelsPanel.add(passLabel);
        labelsPanel.add(pass2Label);
        labelsPanel.add(portLabel);

        userIDField.setColumns(30);

        serverPanel.add(serverField, BorderLayout.CENTER);
        serverPanel.add(chooseButton, BorderLayout.EAST);
        valuesPanel.add(serverPanel);
        valuesPanel.add(userIDField);
        valuesPanel.add(passField);
        valuesPanel.add(pass2Field);
        valuesPanel.add(portField);

        serverField.getDocument().addDocumentListener(this);
        userIDField.getDocument().addDocumentListener(this);
        passField.getDocument().addDocumentListener(this);
        pass2Field.getDocument().addDocumentListener(this);
        portField.getDocument().addDocumentListener(this);

        userIDPassPanel.add(labelsPanel, BorderLayout.WEST);
        userIDPassPanel.add(valuesPanel, BorderLayout.CENTER);

        chooseButton.setText(
            Resources.getString("plugin.jabberaccregwizz.CHOOSE"));
        chooseButton.setMnemonic(
            Resources.getMnemonic("plugin.jabberaccregwizz.CHOOSE"));

        westIconLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20), BorderFactory
                .createTitledBorder("")));
        try
        {
            westIconLabel.setIcon(new ImageIcon(ImageIO
                .read(new ByteArrayInputStream(Resources
                    .getImage(Resources.PAGE_IMAGE)))));
        }
        catch (IOException e)
        {
            logger.error("Could not read image.", e);
        }

        westPanel.add(westIconLabel, BorderLayout.NORTH);
        this.mainPanel.add(westPanel, BorderLayout.WEST);

        // Choose button open the JabberServerChooserDialog
        chooseButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                jabberServerChooserDialog = new JabberServerChooserDialog();
                if (jabberServerChooserDialog.isOK) // OK pressed in
                                                    // JabberServerChooserDialog
                {
                    serverField
                        .setText(jabberServerChooserDialog.serverSelected);
                }
            }
        });

        // Ok button
        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                if (new String(passField.getPassword()).equals(new String(
                    pass2Field.getPassword())))                    
                { // the two password fields are the same
                    new Thread()
                    {
                        public void run()
                        {
                            boolean result = addNewAccount(serverField.getText(),
                               Integer.parseInt(portField.getText()),
                               userIDField.getText(),
                               new String(passField.getPassword()));
                            
                            if (result == true)
                            {
                                // Update FirstWizardDialog field
                                isOK = true;
                                userID = new String(userIDField.getText());
                                password = new String(passField.getPassword());
                                server = new String(serverField.getText());
                                port = new String(portField.getText());
                                dispose();
                            }
                        }
                    }.start();
                    
                    loadingAccountGlassPane.setVisible(true);
                }
                else
                {
                    JOptionPane.showMessageDialog(
                        null,
                        Resources.getString(
                            "plugin.jabberaccregwizz.PROTOCOL_DESCRIPTION"),
                        Resources.getString(
                            "plugin.jabberaccregwizz.XMPP_ERROR"),
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Cancel button
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                dispose();
            }
        });

        okButton.setText(Resources.getString("service.gui.OK"));
        okButton.setMnemonic(Resources.getMnemonic("service.gui.OK"));
        okButton.setEnabled(false);

        cancelButton.setText(Resources.getString("service.gui.CANCEL"));
        cancelButton.setMnemonic(Resources.getMnemonic("service.gui.CANCEL"));

        buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        buttonBox.add(okButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(cancelButton);

        buttonPanel.add(buttonBox);

        this.mainPanel.add(userIDPassPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.getContentPane().add(mainPanel, BorderLayout.NORTH);

        this.setVisible(true);
    }

    /**
     * Sets the "Ok" button enabled if all fields are filled.
     */ 
    private void enableOKButton()
    {
        okButton.setEnabled(false);
        try
        {
            Integer.parseInt(portField.getText());
        }
        catch (NumberFormatException ex)
        {
            okButton.setEnabled(false);
            return;
        }

        if (serverField.getText().equals("")
            || userIDField.getText().equals("")
            || (new String(passField.getPassword())).equals("")
            || (new String(pass2Field.getPassword())).equals(""))
        {
            okButton.setEnabled(false);
        }
        else
        {
            okButton.setEnabled(true);
        }
    }

    public void insertUpdate(DocumentEvent evt)
    {
        this.enableOKButton();
    }

    public void removeUpdate(DocumentEvent evt)
    {
        this.enableOKButton();
    }

    public void changedUpdate(DocumentEvent evt)
    {
    }

    public void pageHiding()
    {
    }

    public void pageShown()
    {
    }

    public void pageBack()
    {
    }

    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }

    /**
     * A GlassPane that would change the cursor to a waiting cursor until the
     * new account is registered.
     */
    private static class LoadingAccountGlassPane extends JComponent
    {
        public LoadingAccountGlassPane()
        {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }
}
