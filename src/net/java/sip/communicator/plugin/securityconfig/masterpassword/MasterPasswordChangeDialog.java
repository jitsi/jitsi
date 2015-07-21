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

import java.awt.event.*;

import net.java.sip.communicator.plugin.securityconfig.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * UI dialog to change the master password.
 *
 * @author Dmitri Melnikov
 * @author Boris Grozev
 */
public class MasterPasswordChangeDialog
    extends PasswordChangeDialog
    implements ActionListener,
               KeyListener
{
    /**
     * Callback interface. Implementing classes know how to change the master
     * password from the old to the new one.
     */
    interface MasterPasswordExecutable
    {
        /**
         * The actions to execute to change the master password.
         *
         * @param masterPassword old master password
         * @param newMasterPassword new master password
         * @return true on success, false on failure.
         */
        public boolean execute(String masterPassword, String newMasterPassword);
    }

     /**
     * Callback to execute on password change.
     */
    private MasterPasswordExecutable callback;

    /**
     * Dialog instance of this class.
     */
    private static MasterPasswordChangeDialog dialog;

    /**
     * Builds the dialog.
     */
    private MasterPasswordChangeDialog()
    {
        super(SecurityConfigActivator.getCredentialsStorageService()
                    .isUsingMasterPassword());

        setTitle(resources.getI18NString(
                        "plugin.securityconfig.masterpassword.MP_TITLE"));
        setInfoText(resources.getI18NString(
                        "plugin.securityconfig.masterpassword.INFO_TEXT"));

        getOkButton().addActionListener(this);
    }

    /**
     * OK button event handler.
     *
     * @param e action event
     */
    public void actionPerformed(ActionEvent e)
    {
        boolean close = false;
        CredentialsStorageService credentialsStorageService
            = SecurityConfigActivator.getCredentialsStorageService();
        String oldMasterPassword = null;

        if (credentialsStorageService.isUsingMasterPassword())
        {
            oldMasterPassword = getCurrentPassword();
            if (oldMasterPassword.length() == 0)
            {
                displayPopupError(resources.getI18NString(
                      "plugin.securityconfig.masterpassword.MP_CURRENT_EMPTY"));
                return;
            }
            boolean verified = credentialsStorageService
                        .verifyMasterPassword(oldMasterPassword);
            if (!verified)
            {
                displayPopupError(resources.getI18NString(
                    "plugin.securityconfig.masterpassword.MP_VERIFICATION_FAILURE_MSG"));
                return;
            }
        }
        // if the callback executes OK, we close the dialog
        if (callback != null)
        {
            close = callback.execute(oldMasterPassword, getNewPassword());
        }

        if (close)
        {
            dialog = null;
            dispose();
        }
    }

    /**
     * @return dialog instance
     */
    public static MasterPasswordChangeDialog getInstance()
    {
        if (dialog == null)
            dialog = new MasterPasswordChangeDialog();
        return dialog;
    }

     /**
     * @param callbackInstance callback instance.
     */
    public void setCallback(MasterPasswordExecutable callbackInstance)
    {
        this.callback = callbackInstance;
    }

}
