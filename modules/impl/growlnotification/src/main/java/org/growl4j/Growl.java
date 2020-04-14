/*
 * growl4j, the OpenSource Java Solution for using Growl.
 * Maintained by the Jitsi community (http://jitsi.org).
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
package org.growl4j;

import java.util.*;
import java.util.logging.*;

/**
 * Growl class provides means to interact with Growl Daemon without using
 * Cocoa-Java bridge.  It is implemented using Java Native Interface, thus
 * accompanied library libgrowl.dylib needs to be present. 
 *
 * You can check if Growl Daemon is installed and running with static methods
 * isGrowlInstalled() and isGrowlRunning(). To use Growl, simply create an
 * instance of this class and use notifyGrowlOf() to send a notification. If
 * your application needs to get click feedback, you need to implement
 * GrowlCallBacksListener interface and register the listener object with Growl.
 *
 * Notes/cautions:
 * 1) If this class is used in application that uses AWT and is run in headless
 * mode, it will probably work, but you will see error messages in console
 * produced by Growl Daemon.
 * 2) You should not assume this class is thread-safe.
 *
 * @author Egidijus Jankauskas
 */
public final class Growl
{
    /* these variables are accessed directly by libgrowl.dylib */
    private final byte[] appIcon;
    private final String appID;
    private final String appName;
    private final String[] allNotifications;
    private final String[] defaultNotifications;

    /* indicates if there is a need to call getAppToFront() */
    private boolean needsFocus = true;

    /* stores the notifications that were sent to Growl */
    private final HashMap<Long, Object> shownNotifications
        = new HashMap<Long, Object>(10);

    /** The list of all added Growl callbacks listeners */
    private final List<GrowlCallbacksListener> listeners
        = new Vector<GrowlCallbacksListener>();

    static { System.loadLibrary("growl4j"); }

    /**
     * A constructor for Growl class.
     * ranges from <code>0</code> to <code>length() - 1</code>.
     *
     * @param appName               the name of the application.
     * @param appID                 the bundle ID if your application. It should
     * be unique to your app.
     * @param appIcon               the icon of the application.
     * @param allNotifications      an array of all notification types that an
     * application will use.
     * @param defaultNotifications  a subset of <code>allNotifiactions</code>
     * that are enabled by default.
     *
     * @exception IllegalArgumentException if any of the parameters is
     * <code>null</code> or <code>defaultNotifications</code> is not a subset of
     * <code>allNotifiactions</code>.
     */
    public Growl(
        String appName,
        String appID,
        byte[] appIcon,
        String[] allNotifications,
        String[] defaultNotifications)
        throws IllegalArgumentException
    {
        if (appName == null)
            throw new IllegalArgumentException("appName must be non null.");
        if (appID == null)
            throw new IllegalArgumentException("appID must be non null");
        if (appIcon == null)
            throw new IllegalArgumentException("appIcon must be non null.");
        if (allNotifications.length == 0)
            throw new IllegalArgumentException(
                "allNotifications must contain at least one element");
        if (defaultNotifications.length == 0)
            throw new IllegalArgumentException(
                "defaultNotifications must contain at least one element");

        this.appName = appName;
        this.appID = appID;
        this.appIcon = appIcon;
        this.allNotifications = allNotifications;
        this.defaultNotifications = defaultNotifications;

        // check if allNotificationsarray contains defaultNotifications array
        if (!checkNotificationTypes())
            throw new IllegalArgumentException("defaultNotifications must be a "
                + "subset of allNotifications");

        registerWithGrowlDaemon();
    }

    /**
     * Helper method to check if <code>defaultNotifications</code> is a subset
     * of <code>allNotifications</code>.
     *
     * @return true if <code>defaultNotifications</code> is a subset of
     * <code>allNotifications</code>.
     */
    private boolean checkNotificationTypes()
    {
        boolean isConsistent = true;
        for(String d: defaultNotifications)
        {
            boolean contains = false;
            for(String a: allNotifications)
            {
                if(a.equals(d))
                {
                    contains = true;
                    break;
                }
            }
            if (!contains)
            {
                isConsistent = false;
                break;
            }
        }
        return isConsistent;
    }

