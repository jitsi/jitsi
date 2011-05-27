/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
import javax.media.format.*;
import javax.media.renderer.*;
import javax.swing.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a <tt>VideoRenderer</tt> which uses JAWT to perform native
 * painting in an AWT or Swing <tt>Component</tt>.
 *
 * @author Lyubomir Marinov
 */
public class JAWTRenderer
    extends ControlsAdapter
    implements VideoRenderer
{
    /**
     * The <tt>Logger</tt> used by the <tt>JAWTRenderer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(JAWTRenderer.class);

    /**
     * The human-readable <tt>PlugIn</tt> name of the <tt>JAWTRenderer</tt>
     * instances.
     */
    private static final String PLUGIN_NAME = "JAWT Renderer";

    /**
     * The array of supported input formats.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS
        = new Format[]
                {
                    OSUtils.IS_LINUX
                        ? new YUVFormat(
                                null /* size */,
                                Format.NOT_SPECIFIED /* maxDataLength */,
                                Format.intArray,
                                Format.NOT_SPECIFIED /* frameRate */,
                                YUVFormat.YUV_420,
                                Format.NOT_SPECIFIED /* strideY */,
                                Format.NOT_SPECIFIED /* strideUV */,
                                Format.NOT_SPECIFIED /* offsetY */,
                                Format.NOT_SPECIFIED /* offsetU */,
                                Format.NOT_SPECIFIED /* offsetV */)
                        : new RGBFormat(
                                null,
                                Format.NOT_SPECIFIED,
                                Format.intArray,
                                Format.NOT_SPECIFIED,
                                32,
                                0x00FF0000, 0x0000FF00, 0x000000FF)
                };

    /**
     * The indicator which determines whether <tt>CALayer</tt>-based painting is
     * to be performed by <tt>JAWTRenderer</tt> on Mac OS X.
     */
    private static final boolean USE_MACOSX_CALAYERS;

    static
    {
        System.loadLibrary("jawtrenderer");

        /*
         * XXX The native JAWTRenderer implementation on Mac OS X which paints
         * in a CALayer-like fashion has been determined through testing to not
         * work as expected on MacBookPro8. Unfortunately, the cause of the
         * problem has not been determined. As a workaround, fall back to the
         * alternative implementation (currently used on the other supported
         * operating systems) on the problematic model. 
         */
        if (OSUtils.IS_MAC)
        {
            String hwModel = sysctlbyname("hw.model");

            if ((hwModel != null) && hwModel.startsWith("MacBookPro8"))
                USE_MACOSX_CALAYERS = false;
            else
                USE_MACOSX_CALAYERS = true;
        }
        else
        {
            // CALayer-like painting is currently only supported on Mac OS X.
            USE_MACOSX_CALAYERS = false;
        }
    }

    /**
     * The AWT <tt>Component</tt> into which this <tt>VideoRenderer</tt> draws.
     */
    private Component component;

    /**
     * The handle of the native counterpart of this <tt>JAWTRenderer</tt>.
     */
    long handle = 0;

    /**
     * The <tt>VideoFormat</tt> of the input processed by this
     * <tt>Renderer</tt>.
     */
    private VideoFormat inputFormat;

    /**
     * The last known height of the input processed by this
     * <tt>JAWTRenderer</tt>.
     */
    private int inputHeight = 0;

    /**
     * The last known width of the input processed by this
     * <tt>JAWTRenderer</tt>.
     */
    private int inputWidth = 0;

    /**
     * Initializes a new <tt>JAWTRenderer</tt> instance.
     */
    public JAWTRenderer()
    {
    }

    private static native void addNotifyLightweightComponent(
            long handle, Component component,
            long parentHandle);

    /**
     * Closes this <tt>PlugIn</tt> and releases the resources it has retained
     * during its execution. No more data will be accepted by this
     * <tt>PlugIn</tt> afterwards. A closed <tt>PlugIn</tt> can be reinstated by
     * calling <tt>open</tt> again.
     */
    public synchronized void close()
    {
        if (handle != 0)
        {
            try
            {
                /*
                 * It may or may not be necessary to ensure that #removeNotify()
                 * is delivered to the native counterpart of this JAWTRenderer.
                 * Anyway, do so for the sake of completeness.
                 */
                /*
                 * Unfortunately, doing so in the synchronized block leads to a
                 * deadlock.
                 */
//                if (JComponent.isLightweightComponent(component))
//                {
//                    Container parent = component.getParent();
//
//                    if (parent != null)
//                        parent.remove(component);
//                }
            }
            finally
            {
                close(handle, component);
                handle = 0;
            }
        }
    }

    /**
     * Closes the native counterpart of a <tt>JAWTRenderer</tt> specified by its
     * handle as returned by {@link #open(Component)} and rendering into a
     * specific AWT <tt>Component</tt>. Releases the resources which the
     * specified native counterpart has retained during its execution and its
     * handle is considered to be invalid afterwards.
     *
     * @param handle the handle to the native counterpart of a
     * <tt>JAWTRenderer</tt> as returned by {@link #open(Component)} which is to
     * be closed
     * @param component the AWT <tt>Component</tt> into which the
     * <tt>JAWTRenderer</tt> and its native counterpart are drawing. The
     * platform-specific info of <tt>component</tt> is not guaranteed to be
     * valid.
     */
    private static native void close(long handle, Component component);

    /**
     * Gets the region in the component of this <tt>VideoRenderer</tt> where the
     * video is rendered.
     *
     * @return the region in the component of this <tt>VideoRenderer</tt> where
     * the video is rendered; <tt>null</tt> if the entire component is used
     */
    public Rectangle getBounds()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the AWT <tt>Component</tt> into which this <tt>VideoRenderer</tt>
     * draws.
     *
     * @return the AWT <tt>Component</tt> into which this <tt>VideoRenderer</tt>
     * draws
     */
    public synchronized Component getComponent()
    {
        if (component == null)
        {
            component
                = (USE_MACOSX_CALAYERS && OSUtils.IS_MAC)
                    ? new SwingVideoComponent()
                    : new AWTVideoComponent()
                            {
                                /* Implements AWTVideoComponent#getHandle(). */
                                protected long getHandle()
                                {
                                    return JAWTRenderer.this.handle;
                                }

                                /*
                                 * Implements AWTVideoComponent#getHandleLock().
                                 */
                                protected Object getHandleLock()
                                {
                                    return JAWTRenderer.this;
                                }
                            };
        }
        return component;
    }

    /**
     * Gets the human-readable name of this <tt>PlugIn</tt>.
     *
     * @return the human-readable name of this <tt>PlugIn</tt>
     */
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Gets the list of input <tt>Format</tt>s supported by this
     * <tt>Renderer</tt>.
     *
     * @return an array of <tt>Format</tt> elements which represent the input
     * <tt>Format</tt>s supported by this <tt>Renderer</tt>
     */
    public Format[] getSupportedInputFormats()
    {
        return SUPPORTED_INPUT_FORMATS.clone();
    }

    /**
     * Opens this <tt>PlugIn</tt> and acquires the resources that it needs to
     * operate. The input format of this <tt>Renderer</tt> has to be set before
     * <tt>open</tt> is called. Buffers should not be passed into this
     * <tt>PlugIn</tt> without first calling <tt>open</tt>.
     *
     * @throws ResourceUnavailableException if there is a problem during opening
     */
    public void open()
        throws ResourceUnavailableException
    {
        boolean addNotify;
        final Component component;

        synchronized (this)
        {
            if (handle == 0)
            {
                /*
                 * If this JAWTRenderer gets opened after its visual/video
                 * Component has been created, send addNotify to the Component
                 * once this JAWTRenderer gets opened so that the Component may
                 * use the handle if it needs to.
                 */
                addNotify
                    = (this.component != null)
                        && (this.component.getParent() != null);
                component = getComponent();

                handle = open(component);
                if (handle == 0)
                {
                    throw new ResourceUnavailableException(
                            "Failed to open the native counterpart of JAWTRenderer");
                }
            }
            else
            {
                addNotify = false;
                component = null;
            }
        }
        /*
         * The #addNotify() invocation, if any, shoud happen outside the
         * synchronized block in order to avoid a deadlock.
         */
        if (addNotify)
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            component.addNotify();
                        }
                    });
        }
    }

    /**
     * Opens a handle to a native counterpart of a <tt>JAWTRenderer</tt> which
     * is to draw into a specific AWT <tt>Component</tt>.
     *
     * @param component the AWT <tt>Component</tt> into which a
     * <tt>JAWTRenderer</tt> and the native counterpart to be opened are to
     * draw. The platform-specific info of <tt>component</tt> is not guaranteed
     * to be valid.
     * @return a handle to a native counterpart of a <tt>JAWTRenderer</tt> which
     * is to draw into the specified AWT <tt>Component</tt>
     * @throws ResourceUnavailableException if there is a problem during opening
     */
    private static native long open(Component component)
        throws ResourceUnavailableException;

    /**
     * Paints a specific <tt>Component</tt> which is the AWT <tt>Component</tt>
     * of a <tt>JAWTRenderer</tt> specified by the handle to its native
     * counterpart.
     *
     * @param handle the handle to the native counterpart of a
     * <tt>JAWTRenderer</tt> which is to draw into the specified AWT
     * <tt>Component</tt>
     * @param component the AWT <tt>Component</tt> into which the
     * <tt>JAWTRenderer</tt> and its native counterpart specified by
     * <tt>handle</tt> are to draw. The platform-specific info of
     * <tt>component</tt> is guaranteed to be valid only during the execution of
     * <tt>paint</tt>.
     * @param g the <tt>Graphics</tt> context into which the drawing is to be
     * performed
     * @return <tt>true</tt> if the native counterpart of a
     * <tt>JAWTRenderer</tt> wants to continue receiving the <tt>paint</tt>
     * calls on the AWT <tt>Component</tt>; otherwise, false. For example, after
     * the native counterpart has been able to acquire the native handle of the
     * AWT <tt>Component</tt>, it may be able to determine when the native
     * handle needs painting without waiting for AWT to call <tt>paint</tt> on
     * the <tt>Component</tt>. In such a scenario, the native counterpart may
     * indicate with <tt>false</tt> that it does not need further <tt>paint</tt>
     * deliveries.
     */
    private static native boolean paint(
            long handle, Component component, Graphics g);

    private static native boolean paintLightweightComponent(
            long handle, JComponent component, Graphics g);

    /**
     * Processes the data provided in a specific <tt>Buffer</tt> and renders it
     * to the output device represented by this <tt>Renderer</tt>.
     *
     * @param buffer a <tt>Buffer</tt> containing the data to be processed and
     * rendered
     * @return <tt>BUFFER_PROCESSED_OK</tt> if the processing is successful;
     * otherwise, the other possible return codes defined in the <tt>PlugIn</tt>
     * interface
     */
    public synchronized int process(Buffer buffer)
    {
        if (buffer.isDiscard())
            return BUFFER_PROCESSED_OK;

        int bufferLength = buffer.getLength();

        if (bufferLength == 0)
            return BUFFER_PROCESSED_OK;

        Format bufferFormat = buffer.getFormat();

        if ((bufferFormat != null)
                && (bufferFormat != this.inputFormat)
                && !bufferFormat.equals(this.inputFormat))
        {
            if (setInputFormat(bufferFormat) == null)
                return BUFFER_PROCESSED_FAILED;
        }

        if (handle == 0)
            return BUFFER_PROCESSED_FAILED;
        else
        {
            Dimension size = null;

            if (bufferFormat != null)
                size = ((VideoFormat) bufferFormat).getSize();
            if (size == null)
            {
                size = this.inputFormat.getSize();
                if (size == null)
                    return BUFFER_PROCESSED_FAILED;
            }

            Component component = getComponent();
            boolean repaint = false;

            repaint
                = process(
                    handle,
                    component,
                    (int[]) buffer.getData(), buffer.getOffset(), bufferLength,
                    size.width, size.height);

            if (repaint)
                component.repaint();
            return BUFFER_PROCESSED_OK;
        }
    }

    /**
     * Processes the data provided in a specific <tt>int</tt> array with a
     * specific offset and length and renders it to the output device
     * represented by a <tt>JAWTRenderer</tt> specified by the handle to it
     * native counterpart.
     *
     * @param handle the handle to the native counterpart of a
     * <tt>JAWTRenderer</tt> to process the specified data and render it
     * @param component the <tt>AWT</tt> component into which the specified
     * <tt>JAWTRenderer</tt> and its native counterpart draw
     * @param data an <tt>int</tt> array which contains the data to be processed
     * and rendered
     * @param offset the index in <tt>data</tt> at which the data to be
     * processed and rendered starts
     * @param length the number of elements in <tt>data</tt> starting at
     * <tt>offset</tt> which represent the data to be processed and rendered
     * @param width the width of the video frame in <tt>data</tt>
     * @param height the height of the video frame in <tt>data</tt>
     * @return <tt>true</tt> if data has been successfully processed
     */
    private static native boolean process(
            long handle,
            Component component,
            int[] data, int offset, int length,
            int width, int height);

    private static native void processLightweightComponentEvent(
            long handle,
            int x, int y, int width, int height);

    private static native void removeNotifyLightweightComponent(
            long handle, Component component);

    /**
     * Resets the state of this <tt>PlugIn</tt>.
     */
    public void reset()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Sets the region in the component of this <tt>VideoRenderer</tt> where the
     * video is to be rendered.
     *
     * @param bounds the region in the component of this <tt>VideoRenderer</tt>
     * where the video is to be rendered; <tt>null</tt> if the entire component
     * is to be used
     */
    public void setBounds(Rectangle bounds)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Sets the AWT <tt>Component</tt> into which this <tt>VideoRenderer</tt> is
     * to draw. <tt>JAWTRenderer</tt> cannot draw into any other AWT
     * <tt>Component</tt> but its own so it always returns <tt>false</tt>.
     *
     * @param component the AWT <tt>Component</tt> into which this
     * <tt>VideoRenderer</tt> is to draw
     * @return <tt>true</tt> if this <tt>VideoRenderer</tt> accepted the
     * specified <tt>component</tt> as the AWT <tt>Component</tt> into which it
     * is to draw; <tt>false</tt>, otherwise
     */
    public boolean setComponent(Component component)
    {
        // We cannot draw into any other AWT Component but our own.
        return false;
    }

    /**
     * Sets the <tt>Format</tt> of the input to be processed by this
     * <tt>Renderer</tt>.
     *
     * @param format the <tt>Format</tt> to be set as the <tt>Format</tt> of the
     * input to be processed by this <tt>Renderer</tt>
     * @return the <tt>Format</tt> of the input to be processed by this
     * <tt>Renderer</tt> if the specified <tt>format</tt> is supported or
     * <tt>null</tt> if the specified <tt>format</tt> is not supported by this
     * <tt>Renderer</tt>. Typically, it is the supported input <tt>Format</tt>
     * which most closely matches the specified <tt>Format</tt>.
     */
    public Format setInputFormat(Format format)
    {
        Format matchingFormat = null;

        for (Format supportedInputFormat : getSupportedInputFormats())
        {
            if (supportedInputFormat.matches(format))
            {
                matchingFormat = supportedInputFormat.intersects(format);
                break;
            }
        }
        if (matchingFormat == null)
            return null;

        inputFormat = (VideoFormat) format;

        /*
         * Know the width and height of the input because we'll be depicting it
         * and we may want, for example, to report it as the preferred size of
         * our AWT Component.
         */
        Dimension inputSize = inputFormat.getSize();

        if (inputSize != null)
        {
            inputWidth = inputSize.width;
            inputHeight = inputSize.height;
        }

        /*
         * Reflect the width and height of the input onto the preferredSize of
         * our AWT Component (if necessary).
         */
        if ((inputWidth > 0) && (inputHeight > 0))
        {
            Component component = getComponent();
            Dimension preferredSize = component.getPreferredSize();

            /*
             * Apart from the simplest of cases in which the component has no
             * preferredSize, it is also necessary to reflect the width and
             * height of the input onto the preferredSize when the ratio of the
             * input is different than the ratio of the preferredSize.
             */
            if ((preferredSize == null)
                    || (preferredSize.width < 1)
                    || (preferredSize.height < 1)
                    || (preferredSize.width * inputHeight
                            != preferredSize.height * inputWidth))
            {
                component.setPreferredSize(
                        new Dimension(inputWidth, inputHeight));
            }

            /*
             * If the component does not have a size, it looks strange given
             * that we know a preferredSize for it.
             */
            Dimension size = component.getSize();

            if ((size.width < 1) || (size.height < 1))
            {
                preferredSize = component.getPreferredSize();
                component.setSize(preferredSize.width, preferredSize.height);
            }
        }

        return inputFormat;
    }

    /**
     * Starts the rendering process. Begins rendering any data available in the
     * internal buffers of this <tt>Renderer</tt>.
     */
    public void start()
    {
    }

    /**
     * Stops the rendering process.
     */
    public void stop()
    {
    }

    private static native String sysctlbyname(String name);

    /**
     * Implements an AWT <tt>Component</tt> in which this <tt>JAWTRenderer</tt>
     * paints.
     */
    private static abstract class AWTVideoComponent
        extends Canvas
    {

        /**
         * The indicator which determines whether the native counterpart of this
         * <tt>JAWTRenderer</tt> wants <tt>paint</tt> calls on its AWT
         * <tt>Component</tt> to be delivered. For example, after the native
         * counterpart has been able to acquire the native handle of the AWT
         * <tt>Component</tt>, it may be able to determine when the native
         * handle needs painting without waiting for AWT to call <tt>paint</tt>
         * on the <tt>Component</tt>. In such a scenario, the native counterpart
         * may indicate with <tt>false</tt> that it does not need further
         * <tt>paint</tt> deliveries.
         */
        private boolean wantsPaint = true;

        @Override
        public void addNotify()
        {
            super.addNotify();

            wantsPaint = true;
        }

        /**
         * Gets the handle of the native counterpart of the
         * <tt>JAWTRenderer</tt> which paints in this
         * <tt>AWTVideoComponent</tt>.
         *
         * @return the handle of the native counterpart of the
         * <tt>JAWTRenderer</tt> which paints in this <tt>AWTVideoComponent</tt>
         */
        protected abstract long getHandle();

        /**
         * Gets the synchronization lock which protects the access to the
         * <tt>handle</tt> property of this <tt>AWTVideoComponent</tt>.
         *
         * @return the synchronization lock which protects the access to the
         * <tt>handle</tt> property of this <tt>AWTVideoComponent</tt>
         */
        protected abstract Object getHandleLock();

        @Override
        public void paint(Graphics g)
        {
            synchronized (getHandleLock())
            {
                long handle = getHandle();

                if (wantsPaint && (handle != 0))
                    wantsPaint = JAWTRenderer.paint(handle, this, g);
            }
        }

        @Override
        public void removeNotify()
        {
            /*
             * In case the associated JAWTRenderer has said that it does not
             * want paint events/notifications, ask it again next time because
             * the native handle of this Canvas may be recreated.
             */
            wantsPaint = true;
            
            super.removeNotify();
        }

        @Override
        public void update(Graphics g)
        {
            /*
             * Skip the filling with the background color because it causes
             * flickering.
             */
            paint(g);
        }
    };

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
     * Implements a Swing <tt>Component</tt> in which this <tt>JAWTRenderer</tt>
     * paints.
     */
    public class SwingVideoComponent
        extends JPanel
    {
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
         * The indicator which determines whether the native counterpart of this
         * <tt>JAWTRenderer</tt> wants <tt>paint</tt> calls on its Swing
         * <tt>Component</tt> to be delivered.
         *
         * @see AWTVideoComponent#wantsPaint
         */
        private boolean wantsPaint = true;

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

            synchronized (JAWTRenderer.this)
            {
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
         */
        public Component createCanvas()
        {
            return new SwingVideoComponentCanvas();
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
            synchronized (JAWTRenderer.this)
            {
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
                synchronized (JAWTRenderer.this)
                {
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

            synchronized (JAWTRenderer.this)
            {
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
    }

    /**
     * Implements an AWT <tt>Component</tt> in which
     * <tt>SwingVideoComponent</tt>s can be rendered.
     */
    private static class SwingVideoComponentCanvas
        extends AWTVideoComponent
    {
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

                    if (!(c instanceof AWTVideoComponent)
                            && !(c instanceof SwingVideoComponent))
                    {
                        nonVideoComponentAdded(c);
                    }
                }

                public void componentRemoved(ContainerEvent e)
                {
                    Component c = e.getChild();

                    if (mousePressedComponent == c)
                        mousePressedComponent = null;

                    if (!(c instanceof AWTVideoComponent)
                            && !(c instanceof SwingVideoComponent))
                    {
                        nonVideoComponentRemoved(c);
                    }
                    else if (SwingVideoComponentCanvas.this.equals(c))
                        removeAllNonVideoComponents();
                }
            };

        /**
         * Initializes a new <tt>SwingVideoComponentCanvas</tt> instance.
         */
        public SwingVideoComponentCanvas()
        {
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
                        this.handle = open(this);
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

        /* Implements AWTVideoComponent#getHandle(). */
        protected long getHandle()
        {
            return handle;
        }

        /* Implements AWTVideoComponent#getHandleLock(). */
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
                        close(handle, this);
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
