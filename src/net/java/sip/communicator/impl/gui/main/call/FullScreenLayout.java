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
     * The vertical gap between the center and the south components.
     */
    private int yGap = 0;

    /**
     * Initializes a new <tt>FullScreenLayout</tt> instance.
     *
     * @param overlay <tt>true</tt> to lay out the <tt>Component</tt> at
     * {@link #SOUTH} on top of the <tt>Component</tt> at {@link #CENTER} i.e as
     * an overlay; otherwise, <tt>false</tt>
     * @oaram yGap the gap betwen the center and the south component
     */
    public FullScreenLayout(boolean overlay, int yGap)
    {
        this.overlay = overlay;
        this.yGap = yGap;
    }

    /**
     * Adds the given component to this layout.
     *
     * @param name the name of the constraint (CENTER or SOUTH)
     * @param comp the component to add to this layout
     */
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

    /**
     * Lays out the components added in the given parent container
     *
     * @param parent the parent container to lay out
     */
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
            int yOffset = overlay ? 0 : southHeight + yGap;

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
