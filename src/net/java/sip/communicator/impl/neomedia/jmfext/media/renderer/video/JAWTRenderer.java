/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.video;

import java.awt.*;
import java.lang.reflect.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.renderer.*;
import javax.swing.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a <tt>VideoRenderer</tt> which uses JAWT to perform native
 * painting in an AWT or Swing <tt>Component</tt>.
 *
 * @author Lyubomir Marinov
 */
public class JAWTRenderer
    extends AbstractRenderer<VideoFormat>
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
        System.loadLibrary("jnawtrenderer");

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
            ConfigurationService cfg
                = NeomediaActivator.getConfigurationService();

            if ((cfg == null)
                    || cfg.getBoolean(
                            JAWTRenderer.class.getName()
                                + ".USE_MACOSX_CALAYERS",
                            false))
            {
                String hwModel = sysctlbyname("hw.model");

                USE_MACOSX_CALAYERS
                    = (hwModel == null)
                        || !(hwModel.startsWith("MacBookPro8")
                                || hwModel.startsWith("MacBookAir4"));
            }
            else
                USE_MACOSX_CALAYERS = false;
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
     * The handle to the native counterpart of this <tt>JAWTRenderer</tt>.
     */
    private long handle = 0;

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

    static native void addNotifyLightweightComponent(
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
    static native void close(long handle, Component component);

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
            StringBuilder componentClassName = new StringBuilder();

            componentClassName.append(JAWTRenderer.class.getName());
            if (USE_MACOSX_CALAYERS && OSUtils.IS_MAC)
                componentClassName.append("Swing");
            else if (OSUtils.IS_ANDROID)
                componentClassName.append("Android");
            componentClassName.append("VideoComponent");

            Class<?> componentClass;

            try
            {
                componentClass = Class.forName(componentClassName.toString());
            }
            catch (ClassNotFoundException cnfe)
            {
                throw new RuntimeException(cnfe);
            }

            Constructor<?> componentConstructor;

            try
            {
                componentConstructor
                    = componentClass.getConstructor(JAWTRenderer.class);
            }
            catch (NoSuchMethodException nsme)
            {
                throw new RuntimeException(nsme);
            }
            try
            {
                component = (Component) componentConstructor.newInstance(this);
            }
            catch (IllegalAccessException iae)
            {
                throw new RuntimeException(iae);
            }
            catch (InstantiationException ie)
            {
                throw new RuntimeException(ie);
            }
            catch (InvocationTargetException ite)
            {
                throw new RuntimeException(ite);
            }
        }
        return component;
    }

    /**
     * Gets the handle to the native counterpart of this <tt>JAWTRenderer</tt>.
     *
     * @return the handle to the native counterpart of this
     * <tt>JAWTRenderer</tt>
     */
    long getHandle()
    {
        return handle;
    }

    /**
     * Gets the <tt>Object</tt> which synchronizes the access to the handle to
     * the native counterpart of this <tt>JAWTRenderer</tt>.
     *
     * @return the <tt>Object</tt> which synchronizes the access to the handle
     * to the native counterpart of this <tt>JAWTRenderer</tt>
     */
    Object getHandleLock()
    {
        return this;
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
         * The #addNotify() invocation, if any, should happen outside the
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
    static native long open(Component component)
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
    static native boolean paint(
            long handle, Component component, Graphics g);

    static native boolean paintLightweightComponent(
            long handle, Component component, Graphics g);

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
    static native boolean process(
            long handle,
            Component component,
            int[] data, int offset, int length,
            int width, int height);

    static native void processLightweightComponentEvent(
            long handle,
            int x, int y, int width, int height);

    static native void removeNotifyLightweightComponent(
            long handle, Component component);

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
    @Override
    public Format setInputFormat(Format format)
    {
        super.setInputFormat(format);

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
}
