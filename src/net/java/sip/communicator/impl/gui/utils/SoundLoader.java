/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.applet.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.Timer;

import net.java.sip.communicator.util.*;

public class SoundLoader
{
    private static Logger log = Logger.getLogger(SoundLoader.class);

    public static SoundID INCOMING_MESSAGE  = new SoundID("INCOMING_MESSAGE");
    public static SoundID OUTGOING_CALL  = new SoundID("OUTGOING_CALL");
    public static SoundID INCOMING_CALL  = new SoundID("INCOMING_CALL");
    
    public static SoundID DIAL_ZERO  = new SoundID("DIAL_ZERO");
    public static SoundID DIAL_ONE  = new SoundID("DIAL_ONE");
    public static SoundID DIAL_TWO  = new SoundID("DIAL_TWO");
    public static SoundID DIAL_THREE  = new SoundID("DIAL_THREE");
    public static SoundID DIAL_FOUR  = new SoundID("DIAL_FOUR");
    public static SoundID DIAL_FIVE  = new SoundID("DIAL_FIVE");
    public static SoundID DIAL_SIX  = new SoundID("DIAL_SIX");
    public static SoundID DIAL_SEVEN  = new SoundID("DIAL_SEVEN");
    public static SoundID DIAL_EIGHT  = new SoundID("DIAL_EIGHT");
    public static SoundID DIAL_NINE  = new SoundID("DIAL_NINE");
    public static SoundID DIAL_DIEZ  = new SoundID("DIAL_DIEZ");
    public static SoundID DIAL_STAR  = new SoundID("DIAL_STAR");
    
    public static SoundID DIALING = new SoundID("DIALING");
    public static SoundID BUSY = new SoundID("BUSY");
    
    /**
     * Stores all already loaded sounds.
     */
    private static Hashtable loadedSounds = new Hashtable();
    
    private static Timer playAudioTimer = new Timer(1000, null);
    
    private static Hashtable audioListeners = new Hashtable();
    
    /**
     * Loads an audio for a given sound identifier.
     * @param soundID The identifier of the sound.
     * @return The sound for the given identifier.
     */
    public static AudioClip getSound(SoundID soundID) {
        AudioClip audio = null;

        if (loadedSounds.containsKey(soundID)) {
            audio = (AudioClip) loadedSounds.get(soundID);
        } else {
            String path = Sounds.getString(soundID.getId());
            
            audio = Applet
                .newAudioClip(SoundLoader.class.getClassLoader()
                    .getResource(path));

            loadedSounds.put(soundID, audio);
        }
        return audio;
    }
    
    /**
     * Represents the Sound Identifier.
     */
    public static class SoundID {
        private String id;

        private SoundID(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
    
    /**
     * Plays the given audio in a loop by making pauses of "interval" seconds.
     * @param audio the audio clip to play
     * @param interval interval in seconds between two audio clip plays
     */
    public static void playInLoop(AudioClip audio, int interval)
    {
        class NewThread extends Thread {
            AudioClip audio;
            int interval;
            
            public NewThread(AudioClip audio, int interval)
            {
                this.audio = audio;
                this.interval = interval;
            }
            
            public void run()
            {
                //first play the audio and then start the timer and wait
                audio.play();
                playAudioTimer.setDelay(interval);
                playAudioTimer.setRepeats(true);
                
                ActionListener audioListener = new PlayAudioListener(audio);
                audioListeners.put(audio, audioListener);
                
                playAudioTimer.addActionListener(audioListener);                                
                playAudioTimer.start();
            }
        }
        
        new NewThread(audio, interval).start();     
    }
    
    /**
     * Stop playing the given audio.
     * @param audio the audio to stop
     */
    public static void stop(AudioClip audio)
    {
        playAudioTimer.stop();
        playAudioTimer.removeActionListener(
            (ActionListener)audioListeners.get(audio));
        audio.stop();
    }
    
    /**
     * Plays an audio clip. Used in the playAudioTimer.
     */
    private static class PlayAudioListener implements ActionListener
    {
        private AudioClip audio;
        
        public PlayAudioListener(AudioClip audio)
        {
            this.audio = audio;
        }
        public void actionPerformed(ActionEvent e)
        {
            this.audio.play();
        }
        
    }
}
