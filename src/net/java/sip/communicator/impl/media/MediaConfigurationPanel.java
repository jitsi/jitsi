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
import javax.media.protocol.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 */
public class MediaConfigurationPanel
    extends TransparentPanel
{
    private static final int HGAP = 5;

    private static final int VGAP = 5;

    private static MediaServiceImpl getMediaService()
    {
        BundleContext bundleContext = MediaActivator.getBundleContext();
        ServiceReference serviceReference =
            bundleContext.getServiceReference(MediaService.class.getName());

        return (serviceReference == null) ? null
            : (MediaServiceImpl) bundleContext.getService(serviceReference);
    }

    private final Logger logger =
        Logger.getLogger(MediaConfigurationPanel.class);

    private final MediaServiceImpl mediaService = getMediaService();

    public MediaConfigurationPanel()
    {
        super(new GridLayout(0, 1, HGAP, VGAP));

        int[] types =
            new int[]
            { DeviceConfigurationComboBoxModel.AUDIO,
                DeviceConfigurationComboBoxModel.VIDEO };
        for (int i = 0; i < types.length; i++)
            add(createControls(types[i]));
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

    private Component createControls(int type)
    {
        final JComboBox comboBox = new JComboBox();
        comboBox.setEditable(false);
        comboBox.setModel(new DeviceConfigurationComboBoxModel(mediaService
            .getDeviceConfiguration(), type));

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

        key = "MediaConfigurationPanel_encodings";
        JLabel label = new JLabel(resources.getI18NString(key));
        label.setDisplayedMnemonic(resources.getI18nMnemonic(key));
        label.setLabelFor(table);

        key = "MediaConfigurationPanel_up";
        final JButton upButton = new JButton(resources.getI18NString(key));
        upButton.setMnemonic(resources.getI18nMnemonic(key));

        key = "MediaConfigurationPanel_down";
        final JButton downButton = new JButton(resources.getI18NString(key));
        downButton.setMnemonic(resources.getI18nMnemonic(key));

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
    {
        videoContainer.removeAll();

        if (device == null)
            return;

        DataSource dataSource = null;
        Exception exception = null;
        try
        {
            dataSource = Manager.createDataSource(device.getLocator());
        }
        catch (IOException ex)
        {
            exception = ex;
        }
        catch (NoDataSourceException ex)
        {
            exception = ex;
        }
        if (exception != null)
        {
            logger.error("Failed to create preview for device " + device,
                exception);
            return;
        }

        Dimension size = videoContainer.getPreferredSize();
        MediaControl.selectVideoSize(dataSource, size.width, size.height);

        Player player = null;
        try
        {
            player = Manager.createPlayer(dataSource);
        }
        catch (IOException ex)
        {
            exception = ex;
        }
        catch (NoPlayerException ex)
        {
            exception = ex;
        }
        if (exception != null)
        {
            logger.error("Failed to create preview for device " + device,
                exception);
            return;
        }

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
                    "MediaConfigurationPanel_noPreview"));
            noPreview.setHorizontalAlignment(SwingConstants.CENTER);
            noPreview.setVerticalAlignment(SwingConstants.CENTER);

            preview = createVideoContainer(noPreview);

            ActionListener comboBoxListener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    Object selection = comboBox.getSelectedItem();
                    CaptureDeviceInfo device = null;
                    if (selection instanceof DeviceConfigurationComboBoxModel.CaptureDevice)
                    {
                        device =
                            ((DeviceConfigurationComboBoxModel.CaptureDevice) selection).info;
                    }

                    createPreview(device, preview);
                }
            };
            comboBox.addActionListener(comboBoxListener);
            comboBoxListener.actionPerformed(null);
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
    }

    private char getDisplayedMnemonic(int type)
    {
        switch (type)
        {
        case DeviceConfigurationComboBoxModel.AUDIO:
            return MediaActivator.getResources().getI18nMnemonic(
                "MediaConfigurationPanel_audio");
        case DeviceConfigurationComboBoxModel.VIDEO:
            return MediaActivator.getResources().getI18nMnemonic(
                "MediaConfigurationPanel_video");
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
                "MediaConfigurationPanel_audio");
        case DeviceConfigurationComboBoxModel.VIDEO:
            return MediaActivator.getResources().getI18NString(
                "MediaConfigurationPanel_video");
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
                }

                public void hierarchyChanged(HierarchyEvent event)
                {
                    if ((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
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
