package net.judah.gui.knobs;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.controllers.MPKmini;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.PlayWidget;
import net.judah.gui.TabZone;
import net.judah.gui.Size;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.CueCombo;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.Integers;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.TrackAmp;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.seq.Seq;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.Gate;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.Programmer;
import net.judah.synth.taco.TacoSynth;
import net.judah.util.Constants;
import net.judah.util.Folders;

// TODO MouseWheel listener -> change pattern
public class TrackKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.Track;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	@Getter private final MidiTrack track;

	private final Seq seq;
	private final Folder file;
	private final Program progChange;
	private final GateCombo gate;
	private final TrackAmp velocity;
	private final CueCombo cue;
	private final PatternLauncher patterns;

	// piano vs. drums
	private ModeCombo mode;
	private JComboBox<Integer> octaves;
	private JComboBox<DrumType> focus = new JComboBox<DrumType>(DrumType.values());
	private Slider focusVol = new Slider(null);

	private class Lbl extends JLabel {
		Lbl(String lbl) {
			super(lbl, JLabel.RIGHT);
			setFont(Gui.FONT11);
			Gui.resize(this, MICRO);
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
			focus.setSelectedItem(DrumType.Snare);
			focus.addActionListener(e->update());
			focusVol.setValue(((DrumTrack)track).getKit().getSample(
					((DrumType)focus.getSelectedItem())).getGain().get(Gain.VOLUME));
			focusVol.addChangeListener(e->drumVol(focusVol.getValue()));
		}
		else {
			mode = new ModeCombo(((PianoTrack)track));
			octaves = new Integers(1, 5);
			octaves.setSelectedItem(((PianoTrack)track).getRange() / 12);
			octaves.addActionListener(e->((PianoTrack)track).setRange((Integer)octaves.getSelectedItem() * 12));
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
		fx.add(new FxButton(track.getMidiOut()));
		column.add(fx);
		settings.add(column);

		column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(Gui.resize(file, COMBO_SIZE));
		column.add(Gui.resize(progChange, COMBO_SIZE));
		column.add(Gui.resize(cue, COMBO_SIZE));
		JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		btns.add(new Btn("Rec", e->track.setCapture(!track.isCapture())));
		btns.add(new Btn(track.isDrums() ? "Kit" : "MPK", e->pad2()));
        if (track.getMidiOut() instanceof TacoSynth)
        	btns.add(new Btn("DCO", e->MainFrame.setFocus(((TacoSynth)track.getMidiOut()).getSynthKnobs())));

		column.add(btns);
		settings.add(column);

		column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(new Lbl("Amp "));
		column.add(new Lbl("Gate"));
		column.add(new Lbl(track.isDrums() ? "Focus " : "Mode "));
		column.add(new Lbl(track.isDrums() ? "Gain " : "Octs. "));
		settings.add(column);

		column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(Gui.resize(velocity, COMBO_SIZE));
		column.add(Gui.resize(gate, COMBO_SIZE));
		column.add(Gui.resize(track.isDrums() ? focus : mode, COMBO_SIZE));
		column.add(Gui.resize(track.isDrums() ? focusVol : octaves, COMBO_SIZE));
		settings.add(column);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(patterns);
        add(top);
        add(settings);

	}

	private void drumVol(int gain) {
		DrumSample s = ((DrumTrack)track).getKit().getSample((DrumType)focus.getSelectedItem());
		s.getGain().set(Gain.VOLUME, gain);
		MainFrame.update(this);
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
		title.add(tracks);
		title.add(new Btn(Icons.SAVE, e->track.save()));
		title.add(new Btn(Icons.HOME, e->track.toFrame(0)));
		title.add(new Btn(Icons.NEW_FILE, e->track.toFrame(track.frames() + 1)));
	}


	@Override public void update() {

		if (track.isDrums()) {
			DrumSample s = ((DrumTrack)track).getKit().getSample((DrumType)focus.getSelectedItem());
			if (s.getGain().get(Gain.VOLUME) != focusVol.getValue())
				focusVol.setValue(s.getGain().get(Gain.VOLUME));
		}
		else if (/*synth*/ (Integer)octaves.getSelectedItem() * 12 != ((PianoTrack)track).getRange() )
				octaves.setSelectedItem(((PianoTrack)track).getRange() / 12);
		if (velocity.getValue() != (int)(track.getAmp() * 100))
			velocity.setValue((int) (track.getAmp() * 100));
		patterns.update();
	}

	@Override
	public boolean doKnob(int knob, int data2) {
		switch (knob) {
			case 0: // tracknum
				seq.getTracks().setCurrent((MidiTrack)Constants.ratio(data2, seq.getTracks()));
				return true;
			case 1: // current pattern
				track.toFrame(Constants.ratio(data2, track.frames()));
				return true;
			case 2: // file (settable)
						File x = (File)Constants.ratio(data2, Folders.sort(track.getFolder()));
						file.midiShow(x);
				return true;
			case 3: // preset (settable)
				progChange.midiShow(Constants.ratio(data2, track.getMidiOut().getPatches()).toString());
				return true;
			case 4: // amp
				track.setAmp(data2 * 0.01f);
				return true;
			case 5: // gate
				track.setGate((Gate)Constants.ratio(data2, Gate.values()));
				return true;
			case 6: // snare/mode
				if (track.isSynth())
					mode.midiShow((Arp)Constants.ratio(data2, Arp.values()));
				else
					focus.setSelectedItem(Constants.ratio(data2, DrumType.values()));
				return true;
			case 7: // clap/octaves
				if (track.isSynth())
					Threads.execute(()-> octaves.setSelectedIndex(Constants.ratio(data2, 3)));
				else
					drumVol(data2);
				return true;
		}
		return false;
	}

	@Override
	public void pad1() {
		TabZone.edit(track);
	}

	@Override // TODO handle navigate away from TrackKnobs onDeck = !onDeck;
	public void pad2() {
		if (track.isDrums())
			MainFrame.setFocus(((DrumTrack)track).getKit().getGui());
		else
			MPKmini.instance.setCaptureTrack((PianoTrack)track);
	}

}
