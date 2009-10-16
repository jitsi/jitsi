/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.media.*;
import javax.media.MediaException;
import javax.media.protocol.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.media.device.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class MediaConfigurationPanel
    extends TransparentPanel
{
    private static final int HGAP = 5;

    private static final int VGAP = 5;

    private static MediaServiceImpl getMediaService()
    {
        BundleContext bundleContext = MediaActivator.getBundleContext();
        ServiceReference serviceReference
            = bundleContext.getServiceReference(MediaService.class.getName());

        return
            (serviceReference == null)
                ? null
                : (MediaServiceImpl) bundleContext.getService(serviceReference);
    }

    private final Logger logger
        = Logger.getLogger(MediaConfigurationPanel.class);

    private final MediaServiceImpl mediaService = getMediaService();

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
    private CaptureDeviceInfo videoDeviceInPreview;

    /**
     * The <code>Player</code> depicting the preview of the currently selected
     * <code>CaptureDeviceInfo</code>. 
     */
    private Player videoPlayerInPreview;

    /**
     * Creates the panel.
     */
    public MediaConfigurationPanel()
    {
        super(new GridLayout(0, 1, HGAP, VGAP));

        int[] types
            = new int[]
                    {
                        DeviceConfigurationComboBoxModel.AUDIO,
                        DeviceConfigurationComboBoxModel.VIDEO
                    };

        for (int type : types)
            add(createControls(type));
    }

    private void controllerUpdateForPreview(ControllerEvent event,
        Container videoContainer)
    {
        if (event instanceof RealizeCompleteEvent)
        {
            Player player = (Player) event.getSourceController();
            Component video = player.getVisualComponent();

            showPreview(videoContainer, video, player);
        }
    }

    private void createPortAudioControls(
        JPanel portAudioPanel, JPanel parentPanel)
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
        parentPanel.setBorder(
                BorderFactory.createTitledBorder("Devices"));
    }

    private Component createControls(int type)
    {
        final JComboBox comboBox = new JComboBox();
        comboBox.setEditable(false);
        comboBox
            .setModel(
                new DeviceConfigurationComboBoxModel(
                        mediaService.getDeviceConfiguration(),
                        type));

        /*
         * We provide additional configuration properties for PortAudio such as
         * input audio device, output audio device and audio device for playback
         * of notifications.
         */
        final JPanel portAudioPanel;
        final JPanel portAudioParentPanel;
        if (type == DeviceConfigurationComboBoxModel.AUDIO)
        {
            portAudioPanel
                = new TransparentPanel(new GridBagLayout());
            portAudioParentPanel = new TransparentPanel(new BorderLayout());

            comboBox.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    if(e.getStateChange() == ItemEvent.SELECTED)
                    {
                        if(DeviceConfiguration
                                .AUDIO_SYSTEM_PORTAUDIO.equals(e.getItem()))
                        {
                            createPortAudioControls(
                                portAudioPanel, portAudioParentPanel);
                        }
                        else
                        {
                            portAudioPanel.removeAll();
                            portAudioParentPanel.setBorder(null);

                            revalidate();
                            repaint();
                        }
                    }
                }
            });
            if(comboBox
                    .getSelectedItem()
                        .equals(DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO))
                createPortAudioControls(portAudioPanel, portAudioParentPanel);
        }
        else
        {
            portAudioPanel = null;
            portAudioParentPanel = null;
        }

        JLabel label = new JLabel(getLabelText(type));
        label.setDisplayedMnemonic(getDisplayedMnemonic(type));
        label.setLabelFor(comboBox);

        Container firstContainer = new TransparentPanel(new GridBagLayout());
        GridBagConstraints firstConstraints = new GridBagConstraints();
        firstConstraints.anchor = GridBagConstraints.NORTHWEST;
        firstConstraints.gridx = 0;
        firstConstraints.gridy = 0;
        firstConstraints.weightx = 0;
        firstContainer.add(label, firstConstraints);
        firstConstraints.gridx = 1;
        firstConstraints.weightx = 1;
        firstContainer.add(comboBox, firstConstraints);

        Container secondContainer =
            new TransparentPanel(new GridLayout(1, 0, HGAP, VGAP));

        // if creating controls for audio will add devices panel
        // otherwise it is video controls and will add preview panel
        if (portAudioPanel != null)
        {
            // add portAudioPanel in new panel on north, as for some reason
            // anchor = GridBagConstraints.NORTHWEST doesn't work
            // and all components are vertically centered
            portAudioParentPanel.add(portAudioPanel, BorderLayout.NORTH);
            secondContainer.add(portAudioParentPanel);
        }
        else
            secondContainer.add(createPreview(type, comboBox));

        secondContainer.add(createEncodingControls(type));

        Container container = new TransparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        container.add(firstContainer, constraints);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy = 1;
        constraints.weighty = 1;
        container.add(secondContainer, constraints);

        return container;
    }

    private Component createEncodingControls(int type)
    {
        ResourceManagementService resources = MediaActivator.getResources();
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

        Container container = new TransparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        container.add(label, constraints);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        container.add(new JScrollPane(table), constraints);
        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        container.add(buttonBar, constraints);

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

    private void createPreview(CaptureDeviceInfo device,
                               final Container videoContainer)
        throws IOException,
               MediaException
    {
        videoContainer.removeAll();
        if (videoPlayerInPreview != null)
            disposePlayer(videoPlayerInPreview);

        if (device == null)
            return;

        DataSource dataSource = Manager.createDataSource(device.getLocator());

        Dimension size = videoContainer.getPreferredSize();
        MediaControl.selectVideoSize(dataSource, size.width, size.height);

        Player player = Manager.createPlayer(dataSource);

        videoPlayerInPreview = player;

        player.addControllerListener(new ControllerListener()
        {
            public void controllerUpdate(ControllerEvent event)
            {
                controllerUpdateForPreview(event, videoContainer);
            }
        });
        player.start();
    }

    private Component createPreview(int type, final JComboBox comboBox)
    {
        final Container preview;
        if (type == DeviceConfigurationComboBoxModel.VIDEO)
        {
            JLabel noPreview =
                new JLabel(MediaActivator.getResources().getI18NString(
                    "impl.media.configform.NO_PREVIEW"));
            noPreview.setHorizontalAlignment(SwingConstants.CENTER);
            noPreview.setVerticalAlignment(SwingConstants.CENTER);

            preview = createVideoContainer(noPreview);

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
                        comboBoxListener.actionPerformed(null);
                }
            });
        } else
            preview = new TransparentPanel();
        return preview;
    }

    private Container createVideoContainer(Component noVideoComponent)
    {
        return new VideoContainer(noVideoComponent);
    }

    private void disposePlayer(Player player)
    {
        player.stop();
        player.deallocate();
        player.close();

        if ((videoPlayerInPreview != null)
                && videoPlayerInPreview.equals(player))
            videoPlayerInPreview = null;
    }

    private char getDisplayedMnemonic(int type)
    {
        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            return MediaActivator.getResources().getI18nMnemonic(
                "impl.media.configform.AUDIO");
        case DeviceConfigurationComboBoxModel.VIDEO:
            return MediaActivator.getResources().getI18nMnemonic(
                "impl.media.configform.VIDEO");
        default:
            throw new IllegalArgumentException("type");
        }
    }

    private String getLabelText(int type)
    {
        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            return MediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO");
        case DeviceConfigurationComboBoxModel.AUDIO_CAPTURE:
            return MediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO_IN");
        case DeviceConfigurationComboBoxModel.AUDIO_NOTIFY:
            return MediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO_NOTIFY");
        case DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK:
            return MediaActivator.getResources().getI18NString(
                "impl.media.configform.AUDIO_OUT");
        case DeviceConfigurationComboBoxModel.VIDEO:
            return MediaActivator.getResources().getI18NString(
                "impl.media.configform.VIDEO");
        default:
            throw new IllegalArgumentException("type");
        }
    }

    private void move(JTable table, boolean up)
    {
        int index =
            ((EncodingConfigurationTableModel) table.getModel()).move(table
                .getSelectedRow(), up);
        table.getSelectionModel().setSelectionInterval(index, index);
    }

    private void showPreview(final Container previewContainer,
        final Component preview, final Player player)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    showPreview(previewContainer, preview, player);
                }
            });
            return;
        }

        previewContainer.removeAll();

        if (preview != null)
        {
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
                    preview.removeHierarchyListener(this);

                    disposePlayer(player);
                    videoDeviceInPreview = null;

                    /*
                     * We've just disposed the player which created the preview
                     * component so the preview component is of no use
                     * regardless of whether the Media configuration form will
                     * be redisplayed or not. And since the preview component
                     * appears to be a huge object even after its player is
                     * disposed, make sure to not reference it.
                     */
                    previewContainer.remove(preview);
                }

                public void hierarchyChanged(HierarchyEvent event)
                {
                    if ((event.getChangeFlags()
                                    & HierarchyEvent.DISPLAYABILITY_CHANGED)
                                != 0)
                    {
                        if (preview.isDisplayable())
                        {
                            if (windowListener == null)
                            {
                                window =
                                    SwingUtilities.windowForComponent(preview);
                                if (window != null)
                                {
                                    windowListener = new WindowAdapter()
                                    {
                                        public void windowClosing(
                                            WindowEvent event)
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
                }
            };
            preview.addHierarchyListener(hierarchyListener);

            previewContainer.add(preview);
        }
        else
            disposePlayer(player);
    }
}
