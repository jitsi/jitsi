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
package net.java.sip.communicator.impl.gui.main.contactlist.addgroup;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>CreateGroupPanel</tt> is the form for creating a group.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CreateGroupPanel
    extends TransparentPanel
{

    private JLabel uinLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.GROUP_NAME"));

    private JTextField textField = new JTextField();

    private TransparentPanel dataPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
            GuiActivator.getResources()
                .getI18NString("service.gui.CREATE_GROUP_NAME"));

    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.CREATE_GROUP"));

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_GROUP_ICON)));

    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private TransparentPanel rightPanel
        = new TransparentPanel(new BorderLayout());

    private JLabel errorLabel = new JLabel();

    /**
     * Creates and initializes the <tt>CreateGroupPanel</tt>.
     */
    public CreateGroupPanel()
    {
        super(new BorderLayout(20, 20));

        this.setPreferredSize(new Dimension(400, 200));

        this.iconLabel.setVerticalAlignment(JLabel.TOP);

        this.infoLabel.setEditable(false);

        this.dataPanel.add(uinLabel, BorderLayout.WEST);

        this.dataPanel.add(textField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);
        this.labelsPanel.add(errorLabel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        errorLabel.setForeground(Color.red);
        errorLabel.setVisible(false);

        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);
    }

    /**
     * Returns the string identifier entered by user.
     * @return the string identifier entered by user
     */
    public String getGroupName()
    {
        return textField.getText();
    }

    /**
     * Requests the focus in the contained text field.
     */
    public void requestFocusInField()
    {
        this.textField.requestFocus();
    }

    /**
     * Shows an error message below the create group text field.
     *
     * @param msg the message to show
     */
    public void showErrorMessage(String msg)
    {
        errorLabel.setText("*" + msg);

        errorLabel.setVisible(true);
    }

    /**
     * Reload icon label.
     */
    public void loadSkin()
    {
        iconLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_GROUP_ICON)));
    }
}
