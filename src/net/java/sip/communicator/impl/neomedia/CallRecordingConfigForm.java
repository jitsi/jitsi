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
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.impl.neomedia.recording.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.recording.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * The saved calls management and configuration form.
 *
 * @author Dmitri Melnikov
 */
public class CallRecordingConfigForm
    extends TransparentPanel
    implements ActionListener,
               DocumentListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallRecordingConfigForm</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallRecordingConfigForm.class);

    /**
     * The resource service.
     */
    private static final ResourceManagementService resources
        = NeomediaActivator.getResources();

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * UI components.
     */
    private JButton callDirChooseButton;

    private JTextField callDirTextField;

    /**
     * Directory choose dialog.
     */
    private final SipCommFileChooser dirChooser;
    private JComboBox formatsComboBox;
    private JCheckBox saveCallsToCheckBox;
    /**
     * Directory where calls are stored. Default is SC_HOME/calls.
     */
    private String savedCallsDir;

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

        dirChooser
            = GenericFileDialog.create(
                    null,
                    resources.getI18NString(
                            "plugin.callrecordingconfig.CHOOSE_DIR"),
                    SipCommFileChooser.LOAD_FILE_OPERATION);
        dirChooser.setSelectionMode(SipCommFileChooser.DIRECTORIES_ONLY);
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
                                .getDefaultDownloadDirectory(),
                        true);
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
            changeCallsDir(newDir, true);
        }
        else if (source == callDirTextField)
        {
            File newDir = new File(callDirTextField.getText());
            changeCallsDir(newDir, true);
        }
    }

    /**
     * Sets the new directory for the saved calls to <tt>dir</tt>.
     *
     * @param dir the new chosen directory
     * @param changeCallDirTextField whether we will set the directory
     * path in callDirTextField.
     * @return <tt>true</tt> if directory was changed successfully,
     *         <tt>false</tt> otherwise
     */
    private boolean changeCallsDir(File dir, boolean changeCallDirTextField)
    {
        if (dir != null && dir.isDirectory())
        {
            savedCallsDir = dir.getAbsolutePath();
            if(changeCallDirTextField)
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

    /**
     * Not used.
     *
     * @param e the document event
     */
    public void changedUpdate(DocumentEvent e){}

    /**
     * Creates a combo box with supported audio formats.
     *
     * @return a combo box with supported audio formats
     */
    private Component createFormatsComboBox()
    {
        ComboBoxModel formatsComboBoxModel
            = new DefaultComboBoxModel(RecorderImpl.SUPPORTED_FORMATS);

        formatsComboBox = new JComboBox();
        formatsComboBox.setPreferredSize(new Dimension(200, 30));
        formatsComboBox.setModel(formatsComboBoxModel);

        formatsComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent event)
            {
                if (event.getStateChange() == ItemEvent.SELECTED)
                {
                    NeomediaActivator
                        .getConfigurationService()
                            .setProperty(Recorder.FORMAT, event.getItem());
                }
            }
        });
        return formatsComboBox;
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

        // saved calls directory panel
        JPanel callDirPanel = new TransparentPanel(new BorderLayout());

        callDirTextField = new JTextField();
        callDirTextField.addActionListener(this);
        callDirPanel.add(callDirTextField);

        callDirChooseButton
            = new JButton(
                    new ImageIcon(
                            resources.getImageInBytes(
                                    "plugin.notificationconfig.FOLDER_ICON")));

        callDirChooseButton.addActionListener(this);
        callDirPanel.add(callDirChooseButton, BorderLayout.EAST);

        // values panel
        JPanel valuesPanel = new TransparentPanel(new GridLayout(2, 1));

        valuesPanel.add(createFormatsComboBox());
        valuesPanel.add(callDirPanel);

        // main panel
        JPanel mainPanel = new TransparentPanel(new BorderLayout());

        mainPanel.add(labelsPanel, BorderLayout.WEST);
        mainPanel.add(valuesPanel, BorderLayout.CENTER);

        this.add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Gives notification that there was an insert into the document. The
     * range given by the DocumentEvent bounds the freshly inserted region.
     *
     * @param e the document event
     */
    public void insertUpdate(DocumentEvent e)
    {
        File insertedFile = new File(callDirTextField.getText());
        if(insertedFile.exists())
            changeCallsDir(insertedFile, false);
    }

    /**
     * Loads values from the configuration and sets the UI components to these
     * values.
     */
    private void loadValues()
    {
        ConfigurationService configuration
            = NeomediaActivator.getConfigurationService();
        String format = configuration.getString(Recorder.FORMAT);

        formatsComboBox.setSelectedItem(
                (format == null)
                    ? SoundFileUtils.DEFAULT_CALL_RECORDING_FORMAT
                    : format);

        savedCallsDir = configuration.getString(Recorder.SAVED_CALLS_PATH);
        saveCallsToCheckBox.setSelected(savedCallsDir != null);
        callDirTextField.setText(savedCallsDir);
        callDirTextField.setEnabled(saveCallsToCheckBox.isSelected());
        callDirTextField.getDocument().addDocumentListener(this);
        callDirChooseButton.setEnabled(saveCallsToCheckBox.isSelected());
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
        File insertedFile = new File(callDirTextField.getText());
        if(insertedFile.exists())
            changeCallsDir(insertedFile, false);
    }
}
