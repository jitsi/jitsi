package net.java.sip.communicator.impl.neomedia;

import javax.media.*;

import gnu.java.zrtp.utils.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;

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
 */
public class GatherEntropy
{

    /**
     * Device config to look for catpture devices.
     */
    private DeviceConfiguration deviceConfiguration;
    
    /**
     * Other methods shall/may check this to see if Fortuna was seeded with
     * entropy.
     */
    private static boolean entropyOk = false;
    
    /**
     * Number of gathered entropy bytes.
     */
    private int gatheredEntropy = 0;

    /**
     * How many audio frames to read. The current value is based on 20ms
     * frames (50 frames per second). Read 2 seconds of audio frames.
     */
    final private static int NUM_OF_FRAMES = 2*50;

    public GatherEntropy(DeviceConfiguration deviceConfiguration) 
    {
        this.deviceConfiguration = deviceConfiguration;
    }
    
    /**
     * Get status of entropy flag.
     * 
     * @return Status if entropy was gathered and set in Fortuna PRNG. 
     */
    public static boolean isEntropyOk()
    {
        return entropyOk;
    }
    
    /**
     * @return the number of gathered entropy bytes.
     */
    protected int getGatheredEntropy() {
        return gatheredEntropy;
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
        if (deviceConfiguration.getAudioSystem().equals(
                DeviceConfiguration.AUDIO_SYSTEM_JAVASOUND))
        {
            // retValue = readJMFAudioEntropy();
        }
        else if (deviceConfiguration.getAudioSystem().equals(
                DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO))
        {
//            GatherPortAudio gatherer = new GatherPortAudio();
//            retValue = gatherer.preparePortAudioEntropy();
//            if (retValue)
//                gatherer.start();
        }
        return retValue;
    }

//    private class GatherPortAudio extends Thread
//    {
//        private InputPortAudioStream portAudioStream = null;
//        
//        /**
//         * Prepares to read entropy data from portaudio capture device.
//         * 
//         * The method gets an PortAudio instance with a set of capture 
//         * parameters.
//         * 
//         * @return True if the PortAudio input stream is available. 
//         */
//        private boolean preparePortAudioEntropy()
//        {
//            int deviceIndex
//                = DataSource.getDeviceIndex(
//                        deviceConfiguration
//                            .getAudioCaptureDevice().getLocator());
//
//            try {
//                portAudioStream = PortAudioManager.getInstance()
//                .getInputStream(deviceIndex, 8000.0, 1);
//            } catch (PortAudioException e) {
//                return false;
//            }
//            return true;
//        }
//
//        /**
//         * Gather entropy from portaudio capture device and seed Fortuna PRNG.
//         * 
//         * The method gathers a number of samples and seeds the Fortuna PRNG.
//         */
//        public void run() {
//            
//            ZrtpFortuna fortuna = ZrtpFortuna.getInstance();
//            
//            Buffer firstBuf = new Buffer();
//
//            if (portAudioStream == null) {
//                return;
//            }
//            
//            try {
//                portAudioStream.start();
//                
//                for (int i = 0; i < NUM_OF_FRAMES; i++) 
//                {
//                    portAudioStream.read(firstBuf);
//                    byte[] entropy = (byte[])firstBuf.getData();
//                    gatheredEntropy += entropy.length;
//                    // distribute first buffers evenly over the pools, put
//                    // others on the first pools. This method is adapted to
//                    // SC requirements to get random data
//                    if (i < 32)
//                    {
//                        fortuna.addSeedMaterial(entropy);
//                    }
//                    else 
//                    {
//                        fortuna.addSeedMaterial((i%3), entropy, 0, entropy.length);
//                    }
//                }
//                entropyOk = true;
//                portAudioStream.stop();
//            } catch (PortAudioException e) {
//                // ignore exception
//            }
//            // this forces a Fortuna to use the new seed (entropy) data.
//            byte[] random = new byte[300];
//            fortuna.nextBytes(random);
//            return;
//        }
//    }
}
