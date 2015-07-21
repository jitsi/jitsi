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
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ChatRoomNamePanel</tt> is the form, where we should enter the chat
 * room name.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomNamePanel
    extends TransparentPanel
    implements DocumentListener
{
   private JLabel nameLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.CHAT_ROOM_NAME"));

    private JTextField textField = new JTextField();

    private JPanel dataPanel = new TransparentPanel(new BorderLayout(5, 5));

    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.CHAT_ROOM_NAME_INFO"));

    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.CREATE_CHAT_ROOM"));

    private JPanel labelsPanel =
        new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel rightPanel = new TransparentPanel(new BorderLayout());

    private WizardContainer parentWizard;

    /**
     * Creates and initializes the <tt>ChatRoomNamePanel</tt>.
     */
    public ChatRoomNamePanel()
    {
        this(null);
    }

    /**
     * Creates and initializes the <tt>ChatRoomNamePanel</tt>.
     * @param wizard The parent wizard, where this panel will be added
     */
    public ChatRoomNamePanel(WizardContainer wizard)
    {
        super(new BorderLayout());

        this.parentWizard = wizard;

        this.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.infoLabel.setEditable(false);

        this.dataPanel.add(nameLabel, BorderLayout.WEST);

        this.dataPanel.add(textField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(
            font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);

        this.rightPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.add(rightPanel, BorderLayout.CENTER);

        this.textField.getDocument().addDocumentListener(this);
    }

    /**
     * Returns the chat room name entered by user.
     * @return the chat room name entered by user
     */
    public String getChatRoomName()
    {
        return textField.getText();
    }

    /**
     * Requests the current focus in the chat room name field.
     */
    public void requestFocusInField()
    {
        this.textField.requestFocus();
    }

    public void changedUpdate(DocumentEvent e)
    {
    }

    public void insertUpdate(DocumentEvent e)
    {
        this.setNextFinishButtonAccordingToUIN();
    }

    public void removeUpdate(DocumentEvent e)
    {
        this.setNextFinishButtonAccordingToUIN();
    }

    /**
     * Enables or disables the Next/Finish button of the parent wizard,
     * depending on whether the text field is empty.
     */
    public void setNextFinishButtonAccordingToUIN()
    {
        if(parentWizard != null)
        {
            if(textField.getText() != null && textField.getText().length() > 0)
            {
                parentWizard.setNextFinishButtonEnabled(true);
            }
            else
            {
                parentWizard.setNextFinishButtonEnabled(false);
            }
        }
    }
}
