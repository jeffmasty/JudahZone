package net.judah.gui.knobs;

import static javax.swing.SwingConstants.CENTER;

import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.gui.settable.Bar;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Program;
import net.judah.seq.MidiTrack;
import net.judah.song.Trigger;

@Getter 
public class TrackSchedule extends JPanel {

	private final MidiTrack track;
	private final JPanel top = new JPanel(new GridLayout(0, 3));
	private final JPanel bottom = new JPanel();
	private final JComboBox<Trigger> cue = new JComboBox<Trigger>(Trigger.values());
	private final Cycle cycle;
	private final Bar frames;
	
	private final JLabel previous = new JLabel("(TD)", CENTER);
	private final JLabel next = new JLabel("0 | 1", CENTER);
	
	private final Program progChange;

	public TrackSchedule(MidiTrack t) {
		track = t;
		frames = new Bar(track);
		progChange = new Program(track.getMidiOut(), track.getCh());
		cycle = new Cycle(track);
		setOpaque(true);
		setBackground(Pastels.BUTTONS);
		setLayout(new GridLayout(2, 3));
		add(cycle);
		add(cue);
		add(progChange);
		
		add(previous);
		add(frames);
		add(next);

	}

	public void update() {
		
		//setBackground(seq.getCurrent() == track ? MY_GRAY : BUTTONS);
		// bars.setBackground(settings.getBackground());

	}
	
}
