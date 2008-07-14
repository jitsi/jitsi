package net.java.sip.communicator.plugin.guicustomization;

import java.applet.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.guicustomization.CustomizationWindow.*;
import net.java.sip.communicator.util.*;

public class SoundsPanel
    extends JScrollPane
{
    private Logger logger = Logger.getLogger(SoundsPanel.class);

    private JTable soundsTable = new JTable();

    private CustomTableModel soundsTableModel = new CustomTableModel();

    private static Constructor acConstructor = null;

    private JFrame parentWindow;

    public SoundsPanel(JFrame parentWindow)
    {
        this.parentWindow = parentWindow;

        this.getViewport().add(soundsTable);

        soundsTable.setModel(soundsTableModel);
        soundsTableModel.addColumn("Description");
        soundsTableModel.addColumn("File path");
        soundsTableModel.addColumn("Change sound");

        TableColumn buttonColumn1
            = soundsTable.getColumnModel().getColumn(1);

        buttonColumn1.setCellRenderer(new ButtonTableCellRenderer());
        buttonColumn1.setCellEditor(new ButtonTableEditor());
        buttonColumn1.setWidth(40);

        TableColumn buttonColumn2
            = soundsTable.getColumnModel().getColumn(2);

        buttonColumn2.setCellRenderer(new ButtonTableCellRenderer());
        buttonColumn2.setCellEditor(new ButtonTableEditor());
        buttonColumn2.setWidth(40);

        this.initSoundTable();
    }
    
    private void initSoundTable()
    {
        Iterator soundKeys
            = GuiCustomizationActivator.getResources()
                .getCurrentSounds();

        int rowHeight = 40;
        while (soundKeys.hasNext())
        {
            String key = (String) soundKeys.next();
            URL soundURL
                = GuiCustomizationActivator.getResources().getSoundURL(key);

            PlaySoundButton playSoundButton = new PlaySoundButton(soundURL);
            playSoundButton.setAction(new PlaySoundAction());

            JButton fileChooserButton = new JButton();
            fileChooserButton.setAction(new ChangeSoundAction(playSoundButton));

            soundsTableModel.addRow(new Object[]{  key,
                                                   playSoundButton,
                                                   fileChooserButton});
            playSoundButton.setText("Play!");
            fileChooserButton.setText("Choose sound");

            soundsTable.setRowHeight(   soundsTableModel.getRowCount() - 1,
                                        rowHeight );
        }
    }


    private class ChangeSoundAction extends AbstractAction
    {
        private PlaySoundButton playSoundButton;

        public ChangeSoundAction(PlaySoundButton button)
        {
            this.playSoundButton = button;
        }

        public void actionPerformed(ActionEvent evt)
        {
            JFileChooser fileChooser
                = new JFileChooser();

            int result
                = fileChooser.showOpenDialog(parentWindow);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                File newSoundFile = fileChooser.getSelectedFile();
                try
                {
                    playSoundButton
                        .setSoundURL(newSoundFile.toURL());
                }
                catch (MalformedURLException e)
                {
                    logger.error("Faile to create sound file.", e);
                }
            }
        }
    }

    private class PlaySoundAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent evt)
        {
            PlaySoundButton button = (PlaySoundButton) evt.getSource();
            AudioClip ac;
            try
            {
                ac = createAppletAudioClip(button.getSoundURL().openStream());

                ac.play();
            }
            catch (IOException e)
            {
                logger.error("Failed to open sound file.", e);
            }
        }
    }

    private class PlaySoundButton extends JButton
    {
        private URL soundURL;

        public PlaySoundButton(URL soundURL)
        {
            this.soundURL = soundURL;
        }

        public URL getSoundURL()
        {
            return soundURL;
        }

        public void setSoundURL(URL url)
        {
            this.soundURL = url;
        }
    }

    /**
     * Creates an AppletAudioClip.
     * 
     * @param inputstream the audio input stream
     * @throws IOException
     */
    private AudioClip createAppletAudioClip(InputStream inputstream)
        throws IOException
    {
        if(acConstructor == null)
        {  
            try
            {
                acConstructor = (Constructor) AccessController
                    .doPrivileged(new PrivilegedExceptionAction()
                {    
                    public Object run()
                        throws  NoSuchMethodException,
                                SecurityException,
                                ClassNotFoundException
                    {
                        
                        Class class1 = null;
                        try
                        {
                            class1 = Class.forName(
                                    "com.sun.media.sound.JavaSoundAudioClip",
                                    true, ClassLoader.getSystemClassLoader());
                        }
                        catch(ClassNotFoundException ex)
                        {
                            class1 = Class.forName(
                                "sun.audio.SunAudioClip", true, null);
                        }
                        Class aclass[] = new Class[1];
                        aclass[0] = Class.forName("java.io.InputStream");
                        return class1.getConstructor(aclass);
                    }
                });
            }
            catch(PrivilegedActionException privilegedactionexception)
            {
                throw new IOException("Failed to get AudioClip constructor: "
                    + privilegedactionexception.getException());
            }
        }
        try
        {
            Object aobj[] = {
                inputstream
            };

            return (AudioClip)acConstructor.newInstance(aobj);
        }
        catch(Exception exception)
        {
            throw new IOException("Failed to construct the AudioClip: "
                + exception);
        }
    }
    
    Hashtable<String, URL> getSounds()
    {
        Hashtable res = new Hashtable();
        int rows = soundsTableModel.getRowCount();
        for (int i = 0; i < rows; i++)
        {
            String key = (String)soundsTableModel.getValueAt(i, 0);
            PlaySoundButton sndButton = 
                (PlaySoundButton)soundsTableModel.getValueAt(i, 1);
            
            res.put(
                key,
                sndButton.getSoundURL());
        }
        
        return res;
    }
}
