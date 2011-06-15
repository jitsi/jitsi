/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.util.*;
import java.util.regex.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.QualityControl; // disambiguation
import net.java.sip.communicator.service.neomedia.control.KeyFrameControl; // disambiguation
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaStreamImpl</tt> in order to provide an implementation of
 * <tt>VideoMediaStream</tt>.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class VideoMediaStreamImpl
    extends MediaStreamImpl
    implements VideoMediaStream
{

    /**
     * The <tt>Logger</tt> used by the <tt>VideoMediaStreamImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(VideoMediaStreamImpl.class);

    /**
     * Negotiated output size of the video stream.
     * It may need to scale original capture device stream.
     */
    private Dimension outputSize;

    /**
     * Use or not RTCP feedback Picture Loss Indication messages.
     */
    private boolean usePLI = false;

    /**
     * The <tt>KeyFrameControl</tt> of this <tt>VideoMediaStream</tt>.
     */
    private final KeyFrameControlImpl keyFrameControl
        = new KeyFrameControlImpl();

    /**
     * The <tt>QualityControl</tt> of this <tt>VideoMediaStream</tt>.
     */
    private final QualityControlImpl qualityControl = new QualityControlImpl();

    /**
     * Selects the <tt>VideoFormat</tt> from the list of supported formats of a
     * specific video <tt>DataSource</tt> which has a size as close as possible
     * to a specific size and sets it as the format of the specified video
     * <tt>DataSource</tt>.
     *
     * @param videoDS the video <tt>DataSource</tt> which is to have its
     * supported formats examined and its format changed to the
     * <tt>VideoFormat</tt> which is as close as possible to the specified
     * <tt>preferredWidth</tt> and <tt>preferredHeight</tt>
     * @param preferredWidth the width of the <tt>VideoFormat</tt> to be
     * selected
     * @param preferredHeight the height of the <tt>VideoFormat</tt> to be
     * selected
     * @return the size of the <tt>VideoFormat</tt> from the list of supported
     * formats of <tt>videoDS</tt> which is as close as possible to
     * <tt>preferredWidth</tt> and <tt>preferredHeight</tt> and which has been
     * set as the format of <tt>videoDS</tt>
     */
    public static Dimension selectVideoSize(
            DataSource videoDS,
            final int preferredWidth, final int preferredHeight)
    {
        if (videoDS == null)
            return null;

        FormatControl formatControl
            = (FormatControl) videoDS.getControl(FormatControl.class.getName());

        if (formatControl == null)
            return null;

        Format[] formats = formatControl.getSupportedFormats();
        final int count = formats.length;

        if (count < 1)
            return null;

        VideoFormat selectedFormat = null;

        if (count == 1)
            selectedFormat = (VideoFormat) formats[0];
        else
        {
            class FormatInfo
            {
                public final VideoFormat format;

                public final double difference;

                public FormatInfo(VideoFormat format)
                {
                    this.format = format;

                    Dimension size = format.getSize();

                    int width = (size == null) ? 0 : size.width;
                    double xScale;

                    if (width == 0)
                        xScale = Double.POSITIVE_INFINITY;
                    else if (width == preferredWidth)
                        xScale = 1;
                    else
                        xScale = (preferredWidth / (double) width);

                    int height = (size == null) ? 0 : size.height;
                    double yScale;

                    if (height == 0)
                        yScale = Double.POSITIVE_INFINITY;
                    else if (height == preferredHeight)
                        yScale = 1;
                    else
                        yScale = (preferredHeight / (double) height);

                    difference = Math.abs(1 - Math.min(xScale, yScale));
                }
            }

            FormatInfo[] infos = new FormatInfo[count];

            for (int i = 0; i < count; i++)
            {
                FormatInfo info
                    = infos[i]
                        = new FormatInfo((VideoFormat) formats[i]);

                if (info.difference == 0)
                {
                    selectedFormat = info.format;
                    break;
                }
            }
            if (selectedFormat == null)
            {
                Arrays.sort(infos, new Comparator<FormatInfo>()
                {
                    public int compare(FormatInfo info0, FormatInfo info1)
                    {
                        return
                            Double.compare(info0.difference, info1.difference);
                    }
                });
                selectedFormat = infos[0].format;
            }

            /*
             * If videoDS states to support any size, use the sizes that we
             * support which is closest(or smaller) to the preferred one.
             */
            if ((selectedFormat != null)
                    && (selectedFormat.getSize() == null))
            {
                VideoFormat currentFormat
                    = (VideoFormat) formatControl.getFormat();
                Dimension currentSize = null;
                int width = preferredWidth;
                int height = preferredHeight;

                // Try to preserve the aspect ratio
                if (currentFormat != null)
                    currentSize = currentFormat.getSize();

                // lets choose the closest size to the preferred one
                for(Dimension supported :
                        DeviceConfiguration.SUPPORTED_RESOLUTIONS)
                {
                    if(supported.height <= preferredHeight
                       && supported.width <= preferredWidth)
                        currentSize = supported;
                }

                if ((currentSize.width > 0) && (currentSize.height > 0))
                {
                    width = currentSize.width;
                    height = currentSize.height;
                }
                selectedFormat
                    = (VideoFormat)new VideoFormat(
                                        null,
                                        new Dimension(width, height),
                                        Format.NOT_SPECIFIED,
                                        null,
                                        Format.NOT_SPECIFIED)
                                .intersects(selectedFormat);
            }
        }

        Format setFormat = formatControl.setFormat(selectedFormat);

        return
            (setFormat instanceof VideoFormat)
                ? ((VideoFormat) setFormat).getSize()
                : null;
    }

    /**
     * The <tt>VideoListener</tt> which handles <tt>VideoEvent</tt>s from the
     * <tt>MediaDeviceSession</tt> of this instance and fires respective
     * <tt>VideoEvent</tt>s from this <tt>VideoMediaStream</tt> to its
     * <tt>VideoListener</tt>s.
     */
    private VideoListener deviceSessionVideoListener;

    /**
     * The facility which aids this instance in managing a list of
     * <tt>VideoListener</tt>s and firing <tt>VideoEvent</tt>s to them.
     */
    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this);

    /**
     * Initializes a new <tt>VideoMediaStreamImpl</tt> instance which will use
     * the specified <tt>MediaDevice</tt> for both capture and playback of video
     * exchanged via the specified <tt>StreamConnector</tt>.
     *
     * @param connector the <tt>StreamConnector</tt> the new instance is to use
     * for sending and receiving video
     * @param device the <tt>MediaDevice</tt> the new instance is to use for
     * both capture and playback of video exchanged via the specified
     * <tt>StreamConnector</tt>
     * @param zrtpControl a control which is already created, used to control
     * the zrtp operations.
     */
    public VideoMediaStreamImpl(StreamConnector connector, MediaDevice device,
        ZrtpControlImpl zrtpControl)
    {
        super(connector, device, zrtpControl);

        if(logger.isTraceEnabled())
            logger.trace("Created Video Stream with hashCode " + hashCode());
    }

    /**
     * Adds a specific <tt>VideoListener</tt> to this <tt>VideoMediaStream</tt>
     * in order to receive notifications when visual/video <tt>Component</tt>s
     * are being added and removed.
     * <p>
     * Adding a listener which has already been added does nothing i.e. it is
     * not added more than once and thus does not receive one and the same
     * <tt>VideoEvent</tt> multiple times.
     * </p>
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoMediaStream</tt>
     */
    public void addVideoListener(VideoListener listener)
    {
        videoNotifierSupport.addVideoListener(listener);
    }

    /**
     * Performs any optional configuration on a specific
     * <tt>RTPConnectorOuputStream</tt> of an <tt>RTPManager</tt> to be used by
     * this <tt>MediaStreamImpl</tt>.
     *
     * @param dataOutputStream the <tt>RTPConnectorOutputStream</tt> to be used
     * by an <tt>RTPManager</tt> of this <tt>MediaStreamImpl</tt> and to be
     * configured
     */
    @Override
    protected void configureDataOutputStream(
            RTPConnectorOutputStream dataOutputStream)
    {
        super.configureDataOutputStream(dataOutputStream);

        int maxBandwidth
            = NeomediaActivator.getMediaServiceImpl().getDeviceConfiguration()
                    .getVideoMaxBandwidth();

        //maximum one packet for X milliseconds(the settings are for one second)
        dataOutputStream.setMaxPacketsPerMillis(1, 1000 / maxBandwidth);
    }

    /**
     * Performs any optional configuration on the <tt>BufferControl</tt> of the
     * specified <tt>RTPManager</tt> which is to be used as the
     * <tt>RTPManager</tt> of this <tt>MediaStreamImpl</tt>.
     *
     * @param rtpManager the <tt>RTPManager</tt> which is to be used by this
     * <tt>MediaStreamImpl</tt>
     * @param bufferControl the <tt>BufferControl</tt> of <tt>rtpManager</tt> on
     * which any optional configuration is to be performed
     */
    @Override
    protected void configureRTPManagerBufferControl(
            RTPManager rtpManager,
            BufferControl bufferControl)
    {
        super.configureRTPManagerBufferControl(rtpManager, bufferControl);

        bufferControl.setBufferLength(BufferControl.MAX_VALUE);
    }

    /**
     * Creates the visual <tt>Component</tt> depicting the video being streamed
     * from the local peer to the remote peer.
     *
     * @return the visual <tt>Component</tt> depicting the video being streamed
     * from the local peer to the remote peer if it was immediately created or
     * <tt>null</tt> if it was not immediately created and it is to be delivered
     * to the currently registered <tt>VideoListener</tt>s in a
     * <tt>VideoEvent</tt> with type {@link VideoEvent#VIDEO_ADDED} and origin
     * {@link VideoEvent#LOCAL}
     */
    public Component createLocalVisualComponent()
    {
        MediaDeviceSession deviceSession = getDeviceSession();

        return
            (deviceSession instanceof VideoMediaDeviceSession)
                ? ((VideoMediaDeviceSession) deviceSession)
                    .createLocalVisualComponent()
                : null;
    }

    /**
     * Notifies this <tt>MediaStream</tt> that the <tt>MediaDevice</tt> (and
     * respectively the <tt>MediaDeviceSession</tt> with it) which this instance
     * uses for capture and playback of media has been changed. Makes sure that
     * the <tt>VideoListener</tt>s of this instance get <tt>VideoEvent</tt>s for
     * the new/current <tt>VideoMediaDeviceSession</tt> and not for the old one.
     *
     * @param oldValue the <tt>MediaDeviceSession</tt> with the
     * <tt>MediaDevice</tt> this instance used work with
     * @param newValue the <tt>MediaDeviceSession</tt> with the
     * <tt>MediaDevice</tt> this instance is to work with
     * @see MediaStreamImpl#deviceSessionChanged(MediaDeviceSession,
     * MediaDeviceSession)
     */
    @Override
    protected void deviceSessionChanged(
            MediaDeviceSession oldValue,
            MediaDeviceSession newValue)
    {
        super.deviceSessionChanged(oldValue, newValue);

        if (oldValue instanceof VideoMediaDeviceSession)
        {
            VideoMediaDeviceSession oldVideoMediaDeviceSession
                = (VideoMediaDeviceSession) oldValue;

            if (deviceSessionVideoListener != null)
                oldVideoMediaDeviceSession.removeVideoListener(
                        deviceSessionVideoListener);

            /*
             * The oldVideoMediaDeviceSession is being disconnected from this
             * VideoMediaStreamImpl so do not let it continue using its
             * keyFrameControl.
             */
            oldVideoMediaDeviceSession.setKeyFrameControl(null);
        }
        if (newValue instanceof VideoMediaDeviceSession)
        {
            VideoMediaDeviceSession newVideoMediaDeviceSession
                = (VideoMediaDeviceSession) newValue;

            if (deviceSessionVideoListener == null)
            {
                deviceSessionVideoListener = new VideoListener()
                {

                    /**
                     * Notifies that a visual <tt>Component</tt> representing
                     * video has been added to the provider this listener has
                     * been added to.
                     *
                     * @param e a <tt>VideoEvent</tt> describing the added
                     * visual <tt>Component</tt> representing video and the
                     * provider it was added into
                     * @see VideoListener#videoAdded(VideoEvent)
                     */
                    public void videoAdded(VideoEvent e)
                    {
                        if (fireVideoEvent(
                                e.getType(),
                                e.getVisualComponent(),
                                e.getOrigin()))
                            e.consume();
                    }

                    /**
                     * Notifies that a visual <tt>Component</tt> representing
                     * video has been removed from the provider this listener
                     * has been added to.
                     *
                     * @param e a <tt>VideoEvent</tt> describing the removed
                     * visual <tt>Component</tt> representing video and the
                     * provider it was removed from
                     * @see VideoListener#videoRemoved(VideoEvent)
                     */
                    public void videoRemoved(VideoEvent e)
                    {
                        videoAdded(e);
                    }

                    public void videoUpdate(VideoEvent e)
                    {
                        fireVideoEvent(e);
                    }
                };
            }
            newVideoMediaDeviceSession.addVideoListener(
                    deviceSessionVideoListener);

            newVideoMediaDeviceSession.setOutputSize(outputSize);

            RTPTransformConnector rtpConnector = getRTPConnector();

            if (rtpConnector != null)
                newVideoMediaDeviceSession.setConnector(rtpConnector);
            newVideoMediaDeviceSession.setRtcpFeedbackPLI(usePLI);

            /*
             * The newVideoMediaDeviceSession is being connected to this
             * VideoMediaStreamImpl so the key frame-related logic will be
             * controlled by the keyFrameControl of this VideoMediaStreamImpl.
             */
            newVideoMediaDeviceSession.setKeyFrameControl(keyFrameControl);
        }
    }

    /**
     * Disposes of the visual <tt>Component</tt> of the local peer.
     *
     * @param component the visual <tt>Component</tt> of the local peer to
     * dispose of
     */
    public void disposeLocalVisualComponent(Component component)
    {
        MediaDeviceSession deviceSession = getDeviceSession();

        if(deviceSession instanceof VideoMediaDeviceSession)
            ((VideoMediaDeviceSession) deviceSession)
                .disposeLocalVisualComponent(component);
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>VideoMediaStream</tt> about a specific type of change in the
     * availability of a specific visual <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this <tt>VideoMediaStream</tt>
     * @param origin {@link VideoEvent#LOCAL} if the origin of the video is
     * local (e.g. it is being locally captured); {@link VideoEvent#REMOTE} if
     * the origin of the video is remote (e.g. a remote peer is streaming it)
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    protected boolean fireVideoEvent(
            int type,
            Component visualComponent,
            int origin)
    {
        if (logger.isTraceEnabled())
            logger
                .trace(
                    "Firing VideoEvent with type "
                        + VideoEvent.typeToString(type)
                        + " and origin "
                        + VideoEvent.originToString(origin));

        return
            videoNotifierSupport.fireVideoEvent(type, visualComponent, origin);
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this instance about
     * a specific <tt>VideoEvent</tt>.
     *
     * @param event the <tt>VideoEvent</tt> to be fired to the
     * <tt>VideoListener</tt>s registered with this instance
     */
    protected void fireVideoEvent(VideoEvent event)
    {
        videoNotifierSupport.fireVideoEvent(event);
    }

    /**
     * Returns a reference to the visual <tt>Component</tt> where video from the
     * remote peer is being rendered or <tt>null</tt> if no video is currently
     * rendered.
     *
     * @return a reference to the visual <tt>Component</tt> where video from
     * the remote peer is being rendered or <tt>null</tt> if no video is
     * currently rendered
     */
    public Component getVisualComponent()
    {
        MediaDeviceSession deviceSession = getDeviceSession();

        return
            (deviceSession instanceof VideoMediaDeviceSession)
                ? ((VideoMediaDeviceSession) deviceSession).getVisualComponent()
                : null;
    }

    /**
     * Removes a specific <tt>VideoListener</tt> from this
     * <tt>VideoMediaStream</tt> in order to have to no longer receive
     * notifications when visual/video <tt>Component</tt>s are being added and
     * removed.
     *
     * @param listener the <tt>VideoListener</tt> to no longer be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoMediaStream</tt>
     */
    public void removeVideoListener(VideoListener listener)
    {
        videoNotifierSupport.removeVideoListener(listener);
    }

    /**
     * Notifies this <tt>MediaStream</tt> implementation that its
     * <tt>RTPConnector</tt> instance has changed from a specific old value to a
     * specific new value. Allows extenders to override and perform additional
     * processing after this <tt>MediaStream</tt> has changed its
     * <tt>RTPConnector</tt> instance.
     *
     * @param oldValue the <tt>RTPConnector</tt> of this <tt>MediaStream</tt>
     * implementation before it got changed to <tt>newValue</tt>
     * @param newValue the current <tt>RTPConnector</tt> of this
     * <tt>MediaStream</tt> which replaced <tt>oldValue</tt>
     * @see MediaStreamImpl#rtpConnectorChanged(RTPTransformConnector,
     * RTPTransformConnector)
     */
    @Override
    protected void rtpConnectorChanged(
            RTPTransformConnector oldValue,
            RTPTransformConnector newValue)
    {
        super.rtpConnectorChanged(oldValue, newValue);

        if (newValue != null)
        {
            MediaDeviceSession deviceSession = getDeviceSession();

            if (deviceSession instanceof VideoMediaDeviceSession)
            {
                ((VideoMediaDeviceSession) deviceSession)
                    .setConnector(newValue);
            }
        }
    }

    /**
     * Handles attributes contained in <tt>MediaFormat</tt>.
     *
     * @param format the <tt>MediaFormat</tt> to handle the attributes of
     * @param attrs the attributes <tt>Map</tt> to handle
     */
    @Override
    protected void handleAttributes(
            MediaFormat format,
            Map<String, String> attrs)
    {
        /* walk through attributes and see if we recognized something
         * we support
         */
        if(attrs != null)
        {
            for(Map.Entry<String, String> mapEntry : attrs.entrySet())
            {
                String key = mapEntry.getKey();
                String value = mapEntry.getValue();

                if(key.equals("rtcp-fb"))
                {
                    if (value.equals("nack pli"))
                        usePLI = true;
                }
                else if(key.equals("imageattr"))
                {
                    Dimension res[] = parseSendRecvResolution(value);

                    if(res != null)
                    {
                        outputSize = res[1];

                        qualityControl.setRemoteSendMaxPreset(new QualityPreset(res[0]));
                        qualityControl.setRemoteReceiveResolution(outputSize);
                        ((VideoMediaDeviceSession)getDeviceSession()).
                            setOutputSize(outputSize);
                    }
                }
                else if(key.equals("CIF"))
                {
                    Dimension dim = new Dimension(352, 288);

                    if(outputSize == null || (outputSize.width < dim.width &&
                            outputSize.height < dim.height))
                    {
                        outputSize = dim;
                        ((VideoMediaDeviceSession)getDeviceSession()).
                            setOutputSize(outputSize);
                    }
                }
                else if(key.equals("QCIF"))
                {
                    Dimension dim = new Dimension(176, 144);

                    if(outputSize == null || (outputSize.width < dim.width &&
                            outputSize.height < dim.height))
                    {
                        outputSize = dim;
                        ((VideoMediaDeviceSession)getDeviceSession()).
                            setOutputSize(outputSize);
                    }
                }
                else if(key.equals("VGA")) // X-lite send it
                {
                    Dimension dim = new Dimension(640, 480);

                    if(outputSize == null || (outputSize.width < dim.width &&
                            outputSize.height < dim.height))
                    {
                        /* X-lite does not display anything if we send 640x480
                         * video
                         */
                        outputSize = dim;
                        ((VideoMediaDeviceSession)getDeviceSession()).
                            setOutputSize(outputSize);
                    }
                }
                else if(key.equals("CUSTOM"))
                {
                    String args[] = value.split(",");

                    if(args.length < 3)
                        continue;

                    try
                    {
                        Dimension dim = new Dimension(Integer.parseInt(args[0]),
                                Integer.parseInt(args[1]));

                        if(outputSize == null || (outputSize.width < dim.width
                                && outputSize.height < dim.height))
                        {
                            outputSize = dim;
                            ((VideoMediaDeviceSession)getDeviceSession()).
                                setOutputSize(outputSize);
                        }
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
        }
    }

    /**
     * Extracts and returns maximum resolution can receive from the image
     * attribute.
     *
     * @param imgattr send/recv resolution string
     * @return maximum resolution array (first element is send, second one is
     * recv). Elements could be null if image attribute is not present or if
     * resolution is a wildcard.
     */
    public static java.awt.Dimension[] parseSendRecvResolution(String imgattr)
    {
        java.awt.Dimension res[] = new java.awt.Dimension[2];
        String token = null;
        Pattern pSendSingle = Pattern.compile("send \\[x=[0-9]+,y=[0-9]+\\]");
        Pattern pRecvSingle = Pattern.compile("recv \\[x=[0-9]+,y=[0-9]+\\]");
        Pattern pSendRange = Pattern.compile(
                "send \\[x=\\[[0-9]+-[0-9]+\\],y=\\[[0-9]+-[0-9]+\\]\\]");
        Pattern pRecvRange = Pattern.compile(
                "recv \\[x=\\[[0-9]+-[0-9]+\\],y=\\[[0-9]+-[0-9]+\\]\\]");
        Pattern pNumeric = Pattern.compile("[0-9]+");
        Matcher mSingle = null;
        Matcher mRange = null;
        Matcher m = null;

        /* resolution (width and height) can be on four forms
         *
         * - single value [x=1920,y=1200]
         * - range of values [x=[800-1024],y=[600-768]]
         * - fixed range of values [x=[800,1024],y=[600,768]]
         * - range of values with step [x=[800:32:1024],y=[600:32:768]]
         *
         * For the moment we only support the first two forms.
         */

        /* send part */
        mSingle = pSendSingle.matcher(imgattr);
        mRange = pSendRange.matcher(imgattr);

        if(mSingle.find())
        {
            int val[] = new int[2];
            int i = 0;
            token = imgattr.substring(mSingle.start(), mSingle.end());
            m = pNumeric.matcher(token);

            while(m.find() && i < 2)
            {
                val[i] = Integer.parseInt(token.substring(m.start(), m.end()));
            }

            res[0] = new java.awt.Dimension(val[0], val[1]);
        }
        else if(mRange.find()) /* try with range */
        {
            /* have two value for width and two for height (min-max) */
            int val[]  = new int[4];
            int i = 0;
            token = imgattr.substring(mRange.start(), mRange.end());
            m = pNumeric.matcher(token);

            while(m.find() && i < 4)
            {
                val[i] = Integer.parseInt(token.substring(m.start(), m.end()));
                i++;
            }

            res[0] = new java.awt.Dimension(val[1], val[3]);
        }

        /* recv part */
        mSingle = pRecvSingle.matcher(imgattr);
        mRange = pRecvRange.matcher(imgattr);

        if(mSingle.find())
        {
            int val[] = new int[2];
            int i = 0;
            token = imgattr.substring(mSingle.start(), mSingle.end());
            m = pNumeric.matcher(token);

            while(m.find() && i < 2)
            {
                val[i] = Integer.parseInt(token.substring(m.start(), m.end()));
            }

            res[1] = new java.awt.Dimension(val[0], val[1]);
        }
        else if(mRange.find()) /* try with range */
        {
            /* have two value for width and two for height (min-max) */
            int val[]  = new int[4];
            int i = 0;
            token = imgattr.substring(mRange.start(), mRange.end());
            m = pNumeric.matcher(token);

            while(m.find() && i < 4)
            {
                val[i] = Integer.parseInt(token.substring(m.start(), m.end()));
                i++;
            }

            res[1] = new java.awt.Dimension(val[1], val[3]);
        }

        token = null;
        mSingle = null;
        mRange = null;
        m = null;
        pRecvRange = null;
        pSendSingle = null;
        pRecvSingle = null;
        pSendRange = null;

        return res;
    }

    /**
     * Set local SSRC.
     *
     * @param ssrc source ID
     */
    @Override
    protected void setLocalSourceID(long ssrc)
    {
        super.setLocalSourceID(ssrc);

        ((VideoMediaDeviceSession) getDeviceSession()).setLocalSSRC(ssrc);
    }

    /**
     * Set remote SSRC.
     *
     * @param ssrc remote SSRC
     */
    @Override
    protected void setRemoteSourceID(long ssrc)
    {
        super.setRemoteSourceID(ssrc);

        ((VideoMediaDeviceSession) getDeviceSession()).setRemoteSSRC(ssrc);
    }

    /**
     * The priority of the video is 5, which is meant to be higher than
     * other threads and lower than the audio one.
     * @return video priority.
     */
    @Override
    protected int getPriority()
    {
        return 5;
    }

    /**
     * Implements {@link VideoMediaStream#getKeyFrameControl()}.
     *
     * {@inheritDoc}
     * @see VideoMediaStream#getKeyFrameControl()
     */
    public KeyFrameControl getKeyFrameControl()
    {
        return keyFrameControl;
    }

    /**
     * Gets the <tt>QualityControl</tt> of this <tt>VideoMediaStream</tt>.
     *
     * @return the <tt>QualityControl</tt> of this <tt>VideoMediaStream</tt>
     */
    public QualityControl getQualityControl()
    {
        return qualityControl;
    }

    /**
     * Implements the <tt>KeyFrameControl</tt> of this
     * <tt>VideoMediaStream</tt>.
     *
     * @author Lyubomir Marinov
     */
    private class KeyFrameControlImpl
        implements KeyFrameControl
    {
    }

    /**
     * Implements the <tt>QualityControl</tt> of this <tt>VideoMediaStream</tt>.
     *
     * @author Damian Minkov
     */
    private class QualityControlImpl
        implements QualityControl
    {
        /**
         * The current used preset.
         */
        private QualityPreset preset;

        /**
         * The minimum values for resolution, framerate ...
         */
        private QualityPreset minPreset;

        /**
         * The maximum values for resolution, framerate ...
         */
        private QualityPreset maxPreset;

        /**
         * This is the local settings from the config panel.
         */
        private QualityPreset localSettingsPreset;

        /**
         * Sets the preset.
         * @param preset the desired video settings
         * @throws OperationFailedException
         */
        private void setRemoteReceivePreset(QualityPreset preset)
            throws OperationFailedException
        {
            if(preset.compareTo(getPreferredSendPreset()) > 0)
                this.preset = getPreferredSendPreset();
            else
                this.preset = preset;
        }

        /**
         * The current preset.
         * @return
         */
        public QualityPreset getRemoteReceivePreset()
        {
            return preset;
        }

        /**
         * The minimum resolution values for remote part.
         * @return
         */
        public QualityPreset getRemoteSendMinPreset()
        {
            return minPreset;
        }

        /**
         * The max resolution values for remote part.
         * @return
         */
        public QualityPreset getRemoteSendMaxPreset()
        {
            return maxPreset;
        }

        /**
         * Does nothing specific locally.
         *
         * @param preset the max preset
         * @throws OperationFailedException not thrown.
         */
        public void setPreferredRemoteSendMaxPreset(QualityPreset preset)
            throws OperationFailedException
        {
            setRemoteSendMaxPreset(preset);
        }

        /**
         * Changes remote send preset, the one we will receive.
         * @param preset
         */
        public void setRemoteSendMaxPreset(QualityPreset preset)
        {
            this.maxPreset = preset;
        }

        /**
         * The local setting of capture.
         * @return
         */
        private QualityPreset getPreferredSendPreset()
        {
            if(localSettingsPreset == null)
            {
                DeviceConfiguration deviceConfiguration =
                    NeomediaActivator.getMediaServiceImpl()
                            .getDeviceConfiguration();

                localSettingsPreset = new QualityPreset(
                        deviceConfiguration.getVideoSize(),
                        deviceConfiguration.getFrameRate());
            }
            return localSettingsPreset;
        }

        /**
         * Sets maximum resolution.
         * @param res
         */
        public void setRemoteReceiveResolution(Dimension res)
        {
            try
            {
                this.setRemoteReceivePreset(new QualityPreset(res));
            }
            catch(OperationFailedException ofe){}
        }
    }
}
