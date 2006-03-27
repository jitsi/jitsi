package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.Component;

import javax.swing.Icon;

public interface ConfigurationForm {

    public String getTitle();
    
    public Icon getIcon();
    
    public Component getForm();
}
