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
