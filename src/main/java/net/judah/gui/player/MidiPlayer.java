package net.judah.gui.player;

import static javax.swing.SwingConstants.CENTER;
import static net.judah.gui.Pastels.*;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobPanel;
import net.judah.midi.ProgChange;
import net.judah.seq.Bar;
import net.judah.seq.Cue;
import net.judah.seq.Cycle;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.util.Constants;
import net.judah.widgets.Knob;

@Getter // TODO MouseWheel listener -> change pattern 
public class MidiPlayer extends KnobPanel {

	private final MidiTrack track;
	private final Seq seq;
	private final JButton play = new JButton("Play");
	private final JButton record = new JButton("Rec");
	private final JButton mpk = new JButton("MPK");
	private final JComboBox<Cue> cue = new JComboBox<Cue>(Cue.values());
	private final JComboBox<Cycle> cycle = new JComboBox<Cycle>(Cycle.values());
	private final ProgChange progChange;
	private final /*FileCombo*/ JComboBox<String> file = new JComboBox<>(new String[] {"Sleepwalk", "AirOnG"});
	private final Knob velocity = new Knob();
	private final JComboBox<Bar> current;
	private final JLabel previous = new JLabel("(TD)", CENTER);
	private final JLabel next = new JLabel("0 | 1", CENTER);
	private final JPanel titleBar = new JPanel(new GridLayout());
	
	private JPanel settings = new JPanel(new GridLayout(0, 3));
	private JPanel bars = new JPanel(new GridLayout(0, 3));
	
	@Override
	public Component installing() {
		return titleBar;
	}
		
	//  namePlay preset |  Cycle [ABCD]    [CUE]
	//	file     vol    | (prev) [Cur]   next|after
	public MidiPlayer(MidiTrack t, Seq seq, boolean showPatterns) {
		super(t.getName());
		this.track = t;
		this.seq = seq;
		progChange = new ProgChange(track.getMidiOut(), track.getCh());
		progChange.setPreferredSize(Size.COMBO_SIZE);
		progChange.setMaximumSize(Size.COMBO_SIZE);
		
		play.addActionListener(e->{
			track.setActive(track.isActive() || track.isOnDeck() ? false : true);});

		current = new JComboBox<>(t.toArray(new Bar[track.size()]));

		settings.setOpaque(true);
		settings.setBackground(BUTTONS);
		JLabel name = new JLabel(track.getName(), CENTER);
		name.setFont(Constants.Gui.BOLD13);
		settings.add(name);
		settings.add(cycle);
		settings.add(cue);

		bars.setOpaque(true);
		bars.setBackground(BUTTONS);
		bars.add(previous);
		bars.add(current);
		bars.add(next);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.PAGE_AXIS));
		top.add(settings);
		top.add(bars);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.PAGE_AXIS));
		bottom.add(Constants.wrap(file, progChange));
		JPanel btns = new JPanel(new GridLayout(0, 4));
		btns.add(play); btns.add(record); btns.add(mpk); btns.add(velocity);
		bottom.add(btns); // Constants.wrap(play, record, mpk, velocity));
		setBorder(Constants.Gui.NONE);
		setLayout(new GridLayout(0, 1));
		add(top);
		add(bottom);
		if (showPatterns) {
			JComboBox<MidiTrack> tracks = new JComboBox<>(seq.getTracks().toArray(new MidiTrack[seq.getTotal()]));
			ActionListener tracker = new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					seq.setCurrent((MidiTrack)tracks.getSelectedItem());
					tracks.removeActionListener(this);
					tracks.setSelectedItem(track);
					tracks.addActionListener(this);
				}
			};
			tracks.setSelectedItem(track);
			tracks.addActionListener(tracker);
			titleBar.add(tracks);
			
			add(new BarRoom(track));
		}
	}

	@Override
	public void update() {

		settings.setBackground(seq.getCurrent() == track ? MY_GRAY : BUTTONS);
		bars.setBackground(settings.getBackground());
		
		// previous next current
		play.setBackground(track.isActive() ? GREEN : null);
		record.setBackground(track.isRecord() ? RED : null);
		// mpk
// 		setBorder(seq.getCurrent() == track ? Constants.Gui.HIGHLIGHT : Constants.Gui.NON);
	}

	@Override
	public boolean doKnob(int knob, int data2) {
		switch (knob) {
			case 0: // tracknum
				int num = Constants.ratio(data2, seq.getTotal() - 1);
				seq.setCurrent(seq.get(num));
				return true;
			case 1: // file (settable)
				File[] folder = track.getFolder().listFiles();
				int idx = Constants.ratio(data2, folder.length + 1);
				file.setSelectedIndex(idx);
				return true;
			case 2: // pattern
//TODO				track.setCurrent(Constants.ratio(data2 -1, track));
				return true;
			case 3: 
				track.setGain(data2 * 0.01f);
				return true;
			case 4: 
				cue.setSelectedIndex(Constants.ratio(data2 -1, cue.getItemCount()));
				return true;
			case 5: 
				cycle.setSelectedIndex(Constants.ratio(data2 - 1, Cycle.values().length));
				return true;
			case 6: // midiOut
				
				return true;
			case 7: 
				progChange.setSelectedIndex(Constants.ratio(data2, progChange.getItemCount() - 1));
				return true;
		}
		return false;		
	}

	@Override
	public void pad1() {
		MainFrame.setFocus(track);
	}

	@Override
	public void pad2() {
		
	}
	
}
