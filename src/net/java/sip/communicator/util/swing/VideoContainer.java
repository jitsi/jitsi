/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.lang.reflect.*;

import javax.swing.*;

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
     * The <tt>Logger</tt> used by the <tt>VideoContainer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(VideoContainer.class);

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
     */
    public VideoContainer(Component noVideoComponent)
    {
        setLayout(new VideoLayout());

        this.noVideoComponent = noVideoComponent;

        add(this.noVideoComponent, VideoLayout.CENTER_REMOTE, -1);
        validate();
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
                    Method m = comp.getClass().getMethod("createCanvas");

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
        else
        {
            /*
             * The canvas in which the other components are to be painted should
             * always be at index 0.
             */
            if ((index == 0) && (comp != canvas))
                index++;
        }

        if (VideoLayout.CENTER_REMOTE.equals(constraints)
                && (noVideoComponent != null)
                && !noVideoComponent.equals(comp))
        {
            remove(noVideoComponent);
            validate();
        }

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
            canvas = null;

        if (VideoLayout.CENTER_REMOTE.equals(
                        ((VideoLayout) getLayout()).getComponentConstraints(
                                comp))
                && (noVideoComponent != null)
                && !noVideoComponent.equals(comp))
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

        if ((canvas != null) && (canvas.getParent() != this))
            canvas = null;

        add(noVideoComponent, VideoLayout.CENTER_REMOTE);
        validate();
    }
}
