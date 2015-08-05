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
package net.java.sip.communicator.plugin.desktoputil.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Dialog in which we can load an image from file or take new one by using
 * the webcam. Scaling the image to desired size.
 *
 * @author Shashank Tyagi
 * @author Damien Roth
 * @author Damian Minkov
 */
public class ImagePickerDialog
    extends SIPCommDialog
    implements ActionListener
{
    private EditPanel editPanel;

    private JButton okButton, cancelButton;
    private JButton selectFileButton, webcamButton;

    private boolean editCanceled = false;

    public ImagePickerDialog(int clipperZoneWidth, int clipperZoneHeight)
    {
        super();
        this.initComponents(clipperZoneWidth, clipperZoneHeight);
        this.initDialog();

        this.pack();
        this.setLocationRelativeTo(null);
    }

    /**
     * Initialize the dialog with the already created components.
     */
    private void initDialog()
    {
        this.setTitle(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.IMAGE_PICKER"));
        this.setModal(true);
        this.setResizable(true);

        this.setLayout(new BorderLayout());

        TransparentPanel editButtonsPanel = new TransparentPanel();
        editButtonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        editButtonsPanel.add(this.selectFileButton);
        editButtonsPanel.add(this.webcamButton);

        TransparentPanel okCancelPanel = new TransparentPanel();
        okCancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        okCancelPanel.add(cancelButton);
        okCancelPanel.add(okButton);

        TransparentPanel buttonsPanel = new TransparentPanel();
        buttonsPanel.setLayout(new BorderLayout());
        buttonsPanel.add(editButtonsPanel, BorderLayout.WEST);
        buttonsPanel.add(okCancelPanel, BorderLayout.CENTER);

        this.add(this.editPanel, BorderLayout.CENTER);
        this.add(buttonsPanel, BorderLayout.SOUTH);

        this.pack();

    }

    /**
     * Initialize UI components.
     * @param clipperZoneWidth
     * @param clipperZoneHeight
     */
    private void initComponents(int clipperZoneWidth, int clipperZoneHeight)
    {
        // Edit panel
        this.editPanel = new EditPanel(clipperZoneWidth, clipperZoneHeight);

        // Buttons
        this.okButton = new JButton(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.SET"));
        this.okButton.addActionListener(this);
        this.okButton.setName("okButton");

        this.cancelButton = new JButton(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CANCEL"));
        this.cancelButton.addActionListener(this);
        this.cancelButton.setName("cancelButton");

        this.selectFileButton = new JButton(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CHOOSE_FILE"));
        this.selectFileButton.addActionListener(this);
        this.selectFileButton.setName("selectFileButton");

        this.webcamButton = new JButton(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.TAKE_PHOTO"));

        this.webcamButton.addActionListener(this);
        this.webcamButton.setName("webcamButton");
    }

    /**
     * Shows current dialog and setting initial picture.
     * @param image the initial picture to show.
     * @return the result: clipped image (from file or webcam).
     */
    public byte[] showDialog(Image image)
    {
        if(image != null)
            this.editPanel.setImage(ImageUtils.getBufferedImage(image));

        this.setVisible(true);

        if (this.editCanceled)
            return null;
        else
            return this.editPanel.getClippedImage();
    }

    /**
     * Invoked for any button activity.
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        String name = ((JButton) e.getSource()).getName();
        if (name.equals("cancelButton"))
        {
            editCanceled = true;
            this.setVisible(false);
        }
        else if (name.equals("selectFileButton"))
        {
            SipCommFileChooser chooser = GenericFileDialog.create(
                //GuiActivator.getUIService().getMainFrame(),
                null,
                DesktopUtilActivator.getResources().getI18NString(
                        "service.gui.avatar.imagepicker.CHOOSE_FILE"),
                SipCommFileChooser.LOAD_FILE_OPERATION);

            chooser.addFilter(new ImageFileFilter());

            File selectedFile = chooser.getFileFromDialog();
            if(selectedFile != null)
            {
                try
                {
                    BufferedImage image = ImageIO.read(selectedFile);
                    this.editPanel.setImage(image);
                }
                catch (IOException ioe)
                {
                    // TODO Auto-generated catch block
                    ioe.printStackTrace();
                }
            }
        }
        else if (name.equals("okButton"))
        {
            editCanceled = false;
            this.setVisible(false);
        }
        else if (name.equals("webcamButton"))
        {
            WebcamDialog dialog = new WebcamDialog(this);
            dialog.setVisible(true);
            byte[] bimage = dialog.getGrabbedImage();

            if (bimage != null)
            {
                Image i = new ImageIcon(bimage).getImage();
                editPanel.setImage(ImageUtils.getBufferedImage(i));
            }
        }
    }

    /**
     * Closes the dialog.
     * @param isEscaped
     */
    @Override
    protected void close(boolean isEscaped)
    {
        dispose();
    }

    /**
     * The filter for file chooser.
     */
    class ImageFileFilter extends SipCommFileFilter
    {
        @Override
        public boolean accept(File f)
        {
            String path = f.getAbsolutePath().toLowerCase();
            if (path.matches("(.*)\\.(jpg|jpeg|png|bmp)$") ||
                    f.isDirectory())
                return true;

            else
                return false;
        }

        @Override
        public String getDescription()
        {
            return DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.IMAGE_FILES");
        }
    }
}
