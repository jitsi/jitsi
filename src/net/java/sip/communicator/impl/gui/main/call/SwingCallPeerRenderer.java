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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import net.java.sip.communicator.service.gui.call.*;

/**
 * The <tt>CallPeerRenderer</tt> interface is meant to be implemented by
 * different renderers of <tt>CallPeer</tt>s. Through this interface they would
 * could be updated in order to reflect the current state of the CallPeer.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public interface SwingCallPeerRenderer
    extends CallPeerRenderer
{
    /**
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     *
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel();

    /**
     * Returns the AWT <tt>Component</tt> which is the user interface equivalent
     * of this <tt>CallPeerRenderer</tt>.
     *
     * @return the AWT <tt>Component</tt> which is the user interface equivalent
     * of this <tt>CallPeerRenderer</tt>
     */
    public Component getComponent();
}
