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
package net.java.sip.communicator.impl.osdependent;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.desktop.*;

/**
 * Implementation of the <tt>DesktopService</tt>.
 *
 * @author Yana Stamcheva
 */
public class DesktopServiceImpl
    implements DesktopService
{
    private final Desktop defaultDesktop;

    /**
     * Creates a <tt>DesktopServiceImpl</tt> and initializes the default
     * desktop to use for all desktop operations.
     */
    public DesktopServiceImpl()
    {
        defaultDesktop = Desktop.getDefaultDesktop();
    }

    /**
     * Invokes the default desktop browse method.
     *
     * @see DesktopService#browse(URI)
     */
    public void browse(URI uri)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().browse(uri);
    }

    /**
     * Invokes the default desktop edit method.
     *
     * @see DesktopService#edit(File)
     */
    public void edit(File file)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().edit(file);
    }

    /**
     * Invokes the default desktop open method.
     *
     * @see DesktopService#open(File)
     */
    public void open(File file)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().open(file);
    }

    /**
     * Invokes the default desktop print method.
     *
     * @see DesktopService#print(File)
     */
    public void print(File file)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().print(file);
    }
}
