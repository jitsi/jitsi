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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.image.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.cert.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.neomedia.*;
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

    private static AccountManager accountManager;

    private static FileAccessService fileAccessService;

    private static MediaService mediaService;

    private static AudioNotifierService audioNotifierService;

    private static GlobalDisplayDetailsService globalDisplayDetailsService;

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

                    AuthenticationWindowCreator creator
                        = new AuthenticationWindowCreator(
                                userName,
                                password,
                                server,
                                isUserNameEditable,
                                isRememberPassword,
                                imageIcon,
                                windowTitle,
                                windowText,
                                usernameLabelText,
                                passwordLabelText,
                                errorMessage,
                                signupLink);

                    try
                    {
                        SwingUtilities.invokeAndWait(creator);
                    }
                    catch(InterruptedException e)
                    {
                        logger.error("Error creating dialog", e);
                    }
                    catch(InvocationTargetException e)
                    {
                        logger.error("Error creating dialog", e);
                    }

                    return creator.authenticationWindow;
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
        VerifyCertificateDialogCreator creator
            = new VerifyCertificateDialogCreator(certs, title, message);

        try
        {
            SwingUtilities.invokeAndWait(creator);
        }
        catch(InterruptedException e)
        {
            logger.error("Error creating dialog", e);
        }
        catch(InvocationTargetException e)
        {
            logger.error("Error creating dialog", e);
        }

        return creator.dialog;
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
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
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService
                = ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
    }

    /**
     * Returns the <tt>AudioNotifierService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioNotifierService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifier()
    {
        if (audioNotifierService == null)
        {
            audioNotifierService
                = ServiceUtils.getService(
                        bundleContext,
                        AudioNotifierService.class);
        }
        return audioNotifierService;
    }
    
    /**
     * Returns the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context
     */
    public static GlobalDisplayDetailsService getGlobalDisplayDetailsService()
    {
        if (globalDisplayDetailsService == null)
        {
            globalDisplayDetailsService
                = ServiceUtils.getService(
                        bundleContext,
                        GlobalDisplayDetailsService.class);
        }
        return globalDisplayDetailsService;
    }

    /**
     * Runnable to create verify dialog.
     */
    private class VerifyCertificateDialogCreator
        implements Runnable
    {
        /**
         * Certs.
         */
        private final Certificate[] certs;

        /**
         * Dialog title.
         */
        private final String title;

        /**
         * Dialog message.
         */
        private final String message;

        /**
         * The result dialog.
         */
        VerifyCertificateDialogImpl dialog = null;

        /**
         * Constructs.
         * @param certs the certificates list
         * @param title The title of the dialog; when null the resource
         * <tt>service.gui.CERT_DIALOG_TITLE</tt> is loaded and used.
         * @param message A text that describes why the verification failed.
         */
        private VerifyCertificateDialogCreator(
            Certificate[] certs, String title, String message)
        {
            this.certs = certs;
            this.title = title;
            this.message = message;
        }

        @Override
        public void run()
        {
            dialog = new VerifyCertificateDialogImpl(certs, title, message);
        }
    }

    /**
     * Runnable to create auth window.
     */
    private class AuthenticationWindowCreator
        implements Runnable
    {
        String userName;
        char[] password;
        String server;
        boolean isUserNameEditable;
        boolean isRememberPassword;
        String windowTitle;
        String windowText;
        String usernameLabelText;
        String passwordLabelText;
        String errorMessage;
        String signupLink;
        ImageIcon imageIcon;

        AuthenticationWindowService.AuthenticationWindow authenticationWindow;

        /**
         * Creates an instance of the <tt>AuthenticationWindow</tt>
         * implementation.
         *
         * @param server the server name
         * @param isUserNameEditable indicates if the user name is editable
         * @param imageIcon the icon to display on the left of
         * the authentication window
         * @param windowTitle customized window title
         * @param windowText customized window text
         * @param usernameLabelText customized username field label text
         * @param passwordLabelText customized password field label text
         * @param errorMessage an error message if this dialog is shown
         * to indicate the user that something went wrong
         * @param signupLink an URL that allows the user to sign up
         */
        public AuthenticationWindowCreator(String userName,
                                           char[] password,
                                           String server,
                                           boolean isUserNameEditable,
                                           boolean isRememberPassword,
                                           ImageIcon imageIcon,
                                           String windowTitle,
                                           String windowText,
                                           String usernameLabelText,
                                           String passwordLabelText,
                                           String errorMessage,
                                           String signupLink)
        {
            this.userName = userName;
            this.password = password;
            this.server = server;
            this.isUserNameEditable = isUserNameEditable;
            this.isRememberPassword = isRememberPassword;
            this.windowTitle = windowTitle;
            this.windowText = windowText;
            this.usernameLabelText = usernameLabelText;
            this.passwordLabelText = passwordLabelText;
            this.errorMessage = errorMessage;
            this.signupLink = signupLink;
            this.imageIcon = imageIcon;
        }

        @Override
        public void run()
        {
            authenticationWindow
                = new net.java.sip.communicator.plugin.desktoputil
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
    }
}
