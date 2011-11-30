/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.globalshortcut;

import java.util.*;
import java.util.List; // disambiguation

import java.awt.*;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;

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

            if(keyStroke == null)
            {
                return;
            }

            if(keystrokes != null)
            {
                if(keyboardHook.registerShortcut(keyStroke.getKeyCode(),
                    getModifiers(keyStroke)))
                {
                    if(add)
                        keystrokes.add(keyStroke);
                }
            }
            else
            {
                keystrokes = new ArrayList<AWTKeyStroke>();
                if(keyboardHook.registerShortcut(keyStroke.getKeyCode(),
                    getModifiers(keyStroke)))
                {
                    if(add)
                        keystrokes.add(keyStroke);
                }
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
                    if(l.getKeyCode() == keycode &&
                        l.getModifiers() == modifiers)
                        ks = l;
                }

                keyboardHook.unregisterShortcut(keyStroke.getKeyCode(),
                    getModifiers(keyStroke));

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
                        mapActions.put(listener, keystrokes);
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

        for(Map.Entry<GlobalShortcutListener, List<AWTKeyStroke>> entry :
            mapActions.entrySet())
        {
            GlobalShortcutListener l = entry.getKey();
            for(AWTKeyStroke e : entry.getValue())
            {
                unregisterShortcut(l, e);
            }
        }

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
     */
    public synchronized void receiveKey(int keycode, int modifiers)
    {
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
                        getModifiers(l) == modifiers)
                    {
                        // notify corresponding listeners
                        GlobalShortcutEvent evt = new GlobalShortcutEvent(l);
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
                entry.getKey().equals("hangup"))
            {
                for(AWTKeyStroke e : entry.getValue())
                {
                    registerShortcut(callShortcut, e);
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
