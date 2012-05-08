/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.event.*;

public abstract class DeviceSystem
    extends PropertyChangeNotifier
{
    private static final Logger logger = Logger.getLogger(DeviceSystem.class);

    public static final int FEATURE_REINITIALIZE = 1;

    public static final String PROP_DEVICES = "devices";

    private static List<DeviceSystem> deviceSystems
        = new LinkedList<DeviceSystem>();

    protected static List<CaptureDeviceInfo> filterDeviceListByLocatorProtocol(
            List<CaptureDeviceInfo> deviceList,
            String locatorProtocol)
    {
        if ((deviceList != null) && (deviceList.size() > 0))
        {
            Iterator<CaptureDeviceInfo> deviceListIter = deviceList.iterator();

            while (deviceListIter.hasNext())
            {
                MediaLocator locator = deviceListIter.next().getLocator();

                if ((locator == null)
                        || !locatorProtocol.equalsIgnoreCase(
                                locator.getProtocol()))
                {
                    deviceListIter.remove();
                }
            }
        }
        return deviceList;
    }

    public static DeviceSystem[] getDeviceSystems(MediaType mediaType)
    {
        List<DeviceSystem> ret;

        synchronized (deviceSystems)
        {
            ret = new ArrayList<DeviceSystem>(deviceSystems.size());
            for (DeviceSystem deviceSystem : deviceSystems)
                if (deviceSystem.getMediaType().equals(mediaType))
                    ret.add(deviceSystem);
        }
        return ret.toArray(new DeviceSystem[ret.size()]);
    }

    public static void initializeDeviceSystems()
    {
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        /*
         * Detect the audio capture devices unless the configuration explicitly
         * states that they are to not be detected.
         */
        if (!cfg.getBoolean(
                MediaServiceImpl.DISABLE_AUDIO_SUPPORT_PNAME,
                false))
        {
            if (logger.isInfoEnabled())
                logger.info("Initializing audio devices");

            initializeDeviceSystems(MediaType.AUDIO);
        }

        /*
         * Detect the video capture devices unless the configuration explicitly
         * states that they are to not be detected.
         */
        if (!cfg.getBoolean(
                MediaServiceImpl.DISABLE_VIDEO_SUPPORT_PNAME,
                false))
        {
            if (logger.isInfoEnabled())
                logger.info("Initializing video devices");

            initializeDeviceSystems(MediaType.VIDEO);
        }
    }

    public static void initializeDeviceSystems(MediaType mediaType)
    {
        String[] classNames;

        switch (mediaType)
        {
        case AUDIO:
            classNames
                = new String[]
                {
                    ".PulseAudioSystem",
                    ".PortAudioSystem",
                    ".NoneAudioSystem"
                };
            break;
        case VIDEO:
            classNames
                = new String[]
                {
                    OSUtils.IS_LINUX ? ".Video4Linux2System" : null,
                    OSUtils.IS_MAC ? ".QuickTimeSystem" : null,
                    OSUtils.IS_WINDOWS ? ".DirectShowSystem" : null,
                    ".ImgStreamingSystem"
                };
            break;
        default:
            throw new IllegalArgumentException("mediaType");
        }

        initializeDeviceSystems(classNames);
    }

    private static void initializeDeviceSystems(String[] classNames)
    {
        synchronized (deviceSystems)
        {
            String packageName = null;

            for (String className : classNames)
            {
                if (className == null)
                    continue;

                if (className.startsWith("."))
                {
                    if (packageName == null)
                        packageName = DeviceSystem.class.getPackage().getName();
                    className = packageName + className;
                }

                // Initialize a single instance per className.
                DeviceSystem deviceSystem = null;

                for (DeviceSystem aDeviceSystem : deviceSystems)
                    if (aDeviceSystem.getClass().getName().equals(className))
                    {
                        deviceSystem = aDeviceSystem;
                        break;
                    }

                boolean reinitialize;

                if (deviceSystem == null)
                {
                    reinitialize = false;

                    Object o = null;

                    try
                    {
                        o = Class.forName(className).newInstance();
                    }
                    catch (Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else if (logger.isDebugEnabled())
                            logger.debug(
                                    "Failed to initialize " + className,
                                    t);
                    }
                    if (o instanceof DeviceSystem)
                    {
                        deviceSystem = (DeviceSystem) o;
                        if (!deviceSystems.contains(deviceSystem))
                            deviceSystems.add(deviceSystem);
                    }
                }
                else
                    reinitialize = true;

                // Reinitializing is an optional feature.
                if (reinitialize
                        && ((deviceSystem.getFeatures() & FEATURE_REINITIALIZE)
                                != 0))
                {
                    try
                    {
                        deviceSystem.initialize();
                    }
                    catch (Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else if (logger.isDebugEnabled())
                            logger.debug(
                                    "Failed to reinitialize " + className,
                                    t);
                    }
                }
            }
        }
    }

    private final int features;

    private final String locatorProtocol;

    private final MediaType mediaType;

    protected DeviceSystem(MediaType mediaType, String locatorProtocol)
        throws Exception
    {
        this(mediaType, locatorProtocol, 0);
    }

    protected DeviceSystem(
            MediaType mediaType,
            String locatorProtocol,
            int features)
        throws Exception
    {
        if (mediaType == null)
            throw new NullPointerException("mediaType");
        if (locatorProtocol == null)
            throw new NullPointerException("locatorProtocol");

        this.mediaType = mediaType;
        this.locatorProtocol = locatorProtocol;
        this.features = features;

        initialize();
    }

    public Renderer createRenderer(boolean playback)
    {
        String className = getRendererClassName();

        if (className != null)
        {
            try
            {
                return (Renderer) Class.forName(className).newInstance();
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                else
                    logger.warn(
                            "Failed to initialize a new "
                                + className
                                + " instance",
                            t);
            }
        }
        return null;
    }

    protected abstract void doInitialize()
        throws Exception;

    public final int getFeatures()
    {
        return features;
    }

    public final String getLocatorProtocol()
    {
        return locatorProtocol;
    }

    public final MediaType getMediaType()
    {
        return mediaType;
    }

    protected String getRendererClassName()
    {
        return null;
    }

    protected final void initialize()
        throws Exception
    {
        preInitialize();
        try
        {
            doInitialize();
        }
        finally
        {
            postInitialize();
        }
    }

    protected void postInitialize()
    {
        firePropertyChange(PROP_DEVICES, null, null);
    }

    protected void preInitialize()
    {
        Format format;

        switch (getMediaType())
        {
        case AUDIO:
            format = new AudioFormat(null);
            break;
        case VIDEO:
            format = new VideoFormat(null);
            break;
        default:
            format = null;
            break;
        }

        if (format != null)
        {
            @SuppressWarnings("unchecked")
            Vector<CaptureDeviceInfo> cdis
                = CaptureDeviceManager.getDeviceList(format);

            if ((cdis != null) && (cdis.size() > 0))
            {
                boolean commit = false;

                for (CaptureDeviceInfo cdi
                        : filterDeviceListByLocatorProtocol(
                                cdis,
                                getLocatorProtocol()))
                {
                    CaptureDeviceManager.removeDevice(cdi);
                    commit = true;
                }
                if (commit && !NeomediaActivator.isJmfRegistryDisableLoad())
                {
                    try
                    {
                        CaptureDeviceManager.commit();
                    }
                    catch (IOException ioe)
                    {
                        /*
                         * We do not really need commit but we have it for
                         * historical reasons.
                         */
                        if (logger.isDebugEnabled())
                            logger.debug(
                                    "Failed to commit CaptureDeviceManager",
                                    ioe);
                    }
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return getLocatorProtocol();
    }
}
