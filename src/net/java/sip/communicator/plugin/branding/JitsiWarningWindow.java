/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>JitsiWarningWindow</tt>.
 *
 * @author Yana Stamcheva
 */
public class JitsiWarningWindow
    extends SIPCommDialog
{
    /**
     * Creates an <tt>JitsiWarningWindow</tt> by specifying the parent frame
     * owner.
     * @param owner the parent owner
     */
    public JitsiWarningWindow(Frame owner)
    {
        super(owner, false);

        ResourceManagementService resources = BrandingActivator.getResources();

        this.setTitle(
            resources.getI18NString("service.gui.UPDATE")
            + " " + resources.getSettingsString(
                "service.gui.APPLICATION_NAME"));

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        StyledHTMLEditorPane textArea = new StyledHTMLEditorPane();
        textArea.setContentType("text/html");
        textArea.setText(resources.getI18NString("service.gui.JITSI_WARNING"));
        textArea.setOpaque(false);
        textArea.setEditable(false);

        JLabel titleLabel = new JLabel(
            resources.getI18NString("service.gui.JITSI_WARNING_TITLE"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setMaximumSize(new Dimension(400, 50));

        JPanel textPanel = new TransparentPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        textPanel.add(titleLabel);
        textPanel.add(Box.createRigidArea(new Dimension(20, 20)));
        textPanel.add(textArea);

        JButton downloadButton = new JButton(
            resources.getI18NString("service.gui.DOWNLOAD_NOW"));
        JButton remindButton = new JButton(
            resources.getI18NString("service.gui.REMIND_ME_LATER"));

        this.getRootPane().setDefaultButton(downloadButton);

        downloadButton.setMnemonic(
            resources.getI18nMnemonic("service.gui.DOWNLOAD_NOW"));
        downloadButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                openURL(getDownloadLink());
                dispose();
            }
        });
        remindButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(remindButton);
        buttonPanel.add(downloadButton);
        buttonPanel.setOpaque(false);

        JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));
        mainPanel.add(new JLabel(resources.getImage(
            "service.gui.SIP_COMMUNICATOR_LOGO_128x128")), BorderLayout.WEST);
        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setPreferredSize(new Dimension(500, 200));
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20));

        getContentPane().add(mainPanel);

        this.pack();
        this.setResizable(false);

        setLocationRelativeTo(getParent());

        this.getRootPane().getActionMap().put("close", new CloseAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

        if(OSUtils.IS_MAC)
        {
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK),
                "close");
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                "close");
        }
    }

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends UIAction
    {
        public void actionPerformed(ActionEvent e)
        {
            setVisible(false);
            dispose();
        }
    }

    /**
     * Opens the given url in a new browser window.
     * @param url the url to open
     */
    private void openURL(String url)
    {
        ServiceReference serviceReference = BrandingActivator
                .getBundleContext().getServiceReference(
                        BrowserLauncherService.class.getName());

        if (serviceReference != null)
        {
            BrowserLauncherService browserLauncherService
                = (BrowserLauncherService) BrandingActivator
                    .getBundleContext().getService(
                        serviceReference);

            browserLauncherService.openURL(url);
        }
    }

    /**
     * Indicates if the application name should be shown.
     *
     * @return <tt>true</tt> if the application name should be shown,
     * <tt>false</tt> - otherwise
     */
    private String getDownloadLink()
    {
        if (OSUtils.IS_WINDOWS)
            return "http://download.sip-communicator.org/nightly/windows/";
        else if (OSUtils.IS_MAC)
            return "http://download.sip-communicator.org/nightly/macosx/";
        else if (OSUtils.IS_LINUX)
            return "http://download.sip-communicator.org/nightly/linux/";

        return "http://download.jitsi.org";
    }

    @Override
    protected void close(boolean escaped) {}
}
