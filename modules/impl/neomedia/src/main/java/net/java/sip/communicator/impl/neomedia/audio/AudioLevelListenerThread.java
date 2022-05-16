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
package net.java.sip.communicator.impl.neomedia.audio;

import java.awt.event.*;
import java.io.*;
import java.util.concurrent.*;
import javax.media.*;
import javax.media.protocol.*;
import javax.swing.*;
import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.utils.*;

/**
 * Creates a new listener to combo box and affect changes to the audio level
 * indicator. The level indicator is updated via a thread in order to avoid
 * deadlock of the user interface.
 */
public class AudioLevelListenerThread
    implements ActionListener,
    HierarchyListener
{
    /**
     * The audio system used to get and set the sound devices.
     */
    private AudioSystem audioSystem;

    /**
     * The combo box used to select the device the user wants to use.
     */
    private JComboBox<CaptureDeviceViewModel> comboBox;

    /**
     * The current capture device.
     */
    private AudioMediaDeviceSession deviceSession;

    /**
     * The new device chosen by the user and that we need to initialize as the
     * new capture device.
     */
    private AudioMediaDeviceSession deviceSessionToSet;

    /**
     * The indicator which determines whether {@link #setDeviceSession(AudioMediaDeviceSession)}
     * is to be invoked when {@link #deviceSessionToSet} is <tt>null</tt>.
     */
    private boolean deviceSessionToSetIsNull;

    /**
     * The <tt>ExecutorService</tt> which is to asynchronously invoke {@link
     * #setDeviceSession(AudioMediaDeviceSession)} with {@link
     * #deviceSessionToSet}.
     */
    private final ExecutorService setDeviceSessionExecutor
        = Executors.newSingleThreadExecutor();

    private final Runnable setDeviceSessionTask = () ->
    {
        AudioMediaDeviceSession deviceSession = null;
        boolean deviceSessionIsNull = false;

        synchronized (AudioLevelListenerThread.this)
        {
            if (deviceSessionToSet != null || deviceSessionToSetIsNull)
            {
                /*
                 * Invoke #setDeviceSession(AudioMediaDeviceSession)
                 * outside the synchronized block to avoid a GUI
                 * deadlock.
                 */
                deviceSession = deviceSessionToSet;
                deviceSessionIsNull = deviceSessionToSetIsNull;
                deviceSessionToSet = null;
                deviceSessionToSetIsNull = false;
            }
        }

        if (deviceSession != null || deviceSessionIsNull)
        {
            /*
             * XXX The method blocks on Mac OS X for Bluetooth
             * devices which are paired but disconnected.
             */
            setDeviceSession(deviceSession);
        }
    };

    /**
     * The sound level indicator used to show the effectiveness of the capture
     * device.
     */
    private SoundLevelIndicator soundLevelIndicator;

    /**
     * Refresh combo box when the user click on it.
     *
     * @param ev The click on the combo box.
     */
    @Override
    public void actionPerformed(ActionEvent ev)
    {
        synchronized (this)
        {
            deviceSessionToSet = null;
            deviceSessionToSetIsNull = true;
            setDeviceSessionExecutor.execute(setDeviceSessionTask);
        }

        CaptureDeviceInfo cdi;

        if (comboBox == null)
        {
            cdi = soundLevelIndicator.isShowing()
                ? audioSystem.getSelectedDevice(AudioSystem.DataFlow.CAPTURE)
                : null;
        }
        else
        {
            Object selectedItem = soundLevelIndicator.isShowing()
                ? comboBox.getSelectedItem()
                : null;

            cdi = selectedItem instanceof CaptureDeviceViewModel
                ? ((CaptureDeviceViewModel) selectedItem).info
                : null;
        }

        if (cdi != null)
        {
            for (MediaDevice md : NeomediaActivator.getMediaServiceImpl()
                .getDevices(MediaType.AUDIO, MediaUseCase.ANY))
            {
                if (md instanceof AudioMediaDeviceImpl)
                {
                    AudioMediaDeviceImpl amd = (AudioMediaDeviceImpl) md;

                    if (cdi.equals(amd.getCaptureDeviceInfo()))
                    {
                        try
                        {
                            MediaDeviceSession deviceSession
                                = amd.createSession();
                            boolean deviceSessionIsSet = false;

                            try
                            {
                                if (deviceSession instanceof
                                    AudioMediaDeviceSession)
                                {
                                    synchronized (this)
                                    {
                                        deviceSessionToSet
                                            = (AudioMediaDeviceSession)
                                            deviceSession;
                                        deviceSessionToSetIsNull = false;
                                        setDeviceSessionExecutor.execute(
                                            setDeviceSessionTask);
                                    }
                                    deviceSessionIsSet = true;
                                }
                            }
                            finally
                            {
                                if (!deviceSessionIsSet)
                                {
                                    deviceSession.close();
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            // ignore
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void hierarchyChanged(HierarchyEvent ev)
    {
        if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
        {
            SwingUtilities.invokeLater(() -> actionPerformed(null));
        }
    }

    /**
     * Creates a new listener to combo box and affect changes to the audio level
     * indicator.
     *
     * @param audioSystem         The audio system used to get and set the sound
     *                            devices.
     * @param comboBox            The combo box used to select the device the
     *                            user wants to use.
     * @param soundLevelIndicator The sound level indicator used to show the
     *                            effectiveness of the capture device.
     */
    public void init(
        AudioSystem audioSystem,
        JComboBox<CaptureDeviceViewModel> comboBox,
        SoundLevelIndicator soundLevelIndicator)
    {
        this.audioSystem = audioSystem;

        if (this.comboBox != comboBox)
        {
            if (this.comboBox != null)
            {
                this.comboBox.removeActionListener(this);
            }
            this.comboBox = comboBox;
            if (comboBox != null)
            {
                comboBox.addActionListener(this);
            }
        }

        if (this.soundLevelIndicator != soundLevelIndicator)
        {
            if (this.soundLevelIndicator != null)
            {
                this.soundLevelIndicator.removeHierarchyListener(this);
            }
            this.soundLevelIndicator = soundLevelIndicator;
            if (soundLevelIndicator != null)
            {
                soundLevelIndicator.addHierarchyListener(this);
            }
        }
    }

    /**
     * Sets the new capture device used by the audio level indicator.
     *
     * @param deviceSession The new capture device used by the audio level
     *                      indicator.
     */
    private void setDeviceSession(AudioMediaDeviceSession deviceSession)
    {
        if (this.deviceSession == deviceSession)
        {
            return;
        }

        if (this.deviceSession != null)
        {
            try
            {
                this.deviceSession.close();
            }
            finally
            {
                this.deviceSession.setLocalUserAudioLevelListener(null);
                soundLevelIndicator.resetSoundLevel();
            }
        }

        this.deviceSession = deviceSession;

        if (deviceSession != null)
        {
            deviceSession.setContentDescriptor(
                new ContentDescriptor(ContentDescriptor.RAW));
            deviceSession.setLocalUserAudioLevelListener(
                level -> soundLevelIndicator.updateSoundLevel(level));

            deviceSession.start(MediaDirection.SENDONLY);

            try
            {
                DataSource dataSource = deviceSession.getOutputDataSource();

                dataSource.connect();

                PushBufferStream[] streams
                    = ((PushBufferDataSource) dataSource).getStreams();

                Buffer transferHandlerBuffer = new Buffer();
                for (PushBufferStream stream : streams)
                {
                    stream.setTransferHandler(s ->
                    {
                        try
                        {
                            s.read(transferHandlerBuffer);
                        }
                        catch (IOException ioe)
                        {
                            // ignore
                        }
                    });
                }

                dataSource.start();
            }
            catch (Exception t)
            {
                // ignore
            }
        }
    }
}
