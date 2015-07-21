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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

/**
 * AnimatedImage will display a series of Images in a predetermined sequence.
 * This sequence can be configured to keep repeating or stop after a specified
 * number of cycles.
 * 
 * An AnimatedImage cannot be shared by different components. However,
 * the Images added to the AnimatedImage can be shared.
 * 
 * The animation sequence is a simple sequential display of each Image. When the
 * end is reached the animation restarts at the first Image. Images are
 * displayed in the order in which they were added. To create custom animation
 * sequences you will need to override the getNextIconIndex() and
 * isCycleCompleted() methods.
 * 
 * @author Marin Dzhigarov
 */
public class AnimatedImage
    extends BufferedImage
    implements ActionListener
{
    /**
     * The default delay between switching over the next image.
     */
    private final static int DEFAULT_DELAY = 500;

    /**
     * The default number of cycles for the animation.
     * -1 means that the animation will go on forever.
     */
    private final static int DEFAULT_CYCLES = -1;

    /**
     * The list of actual images that are going to be displayed
     */
    private List<Image> images = new ArrayList<Image>();

    /**
     * The JComponent that will display the Image
     */
    private JComponent component;

    /**
     * The number of cycles for the animation.
     * -1 means that the animation will go on forever.
     */
    private int cycles;

    private boolean showFirstImage = false;

    private int imageWidth;

    private int imageHeight;

    /**
     * The index of the currently displayed Image
     */
    private int currentImageIndex;

    /**
     * The current number of completed cycles. The animation will go on while
     * cyclesCompleted < cycles.
     */
    private int cyclesCompleted;

    private boolean animationFinished = true;

    /**
     * The Timer that handles switching between Images.
     */
    private Timer timer;

    /**
     * The Graphics2D object used for painting.
     */
    private final Graphics2D g2d;

    /**
     * Create an AnimatedImage that will continuously cycle with the
     * default (500ms).
     *
     * @param component  the component the image will be painted on
     * @param images     the Images to be painted as part of the animation
     */
    public AnimatedImage(JComponent component, Image... images)
    {
        this(component, DEFAULT_DELAY, images);
    }

    /**
     * Create an AnimatedImage that will continuously cycle with the specified
     * delay
     *
     * @param component  the component the image will be painted on
     * @param delay      the delay between painting each image, in milli seconds
     * @param images     the Images to be painted as part of the animation
     */
    public AnimatedImage(JComponent component, int delay, Image... images)
    {
        this(component, delay, DEFAULT_CYCLES, images);
    }

    /**
     * Create an AnimatedImage specifying the required delay between painting
     * each image and the number of times to repeat the animation sequence
     *
     * @param component  the component the image will be painted on
     * @param delay      the delay between painting each image, in milli seconds
     * @param cycles     the number of times to repeat the animation sequence
     * @param images     the Images to be painted as part of the animation
     */
    public AnimatedImage(
        JComponent component, int delay, int cycles, Image... images)
    {
        super(  images[0].getWidth(null),
                images[0].getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        this.component = component;

        g2d = (Graphics2D) getGraphics();

        setCycles( cycles );

        for (int i = 0; i < images.length; i++)
        {
            if (images[i] == null)
            {
                throw new IllegalArgumentException("Images can not be null");
            }
            else
            {
                addImage(images[i]);
            }
        }

        timer = new Timer(delay, this);
    }

    /**
     * Add Image to be used in the animation.
     *
     * @param image  the image to be added
     */
    public void addImage(Image image)
    {
        if (image != null)
        {
            this.images.add(image);
            calculateImageDimensions();
        }
    }

    /**
     * Calculate the width and height of the Image based on the maximum
     * width and height of any individual Image.
     */
    private void calculateImageDimensions()
    {
        imageWidth = 0;
        imageHeight = 0;

        for (Image image : images)
        {
            imageWidth = Math.max(imageWidth, image.getWidth(null));
            imageHeight = Math.max(imageHeight, image.getHeight(null));
        }
    }

    /**
     * Get the index of the currently visible Image
     *
     * @return the index of the Icon
     */
    public int getCurrentImageIndex()
    {
        return currentImageIndex;
    }

    /**
     * Set the index of the Image to be displayed and then repaint the Image.
     *
     * @param index  the index of the Image to be displayed
     */
    public void setCurrentImageIndex(int index)
    {
        currentImageIndex = index;

        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC);
        g2d.setComposite(ac);
        g2d.drawImage(getImage(currentImageIndex), 0, 0, component);

        component.repaint();
    }

    /**
     * Get the remaining cycles to complete before the animation stops.
     *
     * @return the number of remaining cycles
     */
    public int getRemainingCycles()
    {
        return cycles;
    }

    /**
     * Specify the number of times to repeat each animation sequence, or cycle.
     *
     * @param cycles the number of cycles to complete before the animation
     *                stops. The default is -1, which means the animation is
     *                continuous.
     */
    public void setCycles(int cycles)
    {
        this.cycles = cycles;
    }

    /**
     * Returns the delay between painting each Image
     *
     * @return the delay
     */
    public int getDelay()
    {
        return timer.getDelay();
    }

    /**
     * Specify the delay between painting each Image
     *
     * @param delay  the delay between painting each Image (in milli seconds)
     */
    public void setDelay(int delay)
    {
        timer.setDelay(delay);
    }

    /**
     * Returns the Image at the specified index.
     *
     * @param index the index of the Image to be returned
     * @return the Image at the specifed index
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public Image getImage(int index)
    {
        return images.get( index );
    }

    /**
     * Returns the number of Images that are contained in the animation
     *
     * @return the total number of Images
     */
    public int getImagesCount()
    {
        return images.size();
    }

    /**
     * Get the showFirstImage
     *
     * @return the showFirstImage value
     */
    public boolean isShowFirstImage()
    {
        return showFirstImage;
    }

    /**
     * Displays the first image of the animation after the last cycle has passed
     * If set to false, the last image will remain set after the last
     * animation cycle.
     *
     * @param showFirstImage true when the first image is to be displayed,
     *                       false otherwise
     */
    public void setShowFirstImage(boolean showFirstImage)
    {
        this.showFirstImage = showFirstImage;
    }

    /**
     * Pauses the animation. The animation can be restarted from the
     * current Image using the restart() method.
     */
    public void pause()
    {
        timer.stop();
    }

    /**
     * Start the animation from the beginning.
     */
    public void start()
    {
        if (!timer.isRunning())
        {
            setCurrentImageIndex(0);
            animationFinished = false;
            cyclesCompleted = 0;
            timer.start();
        }
    }

    /**
     * Restarts the animation from where the animation was paused. Or, if the
     * animation has finished, it will be restarted from the beginning.
     */
    public void restart()
    {
        if (!timer.isRunning())
        {
            if (animationFinished)
                start();
            else
                timer.restart();
        }
    }

    /**
     * Stops the animation. The first icon will be redisplayed.
     */
    public void stop()
    {
        timer.stop();
        setCurrentImageIndex(0);
        animationFinished = true;
    }

    /**
     * Gets the width of this image.
     *
     * @return the width of the image in pixels.
     */
    public int getWidth(ImageObserver obs)
    {
        return imageWidth;
    }

    /**
     *  Gets the height of this image.
     *
     *  @return the height of the image in pixels.
     */
    public int getHeight(ImageObserver obs)
    {
        return imageHeight;
    }

    /**
     * Controls the image animation that is scheduled by the Timer
     */
    public void actionPerformed(ActionEvent e)
    {
        // Display the next Image in the animation sequence
        setCurrentImageIndex(
            getNextImageIndex(currentImageIndex, images.size()));

        // Track the number of cycles that have been completed
        if (isCycleCompleted(currentImageIndex, images.size()))
        {
            cyclesCompleted++;
        }

        // Stop the animation when the specified number of cycles is completed
        if (cycles > 0
        &&  cycles <= cyclesCompleted)
        {
            timer.stop();
            animationFinished = true;

            // Display the first Image when required
            if (isShowFirstImage()
            &&  getCurrentImageIndex() != 0)
            {
                new Thread(new Runnable() {

                    @Override
                    public void run()
                    {
                        // Wait one more delay interval before displaying the
                        // first Image
                        try
                        {
                            Thread.sleep( timer.getDelay() );
                            setCurrentImageIndex(0);
                        }
                        catch(InterruptedException e) {}
                    }
                    
                }).start();
            }
        }
    }

    /**
     * Gets the index of the next Image to be displayed. Normally, images will
     * be displayed in the order they were added to the AnimatedImage.
     * If however, a custom animation sequence is required one can extend this
     * method and achieve a greater control over the animation sequence.
     *
     *  @param currentIndex the index of the Image currently displayed
     *  @param imageCount the number of Images to be displayed
     *  @return the index of the next Image to be displayed
     */
    protected int getNextImageIndex(int currentIndex, int imageCount)
    {
        return ++currentIndex % imageCount;
    }

    /**
     * Checks if the currently displayed Image is the last image of the
     * animation sequence. This marks the completion of a single animation
     * cycle.
     * If a custom animation sequence is required one can extend this
     * method to achieve a greater control over the animation sequence.
     *
     *  @param currentIndex  the index of the Image currently displayed
     *  @param imageCount the number of Images to be displayed
     *  @return  the index of the next Image to be displayed
     */
    protected boolean isCycleCompleted(int currentIndex, int imageCount)
    {
        return currentIndex == imageCount - 1;
    }
}
