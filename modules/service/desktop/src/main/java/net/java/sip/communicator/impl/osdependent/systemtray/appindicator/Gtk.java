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

import com.sun.jna.*;

/**
 * JNA mappings for the gtk2 library. Only functions required for the tray menu
 * are defined.
 *
 * @author Ingo Bauersachs
 */
interface Gtk extends Library
{
    Gtk INSTANCE = Native.load("gtk-x11-2.0", Gtk.class);

    public enum GtkIconSize
    {
        INVALID,
        MENU,
        SMALL_TOOLBAR,
        LARGE_TOOLBAR,
        BUTTON,
        DND,
        DIALOG
    }

    void gtk_init(int argc, String[] argv);
    void gtk_main();
    Pointer gtk_menu_new();
    Pointer gtk_image_menu_item_new_with_label(String label);
    Pointer gtk_separator_menu_item_new();
    void gtk_menu_item_set_submenu(Pointer menu_item, Pointer submenu);
    void gtk_image_menu_item_set_image(Pointer image_menu_item, Pointer image);
    void gtk_image_menu_item_set_always_show_image(Pointer image_menu_item, int always_show);
    void gtk_menu_item_set_label(Pointer menu_item, String label);
    void gtk_menu_shell_append(Pointer menu_shell, Pointer child);
    void gtk_widget_set_sensitive(Pointer widget, int sesitive);
    void gtk_widget_show_all(Pointer widget);
    void gtk_widget_destroy(Pointer widget);
    Pointer gtk_check_menu_item_new_with_label(String label);
    int gtk_check_menu_item_get_active(Pointer check_menu_item);
    void gtk_check_menu_item_set_active(Pointer check_menu_item, int is_active);

    void gdk_threads_enter();
    void gdk_threads_leave();

    Pointer gdk_pixbuf_new_from_data(Pointer data, int colorspace, int has_alpha,
        int bits_per_sample, int width, int height, int rowstride,
        Pointer destroy_fn, Pointer destroy_fn_data);
    Pointer gtk_image_new_from_pixbuf(Pointer pixbuf);
}
