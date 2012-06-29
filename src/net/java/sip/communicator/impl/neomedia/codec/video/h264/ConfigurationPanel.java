/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video.h264;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.impl.neomedia.codec.video.h264.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.control.*;
import org.jitsi.service.resources.*;

/**
 * Implements the H.264 configuration form (panel).
 *
 * @author Lyubomir Marinov
 */
public class ConfigurationPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private final static long serialVersionUID = 0L;

    /**
     * Initializer a new <tt>ConfigurationPanel</tt> instance.
     */
    public ConfigurationPanel()
    {
        /* Create the UI components. */
        super(new FlowLayout());

        TransparentPanel contentPanel
            = new TransparentPanel(new GridBagLayout());
        add(contentPanel);

        ResourceManagementService resources = NeomediaActivator.getResources();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JLabel defaultProfileLabel
            = new JLabel(
                    resources.getI18NString(
                            "impl.neomedia.configform.H264.defaultProfile"));
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        contentPanel.add(defaultProfileLabel, gridBagConstraints);

        JComboBox defaultProfileComboBox = new JComboBox();
        defaultProfileComboBox.setEditable(false);
        defaultProfileComboBox.addItem(
                new NameValuePair(
                        resources.getI18NString(
                                "impl.neomedia.configform.H264.defaultProfile."
                                    + JNIEncoder.BASELINE_PROFILE),
                        JNIEncoder.BASELINE_PROFILE));
        defaultProfileComboBox.addItem(
                new NameValuePair(
                        resources.getI18NString(
                                "impl.neomedia.configform.H264.defaultProfile."
                                    + JNIEncoder.MAIN_PROFILE),
                        JNIEncoder.MAIN_PROFILE));
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        contentPanel.add(defaultProfileComboBox, gridBagConstraints);

        JLabel preferredKeyFrameRequesterLabel
            = new JLabel(
                    resources.getI18NString(
                            "impl.neomedia.configform.H264"
                                + ".preferredKeyFrameRequester"));
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        contentPanel.add(preferredKeyFrameRequesterLabel, gridBagConstraints);

        JComboBox preferredKeyFrameRequesterComboBox = new JComboBox();
        preferredKeyFrameRequesterComboBox.setEditable(false);
        preferredKeyFrameRequesterComboBox.addItem(
                new NameValuePair(
                        resources.getI18NString(
                                "impl.neomedia.configform.H264"
                                    + ".preferredKeyFrameRequester."
                                    + KeyFrameControl.KeyFrameRequester.RTCP),
                        KeyFrameControl.KeyFrameRequester.RTCP));
        preferredKeyFrameRequesterComboBox.addItem(
                new NameValuePair(
                        resources.getI18NString(
                                "impl.neomedia.configform.H264"
                                    + ".preferredKeyFrameRequester."
                                    + KeyFrameControl.KeyFrameRequester.SIGNALING),
                        KeyFrameControl.KeyFrameRequester.SIGNALING));
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        contentPanel.add(
                preferredKeyFrameRequesterComboBox,
                gridBagConstraints);

        /*
         * Load the values from the ConfigurationService into the UI components.
         */
        ConfigurationService configuration
            = NeomediaActivator.getConfigurationService();

        setSelectedNameValuePair(
                defaultProfileComboBox,
                configuration.getString(
                        JNIEncoder.DEFAULT_PROFILE_PNAME,
                        JNIEncoder.DEFAULT_DEFAULT_PROFILE));
        addActionListener(
                defaultProfileComboBox,
                JNIEncoder.DEFAULT_PROFILE_PNAME);

        setSelectedNameValuePair(
                preferredKeyFrameRequesterComboBox,
                configuration.getString(
                        KeyFrameControl.KeyFrameRequester.PREFERRED_PNAME,
                        KeyFrameControl.KeyFrameRequester.DEFAULT_PREFERRED));
        addActionListener(
                preferredKeyFrameRequesterComboBox,
                KeyFrameControl.KeyFrameRequester.PREFERRED_PNAME);
    }

    /**
     * Adds an <tt>ActionListener</tt> to a specific <tt>JComboBox</tt>
     * populated with <tt>NameValuePair</tt>s which sets the value of a specific
     * <tt>ConfigurationService</tt> property to the <tt>value</tt> of the
     * selected <tt>NameValuePair</tt> of the <tt>comboBox</tt>.
     *
     * @param comboBox the <tt>JComboBox</tt> to add an <tt>ActionListener</tt>
     * to
     * @param property the name of the <tt>ConfigurationService</tt> property
     * to set the value of
     */
    private void addActionListener(
            final JComboBox comboBox,
            final String property)
    {
        comboBox.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        NameValuePair nameValuePair
                            = (NameValuePair) comboBox.getSelectedItem();

                        if (nameValuePair != null)
                        {
                            NeomediaActivator.getConfigurationService()
                                    .setProperty(property, nameValuePair.value);
                        }
                    }
                });
    }

    /**
     * Sets the selected item in a specific <tt>JComboBox</tt> populated with
     * <tt>NameValuePair</tt>s to the one which has a specific <tt>value</tt>.
     *
     * @param comboBox the <tt>JComboBox</tt> to set the selected item of
     * @param value the value of the <tt>NameValuePair</tt> to set as the
     * selected item of <tt>comboBox</tt>
     */
    private void setSelectedNameValuePair(JComboBox comboBox, String value)
    {
        int itemCount = comboBox.getItemCount();

        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++)
        {
            NameValuePair nameValuePair
                = (NameValuePair) comboBox.getItemAt(itemIndex);

            if (nameValuePair.value.equals(value))
            {
                comboBox.setSelectedIndex(itemIndex);
                break;
            }
        }
    }

    /**
     * Represents a <tt>String</tt> value which has a human-readable name
     * associated with it for display purposes.
     */
    private static class NameValuePair
    {
        /**
         * The human-readable name of this <tt>NameValuePair</tt>.
         */
        public final String name;

        /**
         * The <tt>String</tt> value represented by this <tt>NameValuePair</tt>.
         */
        public final String value;

        /**
         * Initializes a new <tt>NameValuePair</tt> which is to represent a
         * specific <tt>String</tt> <tt>value</tt> which is to be displayed to
         * the user as <tt>name</tt>.
         *
         * @param name the human-readable name of the new instance
         * @param value the <tt>String</tt> value to be represented by the new
         * instance
         */
        public NameValuePair(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns a human-readable representation of this <tt>Object</tt> i.e.
         * the name of this <tt>NameValuePair</tt>.
         *
         * @return a human-readable representation of this <tt>Object</tt> i.e.
         * the name of this <tt>NameValuePair</tt>
         */
        @Override
        public String toString()
        {
            return name;
        }
    }
}
