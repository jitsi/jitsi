/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

/**
 * The <tt>NotificationsTableEntry</tt> is a class which defined the different 
 * entry in the utilitary "NotificationConfiguration" JTable. It 
 * regroups one entry's whole parameters.
 * @author Alexandre Maillard
 */
public class NotificationsTableEntry
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
    
    /**
     * Parameter which describes if the notification has been modified
     */
    private boolean isModify = false;
    
    /** 
     * Empty class constructor.
     * Creates a new instance of NotificationsTableEntry.
     */
    public NotificationsTableEntry()
    {
        enabled = false;
        program = false;
        programFile = "";
        popup = false;
        sound = false;
        soundFile = "";
        event = "";
        isModify = false;
    }
    
    /**
     * Class constructor with five parameters.
     * Creates a new instance of NotificationsTableEntry.
     * @param _enabled assigns the value of _enabled to this.enabled.
     * @param _program assigns the value of _program to this.program.
     * @param _programFile assigns the value of _programFile to this.programFile.
     * @param _popup assigns the value of _popup to this.popup.
     * @param _sound assigns the value of _sound to this.sound.
     * @param _soundFile assigns the value of _soundFile to this.soundFile.
     * @param _event assigns the value of _event to this.event.
     * @param _isModify assigns the value of _isModify to this.isModify.
     */
    public NotificationsTableEntry(
            boolean _enabled, boolean _program, 
            String _programFile, boolean _popup, boolean _sound,
            String _soundFile, String _event, boolean _isModify)
    {
        setEnabled(_enabled);
        setProgram(_program);
        setProgramFile(_programFile);
        setPopup(_popup);
        setSound(_sound);
        setSoundFile(_soundFile);
        setEvent(_event);
        setModify(_isModify);
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
     * Method which returns true if the notification has been modified
     * @return boolean true if the notification has been modified
     */
    public boolean isModified()
    {
        return this.isModify;
    }
    
    
    /**
     * Method which assigns the notification state.
     * @param _enabled true if the notification is enabled.
     */
    public void setEnabled(boolean _enabled)
    {
        this.enabled = _enabled;
    }

    /**
     * Method which set a boolean to true if a program is executed for the 
     * notification.
     * @param _program boolean for the program's presence.
     */
    public void setProgram(boolean _program)
    {
        this.program = _program;
    }
    
    /**
     * Method which assigns the program filename for the notification.
     * @param _programFile String representing the program file name.
     */
    public void setProgramFile(String _programFile)
    {
        this.programFile = _programFile;
    }

    /**
     * Method which set a boolean to true if a systray popup is executed for the 
     * notification.
     * @param _popup boolean for the presence of popup.
     */
    public void setPopup(boolean _popup)
    {
        this.popup = _popup;
    }
    
    /**
     * Method which set a boolean to true a sound is playing for the 
     * notification.
     * @param _sound boolean for the presence of a sound.
     */
    public void setSound(boolean _sound)
    {
        this.sound = _sound;
    }
    
    /**
     * Method which assigns the sound file name for the notification.
     * @param _soundFile String for the sound file name.
     */
    public void setSoundFile(String _soundFile)
    {
        this.soundFile = _soundFile;
    }
    
    /**
     * Method which assigns the notification's description.
     * @param _event String to assigns a description of a notification.
     */
    public void setEvent(String _event)
    {
        this.event = _event;
    }
    
    /**
     * Method which defines that the notification has been modified
     * @param _isModify boolean true if the notification is modified
     */
    public void setModify(boolean _isModify)
    {
        this.isModify = _isModify;
    }
}
