/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig.masterpassword;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.securityconfig.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Panel containing the saved passwords button.
 *
 * @author Dmitri Melnikov
 */
public class SavedPasswordsPanel
    extends TransparentPanel
{

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
                SavedPasswordsDialog.getInstance().setVisible(true);
            }
        });
        this.add(savedPasswordsButton, BorderLayout.EAST);
    }
}
