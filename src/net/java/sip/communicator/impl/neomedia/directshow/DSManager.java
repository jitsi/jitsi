/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.directshow;

/**
 * DirectShow capture device manager.
 *
 * DSManager act as a singleton. When you need it
 * call getInstance and don't forget to call dispose()
 * when you have finished.
 *
 * <code>
 * DSManager manager = DSManager.getInstance();
 *
 * DSCaptureDevice[] devices = manager.getCaptureDevices();
 *
 * // do stuff with capture devices
 * // ...
 *
 * DSManager.dispose();
 *
 * // do not use any of DSCaptureDevice objects obtained by
 * // manager
 * </code>
 *
 * @author Sebastien Vincent
 */
public class DSManager
{
    static
    {
        /* load DLL */
        System.loadLibrary("jdirectshow");
    }

    /**
     * Empty array of <tt>DSCaptureDevice</tt>s. Explicitly defined in order to
     * avoid unnecessary allocations.
     */
    private static DSCaptureDevice[] EMPTY_DEVICES = new DSCaptureDevice[0];

    /**
     * Unique instance of <tt>DSManager</tt>.
     */
    private static DSManager instance = null;

    /**
     * Reference count.
     */
    private static int ref = 0;

    /**
     * Synchronization object.
     */
    private static Object sync = new Object();

    /**
     * Array of all <tt>DSCaptureDevice</tt> found
     * on the OS.
     */
    private DSCaptureDevice[] devices = null;

    /**
     * Native pointer.
     */
    private long ptr = 0;

    /**
     * Get the instance.
     *
     * @return unique instance of <tt>DSManager</tt>
     */
    public static synchronized DSManager getInstance()
    {
        synchronized(sync)
        {
            if(instance == null)
            {
                long ptr = init();

                if(ptr != 0)
                {
                    instance = new DSManager(ptr);
                }
            }

            /* increment reference if object is valid */
            if(instance != null)
            {
                ref++;
            }
        }
        return instance;
    }

    /**
     * Dispose the object.
     */
    public static synchronized void dispose()
    {
        synchronized(sync)
        {
            ref--;

            if(ref == 0 && instance != null && instance.ptr != 0)
            {
                destroy(instance.ptr);
            }
            instance = null;
            ref = 0;
        }
    }

    /**
     * Constructor.
     *
     * @param ptr native pointer of DSManager
     */
    private DSManager(long ptr)
    {
        if(ptr == 0)
        {
            throw new IllegalArgumentException("invalid ptr value (0)");
        }

        this.ptr = ptr;
    }

    /**
     * Get the array of capture devices.
     *
     * @return array of <tt>DSCaptureDevice</tt>s
     */
    public DSCaptureDevice[] getCaptureDevices()
    {
        if(devices == null)
        {
            long nativeDevices[] = getCaptureDevices(ptr);

            if(nativeDevices != null && nativeDevices.length > 0)
            {
                devices = new DSCaptureDevice[nativeDevices.length];

                for(int i = 0 ; i < nativeDevices.length ; i++)
                {
                    devices[i] = new DSCaptureDevice(nativeDevices[i]);
                }
            }
            else
            {
                devices = EMPTY_DEVICES;
            }
        }
        return devices;
    }

    /**
     * Initialize and gather existing capture device.
     *
     * @return native pointer
     */
    private static native long init();

    /**
     * Delete native pointer.
     *
     * @param ptr native pointer to delete
     */
    private static native void destroy(long ptr);

    /**
     * Native method to get capture devices pointers.
     *
     * @param ptr native pointer of DSManager
     * @return array of native pointer to DSCaptureDevice
     */
    private native long[] getCaptureDevices(long ptr);
}

