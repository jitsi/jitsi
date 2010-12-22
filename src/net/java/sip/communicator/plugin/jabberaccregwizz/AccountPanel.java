/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 *
 * @author Yana Stamcheva
 */
public class AccountPanel
    extends TransparentPanel
    implements DocumentListener
{
    private static final Logger logger = Logger.getLogger(AccountPanel.class);

    public static final String USER_NAME_EXAMPLE = "Ex: johnsmith@jabber.org";

    private final JPanel userIDPassPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private final JPanel labelsPanel = new TransparentPanel();

    private final JPanel valuesPanel = new TransparentPanel();

    private final JLabel userIDLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.USERNAME"));

    private final JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private final JPanel emptyPanel = new TransparentPanel();

    private final JLabel userIDExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private final JTextField userIDField = new TrimTextField();

    private final JPasswordField passField = new JPasswordField();

    private final JCheckBox rememberPassBox = new SIPCommCheckBox(Resources
        .getString("service.gui.REMEMBER_PASSWORD"));

    private final JPanel registerPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private final JPanel buttonPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private final JTextArea registerArea = new JTextArea(Resources
        .getString("plugin.jabberaccregwizz.REGISTER_NEW_ACCOUNT_TEXT"));

    private final JButton registerButton = new JButton(Resources
        .getString("plugin.jabberaccregwizz.NEW_ACCOUNT_TITLE"));

    private JabberNewAccountDialog jabberNewAccountDialog;

    private final FirstWizardPage parentPage;

    /**
     * Creates an instance of <tt>AccountPanel</tt> by specifying the parent
     * wizard page, where it's contained.
     * @param parentPage the parent page where this panel is contained
     */
    public AccountPanel(final FirstWizardPage parentPage)
    {
        super(new BorderLayout());

        this.parentPage = parentPage;

        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));

        userIDField.getDocument().addDocumentListener(this);
        rememberPassBox.setSelected(true);

        userIDExampleLabel.setForeground(Color.GRAY);
        userIDExampleLabel.setFont(userIDExampleLabel.getFont().deriveFont(8));
        emptyPanel.setMaximumSize(new Dimension(40, 35));
        userIDExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        labelsPanel.add(userIDLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        valuesPanel.add(userIDField);
        valuesPanel.add(userIDExampleLabel);
        valuesPanel.add(passField);

        userIDPassPanel.add(labelsPanel, BorderLayout.WEST);
        userIDPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userIDPassPanel.add(rememberPassBox, BorderLayout.SOUTH);
        userIDPassPanel.setBorder(
                BorderFactory.createTitledBorder(
                        Resources.getString(
                                "plugin.sipaccregwizz.USERNAME_AND_PASSWORD")));

        registerButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Reg OK");

                // Open the new account dialog.
                jabberNewAccountDialog = new JabberNewAccountDialog();

                if (jabberNewAccountDialog.isOK == true)
                {
                    ConnectionPanel connectionPanel
                        = parentPage.getConnectionPanel();

                    if (connectionPanel != null)
                    {
                        connectionPanel
                            .setServerAddress(jabberNewAccountDialog.server);
                        connectionPanel
                            .setServerPort(jabberNewAccountDialog.port);
                    }

                    // This userIDField contains the username "@" the server.
                    userIDField.setText(jabberNewAccountDialog.userID + "@"
                        + jabberNewAccountDialog.server);

                    passField.setText(jabberNewAccountDialog.password);
                }
                if (logger.isDebugEnabled())
                    logger.debug("Reg End");
            }
        });

        buttonPanel.add(registerButton);

        registerArea.setEditable(false);
        registerArea.setOpaque(false);
        registerArea.setLineWrap(true);
        registerArea.setWrapStyleWord(true);

        registerPanel.add(registerArea);
        registerPanel.add(buttonPanel);

        registerPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.jabberaccregwizz.NEW_ACCOUNT_TITLE")));

        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(userIDPassPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(registerPanel);

        add(mainPanel, BorderLayout.NORTH);
    }


    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the
     * UserID field. Enables or disables the "Next" wizard button according to
     * whether the UserID field is empty.
     *
     * @param evt the document event that has triggered this method call.
     */
    public void insertUpdate(DocumentEvent evt)
    {
        parentPage.setNextButtonAccordingToUserIDAndResource();

        parentPage.setServerFieldAccordingToUsername(userIDField.getText());
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the User ID field. Enables or disables the "Next" wizard button
     * according to whether the User ID field is empty.
     *
     * @param evt the document event that has triggered this method call.
     */
    public void removeUpdate(DocumentEvent evt)
    {
        parentPage.setNextButtonAccordingToUserIDAndResource();

        parentPage.setServerFieldAccordingToUsername(userIDField.getText());
    }

    public void changedUpdate(DocumentEvent evt) {}

    /**
     * Returns the username entered in this panel.
     * @return the username entered in this panel
     */
    String getUsername()
    {
        return userIDField.getText();
    }

    /**
     * Sets the username to display in the username field.
     * @param username the username to set
     */
    void setUsername(String username)
    {
        userIDField.setText(username);
        userIDField.setEnabled(false);
    }

    /**
     * Returns the password entered in this panel.
     * @return the password entered in this panel
     */
    char[] getPassword()
    {
        return passField.getPassword();
    }

    /**
     * Sets the password to display in the password field of this panel.
     * @param password the password to set
     */
    void setPassword(String password)
    {
        passField.setText(password);
    }

    /**
     * Indicates if the remember password box is selected.
     * @return <tt>true</tt> if the remember password check box is selected,
     * otherwise returns <tt>false</tt>
     */
    boolean isRememberPassword()
    {
        return rememberPassBox.isSelected();
    }

    /**
     * Selects/deselects the remember password check box depending on the given
     * <tt>isRemember</tt> parameter.
     * @param isRemember indicates if the remember password checkbox should be
     * selected or not
     */
    void setRememberPassword(boolean isRemember)
    {
        rememberPassBox.setSelected(isRemember);
    }
}
