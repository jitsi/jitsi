/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.audionotifier;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.audionotifier.*;

/**
 * The implementation of the AudioNotifierService.
 * 
 * @author Yana Stamcheva
 */
public class AudioNotifierImpl
    implements AudioNotifierService
{
    
    private static Map audioClips = new HashMap();
    
    private boolean isMute;
   
    /**
     * Creates an SCAudioClip from the given uri and adds it to the list of 
     * available audios.
     * 
     * @param uri the path where the audio file could be found
     */
    public SCAudioClip createAudio(String uri)
    {
        SCAudioClip audioClip;
        
        synchronized (audioClips)
        {
            if(audioClips.containsKey(uri))
            {
                audioClip = (SCAudioClip) audioClips.get(uri);
            }
            else
            {
                URL url = AudioNotifierImpl.class.getClassLoader()
                    .getResource(uri);
                
                audioClip = new SCAudioClipImpl(url);
                
                audioClips.put(uri, audioClip);            
            }            
        }
        
        return audioClip;
    }

    /**
     * Removes the given audio from the list of available audio clips. 
     * 
     * @param audioClip the audio to destroy
     */
    public void destroyAudio(SCAudioClip audioClip)
    {
        synchronized (audioClips) {
            audioClips.remove(audioClip);
        }
    }

    /**
     * Enables or disables the sound in the application. If FALSE, we try to
     * restore all looping sounds if any. 
     * 
     * @param isMute when TRUE disables the sound, otherwise enables the sound. 
     */
    public void setMute(boolean isMute)
    {
        this.isMute = isMute;
        
        Iterator audios = audioClips.entrySet().iterator();
            
        while (audios.hasNext())
        {
            SCAudioClipImpl audioClip
                = (SCAudioClipImpl) ((Map.Entry) audios.next()).getValue();
            
            if (isMute)
            {
                audioClip.internalStop();                
            }
            else
            {
                if(audioClip.isLooping())
                {
                    audioClip.playInLoop(audioClip.getLoopInterval());
                }
            }
        }
    }
    
    /**
     * Returns TRUE if the sound is currently disabled, FALSE otherwise.
     * @return TRUE if the sound is currently disabled, FALSE otherwise
     */
    public boolean isMute()
    {
        return isMute;
    }    
}