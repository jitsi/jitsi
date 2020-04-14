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
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Telephony related configuration panel.
 *
 * @author Sebastien Vincent
 */
public class TelephonyConfigPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The check box to enable/disable Jingle (audio and video calls with XMPP).
     */
    private final JCheckBox disableJingle = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.DISABLE_JINGLE"));

    /**
     * Text field for domain name.
     */
    private final JTextField domainField = new TrimTextField();

    /**
     * Text field for domain name.
     */
    private final JTextField domainBypassCapsField = new TrimTextField();

    /**
     * Main panel.
     */
    private final TransparentPanel mainPanel = new TransparentPanel();

    private final JPanel lblPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final JPanel valPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final JPanel telPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    /**
     * Constructor.
     */
    public TelephonyConfigPanel()
    {
        super(new BorderLayout());

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JLabel domainLbl = new JLabel(
            Resources.getString("plugin.jabberaccregwizz.TELEPHONY_DOMAIN"));
        JLabel gtalkCallLbl = new JLabel(
            Resources.getString(
                "plugin.jabberaccregwizz.DOMAIN_BYPASS_CAPS"));

        /*
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(domainLbl);
        panel.add(domainField);
        panel.add(gtalkCallLbl);

        add(panel);
        */
        lblPanel.add(domainLbl);
        lblPanel.add(gtalkCallLbl);
        valPanel.add(domainField);
        valPanel.add(domainBypassCapsField);

        telPanel.add(disableJingle, BorderLayout.NORTH);
        telPanel.add(lblPanel, BorderLayout.WEST);
        telPanel.add(valPanel, BorderLayout.CENTER);

        mainPanel.add(telPanel);

        add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Returns if Jingle is disabled.
     *
     * @return True if Jingle is disabled. False otherwise.
     */
    public boolean isJingleDisabled()
    {
        return disableJingle.isSelected();
    }

    /**
     * Disables or enables Jingle.
     *
     * @param disabled True to disable Jingle. False otherwise.
     */
    public void setDisableJingle(boolean disabled)
    {
        disableJingle.setSelected(disabled);
    }

    /**
     * Returns telephony domain.
     *
     * @return telephony domain
     */
    public String getTelephonyDomain()
    {
        return domainField.getText();
    }

    /**
     * Sets telephony domain.
     *
     * @param text telephony domain to set
     */
    public void setTelephonyDomain(String text)
    {
        domainField.setText(text);
    }

    /**
     * Returns telephony domain that bypass GTalk caps.
     *
     * @return telephony domain
     */
    public String getTelephonyDomainBypassCaps()
    {
        return domainBypassCapsField.getText();
    }

    /**
     * Sets telephony domain that bypass GTalk caps.
     *
     * @param text telephony domain to set
     */
    public void setTelephonyDomainBypassCaps(String text)
    {
        domainBypassCapsField.setText(text);
    }
}
