package net.judah.gui.knobs;

import static javax.swing.SwingConstants.CENTER;
import static net.judah.gui.Pastels.*;

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.seq.MidiTrack;
import net.judah.widgets.Knob;

@Getter
public class TrackSettings extends JPanel {

	private final MidiTrack track;
	private final JButton play = new JButton("Play");
	private final JButton record = new JButton("Rec");
	private final JButton mpk = new JButton("MPK");
	private final Knob velocity = new Knob();
	private final /*FileCombo*/ JComboBox<String> file = new JComboBox<>(new String[] {"Sleepwalk", "AirOnG"});
	
	public TrackSettings(MidiTrack t) {
		track = t;
		play.addActionListener(e->{
			track.setActive(track.isActive() || track.isOnDeck() ? false : true);});
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		JLabel name = new JLabel(track.getName(), CENTER);
		name.setFont(Gui.BOLD13);
		add(Gui.wrap(name, file));

		
		
		JPanel btns = new JPanel(new GridLayout(0, 4));
		btns.add(play); btns.add(record); btns.add(mpk); btns.add(velocity);
		add(btns); 

	}

	public void update() {
		// previous next current
		play.setBackground(track.isActive() ? GREEN : null);
		record.setBackground(track.isRecord() ? RED : null);
		// TODO mpk
	}
	
}
