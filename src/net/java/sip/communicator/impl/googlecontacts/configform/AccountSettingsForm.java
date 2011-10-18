/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.googlecontacts.configform;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.impl.googlecontacts.*;
import net.java.sip.communicator.service.googlecontacts.*;

/**
 * The page with hostname/port/encryption fields
 *
 * @author Sebastien Mazy
 * @author Sebastien Vincent
 */
public class AccountSettingsForm
    extends SIPCommDialog
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * component holding the name
     */
    private JTextField nameField;

    /**
     * the component holding the password
     */
    private JPasswordField passwordField;

    /**
     * The prefix field.
     */
    private JTextField prefixField;

    /**
     * Save button.
     */
    private JButton saveBtn = new JButton(
            Resources.getString("impl.googlecontacts.SAVE"));

    /**
     * Cancel button.
     */
    private JButton cancelBtn = new JButton(
            Resources.getString("impl.googlecontacts.CANCEL"));

    /**
     * Return code.
     */
    private int retCode = 0;

    /**
     * The Google Contacts connection.
     */
    private GoogleContactsConnectionImpl cnx = null;

    /**
     * Constructor.
     */
    public AccountSettingsForm()
    {
        this.setTitle(Resources.getString(
                "impl.googlecontacts.CONFIG_FORM_TITLE"));
        getContentPane().add(getContentPanel());
        setMinimumSize(new Dimension(400, 200));
        setSize(new Dimension(400, 400));
        setPreferredSize(new Dimension(400, 200));
        pack();
    }

    /**
     * the panel to display in the card layout of the wizard
     *
     * @return this page's panel
     */
    public JPanel getContentPanel()
    {
        JPanel contentPanel = new TransparentPanel(new BorderLayout());
        JPanel mainPanel = new TransparentPanel();
        JPanel basePanel = new TransparentPanel(new GridBagLayout());
        JPanel btnPanel = new TransparentPanel(new FlowLayout(
                FlowLayout.RIGHT));
        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);

        GridBagConstraints c = new GridBagConstraints();

        /* name text field */
        JLabel nameLabel = new JLabel(
                Resources.getString("impl.googlecontacts.ACCOUNT_NAME"));
        this.nameField = new JTextField();
        nameLabel.setLabelFor(nameField);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(nameLabel, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(nameField, c);
        JLabel nameExampleLabel = new JLabel("myaccount@gmail.com");
        nameExampleLabel.setForeground(Color.GRAY);
        nameExampleLabel.setFont(nameExampleLabel.getFont().deriveFont(8));
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 13, 2, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(nameExampleLabel, c);

        JLabel passwordLabel = new JLabel(
                Resources.getString("impl.googlecontacts.PASSWORD"));
        this.passwordField = new JPasswordField();
        nameLabel.setLabelFor(passwordField);
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(passwordLabel, c);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(passwordField, c);

        JLabel prefixLabel = new JLabel(
                Resources.getString("service.gui.PREFIX"));
        this.prefixField = new JTextField();
        prefixLabel.setLabelFor(prefixField);
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.insets = new Insets(2, 50, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        basePanel.add(prefixLabel, c);
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(2, 5, 0, 50);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        basePanel.add(prefixField, c);

        mainPanel.setLayout(boxLayout);
        mainPanel.add(basePanel);

        /* listeners */
        this.nameField.addActionListener(this);
        this.passwordField.addActionListener(this);
        this.saveBtn.addActionListener(this);
        this.cancelBtn.addActionListener(this);

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        contentPanel.add(mainPanel, BorderLayout.CENTER);
        contentPanel.add(btnPanel, BorderLayout.SOUTH);

        return contentPanel;
    }

    /**
     * Loads the information.
     *
     * @param cnx connection
     */
    public void loadData(GoogleContactsConnection cnx)
    {
        if(cnx != null)
        {
            this.nameField.setText(cnx.getLogin());
            this.passwordField.setText(cnx.getPassword());
            this.prefixField.setText(cnx.getPrefix());
            this.cnx = (GoogleContactsConnectionImpl) cnx;
        }
        else
        {
            this.nameField.setText("");
            this.passwordField.setText("");
            this.cnx = null;
        }
    }

    /**
     * Implementation of actionPerformed.
     *
     * @param e the ActionEvent triggered
     */
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();

        if(src == saveBtn)
        {
            String login = nameField.getText();
            String password = new String(passwordField.getPassword());
            String prefix = prefixField.getText();

            if(cnx == null)
            {
                cnx = (GoogleContactsConnectionImpl) GoogleContactsActivator
                    .getGoogleContactsService()
                        .getConnection(login, password);
            }
            else
            {
                cnx.setLogin(login);
                cnx.setPassword(password);
            }

            cnx.setPrefix(prefix);

            if(cnx.connect() == GoogleContactsConnection.ConnectionStatus.
                    ERROR_INVALID_CREDENTIALS)
            {
                JOptionPane.showMessageDialog(
                        this,
                        Resources.getString(
                                "impl.googlecontacts.WRONG_CREDENTIALS",
                                new String[]{login}),
                        Resources.getString(
                                "impl.googlecontacts.WRONG_CREDENTIALS",
                                new String[]{login}),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            retCode = 1;
            dispose();
        }
        else if(src == cancelBtn)
        {
            retCode = 0;
            dispose();
        }
    }

    /**
     * Get the connection.
     *
     * @return GoogleContactsConnection
     */
    public GoogleContactsConnection getConnection()
    {
        return cnx;
    }

    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key.
     *
     * @param escaped <tt>true</tt> if this dialog has been closed by pressing
     * the Esc key; otherwise, <tt>false</tt>
     */
    protected void close(boolean escaped)
    {
        cancelBtn.doClick();
    }

    /**
     * Show the dialog and returns if the user has modified something (create
     * or modify entry).
     *
     * @return true if the user has modified something (create
     * or modify entry), false otherwise.
     */
    public int showDialog()
    {
        retCode = 0;

        setVisible(true);

        // this will block until user click on save/cancel/press escape/close
        // the window
        setVisible(false);
        return retCode;
    }

    /**
     * Set the name field enable or not
     *
     * @param enable parameter to set
     */
    public void setNameFieldEnabled(boolean enable)
    {
        this.nameField.setEnabled(enable);
    }
}
