/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Implements a <tt>LayoutManager</tt> for the full-screen <tt>Call</tt>
 * display.
 *
 * @author Lyubomir Marinov
 */
public class FullScreenLayout
    implements LayoutManager
{
    public static final String CENTER = "CENTER";

    public static final String SOUTH = "SOUTH";

    private Component center;

    /**
     * The indicator which determines whether {@link #south} is to be laid out
     * on top of {@link #center} i.e. as an overlay.
     */
    private final boolean overlay;

    private Component south;

    /**
     * Initializes a new <tt>FullScreenLayout</tt> instance.
     *
     * @param overlay <tt>true</tt> to lay out the <tt>Component</tt> at
     * {@link #SOUTH} on top of the <tt>Component</tt> at {@link #CENTER} i.e as
     * an overlay; otherwise, <tt>false</tt>
     */
    public FullScreenLayout(boolean overlay)
    {
        this.overlay = overlay;
    }

    public void addLayoutComponent(String name, Component comp)
    {
        if (CENTER.equals(name))
            center = comp;
        else if (SOUTH.equals(name))
            south = comp;
    }

    /**
     * Gets a <tt>List</tt> of the <tt>Component</tt>s to be laid out by this
     * <tt>LayoutManager</tt> i.e. the non-<tt>null</tt> of {@link #center}
     * and {@link #south}.
     *
     * @return a <tt>List</tt> of the <tt>Component</tt>s to be laid out by this
     * <tt>LayoutManager</tt>
     */
    private List<Component> getLayoutComponents()
    {
        List<Component> layoutComponents = new ArrayList<Component>(2);

        if (center != null)
            layoutComponents.add(center);
        if (south != null)
            layoutComponents.add(south);
        return layoutComponents;
    }

    public void layoutContainer(Container parent)
    {
        int southWidth;
        int southHeight;

        if (south == null)
        {
            southWidth = southHeight = 0;
        }
        else
        {
            Dimension southSize = south.getPreferredSize();

            southWidth = southSize.width;
            southHeight = southSize.height;
        }

        Dimension parentSize = parent.getSize();

        if (center != null)
        {
            /*
             * If the Component at the SOUTH is not to be shown as an overlay,
             * make room for it bellow the Component at the CENTER.
             */
            int yOffset = overlay ? 0 : southHeight;

            center.setBounds(
                    0,
                    0,
                    parentSize.width,
                    parentSize.height - yOffset);
        }
        if (south != null)
        {
            south.setBounds(
                    (parentSize.width - southWidth) / 2,
                    parentSize.height - southHeight,
                    southWidth,
                    southHeight);
        }
    }

    public Dimension minimumLayoutSize(Container parent)
    {
        List<Component> components = getLayoutComponents();
        Dimension size = new Dimension(0, 0);

        for (Component component : components)
        {
            Dimension componentSize = component.getMinimumSize();

            size.width = Math.max(size.width, componentSize.width);
            if (overlay)
                size.height = Math.max(size.height, componentSize.height);
            else
                size.height += componentSize.height;
        }
        return size;
    }

    public Dimension preferredLayoutSize(Container parent)
    {
        List<Component> components = getLayoutComponents();
        Dimension size = new Dimension(0, 0);

        for (Component component : components)
        {
            Dimension componentSize = component.getPreferredSize();

            size.width = Math.max(size.width, componentSize.width);
            if (overlay)
                size.height = Math.max(size.height, componentSize.height);
            else
                size.height += componentSize.height;
        }
        return size;
    }

    public void removeLayoutComponent(Component comp)
    {
        if (comp.equals(center))
            center = null;
        else if (comp.equals(south))
            south = null;
    }
}
