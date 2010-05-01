package net.java.sip.communicator.impl.neomedia;

import javax.media.*;

import gnu.java.zrtp.utils.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.impl.neomedia.portaudio.streams.*;

/**
 * GatherEntropy initializes the Fortuna PRNG with entropy data.
 * 
 * GatherEntropy gets the media device configuration and checks which media
 * systems are available. It then reads some data fom media input (capture)
 * devices and uses this data to seed the Fortuna PRNG. The ZrtpFortuna PRNG
 * is a singleton and all other methods that require random data shall use
 * this singleton.
 * 
 * Use GatherEntropy during startup and initialization phase of SIP 
 * Communicator but after initialization of the media devices to get entropy
 * data at the earliest point. Also make sure that entropy data is read from
 * local sources only and that entropy data is never send out (via networks 
 * for example).  
 * 
 * TODO: add JMF method to read audio (mic) data, check if we can use video?
 * 
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 *
 */
public class GatherEntropy {

    /**
     * Device config to look for catpture devices.
     */
    private DeviceConfiguration deviceConfiguration;

    public GatherEntropy(DeviceConfiguration deviceConfiguration) 
    {
        this.deviceConfiguration = deviceConfiguration;
    }
    
    /**
     * Set entropy to ZrtpFortuna singleton.
     * 
     * The methods reads entropy data and seeds the ZrtpFortuna singleton. 
     * The methods seeds the first pool (0) of Fortuna to make sure that
     * this entropy is always used.
     * 
     * @return true if entropy data was available, false otherwise.
     */
    public boolean setEntropy()
    {
        boolean retValue = false;
        ZrtpFortuna fortuna = ZrtpFortuna.getInstance();
        
        byte entropy[] = readAudioEntropy();
        if (entropy != null) {
            fortuna.addSeedMaterial(0, entropy, 0, entropy.length);
            retValue = true;
        }
        return retValue;
    }
    
    /**
     * Read entropy data from audio capture device.
     * 
     * The method checks which audio systems are available and calls the
     * appropriate method the get some random data.
     * 
     * @return Audio data from capture (microphone) device or null if
     *         no data was available.
     * 
     */
    private byte[] readAudioEntropy()
    {
        try
        {
            if(deviceConfiguration.getAudioSystem().equals(
                DeviceConfiguration.AUDIO_SYSTEM_JAVASOUND))
            {
                // return readJMFAudioEntropy();
            }
            else if(deviceConfiguration.getAudioSystem().equals(
                DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO))
            {
                return readPortAudioEntropy();
            }
            else 
                return null;
        }
        catch (Throwable e)
        {
            // Cannot create audio to read entropy
            return null;
        }
        return null;
    }
    
    /**
     * Read entropy data from portaudio capture device.
     * 
     * The method reads audio samples from the microphone, combines them and 
     * returns the data. 
     * 
     * @return Audio data from capture (microphone) device or null if no data
     *         was available.
     * 
     * @throws PortAudioException
     */
    private byte[] readPortAudioEntropy() throws PortAudioException 
    {
        int deviceIndex = PortAudioUtils
                .getDeviceIndexFromLocator(deviceConfiguration
                        .getAudioCaptureDevice().getLocator());

        InputPortAudioStream portAudioStream = PortAudioManager.getInstance()
                .getInputStream(deviceIndex, 8000.0, 1);
        Buffer firstBuf = new Buffer();
        Buffer secondBuf = new Buffer();

        portAudioStream.start();
        portAudioStream.read(firstBuf);
        portAudioStream.read(secondBuf);
        portAudioStream.stop();

        // make sure we have enough data
        int length = firstBuf.getLength() + secondBuf.getLength();
        if (length < 64) {
            return null;
        }
        byte[] returnData = new byte[length];
        System.arraycopy(firstBuf.getData(), 0, returnData, 0, firstBuf.getLength());
        System.arraycopy(secondBuf.getData(), 0, returnData, firstBuf.getLength(), secondBuf.getLength());

        return returnData;
    }
}
