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

import net.java.sip.communicator.service.gui.call.*;

/**
 * The <tt>CallRenderer</tt> represents a renderer for a call. All user
 * interfaces representing a call should implement this interface.
 *
 * @author Yana Stamcheva
 */
public interface SwingCallRenderer
    extends CallRenderer
{
    /**
     * Returns the parent, container which created this <tt>CallRenderer</tt>
     * and in which this <tt>CallRenderer</tt> is added
     *
     * @return the parent, container which created this <tt>CallRenderer</tt>
     * and in which this <tt>CallRenderer</tt> is added
     */
    public CallPanel getCallContainer();
}
