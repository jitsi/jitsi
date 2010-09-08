/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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

    private VideoContainer videoContainer;

    private JButton grabSnapshot;

    private byte[] grabbedImage = null;
    
    private TimerImage[] timerImages = new TimerImage[3];
    
    /**
     * Construct a <tt>WebcamDialog</tt>
     * @param parent the ImagePickerDialog
     */
    public WebcamDialog(ImagePickerDialog parent)
    {
        super(parent);
        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.TAKE_PHOTO"));
        this.setModal(true);
        
        init();
    }

    /**
     * Init the dialog
     */
    private void init()
    {
        this.grabSnapshot = new JButton();
        this.grabSnapshot.setText(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CLICK"));
        this.grabSnapshot.setName("grab");
        this.grabSnapshot.addActionListener(this);
        this.grabSnapshot.setEnabled(false);

        JButton cancelButton = new JButton(GuiActivator.getResources()
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
                = new TransparentPanel(new GridLayout(1, 2));
        buttonsPanel.add(this.grabSnapshot);
        buttonsPanel.add(cancelButton);
        
        // South Panel
        TransparentPanel southPanel = new TransparentPanel(new BorderLayout());
        southPanel.add(timerPanel, BorderLayout.CENTER);
        southPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.add(this.videoContainer, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);   
    }

    /**
     * Init the access to the webcam (asynchonous call)
     */
    private void initAccessWebcam()
    {
        //Call the method in the media service
        this.videoContainer =
            (VideoContainer)GuiActivator.getMediaService()
                .getVideoPreviewComponent(
                    GuiActivator.getMediaService().getDefaultDevice(
                        MediaType.VIDEO,
                        MediaUseCase.CALL
                        ), 320, 240);
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
        String soundKey = GuiActivator.getResources()
            .getSoundPath("WEBCAM_SNAPSHOT");
        
        SCAudioClip audio = GuiActivator.getAudioNotifier()
            .createAudio(soundKey);
        
        audio.play();
    }

    protected void close(boolean isEscaped)
    {
        this.videoContainer = null;
        dispose();
    }
    
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
        private static final int WIDTH = 30;
        private static final int HEIGHT = 30;
        
        private boolean isElapsed = false;
        private Font textFont = null;
        private String second;
        
        public TimerImage(String second)
        {
            Dimension d = new Dimension(WIDTH, HEIGHT);
            this.setPreferredSize(d);
            this.setMinimumSize(d);
            
            
            this.textFont = new Font("Sans", Font.BOLD, 20);
            this.second = second;
        }
        
        public void setElapsed()
        {
            this.isElapsed = true;
            this.repaint();
        }
        
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
