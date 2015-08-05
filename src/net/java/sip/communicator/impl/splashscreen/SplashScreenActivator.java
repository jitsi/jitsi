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
package net.java.sip.communicator.impl.splashscreen;

import org.osgi.framework.*;

import java.awt.*;

/**
 * Activates if splash screen is available to draw progress and
 * currently loading bundle name.
 *
 * @author Damian Minkov
 */
public class SplashScreenActivator
    implements BundleActivator,
               ServiceListener
{
    /**
     * A reference to the bundle context that is currently in use.
     */
    private BundleContext bundleContext = null;

    /**
     * The splash screen if any.
     */
    private SplashScreen splash;

    /**
     * The splash screen graphics.
     */
    private Graphics2D g;

    /**
     * Progress so far.
     */
    private int progress = 0;

    /**
     * The colors.
     */
    private Color TEXT_BACKGROUND = new Color(203, 202, 202);
    private Color TEXT_FOREGROUND = new Color(82, 82, 82);
    private Color PROGRESS_FOREGROUND = new Color(177, 174, 173);

    /**
     * Starts if the splash screen is available.
     *
     * @throws Exception if starting the arg delegation bundle and registering
     * the delegationPeer with the util package URI manager fails
     */
    public void start(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        splash = SplashScreen.getSplashScreen();

        if(splash == null)
            return;

        g = splash.createGraphics();

        if(g == null)
            return;

        bundleContext.addServiceListener(this);
    }

    /**
     * Unsets the listener that we set when we start this bundle.
     *
     * @param bc an instance of the currently valid bundle context.
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(this);

        this.g = null;
        this.splash = null;
        TEXT_BACKGROUND = null; TEXT_FOREGROUND = null;
        PROGRESS_FOREGROUND = null;
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        if(splash == null)
            return;

        synchronized(splash)
        {
            try
            {
                if(!splash.isVisible())
                {
                    stop(bundleContext);
                    return;
                }

                Bundle bundle =
                    serviceEvent.getServiceReference().getBundle();

                if(bundle == null)
                    return;

                Object bundleName =
                    bundle.getHeaders().get(Constants.BUNDLE_NAME);

                if(bundleName == null)
                    return;

                // If the main frame was set visible, the splash screen was/will
                // be closed by Java automatically. Otherwise we need to do that
                // manually.
                Object service =
                    bundleContext
                        .getService(serviceEvent.getServiceReference());
                if (service.getClass().getSimpleName().equals("UIServiceImpl"))
                {
                    splash.close();
                    stop(bundleContext);
                    return;
                }

                bundleContext.ungetService(serviceEvent.getServiceReference());

                progress++;

                int progressWidth = 233;
                int progressHeight = 14;
                int progressX = 168;
                int progressY = 97;

                int textHeight = 20;
                int textBaseX = 150;
                int textBaseY = 145 + (50 - textHeight)/2 + textHeight;

                int currentProgressWidth = Math.min(2*progress, progressWidth);

                g.setComposite(AlphaComposite.Clear);
                g.setPaintMode();
                // first clear the space for text
                g.setColor(TEXT_BACKGROUND);
                g.clearRect(
                    textBaseX - 1,// -1 to clear a pix left from some txt
                    textBaseY - textHeight,
                    (int) this.splash.getBounds().getWidth() - textBaseX,
                    textHeight + 5);
                g.fillRect(
                    textBaseX - 1,// -1 to clear a pix left from some txt
                    textBaseY - textHeight,
                    (int) this.splash.getBounds().getWidth() - textBaseX,
                    textHeight + 5);

                // then fill the progress
                g.setColor(PROGRESS_FOREGROUND);
                g.fillRect(progressX, progressY,
                    currentProgressWidth, progressHeight);
                g.drawRect(progressX, progressY,
                    currentProgressWidth, progressHeight);

                g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g.setColor(TEXT_FOREGROUND);

                g.drawString(bundleName.toString(), textBaseX, textBaseY);

                splash.update();
            }
            catch(Throwable e)
            {
                stop(bundleContext);
                return;
            }
        }
    }
}
