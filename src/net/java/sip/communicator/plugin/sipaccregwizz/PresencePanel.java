/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>PresencePanel</tt> is the one containing presence information.
 *
 * @author Yana Stamcheva
 */
public class PresencePanel
    extends TransparentPanel
{
    private JPanel presenceOpPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel buttonsPresOpPanel =
        new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel labelsPresOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesPresOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JCheckBox enablePresOpButton =
        new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_PRESENCE"), true);

    private JCheckBox forceP2PPresOpButton =
        new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.FORCE_P2P_PRESENCE"), false);

    private JLabel pollPeriodLabel = new JLabel(
        Resources.getString(
            "plugin.sipaccregwizz.OFFLINE_CONTACT_POLLING_PERIOD"));

    private JLabel subscribeExpiresLabel = new JLabel(
        Resources.getString("plugin.sipaccregwizz.SUBSCRIPTION_EXPIRATION"));

    private JTextField pollPeriodField
        = new JTextField(SIPAccountRegistration.DEFAULT_POLL_PERIOD);

    private JTextField subscribeExpiresField =
        new JTextField(SIPAccountRegistration.DEFAULT_SUBSCRIBE_EXPIRES);

    /**
     * Creates an instance of <tt>PresencePanel</tt>.
     */
    public PresencePanel()
    {
        super(new BorderLayout());

        enablePresOpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                // Perform action
                JCheckBox cb = (JCheckBox) evt.getSource();

                setPresenceOptionsEnabled(cb.isSelected());
            }
        });

        labelsPresOpPanel.add(pollPeriodLabel);
        labelsPresOpPanel.add(subscribeExpiresLabel);

        valuesPresOpPanel.add(pollPeriodField);
        valuesPresOpPanel.add(subscribeExpiresField);

        buttonsPresOpPanel.add(enablePresOpButton);
        buttonsPresOpPanel.add(forceP2PPresOpButton);

        presenceOpPanel.add(buttonsPresOpPanel, BorderLayout.NORTH);
        presenceOpPanel.add(labelsPresOpPanel, BorderLayout.WEST);
        presenceOpPanel.add(valuesPresOpPanel, BorderLayout.CENTER);

        presenceOpPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.sipaccregwizz.PRESENCE_OPTIONS")));

        this.add(presenceOpPanel, BorderLayout.NORTH);
    }

    /**
     * Enables or disable all presence related options.
     *
     * @param isEnabled <code>true</code> to enable the presence related
     * options, <code>false</code> - to disable them.
     */
    void setPresenceOptionsEnabled(boolean isEnabled)
    {
        forceP2PPresOpButton.setEnabled(isEnabled);
        pollPeriodField.setEnabled(isEnabled);
        subscribeExpiresField.setEnabled(isEnabled);
    }

    /**
     * Indicates if the presence is enabled.
     * @return <tt>true</tt> if the presence is enabled, <tt>false</tt> -
     * otherwise
     */
    boolean isPresenceEnabled()
    {
        return enablePresOpButton.isSelected();
    }

    /**
     * Enables/disables the presence.
     * @param isPresenceEnabled <tt>true</tt> to enable the presence,
     * <tt>false</tt> - otherwise
     */
    void setPresenceEnabled(boolean isPresenceEnabled)
    {
        enablePresOpButton.setSelected(isPresenceEnabled);
    }

    /**
     * Indicates if the peer-to-peer presence mode is selected.
     * @return <tt>true</tt> if the peer-to-peer presence mode is selected,
     * <tt>false</tt> - otherwise.
     */
    boolean isForcePeerToPeerMode()
    {
        return forceP2PPresOpButton.isSelected();
    }

    /**
     * Enables/disables the peer-to-peer presence mode.
     * @param forceP2P <tt>true</tt> to select the peer-to-peer presence mode,
     * <tt>false</tt> - otherwise.
     */
    void setForcePeerToPeerMode(boolean forceP2P)
    {
        forceP2PPresOpButton.setSelected(forceP2P);
    }

    /**
     * Returns the poll period.
     * @return the poll period
     */
    String getPollPeriod()
    {
        return pollPeriodField.getText();
    }

    /**
     * Sets the poll period.
     * @param pollPeriod the poll period
     */
    void setPollPeriod(String pollPeriod)
    {
        pollPeriodField.setText(pollPeriod);
    }

    /**
     * Returns the subscription expiration information.
     * @return the subscription expiration information
     */
    String getSubscriptionExpiration()
    {
        return subscribeExpiresField.getText();
    }

    /**
     * Sets the subscription expiration information.
     * @param subscExp the subscription expiration information
     */
    void setSubscriptionExpiration(String subscExp)
    {
        subscribeExpiresField.setText(subscExp);
    }
}
