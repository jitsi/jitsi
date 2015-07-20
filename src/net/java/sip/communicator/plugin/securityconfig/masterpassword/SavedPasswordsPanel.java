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
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

/**
 * Panel containing the saved passwords button.
 *
 * @author Dmitri Melnikov
 */
public class SavedPasswordsPanel
    extends TransparentPanel
{
    /**
     * The logger.
     */
    private static Logger logger
        = Logger.getLogger(SavedPasswordsPanel.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The {@link CredentialsStorageService}.
     */
    private static final CredentialsStorageService credentialsStorageService
        = SecurityConfigActivator.getCredentialsStorageService();

    /**
     * Builds the panel.
     */
    public SavedPasswordsPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setAlignmentX(0.0f);

        initComponents();
    }

    /**
     * Initializes the UI components.
     */
    private void initComponents()
    {
        JButton savedPasswordsButton = new JButton();
        savedPasswordsButton.setText(
                SecurityConfigActivator
                    .getResources()
                        .getI18NString(
                            "plugin.securityconfig.masterpassword.SAVED_PASSWORDS"));
        savedPasswordsButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (credentialsStorageService.isUsingMasterPassword())
                {
                    showSavedPasswordsDialog();
                } else
                {
                    SavedPasswordsDialog.getInstance().setVisible(true);
                }
            }
        });
        this.add(savedPasswordsButton, BorderLayout.EAST);
    }

    /**
     * Displays a master password prompt to the user, verifies the entered
     * password and then shows the <tt>SavedPasswordsDialog</tt>.
     */
    private void showSavedPasswordsDialog()
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
            correct
                = (master.length() != 0)
                    && credentialsStorageService.verifyMasterPassword(master);
        }
        while (!correct);
        SavedPasswordsDialog.getInstance().setVisible(true);
    }
}
