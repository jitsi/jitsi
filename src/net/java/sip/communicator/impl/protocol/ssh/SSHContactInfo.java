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
package net.java.sip.communicator.impl.protocol.ssh;

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * @author Shobhit Jindal
 */
class SSHContactInfo
    extends SIPCommDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private ContactSSH sshContact;

    private JPanel mainPanel = new TransparentPanel();
    private JPanel machinePanel = new TransparentPanel();
    private JPanel detailNamesPanel = new TransparentPanel();
    private JPanel detailFieldsPanel = new TransparentPanel();
    private JPanel detailsPanel = new TransparentPanel();

    private JCheckBox addDetailsCheckBox = new SIPCommCheckBox("Add Details");

    private JButton doneButton = new JButton("Done");
    private JLabel machineID = new JLabel("Hostname / IP: ");
    private JTextField machineIDField = new JTextField();
    private JLabel userName = new JLabel("User Name: ");
    private JTextField userNameField = new JTextField();
    private JLabel password = new JLabel("Password: ");
    private JTextField passwordField = new JPasswordField();
    private JLabel port = new JLabel("Port: ");

    private JFormattedTextField portField;
    private JLabel secs = new JLabel("secs");
    private JLabel statusUpdate = new JLabel("Update Interval: ");
    private JLabel terminalType = new JLabel("Terminal Type: ");
    private JTextField terminalTypeField = new JTextField("SIP Communicator");
    private JSpinner updateTimer = new JSpinner();

    private JPanel emptyPanel1 = new TransparentPanel();

    private JPanel emptyPanel2 = new TransparentPanel();

    private JPanel emptyPanel3 = new TransparentPanel();

    private JPanel emptyPanel4 = new TransparentPanel();

    private JPanel emptyPanel5 = new TransparentPanel();

    private JPanel emptyPanel6 = new TransparentPanel();

    private JPanel emptyPanel7 = new TransparentPanel();

    private JPanel emptyPanel8 = new TransparentPanel();

    private JPanel emptyPanel9 = new TransparentPanel();

    private JPanel emptyPanel10 = new TransparentPanel();

    private JPanel emptyPanel11 = new TransparentPanel();

//    private ContactGroup contactGroup = null;

    /**
     * Creates a new instance of SSHContactInfo
     *
     * @param sshContact the concerned contact
     */
    public SSHContactInfo(ContactSSH sshContact) {
        super(true);

        this.sshContact = sshContact;
        initForm();

        this.getContentPane().add(mainPanel);

        this.setSize(370, 325);

        this.setResizable(false);

        this.setTitle("SSH: Account Details of " + sshContact.getDisplayName());

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x,y);

