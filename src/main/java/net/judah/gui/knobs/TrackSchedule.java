package net.judah.gui.knobs;

import static javax.swing.SwingConstants.CENTER;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.settable.Bar;
import net.judah.gui.settable.Cue;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.Btn;
import net.judah.seq.MidiTrack;

@Getter 
public class TrackSchedule extends JPanel {

	private final MidiTrack track;
	private final JPanel top = new JPanel(new GridLayout(0, 3));
	private final JPanel bottom = new JPanel();
	private final Cue cue;
	private final Cycle cycle;
	private final Bar frames;
	private final JLabel total;
	private final Program progChange;

	public TrackSchedule(MidiTrack t) {
		track = t;
		frames = new Bar(track);
		progChange = new Program(track.getMidiOut(), track.getCh());
		cycle = new Cycle(track);
		cue = new Cue(track);
		total = new JLabel("" + track.frames(), CENTER);
		Arrow left = new Arrow(Arrow.WEST, e->track.setFrame(track.getFrame() - 1));
		Arrow right = new Arrow(Arrow.EAST, e->track.setFrame(track.getFrame() + 1));
		Btn home = new Btn(UIManager.getIcon("FileChooser.homeFolderIcon"), e->track.setFrame(0));

		setOpaque(true);
		setBackground(Pastels.BUTTONS);
		setLayout(new GridLayout(2, 3));
		add(progChange);
		add(cycle);
		add(cue);
		add(Gui.wrap(new JLabel("    "), home, left));
		add(frames);
		add(Gui.wrap(right, total, new JLabel("    ")));
	}

	public void update() {
		total.setText("" + track.frames());
	}
	
}
