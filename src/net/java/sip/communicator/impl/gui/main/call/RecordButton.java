/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

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
     * Configuration service.
     */
    private static final ConfigurationService configuration
        = GuiActivator.getConfigurationService();

    /**
     * Resource service.
     */
    private static final ResourceManagementService resources
        = GuiActivator.getResources();

    /**
     * The date format used in file names.
     */
    private static final SimpleDateFormat format
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
     * Call file chooser.
     */
    private final SipCommFileChooser callFileChooser;

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

        callFileChooser
            = GenericFileDialog.create(
                    null,
                    resources.getI18NString(
                            "plugin.callrecordingconfig.SAVE_CALL"),
                    SipCommFileChooser.SAVE_FILE_OPERATION);
        callFileChooser.addFilter(
                new SipCommFileFilter()
                {
                    public boolean accept(File f)
                    {
                        return
                            f.isDirectory() || SoundFileUtils.isRecordedCall(f);
                    }

                    public String getDescription()
                    {
                        return
                            "Recorded call (*.wav, *.mp2, *.gsm, *.au, *.aif)";
                    }
                });

        String toolTip
            = resources.getI18NString("service.gui.RECORD_BUTTON_TOOL_TIP");
        String saveDir = configuration.getString(Recorder.SAVED_CALLS_PATH);

        if ((saveDir != null) && (saveDir.length() != 0))
        {
            isCallDirSet = true;
            toolTip += " (" + saveDir + ")";
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
            OperationSetBasicTelephony<?> telephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            // start recording
            if (isSelected())
            {
                // ask user input about where to save the call
                if (!isCallDirSet)
                {
                    File selectedFile = callFileChooser.getFileFromDialog();

                    if (selectedFile != null)
                    {
                        callFilename = selectedFile.getAbsolutePath();

                        /*
                         * If the user specified no extension (which seems
                         * common on Mac OS X at least) i.e. no format, then it
                         * is not obvious that we have to override the set
                         * Recorder.CALL_FORMAT.
                         */
                        String callFormat
                            = SoundFileUtils.getExtension(selectedFile);

                        if (callFormat != null)
                        {
                            configuration.setProperty(
                                    Recorder.CALL_FORMAT,
                                    callFormat);
                        }
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
                NotificationManager.fireNotification(
                            NotificationManager.CALL_SAVED,
                            resources.getI18NString(
                                    "plugin.callrecordingconfig.CALL_SAVED"),
                            resources.getI18NString(
                                    "plugin.callrecordingconfig.CALL_SAVED_TO",
                                    new String[] { callFilename }));
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
        String callsDir = configuration.getString(Recorder.SAVED_CALLS_PATH);

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

        String ext = configuration.getString(Recorder.CALL_FORMAT);

        if ((ext == null) || (ext.length() == 0))
            ext = SoundFileUtils.DEFAULT_CALL_RECORDING_FORMAT;
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
}
