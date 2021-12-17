package net.judah.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class ToggleSwitch extends JPanel {
    private boolean activated = false;
    private Color switchColor = new Color(200, 200, 200), 
    		buttonColor = new Color(255, 255, 255), 
    		borderColor = new Color(50, 50, 50);
    // private Color activeSwitch = new Color(0, 125, 255);
    private Color activeSwitch = new Color(30, 179, 0);
    private BufferedImage puffer;
    private int borderRadius = 10;
    private Graphics2D g;
    private ActionListener listener;
    
    public ToggleSwitch(ActionListener listener) {
    	this();
    	this.listener = listener;
    }
    
    public ToggleSwitch() {
        super();
        setVisible(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent arg0) {
                activated = !activated;
                repaint();
                if (listener != null) 
                	listener.actionPerformed(new ActionEvent(this, 0, "" + activated));
            }
        });
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBounds(0, 0, 41, 21);
    }
    
    public void addActionListener(ActionListener l) {
    	listener = l;
    }
    
    @Override
    public void paint(Graphics gr) {
        if(g == null || puffer.getWidth() != getWidth() || puffer.getHeight() != getHeight()) {
            puffer = (BufferedImage) createImage(getWidth(), getHeight());
            g = (Graphics2D)puffer.getGraphics();
            RenderingHints rh = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHints(rh);
        }
        g.setColor(activated?activeSwitch:switchColor);
        g.fillRoundRect(0, 0, this.getWidth()-1,getHeight()-1, 5, borderRadius);
        g.setColor(borderColor);
        g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 5, borderRadius);
        g.setColor(buttonColor);
        if(activated) {
            g.fillRoundRect(getWidth()/2, 1,  (getWidth()-1)/2 -2, (getHeight()-1) - 2,  borderRadius, borderRadius);
            g.setColor(borderColor);
            g.drawRoundRect((getWidth()-1)/2, 0, (getWidth()-1)/2, (getHeight()-1), borderRadius, borderRadius);
        }
        else {
            g.fillRoundRect(1, 1, (getWidth()-1)/2 -2, (getHeight()-1) - 2,  borderRadius, borderRadius);
            g.setColor(borderColor);
            g.drawRoundRect(0, 0, (getWidth()-1)/2, (getHeight()-1), borderRadius, borderRadius);
        }

        gr.drawImage(puffer, 0, 0, null);
    }
    public boolean isActivated() {
        return activated;
    }
    public void setActivated(boolean activated) {
        this.activated = activated;
    }
    public Color getSwitchColor() {
        return switchColor;
    }
    /**
     * Unactivated Background Color of switch
     */
    public void setSwitchColor(Color switchColor) {
        this.switchColor = switchColor;
    }
    public Color getButtonColor() {
        return buttonColor;
    }
    /**
     * Switch-Button color
     */
    public void setButtonColor(Color buttonColor) {
        this.buttonColor = buttonColor;
    }
    public Color getBorderColor() {
        return borderColor;
    }
    /**
     * Border-color of whole switch and switch-button
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }
    public Color getActiveSwitch() {
        return activeSwitch;
    }
    public void setActiveSwitch(Color activeSwitch) {
        this.activeSwitch = activeSwitch;
    }
    /**
     * @return the borderRadius
     */
    public int getBorderRadius() {
        return borderRadius;
    }
    /**
     * @param borderRadius the borderRadius to set
     */
    public void setBorderRadius(int borderRadius) {
        this.borderRadius = borderRadius;
    }

}