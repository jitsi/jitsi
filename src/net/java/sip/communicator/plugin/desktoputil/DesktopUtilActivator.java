package net.java.sip.communicator.plugin.desktoputil;

import java.awt.image.*;
import java.net.*;
import java.security.cert.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

public class DesktopUtilActivator
    implements BundleActivator,
               VerifyCertificateDialogService
{
    /**
     * The <tt>Logger</tt> used by the <tt>SwingUtilActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(DesktopUtilActivator.class);

    private static ConfigurationService configurationService;

    private static ResourceManagementService resourceService;

    private static KeybindingsService keybindingsService;

    private static BrowserLauncherService browserLauncherService;

    private static UIService uiService;

    static BundleContext bundleContext;

    /**
     * Calls <tt>Thread.setUncaughtExceptionHandler()</tt>
     *
     * @param context The execution context of the bundle being started
     * (unused).
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;

        // register the VerifyCertificateDialogService
        bundleContext.registerService(
            VerifyCertificateDialogService.class.getName(),
            this,
            null);

        bundleContext.registerService(
            MasterPasswordInputService.class.getName(),
            new MasterPasswordInputService()
            {
                public String showInputDialog(boolean prevSuccess)
                {
                    return MasterPasswordInputDialog.showInput(prevSuccess);
                }
            },
            null);

        bundleContext.registerService(
            AuthenticationWindowService.class.getName(),
            new AuthenticationWindowService()
            {
                public AuthenticationWindow create(
                            String userName,
                            char[] password,
                            String server,
                            boolean isUserNameEditable,
                            boolean isRememberPassword,
                            Object icon,
                            String windowTitle,
                            String windowText,
                            String usernameLabelText,
                            String passwordLabelText,
                            String errorMessage,
                            String signupLink)
                {
                    ImageIcon imageIcon = null;

                    if(icon instanceof ImageIcon)
                        imageIcon = (ImageIcon)icon;

                    return new net.java.sip.communicator.plugin.desktoputil
                        .AuthenticationWindow(
                            userName, password,
                            server,
                            isUserNameEditable, isRememberPassword,
                            imageIcon,
                            windowTitle, windowText,
                            usernameLabelText, passwordLabelText,
                            errorMessage,
                            signupLink);
                }
            },
            null);
    }

    /**
     * Doesn't do anything.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {
    }

    /**
     * Returns the <tt>ConfigurationService</tt> currently registered.
     *
     * @return the <tt>ConfigurationService</tt>
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
     * Returns the service giving access to all application resources.
     *
     * @return the service giving access to all application resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resourceService;
    }

    /**
     * Returns the image corresponding to the given <tt>imageID</tt>.
     *
     * @param imageID the identifier of the image
     * @return the image corresponding to the given <tt>imageID</tt>
     */
    public static BufferedImage getImage(String imageID)
    {
        BufferedImage image = null;

        URL path = getResources().getImageURL(imageID);

        if (path == null)
            return null;

        try
        {
            image = ImageIO.read(path);
        }
        catch (Exception exc)
        {
            logger.error("Failed to load image:" + path, exc);
        }

        return image;
    }

    /**
     * Returns the <tt>KeybindingsService</tt> currently registered.
     *
     * @return the <tt>KeybindingsService</tt>
     */
    public static KeybindingsService getKeybindingsService()
    {
        if (keybindingsService == null)
        {
            keybindingsService
                = ServiceUtils.getService(
                        bundleContext,
                        KeybindingsService.class);
        }
        return keybindingsService;
    }


    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            browserLauncherService
                = ServiceUtils.getService(
                        bundleContext,
                        BrowserLauncherService.class);
        }
        return browserLauncherService;
    }

    /**
     * Gets the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>UtilActivator</tt>.
     *
     * @return the <tt>UIService</tt> instance registered in the
     * <tt>BundleContext</tt> of the <tt>UtilActivator</tt>
     */
    public static UIService getUIService()
    {
        if (uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * Creates the dialog.
     *
     * @param certs the certificates list
     * @param title The title of the dialog; when null the resource
     * <tt>service.gui.CERT_DIALOG_TITLE</tt> is loaded and used.
     * @param message A text that describes why the verification failed.
     */
    public VerifyCertificateDialog createDialog(
        Certificate[] certs, String title, String message)
    {
        return new VerifyCertificateDialogImpl(certs, title, message);
    }
}