package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JToggleButton;

import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;

public class StartBtn extends JToggleButton implements TimeListener {

	private final JudahClock clock;
	
	public StartBtn(JudahClock clock) {
		this.clock = clock;
        setText();
        setSelected(clock.isActive());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON3) 
        			clock.reset(); // debug
            	else if (clock.isActive()) 
                    clock.end();
                else
                    clock.begin(); }});
        clock.addListener(this);
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.STEP) {
			int step = 100 * (int)value / clock.getSteps();
			setBackground(RainbowFader.chaseTheRainbow(step));
		}
		else if (prop == Property.TRANSPORT) 
			setText();
	}
	
	public void setText() {
		setText(clock.isActive() ? "Stop" : "Play");
	}
}
