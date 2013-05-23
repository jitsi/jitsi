/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

/**
 * Ordered transparent panel. Components added to the panel
 * must implement OrderedComponent to be able to order them or
 * will leave the parent to add them as usual.
 *
 * @author Damian Minkov
 */
public class OrderedTransparentPanel
    extends TransparentPanel
{
    private static final long serialVersionUID = 0L;

    @Override
    public Component add(Component comp)
    {
        if(comp instanceof OrderedComponent)
            return addOrdered(comp);
        else
            return super.add(comp);
    }

    /**
     * Method to order add OrderedComponents.
     * @param comp the component to order.
     * @return the component argument
     */
    private Component addOrdered(Component comp)
    {
        int orederIndex = ((OrderedComponent)comp).getIndex();

        Component[] cs = getComponents();

        // don't add a component if already added or it will be removed
        // and added at the end
        for(Component c : cs)
        {
            if(c.equals(comp))
                return comp;
        }

        for(int i = 0; i < cs.length; i++)
        {
            Component c = cs[i];

            if(c instanceof OrderedComponent)
            {
                int cIndex = ((OrderedComponent) c).getIndex();

                if(orederIndex < cIndex)
                    return super.add(comp, i);
            }
        }

        return super.add(comp);
    }
}
