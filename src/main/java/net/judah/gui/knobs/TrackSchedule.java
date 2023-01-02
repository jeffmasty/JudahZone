package net.judah.gui.knobs;

import static javax.swing.SwingConstants.CENTER;

import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.midi.ProgChange;
import net.judah.seq.Cue;
import net.judah.seq.Cycle;
import net.judah.seq.MidiTrack;

@Getter 
public class TrackSchedule extends JPanel {

	private final MidiTrack track;
	private final JPanel top = new JPanel(new GridLayout(0, 3));
	private final JPanel bottom = new JPanel();
	private final JComboBox<Cue> cue = new JComboBox<Cue>(Cue.values());
	private final JComboBox<Cycle> cycle = new JComboBox<Cycle>(Cycle.values());
	private final JComboBox<Integer> current = new JComboBox<>();
	private final JLabel previous = new JLabel("(TD)", CENTER);
	private final JLabel next = new JLabel("0 | 1", CENTER);
	private final ProgChange progChange;

	
	public TrackSchedule(MidiTrack t) {
		track = t;
		
		progChange = new ProgChange(t.getMidiOut(), t.getCh());
		setOpaque(true);
		setBackground(Pastels.BUTTONS);
		setLayout(new GridLayout(2, 3));
		add(cycle);
		add(cue);
		add(progChange);
		add(previous);
		add(current);
		add(next);

	}

	public void update() {
		
		//setBackground(seq.getCurrent() == track ? MY_GRAY : BUTTONS);
		// bars.setBackground(settings.getBackground());

	}
	
}
