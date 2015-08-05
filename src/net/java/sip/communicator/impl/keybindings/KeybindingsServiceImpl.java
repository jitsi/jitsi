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
package net.java.sip.communicator.impl.keybindings;

import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;
//disambiguation

/**
 * Service that concerns keybinding mappings used by various parts of the UI.
 * Persistence is handled as follows when started:
 * <ol>
 * <li>Load default bindings from relative directory</li>
 * <li>Attempt to load custom bindings and resolve any duplicates</li>
 * <li>If merged bindings differ from the custom bindings then this attempts to
 * save the merged version</li>
 * </ol>
 * Custom bindings attempt to be written again whenever they're changed if the
 * service is running. Each category of keybindings are stored in its own file.
 *
 * @author Damian Johnson
 */
class KeybindingsServiceImpl
    implements KeybindingsService, Observer
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>KeybindingsServiceImpl</tt> class and its instances for logging
     * output.
     */
    private static final Logger logger =
        Logger.getLogger(KeybindingsServiceImpl.class);

    /**
     * Name of the relative directory that holds default bindings.
     */
    private static final String DEFAULT_KEYBINDING_DIR =
        "/resources/config/defaultkeybindings";

    /**
     * Name of the directory that holds custom bindings.
     */
    private static final String CUSTOM_KEYBINDING_DIR = "keybindings";

    /**
     * Path where to store the global shortcuts.
     */
    private final static String CONFIGURATION_PATH =
        "net.java.sip.communicator.impl.keybinding.global";

    /**
     * Path where to store the global shortcuts defaults values.
     */
    private final static String DEFAULTS_VALUES_PATH =
        "impl.keybinding.global";

    /**
     * Flag indicating if service is running.
     */
    private boolean isRunning = false;

    /**
     * Loaded keybinding mappings, maps to null if defaults failed to be loaded.
     */
    private final HashMap<KeybindingSet.Category, KeybindingSetImpl> bindings =
        new HashMap<KeybindingSet.Category, KeybindingSetImpl>();

    /**
     * Loaded global keybinding mappings.
     */
    private GlobalKeybindingSet globalBindings = null;

    /**
     * Starts the KeybindingService, for each keybinding category retrieving the
     * default bindings then overwriting them with any custom bindings that can
     * be retrieved. This writes the merged copy back if it differs from the
     * custom bindings. This is a no-op if the service has already been started.
     *
     * @param bc the currently valid OSGI bundle context.
     */
    synchronized void start(BundleContext bc)
    {
        if (this.isRunning)
            return;
        for (KeybindingSet.Category category : KeybindingSet.Category.values())
        {
            // Retrieves default bindings
            Persistence format = category.getFormat();
            LinkedHashMap<KeyStroke, String> defaultBindings;
            try
            {
                String defaultPath =
                    DEFAULT_KEYBINDING_DIR + "/" + category.getResource();
                InputStream in = getClass().getResourceAsStream(defaultPath);
                defaultBindings = format.load(in);
            }
            catch (IOException exc)
            {
                logger.error("default bindings set missing: "
                    + category.getResource(), exc);
                this.bindings.put(category, null);
                continue;
            }
            catch (ParseException exc)
            {
                logger.error("unable to parse default bindings set: "
                    + category.getResource(), exc);
                this.bindings.put(category, null);
                continue;
            }

            // Attempts to retrieve custom bindings
            String customPath =
                CUSTOM_KEYBINDING_DIR + "/" + category.getResource();
            File customFile;
            try
            {
                ServiceReference faServiceReference =
                    bc.getServiceReference(FileAccessService.class.getName());
                FileAccessService faService =
                    (FileAccessService) bc.getService(faServiceReference);

                // Makes directory for custom bindings if it doesn't exist
                File customDir =
                    faService
                        .getPrivatePersistentDirectory(CUSTOM_KEYBINDING_DIR,
                            FileCategory.PROFILE);
                if (!customDir.exists())
                    customDir.mkdir();

                // Gets file access service to reference persistent storage
                // of the user
                customFile = faService.getPrivatePersistentFile(customPath,
                    FileCategory.PROFILE);
            }
            catch (Exception exc)
            {
                String msg =
                    "unable to secure file for custom bindings (" + customPath
                        + "), using defaults but won't be able to save changes";
                logger.error(msg, exc);
                KeybindingSetImpl newSet =
                    new KeybindingSetImpl(defaultBindings, category, null);
                this.bindings.put(category, newSet);
                newSet.addObserver(this);
                continue;
            }

            LinkedHashMap<KeyStroke, String> customBindings = null;
            if (customFile.exists())
            {
                try
                {
                    FileInputStream in = new FileInputStream(customFile);
                    customBindings = format.load(in);
                    in.close();
                }
                catch (Exception exc)
                {
                    // If either an IO or ParseException occur then we skip
                    // loading custom bindings
                }
            }

            // Merges custom bindings
            LinkedHashMap<KeyStroke, String> merged =
                new LinkedHashMap<KeyStroke, String>();
            if (customBindings != null)
            {
                Map<KeyStroke, String> customTmp =
                    new LinkedHashMap<KeyStroke, String>(customBindings);

                for (Map.Entry<KeyStroke, String> shortcut2action :
                    defaultBindings.entrySet())
                {
                    String action = shortcut2action.getValue();

                    if (customTmp.containsValue(action))
                    {
                        KeyStroke custom = null;
                        for(Map.Entry<KeyStroke, String> customShortcut2action :
                            customTmp.entrySet())
                        {
                            if (customShortcut2action.getValue().equals(action))
                            {
                                custom = customShortcut2action.getKey();
                                break;
                            }
                        }

                        assert custom != null;
                        customTmp.remove(custom);
                        merged.put(custom, action);
                    }
                    else
                    {
                        merged.put(shortcut2action.getKey(), action);
                    }
                }
            }
            else
            {
                merged = defaultBindings;
            }

            // Writes merged result
            if (!merged.equals(customBindings))
            {
                try
                {
                    FileOutputStream out = new FileOutputStream(customFile);
                    format.save(out, merged);
                    out.close();
                }
                catch (IOException exc)
                {
                    logger.error("unable to write to: "
                        + customFile.getAbsolutePath(), exc);
                }
            }

            KeybindingSetImpl newSet =
                new KeybindingSetImpl(merged, category, customFile);
            this.bindings.put(category, newSet);
            newSet.addObserver(this);
        }

        // global shortcut initialization
        globalBindings = new GlobalKeybindingSetImpl();
        globalBindings.setBindings(getGlobalShortcutFromConfiguration());

        this.isRunning = true;
    }

    /**
     * Invalidates references to custom bindings, preventing further writes.
     */
    synchronized void stop()
    {
        if (!this.isRunning)
            return;
        for (KeybindingSetImpl bindingSet : this.bindings.values())
        {
            bindingSet.invalidate();
        }
        this.bindings.clear();
        this.saveGlobalShortcutFromConfiguration();
        this.isRunning = false;
    }

    /**
     * Provides the bindings associated with a given category. This may be null
     * if the default bindings failed to be loaded.
     *
     * @param category segment of the UI for which bindings should be retrieved
     * @return mappings of keystrokes to the string representation of their
     *         actions
     * @throws UnsupportedOperationException if the service isn't running
     */
    public synchronized KeybindingSet getBindings(
        KeybindingSet.Category category)
    {
        if (!this.isRunning)
            throw new UnsupportedOperationException();

        // Started services should have all categories
        assert this.bindings.containsKey(category);
        return this.bindings.get(category);
    }

    /**
     * Listens for changes in binding sets so changes can be written.
     */
    public void update(Observable obs, Object arg)
    {
        if (obs instanceof KeybindingSetImpl)
        {
            KeybindingSetImpl changedBindings = (KeybindingSetImpl) obs;

            // Attempts to avoid lock if unwritable (this works since bindings
            // can't become re-writable)
            if (changedBindings.isWritable())
            {
                synchronized (this)
                {
                    if (changedBindings.isWritable())
                    {
                        // Writes new bindings to custom file
                        File customFile = changedBindings.getCustomFile();

                        try
                        {
                            FileOutputStream out =
                                new FileOutputStream(customFile);
                            Persistence format =
                                changedBindings.getCategory().getFormat();
                            format.save(out, changedBindings.getBindings());
                            out.close();
                        }
                        catch (IOException exc)
                        {
                            logger.error("unable to write to: "
                                + customFile.getAbsolutePath(), exc);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns list of global shortcuts from the configuration file.
     *
     * @return list of global shortcuts.
     */
    public Map<String, List<AWTKeyStroke>> getGlobalShortcutFromConfiguration()
    {
        Map<String, List<AWTKeyStroke>> gBindings = new
            LinkedHashMap<String, List<AWTKeyStroke>>();
        ConfigurationService configService =
            KeybindingsActivator.getConfigService();
        String shortcut = null;
        String shortcut2 = null;
        String propName = null;
        String propName2 = null;
        String names[] = new String[]{"answer", "hangup", "answer_hangup",
            "contactlist", "mute", "push_to_talk"};
        Object configured = configService.getProperty(
            "net.java.sip.communicator.impl.keybinding.global.configured");

        if(configured == null)
        {
            // default keystrokes
            for(String name : names)
            {
                List<AWTKeyStroke> kss = new ArrayList<AWTKeyStroke>();
                propName = DEFAULTS_VALUES_PATH + "." + name + ".1";

                shortcut = propName != null ?
                    KeybindingsActivator.getResourceService().getSettingsString(
                        propName) : null;

                if(shortcut != null)
                {
                    kss.add(AWTKeyStroke.getAWTKeyStroke(shortcut));
                }

                gBindings.put(name, kss);
            }

            configService.setProperty(
                "net.java.sip.communicator.impl.keybinding.global.configured",
                "true");

            return gBindings;
        }

        for(String name : names)
        {
            List<AWTKeyStroke> kss = new ArrayList<AWTKeyStroke>();

            propName = CONFIGURATION_PATH + "." + name + ".1";
            propName2 = CONFIGURATION_PATH + "." + name + ".2";

            shortcut = propName != null ?
                (String)configService.getProperty(propName) : null;
            shortcut2 = propName2 != null ?
                (String)configService.getProperty(propName2) : null;
            if(shortcut != null)
            {
                kss.add(AWTKeyStroke.getAWTKeyStroke(shortcut));
            }

            // second shortcut is always "special"
            if(shortcut2 != null)
            {
                //16367 is the combination of all possible modifiers
                //(shift ctrl meta alt altGraph button1 button2 button3 pressed)
                //it is used to distinguish special key (headset) and others
                int nb = Integer.parseInt(shortcut2);
                kss.add(AWTKeyStroke.getAWTKeyStroke(nb, 16367));
            }
            gBindings.put(name, kss);
        }
        return gBindings;
    }

    /**
     * Save the configuration file.
     */
    public void saveGlobalShortcutFromConfiguration()
    {
        ConfigurationService configService =
            KeybindingsActivator.getConfigService();
        String shortcut = null;
        String shortcut2 = null;

        for(Map.Entry<String, List<AWTKeyStroke>> entry :
            globalBindings.getBindings().entrySet())
        {
            String key = entry.getKey();
            List<AWTKeyStroke> kss = entry.getValue();
            String path = CONFIGURATION_PATH;

            path += "." + key;

            shortcut = path + ".1";
            shortcut2 = path + ".2";

            configService.setProperty(shortcut, kss.size() > 0 ?
                kss.get(0) : null);
            // second shortcut is special
            configService.setProperty(shortcut2,
                (kss.size() > 1 && kss.get(1) != null) ?
                    kss.get(1).getKeyCode() : null);
        }
    }

    /**
     * Provides the bindings associated with the global category.
     *
     * @return global keybinding set
     */
    public GlobalKeybindingSet getGlobalBindings()
    {
        return globalBindings;
    }
}
