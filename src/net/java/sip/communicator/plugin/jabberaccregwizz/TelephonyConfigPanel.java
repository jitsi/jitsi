/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

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
        telPanel.add(lblPanel, BorderLayout.WEST);
        telPanel.add(valPanel, BorderLayout.CENTER);

        mainPanel.add(telPanel);

        add(mainPanel, BorderLayout.NORTH);
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
