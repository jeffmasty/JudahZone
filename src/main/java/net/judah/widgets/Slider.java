package net.judah.widgets;

import java.awt.*;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.event.ChangeListener;

import net.judah.util.Icons;

/**Source: ThemeDemo https://jasperpotts.com/blog/2008/08/skinning-a-slider-with-nimbus/
 * @author Created by Jasper Potts (May 7, 2008) */

public class Slider extends JSlider {

    static Image fader = Icons.load("slider.png").getImage();
    static UIDefaults sliderDefaults = new UIDefaults();

    static {
        sliderDefaults.put("Slider.thumbWidth", 12);
        sliderDefaults.put("Slider.thumbHeight", 22);
        sliderDefaults.put("Slider:SliderThumb.backgroundPainter", new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent c, int w, int h) {
                g.drawImage(fader, 0, 0, null);
            }
        });
        sliderDefaults.put("Slider:SliderTrack.backgroundPainter", new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent c, int w, int h) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(2f));
                g.setColor(Color.WHITE);
                g.fillRoundRect(0, 8, w-1, 4, 4, 4);
                g.setColor(Color.GRAY);
                g.drawRoundRect(0, 8, w-1, 4, 4, 4);
            }
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

}

