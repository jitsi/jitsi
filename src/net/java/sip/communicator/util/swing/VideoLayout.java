/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.util.*;

import javax.swing.*;

/**
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class VideoLayout extends FitLayout
{
    /**
     * The video canvas constraint.
     */
    public static final String CANVAS = "CANVAS";

    /**
     * The center remote video constraint.
     */
    public static final String CENTER_REMOTE = "CENTER_REMOTE";

    /**
     * The east remote video constraint.
     */
    public static final String EAST_REMOTE = "EAST_REMOTE";

    /**
     * The local video constraint.
     */
    public static final String LOCAL = "LOCAL";

    /**
     * The close local video constraint.
     */
    public static final String CLOSE_LOCAL_BUTTON = "CLOSE_LOCAL_BUTTON";

    /**
     * The ration between the local and the remote video.
     */
    private static final float LOCAL_TO_REMOTE_RATIO = 0.30f;

    /**
     * The video canvas.
     */
    private Component canvas;

    /**
     * The close local video button component.
     */
    private Component closeButton;

    /**
     * The map of component constraints.
     */
    private final HashMap<Component, Object> constraints
        = new HashMap<Component, Object>();

    /**
     * The component containing the local video.
     */
    private Component local;

    /**
     * The component containing the remote video.
     */
    private Component remote;

    /**
     * The x coordinate alignment of the remote video.
     */
    private float remoteAlignmentX = Component.CENTER_ALIGNMENT;

    /**
     * Adds the given component in this layout on the specified by name
     * position.
     *
     * @param name the constraint giving the position of the component in this
     * layout
     * @param comp the component to add
     */
    @Override
    public void addLayoutComponent(String name, Component comp)
    {
        super.addLayoutComponent(name, comp);

        synchronized (constraints)
        {
            this.constraints.put(comp, name);
        }

        if ((name == null) || name.equals(CENTER_REMOTE))
        {
            remote = comp;
            remoteAlignmentX = Component.CENTER_ALIGNMENT;
        }
        else if (name.equals(EAST_REMOTE))
        {
            remote = comp;
            remoteAlignmentX = Component.RIGHT_ALIGNMENT;
        }
        else if (name.equals(LOCAL))
            local = comp;
        else if (name.equals(CLOSE_LOCAL_BUTTON))
            closeButton = comp;
        else if (name.equals(CANVAS))
            canvas = comp;
    }

    /**
     * Returns the remote video component.
     *
     * @return the remote video component
     */
    @Override
    protected Component getComponent(Container parent)
    {
        return getRemote();
    }

    /**
     * Returns the constraints for the given component.
     * 
     * @param c the component for which constraints we're looking for
     * @return the constraints for the given component
     */
    public Object getComponentConstraints(Component c)
    {
        synchronized (constraints)
        {
            return constraints.get(c);
        }
    }

    /**
     * Returns the local video component.
     *
     * @return the local video component
     */
    public Component getLocal()
    {
        return local;
    }

    /**
     * Returns the remote video component.
     *
     * @return the remote video component
     */
    public Component getRemote()
    {
        return remote;
    }

    /**
     * Returns the local video close button.
     *
     * @return the local video close button
     */
    public Component getLocalCloseButton()
    {
        return closeButton;
    }

    /**
     * Lays out this given container.
     *
     * @param parent the container to lay out
     */
    public void layoutContainer(Container parent)
    {
        Component local = getLocal();

        super.layoutContainer(parent,
            (local == null) ? Component.CENTER_ALIGNMENT : remoteAlignmentX);
        
        Dimension parentSize = parent.getSize();

        if (local != null)
        {
            int height = Math.round(parentSize.height * LOCAL_TO_REMOTE_RATIO);
            int width = Math.round(parentSize.width * LOCAL_TO_REMOTE_RATIO);

            int localX;
            int localY;

            /*
             * XXX The remote Component being a JLabel is meant to signal that
             * there is no remote video and the remote is the photoLabel.
             */
            if (remote instanceof JLabel)
            {
                localX = parentSize.width/2 - width/2;
                localY = parentSize.height - height;

                super.layoutComponent(
                    local,
                    new Rectangle(
                        localX,
                        localY,
                        width,
                        height),
                    Component.CENTER_ALIGNMENT,
                    Component.BOTTOM_ALIGNMENT);
            }
            else
            {
                localX = remote.getX() + 5;
                localY = parentSize.height - height - 5;

                super.layoutComponent(
                    local,
                    new Rectangle(
                        localX,
                        localY,
                        width,
                        height),
                    Component.LEFT_ALIGNMENT,
                    Component.BOTTOM_ALIGNMENT);
            }

            if (closeButton != null)
            {
                super.layoutComponent(
                    closeButton,
                    new Rectangle(
                        (localX + local.getWidth() - closeButton.getWidth()),
                        localY,
                        closeButton.getWidth(),
                        closeButton.getHeight()),
                        Component.CENTER_ALIGNMENT,
                        Component.CENTER_ALIGNMENT);
            }
        }

        if (canvas != null)
        {
            /*
             * The video canvas will get the locations of the other components
             * to paint so it has to cover the parent completely.
             */
            canvas.setBounds(0, 0, parentSize.width, parentSize.height);
        }
    }

    /**
     * Returns the minimum layout size for the given container.
     *
     * @param parent the container which minimum layout size we're looking for
     * @return a Dimension containing, the minimum layout size for the given
     * container
     */
    public Dimension minimumLayoutSize(Container parent)
    {
        return super.minimumLayoutSize(parent);
    }

    /**
     * Returns the preferred layout size for the given container.
     *
     * @param parent the container which preferred layout size we're looking for
     * @return a Dimension containing, the preferred layout size for the given
     * container
     */
    public Dimension preferredLayoutSize(Container parent)
    {
        return super.preferredLayoutSize(parent);
    }

    /**
     * Removes the given component from this layout.
     *
     * @param comp the component to remove from the layout
     */
    @Override
    public void removeLayoutComponent(Component comp)
    {
        super.removeLayoutComponent(comp);

        if (remote == comp)
            remote = null;
        else if (local == comp)
            local = null;
        else if (closeButton == comp)
            closeButton = null;
        else if (canvas == comp)
            canvas = null;
    }
}
