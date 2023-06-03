package net.judah.gui.knobs;

import static net.judah.gui.Pastels.*;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import lombok.Getter;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumType;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.settable.*;
import net.judah.gui.widgets.*;
import net.judah.mixer.Channel;
import net.judah.seq.Gate;
import net.judah.seq.MidiTrack;
import net.judah.seq.Mode;
import net.judah.seq.Seq;
import net.judah.util.Constants;
import net.judah.util.Folders;

@Getter // TODO MouseWheel listener -> change pattern 
public class TrackKnobs extends KnobPanel {

	private final KnobMode knobMode = KnobMode.Track;
	private final MidiTrack track;
	private final Seq seq;
	private final JPanel titleBar = new JPanel();
	private final JButton play = new JButton("Play");
	private final JButton record = new JButton("Rec");
	private final JButton mpk = new JButton("MPK");
	
	private final Folder file;
	private final Program progChange;
	private final GateCombo gate;
	private final Slider velocity = new Slider(null);
	
	private final CueCombo cue;
	private final Cycle cycle;
	private final Bar current;
	private final Launch launch;

	private final JLabel total;
	private ModeCombo mode;
	private JComboBox<Integer> octaves;
	private Slider snare, clap; // clap = HiCut?
	private final GridBagLayout gridbag = new GridBagLayout();
	private final JPanel grid = new JPanel();
	private final GridBagConstraints c = new GridBagConstraints();
	private final PatternLauncher patterns;
	
	private class Lbl extends JLabel {
		Lbl(String lbl) {
			super(lbl, JLabel.RIGHT);
			setFont(Gui.FONT11);
		}
	}
	
	@Override public Component installing() {
		return titleBar;
	}
	
	// transpose amount?
	public TrackKnobs(MidiTrack t, Seq seq) {
		super(t.getName());
		this.track = t;
		this.seq = seq;
		titleBar();
		
		///////////////////////////
		file = new Folder(track);
		play.addActionListener(e->{
			track.setActive(track.isActive() || track.isOnDeck() ? false : true);});
		progChange = new Program(track.getMidiOut(), track.getCh());
		current = new Bar(track);
		launch = new Launch(track);
		cycle = new Cycle(track);
		cue = new CueCombo(track);
		gate = new GateCombo(track);
		total = new JLabel("of " + track.frames(), JLabel.CENTER);
		Arrow left = new Arrow(Arrow.WEST, e->track.setFrame(track.getFrame() - 1));
		Arrow right = new Arrow(Arrow.EAST, e->track.setFrame(track.getFrame() + 1));
		Btn home = new Btn(UIManager.getIcon("FileChooser.homeFolderIcon"), e->track.setFrame(0));
		
		if (track.isDrums()) {
			snare = new Slider(null);
			snare.setValue(((DrumKit)track.getMidiOut()).getSamples()[7].getVolume());
			snare.addChangeListener(e->drumVol(snare.getValue(), DrumType.Snare));
			clap = new Slider(null);
			clap.setValue(((DrumKit)track.getMidiOut()).getSamples()[1].getVolume());
			clap.addChangeListener(e->drumVol(clap.getValue(), DrumType.Clap));
		}
		else {
			mode = new ModeCombo(track);
			octaves = new Integers(1, 5);
			octaves.setSelectedItem(track.getArp().getRange() / 12);
			octaves.addActionListener(e->track.getArp().setRange((Integer)octaves.getSelectedItem() * 12));
		}
		//////////////////////////
		grid.setLayout(gridbag);
		Gui.resize(current, Size.MICRO);
		Gui.resize(launch, Size.MICRO);
        
        c.ipadx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);

        float weight = 7f;
        Lbl lbl;

        c.gridy = 0;
        c.gridx = -1;
        lbl = new Lbl("File");
        c.weightx = 1 / weight;
        install(lbl);
        c.weightx = 3 / weight;
        install(file);
        c.weightx = 1 / weight;
        lbl = new Lbl("Amp ");
        install(lbl);
        c.weightx = 2 / weight;
        install(velocity);
        
        c.gridy = 1;
        c.gridx = -1;
        lbl = new Lbl("Preset");
        c.weightx = 1 / weight;
        install(lbl);
        c.weightx = 3 / weight;
        install(progChange);
        c.weightx = 1 / weight;
        lbl = new Lbl(track.isDrums() ? "Snare " : "Mode");
        install(lbl);
        c.weightx = 2 / weight;
        install(track.isDrums() ? snare : mode);
        
