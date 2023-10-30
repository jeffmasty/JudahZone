package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import net.judah.gui.Pastels;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;

public class TogglePreset extends JButton {
	
	private final Channel channel;
	private final Looper looper;
	
	public TogglePreset(Channel channel, Looper looper) {
		super(" fx ");
		this.channel = channel;
		this.looper = looper;
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent me) {
				if (SwingUtilities.isRightMouseButton(me)) 
					looper.syncFx(channel);
				else 
					channel.toggleFx();
			}
		});
		update();
	}
	
	
	public void update() {
		setBackground(channel.isPresetActive() ? Pastels.BLUE : 
			looper.getFx().contains(channel) ? Pastels.YELLOW : null);
	}
	
}
