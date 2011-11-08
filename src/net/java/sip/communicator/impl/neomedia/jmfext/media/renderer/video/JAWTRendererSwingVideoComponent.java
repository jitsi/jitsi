/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.video;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List; // disambiguation

import javax.media.*;
import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * Implements a Swing <tt>Component</tt> in which <tt>JAWTRenderer</tt> paints.
 *
 * @author Lyubomir Marinov
 */
public class JAWTRendererSwingVideoComponent
    extends JPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>JAWTRendererSwingVideoComponent</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(JAWTRendererSwingVideoComponent.class);

    /**
     * The <tt>SwingVideoComponentCanvas</tt> in which this
     * <tt>SwingVideoComponent</tt> is painted according to the last call to
     * {@link #addNotify()}.
     */
    private SwingVideoComponentCanvas canvas;

    /**
     * The <tt>Container</tt> which is the last-known parent of this
     * <tt>SwingVideoComponent</tt>.
     */
    private Container parent;

    /**
     * The <tt>ComponentListener</tt> which is to be or is listening to
     * <tt>ComponentEvent</tt>s fired by {@link #parent}.
     */
    private final ComponentListener parentComponentListener
        = new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                /*
                 * It is necessary to call
                 * #procesLightweightComponentEvent() when the parent gets
                 * resized only if the native counterpart of this
                 * SwingVideoComponent expects bounds in a coordinate system
                 * which changes with respect to the bounds of this
                 * SwingVideoComponent when the parent gets resized.
                 */
                //processLightweightComponentEvent();
            }
        };

    /**
     * The <tt>JAWTRenderer</tt> which paints in this
     * <tt>JAWTRendererSwingVideoComponent</tt>.
     */
    private final JAWTRenderer renderer;

    /**
     * The indicator which determines whether the native counterpart of this
     * <tt>JAWTRenderer</tt> wants <tt>paint</tt> calls on its Swing
     * <tt>Component</tt> to be delivered.
     *
     * @see AWTVideoComponent#wantsPaint
     */
    private boolean wantsPaint = true;

    /**
     * Constructor.
     *
     * @param renderer <tt>JAWTRenderer</tt> instance
     */
    public JAWTRendererSwingVideoComponent(JAWTRenderer renderer)
    {
        this.renderer = renderer;
    }

    @Override
    public void addNotify()
    {
        /*
         * Since JAWTRenderer#open() may be performing the call, it may be
         * wrong about this SwingVideoComponent having a parent.
         */
        Container parent = getParent();

        if (parent == null)
            return;

        super.addNotify();

        wantsPaint = true;

        synchronized (renderer.getHandleLock())
        {
            long handle = renderer.getHandle();

            if (handle != 0)
            {
                SwingVideoComponentCanvas canvas = findCanvas();

                if (canvas == null)
                {
                    JAWTRenderer.addNotifyLightweightComponent(
                            handle, this,
                            0);
                }
                else
                {
                    long canvasHandle;

                    synchronized (canvas.getHandleLock())
                    {
                        canvasHandle = canvas.getHandle();
                        JAWTRenderer.addNotifyLightweightComponent(
                                handle, this,
                                canvasHandle);
                    }
                    if ((canvasHandle != 0) && (this.canvas != canvas))
                    {
                        this.canvas = canvas;
                    }
                }

                /*
                 * Emulate a ComponentEvent so that, for example, the native
                 * counterpart of this Component gets its bounds up to date.
                 */
                processLightweightComponentEvent();
            }
        }

        if (this.parent != parent)
        {
            this.parent = parent;
            this.parent.addComponentListener(parentComponentListener);
        }
    }

    /**
     * Creates a new AWT <tt>Component</tt> in which
     * <tt>SwingVideoComponent</tt>s can be rendered.
     * @return new AWT <tt>Component</tt> in which <tt>SwingVideoComponent</tt>s
     * can be rendered
     */
    public Component createCanvas()
    {
        return new SwingVideoComponentCanvas(renderer);
    }

    /**
     * Finds a <tt>SwingVideoComponentCanvas</tt> in which this
     * <tt>SwingVideoComponent</tt> can be painted.
     *
     * @return a <tt>SwingVideoComponentCanvas</tt> in which this
     * <tt>SwingVideoComponent</tt> can be painted if any; otherwise,
     * <tt>null</tt>
     */
    private SwingVideoComponentCanvas findCanvas()
    {
        Container parent = getParent();

        if (parent != null)
            for (Component component : parent.getComponents())
                if (component instanceof SwingVideoComponentCanvas)
                    return (SwingVideoComponentCanvas) component;
        return null;
    }

    /**
     * Gets the <tt>SwingVideoComponentCanvas</tt> in which this
     * <tt>SwingVideoComponent</tt> is being painted.
     *
     * @return the <tt>SwingVideoComponentCanvas</tt> in which this
     * <tt>SwingVideoComponent</tt> is being painted if any; otherwise,
     * <tt>null</tt>
     */
    SwingVideoComponentCanvas getCanvas()
    {
        return canvas;
    }

    @Override
    public void paint(Graphics g)
    {
        synchronized (renderer.getHandleLock())
        {
            long handle = renderer.getHandle();

            if (wantsPaint && (handle != 0))
            {
                wantsPaint
                    = JAWTRenderer.paintLightweightComponent(
                            handle,
                            this,
                            g);
            }
        }
    }

    @Override
    protected void processComponentEvent(ComponentEvent e)
    {
        super.processComponentEvent(e);

        if (equals(e.getComponent()))
        {
            switch(e.getID())
            {
            case ComponentEvent.COMPONENT_MOVED:
            case ComponentEvent.COMPONENT_RESIZED:
                processLightweightComponentEvent();
                break;
            }
        }
    }

    /**
     * Notifies this <tt>SwingVideoComponent</tt> that a
     * <tt>ComponentEvent</tt> related to it has occurred.
     */
    private void processLightweightComponentEvent()
    {
        Rectangle bounds = getBounds();

        if (bounds != null)
        {
            synchronized (renderer.getHandleLock())
            {
                long handle = renderer.getHandle();

                if (handle != 0)
                {
                    JAWTRenderer.processLightweightComponentEvent(
                            handle,
                            bounds.x,
                            bounds.y,
                            bounds.width,
                            bounds.height);
                }
            }
        }
    }

    @Override
    public void removeNotify()
    {
        if (parent != null)
        {
            parent.removeComponentListener(parentComponentListener);
            parent = null;
        }

        synchronized (renderer.getHandleLock())
        {
            long handle = renderer.getHandle();

            if (handle != 0)
            {
                if (canvas != null)
                {
                    synchronized (canvas.getHandleLock())
                    {
                        JAWTRenderer.removeNotifyLightweightComponent(
                                handle, this);
                    }
                    canvas = null;
                }
                else
                {
                    JAWTRenderer.removeNotifyLightweightComponent(
                            handle, this);
                }
            }
        }

        /*
         * In case the associated JAWTRenderer has said that it does not
         * want paint events/notifications, ask it again next time because
         * the native handle of this Canvas may be recreated.
         */
        wantsPaint = true;

        super.removeNotify();
    }

    @Override
    public void repaint()
    {
        super.repaint();

        /*
         * Since SwingVideoComponent is to be painted in a
         * SwingVideoComponentCanvas, the latter should repaint when the
         * former does.
         */
        Component canvas = getCanvas();

        if (canvas != null)
            canvas.repaint();
    }

    @Override
    public void setBounds(int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);

        /*
         * Calling #setBounds(int, int, int, int) does not seem to cause
         * this SwingVideoComponent to process a ComponentEvent so force it
         * because it is really necessary to deliver the up-to-date bounds
         * to the native counterpart.
         */
        processLightweightComponentEvent();
    }

    @Override
    public void setLocation(int x, int y)
    {
        super.setLocation(x, y);

        processLightweightComponentEvent();
    }

    /**
     * Represents a <tt>Component</tt> which is neither a
     * <tt>AWTVideoComponent</tt> nor a <tt>SwingVideoComponent</tt> but which
     * is to be painted by a <tt>SwingVideoComponentCanvas</tt> anyway.
     */
    private static class NonVideoComponent
    {
        /**
         * The <tt>BufferedImage</tt> into which {@link #component} is to be
         * painted so that it can be processed and then rendered by
         * {@link #handle}.
         */
        private BufferedImage bufferedImage;

        /**
         * The <tt>Component</tt> represented by this <tt>NonVideoComponent</tt>
         * instance.
         */
        public final Component component;

        /**
         * The handle of the native <tt>JAWTRenderer</tt> which paints this
         * <tt>NonVideoComponent</tt>.
         */
        private long handle;

        /**
         * The height in pixels of {@link #bufferedImage} and {@link #rgb}.
         */
        private int height;

        /**
         * The pixels of {@link #bufferedImage} to be processed by
         * {@link #handle}.
         */
        private int[] rgb;

        /**
         * The width in pixels of {@link #bufferedImage} and {@link #rgb}.
         */
        private int width;

        /**
         * Initializes a new <tt>NonVideoComponent</tt> instance which is to
         * paint a specific <tt>Component</tt> in the context of a parent
         * <tt>JAWTRenderer</tt>.
         *
         * @param component the <tt>Component</tt> to be painted
         * @param parentHandle the handle of the native <tt>JAWTRenderer</tt> in
         * the context of which <tt>component</tt> is to be painted
         */
        public NonVideoComponent(Component component, long parentHandle)
        {
            this.component = component;

            try
            {
                handle = JAWTRenderer.open(this.component);
                if (handle != 0)
                {
                    JAWTRenderer.addNotifyLightweightComponent(
                            handle, this.component,
                            parentHandle);
                }
            }
            catch (ResourceUnavailableException rue)
            {
                logger.error(
                        "Failed to JAWTRenderer.open for NonVideoComponent",
                        rue);
            }
        }

        /**
         * Releases the resources of this <tt>NonVideoComponent</tt> and
         * prepares it to be garbage collected.
         */
        public void dispose()
        {
            if (handle != 0)
            {
                JAWTRenderer.removeNotifyLightweightComponent(
                        handle, component);
                JAWTRenderer.close(handle, component);
                handle = 0;
            }
        }

        /**
         * Paints the <tt>Component</tt> associated with this
         * <tt>NonVideoComponent</tt> instance.
         */
        public void paint()
        {
            if (handle != 0)
            {
                /*
                 * Make sure the location, the size and the visibility known to
                 * the associated native JAWTRenderer are in sync with these of
                 * the component.
                 */
                Rectangle bounds = component.getBounds();

                if (!component.isVisible())
                {
                    bounds.height = 0;
                    bounds.width = 0;
                }
                JAWTRenderer.processLightweightComponentEvent(
                        handle,
                        bounds.x, bounds.y, bounds.width, bounds.height);

                /*
                 * If the component is not visible, the native JAWTRenderer
                 * already knows that it is not to be rendered.
                 */
                if ((bounds.height < 1) || (bounds.width < 1))
                    return;

                /*
                 * Paint the component and tell the native JAWTRenderer about
                 * the latest painting.
                 */
                if ((height != bounds.height) || (width != bounds.width))
                {
                    rgb = new int[bounds.width * bounds.height];
                    bufferedImage
                        = new BufferedImage(
                                bounds.width, bounds.height,
                                BufferedImage.TYPE_INT_ARGB);
                    height = bounds.height;
                    width = bounds.width;
                }
                if ((bufferedImage != null) && (rgb != null))
                {
                    Graphics g = bufferedImage.createGraphics();
                    boolean process = false;

                    try
                    {
                        component.paint(g);
                        process = true;
                    }
                    finally
                    {
                        g.dispose();
                    }
                    if (process)
                    {
                        bufferedImage.getRGB(
                                0, 0, width, height,
                                rgb, 0, width);
                        JAWTRenderer.process(
                                handle, component,
                                rgb, 0, rgb.length,
                                width, height);
                    }
                }
            }
        }
    }

    /**
     * Implements an AWT <tt>Component</tt> in which
     * <tt>SwingVideoComponent</tt>s can be rendered.
     */
    private static class SwingVideoComponentCanvas
        extends JAWTRendererVideoComponent
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * The handle to the native <tt>JAWTRenderer</tt> which does the actual
         * painting of this <tt>SwingVideoComponentCanvas</tt>.
         */
        private long handle;

        /**
         * The <tt>Component</tt> to which this
         * <tt>SwingVideoComponentCanvas</tt> has dispatched a
         * {@link MouseEvent#MOUSE_PRESSED}.
         */
        private Component mousePressedComponent;

        /**
         * The <tt>NoVideoComponent</tt>s painted by this
         * <tt>SwingVideoComponentCanvas</tt>.
         */
        private final List<NonVideoComponent> nonVideoComponents
            = new LinkedList<NonVideoComponent>();

        /**
         * The <tt>Container</tt> which is the last-known parent of this
         * <tt>SwingVideoComponentCanvas</tt>.
         */
        private Container parent;

        /**
         * The <tt>ContainerListener</tt> which listens to {@link #parent}.
         */
        private final ContainerListener parentContainerListener
            = new ContainerListener()
            {
                public void componentAdded(ContainerEvent e)
                {
                    Component c = e.getChild();

                    if (!(c instanceof JAWTRendererVideoComponent)
                            && !(c instanceof JAWTRendererSwingVideoComponent))
                    {
                        nonVideoComponentAdded(c);
                    }
                }

                public void componentRemoved(ContainerEvent e)
                {
                    Component c = e.getChild();

                    if (mousePressedComponent == c)
                        mousePressedComponent = null;

                    if (!(c instanceof JAWTRendererVideoComponent)
                            && !(c instanceof JAWTRendererSwingVideoComponent))
                    {
                        nonVideoComponentRemoved(c);
                    }
                    else if (SwingVideoComponentCanvas.this.equals(c))
                        removeAllNonVideoComponents();
                }
            };

        /**
         * Initializes a new <tt>SwingVideoComponentCanvas</tt> instance.
         *
         * @param renderer
         */
        public SwingVideoComponentCanvas(JAWTRenderer renderer)
        {
            super(renderer);

            enableEvents(
                    AWTEvent.MOUSE_EVENT_MASK
                        | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        @Override
        public void addNotify()
        {
            super.addNotify();

            synchronized (getHandleLock())
            {
                if (getHandle() == 0)
                {
                    try
                    {
                        this.handle = JAWTRenderer.open(this);
                    }
                    catch (ResourceUnavailableException rue)
                    {
                        throw new RuntimeException(rue);
                    }
                    if (getHandle() == 0)
                    {
                        throw new RuntimeException(
                                "JAWTRenderer#open(Component)");
                    }
                }
            }

            Container parent = getParent();

            if ((parent != null) && (this.parent != parent))
            {
                this.parent = parent;
                this.parent.addContainerListener(parentContainerListener);
            }
        }

        @Override
        public boolean contains(int x, int y)
        {
            /*
             * Act as a "glass pane" i.e. be transparent with respect to points
             * and pretend they are in whatever is underneath.
             */
            return false;
        }

        /**
         * Dispatches <tt>MouseEvent</tt>s to whatever is underneath this
         * <tt>SwingVideoComponentCanvas</tt> because it only renders
         * <tt>Component</tt>s i.e. it is like a "glass pane".
         */
        private boolean dispatchMouseEvent(MouseEvent e)
        {
            Component srcc = e.getComponent();

            if (srcc != null)
            {
                int id = e.getID();
                Component dstc = null;

                /*
                 * After a MOUSE_PRESSED, this SwingVideoComponentCanvas will
                 * continue to receive, for example, MouseMotionEvents even
                 * when the point has moved out of it. Emulate the same behavior
                 * for the Components this SwingVideoComponentCanvas dispatches
                 * events to since it is transparent in this respect.
                 */
                if (MouseEvent.MOUSE_PRESSED == id)
                    mousePressedComponent = null;
                else if (mousePressedComponent != null)
                {
                    dstc = mousePressedComponent;
                    if ((MouseEvent.MOUSE_CLICKED == id)
                            || (MouseEvent.MOUSE_RELEASED == id))
                        mousePressedComponent = null;
                }

                if (dstc == null)
                {
                    Container parent = getParent();

                    if (parent != null)
                    {
                        Point parentPoint
                            = SwingUtilities.convertPoint(
                                    srcc,
                                    e.getPoint(),
                                    parent);

                        dstc = getComponentAt(parent, parentPoint);
                    }
                }

                if (dstc != null)
                {
                    if (MouseEvent.MOUSE_PRESSED == id)
                        mousePressedComponent = dstc;
                    dstc.dispatchEvent(
                            SwingUtilities.convertMouseEvent(
                                    srcc,
                                    e,
                                    dstc));
                    return true;
                }
            }
            return false;
        }

        /**
         * Determines the <tt>Component</tt> which is a child of a specific
         * <tt>Container</tt> which contains a specific <tt>Point</tt>. Since
         * <tt>SwingVideoComponentCanvas</tt> is like a "glass pane", it never
         * contains the specified <tt>point</tt>.
         *
         * @param parent the <tt>Container</tt> which contains the
         * <tt>Component</tt>s which may contain the specified <tt>point</tt>
         * @param point the point in the coordinate system of <tt>parent</tt>
         * which is to be determined which <tt>Component</tt> contains it
         * @return the <tt>Component</tt> which is a child of the specified
         * <tt>Container</tt> and contains the specified <tt>Point</tt> or
         * <tt>null</tt> if there is no such <tt>Component</tt>
         */
        private Component getComponentAt(Container parent, Point point)
        {
            Component[] components = parent.getComponents();

            for (int componentIndex = components.length - 1;
                    componentIndex >= 0;
                    componentIndex--)
            {
                Component component = components[componentIndex];

                if (!equals(component)
                        && component.isVisible()
                        && component.contains(
                                SwingUtilities.convertPoint(
                                        parent,
                                        point,
                                        component)))
                {
                    return component;
                }
            }
            return null;
        }

        /* Overrides JAWTRendererVideoComponent#getHandle(). */
        @Override
        protected long getHandle()
        {
            return handle;
        }

        /* Overrides JAWTRendererVideoComponent#getHandleLock(). */
        @Override
        protected Object getHandleLock()
        {
            return this;
        }

        /**
         * Notifies this <tt>SwingVideoComponentCanvas</tt> that a
         * <tt>Component</tt> which is neither an <tt>AWTVideoComponent</tt> nor
         * a <tt>SwingVideoComponent</tt> has been added to {@link #parent}.
         *
         * @param c the component which has been added
         */
        private void nonVideoComponentAdded(Component c)
        {
            synchronized (getHandleLock())
            {
                synchronized (nonVideoComponents)
                {
                    for (NonVideoComponent nonVideoComponent
                            : nonVideoComponents)
                    {
                        if (nonVideoComponent.component.equals(c))
                            return;
                    }

                    nonVideoComponents.add(
                            new NonVideoComponent(c, getHandle()));
                }
            }
        }

        /**
         * Notifies this <tt>SwingVideoComponentCanvas</tt> that a
         * <tt>Component</tt> which is neither an <tt>AWTVideoComponent</tt> nor
         * a <tt>SwingVideoComponent</tt> has been removed from {@link #parent}.
         *
         * @param c the component which has been removed
         */
        private void nonVideoComponentRemoved(Component c)
        {
            synchronized (nonVideoComponents)
            {
                Iterator<NonVideoComponent> nonVideoComponentIter
                    = nonVideoComponents.iterator();

                while (nonVideoComponentIter.hasNext())
                {
                    NonVideoComponent nonVideoComponent
                        = nonVideoComponentIter.next();

                    if (nonVideoComponent.component.equals(c))
                    {
                        nonVideoComponentIter.remove();
                        nonVideoComponent.dispose();
                        break;
                    }
                }
            }
        }

        @Override
        public void paint(Graphics g)
        {
            try
            {
                synchronized (nonVideoComponents)
                {
                    for (NonVideoComponent nonVideoComponent
                            : nonVideoComponents)
                        nonVideoComponent.paint();
                }
            }
            finally
            {
                super.paint(g);
            }
        }

        @Override
        protected void processMouseEvent(MouseEvent e)
        {
            /*
             * Act as a "glass pane" i.e. be transparent with respect to
             * MouseEvents and dispatch them to whatever is underneath.
             */
            if (!dispatchMouseEvent(e))
                super.processMouseEvent(e);
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e)
        {
            /*
             * Act as a "glass pane" i.e. be transparent with respect to
             * MouseEvents and dispatch them to whatever is underneath.
             */
            if (!dispatchMouseEvent(e))
                super.processMouseMotionEvent(e);
        }

        /**
         * Removes all <tt>NonVideoComponent</tt>s from this
         * <tt>SwingVideoComponentCanvas</tt> so that their associated
         * <tt>Component</tt>s are no longer painted by the reperesented native
         * <tt>JAWTRenderer</tt>.
         */
        private void removeAllNonVideoComponents()
        {
            synchronized (nonVideoComponents)
            {
                Iterator<NonVideoComponent> nonVideoComponentIter
                    = nonVideoComponents.iterator();

                while (nonVideoComponentIter.hasNext())
                {
                    NonVideoComponent nonVideoComponent
                        = nonVideoComponentIter.next();

                    nonVideoComponentIter.remove();
                    nonVideoComponent.dispose();
                }
            }
        }

        @Override
        public void removeNotify()
        {
            mousePressedComponent = null;

            if (parent != null)
            {
                parent.removeContainerListener(parentContainerListener);
                removeAllNonVideoComponents();
                parent = null;
            }

            synchronized (getHandleLock())
            {
                long handle = getHandle();

                if (handle != 0)
                {
                    try
                    {
                        JAWTRenderer.close(handle, this);
                    }
                    finally
                    {
                        this.handle = 0;
                    }
                }
            }

            super.removeNotify();
        }
    }
}
