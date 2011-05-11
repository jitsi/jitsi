/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.media.*;
import javax.media.MediaException; // disambiguation
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * @author Lyubomir Marinov
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class MediaConfiguration
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(MediaConfiguration.class);

    /**
     * The current instance of the media service.
     */
    private static final MediaServiceImpl mediaService
        = NeomediaActivator.getMediaServiceImpl();

    /**
     * The preferred width of all panels.
     */
    private final static int WIDTH = 350;

    /**
     * The video <code>CaptureDeviceInfo</code> this instance started to create
     * the preview of.
     * <p>
     * Because the creation of the preview is asynchronous, it's possible to
     * request the preview of one and the same device multiple times. Which may
     * lead to failures because of, for example, busy devices and/or resources
     * (as is the case with LTI-CIVIL and video4linux2).
     * </p>
     */
    private static CaptureDeviceInfo videoDeviceInPreview;

    /**
     * The listener that listens and changes the preview panel.
     */
    private static ActionListener videoDeviceChangeListener;

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

    /**
     * Creates the ui controls for portaudio.
     * @param portAudioPanel the panel
     */
    private static void createPortAudioControls(JPanel portAudioPanel)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridy = 0;

        portAudioPanel.add(new JLabel(getLabelText(
            DeviceConfigurationComboBoxModel.AUDIO_CAPTURE)), constraints);
        constraints.gridy = 1;
        portAudioPanel.add(new JLabel(getLabelText(
            DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK)), constraints);
        constraints.gridy = 2;
        portAudioPanel.add(new JLabel(getLabelText(
            DeviceConfigurationComboBoxModel.AUDIO_NOTIFY)), constraints);

        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        JComboBox captureCombo = new JComboBox();
        captureCombo.setEditable(false);
        captureCombo.setModel(
        new DeviceConfigurationComboBoxModel(
            mediaService.getDeviceConfiguration(),
            DeviceConfigurationComboBoxModel.AUDIO_CAPTURE));
        portAudioPanel.add(captureCombo, constraints);

        constraints.gridy = 1;
        JComboBox playbackCombo = new JComboBox();
        playbackCombo.setEditable(false);
        playbackCombo.setModel(
            new DeviceConfigurationComboBoxModel(
            mediaService.getDeviceConfiguration(),
            DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK));
        portAudioPanel.add(playbackCombo, constraints);

        constraints.gridy = 2;
        JComboBox notifyCombo = new JComboBox();
        notifyCombo.setEditable(false);
        notifyCombo.setModel(
            new DeviceConfigurationComboBoxModel(
            mediaService.getDeviceConfiguration(),
            DeviceConfigurationComboBoxModel.AUDIO_NOTIFY));
        portAudioPanel.add(notifyCombo, constraints);

        constraints.gridy = 3;
        constraints.insets = new Insets(10,0,0,0);
        final SIPCommCheckBox echoCancelCheckBox = new SIPCommCheckBox(
            NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.ECHOCANCEL"));
        /*
         * First set the selected one, then add the listener in order to avoid
         * saving the value when using the default one and only showing to user
         * without modification.
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
        portAudioPanel.add(echoCancelCheckBox, constraints);

        constraints.gridy = 4;
        constraints.insets = new Insets(0,0,0,0);
        final SIPCommCheckBox denoiseCheckBox = new SIPCommCheckBox(
            NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.DENOISE"));
        /*
         * First set the selected one, then add the listener in order to avoid
         * saving the value when using the default one and only showing to user
         * without modification.
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
        portAudioPanel.add(denoiseCheckBox, constraints);

        portAudioPanel.setBorder(
                BorderFactory.createTitledBorder(
                        NeomediaActivator.getResources().getI18NString(
                        "impl.media.configform.DEVICES")));
    }

    /**
     * Creates all the controls for a type(AUDIO or VIDEO)
     * @param type the type.
     * @return the build Component.
     */
    private static Component createControls(int type)
    {
        final JComboBox comboBox = new JComboBox();
        comboBox.setEditable(false);
        comboBox.setModel(  new DeviceConfigurationComboBoxModel(
                                mediaService.getDeviceConfiguration(),
                            type));

        /*
         * We provide additional configuration properties for PortAudio such as
         * input audio device, output audio device and audio device for playback
         * of notifications.
         */
        final JPanel portAudioPanel;
        if (type == DeviceConfigurationComboBoxModel.AUDIO)
        {
            portAudioPanel = new TransparentPanel(new GridBagLayout());

            portAudioPanel.setPreferredSize(new Dimension(WIDTH, 200));
            portAudioPanel.setMaximumSize(new Dimension(WIDTH, 200));

            comboBox.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    if (ItemEvent.SELECTED == e.getStateChange())
                    {
                        if (DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO.equals(
                                e.getItem()))
                            createPortAudioControls(portAudioPanel);
                        else
                            portAudioPanel.removeAll();

                        portAudioPanel.revalidate();
                        portAudioPanel.repaint();
                    }
                }
            });
            if (DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO.equals(
                    comboBox.getSelectedItem()))
                createPortAudioControls(portAudioPanel);
        }
        else
            portAudioPanel = null;

        JLabel label = new JLabel(getLabelText(type));
        label.setDisplayedMnemonic(getDisplayedMnemonic(type));
        label.setLabelFor(comboBox);

        Container firstContainer
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        firstContainer.setMaximumSize(new Dimension(WIDTH, 25));
        firstContainer.add(label);
        firstContainer.add(comboBox);

        JPanel secondContainer = new TransparentPanel();

        secondContainer.setLayout(
            new BoxLayout(secondContainer, BoxLayout.Y_AXIS));

        // if creating controls for audio will add devices panel
        // otherwise it is video controls and will add preview panel
        if (portAudioPanel != null)
            secondContainer.add(portAudioPanel);
        else
        {
            comboBox.setLightWeightPopupEnabled(false);
            secondContainer.add(createPreview(type, comboBox));
        }

        secondContainer.add(createEncodingControls(type));

        if (portAudioPanel == null)
            secondContainer.add(createVideoAdvancedSettings());

        JPanel container = new TransparentPanel(new BorderLayout());
        container.add(firstContainer, BorderLayout.NORTH);
        container.add(secondContainer, BorderLayout.CENTER);

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

        key = "impl.media.configform.ENCODINGS";
        JLabel label = new JLabel(resources.getI18NString(key));
        label.setDisplayedMnemonic(resources.getI18nMnemonic(key));
        label.setLabelFor(table);

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

        container.add(label, BorderLayout.NORTH);
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
    private static void createPreview(
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

        Iterable<MediaDevice> devs
            = mediaService.getDevices(MediaType.VIDEO, MediaUseCase.ANY);

        for (MediaDevice dev : devs)
        {
            if(((MediaDeviceImpl) dev).getCaptureDeviceInfo().equals(device))
            {
                Dimension videoContainerSize = videoContainer.getSize();
                Component preview
                    = (Component)
                        mediaService.getVideoPreviewComponent(
                                dev,
                                videoContainerSize.width,
                                videoContainerSize.height);

                videoContainer.add(preview);

                break;
            }
        }
    }

    /**
     * Create preview component.
     * @param type type
     * @param comboBox the options.
     * @return the component.
     */
    private static Component createPreview(int type, final JComboBox comboBox)
    {
        final JComponent preview;
        if (type == DeviceConfigurationComboBoxModel.VIDEO)
        {
            JLabel noPreview =
                new JLabel(NeomediaActivator.getResources().getI18NString(
                    "impl.media.configform.NO_PREVIEW"));
            noPreview.setHorizontalAlignment(SwingConstants.CENTER);
            noPreview.setVerticalAlignment(SwingConstants.CENTER);

            preview = createVideoContainer(noPreview);

            preview.setPreferredSize(new Dimension(WIDTH, 280));
            preview.setMaximumSize(new Dimension(WIDTH, 280));

            videoDeviceChangeListener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    Object selection = comboBox.getSelectedItem();
                    CaptureDeviceInfo device = null;
                    if (selection
                            instanceof
                                DeviceConfigurationComboBoxModel.CaptureDevice)
                        device
                            = ((DeviceConfigurationComboBoxModel.CaptureDevice)
                                    selection)
                                .info;

                    ((DeviceConfigurationComboBoxModel)comboBox.getModel()).
                        reinitVideo();

                    if ((device != null) && device.equals(videoDeviceInPreview))
                        return;

                    Exception exception;
                    try
                    {
                        createPreview(device, preview);
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

                    videoDeviceInPreview = device;
                }
            };
            comboBox.addActionListener(videoDeviceChangeListener);

            /*
             * We have to initialize the controls to reflect the configuration
             * at the time of creating this instance. Additionally, because the
             * video preview will stop when it and its associated controls
             * become unnecessary, we have to restart it when the mentioned
             * controls become necessary again. We'll address the two goals
             * described by pretending there's a selection in the video combo
             * box when the combo box in question becomes displayable.
             */
            HierarchyListener hierarchyListener = new HierarchyListener()
            {
                private Window window;

                private WindowListener windowListener;

                public void dispose()
                {
                    if (windowListener != null)
                    {
                        if (window != null)
                        {
                            window.removeWindowListener(windowListener);
                            window = null;
                        }
                        windowListener = null;
                    }

                    videoDeviceInPreview = null;
                }

                public void hierarchyChanged(HierarchyEvent event)
                {
                    if ((event.getChangeFlags()
                                & HierarchyEvent.DISPLAYABILITY_CHANGED)
                            == 0)
                        return;

                    if (comboBox.isDisplayable())
                    {
                        /*
                         * Let current changes end their execution and trigger
                         * action on combobox afterwards.
                         */
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                videoDeviceChangeListener.actionPerformed(null);
                            }
                        });

                        /*
                         * FIXME When the Options dialog closes on Mac OS X, the
                         * displayable property of the comboBox will not become
                         * false. Consequently, the next time the Options dialog
                         * opens, the displayable property will not change.
                         * Which will lead to no preview being created for the
                         * device selected in the comboBox.
                         */
                        if (windowListener == null)
                        {
                            window
                                = SwingUtilities.windowForComponent(comboBox);
                            if (window != null)
                            {
                                windowListener = new WindowAdapter()
                                {
                                    @Override
                                    public void windowClosing(WindowEvent event)
                                    {
                                        dispose();
                                    }
                                };
                                window.addWindowListener(windowListener);
                            }
                        }
                    }
                    else
                    {
                        dispose();
                    }
                }
            };
            comboBox.addHierarchyListener(hierarchyListener);
        } else
            preview = new TransparentPanel();

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

        final TransparentPanel advancedPanel =
            new TransparentPanel(new BorderLayout());
        advancedPanel.setMaximumSize(new Dimension(WIDTH, 150));
        final JLabel advButton = new JLabel(NeomediaActivator.getResources()
            .getI18NString("impl.media.configform.VIDEO_MORE_SETTINGS"));
        advButton.setIcon(NeomediaActivator.getResources()
            .getImage("service.gui.icons.RIGHT_ARROW_ICON"));
        TransparentPanel buttonPanel = new TransparentPanel(
            new BorderLayout());
        buttonPanel.add(advButton, BorderLayout.WEST);
        advancedPanel.add(buttonPanel, BorderLayout.NORTH);

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

        final JPanel centerAdvancedPanel =
            new TransparentPanel(new BorderLayout());
        centerAdvancedPanel.add(centerPanel, BorderLayout.CENTER);
        centerAdvancedPanel.add(resetButtonPanel, BorderLayout.SOUTH);
        centerAdvancedPanel.setBorder(BorderFactory.createLineBorder(
                centerAdvancedPanel.getForeground()));
        centerAdvancedPanel.setVisible(false);

        advancedPanel.add(centerAdvancedPanel, BorderLayout.CENTER);

        advButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                advButton.setIcon(
                        NeomediaActivator.getResources().getImage(
                                centerAdvancedPanel.isVisible()
                                    ? "service.gui.icons.RIGHT_ARROW_ICON"
                                    : "service.gui.icons.DOWN_ARROW_ICON"));

                centerAdvancedPanel.setVisible(
                        !centerAdvancedPanel.isVisible());

                advancedPanel.revalidate();

                NeomediaActivator.getUIService().getConfigurationContainer()
                   .validateCurrentForm();
            }
        });

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
        final JCheckBox frameRateCheck = new JCheckBox(
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
            1, 256, 1));
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

                videoDeviceInPreview = null;
                videoDeviceChangeListener.actionPerformed(null);
            }
        });

        frameRateCheck.setSelected(
            deviceConfig.getFrameRate()
                != DeviceConfiguration.DEFAULT_FRAME_RATE);
        frameRate.setEnabled(frameRateCheck.isSelected());

        if(frameRate.isEnabled())
            frameRate.setValue(deviceConfig.getFrameRate());

        return advancedPanel;
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
         * @return
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
