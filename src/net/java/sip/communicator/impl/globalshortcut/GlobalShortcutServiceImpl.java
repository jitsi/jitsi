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
package net.java.sip.communicator.impl.globalshortcut;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;
// disambiguation

/**
 * This global shortcut service permits to register listeners for global
 * shortcut (i.e. keystroke even if application is not foreground).
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutServiceImpl
    implements GlobalShortcutService,
               NativeKeyboardHookDelegate
{
    /**
     * The <tt>Logger</tt> used by the <tt>GlobalShortcutServiceImpl</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(
        GlobalShortcutServiceImpl.class);

    /**
     * List of action and its corresponding shortcut.
     */
    private final Map<GlobalShortcutListener, List<AWTKeyStroke>> mapActions =
        new HashMap<GlobalShortcutListener, List<AWTKeyStroke>>();

    /**
     * List of notifiers when special key detection is enabled.
     */
    private final List<GlobalShortcutListener> specialKeyNotifiers =
        new ArrayList<GlobalShortcutListener>();
    /**
     * If the service is running or not.
     */
    private boolean isRunning = false;

    /**
     * The <tt>NativeKeyboardHook</tt> that will notify us key press event.
     */
    private NativeKeyboardHook keyboardHook = new NativeKeyboardHook();

    /**
     * Call shortcut to answer/hang up a call.
     */
    private final CallShortcut callShortcut = new CallShortcut();

    /**
     * UI shortcut to display GUI.
     */
    private final UIShortcut uiShortcut = new UIShortcut();

    /**
     * Last special key detected.
     */
    private AWTKeyStroke specialKeyDetected = null;

    /**
     * Object to synchronize special key detection.
     */
    private final Object specialKeySyncRoot = new Object();

    /**
     * Initializes the <tt>GlobalShortcutServiceImpl</tt>.
     */
    public GlobalShortcutServiceImpl()
    {
    }

    /**
     * Registers an action to execute when the keystroke is typed.
     *
     * @param listener listener to notify when keystroke is typed
     * @param keyStroke keystroke that will trigger the action
     */
    public void registerShortcut(GlobalShortcutListener listener,
        AWTKeyStroke keyStroke)
    {
        registerShortcut(listener, keyStroke, true);
    }

    /**
     * Registers an action to execute when the keystroke is typed.
     *
     * @param listener listener to notify when keystroke is typed
     * @param keyStroke keystroke that will trigger the action
     * @param add add the listener/keystrokes to map
     */
    public void registerShortcut(GlobalShortcutListener listener,
        AWTKeyStroke keyStroke, boolean add)
    {
        synchronized(mapActions)
        {
            List<AWTKeyStroke> keystrokes = mapActions.get(listener);
            boolean ok = false;

            if(keyStroke == null)
            {
                return;
            }

            if(keystrokes == null)
            {
                keystrokes = new ArrayList<AWTKeyStroke>();
            }

            if(keyStroke.getModifiers() != SPECIAL_KEY_MODIFIERS)
            {
                ok = keyboardHook.registerShortcut(keyStroke.getKeyCode(),
                    getModifiers(keyStroke), keyStroke.isOnKeyRelease());
            }
            else
            {
                ok = keyboardHook.registerSpecial(keyStroke.getKeyCode(),
                    keyStroke.isOnKeyRelease());
            }
            if(ok && add)
            {
                keystrokes.add(keyStroke);
            }


            if(add)
                mapActions.put(listener, keystrokes);
        }
    }

    /**
     * Unregisters an action to execute when the keystroke is typed.
     *
     * @param listener listener to remove
     * @param keyStroke keystroke that will trigger the action
     */
    public void unregisterShortcut(GlobalShortcutListener listener,
        AWTKeyStroke keyStroke)
    {
        unregisterShortcut(listener, keyStroke, true);
    }

    /**
     * Unregisters an action to execute when the keystroke is typed.
     *
     * @param listener listener to remove
     * @param keyStroke keystroke that will trigger the action
     * @param remove remove or not entry in the map
     */
    public void unregisterShortcut(GlobalShortcutListener listener,
        AWTKeyStroke keyStroke, boolean remove)
    {
        synchronized(mapActions)
        {
            List<AWTKeyStroke> keystrokes = mapActions.get(listener);

            if(keystrokes != null && keyStroke != null)
            {
                int keycode = keyStroke.getKeyCode();
                int modifiers = keyStroke.getModifiers();
                AWTKeyStroke ks = null;

                for(AWTKeyStroke l : keystrokes)
                {
                    if(l.getKeyCode() == keycode
                            && l.getModifiers() == modifiers)
                    {
                        ks = l;
                    }
                }

                if(modifiers != SPECIAL_KEY_MODIFIERS)
                {
                    keyboardHook.unregisterShortcut(
                            keyStroke.getKeyCode(),
                            getModifiers(keyStroke));
                }
                else
                {
                    keyboardHook.unregisterSpecial(keyStroke.getKeyCode());
                }

                if(remove)
                {
                    if(ks != null)
                    {
                        keystrokes.remove(ks);
                    }

                    if(keystrokes.size() == 0)
                    {
                        mapActions.remove(listener);
                    }
                    else
                    {
                        // We do not have to put keystrokes back into mapActions
                        // because keystrokes is a reference to a modifiable
                        // List.
                    }
                }
            }
        }
    }

    /**
     * Start the service.
     */
    public void start()
    {
        if(!isRunning)
        {
            keyboardHook.setDelegate(this);
            keyboardHook.start();
            isRunning = true;
        }
    }

    /**
     * Stop the service.
     */
    public void stop()
    {
        isRunning = false;

        // FIXME Lyubomir Marinov: The method unregisterShortcut will cause a
        // ConcurrentModificationException because of either mapActions or a
        // List<AWTKeyStroke> value. The method stop was never invoked before
        // though because of a bug in the methods start and stop of the class
        // GlobalShortcutActivator.
//        for(Map.Entry<GlobalShortcutListener, List<AWTKeyStroke>> entry
//                : mapActions.entrySet())
//        {
//            GlobalShortcutListener l = entry.getKey();
//            for(AWTKeyStroke e : entry.getValue())
//            {
//                unregisterShortcut(l, e);
//            }
//        }

        if(keyboardHook != null)
        {
            keyboardHook.setDelegate(null);
            keyboardHook.stop();
        }
    }

    /**
     * Receive a key press event.
     *
     * @param keycode keycode received
     * @param modifiers modifiers received (ALT or CTRL + letter, ...)
     * @param isOnKeyRelease this parameter is true if the shortcut is released
     */
    public synchronized void receiveKey(int keycode, int modifiers,
        boolean onRelease)
    {
        if(keyboardHook.isSpecialKeyDetection())
        {
            specialKeyDetected = AWTKeyStroke.getAWTKeyStroke(keycode,
                modifiers);

            synchronized(specialKeySyncRoot)
            {
                specialKeySyncRoot.notify();
            }

            GlobalShortcutEvent evt = new GlobalShortcutEvent(
                specialKeyDetected, onRelease);
            List<GlobalShortcutListener> copyListeners =
                new ArrayList<GlobalShortcutListener>(specialKeyNotifiers);

            for(GlobalShortcutListener l : copyListeners)
            {
                l.shortcutReceived(evt);
            }

            // if special key detection is enabled, disable all other shortcuts
            return;
        }
        synchronized(mapActions)
        {
            // compare keycode/modifiers to keystroke
            for(Map.Entry<GlobalShortcutListener, List<AWTKeyStroke>> entry :
                mapActions.entrySet())
            {
                List<AWTKeyStroke> lst = entry.getValue();

                for(AWTKeyStroke l : lst)
                {
                    if(l.getKeyCode() == keycode &&
                        (getModifiers(l) == modifiers ||
                            (modifiers == SPECIAL_KEY_MODIFIERS &&
                            l.getModifiers() == modifiers)))
                    {
                        // notify corresponding listeners
                        GlobalShortcutEvent evt = new GlobalShortcutEvent(
                            l, onRelease);
                        entry.getKey().shortcutReceived(evt);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Get our user-defined modifiers.
     *
     * @param keystroke keystroke
     * @return user-defined modifiers
     */
    private static int getModifiers(AWTKeyStroke keystroke)
    {
        int modifiers = keystroke.getModifiers();
        int ret = 0;

        if((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) > 0)
        {
            ret |= NativeKeyboardHookDelegate.MODIFIERS_CTRL;
        }
        if((modifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) > 0)
        {
            ret |= NativeKeyboardHookDelegate.MODIFIERS_ALT;
        }
        if((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) > 0)
        {
            ret |= NativeKeyboardHookDelegate.MODIFIERS_SHIFT;
        }
        if((modifiers & java.awt.event.InputEvent.META_DOWN_MASK) > 0)
        {
            ret |= NativeKeyboardHookDelegate.MODIFIERS_LOGO;
        }

        return ret;
    }

    /**
     * Reload global shortcuts.
     */
    public synchronized void reloadGlobalShortcuts()
    {
        // unregister all shortcuts
        GlobalKeybindingSet set =
            GlobalShortcutActivator.getKeybindingsService().getGlobalBindings();

        for(Map.Entry<GlobalShortcutListener, List<AWTKeyStroke>> entry :
            mapActions.entrySet())
        {
            GlobalShortcutListener l = entry.getKey();
            for(AWTKeyStroke e : entry.getValue())
            {
                unregisterShortcut(l, e, false);
            }
        }
        mapActions.clear();

        // add shortcuts from configuration
        for(Map.Entry<String, List<AWTKeyStroke>> entry :
            set.getBindings().entrySet())
        {
            if(entry.getKey().equals("answer") ||
                entry.getKey().equals("hangup") ||
                entry.getKey().equals("answer_hangup") ||
                entry.getKey().equals("mute") ||
                entry.getKey().equals("push_to_talk"))
            {
                for(AWTKeyStroke e : entry.getValue())
                {
                    if(entry.getKey().equals("push_to_talk"))
                    {
                        if(e != null)
                            registerShortcut(callShortcut,
                                AWTKeyStroke.getAWTKeyStroke(
                                    e.getKeyCode(), e.getModifiers(), true));
                    }
                    else
                    {
                        registerShortcut(callShortcut, e);
                    }

                }
            }
            else if(entry.getKey().equals("contactlist"))
            {
                for(AWTKeyStroke e : entry.getValue())
                {
                    registerShortcut(uiShortcut, e);
                }
            }
        }
    }

    /**
     * Returns CallShortcut object.
     *
     * @return CallShortcut object
     */
    public CallShortcut getCallShortcut()
    {
        return callShortcut;
    }

    /**
     * Returns UIShortcut object.
     *
     * @return UIShortcut object
     */
    public UIShortcut getUIShortcut()
    {
        return uiShortcut;
    }

    /**
     * Enable or not global shortcut.
     *
     * @param enable enable or not global shortcut
     */
    public void setEnable(boolean enable)
    {
        if(mapActions.size() > 0)
        {
            if(enable)
            {
                for(Map.Entry<GlobalShortcutListener, List<AWTKeyStroke>> entry
                    : mapActions.entrySet())
                {
                    GlobalShortcutListener l = entry.getKey();
                    for(AWTKeyStroke e : entry.getValue())
                    {
                        registerShortcut(l, e, false);
                    }
                }
            }
            else
            {
                for(Map.Entry<GlobalShortcutListener, List<AWTKeyStroke>> entry
                    : mapActions.entrySet())
                {
                    GlobalShortcutListener l = entry.getKey();
                    for(AWTKeyStroke e : entry.getValue())
                    {
                        unregisterShortcut(l, e, false);
                    }
                }
            }
        }
    }

    /**
     * Enable or disable special key detection.
     *
     * @param enable enable or not special key detection.
     * @param callback object to be notified
     */
    public synchronized void setSpecialKeyDetection(boolean enable,
        GlobalShortcutListener callback)
    {
        keyboardHook.detectSpecialKeyPress(enable);

        if(specialKeyNotifiers.contains(callback) == enable)
        {
            return;
        }

        if(enable)
        {
            specialKeyNotifiers.add(callback);
        }
        else
        {
            specialKeyNotifiers.remove(callback);
        }
    }

    /**
     * Get special keystroke or null if not supported or user cancels. If no
     * special key is detected for 5 seconds, it returns null
     *
     * @return special keystroke or null if not supported or user cancels
     */
    public AWTKeyStroke getSpecialKey()
    {
        AWTKeyStroke ret = null;

        specialKeyDetected = null;
        keyboardHook.detectSpecialKeyPress(true);

        // Windows only for the moment
        if(OSUtils.IS_WINDOWS)
        {
            synchronized(specialKeySyncRoot)
            {
                try
                {
                    specialKeySyncRoot.wait(5000);
                }
                catch(InterruptedException e)
                {
                }
            }

            ret = specialKeyDetected;
            specialKeyDetected = null;
        }

        keyboardHook.detectSpecialKeyPress(false);
        return ret;
    }

    /**
     * Simple test.
     */
    public void test()
    {
        GlobalShortcutListener l = new GlobalShortcutListener()
        {
            public void shortcutReceived(GlobalShortcutEvent evt)
            {
                System.out.println("global shortcut event");
            }
        };

        AWTKeyStroke ks = AWTKeyStroke.getAWTKeyStroke("control B");
        AWTKeyStroke ks2 = AWTKeyStroke.getAWTKeyStroke("control E");

        if(ks == null)
        {
            logger.info("Failed to register keystroke");
            System.out.println("failed to register keystroke");
            return;
        }

        this.registerShortcut(l, ks);
        this.registerShortcut(l, ks2);
        try{Thread.sleep(30000);}catch(InterruptedException e){}
        this.unregisterShortcut(l, ks);
        try{Thread.sleep(5000);}catch(InterruptedException e){}
        this.unregisterShortcut(l, ks2);

        /*
        boolean ret = keyboardHook.registerShortcut(ks.getKeyCode(),
            getModifiers(ks));
        System.out.println("finally " + ret);

        System.out.println("registered");
        try{Thread.sleep(30000);}catch(InterruptedException e){}
        System.out.println("unregistered1");
        keyboardHook.unregisterShortcut(ks.getKeyCode(),
            getModifiers(ks));
        System.out.println("unregistered2");
         */
    }
}
