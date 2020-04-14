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
package net.java.sip.communicator.plugin.exampleplugin;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * A plugin dialog that is open through the right button menu over a contact and
 * shows the contact name.
 *
 * @author Yana Stamcheva
 */
public class PluginDialog
    extends SIPCommDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private JTextArea infoTextArea = new JTextArea();

    private JPanel mainPanel = new TransparentPanel();

    private JLabel contactLabel = new JLabel();
    private JLabel nameLabel = new JLabel();

    /**
     * Creates an instance of this <tt>PluginDialog</tt> by specifying the
     * current <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt> we're going to treat.
     */
    public PluginDialog(MetaContact metaContact)
    {
        this.setTitle("Example plugin");

        this.infoTextArea.setPreferredSize(new Dimension(250, 70));

        this.infoTextArea.setText("This is an example plugin that shows the "
            + "currently selected contact"
            + " in a separate window.");

        this.nameLabel.setText("The name of the selected contact is:");
        this.contactLabel.setText(metaContact.getDisplayName());

        this.mainPanel.add(infoTextArea);
        this.mainPanel.add(nameLabel);
        this.mainPanel.add(contactLabel);

        this.getContentPane().add(mainPanel);

        this.initStyles();

        this.setResizable(false);
        this.pack();
    }

    /**
     * Initializes needed layouts, alignments, borders and different text area
     * style constants.
     */
    private void initStyles()
    {
        this.mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.infoTextArea.setEditable(false);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        this.infoTextArea.setAlignmentX(JTextArea.CENTER_ALIGNMENT);

        this.nameLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        this.contactLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        this.contactLabel.setAlignmentY(JLabel.TOP_ALIGNMENT);
        this.contactLabel.setFont(contactLabel.getFont().deriveFont(Font.BOLD));
    }

    /**
     * Implements {@link SIPCommDialog#close(boolean)} and does not perform any
     * special operations when the dialog is closed.
     *
     * @param escaped <tt>true</tt> if this dialog has been closed by pressing
     * the Esc key; otherwise, <tt>false</tt>
     * @see SIPCommDialog#close(boolean)
     */
    @Override
    protected void close(boolean escaped)
    {
    }
}
