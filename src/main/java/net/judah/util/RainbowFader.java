package net.judah.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**An extended JSlider that paints the Thumb chromatically based on the slider position.
 * <br/><br/>
 * Source: <a href="https://jasperpotts.com/blog/2008/08/skinning-a-slider-with-nimbus/">Skinning a Slider with Nimbus</a>
 * @author Jasper Potts (May 7, 2008) SliderSkinDemo.java 
 * @author Tsukino, Fumino and Jeff Masty (Nov 28, 2021) */
public class RainbowFader extends JSlider {
	
    static final float slope = 0.04f;
    
    public static Color chaseTheRainbow(int percent) {
    	if (percent < 0 || percent > 100) return Color.BLACK;
    	float red = 0;    
    	float green = 0;  
    	float blue = 0;	
    	
    	if (percent < 25) { // green up
    		blue = 1;
    		green = percent * slope ;
    	}
    	else if (percent < 50) { // blue down
    		green = 1;
    		blue = 2 - slope * percent;
    	}
    	else if (percent < 75) { // red up
    		green = 1;
    		red = (percent - 50) * slope;
    	} else { // green down
    		red = 1;
    		green = 4 - slope * percent;
    	}
    	return new Color(red, green, blue);
    }
    
    public static final Dimension SIZE = new Dimension(30, 100);
    
    static UIDefaults newDefaults = new UIDefaults();
    static {
    	newDefaults.put("Slider.thumbWidth", 16);
        newDefaults.put("Slider.thumbHeight", 22);
        newDefaults.put("Slider:SliderThumb.backgroundPainter", new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent comp, int w, int h) {
            	g.setColor(Color.DARK_GRAY);
            	g.fillRect(0, 0, w, h);
            	g.setColor(Color.BLACK);
            	g.fillRect(1, 1, w-2, h-2);

            	g.setColor(chaseTheRainbow(((JSlider)comp).getValue()));
            	g.fillRect(1, 1, w-3, h-3);
            }
        });
        newDefaults.put("Slider:SliderTrack.backgroundPainter", new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent c, int w, int h) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(2f));
                g.setColor(Color.LIGHT_GRAY);
                g.fillRoundRect(0, 8, w-1, 4, 4, 4);
                g.setColor(Color.GRAY);
                g.drawRoundRect(0, 8, w-1, 4, 4, 4);
            }
        });
    }
    
    public RainbowFader(ChangeListener updates) {
        this(0, 100, updates, null);
    }

    public RainbowFader(int min, int max, ChangeListener updates, String tooltip) {
        this(min, max, updates);
        if (tooltip != null)
            setToolTipText(tooltip);
    }

    public RainbowFader(int min, int max, ChangeListener updates) {
        super(min, max);
        putClientProperty("Nimbus.Overrides", newDefaults);
        putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        addChangeListener(updates);
        setOrientation(VERTICAL);
        setPaintTicks(true);
        setMajorTickSpacing(-1);
        setMinorTickSpacing(25);
        setPreferredSize(SIZE); 
    }

    public static void main(String[] args) {
	   try {
            UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) { e.printStackTrace(); }
    	JFrame parent = new JFrame();
    	
    	RainbowFader test = new RainbowFader(0, 100, 
				e -> {System.out.println("val: " + e.getSource());}, "test");
    	test.setPaintTicks(true);
    	test.setMajorTickSpacing(100);
    	test.setMinorTickSpacing(25);
    	test.setOrientation(JSlider.VERTICAL);
    	parent.add(test);
    	parent.pack();
		parent.setVisible(true);
    }
    
    @Override
    public String toString() {
    	return "" + getValue();
    }
    
}

