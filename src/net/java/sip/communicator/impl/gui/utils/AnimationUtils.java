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
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;

import org.jvnet.lafwidget.animation.*;

public class AnimationUtils
{
    public AnimationUtils()
    {
        FadeConfigurationManager
            .addGlobalFadeTrackerCallback(new AnimationTrackerCallBack());
    }

    public static final FadeKind SLIDE_ANIMATION
        = new FadeKind("SLIDE_ANIMATION", true);

    private static class AnimationTrackerCallBack
        implements GlobalFadeTrackerCallback
    {
        public void fadeEnded(Component arg0, Comparable<?> arg1, FadeKind arg2)
        {
        }

        public void fadePerformed(Component arg0, Comparable<?> arg1,
            FadeKind arg2, float arg3)
        {
        }

        public void fadeReversed(Component arg0, Comparable<?> arg1,
            FadeKind arg2, boolean arg3, float arg4)
        {
        }

        public void fadeStarted(Component arg0, Comparable<?> arg1,
            FadeKind arg2, float arg3)
        {
        }
    }
}
