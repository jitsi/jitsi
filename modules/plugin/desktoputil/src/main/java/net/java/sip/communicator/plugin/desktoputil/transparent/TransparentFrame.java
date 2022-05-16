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
package net.java.sip.communicator.plugin.desktoputil.transparent;

import java.awt.*;

import javax.swing.*;

/**
 *
 * @author Yana Stamcheva
 */
public class TransparentFrame
    extends JFrame
    implements RootPaneContainer
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates if the transparency is supported from the current graphics
     * environment.
     */
    public static boolean isTranslucencySupported;

    /**
     * Creates a transparent undecorated frame. If the transparency is not
     * supported creates a normal undecorated frame.
     *
     * @return the created frame
     */
    public static TransparentFrame createTransparentFrame()
    {
        isTranslucencySupported
            = AWTUtilitiesWrapper.isTranslucencySupported(
                AWTUtilitiesWrapper.PERPIXEL_TRANSLUCENT);

        GraphicsConfiguration translucencyCapableGC
            = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();

        if (!AWTUtilitiesWrapper.isTranslucencyCapable(translucencyCapableGC))
        {
            translucencyCapableGC = null;

            GraphicsEnvironment env
                = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = env.getScreenDevices();

            for (int i = 0; i < devices.length
                            && translucencyCapableGC == null; i++)
            {
                GraphicsConfiguration[] configs = devices[i].getConfigurations();
                for (int j = 0; j < configs.length
                                && translucencyCapableGC == null; j++)
                {
                    if (AWTUtilitiesWrapper.isTranslucencyCapable(configs[j]))
                    {
                        translucencyCapableGC = configs[j];
                    }
                }
            }
            if (translucencyCapableGC == null)
            {
                isTranslucencySupported = false;
            }
        }

        if (isTranslucencySupported)
            return new TransparentFrame(translucencyCapableGC);

        return new TransparentFrame();
    }

    /**
     * Creates an undecorated transparent frame.
     *
     * @param gc the <tt>GraphicsConfiguration</tt> to use
     */
    private TransparentFrame(GraphicsConfiguration gc)
    {
        super(gc);

        setUndecorated(true);
        AWTUtilitiesWrapper.setWindowOpaque(this, false);
        AWTUtilitiesWrapper.setWindowOpacity(this, 1f);
    }

    /**
     * Creates an undecorated frame.
     */
    private TransparentFrame()
    {
        setUndecorated(true);
    }
}
