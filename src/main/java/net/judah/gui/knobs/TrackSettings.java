package net.judah.gui.knobs;

import static net.judah.gui.Pastels.*;

import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.settable.Folder;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.Knob;
import net.judah.mixer.Channel;
import net.judah.seq.MidiTrack;

@Getter
public class TrackSettings extends JPanel {

	private final MidiTrack track;
	private final JButton play = new JButton("Play");
	private final JButton record = new JButton("Rec");
	private final JButton mpk = new JButton("MPK");
	private final Knob velocity = new Knob();
	private final Folder file;
	
	public TrackSettings(MidiTrack t) {
		track = t;
		file = new Folder(track);
		play.addActionListener(e->{
			track.setActive(track.isActive() || track.isOnDeck() ? false : true);});
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(new JLabel("  File: "));
		top.add(file); 
		top.add(new FxButton((Channel)track.getMidiOut()));
		top.add(velocity);
		add(top);
		
		JPanel btns = new JPanel(new GridLayout(0, 3));
		btns.add(play); btns.add(record); btns.add(mpk); 
		add(btns); 
		add(Box.createVerticalGlue());

	}

	public void update() {
		// previous next current
		play.setBackground(track.isActive() ? GREEN : null);
		record.setBackground(track.isRecording() ? RED : null);
		// TODO mpk
	}
	
}
