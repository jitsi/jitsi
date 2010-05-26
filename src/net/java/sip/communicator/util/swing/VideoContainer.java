/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;

/**
 * @author Lubomir Marinov
 */
public class VideoContainer
    extends TransparentPanel
{
    private final ContainerListener containerListener = new ContainerListener()
    {

        /*
         * Since the videoContainer displays either noVideoComponent or a single
         * visual Component which represents video, ensures the last Component
         * added to the Container is the only Component it contains i.e.
         * noVideoComponent goes away when the video is displayed and the video
         * goes away when noVideoComponent is displayed.
         */
        public void componentAdded(ContainerEvent event)
        {
            Container container = event.getContainer();
            Component local = ((VideoLayout) container.getLayout()).getLocal();
            Component added = event.getChild();

            if ((local != null) && (local == added))
                return;

            boolean validate = false;

            for (Component component : container.getComponents())
            {
                if ((component != added) && (component != local))
                {
                    container.remove(component);
                    validate = true;
                }
            }
            if (validate)
                container.validate();
        };

        /*
         * Displays noVideoComponent when there is no visual Component which
         * represents video to be displayed.
         */
        public void componentRemoved(ContainerEvent event)
        {
            Container container = event.getContainer();

            if ((container.getComponentCount() <= 0)
                || (((VideoLayout) container.getLayout()).getRemote() == null))
            {
                container.add(noVideoComponent, VideoLayout.CENTER_REMOTE);
                container.validate();
            }
        }
    };

    private final Component noVideoComponent;

    public VideoContainer(Component noVideoComponent)
    {
        setLayout(new VideoLayout());

        this.noVideoComponent = noVideoComponent;

        addContainerListener(containerListener);
        add(this.noVideoComponent, VideoLayout.CENTER_REMOTE);
        validate();
    }

    public Component add(Component comp)
    {
        add(comp, VideoLayout.CENTER_REMOTE);
        return comp;
    }

    /*
     * Ensures noVideoComponent is displayed even when the clients of the
     * videoContainer invoke its #removeAll() to remove their previous visual
     * Components representing video. Just adding noVideoComponent upon
     * ContainerEvent#COMPONENT_REMOVED when there is no other Component left in
     * the Container will cause an infinite loop because Container#removeAll()
     * will detect that a new Component has been added while dispatching the
     * event and will then try to remove the new Component.
     */
    @Override
    public void removeAll()
    {
        removeContainerListener(containerListener);
        try
        {
            super.removeAll();
        }
        finally
        {
            addContainerListener(containerListener);
            containerListener.componentRemoved(new ContainerEvent(this,
                ContainerEvent.COMPONENT_REMOVED, null));
        }
    }
}
