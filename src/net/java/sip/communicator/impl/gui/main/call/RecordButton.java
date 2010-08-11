/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The button that starts/stops the call recording.
 * 
 * @author Dmitri Melnikov
 */
public class RecordButton
    extends AbstractCallToggleButton
{
    /**
     * Resource service.
     */
    private static ResourceManagementService resources
        = GuiActivator.getResources();

    /**
     * Configuration service.
     */
    private static ConfigurationService configurationService
        = GuiActivator.getConfigurationService();

    /**
     * The date format used in file names.
     */
    private static SimpleDateFormat format
        = new SimpleDateFormat("yyyy-MM-dd@HH.mm.ss");

    /**
     * <tt>true</tt> when the default directory to save calls to is set,
     * <tt>false</tt> otherwise.
     */
    private boolean isCallDirSet = false;

    /**
     * The full filename of the saved call on the file system.
     */
    private String callFilename;

    /**
     * Input panel.
     */
    private InputPanel inputPanel;

    /**
     * Initializes a new <tt>RecordButton</tt> instance which is to record the
     * audio stream.
     * 
     * @param call the <tt>Call</tt> to be associated with the new instance and
     *            to have the audio stream recorded
     */
    public RecordButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>RecordButton</tt> instance which is to record the
     * audio stream.
     *
     * @param call the <tt>Call</tt> to be associated with the new instance and
     * to have its audio stream recorded
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public RecordButton(Call call, boolean fullScreen, boolean selected)
    {
        super(call, fullScreen, selected, ImageLoader.RECORD_BUTTON, null);

        inputPanel = new InputPanel();

        String toolTip
            = resources.getI18NString("service.gui.RECORD_BUTTON_TOOL_TIP");
        String saveDir
            = configurationService.getString(Recorder.SAVED_CALLS_PATH);
        if (saveDir != null)
        {
            isCallDirSet = true;
            toolTip = toolTip + " (" + saveDir + ")";
        }
        setToolTipText(toolTip);
    }

    /**
     * Starts/stops the recording of the call when this button is pressed.
     * 
     * @param evt the <tt>ActionEvent</tt> that notified us of the action
     */
    public void actionPerformed(ActionEvent evt)
    {
        if (call != null)
        {
            OperationSetBasicTelephony<?> telephony =
                call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

            boolean isRecordSelected = isSelected();
            // start recording
            if (isRecordSelected)
            {
                // ask user input about where to save the call
                if (!isCallDirSet)
                {
                    int status =
                        JOptionPane
                            .showConfirmDialog(
                                this,
                                inputPanel,
                                resources
                                    .getI18NString("plugin.callrecordingconfig.SAVE_CALL"),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                    if (status == JOptionPane.OK_OPTION)
                    {
                        callFilename = inputPanel.getSelectedFilename();
                        configurationService.setProperty(Recorder.CALL_FORMAT,
                            inputPanel.getSelectedFormat());
                    }
                    else
                    {
                        // user canceled the recording
                        setSelected(false);
                        return;
                    }
                }
                else
                    callFilename = createDefaultFilename();

                telephony.startRecording(call, callFilename);
            }
            // stop recording
            else
            {
                telephony.stopRecording(call);
                JOptionPane.showMessageDialog(this, 
                    resources.getI18NString(
                        "plugin.callrecordingconfig.CALL_SAVED_TO", new String[]
                        { callFilename }), 
                    resources
                        .getI18NString("plugin.callrecordingconfig.CALL_SAVED"),
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Creates a full filename for the call by combining the directory, file
     * prefix and extension. If the directory is <tt>null</tt> user's home
     * directory is used.
     * 
     * @return a full filename for the call
     */
    private String createDefaultFilename()
    {
        String callsDir
            = configurationService.getString(Recorder.SAVED_CALLS_PATH);

        // set to user's home when null
        if (callsDir == null)
        {
            try
            {
                callsDir
                    = GuiActivator
                        .getFileAccessService()
                            .getDefaultDownloadDirectory()
                                .getAbsolutePath();
            }
            catch (IOException ioex)
            {
                // Leave it in the current directory.
            }
        }

        String ext = configurationService.getString(Recorder.CALL_FORMAT);

        if (ext == null)
            ext = SoundFileUtils.mp2;

        return
            ((callsDir == null) ? "" : (callsDir + File.separator))
                + generateCallFilename(ext);
    }

    /**
     * Generates a file name for the call based on the current date.
     * 
     * @param ext file extension
     * @return the file name for the call
     */
    private String generateCallFilename(String ext)
    {
        return format.format(new Date()) + "-confcall." + ext;
    }

    private static class InputPanel
        extends TransparentPanel
    {
        /**
         * Call file chooser.
         */
        private SipCommFileChooser callFileChooser;

        /**
         * Selected file.
         */
        private String selectedFilename;

        /**
         * Format combo box.
         */
        private JComboBox formatComboBox;

        /**
         * Builds the panel.
         */
        public InputPanel()
        {
            super(new BorderLayout());

            initComponents();

            callFileChooser =
                GenericFileDialog.create(null, resources
                    .getI18NString("plugin.callrecordingconfig.SAVE_CALL"),
                    SipCommFileChooser.SAVE_FILE_OPERATION);
        }

        /**
         * Returns the selected file.
         * 
         * @return the selected file
         */
        public String getSelectedFilename()
        {
            return selectedFilename;
        }

        /**
         * Returns the selected format.
         * 
         * @return the selected format
         */
        public String getSelectedFormat()
        {
            return (String) formatComboBox.getSelectedItem();
        }

        /**
         * Initializes the UI components.
         */
        private void initComponents()
        {
            JPanel labelsPanel = new TransparentPanel(new GridLayout(2, 1));
            JLabel formatLabel =
                new JLabel(resources
                    .getI18NString("plugin.callrecordingconfig.FORMAT"));
            JLabel locationLabel =
                new JLabel(resources
                    .getI18NString("plugin.callrecordingconfig.LOCATION"));
            labelsPanel.add(formatLabel);
            labelsPanel.add(locationLabel);

            JPanel dirPanel =
                new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
            final JTextField callDirTextField = new JTextField();
            callDirTextField.setPreferredSize(new Dimension(200, 30));
            callDirTextField.setEditable(false);
            dirPanel.add(callDirTextField);
            JButton callDirChooseButton =
                new JButton(new ImageIcon(resources
                    .getImageInBytes("plugin.notificationconfig.FOLDER_ICON")));
            callDirChooseButton.setMinimumSize(new Dimension(30, 30));
            callDirChooseButton.setPreferredSize(new Dimension(30, 30));
            callDirChooseButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent arg0)
                {
                    File selectedFile = callFileChooser.getFileFromDialog();

                    if (selectedFile != null)
                    {
                        selectedFilename = selectedFile.getAbsolutePath();
                        callDirTextField.setText(selectedFilename);
                    }
                }
            });
            dirPanel.add(callDirChooseButton);

            JPanel comboPanel =
                new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel emptyLabel = new JLabel();
            emptyLabel.setPreferredSize(new Dimension(30, 30));
            comboPanel.add(createFormatsComboBox());
            comboPanel.add(emptyLabel);

            JPanel valuesPanel = new TransparentPanel(new GridLayout(2, 1));
            valuesPanel.add(comboPanel);
            valuesPanel.add(dirPanel);

            this.add(labelsPanel, BorderLayout.WEST);
            this.add(valuesPanel, BorderLayout.CENTER);
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

            formatComboBox = new JComboBox();
            formatComboBox.setPreferredSize(new Dimension(200, 30));
            formatComboBox.setModel(formatsComboBoxModel);
            return formatComboBox;
        }
    }
}
