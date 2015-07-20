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
import java.util.concurrent.*;

import javax.media.*;
import javax.media.MediaException;
import javax.media.protocol.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.util.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.OSUtils;
import org.jitsi.util.swing.*;

/**
 * Implements <tt>MediaConfigurationService</tt> i.e. represents a factory of
 * user interface which allows the user to configure the media-related
 * functionality of the application.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Boris Grozev
 * @author Vincent Lucas
 */
public class MediaConfigurationImpl
    implements ActionListener,
               MediaConfigurationService
{
    /**
     * Creates a new listener to combo box and affect changes to the audio level
     * indicator. The level indicator is updated via a thread in order to avoid
     * deadlock of the user interface.
     */
    private class AudioLevelListenerThread
        implements ActionListener,
                   HierarchyListener
    {
        /**
         * Listener to update the audio level indicator.
         */
        private final SimpleAudioLevelListener audioLevelListener
            = new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    soundLevelIndicator.updateSoundLevel(level);
                }
            };

        /**
         * The audio system used to get and set the sound devices.
         */
        private AudioSystem audioSystem;

        /**
         * The combo box used to select the device the user wants to use.
         */
        private JComboBox comboBox;

        /**
         * The current capture device.
         */
        private AudioMediaDeviceSession deviceSession;

        /**
         * The new device chosen by the user and that we need to initialize as
         * the new capture device.
         */
        private AudioMediaDeviceSession deviceSessionToSet;

        /**
         * The indicator which determines whether
         * {@link #setDeviceSession(AudioMediaDeviceSession)} is to be invoked
         * when {@link #deviceSessionToSet} is <tt>null</tt>.
         */
        private boolean deviceSessionToSetIsNull;

        /**
         * The <tt>ExecutorService</tt> which is to asynchronously invoke
         * {@link #setDeviceSession(AudioMediaDeviceSession)} with
         * {@link #deviceSessionToSet}.
         */
        private final ExecutorService setDeviceSessionExecutor
            = Executors.newSingleThreadExecutor();

        private final Runnable setDeviceSessionTask
            = new Runnable()
            {
                public void run()
                {
                    AudioMediaDeviceSession deviceSession = null;
                    boolean deviceSessionIsNull = false;

                    synchronized (AudioLevelListenerThread.this)
                    {
                        if ((deviceSessionToSet != null)
                                || deviceSessionToSetIsNull)
                        {
                            /*
                             * Invoke #setDeviceSession(AudioMediaDeviceSession)
                             * outside the synchronized block to avoid a GUI
                             * deadlock.
                             */
                            deviceSession = deviceSessionToSet;
                            deviceSessionIsNull = deviceSessionToSetIsNull;
                            deviceSessionToSet = null;
                            deviceSessionToSetIsNull = false;
                        }
                    }

                    if ((deviceSession != null) || deviceSessionIsNull)
                    {
                        /*
                         * XXX The method blocks on Mac OS X for Bluetooth
                         * devices which are paired but disconnected.
                         */
                        setDeviceSession(deviceSession);
                    }
                }
            };

        /**
         * The sound level indicator used to show the effectiveness of the
         * capture device.
         */
        private SoundLevelIndicator soundLevelIndicator;

        /**
         *  Provides an handler which reads the stream into the
         *  "transferHandlerBuffer".
         */
        private final BufferTransferHandler transferHandler
            = new BufferTransferHandler()
            {
                public void transferData(PushBufferStream stream)
                {
                    try
                    {
                        stream.read(transferHandlerBuffer);
                    }
                    catch (IOException ioe)
                    {
                    }
                }
            };

        /**
         * The buffer used for reading the capture device.
         */
        private final Buffer transferHandlerBuffer = new Buffer();

        /**
         * Creates a new listener to combo box and affect changes to the audio
         * level indicator.
         *
         * @param audioSystem The audio system used to get and set the sound
         * devices.
         * @param comboBox The combo box used to select the device the user
         * wants to use.
         * @param soundLevelIndicator The sound level indicator used to show the
         * effectiveness of the capture device.
         */
        public AudioLevelListenerThread(
                AudioSystem audioSystem,
                JComboBox comboBox,
                SoundLevelIndicator soundLevelIndicator)
        {
            init(audioSystem, comboBox, soundLevelIndicator);
        }

        /**
         * Refresh combo box when the user click on it.
         *
         * @param ev The click on the combo box.
         */
        public void actionPerformed(ActionEvent ev)
        {
            synchronized (this)
            {
                deviceSessionToSet = null;
                deviceSessionToSetIsNull = true;
                setDeviceSessionExecutor.execute(setDeviceSessionTask);
            }

            CaptureDeviceInfo cdi;

            if (comboBox == null)
            {
                cdi
                    = soundLevelIndicator.isShowing()
                        ? audioSystem.getSelectedDevice(
                                AudioSystem.DataFlow.CAPTURE)
                        : null;
            }
            else
            {
                Object selectedItem
                    = soundLevelIndicator.isShowing()
                        ? comboBox.getSelectedItem()
                        : null;

                cdi
                    = (selectedItem
                            instanceof
                                DeviceConfigurationComboBoxModel.CaptureDevice)
                        ? ((DeviceConfigurationComboBoxModel.CaptureDevice)
                                selectedItem)
                            .info
                        : null;
            }

            if (cdi != null)
            {
                for (MediaDevice md: mediaService.getDevices(
                            MediaType.AUDIO,
                            MediaUseCase.ANY))
                {
                    if (md instanceof AudioMediaDeviceImpl)
                    {
                        AudioMediaDeviceImpl amd = (AudioMediaDeviceImpl) md;

                        if (cdi.equals(amd.getCaptureDeviceInfo()))
                        {
                            try
                            {
                                MediaDeviceSession deviceSession
                                    = amd.createSession();
                                boolean deviceSessionIsSet = false;

                                try
                                {
                                    if (deviceSession instanceof
                                            AudioMediaDeviceSession)
                                    {
                                        synchronized (this)
                                        {
                                            deviceSessionToSet
                                                = (AudioMediaDeviceSession)
                                                    deviceSession;
                                            deviceSessionToSetIsNull
                                                = (deviceSessionToSet == null);
                                            setDeviceSessionExecutor.execute(
                                                    setDeviceSessionTask);
                                        }
                                        deviceSessionIsSet = true;
                                    }
                                }
                                finally
                                {
                                    if (!deviceSessionIsSet)
                                        deviceSession.close();
                                }
                            }
                            catch (Throwable t)
                            {
                                if (t instanceof ThreadDeath)
                                    throw (ThreadDeath) t;
                            }
                            break;
                        }
                    }
                }
            }
        }

        public void hierarchyChanged(HierarchyEvent ev)
        {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                actionPerformed(null);
                            }
                        });
            }
        }

        /**
         * Creates a new listener to combo box and affect changes to the audio
         * level indicator.
         *
         * @param audioSystem The audio system used to get and set the sound
         * devices.
         * @param comboBox The combo box used to select the device the user
         * wants to use.
         * @param soundLevelIndicator The sound level indicator used to show the
         * effectiveness of the capture device.
         */
        public void init(
                AudioSystem audioSystem,
                JComboBox comboBox,
                SoundLevelIndicator soundLevelIndicator)
        {
            this.audioSystem = audioSystem;

            if (this.comboBox != comboBox)
            {
                if (this.comboBox != null)
                    this.comboBox.removeActionListener(this);
                this.comboBox = comboBox;
                if (comboBox != null)
                    comboBox.addActionListener(this);
            }

            if (this.soundLevelIndicator != soundLevelIndicator)
            {
                if (this.soundLevelIndicator != null)
                    this.soundLevelIndicator.removeHierarchyListener(this);
                this.soundLevelIndicator = soundLevelIndicator;
                if (soundLevelIndicator != null)
                    soundLevelIndicator.addHierarchyListener(this);
            }
        }

        /**
         * Sets the new capture device used by the audio level indicator.
         *
         * @param deviceSession The new capture device used by the audio level
         * indicator.
         */
        private void setDeviceSession(AudioMediaDeviceSession deviceSession)
        {
            if (this.deviceSession == deviceSession)
                return;

            if (this.deviceSession != null)
            {
                try
                {
                    this.deviceSession.close();
                }
                finally
                {
                    this.deviceSession.setLocalUserAudioLevelListener(null);
                    soundLevelIndicator.resetSoundLevel();
                }
            }

            this.deviceSession = deviceSession;

            if (deviceSession != null)
            {
                deviceSession.setContentDescriptor(
                        new ContentDescriptor(ContentDescriptor.RAW));
                deviceSession.setLocalUserAudioLevelListener(
                        audioLevelListener);

                deviceSession.start(MediaDirection.SENDONLY);

                try
                {
                    DataSource dataSource = deviceSession.getOutputDataSource();

                    dataSource.connect();

                    PushBufferStream[] streams
                        = ((PushBufferDataSource) dataSource).getStreams();

                    for (PushBufferStream stream : streams)
                        stream.setTransferHandler(transferHandler);

                    dataSource.start();
                }
                catch (Throwable t)
                {
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                }
            }
        }
    }

    /**
     * Renders the available resolutions in the combo box.
     */
    private static class ResolutionCellRenderer
        extends DefaultListCellRenderer
    {
        /**
         * The serialization version number of the
         * <tt>ResolutionCellRenderer</tt> class. Defined to the value of
         * <tt>0</tt> because the <tt>ResolutionCellRenderer</tt> instances do
         * not have state of their own.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Sets readable text describing the resolution if the selected
         * value is null we return the string "Auto".
         *
         * @param list
         * @param value
         * @param index
         * @param isSelected
         * @param cellHasFocus
         * @return Component
         */
        @Override
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            // call super to set backgrounds and fonts
            super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);

            // now just change the text
            if(value == null)
                setText("Auto");
            else if(value instanceof Dimension)
            {
                Dimension d = (Dimension)value;

                setText(((int) d.getWidth()) + "x" + ((int) d.getHeight()));
            }
            return this;
        }
    }

    /**
     * Wrapper for the device list field.
     */
    private static class DeviceComboBoxField
    {
        /**
         * The combo box with the devices.
         */
        private JComboBox deviceComboBox = null;

        /**
         * The <tt>JList</tt> with the devices.
         */
        private JList deviceList = null;

        /**
         * The current component that displays the list with the devices.
         */
        private Component deviceComponent;

        /**
         * The listener for the field.
         */
        private Listener listener;

        /**
         * Model for the field.
         */
        final DeviceConfigurationComboBoxModel model;

        /**
         * Constructs <tt>DeviceComboBoxField</tt> instance.
         * @param type the type of the configuration panel
         * @param devicePanel the container of the field.
         */
        public DeviceComboBoxField(final int type, Container devicePanel)
        {
            model = new DeviceConfigurationComboBoxModel(
                mediaService.getDeviceConfiguration(),
                type);

            if(!OSUtils.IS_WINDOWS
                || type != DeviceConfigurationComboBoxModel.VIDEO)
            {
                deviceComboBox = new JComboBox();
                deviceComboBox.setEditable(false);
                deviceComboBox.setModel(model);
                devicePanel.add(deviceComboBox);
                deviceComponent = deviceComboBox;
            }
            else
            {
                deviceList = new JList();
                deviceList.setModel(model);
                JScrollPane listScroller = new JScrollPane(deviceList);
                listScroller.setPreferredSize(new Dimension(200, 38));
                deviceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                deviceList.setLayoutOrientation(JList.VERTICAL);
                deviceList.setVisibleRowCount(-1);
                deviceList.setSelectedValue(model.getSelectedItem(), true);
                devicePanel.add(listScroller);
                deviceComponent = deviceList;
            }
        }

        /**
         * Returns the field component
         * @return the field component
         */
        public Component getComponent()
        {
            return deviceComponent;
        }

        /**
         * Returns the selected device
         * @return the selected device
         */
        public Object getSelectedItem()
        {
            return (deviceComboBox != null)?
                deviceComboBox.getSelectedItem() : deviceList.getSelectedValue();
        }

        /**
         * Adds a listener to the field.
         * @param listener the listener to be added.
         */
        public void addListener(final Listener listener)
        {
            this.listener = listener;
            if(deviceComboBox != null)
            {
                deviceComboBox.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        listener.onAction();
                    }
                });
            }
            else
            {
                deviceList.addListSelectionListener(new ListSelectionListener()
                {

                    @Override
                    public void valueChanged(ListSelectionEvent e)
                    {
                        model.setSelectedItem(deviceList.getSelectedValue());
                        listener.onAction();
                    }
                });
            }
        }

        /**
         * Interface for the listener attached to the field.
         */
        public static interface Listener
        {
            public void onAction();
        }
    }

    /**
     * Indicates if the Devices settings configuration tab
     * should be disabled, i.e. not visible to the user.
     */
    private static final String DEVICES_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.devicesconfig.DISABLED";

    /**
     * Indicates if the Audio/Video encodings configuration tab
     * should be disabled, i.e. not visible to the user.
     */
    private static final String ENCODINGS_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.encodingsconfig.DISABLED";

    /**
     * The <tt>Logger</tt> used by the <tt>MediaConfigurationServiceImpl</tt>
     * class for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaConfigurationImpl.class);

    /**
     * The <tt>MediaService</tt> implementation used by
     * <tt>MediaConfigurationImpl</tt>.
     */
    private static final MediaServiceImpl mediaService
        = NeomediaActivator.getMediaServiceImpl();

    /**
     * The name of the sound file used to test the playback and the notification
     * devices.
     */
    private static final String TEST_SOUND_FILENAME_PROP
        = "net.java.sip.communicator.impl.neomedia.TestSoundFilename";

    /**
     * Indicates if the Video/More Settings configuration tab
     * should be disabled, i.e. not visible to the user.
     */
    private static final String VIDEO_MORE_SETTINGS_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.videomoresettingsconfig.DISABLED";

    /**
     * The preferred width of all panels.
     */
    private final static int WIDTH = 350;

    /**
     * Creates the video advanced settings.
     *
     * @return video advanced settings panel.
     */
    private static Component createVideoAdvancedSettings()
    {
        ResourceManagementService resources = NeomediaActivator.getResources();

        final DeviceConfiguration deviceConfig =
            mediaService.getDeviceConfiguration();

        TransparentPanel centerPanel =
            new TransparentPanel(new GridBagLayout());
        centerPanel.setMaximumSize(new Dimension(WIDTH, 150));

        JButton resetDefaultsButton = new JButton(
            resources.getI18NString(
                    "impl.media.configform.VIDEO_RESET"));
        JPanel resetButtonPanel = new TransparentPanel(
                new FlowLayout(FlowLayout.RIGHT));
        resetButtonPanel.add(resetDefaultsButton);

        final JPanel centerAdvancedPanel
            = new TransparentPanel(new BorderLayout());
        centerAdvancedPanel.add(centerPanel, BorderLayout.NORTH);
        centerAdvancedPanel.add(resetButtonPanel, BorderLayout.SOUTH);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridy = 0;

        centerPanel.add(new JLabel(
            resources.getI18NString("impl.media.configform.VIDEO_RESOLUTION")),
            constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        final JCheckBox frameRateCheck = new SIPCommCheckBox(
            resources.getI18NString("impl.media.configform.VIDEO_FRAME_RATE"));
        centerPanel.add(frameRateCheck, constraints);
        constraints.gridy = 2;
        constraints.insets = new Insets(5, 5, 0, 0);
        centerPanel.add(new JLabel(
            resources.getI18NString(
                    "impl.media.configform.VIDEO_PACKETS_POLICY")),
            constraints);
        constraints.gridy = 3;
        centerPanel.add(new JLabel(
            resources.getI18NString(
                    "impl.media.configform.VIDEO_BITRATE")),
            constraints);

        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets = new Insets(5, 0, 0, 5);
        Object[] resolutionValues
            = new Object[DeviceConfiguration.SUPPORTED_RESOLUTIONS.length + 1];
        System.arraycopy(DeviceConfiguration.SUPPORTED_RESOLUTIONS, 0,
                        resolutionValues, 1,
                        DeviceConfiguration.SUPPORTED_RESOLUTIONS.length);
        final JComboBox sizeCombo = new JComboBox(resolutionValues);
        sizeCombo.setRenderer(new ResolutionCellRenderer());
        sizeCombo.setEditable(false);
        centerPanel.add(sizeCombo, constraints);

        // default value is 20
        final JSpinner frameRate = new JSpinner(new SpinnerNumberModel(
            20, 5, 30, 1));
        frameRate.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                deviceConfig.setFrameRate(
                        ((SpinnerNumberModel)frameRate.getModel())
                            .getNumber().intValue());
            }
        });
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 0, 5);
        centerPanel.add(frameRate, constraints);

        frameRateCheck.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(frameRateCheck.isSelected())
                {
                    deviceConfig.setFrameRate(
                        ((SpinnerNumberModel)frameRate.getModel())
                            .getNumber().intValue());
                }
                else // unlimited framerate
                    deviceConfig.setFrameRate(-1);

                frameRate.setEnabled(frameRateCheck.isSelected());
            }
        });

        int videoMaxBandwith = deviceConfig.getVideoRTPPacingThreshold();
        // Accord the current value with the maximum allowed value. Fixes
        // existing configurations that have been set to a number larger than
        // the advised maximum value.
        videoMaxBandwith = ((videoMaxBandwith > 999) ? 999 : videoMaxBandwith);

        final JSpinner videoMaxBandwidth = new JSpinner(new SpinnerNumberModel(
            videoMaxBandwith,
            1, 999, 1));
        videoMaxBandwidth.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                deviceConfig.setVideoRTPPacingThreshold(
                        ((SpinnerNumberModel) videoMaxBandwidth.getModel())
                                .getNumber().intValue());
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 5, 5);
        centerPanel.add(videoMaxBandwidth, constraints);

        final JSpinner videoBitrate = new JSpinner(new SpinnerNumberModel(
            deviceConfig.getVideoBitrate(),
            1, Integer.MAX_VALUE, 1));
        videoBitrate.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                deviceConfig.setVideoBitrate(
                        ((SpinnerNumberModel) videoBitrate.getModel())
                                .getNumber().intValue());
            }
        });
        constraints.gridy = 3;
        centerPanel.add(videoBitrate, constraints);

        resetDefaultsButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // reset to defaults
                sizeCombo.setSelectedIndex(0);
                frameRateCheck.setSelected(false);
                frameRate.setEnabled(false);
                frameRate.setValue(20);
                // unlimited framerate
                deviceConfig.setFrameRate(-1);
                videoMaxBandwidth.setValue(
                        DeviceConfiguration.DEFAULT_VIDEO_RTP_PACING_THRESHOLD);
                videoBitrate.setValue(
                        DeviceConfiguration.DEFAULT_VIDEO_BITRATE);
            }
        });

        // load selected value or auto
        Dimension videoSize = deviceConfig.getVideoSize();

        if((videoSize.getHeight() != DeviceConfiguration.DEFAULT_VIDEO_HEIGHT)
                && (videoSize.getWidth()
                        != DeviceConfiguration.DEFAULT_VIDEO_WIDTH))
            sizeCombo.setSelectedItem(deviceConfig.getVideoSize());
        else
            sizeCombo.setSelectedIndex(0);
        sizeCombo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Dimension selectedVideoSize
                    = (Dimension) sizeCombo.getSelectedItem();

                if(selectedVideoSize == null)
                {
                    // the auto value, default one
                    selectedVideoSize
                        = new Dimension(
                                DeviceConfiguration.DEFAULT_VIDEO_WIDTH,
                                DeviceConfiguration.DEFAULT_VIDEO_HEIGHT);
                }
                deviceConfig.setVideoSize(selectedVideoSize);
            }
        });

        frameRateCheck.setSelected(
            deviceConfig.getFrameRate()
                != DeviceConfiguration.DEFAULT_VIDEO_FRAMERATE);
        frameRate.setEnabled(frameRateCheck.isSelected());

        if(frameRate.isEnabled())
            frameRate.setValue(deviceConfig.getFrameRate());

        return centerAdvancedPanel;
    }

    /**
     * Creates the video container.
     * @param noVideoComponent the container component.
     * @return the video container.
     */
    private static JComponent createVideoContainer(Component noVideoComponent)
    {
        return new VideoContainer(noVideoComponent, false);
    }

    /**
     * Creates preview for the (video) device in the video container.
     *
     * @param device the device
     * @param videoContainer the video container
     * @throws IOException a problem accessing the device
     * @throws MediaException a problem getting preview
     */
    private static void createVideoPreview(
            CaptureDeviceInfo device,
            JComponent videoContainer)
        throws IOException,
               MediaException
    {
        videoContainer.removeAll();

        videoContainer.revalidate();
        videoContainer.repaint();

        if (device == null)
            return;

        for (MediaDevice mediaDevice
                : mediaService.getDevices(MediaType.VIDEO, MediaUseCase.ANY))
        {
            if(((MediaDeviceImpl) mediaDevice).getCaptureDeviceInfo().equals(
                    device))
            {
                Dimension videoContainerSize
                    = videoContainer.getPreferredSize();
                Component preview
                    = (Component)
                        mediaService.getVideoPreviewComponent(
                                mediaDevice,
                                videoContainerSize.width,
                                videoContainerSize.height);

                if (preview != null)
                    videoContainer.add(preview);
                break;
            }
        }
    }

    /**
     * The mnemonic for a type.
     * @param type audio or video type.
     * @return the mnemonic.
     */
    private static char getDisplayedMnemonic(int type)
    {
        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            return NeomediaActivator.getResources().getI18nMnemonic(
                "impl.media.configform.AUDIO");
        case DeviceConfigurationComboBoxModel.VIDEO:
            return NeomediaActivator.getResources().getI18nMnemonic(
                "impl.media.configform.VIDEO");
        default:
            throw new IllegalArgumentException("type");
        }
    }

    /**
     * A label for a type.
     * @param type the type.
     * @return the label.
     */
    private static String getLabelText(int type)
    {
        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            return NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO");
        case DeviceConfigurationComboBoxModel.AUDIO_CAPTURE:
            return NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO_IN");
        case DeviceConfigurationComboBoxModel.AUDIO_NOTIFY:
            return NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO_NOTIFY");
        case DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK:
            return NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO_OUT");
        case DeviceConfigurationComboBoxModel.VIDEO:
            return NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.VIDEO");
        default:
            throw new IllegalArgumentException("type");
        }
    }

    /**
     * Used to move encoding options.
     *
     * @param table the table with encodings
     * @param up move direction.
     */
    private static void move(JTable table, boolean up)
    {
        int index
            = ((EncodingConfigurationTableModel) table.getModel()).move(
                    table.getSelectedRow(),
                    up);

        table.getSelectionModel().setSelectionInterval(index, index);
    }

    /**
     * The thread which updates the capture device as selected by the user. This
     * prevent the UI to lock while changing the device.
     */
    private AudioLevelListenerThread audioLevelListenerThread = null;

    /**
     * The button used to play a sound in order to test notification devices.
     */
    private JButton notificationPlaySoundButton;

    /**
     * The combo box used to selected the notification device.
     */
    private JComboBox notifyCombo;

    /**
     * The combo box used to selected the playback device.
     */
    private JComboBox playbackCombo;

    /**
     * The button used to play a sound in order to test playback device.
     */
    private JButton playbackPlaySoundButton;

    /**
     * Indicates that one of the contained in this panel buttons has been
     * clicked.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        boolean isPlaybackEvent = (e.getSource() == playbackPlaySoundButton);

        // If the user clicked on one pley sound button.
        if(isPlaybackEvent
                || e.getSource() == notificationPlaySoundButton)
        {
            AudioNotifierService audioNotifServ
                = NeomediaActivator.getAudioNotifierService();
            String testSoundFilename
                = NeomediaActivator.getConfigurationService()
                    .getString(
                            TEST_SOUND_FILENAME_PROP,
                            NeomediaActivator.getResources().getSoundPath(
                                "TEST_SOUND")
                            );
            SCAudioClip sound = audioNotifServ.createAudio(
                    testSoundFilename,
                    isPlaybackEvent);
            sound.play();
        }
        // If the selected item of the playback or notify combobox has changed.
        else if(e.getSource() == playbackCombo
                || e.getSource() == notifyCombo)
        {
            DeviceConfigurationComboBoxModel.CaptureDevice device
                = (DeviceConfigurationComboBoxModel.CaptureDevice)
                    ((JComboBox) e.getSource()).getSelectedItem();

            boolean isEnabled = (device.info != null);
            if(e.getSource() == playbackCombo)
            {
                playbackPlaySoundButton.setEnabled(isEnabled);
            }
            else
            {
                notificationPlaySoundButton.setEnabled(isEnabled);
            }
        }
    }

    /**
     * Returns the audio configuration panel.
     *
     * @return the audio configuration panel
     */
    public Component createAudioConfigPanel()
    {
        return createControls(DeviceConfigurationComboBoxModel.AUDIO);
    }

    /**
     * Creates the UI controls which are to control the details of a specific
     * <tt>AudioSystem</tt>.
     *
     * @param audioSystem the <tt>AudioSystem</tt> for which the UI controls to
     * control its details are to be created
     * @param container the <tt>JComponent</tt> into which the UI controls which
     * are to control the details of the specified <tt>audioSystem</tt> are to
     * be added
     */
    public void createAudioSystemControls(
            final AudioSystem audioSystem,
            JComponent container)
    {
        GridBagConstraints cnstrnts = new GridBagConstraints();

        cnstrnts.anchor = GridBagConstraints.NORTHWEST;
        cnstrnts.fill = GridBagConstraints.HORIZONTAL;
        cnstrnts.weighty = 0;

        int audioSystemFeatures = audioSystem.getFeatures();
        boolean featureNotifyAndPlaybackDevices
            = ((audioSystemFeatures
                    & AudioSystem.FEATURE_NOTIFY_AND_PLAYBACK_DEVICES)
                != 0);

        cnstrnts.gridx = 0;
        cnstrnts.insets = new Insets(3, 0, 3, 3);
        cnstrnts.weightx = 0;

        cnstrnts.gridy = 0;
        container.add(new JLabel(getLabelText(
            DeviceConfigurationComboBoxModel.AUDIO_CAPTURE)), cnstrnts);
        if (featureNotifyAndPlaybackDevices)
        {
            cnstrnts.gridy = 2;
            container.add(new JLabel(getLabelText(
                DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK)), cnstrnts);
            cnstrnts.gridy = 3;
            container.add(new JLabel(getLabelText(
                DeviceConfigurationComboBoxModel.AUDIO_NOTIFY)), cnstrnts);
        }

        cnstrnts.gridx = 1;
        cnstrnts.insets = new Insets(3, 3, 3, 0);
        cnstrnts.weightx = 1;

        JComboBox captureCombo = null;

        if (featureNotifyAndPlaybackDevices)
        {
            captureCombo = new JComboBox();
            captureCombo.setEditable(false);
            captureCombo.setModel(
                    new DeviceConfigurationComboBoxModel(
                            mediaService.getDeviceConfiguration(),
                            DeviceConfigurationComboBoxModel.AUDIO_CAPTURE));
            cnstrnts.gridy = 0;
            container.add(captureCombo, cnstrnts);
        }

        int anchor = cnstrnts.anchor;
        SoundLevelIndicator capturePreview
            = new SoundLevelIndicator(
                    SimpleAudioLevelListener.MIN_LEVEL,
                    SimpleAudioLevelListener.MAX_LEVEL);

        cnstrnts.anchor = GridBagConstraints.CENTER;
        cnstrnts.gridy = (captureCombo == null) ? 0 : 1;
        container.add(capturePreview, cnstrnts);
        cnstrnts.anchor = anchor;

        cnstrnts.gridy = GridBagConstraints.RELATIVE;

        if (featureNotifyAndPlaybackDevices)
        {
            playbackCombo = new JComboBox();
            playbackCombo.setEditable(false);
            playbackCombo.setModel(
                    new DeviceConfigurationComboBoxModel(
                            mediaService.getDeviceConfiguration(),
                            DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK));
            playbackCombo.addActionListener(this);
            container.add(playbackCombo, cnstrnts);

            notifyCombo = new JComboBox();
            notifyCombo.setEditable(false);
            notifyCombo.setModel(
                    new DeviceConfigurationComboBoxModel(
                            mediaService.getDeviceConfiguration(),
                            DeviceConfigurationComboBoxModel.AUDIO_NOTIFY));
            notifyCombo.addActionListener(this);
            container.add(notifyCombo, cnstrnts);
        }

        int[] checkBoxAudioSystemFeatures
            = new int[]
                    {
                        AudioSystem.FEATURE_ECHO_CANCELLATION,
                        AudioSystem.FEATURE_DENOISE,
                        AudioSystem.FEATURE_AGC
                    };

        for (int i = 0; i < checkBoxAudioSystemFeatures.length; i++)
        {
            final int f = checkBoxAudioSystemFeatures[i];

            if ((f & audioSystemFeatures) != 0)
            {
                String textKey;
                boolean selected;

                switch (f)
                {
                case AudioSystem.FEATURE_AGC:
                    textKey = "impl.media.configform.AUTOMATICGAINCONTROL";
                    selected = audioSystem.isAutomaticGainControl();
                    break;
                case AudioSystem.FEATURE_DENOISE:
                    textKey = "impl.media.configform.DENOISE";
                    selected = audioSystem.isDenoise();
                    break;
                case AudioSystem.FEATURE_ECHO_CANCELLATION:
                    textKey = "impl.media.configform.ECHOCANCEL";
                    selected = audioSystem.isEchoCancel();
                    break;
                default:
                    continue;
                }

                final SIPCommCheckBox checkBox
                    = new SIPCommCheckBox(
                            NeomediaActivator.getResources().getI18NString(
                                    textKey));

                /*
                 * First set the selected one, then add the listener in order to
                 * avoid saving the value when using the default one and only
                 * showing to user without modification.
                 */
                checkBox.setSelected(selected);
                checkBox.addItemListener(
                        new ItemListener()
                        {
                            public void itemStateChanged(ItemEvent e)
                            {
                                boolean b = checkBox.isSelected();

                                switch (f)
                                {
                                case AudioSystem.FEATURE_AGC:
                                    audioSystem.setAutomaticGainControl(b);
                                    break;
                                case AudioSystem.FEATURE_DENOISE:
                                    audioSystem.setDenoise(b);
                                    break;
                                case AudioSystem.FEATURE_ECHO_CANCELLATION:
                                    audioSystem.setEchoCancel(b);
                                    break;
                                }
                            }
                        });
                container.add(checkBox, cnstrnts);
            }
        }

        // Adds the play buttons for testing playback and notification devices.
        cnstrnts.gridx = 2;
        cnstrnts.insets = new Insets(3, 3, 3, 0);
        cnstrnts.weightx = 0;

        if (featureNotifyAndPlaybackDevices)
        {
            // Playback play sound button.
            cnstrnts.gridy = 2;
            playbackPlaySoundButton
                = new JButton(new ImageIcon(NeomediaActivator.getResources()
                            .getImageInBytes(
                                "plugin.notificationconfig.PLAY_ICON")));
            playbackPlaySoundButton.setMinimumSize(new Dimension(30,30));
            playbackPlaySoundButton.setPreferredSize(new Dimension(30,30));
            if(((DeviceConfigurationComboBoxModel.CaptureDevice)
                        playbackCombo.getSelectedItem()).info == null)
            {
                playbackPlaySoundButton.setEnabled(false);
            }
            playbackPlaySoundButton.setOpaque(false);
            playbackPlaySoundButton.addActionListener(this);
            container.add(playbackPlaySoundButton, cnstrnts);

            // Notification play sound button.
            cnstrnts.gridy = 3;
            notificationPlaySoundButton
                = new JButton(new ImageIcon(NeomediaActivator.getResources()
                            .getImageInBytes(
                                "plugin.notificationconfig.PLAY_ICON")));
            notificationPlaySoundButton.setMinimumSize(new Dimension(30,30));
            notificationPlaySoundButton.setPreferredSize(new Dimension(30,30));
            if(((DeviceConfigurationComboBoxModel.CaptureDevice)
                        notifyCombo.getSelectedItem()).info == null)
            {
                notificationPlaySoundButton.setEnabled(false);
            }
            notificationPlaySoundButton.setOpaque(false);
            notificationPlaySoundButton.addActionListener(this);
            container.add(notificationPlaySoundButton, cnstrnts);
        }

        if (audioLevelListenerThread == null)
        {
            audioLevelListenerThread
                = new AudioLevelListenerThread(
                        audioSystem,
                        captureCombo,
                        capturePreview);
        }
        else
        {
            audioLevelListenerThread.init(
                    audioSystem,
                    captureCombo,
                    capturePreview);
        }
    }

    /**
     * Creates basic controls for a type (AUDIO or VIDEO).
     *
     * @param type the type.
     * @return the build Component.
     */
    private Component createBasicControls(final int type)
    {
        final boolean setAudioSystemIsDisabled
            = (type == DeviceConfigurationComboBoxModel.AUDIO)
                && NeomediaActivator.getConfigurationService().getBoolean(
                        MediaServiceImpl.DISABLE_SET_AUDIO_SYSTEM_PNAME,
                        false);
        final DeviceComboBoxField deviceComboBox;
        final Container devicePanel;

        if (setAudioSystemIsDisabled)
        {
            deviceComboBox = null;
            devicePanel = null;
        }
        else
        {
            JLabel deviceLabel = new JLabel(getLabelText(type));

            deviceLabel.setDisplayedMnemonic(getDisplayedMnemonic(type));

            devicePanel
                = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
            devicePanel.setMaximumSize(new Dimension(WIDTH, 25));
            devicePanel.add(deviceLabel);

            deviceComboBox = new DeviceComboBoxField(type, devicePanel);
            deviceLabel.setLabelFor(deviceComboBox.getComponent());
        }

        final JPanel deviceAndPreviewPanel
            = new TransparentPanel(new BorderLayout());
        int preferredDeviceAndPreviewPanelHeight;

        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            preferredDeviceAndPreviewPanelHeight
                = (devicePanel == null) ? 200 : 245;
            break;
        case DeviceConfigurationComboBoxModel.VIDEO:
            preferredDeviceAndPreviewPanelHeight = 305;
            break;
        default:
            preferredDeviceAndPreviewPanelHeight = 0;
            break;
        }
        if (preferredDeviceAndPreviewPanelHeight > 0)
        {
            deviceAndPreviewPanel.setPreferredSize(
                    new Dimension(WIDTH, preferredDeviceAndPreviewPanelHeight));
        }
        if (devicePanel != null)
            deviceAndPreviewPanel.add(devicePanel, BorderLayout.NORTH);

        final DeviceComboBoxField.Listener deviceComboBoxActionListener
            = new DeviceComboBoxField.Listener()
            {
                public void onAction()
                {
                    boolean revalidateAndRepaint = false;

                    for (int i = deviceAndPreviewPanel.getComponentCount() - 1;
                            i >= 0;
                            i--)
                    {
                        Component c = deviceAndPreviewPanel.getComponent(i);

                        if (c != devicePanel)
                        {
                            deviceAndPreviewPanel.remove(i);
                            revalidateAndRepaint = true;
                        }
                    }

                    Component preview = null;

                    if ((deviceComboBox == null)
                            || ((deviceComboBox.getSelectedItem() != null)
                                    && deviceComboBox.getComponent().isShowing()))
                    {
                        preview
                            = createPreview(
                                    type,
                                    deviceComboBox,
                                    deviceAndPreviewPanel.getPreferredSize());
                    }

                    if (preview != null)
                    {
                        deviceAndPreviewPanel.add(preview, BorderLayout.CENTER);
                        revalidateAndRepaint = true;
                    }

                    if (revalidateAndRepaint)
                    {
                        deviceAndPreviewPanel.revalidate();
                        deviceAndPreviewPanel.repaint();
                    }
                }

            };

        if (deviceComboBox != null)
            deviceComboBox.addListener(deviceComboBoxActionListener);

        /*
         * We have to initialize the controls to reflect the configuration at
         * the time of creating this instance. Additionally, because the
         * preview will stop when it and its associated controls become
         * unnecessary, we have to restart it when the mentioned controls become
         * necessary again. We'll address the two goals described by pretending
         * there's a selection in the combo box when user interface becomes
         * displayable.
         */
        deviceAndPreviewPanel.addHierarchyListener(
                new HierarchyListener()
                {
                    public void hierarchyChanged(HierarchyEvent event)
                    {
                        if ((event.getChangeFlags()
                                    & HierarchyEvent.SHOWING_CHANGED)
                                != 0)
                        {
                            SwingUtilities.invokeLater(
                                    new Runnable()
                                    {
                                        public void run()
                                        {
                                            deviceComboBoxActionListener
                                                .onAction();
                                        }
                                    });
                        }
                    }
                });

        return deviceAndPreviewPanel;
    }

    /**
     * Creates all the controls (including encoding) for a type(AUDIO or VIDEO)
     *
     * @param type the type.
     * @return the build Component.
     */
    private Component createControls(int type)
    {
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        Component devicesComponent = null;
        Component encodingsComponent = null;
        Component videoComponent = null;

        int compCount = 0;

        if (cfg == null || !cfg.getBoolean(DEVICES_DISABLED_PROP, false))
        {
            compCount++;
            devicesComponent = createBasicControls(type);
        }
        if (cfg == null || !cfg.getBoolean(ENCODINGS_DISABLED_PROP, false))
        {
            compCount++;
            encodingsComponent = createEncodingControls(type, null);
        }
        if ((type == DeviceConfigurationComboBoxModel.VIDEO)
                && ((cfg == null)
                    || !cfg.getBoolean(
                            VIDEO_MORE_SETTINGS_DISABLED_PROP,
                            false)))
        {
            compCount++;
            videoComponent = createVideoAdvancedSettings();
        }

        ResourceManagementService res = NeomediaActivator.getResources();
        Container container;

        // If we only have one configuration form we don't need to create a
        // tabbed pane.
        if (compCount < 2)
        {
            container = new TransparentPanel(new BorderLayout());

            if (devicesComponent != null)
                container.add(devicesComponent);
            else if (encodingsComponent != null)
                container.add(encodingsComponent);
            else if (videoComponent != null)
                container.add(videoComponent);
        }
        else
        {
            container = new SIPCommTabbedPane();

            SIPCommTabbedPane tabbedPane = (SIPCommTabbedPane) container;
            int index = 0;

            if (devicesComponent != null)
            {
                tabbedPane.insertTab(
                        res.getI18NString("impl.media.configform.DEVICES"),
                        null,
                        devicesComponent,
                        null,
                        index);
                index = 1;
            }
            if (encodingsComponent != null)
            {
                if (tabbedPane.getTabCount() >= 1)
                    index = 1;
                tabbedPane.insertTab(
                        res.getI18NString("impl.media.configform.ENCODINGS"),
                        null,
                        encodingsComponent,
                        null,
                        index);
            }
            if (videoComponent != null)
            {
                if (tabbedPane.getTabCount() >= 2)
                    index = 2;
                tabbedPane.insertTab(
                        res.getI18NString(
                            "impl.media.configform.VIDEO_MORE_SETTINGS"),
                        null,
                        videoComponent,
                        null,
                        index);
            }
        }

        return container;
    }

    /**
     * Creates Component for the encodings of type(AUDIO or VIDEO).
     *
     * @param type the type, either DeviceConfigurationComboBoxModel.AUDIO or
     * DeviceConfigurationComboBoxModel.AUDIO
     * @param encodingConfiguration The <tt>EncodingConfiguration</tt> instance
     * to use. If null, it will use the current encoding configuration from
     * the media service.
     * @return the component.
     */
    private Component createEncodingControls(int type,
            EncodingConfiguration encodingConfiguration)
    {
        if(encodingConfiguration == null)
        {
            encodingConfiguration
                    = mediaService.getCurrentEncodingConfiguration();
        }

        ResourceManagementService resources = NeomediaActivator.getResources();
        String key;

        final JTable table = new JTable();
        table.setShowGrid(false);
        table.setTableHeader(null);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable rtable,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column)
            {
                Component component = super.getTableCellRendererComponent(
                    rtable, value, isSelected, hasFocus, row, column);
                component.setEnabled(rtable != null && rtable.isEnabled());
                return component;
            }
        });

        key = "impl.media.configform.UP";
        final JButton upButton = new JButton(resources.getI18NString(key));
        upButton.setMnemonic(resources.getI18nMnemonic(key));
        upButton.setOpaque(false);

        key = "impl.media.configform.DOWN";
        final JButton downButton = new JButton(resources.getI18NString(key));
        downButton.setMnemonic(resources.getI18nMnemonic(key));
        downButton.setOpaque(false);

        Container buttonBar = new TransparentPanel(new GridLayout(0, 1));
        buttonBar.add(upButton);
        buttonBar.add(downButton);

        Container parentButtonBar = new TransparentPanel(new BorderLayout());
        parentButtonBar.add(buttonBar, BorderLayout.NORTH);

        table.setModel(new EncodingConfigurationTableModel(type,
                encodingConfiguration));
        /*
         * The first column contains the check boxes which enable/disable their
         * associated encodings and it doesn't make sense to make it wider than
         * the check boxes.
         */
        TableColumnModel tableColumnModel = table.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());

        final ListSelectionListener tableSelectionListener =
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent event)
                {
                    if (table.getSelectedRowCount() == 1)
                    {
                        int selectedRow = table.getSelectedRow();
                        if (selectedRow > -1)
                        {
                            upButton.setEnabled(selectedRow > 0);
                            downButton.setEnabled(selectedRow < (table
                                .getRowCount() - 1));
                            return;
                        }
                    }
                    upButton.setEnabled(false);
                    downButton.setEnabled(false);
                }
            };
        table.getSelectionModel().addListSelectionListener(
            tableSelectionListener);
        tableSelectionListener.valueChanged(null);

        ActionListener buttonListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                Object source = event.getSource();
                boolean up;
                if (source == upButton)
                    up = true;
                else if (source == downButton)
                    up = false;
                else
                    return;

                move(table, up);
            }
        };
        upButton.addActionListener(buttonListener);
        downButton.addActionListener(buttonListener);

        Container container = new TransparentPanel(new BorderLayout())
        {
            @Override
            public void setEnabled(boolean enabled)
            {
                super.setEnabled(enabled);
                table.setEnabled(enabled);
                if (enabled)
                {
                    tableSelectionListener.valueChanged(null);
                }
                else
                {
                    upButton.setEnabled(false);
                    downButton.setEnabled(false);
                }
            }
        };
        container.setPreferredSize(new Dimension(WIDTH, 100));
        container.setMaximumSize(new Dimension(WIDTH, 100));

        container.add(new JScrollPane(table), BorderLayout.CENTER);
        container.add(parentButtonBar, BorderLayout.EAST);
        return container;
    }

    /**
     * Returns a component for encodings configuration for the given
     * <tt>mediaType</tt>
     *
     * @param mediaType Either <tt>MediaType.AUDIO</tt> or
     * <tt>MediaType.VIDEO</tt>
     * @param encodingConfiguration The <tt>EncodingConfiguration</tt> instance
     * to use. If null, it will use the current encoding configuration from
     * the media service.
     * @return The component for encodings configuration.
     */
    public Component createEncodingControls(
            MediaType mediaType,
            EncodingConfiguration encodingConfiguration)
    {
        if(encodingConfiguration == null)
        {
            encodingConfiguration
                = mediaService.getCurrentEncodingConfiguration();
        }

        int deviceConfigurationComboBoxModelType;

        switch (mediaType)
        {
        case AUDIO:
            deviceConfigurationComboBoxModelType
                = DeviceConfigurationComboBoxModel.AUDIO;
            break;
        case VIDEO:
            deviceConfigurationComboBoxModelType
                = DeviceConfigurationComboBoxModel.VIDEO;
            break;
        default:
            throw new IllegalArgumentException("mediaType");
        }

        return
            createEncodingControls(
                    deviceConfigurationComboBoxModelType,
                    encodingConfiguration);
    }

    /**
     * Initializes a new <tt>Component</tt> which.is to preview and/or allow
     * detailed configuration of an audio or video <tt>DeviceSystem</tt>.
     *
     * @param type either {@link DeviceConfigurationComboBoxModel#AUDIO} or
     * {@link DeviceConfigurationComboBoxModel#VIDEO}
     * @param comboBox the <tt>JComboBox</tt> which lists the available
     * alternatives and the selection which is to be previewed. May be
     * <tt>null</tt> in the case of audio in which case it is assumed that the
     * user is not allowed to set the <tt>AudioSystem</tt> to be used and the
     * selection is determined by the <tt>DeviceConfiguration</tt> of the
     * <tt>MediaService</tt>.
     * @param prefSize the preferred size to be applied to the preview
     * @return a new <tt>Component</tt> which is to preview and/or allow
     * detailed configuration of the <tt>DeviceSystem</tt> identified by
     * <tt>type</tt> and <tt>comboBox</tt>
     */
    private Component createPreview(
            int type,
            DeviceComboBoxField comboBox,
            Dimension prefSize)
    {
        JComponent preview = null;

        if (type == DeviceConfigurationComboBoxModel.AUDIO)
        {
            AudioSystem audioSystem = null;

            /*
             * If the Audio System combo box is disabled (i.e. the user is not
             * allowed to set the AudioSystem to be used), the current
             * AudioSystem (specified by the DeviceConfiguration of the
             * MediaService) is to be configured.
             */
            if ((comboBox == null) || !comboBox.getComponent().isEnabled())
            {
                audioSystem
                    = mediaService.getDeviceConfiguration().getAudioSystem();
            }
            else
            {
                Object selectedItem = comboBox.getSelectedItem();

                if (selectedItem instanceof AudioSystem)
                {
                    audioSystem = (AudioSystem) selectedItem;

                    AudioSystem mediaServiceDeviceConfigurationAudioSystem
                        = mediaService
                            .getDeviceConfiguration()
                                .getAudioSystem();

                    if (audioSystem
                            != mediaServiceDeviceConfigurationAudioSystem)
                    {
                        logger.warn(
                                "JComboBox.selectedItem is not identical to"
                                    + " MediaService.deviceConfiguration.audioSystem!");
                    }
                }
            }

            if ((audioSystem != null)
                    && !NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                            audioSystem.getLocatorProtocol()))
            {
                preview = new TransparentPanel(new GridBagLayout());
                createAudioSystemControls(audioSystem, preview);
            }
            else
            {
                /*
                 * If there are AudioSystems other than "None" and they have all
                 * not been reported as available, then each of them failed to
                 * detect any devices whatsoever.
                 */
                AudioSystem[] audioSystems = AudioSystem.getAudioSystems();

                if ((audioSystems != null) && (audioSystems.length != 1))
                {
                    AudioSystem[] availableAudioSystems
                        = mediaService
                            .getDeviceConfiguration()
                                .getAvailableAudioSystems();

                    if ((availableAudioSystems != null)
                            && (availableAudioSystems.length == 1))
                    {
                        String noAvailableAudioDevice
                            = NeomediaActivator.getResources().getI18NString(
                                    "impl.media.configform"
                                        + ".NO_AVAILABLE_AUDIO_DEVICE");

                        preview = new TransparentPanel(new GridBagLayout());
                        preview.add(new JLabel(noAvailableAudioDevice));
                    }
                }
            }
        }
        else if (type == DeviceConfigurationComboBoxModel.VIDEO)
        {
            JLabel noPreview
                = new JLabel(
                        NeomediaActivator.getResources().getI18NString(
                                "impl.media.configform.NO_PREVIEW"));

            noPreview.setHorizontalAlignment(SwingConstants.CENTER);
            noPreview.setVerticalAlignment(SwingConstants.CENTER);

            preview = createVideoContainer(noPreview);
            preview.setPreferredSize(prefSize);

            Object selectedItem = comboBox.getSelectedItem();
            CaptureDeviceInfo device = null;
            if (selectedItem
                    instanceof
                        DeviceConfigurationComboBoxModel.CaptureDevice)
                device
                    = ((DeviceConfigurationComboBoxModel.CaptureDevice)
                            selectedItem)
                        .info;

            Exception exception;
            try
            {
                createVideoPreview(device, preview);
                exception = null;
            }
            catch (IOException ex)
            {
                exception = ex;
            }
            catch (MediaException ex)
            {
                exception = ex;
            }
            if (exception != null)
            {
                logger.error(
                        "Failed to create preview for device " + device,
                        exception);
            }
        }

        return preview;
    }

    /**
     * Returns the video configuration panel.
     *
     * @return the video configuration panel
     */
    public Component createVideoConfigPanel()
    {
        return createControls(DeviceConfigurationComboBoxModel.VIDEO);
    }

    /**
     * Returns the <tt>MediaService</tt> instance.
     *
     * @return the <tt>MediaService</tt> instance
     */
    public MediaService getMediaService()
    {
        return mediaService;
    }
}
