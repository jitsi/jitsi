/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>WarningPanel</tt> allows users to change Jitsi configuration
 * properties at runtime. It wraps the <tt>PropertiesEditorPanel</tt> in order
 * to provide some more explanations and make complex configurations available
 * only to advanced users.
 * 
 * @author Marin Dzhigarov
 */
public class WarningPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    private ResourceManagementService resourceManagementService =
        PropertiesEditorActivator.getResourceManagementService();

    private ConfigurationService confService = PropertiesEditorActivator
        .getConfigurationService();

    private JCheckBox checkBox;

    /**
     * Creates an instance <tt>WarningPanel</tt>
     * 
     * @param startingPanel
     */
    public WarningPanel(final StartingPanel startingPanel)
    {
        super(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel mainPanel = new TransparentPanel(new BorderLayout(0, 10));
        add(mainPanel, BorderLayout.NORTH);

        JTextPane warningMsg = new JTextPane();
        warningMsg.setEditable(false);
        warningMsg.setOpaque(false);
        warningMsg.setText(resourceManagementService
            .getI18NString("plugin.propertieseditor.DESCRIPTION"));

        checkBox =
            new JCheckBox(
                resourceManagementService
                    .getI18NString("plugin.propertieseditor.CHECK_BOX"));
        checkBox.setOpaque(false);
        checkBox.setSelected(true);

        mainPanel.add(warningMsg, BorderLayout.NORTH);
        mainPanel.add(checkBox, BorderLayout.CENTER);

        JButton btn =
            new JButton(
                resourceManagementService
                    .getI18NString("plugin.propertieseditor.IM_AWARE"));
        btn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                confService.setProperty(Constants.SHOW_WARNING_MSG_PROP,
                    new Boolean(checkBox.isSelected()));

                startingPanel.removeAll();
                startingPanel.add(startingPanel.propertiesEditorPanel,
                    BorderLayout.CENTER);
                startingPanel.propertiesEditorPanel.setVisible(true);
                startingPanel.revalidate();
                startingPanel.repaint();
            }
        });

        JPanel buttonPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(btn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }
}
