/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommBorders;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>MessageDialog</tt> is a <tt>JDialog</tt> that contains a question
 * message, two buttons to confirm or cancel the question and a check box that
 * allows user to choose to not be questioned any more over this subject.
 * <p>
 * The message and the name of the "OK" button could be configured.
 * 
 * @author Yana Stamcheva
 */
public class ErrorDialog
    extends SIPCommDialog
    implements  ActionListener,
                HyperlinkListener
{
    private Logger logger = Logger.getLogger(ErrorDialog.class);
    
    private JButton okButton = new JButton(
        Messages.getI18NString("ok").getText());

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ERROR_ICON)));

    private StyledHTMLEditorPane htmlMsgEditorPane = new StyledHTMLEditorPane();
    
    private SIPCommMsgTextArea msgTextArea = new SIPCommMsgTextArea();
    
    private JTextArea stackTraceTextArea = new JTextArea();
    
    private JScrollPane stackTraceScrollPane = new JScrollPane();

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JPanel messagePanel = new JPanel(new BorderLayout(5, 5));

    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    public static final int WARNING = 1;
    
    public static final int ERROR = 0;
    
    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window.
     * @param owner This dialog owner.
     */
    public ErrorDialog(Frame owner)
    {
        super(owner, false);
        
        this.setTitle(Messages.getI18NString("removeContact").getText());

        this.messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        
        this.stackTraceScrollPane.setBorder(new SIPCommBorders.BoldRoundBorder());
        this.stackTraceScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        
        this.init();
    }
    
    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     */
    public ErrorDialog(Frame owner, String message)
    {
        this(owner);
        
        this.messagePanel.add(msgTextArea, BorderLayout.CENTER);
        
        this.msgTextArea.setText(message);
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     * @param e the exception correspinding to the error
     */
    public ErrorDialog(Frame owner, String message, Exception e)
    {   
        this(owner);
        
        this.htmlMsgEditorPane.setEditable(false);
        this.htmlMsgEditorPane.setOpaque(false);
        
        this.htmlMsgEditorPane.addHyperlinkListener(this);

        this.messagePanel.add(htmlMsgEditorPane, BorderLayout.CENTER);
        
        String startDivTag = "<DIV id=\"message\">";
        String endDivTag = "</DIV>";
        
        String msgString = startDivTag + message
                + " <A href=''>more info</A>" + endDivTag;
        
        htmlMsgEditorPane.appendToEnd(msgString);    
        
        String stackTrace = null;
        
        if(e.getCause() != null)
            stackTrace = e.getCause().toString();
        
        for(int i = 0; i < e.getStackTrace().length; i ++)
        {
            StackTraceElement element = e.getStackTrace()[i];
            
            stackTrace += "\n \tat " + element;
        }
        this.stackTraceTextArea.setText(stackTrace);
    }
    
    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     */
    public ErrorDialog(Frame owner, String message, String title)
    {
        this(owner, message);
        
        this.setTitle(title);
    }
    
    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     */
    public ErrorDialog(Frame owner, String message, Exception e, String title)
    {
        this(owner, message, e);
        
        this.setTitle(title);
    }
    
    /**
     * 
     * @param owner
     * @param message
     * @param title
     * @param type
     */
    public ErrorDialog(Frame owner, String message, String title, int type)
    {
        this(owner, message, title);
        
        if(type == WARNING)
            iconLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.WARNING_ICON)));        
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

        this.messagePanel.add(iconLabel, BorderLayout.WEST);
        
        this.mainPanel.add(messagePanel, BorderLayout.NORTH);
        this.mainPanel.add(buttonsPanel, BorderLayout.CENTER);
        
        this.getContentPane().add(mainPanel);
    }

    /**
     * Sets the message to be displayed.
     * @param message The message to be displayed.
     */
    public void setMessage(String message)
    {
        this.msgTextArea.setText(message);
    }
    
    /**
     * Shows the dialog.
     */
    public void showDialog()
    {
        this.pack();
        this.setVisible(true);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        
        if(button.equals(okButton))            
            this.dispose();     
    }

    protected void close(boolean isEscaped)
    {
        this.okButton.doClick();
    }

    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            this.messagePanel.add(stackTraceScrollPane, BorderLayout.SOUTH);
            this.messagePanel.revalidate();
            this.messagePanel.repaint();
            this.pack();
        }
    }
}
