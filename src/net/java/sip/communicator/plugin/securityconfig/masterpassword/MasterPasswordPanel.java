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
package net.java.sip.communicator.plugin.securityconfig.masterpassword;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.securityconfig.*;
import net.java.sip.communicator.plugin.securityconfig.masterpassword.MasterPasswordChangeDialog.MasterPasswordExecutable;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.resources.*;

/**
 * Panel containing the master password checkbox and change button.
 *
 * @author Dmitri Melnikov
 */
public class MasterPasswordPanel
    extends TransparentPanel
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(MasterPasswordPanel.class);

    /**
     * UI components.
     */
    private JCheckBox useMasterPasswordCheckBox;
    private JButton changeMasterPasswordButton;

    /**
     * The <tt>ResourceManagementService</tt> used by this instance to access
     * the localized and internationalized resources of the application.
     */
    private final ResourceManagementService resources
        = SecurityConfigActivator.getResources();

    /**
     * Builds the panel.
     */
    public MasterPasswordPanel()
    {
        this.setLayout(new BorderLayout(10, 10));
        this.setAlignmentX(0.0f);

        initComponents();
    }

    /**
     * Initialises the UI components.
     */
    private void initComponents()
    {
        useMasterPasswordCheckBox
            = new SIPCommCheckBox(
                    resources.getI18NString(
                            "plugin.securityconfig.masterpassword.USE_MASTER_PASSWORD"));
        useMasterPasswordCheckBox.addActionListener(this);
        useMasterPasswordCheckBox.setSelected(
                SecurityConfigActivator
                    .getCredentialsStorageService()
                        .isUsingMasterPassword());
        this.add(useMasterPasswordCheckBox, BorderLayout.WEST);

        changeMasterPasswordButton = new JButton();
        changeMasterPasswordButton.setText(
                resources.getI18NString(
                        "plugin.securityconfig.masterpassword.CHANGE_MASTER_PASSWORD"));
        changeMasterPasswordButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                showMasterPasswordChangeDialog();
            }
        });
        changeMasterPasswordButton.setEnabled(useMasterPasswordCheckBox
            .isSelected());
        this.add(changeMasterPasswordButton, BorderLayout.EAST);
    }

    /**
     * <tt>ActionListener</tt>'s logic for the master password check box.
     *
     * @param e action event
     */
    public void actionPerformed(ActionEvent e)
    {
        boolean isSelected = useMasterPasswordCheckBox.isSelected();
        // do not change the check box yet
        useMasterPasswordCheckBox.setSelected(!isSelected);
        if (isSelected)
            showMasterPasswordChangeDialog();
        else
        {
            // the checkbox is unselected only when this method finishes ok
            removeMasterPassword();
        }
    }

    /**
     * Show the dialog to change master password.
     */
    private void showMasterPasswordChangeDialog()
    {
        MasterPasswordChangeDialog dialog = MasterPasswordChangeDialog.getInstance();
        dialog.setCallback(new ChangeMasterPasswordCallback());
        dialog.setVisible(true);
    }

    /**
     * Displays a master password prompt to the user, verifies the entered
     * password and then executes <tt>ChangeMasterPasswordCallback.execute</tt>
     * method with null as the new password, thus removing it.
     */
    private void removeMasterPassword()
    {
        String master;
        boolean correct = true;

        MasterPasswordInputService masterPasswordInputService
            = SecurityConfigActivator.getMasterPasswordInputService();

        if(masterPasswordInputService == null)
        {
            logger.error("Missing MasterPasswordInputService to show input dialog");
            return;
        }

        do
        {
            master = masterPasswordInputService.showInputDialog(correct);
            if (master == null)
                return;
            correct =
                (master.length() != 0)
                    && SecurityConfigActivator.getCredentialsStorageService()
                        .verifyMasterPassword(master);
        }
        while (!correct);
        // remove the master password by setting it to null
        new ChangeMasterPasswordCallback().execute(master, null);
    }

    /**
     * A callback implementation that changes or removes the master password.
     * When the new password is null, the master password is removed.
     */
    class ChangeMasterPasswordCallback
        implements MasterPasswordExecutable
    {
        public boolean execute(String masterPassword, String newMasterPassword)
        {
            boolean remove = newMasterPassword == null;
            // update all passwords with new master pass
            boolean changed
                = SecurityConfigActivator
                    .getCredentialsStorageService()
                        .changeMasterPassword(
                            masterPassword,
                            newMasterPassword);
            if (!changed)
            {
                String titleKey
                    = remove
                        ? "plugin.securityconfig.masterpassword.MP_REMOVE_FAILURE"
                        : "plugin.securityconfig.masterpassword.MP_CHANGE_FAILURE";
                SecurityConfigActivator
                    .getUIService()
                    .getPopupDialog()
                    .showMessagePopupDialog(
                        resources.getI18NString(
                                "plugin.securityconfig.masterpassword.MP_CHANGE_FAILURE_MSG"),
                        resources.getI18NString(titleKey),
                        PopupDialog.ERROR_MESSAGE);
                return false;
            }
            else
            {
                String title = null;
                String msg = null;
                if (remove)
                {
                    title = "plugin.securityconfig.masterpassword.MP_REMOVE_SUCCESS";
                    msg = "plugin.securityconfig.masterpassword.MP_REMOVE_SUCCESS_MSG";
                    //disable the checkbox and change button
                    useMasterPasswordCheckBox.setSelected(false);
                    changeMasterPasswordButton.setEnabled(false);
                }
                else
                {
                    title = "plugin.securityconfig.masterpassword.MP_CHANGE_SUCCESS";
                    msg = "plugin.securityconfig.masterpassword.MP_CHANGE_SUCCESS_MSG";
                    // Enable the checkbox and change button.
                    useMasterPasswordCheckBox.setSelected(true);
                    changeMasterPasswordButton.setEnabled(true);
                }
                logger.debug("Master password successfully changed");
                SecurityConfigActivator
                    .getUIService()
                    .getPopupDialog()
                    .showMessagePopupDialog(
                        resources.getI18NString(msg),
                        resources.getI18NString(title),
                        PopupDialog.INFORMATION_MESSAGE);
            }
            return true;
        }
    }
}
