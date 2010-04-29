/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.video;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.renderer.*;

import net.java.sip.communicator.impl.neomedia.control.*;

/**
 * Implements a <tt>VideoRenderer</tt> which uses JAWT to perform native
 * painting in an AWT <tt>Canvas</tt>.
 *
 * @author Lubomir Marinov
 */
public class JAWTRenderer
    extends ControlsAdapter
    implements VideoRenderer
{

    /**
     * The human-readable <tt>PlugIn</tt> name of the <tt>JAWTRenderer</tt>
     * instances.
     */
    private static final String PLUGIN_NAME = "JAWT Renderer";

    private static final Format[] SUPPORTED_INPUT_FORMATS
        = new Format[]
                {
                    new RGBFormat(
                            null,
                            Format.NOT_SPECIFIED,
                            Format.intArray,
                            Format.NOT_SPECIFIED,
                            32,
                            0x00FF0000, 0x0000FF00, 0x000000FF)
                };

    static
    {
        System.loadLibrary("jawtrenderer");
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
            close(handle, getComponent());
            handle = 0;
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
     * platform-specific info of <tt>component</tt> is not guranteed to be
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
            component = new Canvas()
            {
                @Override
                public void paint(Graphics g)
                {
                    synchronized (JAWTRenderer.this)
                    {
                        if (handle != 0)
                            JAWTRenderer.this.paint(handle, this, g);
                    }
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
     */
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (handle == 0)
        {
            handle = open(getComponent());
            if (handle == 0)
            {
                throw
                    new ResourceUnavailableException(
                            "Failed to open the native counterpart of"
                                + " JAWTRenderer");
            }
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
     */
    private static native void paint(
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
            boolean processed
                = process(
                    handle,
                    component,
                    (int[]) buffer.getData(), buffer.getOffset(), bufferLength,
                    size.width, size.height);

            if (processed)
            {
                component.repaint(); 
                return BUFFER_PROCESSED_OK;
            }
            else
                return BUFFER_PROCESSED_FAILED;
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
     */
    private static native boolean process(
            long handle,
            Component component,
            int[] data, int offset, int length,
            int width, int height);

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

            if ((preferredSize == null)
                    || (preferredSize.width < 1)
                    || (preferredSize.height < 1))
            {
                component.setPreferredSize(
                        new Dimension(inputWidth, inputHeight));
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
}