//        ProtocolProviderServiceSSHImpl.getUIService().getConfigurationWindow().
//                addConfigurationForm(this);
    }

    /**
     * initialize the form.
     */
    public void initForm() {
        updateTimer.setValue(30);
        MaskFormatter maskFormatter = new MaskFormatter();
        try {
            maskFormatter.setMask("#####");
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        maskFormatter.setAllowsInvalid(false);
        portField = new JFormattedTextField(maskFormatter);
        portField.setValue(22);

        userNameField.setEnabled(false);
        passwordField.setEditable(false);
        portField.setEnabled(false);
        terminalTypeField.setEnabled(false);
        updateTimer.setEnabled(false);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        machinePanel.setLayout(new BoxLayout(machinePanel, BoxLayout.X_AXIS));
        detailNamesPanel.setLayout(new BoxLayout(detailNamesPanel,
                BoxLayout.Y_AXIS));
        detailFieldsPanel.setLayout(new BoxLayout(detailFieldsPanel,
                BoxLayout.Y_AXIS));
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.X_AXIS));

        machinePanel.add(machineID);
        machinePanel.add(machineIDField);

        detailNamesPanel.add(userName);
        detailNamesPanel.add(emptyPanel1);
        detailNamesPanel.add(password);
        detailNamesPanel.add(emptyPanel2);
        detailNamesPanel.add(port);
        detailNamesPanel.add(emptyPanel3);
        detailNamesPanel.add(statusUpdate);
        detailNamesPanel.add(emptyPanel4);
        detailNamesPanel.add(terminalType);

        detailFieldsPanel.add(userNameField);
        detailFieldsPanel.add(emptyPanel5);
        detailFieldsPanel.add(passwordField);
        detailFieldsPanel.add(emptyPanel6);
        detailFieldsPanel.add(portField);
        detailFieldsPanel.add(emptyPanel7);
        detailFieldsPanel.add(updateTimer);
        detailFieldsPanel.add(emptyPanel8);
        detailFieldsPanel.add(terminalTypeField);

        detailsPanel.add(detailNamesPanel);
        detailsPanel.add(detailFieldsPanel);

        detailsPanel.setBorder(BorderFactory.createTitledBorder("Details"));

        mainPanel.add(emptyPanel9);
        mainPanel.add(machinePanel);
        mainPanel.add(addDetailsCheckBox);
        mainPanel.add(detailsPanel);
        mainPanel.add(emptyPanel10);
        mainPanel.add(doneButton);
        mainPanel.add(emptyPanel11);

        addDetailsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addDetailsCheckBox.setEnabled(false);
                userNameField.setEnabled(true);
                passwordField.setEditable(true);
                portField.setEnabled(true);
                terminalTypeField.setEnabled(true);
                updateTimer.setEnabled(true);

                userNameField.grabFocus();
            }
        });

        doneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if(machineIDField.getText().equals("")) {
                    machineIDField.setText("Field needed");
                    return;
                }

                sshContact.savePersistentDetails();

                //add contact to contact list
                ((OperationSetPersistentPresenceSSHImpl)sshContact
                    .getParentPresenceOperationSet())
                        .addContactToList(
                            sshContact.getParentContactGroup(),
                            sshContact);

                setVisible(false);
            }
        });
    }

    /**
     * Return the ssh icon
     *
     * @return the ssh icon
     */
    public byte[] getIcon() {
        return Resources.getImage(Resources.SSH_LOGO);
    }

//
//    public void setContactGroup(ContactGroup contactGroup)
//    {
//        this.contactGroup = contactGroup;
//    }
//
//    public ContactGroup getContactGroup()
//    {
//        return this.contactGroup;
//    }

    /**
     * Sets the UserName of the dialog
     *
     * @param userName to be associated
     */
    public void setUserNameField(String userName) {
        this.userNameField.setText(userName);
    }

    /**
     * Sets the Password of the dialog
     *
     * @param password to be associated
     */
    public void setPasswordField(String password) {
        this.passwordField.setText(password);
    }

    /**
     * Return the hostname
     *
     * @return the hostname
     */
    public String getHostName() {
        return this.machineIDField.getText();
    }

    /**
     * Return the username
     *
     * @return the username
     */
    public String getUserName() {
        return this.userNameField.getText();
    }

    /**
     * Return the password
     *
     * @return the password in a clear form
     */
    public String getPassword() {
        return this.passwordField.getText();
    }

    /**
     * Return the terminal type
     *
     * @return the terminal type
     */
    public String getTerminalType() {
        return this.terminalTypeField.getText();
    }

    /**
     * Return the port
     *
     * @return the port value
     */
    public int getPort() {
        return Integer.parseInt(this.portField.getText().trim());
    }

    /**
     * Return the update interval
     *
     * @return the update interval
     */
    public int getUpdateInterval() {
        return Integer.parseInt(String.valueOf(this.updateTimer.getValue()));
    }

    /**
     * Sets the HostName of the dialog
     *
     * @param hostName to be associated
     */
    public void setHostNameField(String hostName) {
        this.machineIDField.setText(hostName);
    }

    /**
     * Sets the Terminal Type of the dialog
     *
     * @param termType to be associated
     */
    public void setTerminalType(String termType) {
        this.terminalTypeField.setText(termType);
    }

    /**
     * Sets the Update Interval of the dialog
     *
     * @param interval to be associated
     */
    public void setUpdateInterval(int interval) {
        this.updateTimer.setValue(interval);
    }

    /**
     * Sets the Port of the dialog
     *
     * @param port to be associated
     */
    public void setPort(String port) {
        this.portField.setText(port);
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }
}
