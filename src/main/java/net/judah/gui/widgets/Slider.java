package net.judah.gui.widgets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.event.ChangeListener;

import net.judah.fx.Effect;
import net.judah.gui.Updateable;
import net.judah.omni.Icons;

/**Source: ThemeDemo https://jasperpotts.com/blog/2008/08/skinning-a-slider-with-nimbus/
 * @author Created by Jasper Potts (May 7, 2008) */

public class Slider extends JSlider {

    static Image fader = Icons.get("slider.png").getImage();
    static UIDefaults sliderDefaults = new UIDefaults();

    static {
        sliderDefaults.put("Slider.thumbWidth", 12);
        sliderDefaults.put("Slider.thumbHeight", 22);
        sliderDefaults.put("Slider:SliderThumb.backgroundPainter", (Painter<JComponent>) (g, c, w, h) -> g.drawImage(fader, 0, 0, null));
        sliderDefaults.put("Slider:SliderTrack.backgroundPainter", (Painter<JComponent>) (g, c, w, h) -> {
		    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		    g.setStroke(new BasicStroke(2f));
		    g.setColor(Color.WHITE);
		    g.fillRoundRect(0, 8, w-1, 4, 4, 4);
		    g.setColor(Color.GRAY);
		    g.drawRoundRect(0, 8, w-1, 4, 4, 4);
		});
    }

    public Slider(ChangeListener l) {
        this(0, 100, l, null);
    }

    public Slider(int min, int max, ChangeListener l, String tooltip) {
        this(min, max, l);
        if (tooltip != null)
            setToolTipText(tooltip);
    }

    public Slider(int min, int max, ChangeListener l) {
        super(min, max);
        putClientProperty("Nimbus.Overrides",sliderDefaults);
        putClientProperty("Nimbus.Overrides.InheritDefaults",false);
        if (l != null)
        	addChangeListener(l);
        setPreferredSize(new Dimension(100, 38));
    }

    public static class FxSlider extends JSlider implements Updateable {
    	private final Effect fx;
    	private final int idx;
    	public FxSlider(Effect fx, int ordinal, String tooltip) {
    		this.fx = fx;
    		this.idx = ordinal;
    		update();
    		addChangeListener(e->fx.set(idx, getValue()));
    		setToolTipText(tooltip);
    	}
    	@Override public void update() {
    		if (getValue() != fx.get(idx))
    			setValue(fx.get(idx));
    	}

    }

}

