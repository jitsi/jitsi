/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec;

import java.io.*;
import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Simple configuration of encoding priorities.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class EncodingConfiguration
{

    /**
     * The <tt>Logger</tt> used by this <tt>EncodingConfiguration</tt> instance
     * for logging output.
     */
    private final Logger logger = Logger.getLogger(EncodingConfiguration.class);

    /**
     * The SDP preference property.
     */
    private static final String PROP_SDP_PREFERENCE
        = "net.java.sip.communicator.impl.neomedia.codec.EncodingConfiguration";

    /**
     * The indicator which determines whether the G.729 codec is enabled.
     *
     * WARNING: The use of G.729 may require a license fee and/or royalty fee in
     * some countries and is licensed by
     * <a href="http://www.sipro.com">SIPRO Lab Telecom</a>.
     */
    public static final boolean G729 = false;

    /**
     * The additional custom JMF codecs.
     */
    private static final String[] CUSTOM_CODECS =
        {
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.alaw.DePacketizer"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.alaw.DePacketizer",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.alaw.Encoder"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.alaw.JavaEncoder",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.alaw.Packetizer"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.alaw.Packetizer",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.ulaw.Decoder"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.ulaw.JavaDecoder",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.ulaw.Encoder"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.ulaw.JavaEncoder",
            FMJConditionals.FMJ_CODECS
                ? "net.sf.fmj.media.codec.audio.ulaw.Packetizer"
                : "net.java.sip.communicator.impl.neomedia.codec.audio.ulaw.Packetizer",
            "net.java.sip.communicator.impl.neomedia.codec.audio.speex.JNIDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.speex.JNIEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.speex.SpeexResampler",
            "net.java.sip.communicator.impl.neomedia.codec.audio.speex.JavaDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.speex.JavaEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.mp3.JNIEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.ilbc.JavaDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.ilbc.JavaEncoder",
            G729
                ? "net.java.sip.communicator.impl.neomedia.codec.audio.g729.JavaDecoder"
                : null,
            G729
                ? "net.java.sip.communicator.impl.neomedia.codec.audio.g729.JavaEncoder"
                : null,
            "net.java.sip.communicator.impl.neomedia.codec.audio.g722.JNIDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.g722.JNIEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.gsm.Decoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.gsm.Encoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.gsm.DePacketizer",
            "net.java.sip.communicator.impl.neomedia.codec.audio.gsm.Packetizer",
            "net.java.sip.communicator.impl.neomedia.codec.audio.silk.JavaDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.audio.silk.JavaEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.video.h263p.DePacketizer",
            "net.java.sip.communicator.impl.neomedia.codec.video.h263p.JNIDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.video.h263p.JNIEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.video.h263p.Packetizer",
            "net.java.sip.communicator.impl.neomedia.codec.video.h264.DePacketizer",
            "net.java.sip.communicator.impl.neomedia.codec.video.h264.JNIDecoder",
            "net.java.sip.communicator.impl.neomedia.codec.video.h264.JNIEncoder",
            "net.java.sip.communicator.impl.neomedia.codec.video.h264.Packetizer",
            "net.java.sip.communicator.impl.neomedia.codec.video.SwScaler"
        };

    /**
     * The package prefixes of the additional JMF <tt>DataSource</tt>s (e.g. low
     * latency PortAudio and ALSA <tt>CaptureDevice</tt>s).
     */
    private static final String[] CUSTOM_PACKAGES
        = new String[]
                {
                    "net.java.sip.communicator.impl.neomedia.jmfext",
                    "net.sf.fmj"
                };

    /**
     * The <tt>Comparator</tt> which sorts the sets according to the settings in
     * encodingPreferences.
     */
    private final Comparator<MediaFormat> encodingComparator
        = new Comparator<MediaFormat>()
                {
                    public int compare(MediaFormat s1, MediaFormat s2)
                    {
                        return compareEncodingPreferences(s1, s2);
                    }
                };

    /**
     * That's where we keep format preferences matching SDP formats to integers.
     * We keep preferences for both audio and video formats here in case we'd
     * ever need to compare them to one another. In most cases however both
     * would be decorelated and other components (such as the UI) should present
     * them separately.
     */
    private final Map<String, Integer> encodingPreferences
        = new HashMap<String, Integer>();

    /**
     * The cache of supported <tt>AudioMediaFormat</tt>s ordered by decreasing
     * priority.
     */
    private Set<MediaFormat> supportedAudioEncodings;

    /**
     * The cache of supported <tt>VideoMediaFormat</tt>s ordered by decreasing
     * priority.
     */
    private Set<MediaFormat> supportedVideoEncodings;

    /**
     * Default constructor.
     */
    public EncodingConfiguration()
    {
    }

    /**
     * Retrieves (from the configuration service) preferences specified for
     * various formats and assigns default ones to those that haven't been
     * mentioned.
     */
    public void initializeFormatPreferences()
    {
        // first init default preferences
        // video
        setEncodingPreference(
            "H264",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            1100);

        setEncodingPreference(
            "H263-1998",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            0);
        /*
        setEncodingPreference(
            "H263",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            1000);
        */
        setEncodingPreference(
            "JPEG",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            950);
        setEncodingPreference(
            "H261",
            VideoMediaFormatImpl.DEFAULT_CLOCK_RATE,
            800);

        // audio
        setEncodingPreference("G722", 8000 /* actually, 16 kHz */, 705);
        setEncodingPreference("SILK", 24000, 704);
        setEncodingPreference("SILK", 16000, 703);
        setEncodingPreference("speex", 32000, 701);
        setEncodingPreference("speex", 16000, 700);
        setEncodingPreference("PCMU", 8000, 650);
        setEncodingPreference("PCMA", 8000, 600);
        setEncodingPreference("iLBC", 8000, 500);
        setEncodingPreference("GSM", 8000, 450);
        setEncodingPreference("speex", 8000, 352);
        setEncodingPreference("DVI4", 8000, 300);
        setEncodingPreference("DVI4", 16000, 250);
        setEncodingPreference("G723", 8000, 150);

        setEncodingPreference("SILK", 12000, 0);
        setEncodingPreference("SILK", 8000, 0);
        setEncodingPreference("G729", 8000, 0 /* proprietary */);

        // enables by default telephone event(DTMF rfc4733), with lowest
        // priority as it is not needed to order it with audio codecs
        setEncodingPreference(Constants.TELEPHONE_EVENT, 8000, 1);

        // now override with those that are specified by the user.
        ConfigurationService config
            = NeomediaActivator.getConfigurationService();

        for (String pName
                : config.getPropertyNamesByPrefix(PROP_SDP_PREFERENCE, false))
        {
            String prefStr = config.getString(pName);
            String fmtName = pName.substring(pName.lastIndexOf('.') + 1);

            // legacy
            if (fmtName.contains("sdp"))
            {
                fmtName = fmtName.replaceAll("sdp", "");
                /*
                 * If the current version of the property name is also
                 * associated with a value, ignore the value for the legacy one.
                 */
                if (config.getString(PROP_SDP_PREFERENCE + "." + fmtName)
                        != null)
                    continue;
            }

            int preference = -1;
            String encoding;
            double clockRate;
            try
            {
                preference = Integer.parseInt(prefStr);

                int encodingClockRateSeparator = fmtName.lastIndexOf('/');

                if (encodingClockRateSeparator > -1)
                {
                    encoding = fmtName.substring(0, encodingClockRateSeparator);
                    clockRate
                        = Double.parseDouble(
                                fmtName.substring(
                                        encodingClockRateSeparator + 1));
                }
                else
                {
                    encoding = fmtName;
                    clockRate = MediaFormatFactory.CLOCK_RATE_NOT_SPECIFIED;
                }
            }
            catch (NumberFormatException nfe)
            {
                logger
                    .warn(
                        "Failed to parse format ("
                            + fmtName
                            + ") or preference ("
                            + prefStr
                            + ").",
                        nfe);
                continue;
            }

            setEncodingPreference(encoding, clockRate, preference);
        }

        // now update the arrays so that they are returned by order of
        // preference.
        updateSupportedEncodings();
    }

    /**
     * Updates the codecs in the supported sets according to the preferences in
     * encodingPreferences. If the preference value is <tt>0</tt>, the codec is
     * disabled.
     */
    private void updateSupportedEncodings()
    {
        /*
         * If they need updating, their caches are invalid and need rebuilding
         * next time they are requested.
         */
        supportedAudioEncodings = null;
        supportedVideoEncodings = null;
    }

    /**
     * Gets the <tt>Set</tt> of enabled available <tt>MediaFormat</tt>s with the
     * specified <tt>MediaType</tt> sorted in decreasing priority.
     *
     * @param type the <tt>MediaType</tt> of the <tt>MediaFormat</tt>s to get
     * @return a <tt>Set</tt> of enabled available <tt>MediaFormat</tt>s with
     * the specified <tt>MediaType</tt> sorted in decreasing priority
     */
    private Set<MediaFormat> updateSupportedEncodings(MediaType type)
    {
        Set<MediaFormat> supported
            = new TreeSet<MediaFormat>(encodingComparator);

        for (MediaFormat format : getAvailableEncodings(type))
            if (getPriority(format) > 0)
                supported.add(format);
        return supported;
    }

    /**
     * Sets <tt>pref</tt> as the preference associated with <tt>encoding</tt>.
     * Use this method for both audio and video encodings and don't worry if
     * preferences are equal since we rarely need to compare prefs of video
     * encodings to those of audio encodings.
     *
     * @param encoding the SDP int of the encoding whose pref we're setting.
     * @param clockRate clock rate
     * @param pref a positive int indicating the preference for that encoding.
     */
    private void setEncodingPreference(
            String encoding, double clockRate,
            int pref)
    {
        MediaFormat mediaFormat = null;

        /*
         * The key in encodingPreferences associated with a MediaFormat is
         * currently composed of the encoding and the clockRate only so it makes
         * sense to ignore the format parameters.
         */
        for (MediaFormat mf : MediaUtils.getMediaFormats(encoding))
            if (mf.getClockRate() == clockRate)
            {
                mediaFormat = mf;
                break;
            }
        if (mediaFormat != null)
        {
            encodingPreferences.put(
                    getEncodingPreferenceKey(mediaFormat),
                    pref);
        }
    }

    /**
     * Sets <tt>priority</tt> as the preference associated with
     * <tt>encoding</tt>. Use this method for both audio and video encodings and
     * don't worry if the preferences are equal since we rarely need to compare
     * the preferences of video encodings to those of audio encodings.
     *
     * @param encoding the <tt>MediaFormat</tt> specifying the encoding to set
     * the priority of
     * @param priority a positive <tt>int</tt> indicating the priority of
     * <tt>encoding</tt> to set
     */
    public void setPriority(MediaFormat encoding, int priority)
    {
        String encodingEncoding = encoding.getEncoding();

        /*
         * Since we'll be remembering the priority in the ConfigurationService
         * by associating it with a property name/key based on encoding and
         * clock rate only, it does not make sense to store the MediaFormat in
         * encodingPreferences because MediaFormat is much more specific than
         * just encoding and clock rate.
         */
        setEncodingPreference(
                encodingEncoding, encoding.getClockRate(),
                priority);

        updateSupportedEncodings();
    }

    /**
     * Sets the priority of the given encoding in the configuration service.
     *
     * @param encoding the <tt>MediaFormat</tt> specifying the encoding to set
     * the priority of
     * @param priority a positive <tt>int</tt> indicating the priority of
     * <tt>encoding</tt> to set
     */
    public void setPriorityConfig(MediaFormat encoding, int priority)
    {
        String encodingEncoding = encoding.getEncoding();

        // save the settings
        NeomediaActivator.getConfigurationService()
            .setProperty(
                    PROP_SDP_PREFERENCE
                        + "."
                        + encodingEncoding
                        + "/"
                        + encoding.getClockRateString(),
                    priority);
    }

    /**
     * Set the priority for a <tt>MediaFormat</tt>.
     * @param encoding the <tt>MediaFormat</tt>
     * @return the priority
     */
    public int getPriority(MediaFormat encoding)
    {

        /*
         * Directly returning encodingPreference.get(encoding) will throw a
         * NullPointerException if encodingPreferences does not contain a
         * mapping for encoding.
         */
        Integer priority
            = encodingPreferences.get(getEncodingPreferenceKey(encoding));

        return (priority == null) ? 0 : priority;
    }

    /**
     * Register in JMF the custom codecs we provide
     */
    public void registerCustomCodecs()
    {
        // Register the custom codec which haven't already been registered.
        @SuppressWarnings("unchecked")
        Collection<String> registeredPlugins
            = new HashSet<String>(
                    PlugInManager.getPlugInList(
                            null,
                            null,
                            PlugInManager.CODEC));
        boolean commit = false;

        // Remove JavaRGBToYUV.
        PlugInManager.removePlugIn(
                "com.sun.media.codec.video.colorspace.JavaRGBToYUV",
                PlugInManager.CODEC);
        PlugInManager.removePlugIn(
                "com.sun.media.codec.video.colorspace.JavaRGBConverter",
                PlugInManager.CODEC);
        PlugInManager.removePlugIn(
                "com.sun.media.codec.video.colorspace.RGBScaler",
                PlugInManager.CODEC);

        // Remove JMF's H263 codec.
        PlugInManager.removePlugIn(
                "com.sun.media.codec.video.vh263.NativeDecoder",
                PlugInManager.CODEC);
        PlugInManager.removePlugIn(
                "com.ibm.media.codec.video.h263.NativeEncoder",
                PlugInManager.CODEC);

        // Remove JMF's GSM codec. As working only on some OS.
        String gsmCodecPackage = "com.ibm.media.codec.audio.gsm.";
        String[] gsmCodecClasses
            = new String[]
                    {
                        "JavaDecoder",
                        "JavaDecoder_ms",
                        "JavaEncoder",
                        "JavaEncoder_ms",
                        "NativeDecoder",
                        "NativeDecoder_ms",
                        "NativeEncoder",
                        "NativeEncoder_ms",
                        "Packetizer"
                    };
        for(String gsmCodecClass : gsmCodecClasses)
        {
            PlugInManager.removePlugIn(
                gsmCodecPackage + gsmCodecClass,
                PlugInManager.CODEC);
        }

        /*
         * Remove FMJ's JavaSoundCodec because it seems to slow down the
         * building of the filter graph and we do not currently seem to need it.
         */
        PlugInManager.removePlugIn(
                "net.sf.fmj.media.codec.JavaSoundCodec",
                PlugInManager.CODEC);

        for (String className : CUSTOM_CODECS)
        {

            /*
             * A codec with a className of null is configured at compile time to
             * not be registered.
             */
            if (className == null)
                continue;

            if (registeredPlugins.contains(className))
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "Codec " + className + " is already registered");
            }
            else
            {
                commit = true;

                boolean registered;
                Throwable exception = null;

                try
                {
                    Codec codec = (Codec)
                        Class.forName(className).newInstance();

                    registered =
                        PlugInManager.addPlugIn(
                            className,
                            codec.getSupportedInputFormats(),
                            codec.getSupportedOutputFormats(null),
                            PlugInManager.CODEC);
                }
                catch (Throwable ex)
                {
                    registered = false;
                    exception = ex;
                }
                if (registered)
                {
                    if (logger.isDebugEnabled())
                        logger.debug(
                            "Codec "
                                + className
                                + " is successfully registered");
                }
                else
                {
                    if (logger.isDebugEnabled())
                        logger.debug(
                            "Codec "
                                + className
                                + " is NOT succsefully registered", exception);
                }
            }
        }

        /*
         * If Jitsi provides a codec which is also provided by FMJ and/or JMF,
         * use Jitsi's version.
         */
        @SuppressWarnings("unchecked")
        Vector<String> codecs
            = PlugInManager.getPlugInList(null, null, PlugInManager.CODEC);

        if (codecs != null)
        {
            boolean setPlugInList = false;

            for (int i = CUSTOM_CODECS.length - 1; i >= 0; i--)
            {
                String className = CUSTOM_CODECS[i];

                if (className != null)
                {
                    int classNameIndex = codecs.indexOf(className);

                    if (classNameIndex != -1)
                    {
                        codecs.remove(classNameIndex);
                        codecs.add(0, className);
                        setPlugInList = true;
                    }
                }
            }

            if (setPlugInList)
                PlugInManager.setPlugInList(codecs, PlugInManager.CODEC);
        }

        if (commit)
            try
            {
                PlugInManager.commit();
            }
            catch (IOException ex)
            {
                logger.error("Cannot commit to PlugInManager", ex);
            }

        // Register the custom codec formats with the RTP manager once at
        // initialization. This is needed for the Sun JMF implementation. It
        // causes the registration of the formats with the static FormatInfo
        // instance of com.sun.media.rtp.RTPSessionMgr, which in turn makes the
        // formats available when the supported encodings arrays are generated
        // in initProcessor(). In other JMF implementations this might not be
        // needed, but should do no harm.

        // Commented as it fails to load alaw codec
        // RTPManager rtpManager = RTPManager.newInstance();
        // CallSessionImpl.registerCustomCodecFormats(rtpManager);
        // rtpManager.dispose();
    }

    /**
     * Register in JMF the custom packages we provide
     */
    public void registerCustomPackages()
    {
        @SuppressWarnings("unchecked")
        Vector<String> packages = PackageManager.getProtocolPrefixList();
        boolean loggerIsDebugEnabled = logger.isDebugEnabled();

        for (String customPackage : CUSTOM_PACKAGES)
        {
            /*
             * Linear search in a loop but it doesn't have to scale since the
             * list is always short.
             */
            if (!packages.contains(customPackage))
            {
                packages.add(customPackage);
                if (loggerIsDebugEnabled)
                    if (logger.isDebugEnabled())
                        logger.debug("Adding package  : " + customPackage);
            }
        }

        PackageManager.setProtocolPrefixList(packages);
        PackageManager.commitProtocolPrefixList();
        if (loggerIsDebugEnabled)
        {
            if (logger.isDebugEnabled())
                logger.debug(
                    "Registering new protocol prefix list: " + packages);
        }
    }

    /**
     * Get the available encodings for a specific <tt>MediaType</tt>.
     *
     * @param type the <tt>MediaType</tt> we would like to know its available
     * encodings
     * @return array of <tt>MediaFormat</tt> supported for the
     * <tt>MediaType</tt>
     */
    public MediaFormat[] getAvailableEncodings(MediaType type)
    {
        return MediaUtils.getMediaFormats(type);
    }

    /**
     * Gets the supported <tt>MediaFormat</tt>s i.e. the enabled available
     * <tt>MediaFormat</tt>s sorted in decreasing priority.
     *
     * @param type the <tt>MediaType</tt> of the supported <tt>MediaFormat</tt>s
     * to get
     * @return an array of the supported <tt>MediaFormat</tt>s i.e. the enabled
     * available <tt>MediaFormat</tt>s sorted in decreasing priority
     */
    public MediaFormat[] getSupportedEncodings(MediaType type)
    {
        Set<MediaFormat> supportedEncodings;

        switch (type)
        {
        case AUDIO:
            if (supportedAudioEncodings == null)
                supportedAudioEncodings = updateSupportedEncodings(type);
            supportedEncodings = supportedAudioEncodings;
            break;
        case VIDEO:
            if (supportedVideoEncodings == null)
                supportedVideoEncodings = updateSupportedEncodings(type);
            supportedEncodings = supportedVideoEncodings;
            break;
        default:
            return MediaUtils.EMPTY_MEDIA_FORMATS;
        }
        return
            supportedEncodings.toArray(
                    new MediaFormat[supportedEncodings.size()]);
    }

    /**
     * Compares the two formats for order. Returns a negative integer, zero, or
     * a positive integer as the first format has been assigned a preference
     * higher, equal to, or greater than the one of the second.
     *
     * @param enc1 the first format to compare for preference.
     * @param enc2 the second format to compare for preference
     * @return a negative integer, zero, or a positive integer as the first
     * format has been assigned a preference higher, equal to, or greater than
     * the one of the second
     */
    private int compareEncodingPreferences(MediaFormat enc1, MediaFormat enc2)
    {
        int res = getPriority(enc2) - getPriority(enc1);

        /*
         * If the encodings are with same priority, compare them by name. If we
         * return equals, TreeSet will not add equal encodings.
         */
        if (res == 0)
        {
            res = enc1.getEncoding().compareToIgnoreCase(enc2.getEncoding());
            /*
             * There are formats with one and the same encoding (name) but
             * different clock rates.
             */
            if (res == 0)
            {
                res = Double.compare(enc2.getClockRate(), enc1.getClockRate());
                /*
                 * And then again, there are formats (e.g. H.264) with one and
                 * the same encoding (name) and clock rate but different format
                 * parameters (e.g. packetization-mode).
                 */
                if (res == 0)
                {
                    // Try to preserve the order specified by MediaUtils.
                    int index1;
                    int index2;

                    if (((index1 = MediaUtils.getMediaFormatIndex(enc1)) != -1)
                            && ((index2 = MediaUtils.getMediaFormatIndex(enc2))
                                    != -1))
                    {
                        res = (index1 - index2);
                    }

                    if (res == 0)
                    {
                        /*
                         * The format with more parameters will be considered
                         * here to be the format with higher priority.
                         */
                        Map<String, String> fmtps1 = enc1.getFormatParameters();
                        Map<String, String> fmtps2 = enc2.getFormatParameters();
                        int fmtpCount1 = (fmtps1 == null) ? 0 : fmtps1.size();
                        int fmtpCount2 = (fmtps2 == null) ? 0 : fmtps2.size();

                        /*
                         * TODO Even if the number of format parameters is
                         * equal, the two formats may still be different.
                         * Consider ordering by the values of the format
                         * parameters as well.
                         */
                        res = (fmtpCount2 - fmtpCount1);
                    }
                }
            }
        }
        return res;
    }

    /**
     * Gets the key in {@link #encodingPreferences} which is associated with the
     * priority of a specific <tt>MediaFormat</tt>.
     *
     * @param encoding the <tt>MediaFormat</tt> to get the key in
     * {@link #encodingPreferences} of
     * @return the key in {@link #encodingPreferences} which is associated with
     * the priority of the specified <tt>encoding</tt>
     */
    private String getEncodingPreferenceKey(MediaFormat encoding)
    {
        return encoding.getEncoding() + "/" + encoding.getClockRateString();
    }
}
