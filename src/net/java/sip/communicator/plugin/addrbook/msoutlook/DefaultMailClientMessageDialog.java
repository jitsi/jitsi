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
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.awt.event.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * A dialog with warning message that Outlook is not the default mail client
 * shown when the contact source is started.
 * 
 * @author Hristo Terezov
 */
public class DefaultMailClientMessageDialog
    extends MessageDialog
{
    
    /**
     * Serial ID.
     */
    private static final long serialVersionUID = -6321186451307613417L;
    
    /**
     * The <tt>ResourceManagementService</tt>
     */
    private static ResourceManagementService resources 
        = AddrBookActivator.getResources();
    
    /**
     * Make Outlook default mail client check box.
     */
    private JCheckBox defaultMailClientCheckBox = new SIPCommCheckBox(
        resources
            .getI18NString("plugin.addrbook.MAKE_OUTLOOK_DEFAULT_MAIL_CLIENT")); 
    
    public static int DONT_ASK_SELECTED_MASK = 1;
    
    public static int DEFAULT_MAIL_CLIENT_SELECTED_MASK = 2;
    
    /**
     * Creates an instance of <tt>DefaultMailClientMessageDialog</tt>. 
     */
    public DefaultMailClientMessageDialog()
    {
        super(null, 
            AddrBookActivator.getResources().getI18NString(
            "plugin.addrbook.OUTLOOK_IS_NOT_DEFAULT_MAIL_CLIENT_TITLE"), 
            resources.getI18NString(
                "plugin.addrbook.OUTLOOK_IS_NOT_DEFAULT_MAIL_CLIENT",
                new String[]{
                    resources.getSettingsString(
                        "service.gui.APPLICATION_NAME")}), false);
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        checkBoxPanel.add(defaultMailClientCheckBox);
    }
    
    
    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();

        if(!button.equals(okButton))
            return;
        
        this.returnCode = 0;
        
        if (doNotAskAgain.isSelected())
        {
            this.returnCode = this.returnCode | DONT_ASK_SELECTED_MASK;
        }
        
        if (defaultMailClientCheckBox.isSelected())
        {
            this.returnCode 
                = this.returnCode | DEFAULT_MAIL_CLIENT_SELECTED_MASK;
        }

        this.dispose();
    }

}
