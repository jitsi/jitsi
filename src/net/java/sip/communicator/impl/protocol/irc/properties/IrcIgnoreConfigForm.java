package net.java.sip.communicator.impl.protocol.irc.properties;

import net.java.sip.communicator.impl.protocol.irc.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.service.resources.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Implements the Irc ignore contact messages configuration panel.
 *
 * @author Duncan Robertson
 */
public class IrcIgnoreConfigForm
    extends TransparentPanel
        implements ConfigurationForm
{
    /**
     * The "ignore" field
     */
    private final JTextField ignoreField = new JTextField();

    /**
     * The <tt>ConfigurationService</tt> to be used to access configuration
     */
    private final org.jitsi.service.configuration.ConfigurationService configurationService
            = IrcActivator.getConfigurationService();

    /**
     * Resource management service instance.
     */
    private static ResourceManagementService Resources
            = IrcActivator.getResources();

    /**
     * Initialize a new <tt>IrcIgnoreConfigForm</tt> instance.
     */
    public IrcIgnoreConfigForm()
    {
        super(new BorderLayout());
        Box box = Box.createVerticalBox();
        add(box, BorderLayout.NORTH);

        TransparentPanel contentPanel = new TransparentPanel();
        contentPanel.setLayout(new BorderLayout(10, 10));

        box.add(contentPanel);

        TransparentPanel labelPanel
                = new TransparentPanel(new GridLayout(0, 1, 2, 2));
        TransparentPanel mainPanel
                = new TransparentPanel(new GridLayout(0, 1, 2, 2));

        contentPanel.add(labelPanel, BorderLayout.NORTH);
        contentPanel.add(mainPanel, BorderLayout.SOUTH);

        labelPanel.add(new JLabel(Resources.getI18NString(
                "plugin.irc.IRC_IGNORE_DESCRIPTION")));

        ignoreField.setText(
                (configurationService.getString(
                        IrcProperties.PROP_IRC_IGNORE,
                        "")));
        ignoreField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent focusEvent) {}

            public void focusLost(FocusEvent focusEvent) {
                configurationService.setProperty(
                        IrcProperties.PROP_IRC_IGNORE, ignoreField.getText());
            }
        });
        mainPanel.add(ignoreField);
    }

    @Override
    public String getTitle() {
        return Resources.getI18NString("plugin.irc.IRC_IGNORE_CONFIG");
    }

    @Override
    public byte[] getIcon() {
        return new byte[0];
    }

    @Override
    public Object getForm() {
        return this;
    }

    @Override
    public int getIndex() {
        return -1;
    }

    @Override
    public boolean isAdvanced() {
        return true;
    }
}
