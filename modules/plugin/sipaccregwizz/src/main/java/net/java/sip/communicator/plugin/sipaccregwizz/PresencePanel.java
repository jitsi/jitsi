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
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.sip.*;

/**
 * The <tt>PresencePanel</tt> is the one containing presence information.
 *
 * @author Yana Stamcheva
 * @author Grigorii Balutsel
 * @author Damian Minkov
 */
public class PresencePanel
        extends TransparentPanel
        implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
            = new JTextField(SipAccountID.getDefaultStr(
                    ProtocolProviderFactory.POLLING_PERIOD));

    private JTextField subscribeExpiresField
            = new JTextField(SipAccountID.getDefaultStr(
                    ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION));

    private JTextField clistOptionServerUriValue = new JTextField();

    private JTextField clistOptionUserValue = new JTextField();

    private JPasswordField clistOptionPasswordValue = new JPasswordField();

    private final JCheckBox clistOptionUseSipCredentialsBox;

    private String[] contactlistOptions = new String[]
            { "Local", "XCAP", "XiVO" };

    private JComboBox contactlistOptionsCombo = new JComboBox(
            contactlistOptions);

    /**
     * Creates an instance of <tt>PresencePanel</tt>.
     * @param regform the parent registration form
     */
    public PresencePanel(SIPAccountRegistrationForm regform)
    {
        super(new BorderLayout(10, 10));

        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.enablePresOpButton = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.ENABLE_PRESENCE"),
            regform.getRegistration().isEnablePresence());
        this.forceP2PPresOpButton = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.FORCE_P2P_PRESENCE"),
            regform.getRegistration().isForceP2PMode());
        this.clistOptionUseSipCredentialsBox = new SIPCommCheckBox(
            Resources.getString("plugin.sipaccregwizz.XCAP_USE_SIP_CREDENTIALS"),
            regform.getRegistration().isClistOptionUseSipCredentials());

        if(regform.getRegistration().isXCapEnable())
            this.clistOptionServerUriValue.setText(
                    regform.getRegistration().getClistOptionServerUri());

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

        clistOptionUseSipCredentialsBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                JCheckBox checkBox = (JCheckBox) evt.getSource();
                setClistOptionUseSipCredentialsEnabled(checkBox.isSelected());
            }
        });

        JPanel xCapServerUriPanel
                = new TransparentPanel(new BorderLayout());
        xCapServerUriPanel.add(
            new JLabel(
                Resources.getString("plugin.sipaccregwizz.XCAP_SERVER_URI")),
            BorderLayout.WEST);

        xCapServerUriPanel.add(clistOptionServerUriValue, BorderLayout.CENTER);
        String xcapUri = regform.getRegistration().getClistOptionServerUri();
        if (xcapUri != null)
            clistOptionServerUriValue.setText(xcapUri);

        JPanel xCapPanel
                = new TransparentPanel(new BorderLayout(5, 5));
        JPanel xCapButtonsPanel = new TransparentPanel();
        xCapButtonsPanel.setLayout(
            new BoxLayout(xCapButtonsPanel, BoxLayout.Y_AXIS));
        JPanel xCapCredentialsLabelsPanel
                = new TransparentPanel(new GridLayout(0, 1, 0, 0));
        JPanel xCapCredentialsValuesPanel
                = new TransparentPanel(new GridLayout(0, 1, 0, 0));

        JPanel contactlistTypePanel
                = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        contactlistTypePanel.add(new JLabel(
                Resources.getString("plugin.sipaccregwizz.CLIST_TYPE")));
        contactlistTypePanel.add(contactlistOptionsCombo);

        contactlistOptionsCombo.addActionListener(this);

        if(regform.getRegistration().isXCapEnable())
        {
            setXCapEnable(true);
        }
        else if(regform.getRegistration().isXiVOEnable())
        {
            setXiVOEnable(true);
        }

        updateContactListPanel();

        xCapServerUriPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 16, 0, 0));

        xCapButtonsPanel.add(contactlistTypePanel);
        xCapButtonsPanel.add(xCapServerUriPanel);
        JPanel credPanel = new TransparentPanel(new BorderLayout());
        credPanel.add(clistOptionUseSipCredentialsBox, BorderLayout.WEST);
        credPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 7, 0, 0));
        xCapButtonsPanel.add(credPanel);

        xCapCredentialsLabelsPanel.add(new JLabel(
                Resources.getString("plugin.sipaccregwizz.XCAP_USER")));
        xCapCredentialsLabelsPanel.add(new JLabel(
                Resources.getString("plugin.sipaccregwizz.XCAP_PASSWORD")));

        xCapCredentialsValuesPanel.add(clistOptionUserValue);
        xCapCredentialsValuesPanel.add(clistOptionPasswordValue);

        xCapCredentialsLabelsPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 16, 0, 0));

        xCapPanel.add(xCapButtonsPanel, BorderLayout.NORTH);
        xCapPanel.add(xCapCredentialsLabelsPanel, BorderLayout.WEST);
        xCapPanel.add(xCapCredentialsValuesPanel, BorderLayout.CENTER);

        xCapPanel.setBorder(BorderFactory.createTitledBorder(
                Resources.getString("plugin.sipaccregwizz.XCAP_OPTIONS")));

        mainPanel.add(presenceOpPanel);
        mainPanel.add(xCapPanel);

        this.add(mainPanel, BorderLayout.NORTH);
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
     * Enables or disable contact list credentials related options.
     *
     * @param isEnabled <code>true</code> to enable the credentials related
     *                  options, <code>false</code> - to disable them.
     */
    void setClistOptionUseSipCredentialsEnabled(boolean isEnabled)
    {
        clistOptionUserValue.setEnabled(!isEnabled);
        clistOptionPasswordValue.setEnabled(!isEnabled);
    }

    /**
     * Enables or disable contact list related options.
     *
     * @param isEnabled <code>true</code> to enable the clist options related
     *                  options, <code>false</code> - to disable them.
     */
    void setClistOptionEnableEnabled(boolean isEnabled)
    {
        clistOptionUseSipCredentialsBox.setEnabled(isEnabled);
        clistOptionServerUriValue.setEnabled(isEnabled);
        if(isEnabled)
        {
            setClistOptionUseSipCredentialsEnabled(
                clistOptionUseSipCredentialsBox.isSelected());
        }
        else
        {
            setClistOptionUseSipCredentialsEnabled(true);
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
        Object o = contactlistOptionsCombo.getSelectedItem();
        if(o != null && o.equals(contactlistOptions[1]))
            return true;

        return false;
    }

    /**
     * Sets if has to use its capabilities.
     *
     * @param xCapEnable if has to use its capabilities.
     */
    void setXCapEnable(boolean xCapEnable)
    {
        if(xCapEnable)
        {
            contactlistOptionsCombo.setSelectedItem(contactlistOptions[1]);
            updateContactListPanel();
        }
    }

    /**
     * Indicates if XCAP has to use its capabilities.
     *
     * @return <tt>true</tt> if XCAP has to use its capabilities,
     *         <tt>false</tt> - otherwise.
     */
    boolean isXiVOEnable()
    {
        Object o = contactlistOptionsCombo.getSelectedItem();
        if(o != null && o.equals(contactlistOptions[2]))
            return true;

        return false;
    }

    /**
     * Sets if has to use its capabilities.
     *
     * @param xivoEnable if has to use its capabilities.
     */
    void setXiVOEnable(boolean xivoEnable)
    {
        if(xivoEnable)
        {
            contactlistOptionsCombo.setSelectedItem(contactlistOptions[2]);
            updateContactListPanel();
        }
    }

    /**
     * Indicates if contact list has to use SIP account credentials.
     *
     * @return <tt>true</tt> if contact list has to use SIP account credentials,
     *         <tt>false</tt> - otherwise.
     */
    boolean isClistOptionUseSipCredentials()
    {
        return clistOptionUseSipCredentialsBox.isSelected();
    }

    /**
     * Sets if contact list has to use SIP account credentials.
     *
     * @param clistOptionUseSipCredentials if contact list
     * has to use SIP account credentials.
     */
    void setClistOptionUseSipCredentials(boolean clistOptionUseSipCredentials)
    {
        clistOptionUseSipCredentialsBox.setSelected(
                clistOptionUseSipCredentials);
    }

    /**
     * Gets the contact list server uri.
     *
     * @return the contact list server uri.
     */
    String getClistOptionServerUri()
    {
        return clistOptionServerUriValue.getText();
    }

    /**
     * Sets the contact list server uri.
     *
     * @param xCapServerUri the contact list server uri.
     */
    void setClistOptionServerUri(String xCapServerUri)
    {
        clistOptionServerUriValue.setText(xCapServerUri);
    }

    /**
     * Gets the contact list user.
     *
     * @return the contact list user.
     */
    String getClistOptionUser()
    {
        return clistOptionUserValue.getText();
    }

    /**
     * Sets the contact list user.
     *
     * @param clistOptionUser the contact list user.
     */
    void setClistOptionUser(String clistOptionUser)
    {
        clistOptionUserValue.setText(clistOptionUser);
    }

    /**
     * Gets the contact list password.
     *
     * @return the contact list password.
     */
    char[] getClistOptionPassword()
    {
        return clistOptionPasswordValue.getPassword();
    }

    /**
     * Sets the contact list password.
     *
     * @param xCapPassword the contact list password.
     */
    void setClistOptionPassword(String xCapPassword)
    {
        clistOptionPasswordValue.setText(xCapPassword);
    }

    public void actionPerformed(ActionEvent actionEvent)
    {
        updateContactListPanel();
    }

    /**
     * Updates panel states.
     */
    private void updateContactListPanel()
    {
        Object obj = contactlistOptionsCombo.getSelectedItem();

        if(obj == null || obj.equals(contactlistOptions[0]))
        {
            clistOptionUseSipCredentialsBox.setEnabled(false);
            clistOptionServerUriValue.setEnabled(false);
            clistOptionUserValue.setEnabled(false);
            clistOptionPasswordValue.setEnabled(false);
        }
        else
        {
            clistOptionUseSipCredentialsBox.setEnabled(true);
            clistOptionServerUriValue.setEnabled(true);
            setClistOptionUseSipCredentialsEnabled(
                clistOptionUseSipCredentialsBox.isSelected());
        }
    }

    /**
     * Reinits labels and combobox to default values.
     */
    public void reinit()
    {
        setClistOptionEnableEnabled(false);
        setClistOptionUseSipCredentials(false);
        setClistOptionUseSipCredentialsEnabled(false);
        setClistOptionServerUri(null);
        setClistOptionUser(null);
        setClistOptionPassword(null);
        contactlistOptionsCombo.setSelectedItem(contactlistOptions[0]);
    }
}
