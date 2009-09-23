package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * Represents the background status panel of a peer
 */
public class CallStatusPanel
    extends TransparentPanel
{
    /*
     * Silence the serial warning. Though there isn't a plan to serialize
     * the instances of the class, there're no fields so the default
     * serialization routine will work.
     */
    private static final long serialVersionUID = 0L;

    public CallStatusPanel(LayoutManager layout)
    {
        super(layout);
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        this.setBackground(Color.WHITE);
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(Color.DARK_GRAY);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
        }
        finally
        {
            g.dispose();
        }
    }
}