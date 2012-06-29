/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.neomedia.codec.video.h264.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.packetlogging.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the neomedia bundle.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class NeomediaActivator
    implements BundleActivator
{

    /**
     * The <tt>Logger</tt> used by the <tt>NeomediaActivator</tt> class and its
     * instances for logging output.
     */
    private final Logger logger = Logger.getLogger(NeomediaActivator.class);

    /**
     * Indicates if the audio configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String AUDIO_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.AUDIO_CONFIG_DISABLED";

    /**
     * Indicates if the video configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String VIDEO_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.VIDEO_CONFIG_DISABLED";

    /**
     * Indicates if the H.264 configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String H264_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.h264config.DISABLED";

    /**
     * Indicates if the ZRTP configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String ZRTP_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.zrtpconfig.DISABLED";

    /**
     * Indicates if the call recording config form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String CALL_RECORDING_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.callrecordingconfig.DISABLED";

    /**
     * The context in which the one and only <tt>NeomediaActivator</tt> instance
     * has started executing.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>NeomediaActivator</tt> instance to read and write
     * configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * The <tt>FileAccessService</tt> registered in {@link #bundleContext} and
     * used by the <tt>NeomediaActivator</tt> instance to safely access files.
     */
    private static FileAccessService fileAccessService;

    /**
     * The one and only <tt>MediaServiceImpl</tt> instance registered in
     * {@link #bundleContext} by the <tt>NeomediaActivator</tt> instance.
     */
    private static MediaServiceImpl mediaServiceImpl;

    /**
     * The <tt>ResourceManagementService</tt> registered in
     * {@link #bundleContext} and representing the resources such as
     * internationalized and localized text and images used by the neomedia
     * bundle.
     */
    private static ResourceManagementService resources;

    /**
     * The OSGi <tt>PacketLoggingService</tt> of {@link #mediaServiceImpl} in
     * {@link #bundleContext} and used for debugging.
     */
    private static PacketLoggingService packetLoggingService  = null;

    private PropertyChangeListener deviceConfigurationPropertyChangeListener;

    /**
     * Audio configuration dialog.
     */
    private static SIPCommDialog audioConfigDialog = null;

    /**
     * Starts the execution of the neomedia bundle in the specified context.
     *
     * @param bundleContext the context in which the neomedia bundle is to start
     * executing
     * @throws Exception if an error occurs while starting the execution of the
     * neomedia bundle in the specified context
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");

        NeomediaActivator.bundleContext = bundleContext;

        // MediaService
        mediaServiceImpl = (MediaServiceImpl) LibJitsi.getMediaService();

        bundleContext.registerService(
                MediaService.class.getName(),
                mediaServiceImpl,
                null);
        if (logger.isDebugEnabled())
            logger.debug("Media Service ... [REGISTERED]");

        ConfigurationService cfg = NeomediaActivator.getConfigurationService();
        Dictionary<String, String> mediaProps = new Hashtable<String, String>();

        mediaProps.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.GENERAL_TYPE);

        // If the audio configuration form is disabled don't register it.
        if ((cfg == null) || !cfg.getBoolean(AUDIO_CONFIG_DISABLED_PROP, false))
        {
            final ConfigurationForm audioConfigurationForm
                = new LazyConfigurationForm(
                        AudioConfigurationPanel.class.getName(),
                        getClass().getClassLoader(),
                        "plugin.mediaconfig.AUDIO_ICON",
                        "impl.neomedia.configform.AUDIO",
                        3);

            bundleContext.registerService(
                    ConfigurationForm.class.getName(),
                    audioConfigurationForm,
                    mediaProps);

            if (deviceConfigurationPropertyChangeListener == null)
            {
                deviceConfigurationPropertyChangeListener
                    = new PropertyChangeListener()
                    {
                        public void propertyChange(PropertyChangeEvent event)
                        {
                            if (DeviceConfiguration.PROP_AUDIO_SYSTEM_DEVICES
                                    .equals(event.getPropertyName()))
                            {
                                showAudioConfiguration();
                            }
                        }
                    };
                mediaServiceImpl
                    .getDeviceConfiguration()
                        .addPropertyChangeListener(
                                deviceConfigurationPropertyChangeListener);
            }
        }

        // If the video configuration form is disabled don't register it.
        if ((cfg == null) || !cfg.getBoolean(VIDEO_CONFIG_DISABLED_PROP, false))
        {
            bundleContext.registerService(
                    ConfigurationForm.class.getName(),
                    new LazyConfigurationForm(
                            VideoConfigurationPanel.class.getName(),
                            getClass().getClassLoader(),
                            "plugin.mediaconfig.VIDEO_ICON",
                            "impl.neomedia.configform.VIDEO",
                            4),
                    mediaProps);
        }

        // H.264
        // If the H.264 configuration form is disabled don't register it.
        if ((cfg == null) || !cfg.getBoolean(H264_CONFIG_DISABLED_PROP, false))
        {
            Dictionary<String, String> h264Props
                = new Hashtable<String, String>();

            h264Props.put(
                    ConfigurationForm.FORM_TYPE,
                    ConfigurationForm.ADVANCED_TYPE);
            bundleContext.registerService(
                    ConfigurationForm.class.getName(),
                    new LazyConfigurationForm(
                            ConfigurationPanel.class.getName(),
                            getClass().getClassLoader(),
                            "plugin.mediaconfig.VIDEO_ICON",
                            "impl.neomedia.configform.H264",
                            -1,
                            true),
                    h264Props);
        }

        // ZRTP
        // If the ZRTP configuration form is disabled don't register it.
        if ((cfg == null) || !cfg.getBoolean(ZRTP_CONFIG_DISABLED_PROP, false))
        {
            Dictionary<String, String> securityProps
                = new Hashtable<String, String>();

            securityProps.put( ConfigurationForm.FORM_TYPE,
                            ConfigurationForm.SECURITY_TYPE);
            bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    SecurityConfigForm.class.getName(),
                    getClass().getClassLoader(),
                    "impl.media.security.zrtp.CONF_ICON",
                    "impl.media.security.zrtp.TITLE",
                    0),
                securityProps);
        }

        //we use the nist-sdp stack to make parse sdp and we need to set the
        //following property to make sure that it would accept java generated
        //IPv6 addresses that contain address scope zones.
        System.setProperty("gov.nist.core.STRIP_ADDR_SCOPES", "true");

        // AudioNotifierService
        AudioNotifierService audioNotifierService
            = LibJitsi.getAudioNotifierService();

        audioNotifierService.setMute(
                (cfg == null)
                    || !cfg.getBoolean(
                            "net.java.sip.communicator"
                                + ".impl.sound.isSoundEnabled",
                            true));
        bundleContext.registerService(
                AudioNotifierService.class.getName(),
                audioNotifierService,
                null);

        if (logger.isInfoEnabled())
            logger.info("Audio Notifier Service ...[REGISTERED]");

        // Call Recording
        // If the call recording configuration form is disabled don't continue.
        if ((cfg == null)
                || !cfg.getBoolean(CALL_RECORDING_CONFIG_DISABLED_PROP, false))
        {
            Dictionary<String, String> callRecordingProps
                = new Hashtable<String, String>();

            callRecordingProps.put(
                    ConfigurationForm.FORM_TYPE,
                    ConfigurationForm.ADVANCED_TYPE);
            bundleContext.registerService(
                    ConfigurationForm.class.getName(),
                    new LazyConfigurationForm(
                            CallRecordingConfigForm.class.getName(),
                            getClass().getClassLoader(),
                            null,
                            "plugin.callrecordingconfig.CALL_RECORDING_CONFIG", 
                            1100,
                            true), 
                    callRecordingProps);
        }
    }

    /**
     * Show audio configuration panel when media devices change.
     */
    private void showAudioConfiguration()
    {
        if(audioConfigDialog != null && audioConfigDialog.isVisible())
        {
            // Because toFront() method gives us no guarantee that our dialog
            // would go on top we'll try to also first request the focus and
            // set our dialog always on top to put all the chances on our side.
            audioConfigDialog.requestFocus();
            audioConfigDialog.setAlwaysOnTop(true);
            audioConfigDialog.toFront();
            audioConfigDialog.setAlwaysOnTop(false);
            return;
        }

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            showAudioConfiguration();
                        }
                    });
            return;
        }

        audioConfigDialog =
            new SIPCommDialog()
            {
                /** Serial version UID. */
                private static final long serialVersionUID = 0L;

                /** {@inheritDoc} */
                @Override
                protected void close(boolean escaped)
                {
                    setVisible(false);
                    audioConfigDialog = null;
                }
            };

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(20, 5));
        TransparentPanel fieldsPanel
            = new TransparentPanel(new BorderLayout(10, 5));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20));

        TransparentPanel btnPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        ResourceManagementService resources
            = NeomediaActivator.getResources();
        JButton btn
            = new JButton(resources.getI18NString("service.gui.CLOSE"));

        btn.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent evt)
                    {
                        audioConfigDialog.setVisible(false);
                    }
                });
        btnPanel.add(btn);

        JTextArea infoTextArea = new JTextArea();

        infoTextArea.setOpaque(false);
        infoTextArea.setEditable(false);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setLineWrap(true);
        infoTextArea.setText(
                resources.getI18NString(
                        "impl.media.configform"
                            + ".AUDIO_DEVICE_CONNECTED_REMOVED"));

        JPanel preview = new TransparentPanel(new GridBagLayout());
        MediaConfiguration.createAudioSystemControls(
            mediaServiceImpl.getDeviceConfiguration().getAudioSystem(),
            preview);

        fieldsPanel.add(infoTextArea, BorderLayout.NORTH);
        fieldsPanel.add(preview, BorderLayout.CENTER);
        fieldsPanel.add(btnPanel, BorderLayout.SOUTH);

        TransparentPanel iconPanel
            = new TransparentPanel(new BorderLayout());

        iconPanel.add(
                new JLabel(
                        resources.getImage(
                                "plugin.mediaconfig.AUDIO_ICON_64x64")),
                BorderLayout.NORTH);

        mainPanel.add(iconPanel, BorderLayout.WEST);
        mainPanel.add(fieldsPanel, BorderLayout.CENTER);

        audioConfigDialog.setTitle(
                resources.getI18NString(
                        "impl.media.configform.AUDIO_DEVICE_CONFIG"));
        audioConfigDialog.add(mainPanel);
        audioConfigDialog.validate();
        audioConfigDialog.pack();

        audioConfigDialog.setVisible(true);
    }

    /**
     * Stops the execution of the neomedia bundle in the specified context.
     *
     * @param bundleContext the context in which the neomedia bundle is to stop
     * executing
     * @throws Exception if an error occurs while stopping the execution of the
     * neomedia bundle in the specified context
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        try
        {
            if (deviceConfigurationPropertyChangeListener != null)
            {
                mediaServiceImpl
                    .getDeviceConfiguration()
                        .removePropertyChangeListener(
                                deviceConfigurationPropertyChangeListener);
                deviceConfigurationPropertyChangeListener = null;
            }
        }
        finally
        {
            configurationService = null;
            fileAccessService = null;
            mediaServiceImpl = null;
            resources = null;
        }
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a FileAccessService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * FileAccessService .
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    /**
     * Gets the <tt>MediaService</tt> implementation instance registered by the
     * neomedia bundle.
     *
     * @return the <tt>MediaService</tt> implementation instance registered by
     * the neomedia bundle
     */
    public static MediaServiceImpl getMediaServiceImpl()
    {
        return mediaServiceImpl;
    }

    /**
     * Gets the <tt>ResourceManagementService</tt> instance which represents the
     * resources such as internationalized and localized text and images used by
     * the neomedia bundle.
     *
     * @return the <tt>ResourceManagementService</tt> instance which represents
     * the resources such as internationalized and localized text and images
     * used by the neomedia bundle
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resources;
    }

    /**
     * Returns a reference to the <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static PacketLoggingService getPacketLogging()
    {
        if (packetLoggingService == null)
        {
            packetLoggingService
                = ServiceUtils.getService(
                        bundleContext,
                        PacketLoggingService.class);
        }
        return packetLoggingService;
    }
}
