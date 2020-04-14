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
package net.java.sip.communicator.plugin.spellcheck;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.plugin.spellcheck.Parameters.Default;
import net.java.sip.communicator.plugin.spellcheck.Parameters.Locale;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Combo box providing a listing of all available locales with corresponding
 * country flags. Selecting a new field causes that locale's dictionary to be
 * downloaded, if not available. The spell checker then use the selected
 * language for further checking.
 *
 * @author Damian Johnson
 * @author Yana Stamcheva
 */
public class LanguageMenuBar
    extends SIPCommMenuBar
    implements PluginComponent,
               PropertyChangeListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    // parallel maps containing cached instances of country flags
    private static final HashMap<Parameters.Locale, ImageIcon>
        AVAILABLE_FLAGS = new HashMap<Parameters.Locale, ImageIcon>();

    private static final HashMap<Parameters.Locale, ImageIcon>
        UNAVAILABLE_FLAGS = new HashMap<Parameters.Locale, ImageIcon>();

    private static final Logger logger = Logger
        .getLogger(LanguageMenuBar.class);

    private static final ImageIcon BLANK_FLAG_ICON = Resources
        .getImage("plugin.spellcheck.BLANK_FLAG");

    private final ListCellRenderer languageSelectionRenderer;

    private final HashMap<Parameters.Locale, Boolean> localeAvailabilityCache =
        new HashMap<Parameters.Locale, Boolean>();

    private final SpellChecker spellChecker;

    private final SIPCommMenu menu = new SelectorMenu();

    private final List<Parameters.Locale> localeList
        = new ArrayList<Parameters.Locale>();

    private final SIPCommTextButton removeItem = new SIPCommTextButton(
        Resources.getString("plugin.spellcheck.UNINSTALL_DICTIONARY"));

    public final JList list;

    /**
     * The parent factory.
     */
    private final PluginComponentFactory parentFactory;

    LanguageMenuBar(SpellChecker checker, PluginComponentFactory parentFactory)
    {
        this.spellChecker = checker;
        this.spellChecker.addPropertyChangeListener(this);
        this.parentFactory = parentFactory;

        setPreferredSize(new Dimension(30, 28));
        setMaximumSize(new Dimension(30, 28));
        setMinimumSize(new Dimension(30, 28));

        menu.setPreferredSize(new Dimension(30, 45));
        menu.setMaximumSize(new Dimension(30, 45));
        menu.setToolTipText(
            Resources.getString("plugin.spellcheck.SPELLCHECK"));

        this.add(menu);

        this.setBorder(null);
        this.menu.add(createEnableCheckBox());
        this.menu.addSeparator();
        this.menu.setBorder(null);
        this.menu.setOpaque(false);
        this.setOpaque(false);

        final DefaultListModel model = new DefaultListModel();
        list = new JList(model);

        this.languageSelectionRenderer = new LanguageListRenderer();

        for (Parameters.Locale locale : Parameters.getLocales())
        {
            if (!localeAvailabilityCache.containsKey(locale))
            {
                localeAvailabilityCache.put(locale,
                    spellChecker.isLocaleAvailable(locale));
            }

            localeList.add(locale);
        }

        setModelElements(model);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);

        String localeIso = Parameters.getDefault(Default.LOCALE);
        Parameters.Locale loc = Parameters.getLocale(localeIso);

        list.setCellRenderer(languageSelectionRenderer);
        list.setSelectedIndex(localeList.indexOf(loc) + 1);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        removeItem.setPreferredSize(new Dimension(30, 28));

        list.addListSelectionListener(new LanguageListSelectionListener());

        menu.add(scroll);

        ImageIcon flagIcon =
            getLocaleIcon(checker.getLocale(),
                localeAvailabilityCache.get(checker.getLocale()));
        SelectedObject selectedObject =
            new SelectedObject(flagIcon, checker.getLocale());
        menu.setSelected(selectedObject);

        this.menu.addSeparator();

        menu.add(removeItem);

        list.addKeyListener(new KeyListener()
        {
            long time = 0;

            String key = "";

            public void keyTyped(KeyEvent e)
            {
                char ch = e.getKeyChar();

                if (!Character.isLetter(ch))
                    return;

                if (time + 1000 < System.currentTimeMillis())
                    key = "";

                time = System.currentTimeMillis();

                key += ch;

                for (int i = 0; i < model.getSize(); i++)
                {
                    String label =
                        ((Parameters.Locale) model.getElementAt(i)).getLabel()
                            .toLowerCase();
                    if (label.startsWith(key.toLowerCase()))
                    {
                        list.setSelectedIndex(i);
                        list.ensureIndexIsVisible(i);
                        break;
                    }
                }

                list.requestFocusInWindow();

            }

            public void keyReleased(KeyEvent e)
            {

            }

            public void keyPressed(KeyEvent e)
            {

            }
        });

        removeItem.setEnabled(!spellChecker.getLocale().getIsoCode()
            .equals(Parameters.getDefault(Parameters.Default.LOCALE))
            && spellChecker.isUserLocale(spellChecker.getLocale()));

        removeItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    list.setEnabled(false);
                    Locale locale = (Locale) menu.getSelectedObject();

                    localeAvailabilityCache.put(locale, false);

                    spellChecker.removeLocale(locale);

                    list.setEnabled(true);

                    ImageIcon flagIcon =
                        getLocaleIcon(spellChecker.getLocale(),
                            localeAvailabilityCache.get(spellChecker
                                .getLocale()));
                    SelectedObject selectedObject =
                        new SelectedObject(flagIcon, spellChecker.getLocale());
                    menu.setSelected(selectedObject);

                }
                catch (Exception ex)
                {
                    PopupDialog dialog =
                        SpellCheckActivator.getUIService().getPopupDialog();

                    String message =
                        Resources
                            .getString("plugin.spellcheck.DICT_ERROR_DELETE")
                            + ex.getMessage();

                    dialog.showMessagePopupDialog(
                        message,
                        Resources
                            .getString("plugin.spellcheck.DICT_ERROR_DELETE_TITLE"),
                        PopupDialog.WARNING_MESSAGE);
                    logger.error("Error removing dict", ex);
                }
            }
        });
    }

    @Override
    public String getName()
    {
        return "Spell Checker Toggle";
    }

    public void setCurrentContact(MetaContact metaContact)
    {

    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {

    }

    public void setCurrentAccountID(AccountID account)
    {

    }

    private static ImageIcon getLocaleIcon(Parameters.Locale locale,
        boolean isAvailable)
    {
        if (isAvailable && AVAILABLE_FLAGS.containsKey(locale))
            return AVAILABLE_FLAGS.get(locale);
        else if (!isAvailable && UNAVAILABLE_FLAGS.containsKey(locale))
            return UNAVAILABLE_FLAGS.get(locale);
        else
        {
            // load resource
            ImageIcon localeFlag;

            try
            {
                localeFlag = Resources.getFlagImage(locale.getFlagIcon());

                BufferedImage flagBuffer = copy(localeFlag.getImage());
                setFaded(flagBuffer);
                ImageIcon unavailableLocaleFlag = new ImageIcon(flagBuffer);

                AVAILABLE_FLAGS.put(locale, localeFlag);
                UNAVAILABLE_FLAGS.put(locale, unavailableLocaleFlag);
                return isAvailable ? localeFlag : unavailableLocaleFlag;
            }
            catch (IOException exc)
            {
                AVAILABLE_FLAGS.put(locale, BLANK_FLAG_ICON);
                UNAVAILABLE_FLAGS.put(locale, BLANK_FLAG_ICON);
                return BLANK_FLAG_ICON;
            }
        }
    }

    /**
     * Creates a deep copy of an image.
     *
     * @param image picture to be processed
     * @return copy of the image
     */
    private static BufferedImage copy(Image image)
    {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        BufferedImage copy;
        try
        {
            PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
            pg.grabPixels();
            ColorModel cm = pg.getColorModel();

            WritableRaster raster =
                cm.createCompatibleWritableRaster(width, height);
            boolean isRasterPremultiplied = cm.isAlphaPremultiplied();
            copy = new BufferedImage(cm, raster, isRasterPremultiplied, null);
        }
        catch (InterruptedException e)
        {
            copy =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g2 = copy.createGraphics();
        g2.setComposite(AlphaComposite.Src); // Preserves color of
        // transparent pixels
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return copy;
    }

    /**
     * Removes all color from an image and makes it partly translucent. Original
     * grayscale method written by Marty Stepp.
     *
     * @param image picture to be processed
     */
    private static void setFaded(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int row = 0; row < width; ++row)
        {
            for (int col = 0; col < height; ++col)
            {
                int c = image.getRGB(row, col);

                int r =
                    (((c >> 16) & 0xff) + ((c >> 8) & 0xff) + (c & 0xff)) / 3;

                int newRgb = (0xff << 24) | (r << 16) | (r << 8) | r;
                newRgb &= (1 << 24) - 1; // Blanks alpha value
                newRgb |= 128 << 24; // Resets it to the alpha of 128
                image.setRGB(row, col, newRgb);
            }
        }
    }

    public void setCurrentContact(Contact contact)
    {
        // TODO Auto-generated method stub

    }

    /**
     * When locale changed update the selected dict.
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(!evt.getPropertyName().equals(SpellChecker.LOCALE_CHANGED_PROP))
            return;

        Locale currentLocale = spellChecker.getLocale();
        ImageIcon flagIcon =
            getLocaleIcon(currentLocale,
                localeAvailabilityCache.get(currentLocale));
        SelectedObject selectedObject =
            new SelectedObject(flagIcon, currentLocale);
        menu.setSelected(selectedObject);
    }

    private static class SelectorMenu
        extends SIPCommMenu
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        Image image = Resources.getImage("service.gui.icons.DOWN_ARROW_ICON")
            .getImage();

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g.drawImage(image, getWidth() - image.getWidth(this) - 1,
                (getHeight() - image.getHeight(this) - 1) / 2, this);
        }
    }

    /**
     * Returns the enable spell check checkbox.
     *
     * @return the created checkbox
     */
    private JCheckBox createEnableCheckBox()
    {
        final JCheckBox checkBox = new SIPCommCheckBox(
            Resources.getString("plugin.spellcheck.ENABLE_SPELL_CHECK"));

        checkBox.setSelected(spellChecker.isEnabled());
        checkBox.setIconTextGap(0);
        checkBox.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent evt)
            {
                spellChecker.setEnabled(checkBox.isSelected());
                SpellCheckActivator.getConfigService().setProperty(
                    "plugin.spellcheck.ENABLE", checkBox.isSelected());
            }
        });

        return checkBox;
    }

    /**
     * Set the elements for list model
     *
     * @param model the model whose elements are to be set
     */
    private void setModelElements(DefaultListModel model)
    {
        synchronized (model)
        {
            model.clear();

            Collections.sort(
                    localeList,
                    new Comparator<Parameters.Locale>()
                    {
                        public int compare(Locale o1, Locale o2)
                        {
                            boolean b1 = spellChecker.isLocaleAvailable(o1);
                            boolean b2 = spellChecker.isLocaleAvailable(o2);

                            if (b1 == b2)
                                return 0;

                            return (b1 ? -1 : 1);
                        }
                    });

            for (Parameters.Locale loc : localeList)
                model.addElement(loc);
        }
    }

    /**
     * SpellChecker worker, sets specified locale.
     * @param locale the locale to set.
     * @return
     */
    public SetSpellChecker createSpellCheckerWorker(Parameters.Locale locale)
    {
        return new SetSpellChecker(locale, list);
    }

    /**
     * Returns the factory that has created the component.
     * @return the parent factory.
     */
    public PluginComponentFactory getParentFactory()
    {
        return parentFactory;
    }

    /**
     * The worker.
     */
    public class SetSpellChecker extends SwingWorker
    {
        private final Parameters.Locale locale;

        private final JList sourceList;

        private boolean skipFiring = false;

        public SetSpellChecker(Parameters.Locale locale, JList sourceList)
        {
            this.locale = locale;
            this.sourceList = sourceList;
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        @Override
        public void finished()
        {
            if (get() != null)
            {
                sourceList.setEnabled(true);

                localeAvailabilityCache.put(locale, true);

                ImageIcon flagIcon = getLocaleIcon(locale,
                        localeAvailabilityCache.get(locale));

                SelectedObject selectedObject =
                    new SelectedObject(flagIcon, locale);

                menu.setSelected(selectedObject);
            }
            else
            {
                // reverts selection
                skipFiring = true;

                // source.setSelectedItem(spellChecker.getLocale());
                ImageIcon flagIcon =
                    getLocaleIcon(locale,
                        localeAvailabilityCache.get(locale));

                SelectedObject selectedObject =
                    new SelectedObject(flagIcon, locale);

                menu.setSelected(selectedObject);

                skipFiring = false;

                sourceList.setEnabled(true);
            }

            // Indicate to the user that the language is currently
            // loading.
            locale.setLoading(false);

            sourceList.removeListSelectionListener(sourceList
                .getListSelectionListeners()[0]);
            setModelElements((DefaultListModel) sourceList.getModel());
            sourceList.setSelectedValue(locale, true);
            removeItem.setEnabled(!spellChecker.getLocale().getIsoCode()
                .equals(Parameters.getDefault(Parameters.Default.LOCALE))
                && spellChecker.isUserLocale(spellChecker.getLocale()));
            sourceList
                .addListSelectionListener(new LanguageListSelectionListener());

        }

        /**
         * Download the dictionary.
         */
        @Override
        public Object construct() throws Exception
        {
            try
            {
                // prevents potential infinite loop during errors
                if (this.skipFiring)
                    return null;

                spellChecker.setLocale(locale);

                return locale;
            }
            catch (Exception exc)
            {
                logger.warn(
                    "Unable to retrieve dictionary for " + locale, exc);

                // warns that it didn't work
                PopupDialog dialog =
                    SpellCheckActivator.getUIService()
                        .getPopupDialog();
                String message
                    = Resources.getString(
                        "plugin.spellcheck.DICT_ERROR");
                if (exc instanceof IOException)
                {
                    message = Resources.getString(
                        "plugin.spellcheck.DICT_RETRIEVE_ERROR")
                            + ":\n" + locale.getDictUrl();
                }
                else if (exc instanceof IllegalArgumentException)
                {
                    message = Resources.getString(
                        "plugin.spellcheck.DICT_PROCESS_ERROR");
                }

                dialog.showMessagePopupDialog(
                    message,
                    Resources
                        .getString("plugin.spellcheck.DICT_ERROR_TITLE"),
                    PopupDialog.WARNING_MESSAGE);
            }

            return null;
        }
    }

    /**
     * A custom renderer for languages list, transforming a Locale to a row
     * with an icon and text.
     */
    private class LanguageListRenderer
        extends DefaultListCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        @Override
        public Component getListCellRendererComponent(JList list,
            Object value, int index, boolean isSelected,
            boolean cellHasFocus)
        {
            Parameters.Locale locale = (Parameters.Locale) value;

            if (!localeAvailabilityCache.containsKey(locale))
            {
                localeAvailabilityCache.put(locale,
                    spellChecker.isLocaleAvailable(locale));
            }

            ImageIcon flagIcon =
                getLocaleIcon(locale, localeAvailabilityCache.get(locale));

            String localeLabel = locale.getLabel();

            if (locale.isLoading())
                setText("<html>" + localeLabel
                    + " <font color='gray'><i>loading...</i></font><html>");
            else
                setText(localeLabel);

            setIcon(flagIcon);

            return this;
        }
    }

    private class LanguageListSelectionListener
        implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting())
            {
                JList source = (JList) e.getSource();
                Parameters.Locale locale
                    = (Parameters.Locale) source.getSelectedValue();

                source.setEnabled(false);

                // Indicate to the user that the language is currently
                // loading.
                locale.setLoading(true);

                new SetSpellChecker(locale, source).start();
                source.requestFocusInWindow();
                removeItem.setEnabled(false);

            }
        }
    }

    /**
     * Returns the index indicating the position of this menu in its container.
     * 
     * @return -1 to indicate that this menu should take the last position
     */
    @Override
    public int getPositionIndex()
    {
        return -1;
    }

    @Override
    public void setCurrentContact(Contact contact, String resourceName)
    {
    }
}
