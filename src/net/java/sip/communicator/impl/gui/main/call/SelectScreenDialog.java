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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.util.OSUtils;
import org.jitsi.util.swing.*;

/**
 * A dialog dedicated to desktop streaming/sharing. Shows the possible screens
 * to select from to use for the streaming/sharing session.
 *
 * @author Yana Stamcheva
 */
public class SelectScreenDialog
    extends SIPCommDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The object used for logging.
     */
    private final static Logger logger
        = Logger.getLogger(SelectScreenDialog.class);

    /**
     * The combo box containing screen choice.
     */
    private final DeviceComboBoxField deviceComboBox;
    
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
         * A selection change listener.
         */
        private Listener listener;

        /**
         * Constructs <tt>DeviceComboBoxField</tt> instance.
         * @param desktopDevices list with the available devices.
         * @param devicePanel the container of the field.
         */
        public DeviceComboBoxField(Container devicePanel, 
            List<MediaDevice> desktopDevices)
        {
            if(!OSUtils.IS_WINDOWS)
            {
                deviceComboBox = new JComboBox(desktopDevices.toArray());
                deviceComboBox.setRenderer(new ComboRenderer());
                devicePanel.add(deviceComboBox);
                deviceComponent = deviceComboBox;
            }
            else
            {
                deviceList = new JList(desktopDevices.toArray());
                deviceList.setCellRenderer(new ComboRenderer());
                JScrollPane listScroller = new JScrollPane(deviceList);
                listScroller.setPreferredSize(new Dimension(200, 38));
                deviceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                deviceList.setLayoutOrientation(JList.VERTICAL);
                deviceList.setVisibleRowCount(-1);
                deviceList.setSelectedValue(desktopDevices.get(0), true);
                devicePanel.add(listScroller, BorderLayout.NORTH);
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
     * The cancel button of this dialog.
     */
    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

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
    private static MediaDevice videoDeviceInPreview;

    /**
     * The selected media device.
     */
    private MediaDevice selectedDevice;

    /**
     * Creates an instance of <tt>SelectScreenDialog</tt> by specifying the list
     * of possible desktop devices to choose from.
     *
     * @param desktopDevices the list of possible desktop devices to choose
     * from
     */
    public SelectScreenDialog(List<MediaDevice> desktopDevices)
    {
        setModal(true);

        setPreferredSize(new Dimension(400, 300));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        deviceComboBox = new DeviceComboBoxField(contentPane, desktopDevices);

        contentPane.add(createPreview(deviceComboBox));

        contentPane.add(createButtonsPanel(), BorderLayout.SOUTH);
    }

    /**
     * Returns the selected device.
     *
     * @return the selected device
     */
    public MediaDevice getSelectedDevice()
    {
        return selectedDevice;
    }

    /**
     * Creates the buttons panel.
     *
     * @return the buttons panel
     */
    private Component createButtonsPanel()
    {
        JPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.OK"));

        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                selectedDevice
                    = (MediaDevice) deviceComboBox.getSelectedItem();

                dispose();
            }
        });

        buttonsPanel.add(okButton);

        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                selectedDevice = null;
                dispose();
            }
        });

        buttonsPanel.add(cancelButton);

        return buttonsPanel;
    }

    /**
     * Create preview component.
     *
     * @param comboBox the options.
     * @return the component.
     */
    private static Component createPreview(final DeviceComboBoxField comboBox)
    {
        final JComponent preview;

        JLabel noPreview
            = new JLabel(GuiActivator.getResources().getI18NString(
                "impl.media.configform.NO_PREVIEW"));
        noPreview.setHorizontalAlignment(SwingConstants.CENTER);
        noPreview.setVerticalAlignment(SwingConstants.CENTER);

        preview = createVideoContainer(noPreview);

        preview.setPreferredSize(new Dimension(WIDTH, 280));
        preview.setMaximumSize(new Dimension(WIDTH, 280));

        final DeviceComboBoxField.Listener comboBoxListener 
            = new DeviceComboBoxField.Listener()
        {
            public void onAction()
            {
                MediaDevice device = (MediaDevice) comboBox.getSelectedItem();

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
        comboBox.addListener(comboBoxListener);

        /*
         * We have to initialize the controls to reflect the configuration
         * at the time of creating this instance. Additionally, because the
         * video preview will stop when it and its associated controls
         * become unnecessary, we have to restart it when the mentioned
         * controls become necessary again. We'll address the two goals
         * described by pretending there's a selection in the video combo
         * box when the combo box in question becomes displayable.
         */
        comboBox.getComponent().addHierarchyListener(new HierarchyListener()
        {
            public void hierarchyChanged(HierarchyEvent event)
            {
                if (((event.getChangeFlags()
                                & HierarchyEvent.DISPLAYABILITY_CHANGED)
                            != 0)
                        && comboBox.getComponent().isDisplayable())
                {
                    // let current changes end their execution
                    // and after that trigger action on combobox
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run()
                        {
                            comboBoxListener.onAction();
                        }
                    });
                }
                else
                {
                    if(!comboBox.getComponent().isDisplayable())
                        videoDeviceInPreview = null;
                }
            }
        });

        return preview;
    }

    /**
     * Creates preview for the device(video) in the video container.
     *
     * @param device the device
     * @param videoContainer the container
     * @throws IOException a problem accessing the device.
     * @throws MediaException a problem getting preview.
     */
    private static void createPreview( MediaDevice device,
                                       final JComponent videoContainer)
        throws IOException,
               MediaException
    {
        videoContainer.removeAll();

        videoContainer.revalidate();
        videoContainer.repaint();

        if (device == null)
            return;

        Component c = (Component)GuiActivator.getMediaService()
            .getVideoPreviewComponent(
                    device,
                    videoContainer.getSize().width,
                    videoContainer.getSize().height);

        videoContainer.add(c);
    }

    /**
     * Creates the video container.
     *
     * @param noVideoComponent the container component.
     * @return the video container.
     */
    private static JComponent createVideoContainer(Component noVideoComponent)
    {
        return new VideoContainer(noVideoComponent, false);
    }

    /**
     * Custom combo box renderer.
     */
    private static class ComboRenderer
        extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

            MediaDevice mediaDevice = (MediaDevice) value;

            Dimension screenSize = null;
            if (mediaDevice != null)
                screenSize
                    = ((VideoMediaFormat) mediaDevice.getFormat()).getSize();

            this.setText(screenSize.width + "x" + screenSize.height);

            return this;
        }
    }

    /**
     * Automatically press the cancel button when this dialog has been escaped.
     *
     * @param escaped indicates if this dialog has been closed by pressing the
     * ESC key
     */
    @Override
    protected void close(boolean escaped)
    {
        if (escaped)
            cancelButton.doClick();
    }
}
