package net.judah.gui.knobs;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumType;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.PlayWidget;
import net.judah.gui.Size;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.*;
import net.judah.mixer.Channel;
import net.judah.seq.Seq;
import net.judah.seq.arp.Mode;
import net.judah.seq.track.Gate;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.Programmer;
import net.judah.synth.JudahSynth;
import net.judah.util.Constants;
import net.judah.util.Folders;

@Getter // TODO MouseWheel listener -> change pattern 
public class TrackKnobs extends KnobPanel {

	private final KnobMode knobMode = KnobMode.Track;
	private final MidiTrack track;
	private final Seq seq;
	private final JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	
	private final Folder file;
	private final Program progChange;
	private final GateCombo gate;
	private final TrackAmp velocity;
	private final CueCombo cue;
	private ModeCombo mode;
	private JComboBox<Integer> octaves;
	private Slider snare, clap; // clap = HiCut?
	private final GridBagLayout gridbag = new GridBagLayout();
	private final GridBagConstraints c = new GridBagConstraints();
	private final PatternLauncher patterns;
	@Override public Component installing() { return titleBar; }
	
	private class Lbl extends JLabel {
		Lbl(String lbl) {
			super(lbl, JLabel.RIGHT);
			setFont(Gui.FONT11);
			Gui.resize(this, Size.MICRO);
		}
	}
	
	// transpose amount?
	public TrackKnobs(MidiTrack t, Seq seq) {
		this.track = t;
		this.seq = seq;
		titleBar();
		
		///////////////////////////
		file = new Folder(track);
		progChange = new Program(track);
        patterns = new PatternLauncher(track);
		cue = new CueCombo(track);
		gate = new GateCombo(track);
		velocity = new TrackAmp(track);
		
		if (track.isDrums()) {
			snare = new Slider(null);
			snare.setValue(((DrumKit)track.getMidiOut()).getSamples()[7].getVolume());
			snare.addChangeListener(e->drumVol(snare.getValue(), DrumType.Snare));
			clap = new Slider(null);
			clap.setValue(((DrumKit)track.getMidiOut()).getSamples()[1].getVolume());
			clap.addChangeListener(e->drumVol(clap.getValue(), DrumType.Clap));
			Gui.resize(snare, Size.COMBO_SIZE);
			Gui.resize(clap, Size.COMBO_SIZE);
		}
		else {
			mode = new ModeCombo(((PianoTrack)track));
			octaves = new Integers(1, 5);
			octaves.setSelectedItem(((PianoTrack)track).getArp().getRange() / 12);
			octaves.addActionListener(e->((PianoTrack)track).getArp().setRange((Integer)octaves.getSelectedItem() * 12));
			Gui.resize(mode, Size.COMBO_SIZE);
			Gui.resize(octaves, Size.COMBO_SIZE);
		}

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
        top.add(new PlayWidget(track, "  Play    "));
        top.add(Box.createHorizontalStrut(14));
        top.add(new Programmer(track));

		JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(new Lbl("File"));
		column.add(new Lbl("Preset"));
		column.add(new Lbl("Cue"));
		JPanel fx = new JPanel();
		fx.setLayout(new BoxLayout(fx, BoxLayout.LINE_AXIS));
		fx.add(Box.createHorizontalGlue());
		fx.add(new FxButton((Channel)track.getMidiOut()));
		column.add(fx);
		settings.add(column);

		column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(Gui.resize(file, Size.COMBO_SIZE));
		column.add(Gui.resize(progChange, Size.COMBO_SIZE));
		column.add(Gui.resize(cue, Size.COMBO_SIZE));
		JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		btns.add(new Btn("Rec", e->{})); 
		btns.add(new Btn("MPK", e->{})); // TODO
        if (track.isDrums())
        	btns.add(new Btn("Kit", e->MainFrame.setFocus(JudahZone.getDrumMachine().getKnobs((DrumKit)track.getMidiOut()))));
        else if (track.getMidiOut() == JudahZone.getSynth1() || track.getMidiOut() == JudahZone.getSynth2())
        	btns.add(new Btn("DCO", e->MainFrame.setFocus(((JudahSynth)track.getMidiOut()).getSynthKnobs())));

		column.add(btns);
		settings.add(column);
		
		column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(new Lbl("Amp "));
		column.add(new Lbl(track.isDrums() ? "Snare " : "Mode"));
		column.add(new Lbl(track.isDrums() ? "Clap " : "Octs"));
		column.add(new Lbl("Gate"));
		settings.add(column);
		
		column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(Gui.resize(velocity, Size.COMBO_SIZE));
		column.add(track.isDrums() ? snare : mode);
		column.add(track.isDrums() ? clap : octaves);
		column.add(Gui.resize(gate, Size.COMBO_SIZE));
		settings.add(column);
		
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(patterns);
        add(top);
        add(settings);
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
		titleBar.add(new Btn(Icons.SAVE, e->track.save()));
		titleBar.add(new Btn(Icons.HOME, e->track.toFrame(0)));
		titleBar.add(new Btn(Icons.NEW_FILE, e->track.toFrame(track.frames() + 1))); 
	}
	
	private void drumVol(int val, DrumType type) {
		((DrumKit)track.getMidiOut()).getSamples()[type.ordinal()].getGain().set(Gain.VOLUME, val);
	}
	
	@Override public void update() {
		
		if (track.isDrums()) {
			int vol = ((DrumKit)track.getMidiOut()).getSamples()[DrumType.Snare.ordinal()].getVolume();
			if (snare.getValue() != vol)
				snare.setValue(vol);
			vol = ((DrumKit)track.getMidiOut()).getSamples()[DrumType.Clap.ordinal()].getVolume();
			if (clap.getValue() != vol)
				clap.setValue(vol);
		}
		else if ((Integer)octaves.getSelectedItem() * 12 != ((PianoTrack)track).getArp().getRange() ) 
				octaves.setSelectedItem(((PianoTrack)track).getArp().getRange() / 12);
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
				track.toFrame(Constants.ratio(data2, track.frames()));
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
					Constants.execute(()-> octaves.setSelectedIndex(Constants.ratio(data2, 3)));
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
