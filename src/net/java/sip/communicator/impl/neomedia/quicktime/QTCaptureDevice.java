/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

import java.util.*;

/**
 * Represents a QTKit capture device which is connected or has been previously
 * connected to the user's computer during the lifetime of the application.
 *
 * @author Lubomir Marinov
 */
public class QTCaptureDevice
    extends NSObject
{

    /**
     * The cached <tt>QTCaptureDevice</tt> instances previously returned by the
     * last call to {@link #inputDevicesWithMediaType(QTMediaType).
     */
    private static final Map<QTMediaType, List<QTCaptureDevice>> inputDevices
        = new HashMap<QTMediaType, List<QTCaptureDevice>>();

    /**
     * The constant which represents an empty array with
     * <tt>QTFormatDescription</tt> element type. Explicitly defined in order to
     * avoid unnecessary allocations.
     */
    private static final QTFormatDescription[] NO_FORMAT_DESCRIPTIONS
        = new QTFormatDescription[0];

    /**
     * The constant which represents an empty array with
     * <tt>QTCaptureDevice</tt> element type. Explicitly defined in order to
     * avoid unnecessary allocations.
     */
    private static final QTCaptureDevice[] NO_INPUT_DEVICES
        = new QTCaptureDevice[0];

    /**
     * Initializes a new <tt>QTCaptureDevice</tt> instance which is to represent
     * a specific QTKit <tt>QTCaptureDevice</tt> object.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureDevice</tt> object which
     * is to be represented by the new instance
     */
    public QTCaptureDevice(long ptr)
    {
        super(ptr);
    }

    /**
     * Releases application control over this device acquired in the
     * {@link #open()} method.
     */
    public void close()
    {
        close(getPtr());
    }

    /**
     * Releases application control over a specific QTKit
     * <tt>QTCaptureDevice</tt> object acquired in the {@link #open(long)}
     * method.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureDevice</tt> object to
     * close
     */
    private static native void close(long ptr);

    /**
     * Gets the <tt>QTCaptureDevice</tt> with a specific unique identifier.
     *
     * @param deviceUID the unique identifier of the <tt>QTCaptureDevice</tt> to
     * be retrieved
     * @return the <tt>QTCaptureDevice</tt> with the specified unique identifier
     * if such a <tt>QTCaptureDevice</tt> exists; otherwise, <tt>null</tt>
     */
    public static QTCaptureDevice deviceWithUniqueID(String deviceUID)
    {
        QTCaptureDevice[] inputDevices
            = inputDevicesWithMediaType(QTMediaType.Video);
        QTCaptureDevice deviceWithUniqueID
            = deviceWithUniqueID(deviceUID, inputDevices);

        if (deviceWithUniqueID == null)
        {
            inputDevices = inputDevicesWithMediaType(QTMediaType.Sound);
            deviceWithUniqueID = deviceWithUniqueID(deviceUID, inputDevices);
        }
        return deviceWithUniqueID;
    }

    private static QTCaptureDevice deviceWithUniqueID(
            String deviceUID,
            QTCaptureDevice[] inputDevices)
    {
        if (inputDevices != null)
            for (QTCaptureDevice inputDevice : inputDevices)
                if (deviceUID.equals(inputDevice.uniqueID()))
                    return inputDevice;
        return null;
    }

    /**
     * Called by the garbage collector to release system resources and perform
     * other cleanup.
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize()
    {
        release();
    }

    public QTFormatDescription[] formatDescriptions()
    {
        long[] formatDescriptionPtrs = formatDescriptions(getPtr());
        QTFormatDescription[] formatDescriptions;

        if (formatDescriptionPtrs == null)
            formatDescriptions = NO_FORMAT_DESCRIPTIONS;
        else
        {
            int formatDescriptionCount = formatDescriptionPtrs.length;

            if (formatDescriptionCount == 0)
                formatDescriptions = NO_FORMAT_DESCRIPTIONS;
            else
            {
                formatDescriptions
                    = new QTFormatDescription[formatDescriptionCount];
                for (int i = 0; i < formatDescriptionCount; i++)
                    formatDescriptions[i]
                        = new QTFormatDescription(formatDescriptionPtrs[i]);
            }
        }
        return formatDescriptions;
    }

    private static native long[] formatDescriptions(long ptr);

    public static QTCaptureDevice[] inputDevicesWithMediaType(
            QTMediaType mediaType)
    {
        long[] inputDevicePtrs = inputDevicesWithMediaType(mediaType.name());
        int inputDeviceCount
            = (inputDevicePtrs == null) ? 0 : inputDevicePtrs.length;
        QTCaptureDevice[] inputDevicesWithMediaType;

        if (inputDeviceCount == 0)
        {
            inputDevicesWithMediaType = NO_INPUT_DEVICES;
            inputDevices.remove(mediaType);
        }
        else
        {
            inputDevicesWithMediaType = new QTCaptureDevice[inputDeviceCount];

            List<QTCaptureDevice> cachedInputDevicesWithMediaType
                = inputDevices.get(mediaType);

            if (cachedInputDevicesWithMediaType == null)
            {
                cachedInputDevicesWithMediaType
                    = new LinkedList<QTCaptureDevice>();
                inputDevices.put(mediaType, cachedInputDevicesWithMediaType);
            }
            for (int i = 0; i < inputDeviceCount; i++)
            {
                long inputDevicePtr = inputDevicePtrs[i];
                QTCaptureDevice inputDevice = null;

                for (QTCaptureDevice cachedInputDevice
                        : cachedInputDevicesWithMediaType)
                    if (inputDevicePtr == cachedInputDevice.getPtr())
                    {
                        inputDevice = cachedInputDevice;
                        break;
                    }
                if (inputDevice == null)
                {
                    inputDevice = new QTCaptureDevice(inputDevicePtr);
                    cachedInputDevicesWithMediaType.add(inputDevice);
                }
                else
                    NSObject.release(inputDevicePtr);
                inputDevicesWithMediaType[i] = inputDevice;
            }

            Iterator<QTCaptureDevice> cachedInputDeviceIter
                = cachedInputDevicesWithMediaType.iterator();

            while (cachedInputDeviceIter.hasNext())
            {
                long cachedInputDevicePtr
                    = cachedInputDeviceIter.next().getPtr();
                boolean remove = true;

                for (long inputDevicePtr : inputDevicePtrs)
                    if (cachedInputDevicePtr == inputDevicePtr)
                    {
                        remove = false;
                        break;
                    }
                if (remove)
                    cachedInputDeviceIter.remove();
            }
        }
        return inputDevicesWithMediaType;
    }

    private static native long[] inputDevicesWithMediaType(String mediaType);

    /**
     * Gets the indicator which determines whether this <tt>QTCaptureDevice</tt>
     * is connected and available to applications.
     *
     * @return <tt>true</tt> if this <tt>QTCaptureDevice</tt> is connected and
     * available to applications; otherwise, <tt>false</tt>
     */
    public boolean isConnected()
    {
        return isConnected(getPtr());
    }

    /**
     * Gets the indicator which determines whether a specific QTKit
     * <tt>QTCaptureDevice</tt> object is connected and available to
     * applications.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureDevice</tt> object which
     * is to get the indicator for
     * @return <tt>true</tt> if the specified QTKit <tt>QTCaptureDevice</tt>
     * object is connected and available to applications; otherwise,
     * <tt>false</tt>
     */
    private static native boolean isConnected(long ptr);

    /**
     * Gets the localized human-readable name of this <tt>QTCaptureDevice</tt>.
     *
     * @return the localized human-readable name of this
     * <tt>QTCaptureDevice</tt>
     */
    public String localizedDisplayName()
    {
        return localizedDisplayName(getPtr());
    }

    /**
     * Gets the localized human-readable name of a specific QTKit
     * <tt>QTCaptureDevice</tt> object.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureDevice</tt> object to
     * get the localized human-readable name of
     * @return the localized human-readable name of the specified QTKit
     * <tt>QTCaptureDevice</tt> object
     */
    private static native String localizedDisplayName(long ptr);

    /**
     * Attempts to give the application control over this
     * <tt>QTCaptureDevice</tt> so that it can be used for capture.
     *
     * @return <tt>true</tt> if this device was opened successfully; otherwise,
     * <tt>false</tt>
     * @throws NSErrorException if this device was not opened successfully and
     * carries an <tt>NSError</tt> describing why this device could not be
     * opened
     */
    public boolean open()
        throws NSErrorException
    {
        return open(getPtr());
    }

    /**
     * Attempts to give the application control over a specific QTKit
     * <tt>QTCaptureDevice</tt> object so that it can be used for capture.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureDevice</tt> to be opened
     * @return <tt>true</tt> if the device was opened successfully; otherwise,
     * <tt>false</tt>
     * @throws NSErrorException if the device was not opened successfully and
     * carries an <tt>NSError</tt> describing why the device could not be opened
     */
    private static native boolean open(long ptr) throws NSErrorException;

    /**
     * Gets the unique identifier of this <tt>QTCaptureDevice</tt>.
     *
     * @return the unique identifier of this <tt>QTCaptureDevice</tt>
     */
    public String uniqueID()
    {
        return uniqueID(getPtr());
    }

    /**
     * Gets the unique identifier of a specific QTKit <tt>QTCaptureDevice</tt>
     * object.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureDevice</tt> object to
     * get the unique identifier of
     * @return the unique identifier of the specified QTKit
     * <tt>QTCaptureDevice</tt> object
     */
    private static native String uniqueID(long ptr);
}
