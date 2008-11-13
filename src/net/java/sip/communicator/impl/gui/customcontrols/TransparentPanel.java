package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import javax.swing.*;

public class TransparentPanel
    extends JPanel
{
    public TransparentPanel ()
    {
        super();

        this.setOpaque(false);
    }

    public TransparentPanel (LayoutManager layoutManager)
    {
        super(layoutManager);

        this.setOpaque(false);
    }
}
