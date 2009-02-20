/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.systray.jdic;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.impl.systray.jdic.SystemTray.*;
import net.java.sip.communicator.service.gui.*;

/**
 * @author Lubomir Marinov
 */
public class TrayIcon
{
    public static final int ERROR_MESSAGE_TYPE =
        org.jdesktop.jdic.tray.TrayIcon.ERROR_MESSAGE_TYPE;

    public static final int INFO_MESSAGE_TYPE =
        org.jdesktop.jdic.tray.TrayIcon.INFO_MESSAGE_TYPE;

    public static final int NONE_MESSAGE_TYPE =
        org.jdesktop.jdic.tray.TrayIcon.NONE_MESSAGE_TYPE;

    public static final int WARNING_MESSAGE_TYPE =
        org.jdesktop.jdic.tray.TrayIcon.WARNING_MESSAGE_TYPE;

    private final TrayIconPeer peer;

    public TrayIcon(ImageIcon icon, String tooltip, Object popup)
        throws IllegalArgumentException,
        UnsupportedOperationException,
        HeadlessException,
        SecurityException
    {
        SystemTrayPeer systemTrayPeer =
            SystemTray.getDefaultSystemTray().getPeer();
        if (systemTrayPeer != null)
            peer = systemTrayPeer.createTrayIcon(icon, tooltip, popup);
        else
            peer = null;
    }

    public void addActionListener(ActionListener listener)
    {
        if (peer != null)
            peer.addActionListener(listener);
    }

    public void addBalloonActionListener(ActionListener listener)
    {
        if (peer != null)
            peer.addBalloonActionListener(listener);
    }

    public void displayMessage(String caption, String text, int messageType)
        throws NullPointerException
    {
        if (peer != null)
            peer.displayMessage(caption, text, messageType);
    }

    TrayIconPeer getPeer()
    {
        return peer;
    }

    public void setIcon(ImageIcon icon) throws NullPointerException
    {
        if (peer != null)
            peer.setIcon(icon);
    }

    public void setIconAutoSize(boolean autoSize)
    {
        if (peer != null)
            peer.setIconAutoSize(autoSize);
    }

    static interface TrayIconPeer
    {
        void addActionListener(ActionListener listener);

        void addBalloonActionListener(ActionListener listener);

        void displayMessage(String caption, String text, int messageType)
            throws NullPointerException;

        void setIcon(ImageIcon icon) throws NullPointerException;

        void setIconAutoSize(boolean autoSize);
    }

