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
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.java.sip.communicator.util.skin.*;

/**
 * The SIPCommScrollBarUI implementation.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommScrollBarUI
    extends MetalScrollBarUI
    implements Skinnable
{
    /**
     * The horizontal thumb image.
     */
    private BufferedImage horizontalThumb;

    /**
     * The vertical thumb image.
     */
    private BufferedImage verticalThumb;

    /**
     * The horizontal thumb handle image.
     */
    private BufferedImage horizontalThumbHandle;

    /**
     * The vertical thumb handle image.
     */
    private BufferedImage verticalThumbHandle;

    /**
     * Creates an instance of <tt>SIPCommScrollBarUI</tt>.
     */
    public SIPCommScrollBarUI()
    {
        loadSkin();
    }

    /**
     * Creates the UI for the given <tt>JComponent</tt>.
     *
     * @param c the <tt>JComponent</tt>, for which to create an UI
     * @return the component UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommScrollBarUI();
    }

    /**
     * Paints the track of the scroll bar.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     * @param c the component to paint a track for
     * @param trackBounds the bounds of the track to paint
     */
    @Override
    protected void paintTrack( Graphics g, JComponent c, Rectangle trackBounds)
    {
        g.translate( trackBounds.x, trackBounds.y );

        boolean leftToRight = c.getComponentOrientation().isLeftToRight();

        if ( scrollbar.getOrientation() == JScrollBar.VERTICAL )
        {
            if ( !isFreeStanding )
            {
                trackBounds.width += 2;

                if ( !leftToRight )
                    g.translate( -1, 0 );
            }

            g.setColor(this.trackColor);
            g.fillRect(0, 0, trackBounds.width-2, trackBounds.height);

            g.setColor(this.trackHighlightColor);
            g.drawRect(0, 0, trackBounds.width-2, trackBounds.height);

            if ( !isFreeStanding )
            {
                trackBounds.width -= 2;

                if ( !leftToRight )
                    g.translate( 1, 0 );
            }
        }
        else  // HORIZONTAL
        {
            if ( !isFreeStanding )
            {
                trackBounds.height += 2;
            }

            g.setColor(this.trackColor);
            g.fillRect(0, 0, trackBounds.width, trackBounds.height-2);

            g.setColor(this.trackHighlightColor);
            g.drawRect(0, 0, trackBounds.width, trackBounds.height-2);

            if ( !isFreeStanding )
            {
                trackBounds.height -= 2;
            }
        }
        g.translate( -trackBounds.x, -trackBounds.y );
    }

    /**
     * Paints the thumb of the scroll bar.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     * @param c the component to paint a thumb for
     * @param thumbBounds the bounds of the thumb to paint
     */
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
    {
        if (!c.isEnabled()) {
            return;
        }

        boolean leftToRight = c.getComponentOrientation().isLeftToRight();

        g.translate( thumbBounds.x, thumbBounds.y );

        int imgWidth;
        int imgHeight;
        int indentWidth  = 10;

        if(scrollbar.getOrientation() == JScrollBar.VERTICAL)
        {
            if(!isFreeStanding)
            {
                thumbBounds.width += 2;

                if ( !leftToRight )
                {
                    g.translate( -1, 0 );
                }
            }

            imgWidth = verticalThumb.getWidth();
            imgHeight = verticalThumb.getHeight();

            Image topImage
                = verticalThumb.getSubimage(0, 0,
                                            imgWidth,
                                            indentWidth);
            Image middleImage
                = verticalThumb.getSubimage(0, indentWidth,
                                            imgWidth,
                                            imgHeight-2*indentWidth);
            Image bottomImage
                = verticalThumb.getSubimage(0, imgHeight-indentWidth,
                                            imgWidth, indentWidth);

            g.drawImage(topImage, 0, 0,
                    thumbBounds.width-2, indentWidth , null);

            g.drawImage(middleImage, thumbBounds.x, indentWidth,
                    thumbBounds.width-2,
                    thumbBounds.height-indentWidth , null);

            g.drawImage(bottomImage, thumbBounds.x,
                    thumbBounds.height-indentWidth,
                    thumbBounds.width-2, indentWidth, null);

            g.drawImage(verticalThumbHandle,
                        thumbBounds.width/2-verticalThumbHandle.getWidth()/2,
                        thumbBounds.height/2-verticalThumbHandle.getHeight()/2,
                        verticalThumbHandle.getWidth(),
                        verticalThumbHandle.getHeight(), null);

            if (!isFreeStanding)
            {
                thumbBounds.width -= 2;
                if(!leftToRight)
                {
                    g.translate( 1, 0 );
                }
            }
        }
        else  // HORIZONTAL
        {
            if (!isFreeStanding)
                thumbBounds.height += 2;

            imgWidth = horizontalThumb.getWidth();
            imgHeight = horizontalThumb.getHeight();

            Image leftImage
                = horizontalThumb.getSubimage(0, 0,
                                            indentWidth, imgHeight);
            Image middleImage
                = horizontalThumb.getSubimage(indentWidth, 0,
                                            imgWidth-2*indentWidth,
                                            imgHeight);
            Image rightImage
                = horizontalThumb.getSubimage(imgWidth-indentWidth, 0,
                                            indentWidth,
                                            imgHeight);

            g.drawImage(leftImage, 0, 0,
                    indentWidth, thumbBounds.height-2, null);

            g.drawImage(middleImage, indentWidth, thumbBounds.y,
                    thumbBounds.width-indentWidth,
                    thumbBounds.height-2 , null);

            g.drawImage(rightImage, thumbBounds.width-indentWidth, thumbBounds.y,
                    indentWidth, thumbBounds.height-2, null);

            g.drawImage(horizontalThumbHandle,
                    thumbBounds.width/2-horizontalThumbHandle.getWidth()/2,
                    thumbBounds.height/2-horizontalThumbHandle.getHeight()/2,
                    horizontalThumbHandle.getWidth(),
                    horizontalThumbHandle.getHeight(), null);

            if (!isFreeStanding)
                thumbBounds.height -= 2;
        }
        g.translate(-thumbBounds.x, -thumbBounds.y);
    }

    /**
     * Returns the minimum scroll thumb size.
     *
     * @return the minimum scroll thumb size
     */
    @Override
    protected Dimension getMinimumThumbSize()
    {
        if(scrollbar.getOrientation() == JScrollBar.VERTICAL)
            return new Dimension(   scrollBarWidth,
                                    verticalThumbHandle.getHeight()+4);
        else
            return new Dimension(   horizontalThumbHandle.getWidth()+4,
                                    scrollBarWidth);
    }

    /**
     * Loads UI resources.
     */
    public void loadSkin()
    {
        horizontalThumb         = (BufferedImage)UIManager
                                    .get("ScrollBar.horizontalThumbIcon");
        verticalThumb           = (BufferedImage)UIManager
                                    .get("ScrollBar.verticalThumbIcon");
        horizontalThumbHandle   = (BufferedImage)UIManager
                                    .get("ScrollBar.horizontalThumbHandleIcon");
        verticalThumbHandle     = (BufferedImage)UIManager
                                    .get("ScrollBar.verticalThumbHandleIcon");
    }

}
