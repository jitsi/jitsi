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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * Implements a <tt>JDialog</tt> which displays an error message and,
 * optionally, a <tt>Throwable</tt> stack trace. <tt>ErrorDialog</tt> has an OK
 * button which dismisses the message and a link to display the
 * <tt>Throwable</tt> stack trace upon request if available.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class ErrorDialog
    extends SIPCommDialog
    implements  ActionListener,
                HyperlinkListener,
                Skinnable
{
    private static final long serialVersionUID = 1L;

    /**
     * The <tt>Logger</tt> used by the <tt>ErrorDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ErrorDialog.class);

    private JButton okButton
        = new JButton(
                DesktopUtilActivator.getResources().getI18NString("service.gui.OK"));

    private JLabel iconLabel
        = new JLabel(new ImageIcon(
                DesktopUtilActivator.getImage("service.gui.icons.ERROR_ICON")));

    private StyledHTMLEditorPane htmlMsgEditorPane = new StyledHTMLEditorPane();

    private JTextArea stackTraceTextArea = new JTextArea();

    private JScrollPane stackTraceScrollPane = new JScrollPane();

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private TransparentPanel infoMessagePanel = new TransparentPanel();

    private TransparentPanel messagePanel
        = new TransparentPanel(new BorderLayout());

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    /**
     * Load the "net.java.sip.communicator.SHOW_STACK_TRACE" property to
     * determine whether we should show stack trace in error dialogs.
     * Default is show.
     */
    private static String showStackTraceDefaultProp
        = DesktopUtilActivator.getResources().getSettingsString(
            "net.java.sip.communicator.SHOW_STACK_TRACE");

    /**
     * Should we show stack trace.
     */
    private final static boolean showStackTrace =
        (showStackTraceDefaultProp != null) ?
            Boolean.parseBoolean(showStackTraceDefaultProp) : true;

    /**
     * The indicator which determines whether the details of the error are
     * currently shown.
     * <p>
     * The indicator is initially set to <tt>true</tt> because the constructor
     * {@link #ErrorDialog(Frame, String, String, Throwable)} calls
     * {@link #showOrHideDetails()} and thus <tt>ErrorDialog</tt> defaults to
     * not showing the details of the error.
     * </p>
     */
    private boolean detailsShown = true;

    /**
     * The type of <tt>ErrorDialog</tt> which displays a warning instead of an
     * error.
     */
    public static final int WARNING = 1;

    /**
     * The type of this <tt>ErrorDialog</tt> (e.g. {@link #WARNING}). The
     * default <tt>ErrorDialog</tt> displays an error.
     */
    private int type = 0;

    /**
     * The maximum width that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_WIDTH = 340;

    /**
     * The maximum height that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_HEIGHT = 800;

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed.
     *
     * @param owner the dialog owner
     * @param title the title of the dialog
     * @param message the message to be displayed
     */
    public ErrorDialog( Frame owner,
                        String title,
                        String message)
    {
        super(owner, false);

        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 10, 20));

        if (showStackTrace)
        {
            this.stackTraceScrollPane.setBorder(BorderFactory.createLineBorder(
                iconLabel.getForeground()));

            this.stackTraceScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        }

        this.setTitle(title);

        this.infoMessagePanel.setLayout(new BorderLayout());

        JEditorPane messageArea = new JEditorPane();

        /*
         * Make JEditorPane respect our default font because we will be using it
         * to just display text.
         */
        messageArea.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                true);

        messageArea.setOpaque(false);
        messageArea.setEditable(false);
        messageArea.setContentType("text/html");
        messageArea.setText(
                "<html><body><p align=\"left\" >"+message+"</p></body></html>");
        //try to reevaluate the preferred size of the message pane.
        //(this is definitely not a neat way to do it ... but it works).
        messageArea.setSize(
                new Dimension(MAX_MSG_PANE_WIDTH, MAX_MSG_PANE_HEIGHT));
        messageArea.setPreferredSize(
                new Dimension(
                        MAX_MSG_PANE_WIDTH,
                        messageArea.getPreferredSize().height));

        this.infoMessagePanel.add(messageArea, BorderLayout.CENTER);

        this.init();
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title, error message to be displayed and the
     * <tt>Throwable</tt> associated with the error.
     *
     * @param owner the dialog owner
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param e the exception corresponding to the error
     */
    public ErrorDialog( Frame owner,
                        String title,
                        String message,
                        Throwable e)
    {
        this(owner, title, message);

        if (showStackTrace && e != null)
        {
            this.setTitle(title);

            this.htmlMsgEditorPane.setEditable(false);
            this.htmlMsgEditorPane.setOpaque(false);

            this.htmlMsgEditorPane.addHyperlinkListener(this);

            showOrHideDetails();

            this.infoMessagePanel.add(htmlMsgEditorPane, BorderLayout.SOUTH);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();

            String stackTrace = sw.toString();

            try
            {
                sw.close();
            }
            catch (IOException ex)
            {
                //really shouldn't happen. but log anyway
                logger.error("Failed to close a StringWriter. ", ex);
            }

            this.stackTraceTextArea.setText(stackTrace);
        }
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed and of a specific type.
     *
     * @param owner the dialog owner
     * @param title the title of the error dialog
     * @param message the message to be displayed
     * @param type the dialog type
     */
    public ErrorDialog( Frame owner,
                        String title,
                        String message,
                        int type)
    {
        this(owner, title, message);

        if(type == WARNING)
        {
            iconLabel.setIcon(new ImageIcon(
                DesktopUtilActivator.getImage("service.gui.icons.WARNING_ICON")));
            this.type = type;
        }
    }

    /**
     * Initializes this dialog.
     */
    private void init()
    {
        this.getRootPane().setDefaultButton(okButton);

        this.stackTraceScrollPane.getViewport().add(stackTraceTextArea);
        this.stackTraceScrollPane.setPreferredSize(
            new Dimension(this.getWidth(), 100));

        this.buttonsPanel.add(okButton);

        this.okButton.addActionListener(this);

        this.mainPanel.add(iconLabel, BorderLayout.WEST);

        this.messagePanel.add(infoMessagePanel, BorderLayout.NORTH);

        this.mainPanel.add(messagePanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Shows if previously hidden or hides if previously shown the details of
     * the error. Called when the "more" link is clicked.
     */
    public void showOrHideDetails()
    {
        String startDivTag = "<div id=\"message\">";
        String endDivTag = "</div>";
        String msgString;

        detailsShown = !detailsShown;

        if(detailsShown)
        {
             msgString = startDivTag
                + " <p align=\"right\"><a href=\"\">&lt;&lt; Hide info</a></p>"
                + endDivTag;
             this.messagePanel.add(stackTraceScrollPane, BorderLayout.CENTER);
        }
        else
        {
             msgString = startDivTag
                + " <p align=\"right\"><a href=\"\">More info &gt;&gt;</a></p>"
                + endDivTag;
             this.messagePanel.remove(stackTraceScrollPane);
        }

        htmlMsgEditorPane.setText(msgString);

        this.messagePanel.revalidate();
        this.messagePanel.repaint();
        // restore default values for preferred size,
        // as we have resized its components let it calculate
        // that size
        setPreferredSize(null);
        this.pack();
    }

    /**
     * Shows the dialog.
     */
    public void showDialog()
    {
        this.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        this.setLocation(screenSize.width/2 - this.getWidth()/2,
                screenSize.height/2 - this.getHeight()/2);

        this.setVisible(true);

        this.toFront();
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     *
     * @param e the <tt>ActionEvent</tt> instance that has just been fired.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();

        if(button.equals(okButton))
            this.dispose();
    }

    /**
     * Close the ErrorDialog. This function is invoked when user
     * presses the Escape key.
     *
     * @param isEscaped Specifies whether the close was triggered by pressing
     * the escape key.
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.okButton.doClick();
    }

    /**
     * Update the ErrorDialog when the user clicks on the hyperlink.
     *
     * @param e The event generated by the click on the hyperlink.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            showOrHideDetails();
    }

    /**
     * Reloads icon.
     */
    public void loadSkin()
    {
        String icon
            = (type == WARNING)
                ? "service.gui.icons.WARNING_ICON"
                : "service.gui.icons.ERROR_ICON";

        iconLabel.setIcon(new ImageIcon(DesktopUtilActivator.getImage(icon)));
    }
}
