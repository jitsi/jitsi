/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

/**
 * @author Lubomir Marinov
 */
public class FullScreenLayout implements LayoutManager
{
    public static final String CENTER = "CENTER";

    public static final String SOUTH = "SOUTH";

    private Component center;

    private final boolean overlay;

    private Component south;

    public FullScreenLayout(boolean overlay)
    {
        this.overlay = overlay;
    }

    public void addLayoutComponent(String name, Component comp)
    {
        if (CENTER.equals(name))
        {
            center = comp;
        }
        else if (SOUTH.equals(name))
        {
            south = comp;
        }
    }

    private Component[] getLayoutComponents()
    {
        if (center == null)
        {
            if (south == null)
            {
                return new Component[0];
            }
            else
            {
                return new Component[]
                { south };
            }
        }
        else if (south == null)
        {
            return new Component[]
            { center };
        }
        else
        {
            return new Component[]
            { center, south };
        }
    }

    public void layoutContainer(Container parent)
    {
        Dimension parentSize = parent.getSize();
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

        if (center != null)
        {
            int yOffset = overlay ? 0 : southHeight;

            center.setBounds(0, yOffset, parentSize.width, parentSize.height
                - 2 * yOffset);
        }
        if (south != null)
        {
            south.setBounds((parentSize.width - southWidth) / 2,
                parentSize.height - southHeight, southWidth, southHeight);
        }
    }

    public Dimension minimumLayoutSize(Container parent)
    {
        Component[] components = getLayoutComponents();
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
        return null;
    }

    public Dimension preferredLayoutSize(Container parent)
    {
        Component[] components = getLayoutComponents();
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
        return null;
    }

    public void removeLayoutComponent(Component comp)
    {
        if (comp.equals(center))
        {
            center = null;
        }
        else if (comp.equals(south))
        {
            south = null;
        }
    }
}
