/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.skinmanager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * @author Adam Netocny
 */
public class SkinManagerPanel
        extends TransparentPanel
{
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
