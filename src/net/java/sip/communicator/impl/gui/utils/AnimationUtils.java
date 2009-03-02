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
