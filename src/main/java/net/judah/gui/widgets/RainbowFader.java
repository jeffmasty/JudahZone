package net.judah.gui.widgets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeListener;

import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.util.Rainbow;

/**An extended JSlider that paints the Thumb chromatically based on the slider position.
 * <br/><br/>
 * Source: <a href="https://jasperpotts.com/blog/2008/08/skinning-a-slider-with-nimbus/">Skinning a Slider with Nimbus</a>
 * @author Jasper Potts (May 7, 2008) SliderSkinDemo.java
 * @author Tsukino, Fumino and Jeff Masty (Nov 28, 2021) */
public class RainbowFader extends JSlider {

	public static final int THUMB_WIDTH = 14;
	public static final int THUMB_HEIGHT = 28;

	static final float slope = 0.04f;

//	public static Color chaseTheRainbow(float amt) {
//		return chaseTheRainbow((int)(amt * 100));
//	}
//
//    public static Color chaseTheRainbow(int percent) {
//    	if (percent < 0 || percent > 100) return Color.BLACK;
//    	float red = 0;
//    	float green = 0;
//    	float blue = 0;
//
//    	if (percent < 25) { // green up
//    		blue = 1;
//    		green = percent * slope ;
//    	}
//    	else if (percent < 50) { // blue down
//    		green = 1;
//    		blue = 2 - slope * percent;
//    	}
//    	else if (percent < 75) { // red up
//    		green = 1;
//    		red = (percent - 50) * slope;
//    	} else { // green down
//    		red = 1;
//    		green = 4 - slope * percent;
//    	}
//    	return new Color(red, green, blue);
//    }

    static UIDefaults newDefaults = new UIDefaults();
    static {
    	int round = 12;
    	newDefaults.put("Slider.thumbWidth", THUMB_WIDTH);
        newDefaults.put("Slider.thumbHeight", THUMB_HEIGHT + 1);
        newDefaults.put("Slider:SliderThumb.backgroundPainter", (Painter<JComponent>) (g, comp, w, h) -> {

			// coords rotated
			g.setColor(Color.DARK_GRAY);
			g.fillRoundRect(0, 0, w, h-1, round, round);

			g.setColor(Rainbow.get(((JSlider)comp).getValue()));
			g.fillRoundRect(1, 2, w-4, h-5, round, round);

			// spicy highlight
			g.setColor(Color.GRAY);
			g.drawLine(w/2 + 1, 4, w/2 + 1, h-4);
			g.drawLine(w/2, 3, w/2, h-3);
		});
        newDefaults.put("Slider:SliderTrack.backgroundPainter", (Painter<JComponent>) (g, c, w, h) -> {
		    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		    g.setStroke(new BasicStroke(2f));
		    g.setColor(Pastels.MY_GRAY);
		    g.fillRoundRect(0, 12, w-1, 4, 4, 4);
		    g.setColor(Color.GRAY);
		    g.drawRoundRect(0, 12, w-1, 4, 4, 4);
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
        Gui.resize(this, Size.FADER_SIZE);
        setOpaque(true);
    }

    public static void main2(String[] args) {
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

