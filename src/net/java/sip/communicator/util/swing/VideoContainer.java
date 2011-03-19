/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

/**
 * Implements a <tt>Container</tt> for video/visual <tt>Component</tt>s.
 * <tt>VideoContainer</tt> uses {@link VideoLayout} to layout the video/visual
 * <tt>Component</tt>s it contains. A specific <tt>Component</tt> can be
 * displayed by default at {@link VideoLayout#CENTER_REMOTE}.
 *
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 */
public class VideoContainer
    extends TransparentPanel
{
    /**
     * The <tt>Component</tt> to be displayed by this <tt>VideoContainer</tt>
     * at {@link VideoLayout#CENTER_REMOTE} when no other <tt>Component</tt> has
     * been added to it to be displayed there. For example, the avatar of the
     * remote peer may be displayed in place of the remote video when the remote
     * video is not available.
     */
    private final Component noVideoComponent;

    /**
     * Initializes a new <tt>VideoContainer</tt> with a specific
     * <tt>Component</tt> to be displayed when no remote video is available.
     *
     * @param noVideoComponent the component to be displayed when no remote
     * video is available
     */
    public VideoContainer(Component noVideoComponent)
    {
        setLayout(new VideoLayout());

        this.noVideoComponent = noVideoComponent;

        add(this.noVideoComponent, VideoLayout.CENTER_REMOTE, -1);
        validate();
    }

    /**
     * Adds the given component at the CENTER_REMOTE position in the default
     * video layout.
     *
     * @param comp the component to add
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
     *
     * @param addedComp the component to add
     * @param constraints
     * @param index
     */
    @Override
    public void add(Component addedComp, Object constraints, int index)
    {
        if (VideoLayout.CENTER_REMOTE.equals(constraints)
                && (noVideoComponent != null)
                && !noVideoComponent.equals(addedComp))
        {
            remove(noVideoComponent);
            validate();
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

        if (VideoLayout.CENTER_REMOTE.equals(
                        ((VideoLayout) getLayout()).getComponentConstraints(
                                removedComp))
                && (noVideoComponent != null)
                && !noVideoComponent.equals(removedComp))
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
