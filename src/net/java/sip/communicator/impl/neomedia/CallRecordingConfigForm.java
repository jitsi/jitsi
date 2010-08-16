/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The saved calls management and configuration form.
 *
 * @author Dmitri Melnikov
 */
public class CallRecordingConfigForm
    extends TransparentPanel 
    implements ActionListener
{
    /**
     * Logger for this class.
     */
    private final Logger logger
        = Logger.getLogger(CallRecordingConfigForm.class);
    
    /**
     * The resource service.
     */
    private static final ResourceManagementService resources
        = NeomediaActivator.getResources();
    
    /**
     * Directory where calls are stored. Default is SC_HOME/calls.
     */
    private String savedCallsDir;
    
    /**
     * Directory choose dialog.
     */
    private SipCommFileChooser dirChooser;
    
    /**
     * UI components.
     */
    private JButton callDirChooseButton;
    private JTextField callDirTextField;
    private JComboBox formatsComboBox;
    private JCheckBox saveCallsToCheckBox;
    
    /**
     * Creates an instance of the <tt>CallConfigurationPanel</tt>.
     * Checks for the <tt>SAVED_CALLS_PATH</tt> and sets it if it does not
     * exist.
     */
    public CallRecordingConfigForm()
    {
        super(new BorderLayout());
        
        initComponents();
        loadValues();

        dirChooser =
            GenericFileDialog.create(null, resources
                .getI18NString("plugin.callrecordingconfig.CHOOSE_DIR"),
                SipCommFileChooser.LOAD_FILE_OPERATION);
        ((JFileChooser) dirChooser)
            .setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

    /**
     * Loads values from the configuration and sets the UI components to these
     * values.
     */
    private void loadValues()
    {
        ConfigurationService configurationService
            = NeomediaActivator.getConfigurationService();

        String callFormat = configurationService.getString(Recorder.CALL_FORMAT);
        formatsComboBox.setSelectedItem(callFormat == null ? SoundFileUtils.mp2
            : callFormat);
        
        savedCallsDir = configurationService.getString(Recorder.SAVED_CALLS_PATH);
        saveCallsToCheckBox.setSelected(savedCallsDir != null);
        callDirTextField.setText(savedCallsDir);
        callDirTextField.setEnabled(saveCallsToCheckBox.isSelected());
        callDirChooseButton.setEnabled(saveCallsToCheckBox.isSelected());
    }

    /**
     * Creates a panel with call management components.
     */
    private void initComponents()
    {
        // labels panel
        JPanel labelsPanel = new TransparentPanel(new GridLayout(2, 1));        
        JLabel formatsLabel
            = new JLabel(
                    resources.getI18NString(
                            "plugin.callrecordingconfig.SUPPORTED_FORMATS"));

        saveCallsToCheckBox
            = new SIPCommCheckBox(
                    resources.getI18NString(
                            "plugin.callrecordingconfig.SAVE_CALLS"));
        saveCallsToCheckBox.addActionListener(this);

        labelsPanel.add(formatsLabel);
        labelsPanel.add(saveCallsToCheckBox);

        // combo box panel
        JPanel comboPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel emptyLabel = new JLabel();

        emptyLabel.setPreferredSize(new Dimension(30, 30));
        comboPanel.add(createFormatsComboBox());
        comboPanel.add(emptyLabel);

        // saved calls directory panel 
        JPanel callDirPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

        callDirTextField = new JTextField();
        callDirTextField.setPreferredSize(new Dimension(200, 30));
        callDirTextField.addActionListener(this);
        callDirPanel.add(callDirTextField);

        callDirChooseButton
            = new JButton(
                    new ImageIcon(
                            resources.getImageInBytes(
                                    "plugin.notificationconfig.FOLDER_ICON")));
        callDirChooseButton.setMinimumSize(new Dimension(30,30));
        callDirChooseButton.setPreferredSize(new Dimension(30,30));
        callDirChooseButton.addActionListener(this);
        callDirPanel.add(callDirChooseButton);

        // values panel
        JPanel valuesPanel = new TransparentPanel(new GridLayout(2, 1));

        valuesPanel.add(comboPanel);
        valuesPanel.add(callDirPanel);

        // main panel
        JPanel mainPanel = new TransparentPanel(new BorderLayout());

        mainPanel.add(labelsPanel, BorderLayout.WEST);
        mainPanel.add(valuesPanel, BorderLayout.CENTER);

        this.add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Creates a combo box with supported audio formats.
     * 
     * @return a combo box with supported audio formats
     */
    private Component createFormatsComboBox()
    {
        ComboBoxModel formatsComboBoxModel =
            new DefaultComboBoxModel(
                new String[] {
                    SoundFileUtils.mp2,
                    SoundFileUtils.wav,
                    SoundFileUtils.au,
                    SoundFileUtils.aif,
                    SoundFileUtils.gsm });
        
        formatsComboBox = new JComboBox();
        formatsComboBox.setPreferredSize(new Dimension(200, 30));
        formatsComboBox.setModel(formatsComboBoxModel);

        formatsComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent event)
            {
                if (event.getStateChange() == ItemEvent.SELECTED)
                    NeomediaActivator
                        .getConfigurationService()
                            .setProperty(Recorder.CALL_FORMAT, event.getItem());
            }
        });
        return formatsComboBox;
    }

    /**
     * Indicates that one of the contained in this panel components has
     * performed an action.
     * 
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        if (source == saveCallsToCheckBox)
        {
            boolean selected = saveCallsToCheckBox.isSelected();
            callDirTextField.setEnabled(selected);
            callDirChooseButton.setEnabled(selected);
            if (selected)
            {
                // set default directory
                try
                {
                    changeCallsDir(
                        NeomediaActivator
                            .getFileAccessService()
                                .getDefaultDownloadDirectory());
                }
                catch (IOException ioex)
                {
                }
            }
            else
            {
                // remove default directory prop
                NeomediaActivator
                    .getConfigurationService()
                        .setProperty(Recorder.SAVED_CALLS_PATH, null);
                callDirTextField.setText(null);
            }
        }
        else if (source == callDirChooseButton)
        {
            File newDir = dirChooser.getFileFromDialog();
            changeCallsDir(newDir);
        }
        else if (source == callDirTextField)
        {
            File newDir = new File(callDirTextField.getText());
            changeCallsDir(newDir);
        }
    }

    /**
     * Sets the new directory for the saved calls to <tt>dir</tt>.
     * 
     * @param dir the new chosen directory
     * @return <tt>true</tt> if directory was changed successfully,
     *         <tt>false</tt> otherwise
     */
    private boolean changeCallsDir(File dir)
    {
        if (dir != null && dir.isDirectory())
        {
            savedCallsDir = dir.getAbsolutePath();
            callDirTextField.setText(savedCallsDir);
            NeomediaActivator
                .getConfigurationService()
                    .setProperty(Recorder.SAVED_CALLS_PATH, savedCallsDir);
            
            if (logger.isDebugEnabled())
                logger.debug("Calls directory changed to " + savedCallsDir);
            return true;
        }
        else
        {
            if (logger.isDebugEnabled())
                logger.debug("Calls directory not changed.");
            return false;
        }
    }
}
