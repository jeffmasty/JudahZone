package net.judah.seq.beatbox;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import net.judah.drumkit.DrumType;
import net.judah.gui.Actionable;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.TrackMenu;

public class DrumMenu extends TrackMenu {

	private final DrumZone tab;
	private final DrumTrack track;

	public DrumMenu(BeatBox drumz, DrumZone tab) {

		super(drumz);
		setMaximumSize(new Dimension(3000, Size.KNOB_HEIGHT));

		this.tab = tab;
		this.track = (DrumTrack)drumz.getTrack();
		add(velocity);
		file.add(new Actionable("Record On/Off", e->track.setCapture(!track.isCapture())));
		tools.add(new Actionable("Remap", e->MainFrame.setFocus(new RemapView(drumz))));
		tools.add(new Actionable("Clean", e->drumz.clean()));
		tools.add(new Actionable("Condense", e->drumz.condense()));

		JMenu cc = new JMenu("CC on");
		ButtonGroup ccMap = new ButtonGroup();
		for (DrumType t : DrumType.values()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(t.name());
			ccMap.add(item);
			cc.add(item);
			if (t == DrumType.Bongo)
				item.setSelected(true);
			item.addActionListener(e->drumz.setCCType(t));
		}
		tools.add(cc);
	}

	@Override public void mousePressed(MouseEvent e) {
		if (tab.getCurrent() == track)
			return;
		tab.setCurrent(track); // drums only
		update();
	}
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

}