    /**
     * Sends Growl daemon a message with provided information.
     *
     *
     * @param msgTitle              the title of the notification.
     * @param msgBody               the body of the notification.
     * @param msgType               the type of the notifications. Must be one
     * of <code>allNotifications</code> or otherwise will be ignored.
     * @param icon                  a byte array representation of an icon to be
     * shown in notification.  If <code>null</code> is passed, default
     * application icon is shown.
     * @param ctx                   a context object for a notification. All
     * <code>GrowlCallbacksListener</code>s receive <code>ctx</code> when the
     * corresponding notification is clicked or times out.
     *
     * @exception IllegalArgumentException if <code>msgTitle</code>,
     * <code>msgBody</code>, or <code>msgType</code> is <code>null</code>.
     */
    public void notifyGrowlOf(
        String msgTitle,
        String msgBody,
        String msgType,
        byte[] icon,
        Object ctx)
        throws IllegalArgumentException
    {
        if (msgTitle == null)
            throw new IllegalArgumentException("Message title must be not null");
        if (msgBody == null)
            throw new IllegalArgumentException("Message body must be not null");
        if (msgType == null)
            throw new IllegalArgumentException("Message type must be not null");

        synchronized(this)
        {
            long timestamp = System.currentTimeMillis();
            shownNotifications.put(timestamp, ctx);
            showGrowlMessage(msgTitle, msgBody, msgType, icon, timestamp);
        }
    }

    /**
     * Adds GrowlCallBacksListener.
     *
     * @param l an object that implements <code>GrowlCallBacksListener</code>
     * interface.
     */
    public synchronized void addClickedNotificationsListener(
        GrowlCallbacksListener l)
    {
        if(!listeners.contains(l))
        {
            listeners.add(l);
        }
    }

    /**
     * Removes GrowlCallBacksListener.
     *
     * @param l an object that implements <code>GrowlCallBacksListener</code>
     * interface.
     */
    public synchronized void removeClickedNotificationsListener(
        GrowlCallbacksListener l)
    {
        listeners.remove(l);
    }

    /**
     * Informs Growl if it needs to take care of application focus when user
     * clicks on notification.
     * Default value is <code>true</code>.
     *
     * @param focus
     */
    public void takeCareOfSystemWideFocus(boolean focus)
    {
        needsFocus = focus;
    }

    /**
     * Private method that is called by libgrowl.dylib when user clicks on
     * notification.
     *
     * @param context a timestamp used to identify the notification that was
     * clicked.
     */
    private synchronized void growlNotificationWasClicked(long context)
    {
        if (shownNotifications.containsKey(context))
        {
            Object o = shownNotifications.remove(context);
            if (o != null)
            {
                informListeners(o, true);
            }
            if (needsFocus)
                getAppToFront();
        }
    }

    /**
     * Private method that is called by libgrowl.dylib when notification times
     * out.
     *
     * @param context a timestamp used to identify the notification that was
     * clicked.
     */
    private synchronized void growlNotificationTimedOut(long context)
    {
        if (shownNotifications.containsKey(context))
        {
            Object o = shownNotifications.remove(context);
            if (o != null)
            {
                informListeners(o, false);
            }
        }
    }

    /**
     * Helper method that informs all <code>GrowlCallbacksListener</code>s about
     * clicked and timed out notifications.
     *
     * @param context an object that was passed by the developer to identify
     * notification.
     * @param isClicked a <code>boolean</code> variable set to <code>true</code>
     * if notification was clicked, and to <code>false</code> otherwise.
     */
    private synchronized void informListeners(Object context, boolean isClicked)
    {
        for(GrowlCallbacksListener l: listeners)
        {
            if (isClicked)
            {
                l.growlNotificationWasClicked(context);
            } else {
                l.growlNotificationTimedOut(context);
            }
        }
    }

    /**
     * A class method that checks if Growl is running.
     *
     * @return <code>true</code> if Growl is running and <code>false</code>
     * otherwise.
     */
    public static native boolean isGrowlRunning();

    /* private native methods used in this class. */
    private native void getAppToFront();

    private native void showGrowlMessage(
        String title,
        String body,
        String type,
        byte[] icon,
        long context);

    private native void registerWithGrowlDaemon();

    public native void doFinalCleanUp();
}
