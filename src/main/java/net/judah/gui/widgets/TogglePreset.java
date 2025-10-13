package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import net.judah.JudahZone;
import net.judah.gui.Pastels;
import net.judah.mixer.Channel;

public class TogglePreset extends JButton {

	private final Channel channel;

	public TogglePreset(Channel channel) {
		super(" fx ");
		this.channel = channel;
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent me) {
				if (SwingUtilities.isRightMouseButton(me))
					JudahZone.getLooper().syncFx(channel);
				else
					channel.toggleFx();
			}
		});
	}

	public void update() {
		setBackground(channel.isPresetActive() ? Pastels.BLUE :
			JudahZone.getLooper().getFx().contains(channel) ? Pastels.YELLOW : null);
	}

}
