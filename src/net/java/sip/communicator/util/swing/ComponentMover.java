/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class ComponentMover
{
    /**
     * Registers the given component for component dragging/moving functionality.
     *
     * @param c the component, which should be moved on drag
     */
    public static void registerComponent(Component c)
    {
        MoverMouseListener l = new MoverMouseListener();

        c.addMouseListener(l);
        c.addMouseMotionListener(l);
    }

    /**
     * The Mouse listener for local video. It is responsible for dragging local
     * video.
     */
    private static class MoverMouseListener
        implements  MouseListener,
                    MouseMotionListener
    {
        /**
         * Indicates if we're currently during a drag operation.
         */
        private boolean inDrag = false;

        /**
         * The previous x coordinate of the drag.
         */
        private int previousX = 0;

        /**
         * The previous y coordinate of the drag.
         */
        private int previousY = 0;

        /**
         * Indicates that the mouse has been dragged.
         *
         * @param event the <tt>MouseEvent</tt> that notified us
         */
        public void mouseDragged(MouseEvent event)
        {
            Point p = event.getPoint();

            if (inDrag)
            {
                Component c = (Component) event.getSource();

                int newX = c.getX() + p.x - previousX;
                int newY = c.getY() + p.y - previousY;

                c.setLocation(newX, newY);
            }
        }

        public void mouseMoved(MouseEvent event) {}

        public void mouseClicked(MouseEvent event) {}

        public void mouseEntered(MouseEvent event) {}

        public void mouseExited(MouseEvent event) {}

        /**
         * Indicates that the mouse has been pressed.
         *
         * @param event the <tt>MouseEvent</tt> that notified us
         */
        public void mousePressed(MouseEvent event)
        {
            Point p = event.getPoint();

            previousX = p.x;
            previousY = p.y;
            inDrag = true;
        }

        /**
         * Indicates that the mouse has been released.
         *
         * @param event the <tt>MouseEvent</tt> that notified us
         */
        public void mouseReleased(MouseEvent event)
        {
            inDrag = false;
            previousX = 0;
            previousY = 0;
        }
    }
}
