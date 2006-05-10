package net.java.sip.communicator.impl.gui.main.customcontrols;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class RolloverButton extends JButton {

    public RolloverButton(String text){
        super(text);
        
        this.setRolloverEnabled(true);
    }
    
    public RolloverButton(ImageIcon icon){
        super(icon);
        
        this.setRolloverEnabled(true);
    }
}
