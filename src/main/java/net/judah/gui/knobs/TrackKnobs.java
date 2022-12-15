package net.judah.gui.knobs;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.player.BarRoom;
import net.judah.seq.Cue;
import net.judah.seq.Cycle;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;
import net.judah.widgets.Slider;

@Getter 
public class TrackKnobs extends KnobPanel {
	
	private final MidiTrack track;
	private final /*FileCombo*/ JComboBox<String> file = new JComboBox<>(new String[] {"Sleepwalk", "AirOnG"});
	private final JComboBox<MidiTrack> tracks; 
	private final JPanel upper, lower;
	
	@Override
	public boolean doKnob(int idx, int value) {
		// TODO
		return false;
	}

	@Override
	public void update() {
		// TODO
	}
	
	public TrackKnobs(MidiTrack t, List<MidiTrack> seq) {
		super(t.getName());
		this.track = t;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		upper = new JPanel(/* new GridLayout(1, 3, 5, 1) */);
		upper.add(file);
		upper.add(new BarRoom(track));
		upper.add(new Slider(null));
		add(upper);
		
		lower = new JPanel(new GridLayout(1, 4, 1, 1));
		lower.add(new JComboBox<Cue>(Cue.values()));
		lower.add(new JComboBox<Cycle>(Cycle.values()));
		lower.add(new JComboBox<>(new String[] {"Synth1", "Synth2"}));
		lower.add(new JComboBox<>(new String[] {"FeelGood", "Drops1", "Drops2"}));
		JPanel middle = new JPanel(new GridLayout(1, 4, 1, 1));
		middle.add(new JLabel("Cue", CENTER));
		middle.add(new JLabel("Cycle", CENTER));
		middle.add(new JLabel("MidiOut", CENTER));
		middle.add(new JLabel("Preset", CENTER));

		add(upper);
		add(middle);
		add(lower);

		tracks = new JComboBox<>();
		for (MidiTrack track : seq)
			tracks.addItem(track);
		tracks.setSelectedItem(track);
		
	}

	@Override
	public Component installing() {
		return Constants.wrap(tracks);
	}
	

}
