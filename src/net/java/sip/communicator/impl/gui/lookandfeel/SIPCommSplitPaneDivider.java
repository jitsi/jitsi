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
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;

/**
 * Divider used by SIPCommSplitPaneUI.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
class SIPCommSplitPaneDivider
    extends BasicSplitPaneDivider
    implements Skinnable
{
    /**
     * The horizontal divider.
     */
    private BufferedImage horizontalDivider
        = ImageLoader.getImage(ImageLoader.SPLITPANE_HORIZONTAL);

    /**
     * The vertical divider.
     */
    private BufferedImage verticalDivider
        = ImageLoader.getImage(ImageLoader.SPLITPANE_VERTICAL);

    /**
     * Creates an instance of <tt>SIPCommSplitPaneDivider</tt> by specifying the
     * <tt>BasicSplitPaneUI</tt> to use.
     *
     * @param ui the <tt>BasicSplitPaneUI</tt>
     */
    public SIPCommSplitPaneDivider(BasicSplitPaneUI ui)
    {
        super(ui);
        setLayout(new DividerLayout());
    }

    /**
     * Paints this split pane divider.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paint(Graphics g)
    {
        Rectangle clip = g.getClipBounds();

        if(getOrientationFromSuper() == JSplitPane.VERTICAL_SPLIT)
            g.drawImage(horizontalDivider,
                (getWidth() - horizontalDivider.getWidth(null))/2 ,
                clip.y + (getHeight() - horizontalDivider.getHeight(null))/2,
                horizontalDivider.getWidth(null),
                horizontalDivider.getHeight(null),
                null);
        else
            g.drawImage(verticalDivider,
                clip.x + (getWidth() - verticalDivider.getWidth(null))/2 ,
                (getHeight() - verticalDivider.getHeight(null))/2,
                verticalDivider.getWidth(null),
                verticalDivider.getHeight(null),
                null);

        super.paint(g);
    }

    /**
    * Creates and returns an instance of JButton that can be used to
    * collapse the left component in the metal split pane.
    *
    * @return the created button
    */
    @Override
    protected JButton createLeftOneTouchButton()
    {
        JButton b = new JButton()
        {
            // Sprite buffer for the arrow image of the left button
            int[][]     buffer = {{0, 0, 0, 2, 2, 0, 0, 0, 0},
                          {0, 0, 2, 1, 1, 1, 0, 0, 0},
                          {0, 2, 1, 1, 1, 1, 1, 0, 0},
                          {2, 1, 1, 1, 1, 1, 1, 1, 0},
                          {0, 3, 3, 3, 3, 3, 3, 3, 3}};

            @Override
            public void setBorder(Border b) {}

            @Override
            public void paint(Graphics g)
            {
                JSplitPane splitPane = getSplitPaneFromSuper();
                if(splitPane != null) {
                    int         oneTouchSize = getOneTouchSizeFromSuper();
                    int         orientation = getOrientationFromSuper();
                    int         blockSize = Math.min(getDividerSize(),
                                                     oneTouchSize);

                    // Initialize the color array
                    Color[]     colors = {
                            this.getBackground(),
                            MetalLookAndFeel.getPrimaryControlDarkShadow(),
                            MetalLookAndFeel.getPrimaryControlInfo(),
                            MetalLookAndFeel.getPrimaryControlHighlight()};

                    // Fill the background first ...
                    g.setColor(this.getBackground());
                    g.fillRect(0, 0, this.getWidth(),
                               this.getHeight());

                    // ... then draw the arrow.
                    if (getModel().isPressed())
                    {
                            // Adjust color mapping for pressed button state
                            colors[1] = colors[2];
                    }
                    if(orientation == JSplitPane.VERTICAL_SPLIT)
                    {
                            // Draw the image for a vertical split
                            for (int i=1; i<=buffer[0].length; i++)
                            {
                                for (int j=1; j<blockSize; j++)
                                {
                                        if (buffer[j-1][i-1] == 0)
                                                continue;
                                        else
                                        {
                                            g.setColor(
                                                colors[buffer[j-1][i-1]]);
                                        }

                                        g.drawLine(i, j, i, j);
                                }
                            }
                    }
                    else {
                        // Draw the image for a horizontal split
                        // by simply swaping the i and j axis.
                        // Except the drawLine() call this code is
                        // identical to the code block above. This was done
                        // in order to remove the additional orientation
                        // check for each pixel.
                        for (int i=1; i<=buffer[0].length; i++)
                        {
                            for (int j=1; j<blockSize; j++)
                            {
                                if (buffer[j-1][i-1] == 0)
                                {
                                        // Nothing needs
                                        // to be drawn
                                        continue;
                                }
                                else
                                {
                                        // Set the color from the
                                        // color map
                                        g.setColor(
                                        colors[buffer[j-1][i-1]]);
                                }
                                // Draw a pixel
                                g.drawLine(j, i, j, i);
                            }
                        }
                    }
                }
            }
        };

        b.setRequestFocusEnabled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        return b;
    }

    /**
    * Creates and return an instance of JButton that can be used to
    * collapse the right component in the metal split pane.
    */
    @Override
    protected JButton createRightOneTouchButton()
    {
        JButton b = new JButton()
        {
            // Sprite buffer for the arrow image of the right button
            int[][]     buffer = {{2, 2, 2, 2, 2, 2, 2, 2},
                          {0, 1, 1, 1, 1, 1, 1, 3},
                          {0, 0, 1, 1, 1, 1, 3, 0},
                          {0, 0, 0, 1, 1, 3, 0, 0},
                          {0, 0, 0, 0, 3, 0, 0, 0}};

        @Override
        public void setBorder(Border border) {}

            @Override
            public void paint(Graphics g)
            {
                JSplitPane splitPane = getSplitPaneFromSuper();
                if(splitPane != null) {
                    int         oneTouchSize = getOneTouchSizeFromSuper();
                    int         orientation = getOrientationFromSuper();
                    int         blockSize = Math.min(getDividerSize(),
                                                     oneTouchSize);

                    // Initialize the color array
                    Color[]     colors = {
                        this.getBackground(),
                        MetalLookAndFeel.getPrimaryControlDarkShadow(),
                        MetalLookAndFeel.getPrimaryControlInfo(),
                        MetalLookAndFeel.getPrimaryControlHighlight()};

                    // Fill the background first ...
                    g.setColor(this.getBackground());
                    g.fillRect(0, 0, this.getWidth(),
                               this.getHeight());

                    // ... then draw the arrow.
                    if (getModel().isPressed()) {
                            // Adjust color mapping for pressed button state
                            colors[1] = colors[2];
                    }
                    if(orientation == JSplitPane.VERTICAL_SPLIT)
                    {
                            // Draw the image for a vertical split
                        for (int i=1; i<=buffer[0].length; i++)
                        {
                            for (int j=1; j<blockSize; j++)
                            {
                                if (buffer[j-1][i-1] == 0)
                                {
                                        continue;
                                }
                                else
                                {
                                    g.setColor(
                                        colors[buffer[j-1][i-1]]);
                                }
                                g.drawLine(i, j, i, j);
                            }
                        }
                    }
                    else
                    {
                        // Draw the image for a horizontal split
                        // by simply swaping the i and j axis.
                        // Except the drawLine() call this code is
                        // identical to the code block above. This was done
                        // in order to remove the additional orientation
                        // check for each pixel.
                        for (int i=1; i<=buffer[0].length; i++)
                        {
                            for (int j=1; j<blockSize; j++)
                            {
                                if (buffer[j-1][i-1] == 0)
                                {
                                    // Nothing needs
                                    // to be drawn
                                    continue;
                                }
                                else
                                {
                                    // Set the color from the
                                    // color map
                                    g.setColor(
                                    colors[buffer[j-1][i-1]]);
                                }
                                // Draw a pixel
                                g.drawLine(j, i, j, i);
                            }
                        }
                    }
                }
            }
        };

        b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setRequestFocusEnabled(false);
        return b;
    }

    /**
    * Used to layout a SIPCommSplitPaneDivider. Layout for the divider
    * involves appropriately moving the left/right buttons around.
    * <p>
    * This inner class is marked &quot;public&quot; due to a compiler bug.
    * This class should be treated as a &quot;protected&quot; inner class.
    * Instantiate it only within subclasses of MetalSplitPaneDivider.
    */
    public class DividerLayout
        implements LayoutManager
    {
        public void layoutContainer(Container c)
        {
            if (leftButton != null && rightButton != null &&
                c == SIPCommSplitPaneDivider.this) {

              Dimension leftSize = leftButton.getPreferredSize();
              Dimension rightSize = rightButton.getPreferredSize();

              if (splitPane.isOneTouchExpandable())
              {
                  Insets insets = getInsets();

                  if (insets == null)
                      insets = new Insets(0 , 0, 0, 0);

                    if (orientation == JSplitPane.VERTICAL_SPLIT)
                    {
                        int blockSize = getDividerSize()
                            - (insets.left + insets.right);
                        int y = (c.getHeight() - blockSize) / 2;
                        leftButton.setBounds(insets.left + leftSize.width, y,
                                leftSize.width, leftSize.height);
                        rightButton.setBounds((insets.left * 2) + leftSize.width
                                + rightSize.width, y, rightSize.width,
                                rightSize.height);
                    }
                    else
                    {
                        int blockSize = getDividerSize()
                            - (insets.top + insets.bottom);
                        int x = (c.getWidth() - blockSize) / 2;
                        leftButton.setBounds(x, insets.top + leftSize.height,
                                leftSize.width, leftSize.height);
                        rightButton.setBounds(x,
                            (insets.top * 2) + leftSize.height + rightSize.height,
                            rightSize.width, rightSize.height);
                    }
                }
                else
                {
                    leftButton.setBounds(-5, -5, 1, 1);
                    rightButton.setBounds(-5, -5, 1, 1);
                }
            }
        }

        /**
         * Returns the minimum layout size.
         *
         * @param c the container for which to return a minimum size
         * @return the preferred layout size
         */
        public Dimension minimumLayoutSize(Container c)
        {
            return new Dimension(0,0);
        }

        /**
         * Returns the preferred layout size.
         *
         * @param c the container for which to return a preferred size
         * @return the preferred layout size
         */
        public Dimension preferredLayoutSize(Container c)
        {
            return new Dimension(0, 0);
        }

        public void removeLayoutComponent(Component c) {}

        public void addLayoutComponent(String string, Component c) {}
    }

    /*
    * The following methods only exist in order to be able to access protected
    * members in the superclass, because these are otherwise not available
    * in any inner class.
    */

    /**
     * Returns the one touch size.
     *
     * @return the one touch size
     */
    int getOneTouchSizeFromSuper()
    {
        return BasicSplitPaneDivider.ONE_TOUCH_SIZE;
    }

    /**
     * Returns the one touch offset.
     *
     * @return the one touch offset
     */
    int getOneTouchOffsetFromSuper()
    {
        return BasicSplitPaneDivider.ONE_TOUCH_OFFSET;
    }

    /**
     * Returns the orientation.
     *
     * @return the orientation
     */
    int getOrientationFromSuper()
    {
        return super.orientation;
    }

    /**
     * Returns the split pane.
     *
     * @return the split pane
     */
    JSplitPane getSplitPaneFromSuper()
    {
        return super.splitPane;
    }

    /**
     * Returns the left button.
     *
     * @return the left button
     */
    JButton getLeftButtonFromSuper()
    {
        return super.leftButton;
    }

    /**
     * Returns the right button.
     *
     * @return the right button
     */
    JButton getRightButtonFromSuper()
    {
        return super.rightButton;
    }

    /**
     * Reloads buffered images.
     */
    public void loadSkin()
    {
        horizontalDivider
            = ImageLoader.getImage(ImageLoader.SPLITPANE_HORIZONTAL);
        verticalDivider
            = ImageLoader.getImage(ImageLoader.SPLITPANE_VERTICAL);
    }
}
