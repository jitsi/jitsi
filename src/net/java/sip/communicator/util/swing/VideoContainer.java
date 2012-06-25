/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import net.java.sip.communicator.util.*;

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
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>VideoContainer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(VideoContainer.class);

    /**
     * The default background color.
     */
    public static final Color DEFAULT_BACKGROUND_COLOR
        = OSUtils.IS_MAC ? Color.BLACK : null;

    /**
     * The name of the instance method of <tt>Component</tt>s added to
     * <tt>VideoContainer</tt> which creates a new <tt>Component</tt> acting as
     * a canvas in which the other <tt>Component</tt>s contained in the
     * <tt>VideoContainer</tt> are painted.
     */
    private static final String VIDEO_CANVAS_FACTORY_METHOD_NAME
        = "createCanvas";

    /**
     * The <tt>Component</tt> which is the canvas, if any, in which the other
     * <tt>Component</tt>s contained in this <tt>VideoContainer</tt> are
     * painted.
     */
    private Component canvas;

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
     * @param isConference indicates if this <tt>VideoLayout</tt> is dedicated
     * to a conference interface.
     */
    public VideoContainer(Component noVideoComponent, boolean isConference)
    {
        setLayout(new VideoLayout(isConference));

        this.noVideoComponent = noVideoComponent;

        /*
         * JAWTRenderer on Mac OS X will by default occupy the whole video
         * container and will, consequently, draw a black background. In certain
         * problematic cases which it will try to provide a workaround, it will
         * not occupy the whole video container. To make the experience
         * relatively the same, always use a black background.
         */
        if (DEFAULT_BACKGROUND_COLOR != null)
        {
            setBackground(DEFAULT_BACKGROUND_COLOR);
            addContainerListener(
                new ContainerListener()
                {
                    public void componentAdded(ContainerEvent e)
                    {
                        int componentCount = getComponentCount();

                        if ((componentCount == 1)
                                && (getComponent(0)
                                    == VideoContainer.this.noVideoComponent))
                            componentCount = 0;

                        setOpaque(componentCount > 0);
                    }

                    public void componentRemoved(ContainerEvent e)
                    {
                        /*
                         * It's all the same with respect to the purpose of this
                         * ContainerListener.
                         */
                        componentAdded(e);
                    }
                });
        }

        if (this.noVideoComponent != null)
        {
            add(this.noVideoComponent, VideoLayout.CENTER_REMOTE, -1);
            validate();
        }
    }

    /**
     * Adds the given component at the {@link VideoLayout#CENTER_REMOTE}
     * position in the default video layout.
     *
     * @param comp the component to add
     * @return the added component
     */
    @Override
    public Component add(Component comp)
    {
        add(comp, VideoLayout.CENTER_REMOTE);
        return comp;
    }

    @Override
    public Component add(Component comp, int index)
    {
        add(comp, null, index);
        return comp;
    }

    @Override
    public void add(Component comp, Object constraints)
    {
        add(comp, constraints, -1);
    }

    /**
     * Overrides the default behavior of add in order to be sure to remove the
     * default "no video" component when a remote video component is added.
     *
     * @param comp the component to add
     * @param constraints
     * @param index
     */
    @Override
    public void add(Component comp, Object constraints, int index)
    {
        if (VideoLayout.CENTER_REMOTE.equals(constraints)
                && (noVideoComponent != null)
                && !noVideoComponent.equals(comp)
            || (comp.equals(noVideoComponent)
                && noVideoComponent.getParent() != null))
        {
            remove(noVideoComponent);
            validate();
        }

        if ((canvas == null) || (canvas.getParent() != this))
        {
            if (OSUtils.IS_MAC && (comp != canvas))
            {
                /*
                 * Unless the comp has a createCanvas() method, it makes no
                 * sense to consider any exception a problem.
                 */
                boolean ignoreException;
                Throwable exception;

                ignoreException = true;
                exception = null;
                canvas = null;

                try
                {
                    Method m
                        = comp.getClass().getMethod(
                                VIDEO_CANVAS_FACTORY_METHOD_NAME);

                    if (m != null)
                    {
                        ignoreException = false;

                        Object c = m.invoke(comp);

                        if (c instanceof Component)
                            canvas = (Component) c;
                    }
                }
                catch (ClassCastException cce)
                {
                    exception = cce;
                }
                catch (ExceptionInInitializerError eiie)
                {
                    exception = eiie;
                }
                catch (IllegalAccessException illegalAccessException)
                {
                    exception = illegalAccessException;
                }
                catch (IllegalArgumentException illegalArgumentException)
                {
                    exception = illegalArgumentException;
                }
                catch (InvocationTargetException ita)
                {
                    exception = ita;
                }
                catch (NoSuchMethodException nsme)
                {
                    exception = nsme;
                }
                catch (NullPointerException npe)
                {
                    exception = npe;
                }
                catch (SecurityException se)
                {
                    exception = se;
                }
                if (canvas != null)
                    add(canvas, VideoLayout.CANVAS, 0);
                else if ((exception != null) && !ignoreException)
                    logger.error("Failed to create video canvas.", exception);
            }
        }
        if ((canvas != null)
                && (canvas.getParent() == this)
                && OSUtils.IS_MAC
                && (comp != canvas))
        {
            /*
             * The canvas in which the other components are to be painted should
             * always be at index 0. And the order of adding is important so no
             * index should be specified
             */
            index = -1;
        }

        /*
         * XXX Do not call #remove(Component) beyond this point and before
         * #add(Component, Object, int) because #removeCanvasIfNecessary() will
         * remove the canvas.
         */

        super.add(comp, constraints, index);
    }

    /**
     * Overrides the default remove behavior in order to add the default no
     * video component when the remote video is removed.
     *
     * @param comp the component to remove
     */
    @Override
    public void remove(Component comp)
    {
        super.remove(comp);

        if ((comp == canvas)
                && (canvas != null)
                && (canvas.getParent() != this))
        {
            canvas = null;
            validate();
        }

        Component[] components = getComponents();
        VideoLayout videoLayout = (VideoLayout) getLayout();
        boolean hasComponentsAtCenterRemote = false;

        for (Component c : components)
        {
            if (!c.equals(noVideoComponent)
                    && VideoLayout.CENTER_REMOTE.equals(
                            videoLayout.getComponentConstraints(c)))
            {
                hasComponentsAtCenterRemote = true;
                break;
            }
        }

        if (!hasComponentsAtCenterRemote
                && (noVideoComponent != null)
                && !noVideoComponent.equals(comp))
        {
            add(noVideoComponent, VideoLayout.CENTER_REMOTE);
            validate();
        }

        removeCanvasIfNecessary();
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

        if ((canvas != null) && (canvas.getParent() != this))
            canvas = null;

        if (noVideoComponent != null)
        {
            add(noVideoComponent, VideoLayout.CENTER_REMOTE);
            validate();
        }
    }

    /**
     * Removes {@link #canvas} from this <tt>VideoContainer</tt> if no sibling
     * <tt>Component</tt> needs it.
     */
    public void removeCanvasIfNecessary()
    {
        if ((canvas == null) || !OSUtils.IS_MAC)
            return;

        boolean removeCanvas = true;

        for (Component component : getComponents())
        {
            if (component == canvas)
                continue;
            try
            {
                component.getClass().getMethod(
                        VIDEO_CANVAS_FACTORY_METHOD_NAME);
                removeCanvas = false;
                break;
            }
            catch (NoSuchMethodException nsme)
            {
                /*
                 * Ignore it because we already presume that component does not
                 * need the canvas.
                 */
            }
        }
        if (removeCanvas)
            remove(canvas);
    }
}
