/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

/**
 * Represents a <code>LayoutManager</code> which centers the first
 * <code>Component</code> within its <code>Container</code> and, if the
 * preferred size of the <code>Component</code> is larger than the size of the
 * <code>Container</code>, scales the former within the bounds of the latter
 * while preserving the aspect ratio. <code>FitLayout</code> is appropriate for
 * <code>Container</code>s which display a single image or video
 * <code>Component</code> in its entirety for which preserving the aspect ratio
 * is important.
 * 
 * @author Lyubomir Marinov
 */
public class FitLayout
    implements LayoutManager
{

    /*
     * Does nothing because this LayoutManager lays out only the first Component
     * of the parent Container and thus doesn't need String associations.
     */
    public void addLayoutComponent(String name, Component comp)
    {
    }

    /**
     * Gets the first <code>Component</code> of a specific
     * <code>Container</code> if there is such a <code>Component</code>.
     * 
     * @param parent the <code>Container</code> to retrieve the first
     *            <code>Component</code> of
     * @return the first <code>Component</code> of a specific
     *         <code>Container</code> if there is such a <code>Component</code>;
     *         otherwise, <tt>null</tt>
     */
    protected Component getComponent(Container parent)
    {
        Component[] components = parent.getComponents();

        return (components.length > 0) ? components[0] : null;
    }

    protected void layoutComponent(Component component, Rectangle bounds,
        float alignmentX, float alignmentY)
    {
        Dimension componentSize = component.getPreferredSize();
        boolean scale = false;
        double widthRatio;
        double heightRatio;

        if ((componentSize.width != bounds.width) && (componentSize.width > 0))
        {
            scale = true;
            widthRatio = bounds.width / (double) componentSize.width;
        }
        else
            widthRatio = 1;
        if ((componentSize.height != bounds.height)
                && (componentSize.height > 0))
        {
            scale = true;
            heightRatio = bounds.height / (double) componentSize.height;
        }
        else
            heightRatio = 1;
        if (scale)
        {
            double ratio = Math.min(widthRatio, heightRatio);

            componentSize.width = (int) (componentSize.width * ratio);
            componentSize.height = (int) (componentSize.height * ratio);

            // Respect the maximumSize of the component.
            if (component.isMaximumSizeSet())
            {
                Dimension maximumSize = component.getMaximumSize();

                if (componentSize.width > maximumSize.width)
                    componentSize.width = maximumSize.width;
                if (componentSize.height > maximumSize.height)
                    componentSize.height = maximumSize.height;
            }
        }

        /*
         * Why would one fit a Component into a rectangle with zero width and
         * height?
         */
        if (componentSize.height < 1)
            componentSize.height = 1;
        if (componentSize.width < 1)
            componentSize.width = 1;

        component.setBounds(
                bounds.x
                    + Math.round(
                        (bounds.width - componentSize.width) * alignmentX),
                bounds.y
                    + Math.round(
                        (bounds.height - componentSize.height) * alignmentY),
                componentSize.width,
                componentSize.height);
    }

    /*
     * Scales the first Component if its preferred size is larger than the size
     * of its parent Container in order to display the Component in its entirety
     * and then centers it within the display area of the parent.
     */
    public void layoutContainer(Container parent)
    {
        layoutContainer(parent, Component.CENTER_ALIGNMENT);
    }

    protected void layoutContainer(Container parent, float componentAlignmentX)
    {
        Component component = getComponent(parent);

        if (component != null)
            layoutComponent(component, new Rectangle(parent.getSize()),
                componentAlignmentX, Component.CENTER_ALIGNMENT);
    }

    /*
     * Since this LayoutManager lays out only the first Component of the
     * specified parent Container, the minimum size of the Container is the
     * minimum size of the mentioned Component.
     */
    public Dimension minimumLayoutSize(Container parent)
    {
        Component component = getComponent(parent);

        return (component != null) ? component.getMinimumSize()
            : new Dimension(0, 0);
    }

    /*
     * Since this LayoutManager lays out only the first Component of the
     * specified parent Container, the preferred size of the Container is the
     * preferred size of the mentioned Component.
     */
    public Dimension preferredLayoutSize(Container parent)
    {
        Component component = getComponent(parent);

        return (component != null) ? component.getPreferredSize()
            : new Dimension(0, 0);
    }

    /*
     * Does nothing because this LayoutManager lays out only the first Component
     * of the parent Container and thus doesn't need String associations.
     */
    public void removeLayoutComponent(Component comp)
    {
    }
}
