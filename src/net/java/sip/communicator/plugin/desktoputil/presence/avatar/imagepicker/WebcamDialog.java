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

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.neomedia.*;

/**
 * A dialog showing the webcam and allowing the user to grap a snapshot
 *
 * @author Damien Roth
 * @author Damian Minkov
 */
public class WebcamDialog
    extends SIPCommDialog
    implements ActionListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>WebcamDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(WebcamDialog.class);

    private Component videoContainer;

    private JButton grabSnapshot;

    private byte[] grabbedImage = null;

    private TimerImage[] timerImages = new TimerImage[3];

    /**
     * Construct a <tt>WebcamDialog</tt>
     * @param parent the ImagePickerDialog
     */
    public WebcamDialog(ImagePickerDialog parent)
    {
        super(false);
        this.setTitle(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.TAKE_PHOTO"));
        this.setModal(true);

        init();

        this.setSize(320, 240);
    }

    /**
     * Init the dialog
     */
    private void init()
    {
        this.grabSnapshot = new JButton();
        this.grabSnapshot.setText(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CLICK"));
        this.grabSnapshot.setName("grab");
        this.grabSnapshot.addActionListener(this);
        this.grabSnapshot.setEnabled(false);

        JButton cancelButton = new JButton(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CANCEL"));
        cancelButton.setName("cancel");
        cancelButton.addActionListener(this);

        initAccessWebcam();

        // Timer Panel
        TransparentPanel timerPanel = new TransparentPanel();
        timerPanel.setLayout(new GridLayout(0, timerImages.length));

        TransparentPanel tp;
        for (int i = 0; i < this.timerImages.length; i++)
        {
            this.timerImages[i] = new TimerImage("" + (timerImages.length - i));

            tp = new TransparentPanel();
            tp.add(this.timerImages[i], BorderLayout.CENTER);

            timerPanel.add(tp);
        }

        TransparentPanel buttonsPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(this.grabSnapshot);
        buttonsPanel.add(cancelButton);

        // South Panel
        TransparentPanel southPanel = new TransparentPanel(new BorderLayout());
        southPanel.add(timerPanel, BorderLayout.CENTER);
        southPanel.add(buttonsPanel, BorderLayout.SOUTH);

        TransparentPanel videoPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        videoPanel.add(this.videoContainer);

        this.add(videoPanel, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);

        this.setResizable(false);
    }

    /**
     * Init the access to the webcam (asynchonous call)
     */
    private void initAccessWebcam()
    {
        //Call the method in the media service
        MediaService mediaService = DesktopUtilActivator.getMediaService();

        this.videoContainer
            = (Component)
                mediaService.getVideoPreviewComponent(
                        mediaService.getDefaultDevice(
                                MediaType.VIDEO,
                                MediaUseCase.CALL),
                        320,
                        240);
        this.grabSnapshot.setEnabled(true);
    }

    /**
     * Grab the current image of the webcam through the MediaService
     */
    private void grabSnapshot()
    {
        try
        {
            Robot robot = new Robot();
            Point location = videoContainer.getLocationOnScreen();

            BufferedImage bi = robot.createScreenCapture(new Rectangle(
                    location.x,
                    location.y,
                    videoContainer.getWidth(),
                    videoContainer.getHeight()));
            this.grabbedImage = ImageUtils.toByteArray(bi);
        }
        catch (Throwable e)
        {
            logger.error("Cannot create snapshot!", e);
        }

        close(false);
        this.setVisible(false);
    }

    /**
     * Return the grabbed snapshot as a byte array
     *
     * @return the grabbed snapshot
     */
    public byte[] getGrabbedImage()
    {
        return this.grabbedImage;
    }

    /**
     * Play a snapshot sound
     */
    private void playSound()
    {
        String soundKey = DesktopUtilActivator.getResources()
            .getSoundPath("WEBCAM_SNAPSHOT");

        SCAudioClip audio = DesktopUtilActivator.getAudioNotifier()
            .createAudio(soundKey);

        audio.play();
    }

    /**
     * Invoked when a window is closed. Dispose dialog.
     * @param isEscaped
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.videoContainer = null;
        dispose();
    }

    /**
     * Listens for actions for the buttons in this dialog.
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        String actName = ((JButton) e.getSource()).getName();

        if (actName.equals("grab"))
        {
            this.grabSnapshot.setEnabled(false);
            new SnapshotTimer().start();
        }
        else
        {
            close(false);
            dispose();
        }
    }

    /**
     * This thread grabs the snapshot by counting down.
     */
    private class SnapshotTimer
        extends Thread
    {
        @Override
        public void run()
        {
            int i;

            for (i=0; i < timerImages.length; i++)
            {
                timerImages[i].setElapsed();
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    logger.error("", e);
                }
            }

            playSound();
            grabSnapshot();

            WebcamDialog.this.setVisible(false);
            WebcamDialog.this.dispose();

        }
    }

    /**
     * These are the images shown as timer while grabbing the snapshot.
     */
    private class TimerImage
        extends JComponent
    {
        /**
         * Image width.
         */
        private static final int WIDTH = 30;

        /**
         * Image height.
         */
        private static final int HEIGHT = 30;

        /**
         * Whether image is already elapsed.
         */
        private boolean isElapsed = false;

        /**
         * Font of the image.
         */
        private Font textFont = null;

        /**
         * The string that will be shown in the image.
         */
        private String second;

        public TimerImage(String second)
        {
            Dimension d = new Dimension(WIDTH, HEIGHT);
            this.setPreferredSize(d);
            this.setMinimumSize(d);


            this.textFont = new Font("Sans", Font.BOLD, 20);
            this.second = second;
        }

        /**
         * Is current image elapsed.
         */
        public void setElapsed()
        {
            this.isElapsed = true;
            this.repaint();
        }

        /**
         * Paint the number image.
         * @param g
         */
        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g;
            AntialiasingManager.activateAntialiasing(g);

            Color c = (isElapsed)
                    ? Color.RED
                    : new Color(150, 0, 0);

            g2d.setColor(c);
            g2d.fillOval(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(textFont);
            g2d.drawString(this.second, 7, 21);
        }
    }
}
