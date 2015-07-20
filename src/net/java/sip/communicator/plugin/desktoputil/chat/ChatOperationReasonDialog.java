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
package net.java.sip.communicator.plugin.desktoputil.chat;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 */
public class ChatOperationReasonDialog extends MessageDialog
{
    private static final long serialVersionUID = 3290030744711759011L;

    private final JTextField reasonField = new JTextField();

    private final JPanel reasonFieldPanel = new JPanel(new BorderLayout());

    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> using the
     * default title and message.
     */
    public ChatOperationReasonDialog()
    {
        this(null,
            DesktopUtilActivator.getResources().getI18NString(
            "service.gui.REASON"),
            DesktopUtilActivator.getResources().getI18NString(
            "service.gui.SPECIFY_REASON"),
            DesktopUtilActivator.getResources().getI18NString(
            "service.gui.OK"), true, false);
        
        
    }
    
    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> using the
     * default title and message.
     * 
     * @param disableOKIfReasonIsEmpty if true the OK button will be disabled if
     * the reason text is empty.
     */
    public ChatOperationReasonDialog(boolean disableOKIfReasonIsEmpty)
    {
        this(null,
            DesktopUtilActivator.getResources().getI18NString(
            "service.gui.REASON"),
            DesktopUtilActivator.getResources().getI18NString(
            "service.gui.SPECIFY_REASON"),
            DesktopUtilActivator.getResources().getI18NString(
            "service.gui.OK"), true, disableOKIfReasonIsEmpty);
        
        
    }

    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the title and the message shown in the dialog.
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     */
    public ChatOperationReasonDialog(String title, String message)
    {
        this(null,
            title,
            message,
            DesktopUtilActivator.getResources().getI18NString("service.gui.OK"),
            true,
            false);
        
    }
    
    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the title and the message shown in the dialog.
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     * @param disableOKIfReasonIsEmpty if true the OK button will be disabled if
     * the reason text is empty.
     * @param showReasonLabel specify if we want the "Reason:" label
     */
    public ChatOperationReasonDialog(String title, String message,
        boolean showReasonLabel,
        boolean disableOKIfReasonIsEmpty)
    {
        this(null,
            title,
            message,
            DesktopUtilActivator.getResources().getI18NString("service.gui.OK"),
            showReasonLabel,
            disableOKIfReasonIsEmpty);
    }

    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the parent window, the title and the message to show.
     * @param chatWindow the parent window
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     * @param okButtonName the custom name of the ok button
     * @param showReasonLabel specify if we want the "Reason:" label
     */
    public ChatOperationReasonDialog(Frame chatWindow, String title,
        String message, String okButtonName, boolean showReasonLabel)
    {
        this(chatWindow,
            title,
            message,
            okButtonName,
            showReasonLabel,
            false);
    }
    
    /**
     * Creates an instance of <tt>ChatOperationReasonDialog</tt> by specifying
     * the parent window, the title and the message to show.
     *
     * @param chatWindow the parent window
     * @param title the title of this dialog
     * @param message the message shown in this dialog
     * @param okButtonName the custom name of the ok button
     * @param showReasonLabel specify if we want the "Reason:" label
     * @param disableOKIfReasonIsEmpty if true the OK button will be disabled if
     * the reason text is empty.
     */
    public ChatOperationReasonDialog(Frame chatWindow, String title,
        String message, String okButtonName, boolean showReasonLabel, 
        boolean disableOKIfReasonIsEmpty)
    {
        super(chatWindow, title, message, okButtonName, false);

        JPanel reasonPanel = new JPanel(new BorderLayout());
        JLabel reasonLabel
            = new JLabel(
                    showReasonLabel
                        ? (DesktopUtilActivator.getResources().getI18NString(
                                "service.gui.REASON")
                            + ":")
                        : "");

        reasonPanel.add(reasonLabel, BorderLayout.WEST);
        reasonPanel.add(new JLabel("          "), BorderLayout.EAST);

        reasonFieldPanel.add(reasonField, BorderLayout.NORTH);
        reasonFieldPanel.setOpaque(false);
        reasonPanel.add(reasonFieldPanel, BorderLayout.CENTER);
        reasonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reasonPanel.setOpaque(false);

        replaceCheckBoxPanel(reasonPanel);
        
        if(disableOKIfReasonIsEmpty)
        {
            updateOKButtonState();
            reasonField.getDocument().addDocumentListener(
                    new DocumentListener()
                    {
                        public void removeUpdate(DocumentEvent ev)
                        {
                            updateOKButtonState();                
                        }

                        public void insertUpdate(DocumentEvent ev)
                        {
                            updateOKButtonState();
                        }

                        public void changedUpdate(DocumentEvent ev)
                        {
                            updateOKButtonState();
                        }
                    });
        }
        this.pack();
    }
    
    /**
     * Adds component to panel which contains the reason text field.
     * @param comp the component to be added.
     */
    public void addToReasonFieldPannel(Component comp)
    {
        reasonFieldPanel.add(comp, BorderLayout.CENTER);
    }

    /**
     * Enables the OK button if reason field is not empty and disables it if the
     * reason field is empty.
     */
    private void updateOKButtonState()
    {
        okButton.setEnabled(!reasonField.getText().trim().equals(""));
    }

    /**
     * Returns the text entered in the reason field.
     * @return the text entered in the reason field
     */
    public String getReason()
    {
        return reasonField.getText();
    }

    /**
     * Sets a default value for the reason field.
     * @param value the text to set as default text for the reason field
     */
    public void setReasonFieldText(String value)
    {
        reasonField.setText(value);
    }

    /**
     * Sets the message to be displayed.
     * @param message The message to be displayed.
     */
    public void setMessage(String message)
    {
        super.setMessage(message);

        setMaxWidth(400);
    }
}
