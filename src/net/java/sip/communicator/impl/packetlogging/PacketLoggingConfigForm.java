/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.packetlogging;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The Packet Logging Service configuration form.
 * @author Damian Minkov
 */
public class PacketLoggingConfigForm
    extends TransparentPanel
    implements ActionListener,
        DocumentListener
{
    /**
     * The enable packet logging check box.
     */
    private JCheckBox enableCheckBox;

    /**
     * Check box to enable/disable packet debug of sip protocol.
     */
    private JCheckBox sipProtocolCheckBox;

    /**
     * Check box to enable/disable packet debug of jabber protocol.
     */
    private JCheckBox jabberProtocolCheckBox;

    /**
     * Check box to enable/disable packet debug of media protocol/RTP.
     */
    private JCheckBox rtpProtocolCheckBox;

    /**
     * Check box to enable/disable packet debug of Ice4J.
     */
    private JCheckBox ice4jProtocolCheckBox;

    /**
     * The file count label.
     */
    private JLabel fileCountLabel;

    /**
     * The filed for file count value.
     */
    private JTextField fileCountField = new JTextField();

    /**
     * The file size label.
     */
    private JLabel fileSizeLabel;

    /**
     * The filed for file size value.
     */
    private JTextField fileSizeField = new JTextField();


    /**
     * Creates Packet Logging Config form.
     */
    public PacketLoggingConfigForm()
    {
        super(new BorderLayout());

        init();
        loadValues();
    }

    /**
     * Creating the configuration form
     */
    private void init()
    {
        ResourceManagementService resources =
                PacketLoggingActivator.getResourceService();

        enableCheckBox = new SIPCommCheckBox(
            resources.getI18NString("impl.packetlogging.ENABLE_DISABLE"));
        enableCheckBox.addActionListener(this);

        sipProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.sipaccregwizz.PROTOCOL_NAME"));
        sipProtocolCheckBox.addActionListener(this);

        jabberProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.jabberaccregwizz.PROTOCOL_NAME"));
        jabberProtocolCheckBox.addActionListener(this);

        String rtpDescription = resources.getI18NString(
            "impl.packetlogging.PACKET_LOGGING_RTP_DESCRIPTION");
        rtpProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("impl.packetlogging.PACKET_LOGGING_RTP")
            + " " + rtpDescription);
        rtpProtocolCheckBox.addActionListener(this);
        rtpProtocolCheckBox.setToolTipText(rtpDescription);

        ice4jProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("impl.packetlogging.PACKET_LOGGING_ICE4J"));
        ice4jProtocolCheckBox.addActionListener(this);

        JPanel mainPanel = new TransparentPanel();

        add(mainPanel, BorderLayout.NORTH);

        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        enableCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(enableCheckBox, c);

        String label = resources.getI18NString(
                "impl.packetlogging.PACKET_LOGGING_DESCRIPTION");
        JLabel descriptionLabel = new JLabel(label);
        descriptionLabel.setToolTipText(label);
        enableCheckBox.setToolTipText(label);
        descriptionLabel.setForeground(Color.GRAY);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(8));
        c.gridy = 1;
        c.insets = new Insets(0, 25, 10, 0);
        mainPanel.add(descriptionLabel, c);

        final JPanel loggersButtonPanel
            = new TransparentPanel(new GridLayout(0, 1));

        loggersButtonPanel.setBorder(BorderFactory.createTitledBorder(
            resources.getI18NString("service.gui.PROTOCOL")));

        loggersButtonPanel.add(sipProtocolCheckBox);
        loggersButtonPanel.add(jabberProtocolCheckBox);
        loggersButtonPanel.add(rtpProtocolCheckBox);
        loggersButtonPanel.add(ice4jProtocolCheckBox);

        c.insets = new Insets(0, 20, 10, 0);
        c.gridy = 2;
        mainPanel.add(loggersButtonPanel, c);

        final JPanel advancedPanel
            = new TransparentPanel(new GridLayout(0, 2));

        advancedPanel.setBorder(BorderFactory.createTitledBorder(
            resources.getI18NString("service.gui.ADVANCED")));

        fileCountField.getDocument().addDocumentListener(this);
        fileSizeField.getDocument().addDocumentListener(this);

        fileCountLabel = new JLabel(resources.getI18NString(
                "impl.packetlogging.PACKET_LOGGING_FILE_COUNT"));
        advancedPanel.add(fileCountLabel);
        advancedPanel.add(fileCountField);
        fileSizeLabel = new JLabel(resources.getI18NString(
                "impl.packetlogging.PACKET_LOGGING_FILE_SIZE"));
        advancedPanel.add(fileSizeLabel);
        advancedPanel.add(fileSizeField);

        c.gridy = 3;
        mainPanel.add(advancedPanel, c);
        
    }

    /**
     * Loading the values stored into configuration form
     */
    private void loadValues()
    {
        enableCheckBox.setSelected(
                PacketLoggingActivator.isGlobalLoggingEnabled());
        sipProtocolCheckBox.setSelected(
                PacketLoggingActivator.isSipLoggingEnabled());
        jabberProtocolCheckBox.setSelected(
                PacketLoggingActivator.isJabberLoggingEnabled());
        rtpProtocolCheckBox.setSelected(
                PacketLoggingActivator.isRTPLoggingEnabled());
        ice4jProtocolCheckBox.setSelected(
                PacketLoggingActivator.isIce4JLoggingEnabled());
        fileCountField.setText(String.valueOf(PacketLoggingActivator
                .getPacketLoggingService().getLogfileCount()));
        fileSizeField.setText(String.valueOf(PacketLoggingActivator
                .getPacketLoggingService().getLimit()/1000));

        updateButtonsState();
    }

    /**
     * Update button enable/disable state according enableCheckBox.
     */
    private void updateButtonsState()
    {
        sipProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        jabberProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        rtpProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        ice4jProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        fileCountField.setEnabled(enableCheckBox.isSelected());
        fileSizeField.setEnabled(enableCheckBox.isSelected());
        fileSizeLabel.setEnabled(enableCheckBox.isSelected());
        fileCountLabel.setEnabled(enableCheckBox.isSelected());
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if(source.equals(enableCheckBox))
        {
            // turn it on/off in activator
            PacketLoggingActivator.setGlobalLoggingEnabled(
                    enableCheckBox.isSelected());
            updateButtonsState();
        }
        else if(source.equals(sipProtocolCheckBox))
        {
            PacketLoggingActivator.setSipLoggingEnabled(
                    sipProtocolCheckBox.isSelected());
        }
        else if(source.equals(jabberProtocolCheckBox))
        {
            PacketLoggingActivator.setJabberLoggingEnabled(
                    jabberProtocolCheckBox.isSelected());
        }
        else if(source.equals(rtpProtocolCheckBox))
        {
            PacketLoggingActivator.setRTPLoggingEnabled(
                    rtpProtocolCheckBox.isSelected());
        }
        else if(source.equals(ice4jProtocolCheckBox))
        {
            PacketLoggingActivator.setIce4JLoggingEnabled(
                    ice4jProtocolCheckBox.isSelected());
        }
    }

    /**
     * Gives notification that there was an insert into the document.  The
     * range given by the DocumentEvent bounds the freshly inserted region.
     *
     * @param e the document event
     */
    public void insertUpdate(DocumentEvent e)
    {
        documentChanged(e);
    }

    /**
     * Gives notification that a portion of the document has been
     * removed.  The range is given in terms of what the view last
     * saw (that is, before updating sticky positions).
     *
     * @param e the document event
     */
    public void removeUpdate(DocumentEvent e)
    {
        documentChanged(e);
    }

    /**
     * Not used.
     *
     * @param e the document event
     */
    public void changedUpdate(DocumentEvent e)
    {}

    /**
     * A change in the text fields.
     * @param e the document event.
     */
    private void documentChanged(DocumentEvent e)
    {
        if(e.getDocument().equals(fileCountField.getDocument()))
        {
            // set file count only if its un integer
            try
            {
                int newFileCount = Integer.valueOf(fileCountField.getText());
                fileCountField.setForeground(Color.black);
                PacketLoggingActivator.getPacketLoggingService()
                        .setLogfileCount(newFileCount);
            }
            catch(Throwable t)
            {
                fileCountField.setForeground(Color.red);
            }
        }
        else if(e.getDocument().equals(fileSizeField.getDocument()))
        {
            // set file size only if its un integer
            try
            {
                int newFileSize = Integer.valueOf(fileSizeField.getText());
                fileSizeField.setForeground(Color.black);
                PacketLoggingActivator.getPacketLoggingService()
                        .setLimit(newFileSize * 1000);
            }
            catch(Throwable t)
            {
                fileSizeField.setForeground(Color.red);
            }
        }
    }
}
