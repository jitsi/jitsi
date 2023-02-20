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
package net.java.sip.communicator.impl.neomedia.video;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.media.*;
import javax.media.MediaException;
import javax.swing.*;
import lombok.extern.slf4j.*;
import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.swing.*;
import org.jitsi.utils.*;

@Slf4j
public class VideoDeviceTab
    extends TransparentPanel
{
    private final MediaServiceImpl mediaService;

    private final JComboBox<CaptureDeviceViewModel> deviceComboBox;

    private final JComponent previewContainer;

    public VideoDeviceTab()
    {
        this.mediaService = NeomediaActivator.getMediaServiceImpl();
        ResourceManagementService res = NeomediaActivator.getResources();

        JLabel deviceLabel =
            new JLabel(res.getI18NString("impl.media.configform.VIDEO"));
        deviceComboBox = new JComboBox<>();
        deviceComboBox.setModel(new VideoDeviceComboBoxModel(mediaService));
        deviceComboBox.addActionListener(this::deviceComboBoxActionListener);

        Container deviceSelectionPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        deviceSelectionPanel.setMaximumSize(new Dimension(WIDTH, 25));
        deviceSelectionPanel.add(deviceLabel);
        deviceSelectionPanel.add(deviceComboBox);

        JLabel noPreview = new JLabel(
            NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.NO_PREVIEW"));
        noPreview.setHorizontalAlignment(SwingConstants.CENTER);
        noPreview.setVerticalAlignment(SwingConstants.CENTER);

        previewContainer = new VideoContainer(noPreview, false);
        previewContainer.setPreferredSize(getPreferredSize());

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(deviceSelectionPanel, BorderLayout.NORTH);
        add(previewContainer, BorderLayout.CENTER);

        /*
         * We have to initialize the controls to reflect the configuration at
         * the time of creating this instance. Additionally, because the
         * preview will stop when it and its associated controls become
         * unnecessary, we have to restart it when the mentioned controls become
         * necessary again. We'll address the two goals described by pretending
         * there's a selection in the combo box when user interface becomes
         * displayable.
         */
        addHierarchyListener(event ->
        {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
            {
                SwingUtilities
                    .invokeLater(() -> deviceComboBoxActionListener(null));
            }
        });
    }

    private void deviceComboBoxActionListener(ActionEvent e)
    {
        createPreview();
        revalidate();
        repaint();
    }

    /**
     * Initializes a new <tt>Component</tt> which.is to preview and/or allow
     * detailed configuration of an audio or video <tt>DeviceSystem</tt>.
     */
    private void createPreview()
    {
        Object selectedItem = deviceComboBox.getSelectedItem();
        CaptureDeviceInfo device = null;
        if (selectedItem instanceof CaptureDeviceViewModel)
        {
            device = ((CaptureDeviceViewModel) selectedItem).info;
        }

        try
        {
            createVideoPreview(device);
        }
        catch (IOException | MediaException ex)
        {
            logger.error("Failed to create preview for device {}", device, ex);
        }
    }

    /**
     * Creates preview for the (video) device in the video container.
     *
     * @param device the device
     * @throws IOException    a problem accessing the device
     * @throws MediaException a problem getting preview
     */
    private void createVideoPreview(CaptureDeviceInfo device)
        throws IOException, MediaException
    {
        previewContainer.removeAll();
        previewContainer.revalidate();
        previewContainer.repaint();

        if (device == null || !deviceComboBox.isShowing())
        {
            return;
        }

        for (MediaDevice mediaDevice
            : mediaService.getDevices(MediaType.VIDEO, MediaUseCase.ANY))
        {
            if (((MediaDeviceImpl) mediaDevice).getCaptureDeviceInfo().equals(
                device))
            {
                Dimension videoContainerSize
                    = previewContainer.getSize();
                Component preview
                    = (Component)
                    mediaService.getVideoPreviewComponent(
                        mediaDevice,
                        videoContainerSize.width,
                        videoContainerSize.height);

                if (preview != null)
                {
                    previewContainer.add(preview);
                }
                break;
            }
        }
    }
}
