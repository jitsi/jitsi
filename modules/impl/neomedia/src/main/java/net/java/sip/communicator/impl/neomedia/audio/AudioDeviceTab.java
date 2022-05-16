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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.impl.neomedia.device.AudioSystem.*;
import org.jitsi.service.audionotifier.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.resources.*;

public class AudioDeviceTab extends TransparentPanel
{
    /**
     * The name of the sound file used to test the playback and the notification
     * devices.
     */
    private static final String TEST_SOUND_FILENAME_PROP
        = "net.java.sip.communicator.impl.neomedia.TestSoundFilename";

    private final static int WIDTH = 350;
    private final static int HEIGHT = 305;

    /**
     * The thread which updates the capture device as selected by the user. This
     * prevent the UI to lock while changing the device.
     */
    private final AudioLevelListenerThread audioLevelListenerThread
        = new AudioLevelListenerThread();

    private JComboBox<CaptureDeviceViewModel> playbackCombo;
    private JComboBox<CaptureDeviceViewModel> notifyCombo;
    private JButton playbackPlaySoundButton;
    private JButton notificationPlaySoundButton;

    private final JComponent audioSystemControlsContainer;
    private final JComboBox<AudioSystem> audioSystemCombo;

    private final MediaServiceImpl mediaService;
    private final ResourceManagementService res;

    public AudioDeviceTab()
    {
        res = NeomediaActivator.getResources();
        mediaService = NeomediaActivator.getMediaServiceImpl();
        boolean setAudioSystemIsDisabled = NeomediaActivator
            .getConfigurationService()
            .getBoolean(MediaServiceImpl.DISABLE_SET_AUDIO_SYSTEM_PNAME, false);

        setLayout(new BorderLayout());

        if (setAudioSystemIsDisabled)
        {
            audioSystemCombo = null;
        }
        else
        {
            JLabel deviceLabel = new JLabel(
                res.getI18NString("impl.media.configform.AUDIO"));

            audioSystemCombo = new JComboBox<>();
            audioSystemCombo.setModel(new AudioSystemComboBoxModel(mediaService.getDeviceConfiguration()));
            audioSystemCombo.addActionListener(e -> deviceComboBoxActionListener());

            Container devicePanel =
                new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
            devicePanel.add(deviceLabel);
            devicePanel.add(audioSystemCombo);
            add(devicePanel, BorderLayout.NORTH);
        }

        JComponent controlsWrapper = new TransparentPanel(new BorderLayout());
        audioSystemControlsContainer = new TransparentPanel(new GridBagLayout());
        controlsWrapper.add(audioSystemControlsContainer, BorderLayout.NORTH);
        add(controlsWrapper, BorderLayout.CENTER);
        deviceComboBoxActionListener();
    }

    private void deviceComboBoxActionListener()
    {
        if (audioSystemCombo.getSelectedItem() instanceof AudioSystem)
        {
            createAudioSystemControls(
                (AudioSystem) audioSystemCombo.getSelectedItem(),
                audioSystemControlsContainer);
        }
    }

