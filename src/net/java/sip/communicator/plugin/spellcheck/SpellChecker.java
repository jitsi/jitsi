/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.spellcheck;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.Logger;

import org.dts.spell.dictionary.*;
import org.osgi.framework.*;

/**
 * Model for spell checking capabilities. This allows for the on-demand
 * retrieval of dictionaries in other languages which are cached with the user's
 * configurations.
 * 
 * @author Damian Johnson
 */
class SpellChecker
    implements ChatListener
{
    private static final Logger logger = Logger.getLogger(SpellChecker.class);

    private static final String LOCALE_CONFIG_PARAM =
        "net.java.sip.communicator.plugin.spellchecker.LOCALE";

    // default bundled dictionary
    private static final String DEFAULT_DICT_PATH =
        "/resources/config/spellcheck/";

    // location where dictionaries are stored
    private static final String DICT_DIR = "spellingDictionaries/";

    // filename of custom dictionary (added words)
    private static final String PERSONAL_DICT_NAME = "custom.per";

    /*-
     * Dictionary resources.
     * Note: Dictionary needs to be created with input streams that AREN'T CLOSED
     * (dictionaries will handle it). Closing the stream or creating with a file
     * will cause an internal NullPointerException in the spell checker.
     */
    private File personalDictLocation;

    private File dictLocation;

    private SpellDictionary dict;

    private Parameters.Locale locale; // dictionary locale

    // chat instances the spell checker is currently attached to
    private ArrayList<ChatAttachments> attachedChats =
        new ArrayList<ChatAttachments>();

    private boolean isEnabled = true;

    /**
     * Associates spell checking capabilities with all chats. This doesn't do
     * anything if this is already running.
     * 
     * @param bc execution context of the bundle
     */
    synchronized void start(BundleContext bc) throws Exception
    {
        isEnabled = SpellCheckActivator.getConfigService()
        .getBoolean("plugin.spellcheck.ENABLE", true);
        
        FileAccessService faService =
            SpellCheckActivator.getFileAccessService();

        // checks if DICT_DIR exists to see if this is the first run
        File dictionaryDir = faService.getPrivatePersistentFile(DICT_DIR);

        if (!dictionaryDir.exists())
        {
            dictionaryDir.mkdir();

            // copy default dictionaries so they don't need to be downloaded
            @SuppressWarnings ("unchecked")
            Enumeration<URL> dictUrls
                = SpellCheckActivator.bundleContext.getBundle()
                    .findEntries(DEFAULT_DICT_PATH,
                                "*.zip",
                                false);

            if (dictUrls != null)
            {
                while (dictUrls.hasMoreElements())
                {
                    URL dictUrl = dictUrls.nextElement();

                    InputStream source = dictUrl.openStream();

                    int filenameStart = dictUrl.getPath().lastIndexOf('/') + 1;
                    String filename = dictUrl.getPath().substring(filenameStart);

                    File dictLocation =
                        faService.getPrivatePersistentFile(DICT_DIR + filename);

                    copyDictionary(source, dictLocation);
                }
            }
        }

        // gets resource for personal dictionary
        this.personalDictLocation =
            faService.getPrivatePersistentFile(DICT_DIR + PERSONAL_DICT_NAME);

        if (!personalDictLocation.exists())
            personalDictLocation.createNewFile();

        // gets dictionary locale
        String localeIso =
            SpellCheckActivator.getConfigService().getString(
                LOCALE_CONFIG_PARAM);

        if (localeIso == null)
        {
            // sets locale to be the default
            localeIso = Parameters.getDefault(Parameters.Default.LOCALE);
            if (localeIso == null)
                throw new Exception(
                    "No default locale provided for spell checker");
        }

        Parameters.Locale tmp = Parameters.getLocale(localeIso);
        if (tmp == null)
            throw new Exception("No dictionary resources defined for locale: "
                + localeIso);
        this.locale = tmp; // needed for synchronization lock
        setLocale(tmp); // initializes dictionary and saves locale config

        // attaches to uiService so this'll be attached to future chats
        synchronized (this.attachedChats)
        {
            SpellCheckActivator.getUIService().addChatListener(this);
            for (Chat chat : SpellCheckActivator.getUIService().getAllChats())
            {
                ChatAttachments wrapper = new ChatAttachments(chat, this.dict);

                wrapper.attachListeners();
                this.attachedChats.add(wrapper);
            }
        }

        if (logger.isInfoEnabled())
            logger.info("Spell Checker loaded.");
    }

    /**
     * Removes the spell checking capabilities from all chats. This doesn't do
     * anything if this isn't running.
     */
    synchronized void stop()
    {
        // removes spell checker listeners from chats
        synchronized (this.attachedChats)
        {
            for (ChatAttachments chat : this.attachedChats)
            {
                chat.detachListeners();
            }
            this.attachedChats.clear();
            SpellCheckActivator.getUIService().removeChatListener(this);
        }
    }

    // attaches listeners to new chats
    public void chatCreated(Chat chat)
    {
        synchronized (this.attachedChats)
        {
            ChatAttachments wrapper = new ChatAttachments(chat, this.dict);
            wrapper.setEnabled(this.isEnabled);
            wrapper.attachListeners();

            this.attachedChats.add(wrapper);
        }
    }

    /**
     * Provides the user's list of words to be ignored by the spell checker.
     * 
     * @return user's word list
     */
    ArrayList<String> getPersonalWords()
    {
        synchronized (this.personalDictLocation)
        {
            try
            {
                // Retrieves contents of the custom dictionary
                ArrayList<String> customWords = new ArrayList<String>();
                Scanner customDictScanner =
                    new Scanner(this.personalDictLocation);
                while (customDictScanner.hasNextLine())
                {
                    customWords.add(customDictScanner.nextLine());
                }
                customDictScanner.close();
                return customWords;
            }
            catch (FileNotFoundException exc)
            {
                logger.error("Unable to read custom dictionary", exc);
                return new ArrayList<String>();
            }
        }
    }

    /**
     * Writes custom dictionary and updates spell checker to utilize new
     * listing.
     * 
     * @param words words to be ignored by the spell checker
     */
    void setPersonalWords(List<String> words)
    {
        synchronized (this.personalDictLocation)
        {
            try
            {
                // writes new word list
                BufferedWriter writer =
                    new BufferedWriter(
                        new FileWriter(this.personalDictLocation));

                for (String customWord : words)
                {
                    writer.append(customWord);
                    writer.newLine();
                }
                writer.flush();
                writer.close();

                // resets dictionary being used to include changes
                synchronized (this.attachedChats)
                {
                    InputStream dictInput =
                        new FileInputStream(this.dictLocation);
                    this.dict =
                        new OpenOfficeSpellDictionary(dictInput,
                            this.personalDictLocation);

                    // updates chats
                    for (ChatAttachments chat : this.attachedChats)
                    {
                        chat.setDictionary(this.dict);
                    }
                }
            }
            catch (IOException exc)
            {
                logger.error("Unable to access personal spelling dictionary",
                    exc);
            }
        }
    }

    /**
     * Provides the locale of the dictionary currently being used by the spell
     * checker.
     * 
     * @return locale of current dictionary
     */
    Parameters.Locale getLocale()
    {
        synchronized (this.locale)
        {
            return this.locale;
        }
    }

    /**
     * Resets spell checker to use a different locale's dictionary. This uses
     * the local copy of the dictionary if available, otherwise it's downloaded
     * and saved for future use.
     * 
     * @param locale locale of dictionary to be used
     * @throws Exception problem occurring in utilizing locale's dictionary
     */
    void setLocale(Parameters.Locale locale) throws Exception
    {
        synchronized (this.locale)
        {
            String path = locale.getDictUrl().getFile();

            int filenameStart = path.lastIndexOf('/') + 1;
            String filename = path.substring(filenameStart);

            File dictLocation =
                SpellCheckActivator.getFileAccessService()
                    .getPrivatePersistentFile(DICT_DIR + filename);

            // downloads dictionary if unavailable (not cached)
            if (!dictLocation.exists())
                copyDictionary(locale.getDictUrl().openStream(), dictLocation);

            // resets dictionary being used to include changes
            synchronized (this.attachedChats)
            {
                InputStream dictInput = new FileInputStream(dictLocation);

                SpellDictionary dict =
                    new OpenOfficeSpellDictionary(dictInput,
                        this.personalDictLocation);

                this.dict = dict;
                this.dictLocation = dictLocation;
                this.locale = locale;

                // saves locale choice to configuration properties
                SpellCheckActivator.getConfigService().setProperty(
                    LOCALE_CONFIG_PARAM, locale.getIsoCode());

                // updates chats
                for (ChatAttachments chat : this.attachedChats)
                {
                    chat.setDictionary(this.dict);
                }
            }
        }
    }

    /**
     * Removes the dictionary from the system, and sets the default locale
     * dictionary as the current dictionary
     * 
     * @param locale locale to be removed
     */
    void removeLocale(Parameters.Locale locale) throws Exception
    {
        synchronized (this.locale)
        {
            String path = locale.getDictUrl().getFile();

            int filenameStart = path.lastIndexOf('/') + 1;
            String filename = path.substring(filenameStart);

            File dictLocation =
                SpellCheckActivator.getFileAccessService()
                    .getPrivatePersistentFile(DICT_DIR + filename);

            if (dictLocation.exists())
                dictLocation.delete();

            String localeIso = Parameters.getDefault(Parameters.Default.LOCALE);
            Parameters.Locale loc = Parameters.getLocale(localeIso);
            setLocale(loc);
        }
    }
    
    /**
     * Determines if locale's dictionary is locally available or not.
     * 
     * @param locale locale to be checked
     * @return true if local resources for dictionary are available and
     *         accessible, false otherwise
     */
    boolean isLocaleAvailable(Parameters.Locale locale)
    {
        String path = locale.getDictUrl().getFile();
        int filenameStart = path.lastIndexOf('/') + 1;
        String filename = path.substring(filenameStart);
        try
        {
            File dictLocation =
                SpellCheckActivator.getFileAccessService()
                    .getPrivatePersistentFile(DICT_DIR + filename);

            return dictLocation.exists();
        }
        catch (Exception exc)
        {
            return false;
        }
    }

    boolean isEnabled()
    {
        synchronized (this.attachedChats)
        {
            return this.isEnabled;
        }
    }

    void setEnabled(boolean enable)
    {
        if (enable != this.isEnabled)
        {
            synchronized (this.attachedChats)
            {
                this.isEnabled = enable;
                for (ChatAttachments chatAttachment : this.attachedChats)
                {
                    chatAttachment.setEnabled(enable);
                }
            }
        }
    }

    // copies dictionary to appropriate location, closing the stream afterward
    private void copyDictionary(InputStream input, File dest)
        throws IOException,
        FileNotFoundException
    {
        byte[] buf = new byte[1024];
        FileOutputStream output = new FileOutputStream(dest);

        int len;
        while ((len = input.read(buf)) > 0)
        {
            output.write(buf, 0, len);
        }

        input.close();
        output.close();
    }

    /**
     * Determines if spell checker dictionary works. Backend API often fails
     * when used so this tests that the current dictionary is able to process
     * words.
     * 
     * @return true if current dictionary can check words, false otherwise
     */
    private boolean isDictionaryValid(SpellDictionary dict)
    {
        try
        {
            // spell checker API often works until used
            dict.isCorrect("foo");
            return true;
        }
        catch (Exception exc)
        {
            logger.error("Dictionary validation failed", exc);
            return false;
        }
    }
}