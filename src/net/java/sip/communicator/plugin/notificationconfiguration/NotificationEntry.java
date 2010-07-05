/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import net.java.sip.communicator.service.notification.*;

/**
 * The <tt>NotificationsTableEntry</tt> is a class which defined the different 
 * entry in the utilitary "NotificationConfiguration" JTable. It 
 * regroups one entry's whole parameters.
 * @author Alexandre Maillard
 */
public class NotificationEntry
{
    /**
     * Parameter which defines if the notification is enabled or disabled.
     */
    private boolean enabled = false;

    /**
     * Parameter which defines if the program's execution is activated.
     */
    private boolean program = false;

    /**
     * Program filenames which is executed.
     */
    private String programFile = "";

    /**
     *  Parameter which defines if the popup is activated.
     */
    private boolean popup = false;

    /**
     * Parameter which defines if the sound is activated.
     */
    private boolean sound = false;

    /**
     * Name of sound file which is play
     */
    private String soundFile = "";

    /**
     * Parameter which describes, in a simple sentence, the notification.
     */
    private String event = "";

    private final NotificationService notificationService
        = NotificationConfigurationActivator.getNotificationService();

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
        sound = false;
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
     * @param sound assigns the value of _sound to this.sound.
     * @param soundFile assigns the value of _soundFile to this.soundFile.
     * @param event assigns the value of _event to this.event.
     */
    public NotificationEntry(
            boolean enabled, boolean program, 
            String programFile, boolean popup, boolean sound,
            String soundFile, String event)
    {
        this.enabled = enabled;
        this.program = program;
        setProgramFile(programFile);
        this.popup = popup;
        this.sound = sound;
        setSoundFile(soundFile);
        setEvent(event);
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
     * Method which returns if one sound is executed.
     * @return boolean true if a sound is playing.
     */
    public boolean getSound()
    {
        return this.sound;
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
     * @param sound boolean for the presence of a sound.
     */
    public void setSound(boolean sound)
    {
        this.sound = sound;
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