    static class AWTTrayIconPeer
        implements TrayIconPeer
    {
        private final Method addActionListener;
        
        private final Method addMouseListener;

        private final Method displayMessage;

        private final Object impl;

        private final Class<?> messageTypeClass;

        private final Method setIcon;

        private final Method setIconAutoSize;

        public AWTTrayIconPeer(Class<?> clazz, Image image, String tooltip,
            JPopupMenu popup)
            throws IllegalArgumentException,
            UnsupportedOperationException,
            HeadlessException,
            SecurityException
        {
            Constructor<?> constructor;
            try
            {
                constructor = clazz.getConstructor(new Class<?>[]
                { Image.class, String.class });
                addActionListener =
                    clazz.getMethod("addActionListener", new Class<?>[]
                    { ActionListener.class });
                addMouseListener =
                    clazz.getMethod("addMouseListener", new Class<?>[]
                    { MouseListener.class });
                messageTypeClass =
                    Class.forName("java.awt.TrayIcon$MessageType");
                displayMessage =
                    clazz.getMethod("displayMessage", new Class<?>[]
                    { String.class, String.class, messageTypeClass });
                setIcon = clazz.getMethod("setImage", new Class<?>[]
                { Image.class });
                setIconAutoSize =
                    clazz.getMethod("setImageAutoSize", new Class<?>[]
                    { boolean.class });
            }
            catch (ClassNotFoundException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
            catch (NoSuchMethodException ex)
            {
                throw new UnsupportedOperationException(ex);
            }

            try
            {
                impl = constructor.newInstance(new Object[]
                { image, tooltip });
                
                addMouseListener(new AWTMouseAdapter(popup));
            }
            catch (IllegalAccessException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
            catch (InstantiationException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UnsupportedOperationException(ex);
                if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                if (cause instanceof HeadlessException)
                    throw (HeadlessException) cause;
                if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                throw new UnsupportedOperationException(cause);
            }
        }

        public void addActionListener(ActionListener listener)
        {
            try
            {
                addActionListener.invoke(getImpl(), new Object[]
                { listener });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }
        
        public void addMouseListener(MouseListener listener)
        {
            try
            {
                addMouseListener.invoke(getImpl(), new Object[] { listener });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }

        public void addBalloonActionListener(ActionListener listener)
        {
            // java.awt.TrayIcon doesn't support addBalloonActionListener()
        }

        public void displayMessage(String caption, String text, int messageType)
            throws NullPointerException
        {
            try
            {
                displayMessage.invoke(getImpl(), new Object[]
                { caption, text, getMessageType(messageType) });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }

        public Object getImpl()
        {
            return impl;
        }

        private Object getMessageType(int messageType)
        {
            Object[] constants = messageTypeClass.getEnumConstants();
            String name;
            switch (messageType)
            {
            case ERROR_MESSAGE_TYPE:
                name = "ERROR";
                break;
            case INFO_MESSAGE_TYPE:
                name = "INFO";
                break;
            case NONE_MESSAGE_TYPE:
                name = "NONE";
                break;
            case WARNING_MESSAGE_TYPE:
                name = "WARNING";
                break;
            default:
                throw new IllegalArgumentException("messageType");
            }
            for (int i = 0; i < constants.length; i++)
            {
                Object constant = constants[i];
                if (name.equals(constant.toString()))
                    return constant;
            }
            throw new IllegalArgumentException("messageType");
        }

        public void setIcon(ImageIcon icon) throws NullPointerException
        {
            try
            {
                setIcon.invoke(getImpl(), new Object[]
                { (icon == null) ? null : icon.getImage() });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }

        public void setIconAutoSize(boolean autoSize)
        {
            try
            {
                setIconAutoSize.invoke(getImpl(), new Object[]
                { autoSize });
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                throw new UndeclaredThrowableException((cause == null) ? ex
                    : cause);
            }
        }
    }

    static class JdicTrayIconPeer
        implements TrayIconPeer
    {
        private final org.jdesktop.jdic.tray.TrayIcon impl;

        public JdicTrayIconPeer(ImageIcon icon, String tooltip, JPopupMenu popup)
        {
            impl = new org.jdesktop.jdic.tray.TrayIcon(icon, tooltip, popup);
        }

        public void addActionListener(ActionListener listener)
        {
            getImpl().addActionListener(listener);
        }

        public void addBalloonActionListener(ActionListener listener)
        {
            getImpl().addBalloonActionListener(listener);
        }

        public void displayMessage(String caption, String text, int messageType)
            throws NullPointerException
        {
            getImpl().displayMessage(caption, text, messageType);
        }

        org.jdesktop.jdic.tray.TrayIcon getImpl()
        {
            return impl;
        }

        public void setIcon(ImageIcon icon)
        {
            getImpl().setIcon(icon);
        }

        public void setIconAutoSize(boolean autoSize)
        {
            getImpl().setIconAutoSize(autoSize);
        }
    }
    
    /**
     * Extended mouse adapter to show the JpopupMenu in java 6
     * Based on : http://weblogs.java.net/blog/ixmal/archive/2006/05/using_jpopupmen.html
     * 
     * @author Damien Roth
     */
    static class AWTMouseAdapter
        extends MouseAdapter
    {
    	private static int MARGIN = 15;
        private JPopupMenu popup;
        private boolean popupVisible = false;
        private Component mframe = null;
    
        public AWTMouseAdapter(JPopupMenu p)
        {
            this.popup = p;
            
            // Get the MainFrame
            ExportedWindow win = SystrayActivator.getUIService()
                .getExportedWindow(ExportedWindow.MAIN_WINDOW);
            
            /* 
             * The JPopupMenu need to have a invoker defined to work correctly.
             * It can be the popup menu itself, but if the mainframe is used, the popup
             * automatically close when the mainframe lost the focus
             */
            if(win != null || win.getSource() != null || (win.getSource() instanceof JFrame))
                this.mframe = (Component) win.getSource();
        }
        
        public void mouseReleased(MouseEvent e)
        {
            // Trick used here, the isVisible function always return false
            // So we manage this manually
            if (e.getButton() == MouseEvent.BUTTON3 && !popupVisible)
            {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int x = e.getX(), y = e.getY();
                
                // Use a margin to avoid the menu hiding the icon
                if (x < MARGIN)
                    x = MARGIN;
                else if (x > screen.getWidth()-MARGIN)
                    x = (int) screen.getWidth()-MARGIN;
                if (y < MARGIN)
                    y = MARGIN;
                else if (y > screen.getHeight()-MARGIN)
                    y = (int) screen.getWidth()-MARGIN;
                
                popup.setLocation(x, y);
                
                if (mframe != null && mframe.isVisible())
                	popup.setInvoker(mframe);
                else
                    popup.setInvoker(popup);
                popup.setVisible(true);
                popupVisible = true;
            }
            else
            {
                popup.setVisible(false);
                popupVisible = false;
            }
        }
    }
}
