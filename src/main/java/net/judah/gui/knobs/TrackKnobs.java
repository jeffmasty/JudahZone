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

import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.CueCombo;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.Integers;
import net.judah.gui.widgets.PlayWidget;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.TrackAmp;
import net.judah.seq.TrackList;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.Gate;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.Programmer;
import net.judah.synth.taco.TacoSynth;
import net.judahzone.gui.Gui;
import net.judahzone.gui.Icons;

// TODO MouseWheel listener -> change pattern
public class TrackKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.Track;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final MidiTrack track;
	private final TrackList<MidiTrack> sequencerTracks;

	private final PlayWidget play;
	private final Folder file;
	private final Program program;
	private final Programmer programmer;
	private final GateCombo gate;
	private final TrackAmp velocity;
	private final CueCombo cue;
	private final PatternLauncher patterns;
	private final JComboBox<MidiTrack> trax;
	private final ActionListener tracker = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			trax.removeActionListener(this);
			sequencerTracks.setCurrent((MidiTrack)trax.getSelectedItem());
			trax.setSelectedItem(track);
			trax.addActionListener(this);
		}
	};

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

	public TrackKnobs(MidiTrack t, TrackList<MidiTrack> tracks) {
		this.track = t;
		sequencerTracks = tracks;
		trax = new JComboBox<>(tracks.toArray(new MidiTrack[tracks.size()]));
		titleBar();

		///////////////////////////
		play = new PlayWidget(track, "  Play    ");
		file = new Folder(track); // TODO
		program = new Program(track);
		programmer = new Programmer(track);
        patterns = new PatternLauncher(track);
		cue = new CueCombo(track);
		gate = track instanceof NoteTrack notes ? new GateCombo(notes) : null;
		velocity = new TrackAmp(track);

		if (track.isDrums()) {
			focus.setSelectedItem(DrumType.Snare);
			focus.addActionListener(e->update());
			focusVol.setValue(((DrumTrack)track).getKit().getSample(
					((DrumType)focus.getSelectedItem())).getGain().get(Gain.VOLUME));
			focusVol.addChangeListener(e->drumVol(focusVol.getValue()));
		}
		else if (track.isSynth()) {
			mode = new ModeCombo(((PianoTrack)track));
			octaves = new Integers(1, 5);
			octaves.setSelectedItem(((PianoTrack)track).getRange() / 12);
			octaves.addActionListener(e->((PianoTrack)track).setRange((Integer)octaves.getSelectedItem() * 12));
		}

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
        top.add(play);
        top.add(Box.createHorizontalStrut(14));
        top.add(programmer);

		JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(new Lbl("File"));
		column.add(new Lbl("Preset"));
		column.add(new Lbl("Cue"));
		JPanel fx = new JPanel();
		fx.setLayout(new BoxLayout(fx, BoxLayout.LINE_AXIS));
		fx.add(Box.createHorizontalGlue());
		JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		btns.add(new Btn("Rec", e->track.setCapture(!track.isCapture())));
		btns.add(new Btn(track.isDrums() ? "Kit" : "MPK", e->MainFrame.setFocus(((DrumTrack)track).getKit())));

		if (track instanceof NoteTrack notes) {
			fx.add(new FxButton(notes.getChannel()));
	        if (notes.getMidiOut() instanceof TacoSynth)
	        	btns.add(new Btn("DCO", e->MainFrame.setFocus(((TacoSynth)notes.getMidiOut()).getKnobs())));
		}
		column.add(fx);
		settings.add(column);

		column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		column.add(Gui.resize(file, COMBO_SIZE));
		column.add(Gui.resize(program, COMBO_SIZE));
		column.add(Gui.resize(cue, COMBO_SIZE));

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

		column.add(gate == null ? new JLabel("--") : Gui.resize(gate, COMBO_SIZE));
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
		trax.setSelectedItem(track);
		trax.addActionListener(tracker);
		trax.setFont(Gui.BOLD);
		Gui.resize(trax, Size.COMBO_SIZE);

		title.add(trax);
		title.add(new Btn(Icons.SAVE, e->track.save()));
		title.add(new Btn(Icons.HOME, e->track.toFrame(0)));
		title.add(new Btn(Icons.NEW_FILE, e->track.toFrame(track.getFrames() + 1)));
	}

	public void refill(TrackList<MidiTrack> tracks) {
		trax.removeActionListener(tracker);
		trax.removeAllItems();
		tracks.forEach(t->trax.addItem(t));
		trax.setSelectedItem(tracks.getCurrent());
		trax.addActionListener(tracker);
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
		program.update();
	}

	@Override
	public boolean doKnob(int knob, int data2) {
		switch (knob) {
			case 0: // tracknum
				sequencerTracks.setCurrent((MidiTrack)Constants.ratio(data2, sequencerTracks));
				return true;
			case 1: // current pattern
				track.toFrame(Constants.ratio(data2, track.getFrames()));
				return true;
			case 2: // file (settable)
				File x = (File)Constants.ratio(data2, Folders.sort(track.getFolder()));
				file.midiShow(x);
				return true;
			case 3: // preset (settable)
				program.midiShow(track.getPatches()[data2]);
				return true;
			case 4: // amp
				track.setAmp(data2 * 0.01f);
				return true;
			case 5: // gate
				if (track instanceof NoteTrack notes)
					notes.setGate((Gate)Constants.ratio(data2, Gate.values()));
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
//		if (track.isDrums())
//			MainFrame.setFocus(((DrumTrack)track).getKit().getGui());
//		else
//			// MPKmini.instance.setMpkTrack((PianoTrack)track);
	}

	public void update(Update type) {
		if (type == Update.PROGRAM)
			program.update();
		else if (type == Update.ARP)
			mode.update();
		else if (type == Update.CUE)
			cue.update();
		else if (type == Update.GATE && gate != null)
			gate.update();
		else if (type == Update.CYCLE) {
			programmer.getCycle().update();
			patterns.update();
		}
		else if (type == Update.CAPTURE)
			play.update();
		else if (type == Update.PLAY)
			play.update();
		else if (type == Update.CURRENT) {
			programmer.getCurrent().update();
			patterns.update();
		}
		else if (type == Update.LAUNCH)
			programmer.liftOff();
		else if (type == Update.AMP)
			velocity.update();
		else if (type == Update.FILE) {
			file.update();
			programmer.liftOff();
		}
		else if (type == Update.REFILL)
			file.refill();
		else if (type == Update.RANGE && track instanceof PianoTrack p)
			octaves.setSelectedItem((int) (p.getRange() / 12f));
		else if (type == Update.EDIT) {
			patterns.update();
			programmer.liftOff();
		}

	}

}
