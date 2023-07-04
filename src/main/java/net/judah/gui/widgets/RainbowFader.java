package net.judah.gui.widgets;

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

	public static final Dimension MINIMUM = new Dimension(32, 66);
	public static final int THUMB_WIDTH = 15; 
	public static final int THUMB_HEIGHT = 25; 

	static final float slope = 0.04f;
    
	public static Color chaseTheRainbow(float amt) {
		return chaseTheRainbow((int)(amt * 100));
	}
	
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
    
    static UIDefaults newDefaults = new UIDefaults();
    static {
    	int round = 10;
    	newDefaults.put("Slider.thumbWidth", THUMB_WIDTH);
        newDefaults.put("Slider.thumbHeight", THUMB_HEIGHT);
        newDefaults.put("Slider:SliderThumb.backgroundPainter", new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent comp, int w, int h) {
            	g.setColor(Color.DARK_GRAY);
            	g.fillRoundRect(0, 0, w, h-1, round, round);
            	g.setColor(Color.BLACK);
            	g.fillRoundRect(1, 0, w-2, h-3, round, round);

            	g.setColor(chaseTheRainbow(((JSlider)comp).getValue()));
            	g.fillRoundRect(1, 1, w-3, h-4, round, round);
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
        setPreferredSize(MINIMUM); 
        setMinimumSize(MINIMUM);
        
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

