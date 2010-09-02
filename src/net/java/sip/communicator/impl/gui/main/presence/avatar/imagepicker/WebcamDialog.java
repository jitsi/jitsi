/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.audionotifier.*;
//import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A dialog showing the webcam and allowing the user to grap a snapshot 
 *
 * @author Damien Roth
 */
public class WebcamDialog
    extends SIPCommDialog
    implements ActionListener
//        , VideoListener
{
    private VideoContainer videoContainer;
    private Component videoComponent = null;
    
    private JButton cancelButton;
    private JButton grabSnapshot;
    
    private byte[] grabbedImage = null;
    
    private TransparentPanel southPanel;
    
    private TransparentPanel timerPanel;
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

        this.cancelButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CANCEL"));
        this.cancelButton.setName("cancel");
        this.cancelButton.addActionListener(this);
        
        initAccessWebcam();


        // Timer Panel
        this.timerPanel = new TransparentPanel();
        this.timerPanel.setLayout(new GridLayout(0, 3));
        
        TransparentPanel tp;
        for (int i = 0; i < this.timerImages.length; i++)
        {
            this.timerImages[i] = new TimerImage("" + (3-i));
            
            tp = new TransparentPanel();
            tp.add(this.timerImages[i], BorderLayout.CENTER);
            
            this.timerPanel.add(tp);
        }
        
        TransparentPanel buttonsPanel = new TransparentPanel(new GridLayout(1, 2));
        buttonsPanel.add(this.grabSnapshot);
        buttonsPanel.add(this.cancelButton);
        
        // South Panel
        this.southPanel = new TransparentPanel(new BorderLayout());
        this.southPanel.add(this.timerPanel, BorderLayout.CENTER);
        this.southPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.add(this.videoContainer, BorderLayout.CENTER);
        this.add(this.southPanel, BorderLayout.SOUTH);   
    }

    /**
     * Init the access to the webcam (asynchonous call)
     */
    private void initAccessWebcam()
    {
        Dimension d = new Dimension(320, 240);
        
        // Create a container for the video
        this.videoContainer = new VideoContainer(new JLabel(
                GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.INITIALIZING"),
                JLabel.CENTER));
        this.videoContainer.setPreferredSize(d);
        this.videoContainer.setMinimumSize(d);
        this.videoContainer.setMaximumSize(d);
        
//        try
//        {
//            // Call the method in the media service
//            GuiActivator.getMediaService().createLocalVideoComponent(this);
//        } catch (MediaException e)
//        {
//            //todo: In what scenarios are exceptions thrown and how to manage them?
//            this.videoContainer = new VideoContainer(new JLabel(
//                    GuiActivator.getResources()
//                    .getI18NString("service.gui.avatar.imagepicker.WEBCAM_ERROR")));
//            e.printStackTrace();
//        }
    }
    
    /**
     * Grap the current image of the webcam throught the MediaService
     */
    private void grabSnapshot()
    {
        // Just call the method "grabSnapshot" from the MediaService with the component
//        try
//        {
//            this.grabbedImage = GuiActivator.getMediaService()
//                .grabSnapshot(this.videoComponent);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
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
//        try
//        {
//            if (this.videoComponent != null)
//            {
//                GuiActivator.getMediaService()
//                    .disposeLocalVideoComponent(this.videoComponent);
//                this.videoComponent = null;
//            }
//        }
//        catch (MediaException e)
//        {
//            // Better manager the exception !
//            e.printStackTrace();
//        }
    }
    
    public void videoAdded(VideoEvent event)
    {
        // Here is the important part. With this event, you get the component
        // containing the video !

        // You need to keep it. The returned componant is the key for all
        // the other methods !
        this.videoComponent = event.getVisualComponent();

        // Add the component in the container
        this.videoContainer.add(this.videoComponent);

        this.grabSnapshot.setEnabled(true);
    }

    public void videoRemoved(VideoEvent event)
    {
        // In case of the video is removed elsewhere
        this.videoComponent = null;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String actName = ((JButton) e.getSource()).getName();
        
        if (actName.equals("grab"))
        {
            if (this.videoComponent == null)
                return;
            
            this.grabSnapshot.setEnabled(false);
            new SnapshotTimer().start();
        }
        else
        {
            close(false);
            this.setVisible(false);
        }
    }
    
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
                    e.printStackTrace();
                }
            }
            
            playSound();
            grabSnapshot();
            
            WebcamDialog.this.setVisible(false);
            WebcamDialog.this.dispose();

        }
    }
    
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
