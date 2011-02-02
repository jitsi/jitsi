/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

/**
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class VideoContainer
    extends TransparentPanel
{
    private final Component noVideoComponent;

    /**
     * Creates a video container by specifying the default "no video" component.
     *
     * @param noVideoComponent the component shown when no remote video is
     * available
     */
    public VideoContainer(Component noVideoComponent)
    {
        setLayout(new VideoLayout());

        this.noVideoComponent = noVideoComponent;

        this.add(this.noVideoComponent, VideoLayout.CENTER_REMOTE, -1);
        validate();
    }

    /**
     * Adds the given component at the CENTER_REMOTE position in the default
     * video layout.
     *
     * @return the added component
     */
    @Override
    public Component add(Component comp)
    {
        add(comp, VideoLayout.CENTER_REMOTE, -1);
        return comp;
    }

    /**
     * Overrides the default behavior of add in order to be sure to remove the
     * default "no video" component, when a remote video component is added.
     */
    @Override
    public void add(Component addedComp, Object constraints, int index)
    {
        if (constraints.equals(VideoLayout.CENTER_REMOTE)
            && noVideoComponent != null
            && !addedComp.equals(noVideoComponent))
        {
            remove(noVideoComponent);
            revalidate();
        }

        super.add(addedComp, constraints, index);
    }

    /**
     * Overrides the default remove behavior in order to add the default no
     * video component when the remote video is removed.
     *
     * @param removedComp the component to remove
     */
    @Override
    public void remove(Component removedComp)
    {
        super.remove(removedComp);

        if (((VideoLayout) getLayout()).getComponentConstraints(removedComp)
                    .equals(VideoLayout.CENTER_REMOTE)
                && !removedComp.equals(noVideoComponent))
        {
            add(noVideoComponent, VideoLayout.CENTER_REMOTE);
            validate();
        }
    }

    /**
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
        super.removeAll();

        add(noVideoComponent, VideoLayout.CENTER_REMOTE);
        validate();
    }
}