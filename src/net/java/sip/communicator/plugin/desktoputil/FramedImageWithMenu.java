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

import javax.swing.*;
import javax.swing.event.*;

import org.jvnet.lafwidget.animation.*;

/**
 * A custom component, used to show images in a frame. A rollover for the
 * content image and optional menu in dialog.
 *
 * @author Damien Roth
 */
public class FramedImageWithMenu
    extends FramedImage
    implements MouseListener, PopupMenuListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The dialog containing the menu with actions.
     */
    private JPopupMenu popupMenu;

    /**
     * Should we currently draw overlay.
     */
    private boolean drawOverlay = false;

    /**
     * Are we showing custom image or the default one.
     */
    private boolean isDefaultImage = true;

    /**
     * The current image.
     */
    private Image currentImage;

    /**
     * Creates the component.
     * @param imageIcon the image icon to show as default one.
     * @param width width of component.
     * @param height height of component.
     */
    public FramedImageWithMenu(
        ImageIcon imageIcon,
        int width,
        int height)
    {
        super(imageIcon, width, height);

        this.addMouseListener(this);
    }

    /**
     * Sets the dialog used for menu for this Image.
     * @param popupMenu the dialog to show as menu. Can be null if no menu
     *        will be available.
     */
    public void setPopupMenu(JPopupMenu popupMenu)
    {
        this.popupMenu = popupMenu;
        if(popupMenu != null)
            this.popupMenu.addPopupMenuListener(this);
    }

    /**
     * Sets the image to display in the frame.
     *
     * @param imageIcon the image to display in the frame
     */
    public void setImageIcon(ImageIcon imageIcon)
    {
        // Intercept the action to validate the user icon and not the default
        super.setImageIcon(imageIcon.getImage());
        this.isDefaultImage = false;

        this.currentImage = imageIcon.getImage();
    }

    /**
     * Sets the image to display in the frame.
     *
     * @param image the image to display in the frame
     */
    public void setImageIcon(byte[] image)
    {
        // Intercept the action to validate the user icon and not the default
        super.setImageIcon(image);
        this.isDefaultImage = false;

        this.currentImage = super.getImage();
    }

    /**
     * Returns the current image with no rounded corners. Only return the user
     * image and not the default image.
     *
     * @return the current image - null if it's the default image
     */
    public Image getAvatar()
    {
        return (!this.isDefaultImage) ? this.currentImage : this.getImage();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (drawOverlay)
        {
            g = g.create();
            AntialiasingManager.activateAntialiasing(g);

            try
            {
                // Paint a roll over fade out.
                FadeTracker fadeTracker = FadeTracker.getInstance();

                float visibility = 0.0f;
                if (fadeTracker.isTracked(this, FadeKind.ROLLOVER))
                {
                    visibility = fadeTracker.getFade(this, FadeKind.ROLLOVER);
                    visibility /= 4;
                }
                else
                    visibility = 0.5f;

                // Draw black overlay
                g.setColor(new Color(0.0f, 0.0f, 0.0f, visibility));
                g.fillRoundRect(1, 1, width - 2, height - 2, 10, 10);

                // Draw arrow
                g.setColor(Color.WHITE);

                int[] arrowX = new int[] {
                        width - 17,
                        width - 7,
                        width - 12
                };
                int[] arrowY = new int[] {
                        height - 12,
                        height - 12,
                        height - 7
                };
                g.fillPolygon(arrowX, arrowY, arrowX.length);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Show the avatar dialog as a glasspane of the mainframe
     *
     * @param show show dialogs if sets to TRUE - hide otherwise
     */
    private void showDialog(MouseEvent e, boolean show)
    {
        if (this.popupMenu == null)
        {
            return;
        }

        if (show)
        {
            Point imageLoc = this.getLocationOnScreen();
            Point rootPaneLoc = this.getRootPane().getLocationOnScreen();

            this.popupMenu.setSize(this.getRootPane().getWidth(),
                    this.popupMenu.getHeight());

            this.popupMenu.show(this, (rootPaneLoc.x - imageLoc.x),
                    this.getHeight());
        }
        else
        {
            this.drawOverlay = false;
            this.repaint();
        }
    }

    public void mouseEntered(MouseEvent e)
    {
        if (this.popupMenu == null)
        {
            return;
        }

        if (this.drawOverlay || !this.isEnabled())
            return;

        this.drawOverlay = true;

        FadeTracker fadeTracker = FadeTracker.getInstance();

        fadeTracker.trackFadeIn(FadeKind.ROLLOVER,
            FramedImageWithMenu.this,
            true,
            new AvatarRepaintCallback());
    }

    public void mouseExited(MouseEvent e)
    {
        if (this.popupMenu == null)
        {
            return;
        }

        // Remove overlay only if the dialog isn't visible
        if (!popupMenu.isVisible() && this.isEnabled())
        {
            this.drawOverlay = false;
            this.repaint();
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        if (this.popupMenu == null)
        {
            return;
        }

        if (this.isEnabled())
            showDialog(e, !popupMenu.isVisible());
    }

    /**
     * This method is called before the popup menu becomes visible
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

    /**
     * This method is called before the popup menu becomes invisible
     * Note that a JPopupMenu can become invisible any time
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        this.drawOverlay = false;
        this.repaint();
    }

    /**
     * This method is called when the popup menu is canceled
     */
    public void popupMenuCanceled(PopupMenuEvent e){}

    /**
     * The <tt>ButtonRepaintCallback</tt> is charged to repaint this button
     * when the fade animation is performed.
     */
    private class AvatarRepaintCallback
        implements FadeTrackerCallback
    {
        public void fadeEnded(FadeKind arg0)
        {
            repaintLater();
        }

        public void fadePerformed(FadeKind arg0, float arg1)
        {
            repaintLater();
        }

        private void repaintLater()
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    FramedImageWithMenu.this.repaint();
                }
            });
        }

        public void fadeReversed(FadeKind arg0, boolean arg1, float arg2)
        {
        }
    }

    public void mouseClicked(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {}
}
