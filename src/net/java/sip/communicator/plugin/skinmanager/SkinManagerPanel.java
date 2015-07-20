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
package net.java.sip.communicator.plugin.skinmanager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.osgi.framework.*;

/**
 * @author Adam Netocny
 */
public class SkinManagerPanel
        extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Remove button.
     */
    private final JButton rmButton = new JButton("Remove selected skin");

    /**
     * <tt>SkinSelector</tt> component.
     */
    private final SkinSelector skinSelector = new SkinSelector();

    /**
     * Creates an instance of <tt>SkinManagerPanel</tt>.
     */
    public SkinManagerPanel()
    {
        super(new BorderLayout());

        JPanel selectorPanel = new TransparentPanel();
        selectorPanel.setLayout(new BoxLayout(selectorPanel, BoxLayout.Y_AXIS));

        skinSelector.setAlignmentX(Component.CENTER_ALIGNMENT);
        skinSelector.addItemListener(new EnableDisableListener());
        selectorPanel.add(skinSelector);

        rmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rmButton.addActionListener(new RemoveListener());
        selectorPanel.add(rmButton);

        enableDisableButton();

        add(selectorPanel, BorderLayout.NORTH);
    }

    /**
     * Enables(if a skin <tt>Bundle</tt> is selected) or disables the remove
     * button.
     */
    private void enableDisableButton()
    {
        Object tmp = skinSelector.getSelectedItem();

        if(tmp != null)
        {
            if(tmp instanceof Bundle)
            {
                rmButton.setEnabled(true);

                return;
            }
        }
        rmButton.setEnabled(false);
    }

    /**
     * Listener for the remove button events.
     */
    private class RemoveListener implements ActionListener
    {
        /**
         * Invoked when an action occurs.
         * @param e <tt>ActionEvent</tt>.
         */
        public void actionPerformed(ActionEvent e)
        {
            Object tmp = skinSelector.getSelectedItem();
            if(tmp != null)
            {
                if(tmp instanceof Bundle)
                {
                    try
                    {
                        ((Bundle) tmp).uninstall();
                    }
                    catch (BundleException ex)
                    {
                    }
                }
            }
        }
    }

    /**
     * Selection listener for enabling/disabling of remove button.
     */
    private class EnableDisableListener
        implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    enableDisableButton();
                }
            });
        }
    }
}
