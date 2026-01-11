package net.judah.seq.beatbox;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import judahzone.gui.Actionable;
import judahzone.gui.Gui;
import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.TrackMenu;

public class DrumMenu extends TrackMenu implements Gui.Mouser {

	private final DrumZone tab;
	private final BeatBox drumz;
	private final DrumTrack track;


	public DrumMenu(BeatBox drumz, DrumZone tab) {
		super(drumz);
		this.drumz = drumz;
		this.tab = tab;
		this.track = (DrumTrack)drumz.getTrack();
		setMaximumSize(new Dimension(3000, Size.KNOB_HEIGHT));
		addMouseListener(this);
    	file.add(tools);
		file.setText(track.getName());
	    file.setFont(Gui.BOLD12);
	}

	@Override public void mousePressed(MouseEvent e) {
		if (tab.getCurrent() == track)
			return;
		tab.setCurrent(track); // drums only
		update();
	}

	 /** DrumMenu specific: CC mapping, recording toggle, remap/clean/condense tools */
	 @Override protected void childMenus() {
		file.add(new Actionable("Record On/Off", e->track.setCapture(!track.isCapture())));
		tools.add(new Actionable("Remap", e->MainFrame.setFocus(new RemapView(drumz))));
		tools.add(new Actionable("Clean", e->drumz.clean()));
		tools.add(new Actionable("Condense", e->drumz.condense()));
	    file.add(buildCCMenu());
	}

	private JMenu buildCCMenu() {
	    JMenu cc = new JMenu("CC on");
	    ButtonGroup ccMap = new ButtonGroup();
	    for (DrumType t : DrumType.values()) {
	        JRadioButtonMenuItem item = new JRadioButtonMenuItem(t.name());
	        ccMap.add(item);
	        cc.add(item);
	        if (t == DrumType.Bongo) item.setSelected(true);
	        item.addActionListener(e -> drumz.setCCType(t));
	    }
	    return cc;
	}

}
