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
 * @author Grigorii Balutsel
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

    private final JCheckBox enablePresOpButton;

    private final JCheckBox forceP2PPresOpButton;

    private JLabel pollPeriodLabel = new JLabel(
            Resources.getString(
                    "plugin.sipaccregwizz.OFFLINE_CONTACT_POLLING_PERIOD"));

    private JLabel subscribeExpiresLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.SUBSCRIPTION_EXPIRATION"));

    private JTextField pollPeriodField
            = new JTextField(SIPAccountRegistration.DEFAULT_POLL_PERIOD);

    private JTextField subscribeExpiresField =
            new JTextField(SIPAccountRegistration.DEFAULT_SUBSCRIBE_EXPIRES);

    private JPanel xCapPanel
            = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel xCapButtonsPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel xCapCredetialsLabelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel xCapCredetialsValuesPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JLabel xCapServerUriLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.XCAP_SERVER_URI"));

    private JLabel xCapUserLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.XCAP_USER"));

    private JLabel xCapPasswordLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.XCAP_PASSWORD"));

    private JTextField xCapServerUriValue = new JTextField();

    private JTextField xCapUserValue = new JTextField();

    private JPasswordField xCapPasswordValue = new JPasswordField();

    private final JCheckBox xCapEnableBox;

    private final JCheckBox xCapUseSipCredetialsBox;

    /**
     * Creates an instance of <tt>PresencePanel</tt>.
     * @param regform the parent registration form
     */
    public PresencePanel(SIPAccountRegistrationForm regform)
    {
        super(new BorderLayout());

        this.enablePresOpButton = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.ENABLE_PRESENCE"),
            regform.getRegistration().isEnablePresence());
        this.forceP2PPresOpButton = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.FORCE_P2P_PRESENCE"),
            regform.getRegistration().isForceP2PMode());
        this.xCapEnableBox = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.XCAP_ENABLE"),
            regform.getRegistration().isXCapEnable());
        this.xCapUseSipCredetialsBox = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.XCAP_USE_SIP_CREDETIALS"),
            regform.getRegistration().isXCapUseSipCredetials());

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

        xCapEnableBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                JCheckBox checkBox = (JCheckBox) evt.getSource();
                setXCapEnableEnabled(checkBox.isSelected());
            }
        });

        xCapUseSipCredetialsBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                JCheckBox checkBox = (JCheckBox) evt.getSource();
                setXCapUseSipCredetialsEnabled(checkBox.isSelected());
            }
        });

        setXCapEnableEnabled(xCapEnableBox.isSelected());

        JPanel xCapServerUriPanel
                = new TransparentPanel(new BorderLayout(10, 10));
        xCapServerUriPanel.add(xCapServerUriLabel, BorderLayout.WEST);
        xCapServerUriPanel.add(xCapServerUriValue, BorderLayout.CENTER);

        xCapButtonsPanel.add(xCapEnableBox);
        xCapButtonsPanel.add(xCapServerUriPanel);
        xCapButtonsPanel.add(xCapUseSipCredetialsBox);

        xCapCredetialsLabelsPanel.add(xCapUserLabel);
        xCapCredetialsLabelsPanel.add(xCapPasswordLabel);

        xCapCredetialsValuesPanel.add(xCapUserValue);
        xCapCredetialsValuesPanel.add(xCapPasswordValue);

        xCapPanel.add(xCapButtonsPanel, BorderLayout.NORTH);
        xCapPanel.add(xCapCredetialsLabelsPanel, BorderLayout.WEST);
        xCapPanel.add(xCapCredetialsValuesPanel, BorderLayout.CENTER);

        xCapPanel.setBorder(BorderFactory.createTitledBorder(
                Resources.getString("plugin.sipaccregwizz.XCAP_OPTIONS")));

        this.add(presenceOpPanel, BorderLayout.NORTH);
        this.add(xCapPanel, BorderLayout.SOUTH);
    }

    /**
     * Enables or disable all presence related options.
     *
     * @param isEnabled <code>true</code> to enable the presence related
     *                  options, <code>false</code> - to disable them.
     */
    void setPresenceOptionsEnabled(boolean isEnabled)
    {
        forceP2PPresOpButton.setEnabled(isEnabled);
        pollPeriodField.setEnabled(isEnabled);
        subscribeExpiresField.setEnabled(isEnabled);
    }

    /**
     * Enables or disable XCAP credetials related options.
     *
     * @param isEnabled <code>true</code> to enable the credetials related
     *                  options, <code>false</code> - to disable them.
     */
    void setXCapUseSipCredetialsEnabled(boolean isEnabled)
    {
        xCapUserValue.setEnabled(!isEnabled);
        xCapPasswordValue.setEnabled(!isEnabled);
    }

    /**
     * Enables or disable XCAP related options.
     *
     * @param isEnabled <code>true</code> to enable the XCAP related
     *                  options, <code>false</code> - to disable them.
     */
    void setXCapEnableEnabled(boolean isEnabled)
    {
        xCapUseSipCredetialsBox.setEnabled(isEnabled);
        xCapServerUriValue.setEnabled(isEnabled);
        if(isEnabled)
        {
            setXCapUseSipCredetialsEnabled(xCapUseSipCredetialsBox.isSelected());
        }
        else
        {
            setXCapUseSipCredetialsEnabled(true);
        }
    }

    /**
     * Indicates if the presence is enabled.
     *
     * @return <tt>true</tt> if the presence is enabled, <tt>false</tt> -
     *         otherwise
     */
    boolean isPresenceEnabled()
    {
        return enablePresOpButton.isSelected();
    }

    /**
     * Enables/disables the presence.
     *
     * @param isPresenceEnabled <tt>true</tt> to enable the presence,
     *                          <tt>false</tt> - otherwise
     */
    void setPresenceEnabled(boolean isPresenceEnabled)
    {
        enablePresOpButton.setSelected(isPresenceEnabled);
    }

    /**
     * Indicates if the peer-to-peer presence mode is selected.
     *
     * @return <tt>true</tt> if the peer-to-peer presence mode is selected,
     *         <tt>false</tt> - otherwise.
     */
    boolean isForcePeerToPeerMode()
    {
        return forceP2PPresOpButton.isSelected();
    }

    /**
     * Enables/disables the peer-to-peer presence mode.
     *
     * @param forceP2P <tt>true</tt> to select the peer-to-peer presence mode,
     *                 <tt>false</tt> - otherwise.
     */
    void setForcePeerToPeerMode(boolean forceP2P)
    {
        forceP2PPresOpButton.setSelected(forceP2P);
    }

    /**
     * Returns the poll period.
     *
     * @return the poll period
     */
    String getPollPeriod()
    {
        return pollPeriodField.getText();
    }

    /**
     * Sets the poll period.
     *
     * @param pollPeriod the poll period
     */
    void setPollPeriod(String pollPeriod)
    {
        pollPeriodField.setText(pollPeriod);
    }

    /**
     * Returns the subscription expiration information.
     *
     * @return the subscription expiration information
     */
    String getSubscriptionExpiration()
    {
        return subscribeExpiresField.getText();
    }

    /**
     * Sets the subscription expiration information.
     *
     * @param subscExp the subscription expiration information
     */
    void setSubscriptionExpiration(String subscExp)
    {
        subscribeExpiresField.setText(subscExp);
    }

     /**
     * Indicates if XCAP has to use its capabilities.
     *
     * @return <tt>true</tt> if XCAP has to use its capabilities,
     *         <tt>false</tt> - otherwise.
     */
    boolean isXCapEnable()
    {
        return xCapEnableBox.isSelected();
    }

    /**
     * Sets if has to use its capabilities.
     *
     * @param xCapEnable if has to use its capabilities.
     */
    void setXCapEnable(boolean xCapEnable)
    {
        xCapEnableBox.setSelected(xCapEnable);
    }

    /**
     * Indicates if XCAP has to use SIP account credetials.
     *
     * @return <tt>true</tt> if XCAP has to use SIP account credetials,
     *         <tt>false</tt> - otherwise.
     */
    boolean isXCapUseSipCredetials()
    {
        return xCapUseSipCredetialsBox.isSelected();
    }

    /**
     * Sets if XCAP has to use SIP account credetials.
     *
     * @param xCapUseSipCredetials if XCAP has to use SIP account credetials.
     */
    void setXCapUseSipCredetials(boolean xCapUseSipCredetials)
    {
        xCapUseSipCredetialsBox.setSelected(xCapUseSipCredetials);
    }

    /**
     * Gets the XCAP server uri.
     *
     * @return the XCAP server uri.
     */
    String getXCapServerUri()
    {
        return xCapServerUriValue.getText();
    }

    /**
     * Sets the XCAP server uri.
     *
     * @param xCapServerUri the XCAP server uri.
     */
    void setXCapServerUri(String xCapServerUri)
    {
        xCapServerUriValue.setText(xCapServerUri);
    }

    /**
     * Gets the XCAP user.
     *
     * @return the XCAP user.
     */
    String getXCapUser()
    {
        return xCapUserValue.getText();
    }

    /**
     * Sets the XCAP user.
     *
     * @param xCapUser the XCAP user.
     */
    void setXCapUser(String xCapUser)
    {
        xCapUserValue.setText(xCapUser);
    }

    /**
     * Gets the XCAP password.
     *
     * @return the XCAP password.
     */
    char[] getXCapPassword()
    {
        return xCapPasswordValue.getPassword();
    }

    /**
     * Sets the XCAP password.
     *
     * @param xCapPassword the XCAP password.
     */
    void setXCapPassword(String xCapPassword)
    {
        xCapPasswordValue.setText(xCapPassword);
    }
}