        c.gridy = 2;
        c.gridx = -1;
        lbl = new Lbl("Cycle");
        c.weightx = 1 / weight;
        install(lbl);
        c.weightx = 3 / weight;
        install(cycle);
        c.weightx = 1 / weight;
        lbl = new Lbl(track.isDrums() ? "Clap " : "Octs");
        install(lbl);
        c.weightx = 2 / weight;
        install(track.isDrums() ? clap : octaves);
        
        c.gridy = 3;
        c.gridx = -1;
        lbl = new Lbl("Launch");
        c.weightx = 1 / weight;
        install(lbl);
        c.weightx = 3 / weight;
        install(Gui.wrap(home, launch, total));
        c.weightx = 1 / weight;
        lbl = new Lbl("Gate");
        install(lbl);
        c.weightx = 2 / weight;
        install(gate);

        c.gridy = 4;
        c.gridx = -1;
        lbl = new Lbl("Current");
        c.weightx = 1 / weight;
        install(lbl);
        c.weightx = 3 / weight;
        install(Gui.wrap(left, current, right));
        c.weightx = 1 / weight;
        lbl = new Lbl("Cue");
        install(lbl);
        c.weightx = 2 / weight;
        install(cue);
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(grid);
        patterns = new PatternLauncher(track);
        add(patterns);
        add(Box.createVerticalGlue());
	}

	private void install(Component item) {
		c.gridx = c.gridx + 1;
		gridbag.setConstraints(item, c);
		grid.add(item);
	}
		

	private void titleBar() {
		JComboBox<MidiTrack> tracks = new JComboBox<>(seq.getTracks().toArray(new MidiTrack[seq.numTracks()]));
		ActionListener tracker = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				seq.getTracks().setCurrent((MidiTrack)tracks.getSelectedItem());
				tracks.removeActionListener(this);
				tracks.setSelectedItem(track);
				tracks.addActionListener(this);
			}
		};
		tracks.setSelectedItem(track);
		tracks.addActionListener(tracker);
		tracks.setFont(Gui.BOLD);
		Gui.resize(tracks, Size.COMBO_SIZE);
		
		titleBar.add(tracks);
		titleBar.add(play);
		titleBar.add(new FxButton((Channel)track.getMidiOut()));
	}
	
	private void drumVol(int val, DrumType type) {
		((DrumKit)track.getMidiOut()).getSamples()[type.ordinal()].getGain().set(Gain.VOLUME, val);
	}
	
	@Override
	public void update() {
		play.setBackground(track.isActive() ? GREEN : null);
		record.setBackground(track.getRecorder().isActive() ? RED : null);
		total.setText("/" + track.frames());
		
		if (track.isDrums()) {
			int vol = ((DrumKit)track.getMidiOut()).getSamples()[DrumType.Snare.ordinal()].getVolume();
			if (snare.getValue() != vol)
				snare.setValue(vol);
			vol = ((DrumKit)track.getMidiOut()).getSamples()[DrumType.Clap.ordinal()].getVolume();
			if (clap.getValue() != vol)
				clap.setValue(vol);
		}
		else if ((Integer)octaves.getSelectedItem() * 12 != track.getArp().getRange() ) 
				octaves.setSelectedItem(track.getArp().getRange() / 12);
		if (velocity.getValue() != (int)(track.getAmp() * 100))
			velocity.setValue((int) (track.getAmp() * 100));
		patterns.update();
	}

	@Override
	public boolean doKnob(int knob, int data2) {
		switch (knob) {
			case 0: // tracknum
				seq.getTracks().setCurrent(seq.get(Constants.ratio(data2, seq.numTracks() - 1)));
				return true;
			case 1: // file (settable)
						File x = (File)Constants.ratio(data2, Folders.sort(track.getFolder()));
						file.midiShow(x);
				return true;
			case 2: // preset (settable)
				progChange.midiShow(Constants.ratio(data2 - 1, track.getMidiOut().getPatches()).toString());
				return true;
			case 3: // amp 
				track.setAmp(data2 * 0.01f);
				return true;
			case 4: // current
				track.setFrame(Constants.ratio(data2, track.frames()));
				return true;
			case 5: // gate
				track.setGate((Gate)Constants.ratio(data2, Gate.values()));
				return true;
			case 6: // snare/mode
				if (track.isSynth()) 
					mode.midiShow((Mode)Constants.ratio(data2, Mode.values()));
				else 
					drumVol(data2, DrumType.Snare);
				return true;
			case 7: // clap/octaves
				if (track.isSynth())
					Constants.execute(()-> octaves.setSelectedIndex(Constants.ratio(data2, 4)));
				else
					drumVol(data2, DrumType.Clap);
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
