/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.media.*;
import javax.media.MediaException;
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
 * @author Lubomir Marinov
 * @author Damian Minkov
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
        // first set the selected one than add the listener
        // in order to avoid saving tha value when using the default one
        // and only showing to user without modification
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
        // first set the selected one than add the listener
        // in order to avoid saving tha value when using the default one
        // and only showing to user without modification
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
                    if(e.getStateChange() == ItemEvent.SELECTED)
                    {
                        if(DeviceConfiguration
                                .AUDIO_SYSTEM_PORTAUDIO.equals(e.getItem()))
                        {
                            createPortAudioControls(portAudioPanel);
                        }
                        else
                        {
                            portAudioPanel.removeAll();
                        }

                        portAudioPanel.revalidate();
                        portAudioPanel.repaint();
                    }
                }
            });
            if(comboBox.getSelectedItem()
                .equals(DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO))
                createPortAudioControls(portAudioPanel);
        }
        else
        {
            portAudioPanel = null;
        }

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
        {
            secondContainer.add(portAudioPanel);
        }
        else
        {
            comboBox.setLightWeightPopupEnabled(false);
            secondContainer.add(createPreview(type, comboBox));
        }

        secondContainer.add(createEncodingControls(type));

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
     * Creates preview for the device(video) in the video container.
     * @param device the device
     * @param videoContainer the container
     * @throws IOException a problem accessing the device.
     * @throws MediaException a problem getting preview.
     */
    private static void createPreview(CaptureDeviceInfo device,
                               final JComponent videoContainer)
        throws IOException,
               MediaException
    {
        videoContainer.removeAll();

        videoContainer.revalidate();
        videoContainer.repaint();

        if (device == null)
            return;

        Iterator<MediaDevice> mDevsIter =
                NeomediaActivator.getMediaServiceImpl()
                    .getDevices(MediaType.VIDEO, MediaUseCase.ANY)
                    .iterator();
        while(mDevsIter.hasNext())
        {
            MediaDeviceImpl dev = (MediaDeviceImpl)mDevsIter.next();
            if(dev.getCaptureDeviceInfo().equals(device))
            {
                Component c = (Component)NeomediaActivator.getMediaServiceImpl()
                    .getVideoPreviewComponent(
                            dev,
                            videoContainer.getSize().width,
                            videoContainer.getSize().height);

                videoContainer.add(c);

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

            final ActionListener comboBoxListener = new ActionListener()
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
            comboBox.addActionListener(comboBoxListener);

            /*
             * We have to initialize the controls to reflect the configuration
             * at the time of creating this instance. Additionally, because the
             * video preview will stop when it and its associated controls
             * become unnecessary, we have to restart it when the mentioned
             * controls become necessary again. We'll address the two goals
             * described by pretending there's a selection in the video combo
             * box when the combo box in question becomes displayable.
             */
            comboBox.addHierarchyListener(new HierarchyListener()
            {
                public void hierarchyChanged(HierarchyEvent event)
                {
                    if (((event.getChangeFlags()
                                    & HierarchyEvent.DISPLAYABILITY_CHANGED)
                                != 0)
                            && comboBox.isDisplayable())
                    {
                        // let current changes end their execution
                        // and after that trigger action on combobox
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run()
                            {
                                comboBoxListener.actionPerformed(null);
                            }
                        });
                    }
                    else
                    {
                        if(!comboBox.isDisplayable())
                            videoDeviceInPreview = null;
                    }
                }
            });
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
}