    /**
     * Creates the UI controls which are to control the details of a specific
     * <tt>AudioSystem</tt>.
     *
     * @param audioSystem the <tt>AudioSystem</tt> for which the UI controls to
     * control its details are to be created
     * @param container the <tt>JComponent</tt> into which the UI controls which
     * are to control the details of the specified <tt>audioSystem</tt> are to
     * be added
     */
    public void createAudioSystemControls(
        final AudioSystem audioSystem,
        JComponent container)
    {
        audioSystemControlsContainer.removeAll();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(3, 0, 3, 3);

        JComboBox<CaptureDeviceViewModel> captureCombo = null;
        SoundLevelIndicator capturePreview = null;

        int audioSystemFeatures = audioSystem.getFeatures();
        boolean featureNotifyAndPlaybackDevices
            = (audioSystemFeatures
            & AudioSystem.FEATURE_NOTIFY_AND_PLAYBACK_DEVICES) != 0;
        if (featureNotifyAndPlaybackDevices)
        {
            // capture
            constraints.weightx = 0.2;
            container.add(new JLabel(
                res.getI18NString("impl.media.configform.AUDIO_IN")),
                constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.7;
            captureCombo = new JComboBox<>();
            captureCombo.setEditable(false);
            captureCombo.setModel(
                new AudioDeviceComboBoxModel(
                    mediaService.getDeviceConfiguration(),
                    DataFlow.CAPTURE));
            container.add(captureCombo, constraints);

            constraints.gridy = 1;
            constraints.weightx = 0.7;
            capturePreview = new SoundLevelIndicator(
                SimpleAudioLevelListener.MIN_LEVEL,
                SimpleAudioLevelListener.MAX_LEVEL);
            container.add(capturePreview, constraints);

            // out
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 0.2;
            container.add(new JLabel(
                res.getI18NString("impl.media.configform.AUDIO_OUT")),
                constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.7;
            playbackCombo = new JComboBox<>();
            playbackCombo.setEditable(false);
            playbackCombo.setModel(
                new AudioDeviceComboBoxModel(
                    mediaService.getDeviceConfiguration(),
                    AudioSystem.DataFlow.PLAYBACK));
            playbackCombo.addActionListener(this::playButtonActionListener);
            container.add(playbackCombo, constraints);

            // out play sound button
            constraints.gridx = 2;
            constraints.weightx = 0.1;
            playbackPlaySoundButton = new JButton("\u25B6");
            if(((CaptureDeviceViewModel)
                playbackCombo.getSelectedItem()).info == null)
            {
                playbackPlaySoundButton.setEnabled(false);
            }
            playbackPlaySoundButton.setOpaque(false);
            playbackPlaySoundButton.addActionListener(this::playButtonActionListener);
            container.add(playbackPlaySoundButton, constraints);

            // Notification
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.weightx = 0.2;
            container.add(new JLabel(
                    res.getI18NString("impl.media.configform.AUDIO_NOTIFY")),
                constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.7;
            notifyCombo = new JComboBox<>();
            notifyCombo.setEditable(false);
            notifyCombo.setModel(
                new AudioDeviceComboBoxModel(
                    mediaService.getDeviceConfiguration(),
                    AudioSystem.DataFlow.NOTIFY));
            notifyCombo.addActionListener(this::playButtonActionListener);
            container.add(notifyCombo, constraints);

            // Notification play sound button
            constraints.gridx = 2;
            constraints.weightx = 0.1;
            notificationPlaySoundButton = new JButton("\u25B6");
            if(((CaptureDeviceViewModel)
                notifyCombo.getSelectedItem()).info == null)
            {
                notificationPlaySoundButton.setEnabled(false);
            }
            notificationPlaySoundButton.setOpaque(false);
            notificationPlaySoundButton.addActionListener(this::playButtonActionListener);
            container.add(notificationPlaySoundButton, constraints);
        }

        int[] checkBoxAudioSystemFeatures = new int[]
        {
            AudioSystem.FEATURE_ECHO_CANCELLATION,
            AudioSystem.FEATURE_DENOISE,
            AudioSystem.FEATURE_AGC
        };

        for (final int feature : checkBoxAudioSystemFeatures)
        {
            constraints.gridx = 1;
            constraints.gridy++;
            constraints.weightx = 0.7;
            if ((feature & audioSystemFeatures) != 0)
            {
                String textKey;
                boolean selected;

                switch (feature)
                {
                case AudioSystem.FEATURE_AGC:
                    textKey = "impl.media.configform.AUTOMATICGAINCONTROL";
                    selected = audioSystem.isAutomaticGainControl();
                    break;
                case AudioSystem.FEATURE_DENOISE:
                    textKey = "impl.media.configform.DENOISE";
                    selected = audioSystem.isDenoise();
                    break;
                case AudioSystem.FEATURE_ECHO_CANCELLATION:
                    textKey = "impl.media.configform.ECHOCANCEL";
                    selected = audioSystem.isEchoCancel();
                    break;
                default:
                    continue;
                }

                final SIPCommCheckBox checkBox
                    = new SIPCommCheckBox(
                    NeomediaActivator.getResources().getI18NString(
                        textKey));

                // First set the selected one, then add the listener in order to
                // avoid saving the value when using the default one and only
                // showing to user without modification.
                checkBox.setSelected(selected);
                checkBox.addItemListener(e ->
                {
                    boolean b = checkBox.isSelected();

                    switch (feature)
                    {
                    case AudioSystem.FEATURE_AGC:
                        audioSystem.setAutomaticGainControl(b);
                        break;
                    case AudioSystem.FEATURE_DENOISE:
                        audioSystem.setDenoise(b);
                        break;
                    case AudioSystem.FEATURE_ECHO_CANCELLATION:
                        audioSystem.setEchoCancel(b);
                        break;
                    }
                });
                container.add(checkBox, constraints);
            }
        }

        invalidate();
        repaint();
        audioLevelListenerThread.init(
            audioSystem,
            captureCombo,
            capturePreview);
    }

    /**
     * Indicates that one of the contained in this panel buttons has been
     * clicked.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void playButtonActionListener(ActionEvent e)
    {
        boolean isPlaybackEvent = e.getSource() == playbackPlaySoundButton;

        // If the user clicked on one play sound button.
        if(isPlaybackEvent || e.getSource() == notificationPlaySoundButton)
        {
            AudioNotifierService audioNotifServ
                = NeomediaActivator.getAudioNotifierService();
            String testSoundFilename
                = NeomediaActivator.getConfigurationService()
                .getString(
                    TEST_SOUND_FILENAME_PROP,
                    NeomediaActivator.getResources().getSoundPath(
                        "TEST_SOUND")
                );
            SCAudioClip sound = audioNotifServ.createAudio(
                testSoundFilename,
                isPlaybackEvent);
            sound.play();
        }
        // If the selected item of the playback or notify combobox has changed.
        else if(e.getSource() == playbackCombo || e.getSource() == notifyCombo)
        {
            CaptureDeviceViewModel device
                = (CaptureDeviceViewModel)
                ((JComboBox) e.getSource()).getSelectedItem();

            boolean isEnabled = device.info != null;
            if(e.getSource() == playbackCombo)
            {
                playbackPlaySoundButton.setEnabled(isEnabled);
            }
            else
            {
                notificationPlaySoundButton.setEnabled(isEnabled);
            }
        }
    }
}
