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
package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.browserlauncher.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;
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
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

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
            return "http://download.jitsi.org/jitsi/windows/";
        else if (OSUtils.IS_MAC)
            return "http://download.jitsi.org/jitsi/macosx/";

        return "http://download.jitsi.org";
    }

    @Override
    protected void close(boolean escaped) {}
}
