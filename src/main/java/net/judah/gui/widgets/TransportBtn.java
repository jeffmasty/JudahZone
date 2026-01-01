package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JToggleButton;

import judahzone.api.TimeListener;
import judahzone.api.Notification.Property;
import judahzone.util.Rainbow;
import net.judah.midi.JudahClock;

public class TransportBtn extends JToggleButton implements TimeListener {

	private final JudahClock clock;

	public TransportBtn(JudahClock clock) {
		this.clock = clock;
        setText();
        setSelected(clock.isActive());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON3)
        			clock.reset(); // debug
            	else clock.toggle();
            }});
        clock.addListener(this);
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.STEP) {
			int step = 100 * (int)value / clock.getSteps();
			setBackground(Rainbow.get(step));
		}
		else if (prop == Property.TRANSPORT)
			setText();
	}

	public void setText() {
		setText(clock.isActive() ? "Stop" : "Play");
	}
}
