/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>SecurityConfigForm</tt> allows the user to make all needed call
 * security configurations. It now wraps the ZRTP form in order to provide some
 * more explanations and make complex configurations available only to advanced
 * users.
 *
 * @author Yana Stamcheva
 */
public class SecurityConfigForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of <tt>SecurityConfigForm</tt>.
     */
    public SecurityConfigForm()
    {
        super(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        final ResourceManagementService resources
            = NeomediaActivator.getResources();

        JPanel mainPanel = new TransparentPanel(new BorderLayout(0, 10));
        add(mainPanel, BorderLayout.NORTH);

        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setText(resources.getI18NString(
            "impl.media.security.zrtp.DESCRIPTION",
            new String[]{resources.getSettingsString(
                "service.gui.APPLICATION_NAME")}));

        mainPanel.add(pane);

        JButton zrtpButton = new JButton(
            resources.getI18NString("impl.media.security.zrtp.ZRTP_NINJA"));

        zrtpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                SIPCommDialog zrtpDialog = new SIPCommDialog()
                {
                    /**
                     * Serial version UID.
                     */
                    private static final long serialVersionUID = 0L;

                    @Override
                    protected void close(boolean escaped) {}
                };

                zrtpDialog.setTitle(
                    resources.getI18NString("impl.media.security.zrtp.CONFIG"));
                zrtpDialog.getContentPane().add(new ZrtpConfigurePanel());

                zrtpDialog.setVisible(true);
            }
        });

        JPanel buttonPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(zrtpButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }
}
