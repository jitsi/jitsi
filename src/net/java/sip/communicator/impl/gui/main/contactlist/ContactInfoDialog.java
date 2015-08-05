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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>ContactInfoPanel</tt> is a popup dialog containing the contact
 * detailed info.
 *
 * @author Yana Stamcheva
 */
public class ContactInfoDialog
    extends SIPCommDialog
    implements WindowFocusListener
{

    private JPanel protocolsPanel = new TransparentPanel(new GridLayout(0, 1));

    private TransparentBackground bg;

    /**
     * Creates an instance of the <tt>ContactInfoPanel</tt>.
     *
     * @param owner The frame owner of this dialog.
     * @param contactItem The <tt>MetaContact</tt> for the info.
     */
    public ContactInfoDialog(Frame owner, MetaContact contactItem)
    {
        super(owner);

        this.setUndecorated(true);

        this.setModal(true);

        // Create the transparent background component
        this.bg = new TransparentBackground(this);

        this.bg.setLayout(new BorderLayout());

        this.bg.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.getContentPane().setLayout(new BorderLayout());

        this.init();

        this.getContentPane().add(bg, BorderLayout.CENTER);

        this.pack();

        this.setSize(140, 50);

        this.addWindowFocusListener(this);
    }

    /**
     * Initializes the <tt>ContactInfoPanel</tt>.
     */
    private void init()
    {
        /*
         * String[] protocolList = this.contactItem.getC();
         *
         * if(protocolsPanel.getComponentCount() == 0){ for(int i = 0; i <
         * protocolList.length; i ++){
         *
         * JLabel protocolLabel = new JLabel(protocolList[i], new
         * ImageIcon(Constants.getProtocolIcon(protocolList[i])), JLabel.LEFT);
         *
         * this.protocolsPanel.add(protocolLabel); } }
         *
         * this.bg.add(protocolsPanel, BorderLayout.CENTER);
         */
    }

    /**
     * Returns the panel containing all contact protocols' information.
     *
     * @return the panel containing all contact protocols' information.
     */
    public JPanel getProtocolsPanel()
    {
        return protocolsPanel;
    }

    public void windowGainedFocus(WindowEvent e)
    {

    }

    public void windowLostFocus(WindowEvent e)
    {
        close(false);
    }

    public void setPopupLocation(int x, int y)
    {
        this.setLocation(x, y);

        this.bg.updateBackground(x, y);
    }

    @Override
    protected void close(boolean isEscaped)
    {
        this.setVisible(false);
        this.dispose();
    }
}
