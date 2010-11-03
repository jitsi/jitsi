/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>MessageDialog</tt> is a <tt>JDialog</tt> that contains a question
 * message, two buttons to confirm or cancel the question and a check box that
 * allows user to choose to not be questioned any more over this subject.
 * <p>
 * The message and the name of the "OK" button could be configured.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ErrorDialog
    extends SIPCommDialog
    implements  ActionListener,
                HyperlinkListener,
                Skinnable
{
    private static final long serialVersionUID = 1L;

    private final Logger logger = Logger.getLogger(ErrorDialog.class);

    private JButton okButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ERROR_ICON)));

    private StyledHTMLEditorPane htmlMsgEditorPane = new StyledHTMLEditorPane();

    private JEditorPane messageArea = new JEditorPane();

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
     * Set visible or hide the details of the error.
     * By default, this boolean is set to true, beacause while initialization it
     * will be reversed and set to false.
     */
    private boolean isDetailsShowed = true;

    public static final int WARNING = 1;

    /**
     * Type of this dialog.
     */
    private int type = 0;

    /**
     * The maximum width that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_WIDTH = 600;

    /**
     * The maximum height that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_HEIGHT = 800;

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
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

        this.stackTraceScrollPane.setBorder(
            new SIPCommBorders.BoldRoundBorder());

        this.stackTraceScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        this.setTitle(title);

        this.infoMessagePanel.setLayout(new BorderLayout());

        this.messageArea.setOpaque(false);
        this.messageArea.setEditable(false);
        this.messageArea.setContentType("text/html");
        messageArea.setText("<html><body><p align=\"left\" >"+message+"</p></body></html>");

        //try to reevaluate the preferred size of the message pane.
        //(this is definitely not a neat way to do it ... but it works).
        this.messageArea.setSize(
                        new Dimension(MAX_MSG_PANE_WIDTH, MAX_MSG_PANE_HEIGHT));
        int height = this.messageArea.getPreferredSize().height;
        this.messageArea.setPreferredSize(
                        new Dimension(MAX_MSG_PANE_WIDTH, height));

        this.infoMessagePanel.add(messageArea, BorderLayout.CENTER);

        this.init();
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
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

        this.setTitle(title);

        this.htmlMsgEditorPane.setEditable(false);
        this.htmlMsgEditorPane.setOpaque(false);

        this.htmlMsgEditorPane.addHyperlinkListener(this);

        displayOrHideDetails();

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

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
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
            iconLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.WARNING_ICON)));
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
     * This function show (if previously hided) or hide (if previously showed) the details of the error.
     * Function called when the "more" link is clicked.
     */
    public void displayOrHideDetails()
    {
        String startDivTag = "<div id=\"message\">";
        String endDivTag = "</div>";
        String msgString;

        isDetailsShowed = !isDetailsShowed;

        if(isDetailsShowed)
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
        // restore default values for prefered size,
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
        {
            displayOrHideDetails();
        }
    }

    /**
     * Reloads icon.
     */
    public void loadSkin()
    {
        if(type == WARNING)
        {
            iconLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.WARNING_ICON)));
        }
        else
        {
            iconLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.ERROR_ICON)));
        }
    }
}
