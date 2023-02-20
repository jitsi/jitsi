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
package net.java.sip.communicator.impl.osdependent.systemtray.appindicator;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.*;

/**
 * JNA mappings for libappindicator1.
 *
 * @author Ingo Bauersachs
 */
interface AppIndicator1 extends Library
{
    AppIndicator1 INSTANCE =
        Native.load("ayatana-appindicator", AppIndicator1.class);

    String APP_INDICATOR_SIGNAL_NEW_ICON            = "new-icon";
    String APP_INDICATOR_SIGNAL_NEW_ATTENTION_ICON  = "new-attention-icon";
    String APP_INDICATOR_SIGNAL_NEW_STATUS          = "new-status";
    String APP_INDICATOR_SIGNAL_NEW_LABEL           = "new-label";
    String APP_INDICATOR_SIGNAL_CONNECTION_CHANGED  = "connection-changed";
    String APP_INDICATOR_SIGNAL_NEW_ICON_THEME_PATH = "new-icon-theme-path";
    String APP_INDICATOR_SIGNAL_SCROLL_EVENT        = "scroll-event";

    /**
     * The category provides grouping for the indicators so that users can find
     * indicators that are similar together.
     */
    enum APP_INDICATOR_CATEGORY
    {
        /** The indicator is used to display the status of the application. */
        APPLICATION_STATUS,

        /** The application is used for communication with other people. */
        COMMUNICATIONS,

        /** A system indicator relating to something in the user's system. */
        SYSTEM_SERVICES,

        /** An indicator relating to the user's hardware. */
        HARDWARE,

        /**
         * Something not defined in this enum, please don't use unless you
         * really need it.
         */
        OTHER
    }

    /**
     * These are the states that the indicator can be on in the user's panel.
     * The indicator by default starts in the state {@link #PASSIVE} and can be
     * shown by setting it to {@link #ACTIVE}.
     */
    enum APP_INDICATOR_STATUS
    {
        /** The indicator should not be shown to the user. */
        PASSIVE,

        /** The indicator should be shown in it's default state. */
        ACTIVE,

        /** The indicator should show it's attention icon. */
        ATTENTION
    }

    class AppIndicatorClass extends Structure
    {
        // Parent
        public /*Gobject.GObjectClass*/ Pointer parent_class;

        // DBus Signals
        public Pointer new_icon;
        public Pointer new_attention_icon;
        public Pointer new_status;
        public Pointer new_icon_theme_path;
        public Pointer new_label;

        // Local Signals
        public Pointer connection_changed;
        public Pointer scroll_event;
        public Pointer app_indicator_reserved_ats;

        // Overridable Functions
        public Pointer fallback;
        public Pointer unfallback;

        // Reserved
        public Pointer app_indicator_reserved_1;
        public Pointer app_indicator_reserved_2;
        public Pointer app_indicator_reserved_3;
        public Pointer app_indicator_reserved_4;
        public Pointer app_indicator_reserved_5;
        public Pointer app_indicator_reserved_6;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                "parent_class",
                "new_icon",
                "new_attention_icon",
                "new_status",
                "new_icon_theme_path",
                "new_label",

                "connection_changed",
                "scroll_event",
                "app_indicator_reserved_ats",

                "fallback",
                "unfallback",

                "app_indicator_reserved_1",
                "app_indicator_reserved_2",
                "app_indicator_reserved_3",
                "app_indicator_reserved_4",
                "app_indicator_reserved_5",
                "app_indicator_reserved_6");
        }
    }

    class AppIndicator extends Structure
    {
        public /*Gobject.GObject*/ Pointer parent;
        public Pointer priv;

        @Override
        protected List<String> getFieldOrder()
        {
            return Arrays.asList("parent", "priv");
        }
    }

    // GObject Stuff
    NativeLong app_indicator_get_type();
    AppIndicator app_indicator_new(String id, String icon_name, int category);
    AppIndicator app_indicator_new_with_path(String id, String icon_name, int category, String icon_theme_path);

    // Set properties
    void app_indicator_set_status(AppIndicator self, int status);
    void app_indicator_set_attention_icon(AppIndicator self, String icon_name);
    void app_indicator_set_attention_icon_full(AppIndicator self, String name, String icon_desc);
    void app_indicator_set_menu(AppIndicator self, Pointer menu);
    void app_indicator_set_icon(AppIndicator self, String icon_name);
    void app_indicator_set_icon_full(AppIndicator self, String icon_name, String icon_desc);
    void app_indicator_set_label(AppIndicator self, String label, String guide);
    void app_indicator_set_icon_theme_path(AppIndicator self, String icon_theme_path);
    void app_indicator_set_ordering_index(AppIndicator self, int ordering_index);
    void app_indicator_set_secondary_activate_target(AppIndicator self, Pointer menuitem);
    void app_indicator_set_title(AppIndicator self, String title);

    // Get properties
    String app_indicator_get_id(AppIndicator self);
    int    app_indicator_get_category(AppIndicator self);
    int    app_indicator_get_status(AppIndicator self);
    String app_indicator_get_icon(AppIndicator self);
    String app_indicator_get_icon_desc(AppIndicator self);
    String app_indicator_get_icon_theme_path(AppIndicator self);
    String app_indicator_get_attention_icon(AppIndicator self);
    String app_indicator_get_attention_icon_desc(AppIndicator self);
    String app_indicator_get_title(AppIndicator self);

    Pointer app_indicator_get_menu(AppIndicator self);
    String  app_indicator_get_label(AppIndicator self);
    String  app_indicator_get_label_guide(AppIndicator self);
    int     app_indicator_get_ordering_index(AppIndicator self);
    Pointer app_indicator_get_secondary_activate_target(AppIndicator self, Pointer widget);

    // Helpers
    void app_indicator_build_menu_from_desktop(AppIndicator self, String desktop_file, String destkop_profile);
}
