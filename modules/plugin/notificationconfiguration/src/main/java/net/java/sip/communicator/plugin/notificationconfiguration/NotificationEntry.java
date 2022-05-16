/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

/**
 * The <tt>NotificationEntry</tt> is a class which defines the different
 * entries in the <tt>NotificationConfiguration<tt> JTable. It stores the
 * configuration parameters of an entry.
 *
 * @author Alexandre Maillard
 */
public class NotificationEntry
{
    /**
     * Parameter which defines if the notification is enabled or disabled.
     */
    private boolean enabled;

    /**
     * Parameter which defines if the program's execution is activated.
     */
    private boolean program;

    /**
     * Program filenames which is executed.
     */
    private String programFile;

    /**
     *  Parameter which defines if the popup is activated.
     */
    private boolean popup;

    /**
     * Parameter which defines if the sound is played on notification device.
     */
    private boolean soundNotification;

    /**
     * Parameter which defines if the sound is played on playback device.
     */
    private boolean soundPlayback;

    /**
     * Parameter which defines if the sound is played on pc speaker device.
     */
    private boolean soundPCSpeaker;

    /**
     * Name of sound file which is play
     */
    private String soundFile;

    /**
     * Parameter which describes, in a simple sentence, the notification.
     */
    private String event;

    /**
     * Empty class constructor.
     * Creates a new instance of NotificationsTableEntry.
     */
    public NotificationEntry()
    {
        enabled = false;
        program = false;
        programFile = "";
        popup = false;
        soundNotification = false;
        soundPlayback = false;
        soundPCSpeaker = false;
        soundFile = "";
        event = "";
    }

    /**
     * Class constructor with five parameters.
     * Creates a new instance of NotificationsTableEntry.
     * @param enabled assigns the value of _enabled to this.enabled.
     * @param program assigns the value of _program to this.program.
     * @param programFile assigns the value of _programFile to this.programFile.
     * @param popup assigns the value of _popup to this.popup.
     * @param soundPlayback assigns the value of _soundPlayback
     *                      to this.soundPlayback.
     * @param soundNotification assigns the value of _soundNotification
     *                          to this.soundNotification.
     * @param soundPCSpeaker assigns the value of _soundPCSpeaker
     *                       to this.soundPCSpeaker.
     * @param soundFile assigns the value of _soundFile to this.soundFile.
     * @param event assigns the value of _event to this.event.
     */
    public NotificationEntry(
            boolean enabled, boolean program,
            String programFile, boolean popup,
            boolean soundNotification, boolean soundPlayback,
            boolean soundPCSpeaker,
            String soundFile, String event)
    {
        this.enabled = enabled;
        this.program = program;
        this.programFile = programFile;
        this.popup = popup;
        this.soundNotification = soundNotification;
        this.soundPlayback = soundPlayback;
        this.soundPCSpeaker = soundPCSpeaker;
        this.soundFile = soundFile;
        this.event = event;
    }

    /**
     * Method which returns the state of the notification.
     * @return boolean enable/disable.
     */
    public boolean getEnabled()
    {
        return this.enabled;
    }

    /**
     * Method which returns true if one program is executed.
     * @return boolean true if a programm is executed
     */
    public boolean getProgram()
    {
        return this.program;
    }

    /**
     * Method which returns the program's name which is executed.
     * @return String representing the program file name.
     */
    public String getProgramFile()
    {
        return this.programFile;
    }

    /**
     * Method which returns true if one systray popup is executed.
     * @return boolean true if a popup is executed.
     */
    public boolean getPopup()
    {
        return this.popup;
    }

    /**
     * Method which returns if one sound is to be played on notification device.
     * @return boolean true if a sound is playing on notification device.
     */
    public boolean getSoundNotification()
    {
        return this.soundNotification;
    }

    /**
     * Method which returns if one sound is to be played on playback device.
     * @return boolean true if a sound is playing on playback device.
     */
    public boolean getSoundPlayback()
    {
        return this.soundPlayback;
    }

    /**
     * Method which returns if one sound is to be played on pc speaker device.
     * @return boolean true if a sound is playing on pc speaker device.
     */
    public boolean getSoundPCSpeaker()
    {
        return this.soundPCSpeaker;
    }

    /**
     * Method which returns the sound file name which is executed.
     * @return String representing the sound file name.
     */
    public String getSoundFile()
    {
        return this.soundFile;
    }

    /**
     * Method which returns the description of the notification.
     * @return String representing the notification's description.
     */
    public String getEvent()
    {
        return this.event;
    }

    /**
     * Method which assigns the notification state.
     * @param enabled true if the notification is enabled.
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Method which set a boolean to true if a program is executed for the
     * notification.
     * @param program boolean for the program's presence.
     */
    public void setProgram(boolean program)
    {
        this.program = program;
    }

    /**
     * Method which assigns the program filename for the notification.
     * @param programFile String representing the program file name.
     */
    public void setProgramFile(String programFile)
    {
        this.programFile = programFile;
    }

    /**
     * Method which set a boolean to true if a systray popup is executed for the
     * notification.
     * @param popup boolean for the presence of popup.
     */
    public void setPopup(boolean popup)
    {
        this.popup = popup;
    }

    /**
     * Method which set a boolean to true a sound is playing for the
     * notification.
     * @param sound boolean for the presence of a sound for notification device.
     */
    public void setSoundNotification(boolean sound)
    {
        this.soundNotification = sound;
    }

    /**
     * Method which set a boolean to true a sound is playing for the
     * playback.
     * @param sound boolean for the presence of a sound for playback device.
     */
    public void setSoundPlayback(boolean sound)
    {
        this.soundPlayback = sound;
    }

    /**
     * Method which set a boolean to true a sound is playing for the
     * pc speaker.
     * @param sound boolean for the presence of a sound for pc speaker device.
     */
    public void setSoundPCSpeaker(boolean sound)
    {
        this.soundPCSpeaker = sound;
    }

    /**
     * Method which assigns the sound file name for the notification.
     * @param soundFile String for the sound file name.
     */
    public void setSoundFile(String soundFile)
    {
        this.soundFile = soundFile;
    }

    /**
     * Method which assigns the notification's description.
     * @param event String to assigns a description of a notification.
     */
    public void setEvent(String event)
    {
        this.event = event;
    }
}
