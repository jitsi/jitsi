/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.media.*;
import javax.media.MediaException;
import javax.media.protocol.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;
import org.osgi.framework.*;

/**
 * @author Lyubomir Marinov
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class MediaConfiguration
{
    /**
     * The <tt>Logger</tt> used by the <tt>MediaConfiguration</tt> class for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaConfiguration.class);

    /**
     * The <tt>MediaService</tt> implementation used by
     * <tt>MediaConfiguration</tt>.
     */
    private static final MediaServiceImpl mediaService
        = NeomediaActivator.getMediaServiceImpl();

    /**
     * The preferred width of all panels.
     */
    private final static int WIDTH = 350;

    /**
     * Indicates if the Devices settings configuration tab
     * should be disabled, i.e. not visible to the user.
     */
    private static final String DEVICES_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.DEVICES_CONFIG_DISABLED";

    /**
     * Indicates if the Audio/Video encodings configuration tab
     * should be disabled, i.e. not visible to the user.
     */
    private static final String ENCODINGS_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.ENCODINGS_CONFIG_DISABLED";

     /**
     * Indicates if the Video/More Settings configuration tab
     * should be disabled, i.e. not visible to the user.
     */
    private static final String VIDEO_MORE_SETTINGS_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.VIDEO_MORE_SETTINGS_CONFIG_DISABLED";

    /**
     * The bundle context.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>MediaConfiguration</tt> instance to read and
     * write configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * Returns a reference to the ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (bundleContext == null)
            bundleContext = NeomediaActivator.getBundleContext();

        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * Returns the audio configuration panel.
     * @return the audio configuration panel
     */
    public static Component createAudioConfigPanel()
    {
        return createControls(DeviceConfigurationComboBoxModel.AUDIO);
    }

    /**
     * Returns the video configuration panel.
     * @return the video configuration panel
     */
    public static Component createVideoConfigPanel()
    {
        return createControls(DeviceConfigurationComboBoxModel.VIDEO);
    }

    private static void createAudioPreview(
            final AudioSystem audioSystem,
            final JComboBox comboBox,
            final SoundLevelIndicator soundLevelIndicator)
    {
        final ActionListener captureComboActionListener
            = new ActionListener()
            {
                private final SimpleAudioLevelListener audioLevelListener
                    = new SimpleAudioLevelListener()
                    {
                        public void audioLevelChanged(int level)
                        {
                            soundLevelIndicator.updateSoundLevel(level);
                        }
                    };

                private AudioMediaDeviceSession deviceSession;

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

                private final Buffer transferHandlerBuffer = new Buffer();

                public void actionPerformed(ActionEvent event)
                {
                    setDeviceSession(null);

                    CaptureDeviceInfo cdi;

                    if (comboBox == null)
                    {
                        cdi
                            = soundLevelIndicator.isShowing()
                                ? audioSystem.getCaptureDevice()
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
                                        DeviceConfigurationComboBoxModel
                                            .CaptureDevice)
                                ? ((DeviceConfigurationComboBoxModel
                                            .CaptureDevice)
                                        selectedItem)
                                    .info
                                : null;
                    }

                    if (cdi != null)
                    {
                        for (MediaDevice md
                                : mediaService.getDevices(
                                        MediaType.AUDIO,
                                        MediaUseCase.ANY))
                        {
                            if (md instanceof AudioMediaDeviceImpl)
                            {
                                AudioMediaDeviceImpl amd
                                    = (AudioMediaDeviceImpl) md;

                                if (cdi.equals(amd.getCaptureDeviceInfo()))
                                {
                                    try
                                    {
                                        MediaDeviceSession deviceSession
                                            = amd.createSession();
                                        boolean setDeviceSession = false;

                                        try
                                        {
                                            if (deviceSession
                                                    instanceof
                                                        AudioMediaDeviceSession)
                                            {
                                                setDeviceSession(
                                                    (AudioMediaDeviceSession)
                                                        deviceSession);
                                                setDeviceSession = true;
                                            }
                                        }
                                        finally
                                        {
                                            if (!setDeviceSession)
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

                private void setDeviceSession(
                        AudioMediaDeviceSession deviceSession)
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
                            this.deviceSession.setLocalUserAudioLevelListener(
                                    null);
                            soundLevelIndicator.resetSoundLevel();
                        }
                    }

                    this.deviceSession = deviceSession;

                    if (this.deviceSession != null)
                    {
                        this.deviceSession.setContentDescriptor(
                                new ContentDescriptor(ContentDescriptor.RAW));
                        this.deviceSession.setLocalUserAudioLevelListener(
                                audioLevelListener);
                        this.deviceSession.start(MediaDirection.SENDONLY);

                        try
                        {
                            DataSource dataSource
                                = this.deviceSession.getOutputDataSource();

                            dataSource.connect();

                            PushBufferStream[] streams
                                = ((PushBufferDataSource) dataSource)
                                    .getStreams();

                            for (PushBufferStream stream : streams)
                                stream.setTransferHandler(transferHandler);

                            dataSource.start();
                        }
                        catch (Throwable t)
                        {
                            if (t instanceof ThreadDeath)
                                throw (ThreadDeath) t;
                            else
                                setDeviceSession(null);
                        }
                    }
                }
            };

        if (comboBox != null)
            comboBox.addActionListener(captureComboActionListener);

        soundLevelIndicator.addHierarchyListener(
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
                                            captureComboActionListener
                                                .actionPerformed(null);
                                        }
                                    });
                        }
                    }
                });
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
    private static void createAudioSystemControls(
            AudioSystem audioSystem,
            JComponent container)
    {
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 0;

        int audioSystemFeatures = audioSystem.getFeatures();
        boolean featureNotifyAndPlaybackDevices
            = ((audioSystemFeatures
                    & AudioSystem.FEATURE_NOTIFY_AND_PLAYBACK_DEVICES)
                != 0);

        constraints.gridx = 0;
        constraints.insets = new Insets(3, 0, 3, 3);
        constraints.weightx = 0;

        constraints.gridy = 0;
        container.add(new JLabel(getLabelText(
            DeviceConfigurationComboBoxModel.AUDIO_CAPTURE)), constraints);
        if (featureNotifyAndPlaybackDevices)
        {
            constraints.gridy = 2;
            container.add(new JLabel(getLabelText(
                DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK)), constraints);
            constraints.gridy = 3;
            container.add(new JLabel(getLabelText(
                DeviceConfigurationComboBoxModel.AUDIO_NOTIFY)), constraints);
        }

        constraints.gridx = 1;
        constraints.insets = new Insets(3, 3, 3, 0);
        constraints.weightx = 1;

        JComboBox captureCombo = null;

        if (featureNotifyAndPlaybackDevices)
        {
            captureCombo = new JComboBox();
            captureCombo.setEditable(false);
            captureCombo.setModel(
                    new DeviceConfigurationComboBoxModel(
                            mediaService.getDeviceConfiguration(),
                            DeviceConfigurationComboBoxModel.AUDIO_CAPTURE));
            constraints.gridy = 0;
            container.add(captureCombo, constraints);
        }

        int anchor = constraints.anchor;
        SoundLevelIndicator capturePreview
            = new SoundLevelIndicator(
                    SimpleAudioLevelListener.MIN_LEVEL,
                    SimpleAudioLevelListener.MAX_LEVEL);

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridy = (captureCombo == null) ? 0 : 1;
        container.add(capturePreview, constraints);
        constraints.anchor = anchor;

        constraints.gridy = GridBagConstraints.RELATIVE;

        if (featureNotifyAndPlaybackDevices)
        {
            JComboBox playbackCombo = new JComboBox();

            playbackCombo.setEditable(false);
            playbackCombo.setModel(
                    new DeviceConfigurationComboBoxModel(
                            mediaService.getDeviceConfiguration(),
                            DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK));
            container.add(playbackCombo, constraints);

            JComboBox notifyCombo = new JComboBox();

            notifyCombo.setEditable(false);
            notifyCombo.setModel(
                    new DeviceConfigurationComboBoxModel(
                            mediaService.getDeviceConfiguration(),
                            DeviceConfigurationComboBoxModel.AUDIO_NOTIFY));
            container.add(notifyCombo, constraints);
        }

        if ((AudioSystem.FEATURE_ECHO_CANCELLATION & audioSystemFeatures) != 0)
        {
            final SIPCommCheckBox echoCancelCheckBox
                = new SIPCommCheckBox(
                        NeomediaActivator.getResources().getI18NString(
                                "impl.media.configform.ECHOCANCEL"));

            /*
             * First set the selected one, then add the listener in order to
             * avoid saving the value when using the default one and only
             * showing to user without modification.
             */
            echoCancelCheckBox.setSelected(
                    mediaService.getDeviceConfiguration().isEchoCancel());
            echoCancelCheckBox.addItemListener(
                    new ItemListener()
                    {
                        public void itemStateChanged(ItemEvent e)
                        {
                            mediaService.getDeviceConfiguration().setEchoCancel(
                                    echoCancelCheckBox.isSelected());
                        }
                    });
            container.add(echoCancelCheckBox, constraints);
        }

        if ((AudioSystem.FEATURE_DENOISE & audioSystemFeatures) != 0)
        {
            final SIPCommCheckBox denoiseCheckBox
                = new SIPCommCheckBox(
                        NeomediaActivator.getResources().getI18NString(
                                "impl.media.configform.DENOISE"));

            /*
             * First set the selected one, then add the listener in order to
             * avoid saving the value when using the default one and only
             * showing to user without modification.
             */
            denoiseCheckBox.setSelected(
                    mediaService.getDeviceConfiguration().isDenoise());
            denoiseCheckBox.addItemListener(
                    new ItemListener()
                    {
                        public void itemStateChanged(ItemEvent e)
                        {
                            mediaService.getDeviceConfiguration().setDenoise(
                                    denoiseCheckBox.isSelected());
                        }
                    });
            container.add(denoiseCheckBox, constraints);
        }

        createAudioPreview(audioSystem, captureCombo, capturePreview);
    }

    /**
     * Creates basic controls for a type (AUDIO or VIDEO).
     *
     * @param type the type.
     * @return the build Component.
     */
    public static Component createBasicControls(final int type)
    {
        final JComboBox deviceComboBox = new JComboBox();

        deviceComboBox.setEditable(false);
        deviceComboBox.setModel(
                new DeviceConfigurationComboBoxModel(
                        mediaService.getDeviceConfiguration(),
                        type));

        JLabel deviceLabel = new JLabel(getLabelText(type));

        deviceLabel.setDisplayedMnemonic(getDisplayedMnemonic(type));
        deviceLabel.setLabelFor(deviceComboBox);

        final Container devicePanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

        devicePanel.setMaximumSize(new Dimension(WIDTH, 25));
        devicePanel.add(deviceLabel);
        devicePanel.add(deviceComboBox);

        final JPanel deviceAndPreviewPanel
            = new TransparentPanel(new BorderLayout());
        int preferredDeviceAndPreviewPanelHeight;

        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            preferredDeviceAndPreviewPanelHeight = 225;
            break;
        case DeviceConfigurationComboBoxModel.VIDEO:
            preferredDeviceAndPreviewPanelHeight = 305;
            break;
        default:
            preferredDeviceAndPreviewPanelHeight = 0;
            break;
        }
        if (preferredDeviceAndPreviewPanelHeight > 0)
            deviceAndPreviewPanel.setPreferredSize(
                    new Dimension(WIDTH, preferredDeviceAndPreviewPanelHeight));
        deviceAndPreviewPanel.add(devicePanel, BorderLayout.NORTH);

        final ActionListener deviceComboBoxActionListener
            = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
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

                    if ((deviceComboBox.getSelectedItem() != null)
                            && deviceComboBox.isShowing())
                    {
                        preview = createPreview(type, deviceComboBox,
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

        deviceComboBox.addActionListener(deviceComboBoxActionListener);
        /*
         * We have to initialize the controls to reflect the configuration
         * at the time of creating this instance. Additionally, because the
         * video preview will stop when it and its associated controls
         * become unnecessary, we have to restart it when the mentioned
         * controls become necessary again. We'll address the two goals
         * described by pretending there's a selection in the video combo
         * box when the combo box in question becomes displayable.
         */
        deviceComboBox.addHierarchyListener(
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
                                                .actionPerformed(null);
                                        }
                                    });
                        }
                    }
                });

        return deviceAndPreviewPanel;
    }

    /**
     * Creates all the controls (including encoding) for a type(AUDIO or VIDEO)
     * @param type the type.
     * @return the build Component.
     */
    private static Component createControls(int type)
    {
        SIPCommTabbedPane container = new SIPCommTabbedPane();
        ResourceManagementService R = NeomediaActivator.getResources();

        if(!getConfigurationService().getBoolean(DEVICES_DISABLED_PROP, false))
        {
            container.insertTab(
                R.getI18NString("impl.media.configform.DEVICES"),
                null,
                createBasicControls(type),
                null,
                0);
        }
        if(!getConfigurationService()
                .getBoolean(ENCODINGS_DISABLED_PROP, false))
        {
            container.insertTab(
                R.getI18NString("impl.media.configform.ENCODINGS"),
                null,
                createEncodingControls(type),
                null,
                1);
        }
        if (type == DeviceConfigurationComboBoxModel.VIDEO
                && !getConfigurationService()
                    .getBoolean(VIDEO_MORE_SETTINGS_DISABLED_PROP, false))
        {
            container.insertTab(
                R.getI18NString("impl.media.configform.VIDEO_MORE_SETTINGS"),
                null,
                createVideoAdvancedSettings(),
                null,
                2);
        }
        return container;
    }

    /**
     * Creates Component for the encodings of type(AUDIO or VIDEO).
     * @param type the type
     * @return the component.
     */
    private static Component createEncodingControls(int type)
    {
        ResourceManagementService resources = NeomediaActivator.getResources();
        String key;

        final JTable table = new JTable();
        table.setShowGrid(false);
        table.setTableHeader(null);

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

        Container container = new TransparentPanel(new BorderLayout());
        container.setPreferredSize(new Dimension(WIDTH, 100));
        container.setMaximumSize(new Dimension(WIDTH, 100));

        container.add(new JScrollPane(table), BorderLayout.CENTER);
        container.add(parentButtonBar, BorderLayout.EAST);

        table.setModel(new EncodingConfigurationTableModel(mediaService
            .getEncodingConfiguration(), type));

        /*
         * The first column contains the check boxes which enable/disable their
         * associated encodings and it doesn't make sense to make it wider than
         * the check boxes.
         */
        TableColumnModel tableColumnModel = table.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());

        ListSelectionListener tableSelectionListener =
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

        return container;
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
                Dimension videoContainerSize = videoContainer.getPreferredSize();
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
     * Create preview component.
     * @param type type
     * @param comboBox the options.
     * @param prefSize the preferred size
     * @return the component.
     */
    private static Component createPreview(int type, final JComboBox comboBox,
                                           Dimension prefSize)
    {
        JComponent preview = null;

        if (type == DeviceConfigurationComboBoxModel.AUDIO)
        {
            Object selectedItem = comboBox.getSelectedItem();

            if (selectedItem instanceof AudioSystem)
            {
                AudioSystem audioSystem = (AudioSystem) selectedItem;

                if (!NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                        audioSystem.getLocatorProtocol()))
                {
                    preview = new TransparentPanel(new GridBagLayout());
                    createAudioSystemControls(audioSystem, preview);
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
                device = null;
            }
        }

        return preview;
    }

    /**
     * Creates the video container.
     * @param noVideoComponent the container component.
     * @return the video container.
     */
    private static JComponent createVideoContainer(Component noVideoComponent)
    {
        return new VideoContainer(noVideoComponent);
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
     * @param table the table with encodings
     * @param up move direction.
     */
    private static void move(JTable table, boolean up)
    {
        int index =
            ((EncodingConfigurationTableModel) table.getModel()).move(table
                .getSelectedRow(), up);
        table.getSelectionModel().setSelectionInterval(index, index);
    }

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

        final JSpinner videoMaxBandwidth = new JSpinner(new SpinnerNumberModel(
            deviceConfig.getVideoMaxBandwidth(),
            1, Integer.MAX_VALUE, 1));
        videoMaxBandwidth.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                deviceConfig.setVideoMaxBandwidth(
                        ((SpinnerNumberModel)videoMaxBandwidth.getModel())
                            .getNumber().intValue());
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 5, 5);
        centerPanel.add(videoMaxBandwidth, constraints);

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
                        DeviceConfiguration.DEFAULT_VIDEO_MAX_BANDWIDTH);
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
}
