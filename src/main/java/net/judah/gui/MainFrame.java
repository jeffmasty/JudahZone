package net.judah.gui;

import static net.judah.gui.Size.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.controllers.KnobData;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.fx.Compressor;
import net.judah.fx.Effect;
import net.judah.fx.LFO;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.fx.PresetsView;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.waves.LiveWave.LiveWaveData;
import net.judah.gui.waves.Tuner.Tuning;
import net.judah.gui.waves.WaveKnobs;
import net.judah.gui.widgets.ModalDialog;
import net.judah.gui.widgets.TrackGain;
import net.judah.looper.Looper;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;
import net.judah.seq.MidiConstants;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.seq.beatbox.DrumZone;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.ChordScroll;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.chords.Section;
import net.judah.seq.chords.SectionCombo;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.Programmer;
import net.judah.song.Overview;
import net.judah.song.Scene;
import net.judah.song.SceneLauncher;
import net.judah.song.SongView;
import net.judah.song.setlist.SetlistView;
import net.judah.synth.taco.TacoSynth;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** over-all layout and background updates thread */
public class MainFrame extends JFrame implements Runnable {
	public static record FxChange(Channel ch, Effect fx) {}
	private static MainFrame instance;
    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
	//public static final ExecutorService focus = Executors.newVirtualThreadPerTaskExecutor();
	@Getter private static KnobMode knobMode = KnobMode.MIDI;
	@Getter private static KnobPanel knobs;

	private final TabZone tabs;
    private final DrumZone drumZone;
    private final DJJefe mixer;
	private final FxPanel effects;
    private final MiniSeq miniSeq;
	private final Overview songs;
    private final HQ hq;
	private final Seq seq;
	private final DrumMachine drums;
    private final Looper looper;
    private final MiniLooper loops;
    private final ChordTrack chords;
    private final MidiGui midiGui;
    private final Sampler sampler;
	private final JudahMenu menu;
	private final JComboBox<KnobMode> mode = new JComboBox<>(KnobMode.values());
    private final JComponent knobHolder = Box.createVerticalBox();
    private final PresetsView presets;
    private final SetlistView setlists;

    public MainFrame(String name, JudahClock clock, FxPanel controls, DJJefe djJefe, Seq sequencer, Looper loopr,
    		Overview songz, MidiGui gui, DrumMachine drumz, Channel focus) {
    	super(name);
        instance = this;
        this.effects = controls;
        this.mixer = djJefe;
        this.looper = loopr;
        this.seq = sequencer;
        this.drums = drumz;
        this.songs = songz;
        this.chords = seq.getChords();
        this.midiGui = gui;
        this.sampler = seq.getSampler();
        hq = new HQ(clock, looper, songs, chords);
        miniSeq = new MiniSeq(seq.getTracks(), clock);
        loops = new MiniLooper(looper, clock);
        presets = new PresetsView(JudahZone.getPresets());
        setlists = new SetlistView(JudahZone.getSetlists(), songs);
    	drumZone = new DrumZone(seq.getDrumTracks());
        tabs = new TabZone(songs, drumZone, chords.getChordSheet());
        menu = new JudahMenu(WIDTH_KNOBS, songs, tabs);
        mode.setFont(Gui.BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());});
        mixer.updateAll();

        Gui.resize(knobHolder, KNOB_PANEL);
        Gui.resize(mode, new Dimension(COMBO_SIZE.width, STD_HEIGHT + 2));
        Gui.resize(mixer, MIXER_SIZE);
        knobHolder.setBorder(Gui.SUBTLE);

        // clock, midi panel, fx controls
        JComponent left = Box.createVerticalBox();
        left.add(Gui.wrap(menu));
        left.add(Gui.wrap(hq));
        left.add(Gui.wrap(Gui.box(miniSeq, loops)));
        left.add(knobHolder);
        left.add(Box.createVerticalStrut(1));
        left.add(effects);
        left.add(Gui.wrap(RTLogger.getTicker()));
        Gui.resize(left, new Dimension(WIDTH_KNOBS, HEIGHT_FRAME));

        // mixer & main view
        JComponent right = Box.createVerticalBox();
        right.add(tabs);
        right.add(mixer);
        right.add(Box.createVerticalGlue());
        Gui.resize(right, new Dimension(WIDTH_TAB, HEIGHT_FRAME));

    	setLocationByPlatform(true);
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(Icons.get("icon.png").getImage());
        setForeground(Color.DARK_GRAY);
        setSize(SCREEN_SIZE);
        setLocation(0, 0);
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);

        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (screens.length == 2) { // specific screen if dual monitor
        	JFrame dummy = new JFrame(screens[1].getDefaultConfiguration());
        	setLocationRelativeTo(dummy);
        	dummy.dispose();
        }

        JPanel content = (JPanel)getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.LINE_AXIS));
        content.add(left);
        content.add(right);
        content.add(Box.createHorizontalGlue());

        validate(); pack();

        Thread.ofVirtual().start(this);
        EventQueue.invokeLater(()->{
	        setFocus(focus);
	        tabs.setSelectedIndex(0);
	        tabs.requestFocusInWindow();
	        seq.getTracks().setCurrent(seq.getTracks().getFirst());
	        knobMode(KnobMode.MIDI);
	        setVisible(true);
	        new Bindings(this, seq, mixer);
        });
    }

    public static void setFocus(Object o) {
    	Threads.execute(()->instance.focus(o));
    }

    private void focus(Object o) {
		if (o instanceof KnobMode mode) {
			knobMode(mode);
			return;
		}
		if (o instanceof KnobPanel p) {
			knobPanel(p);
			return;
		}

		if (o instanceof Channel ch) {
    		effects.setFocus(ch);
    		mixer.highlight(ch);
    		update(o);
    		if (knobMode == KnobMode.LFOz)
    			knobPanel(ch.getLfoKnobs());

		}
		else if (o instanceof MultiSelect multiselect) { // TODO LFO/Compressor
			Channel next = multiselect.get(multiselect.size()-1);
			mixer.highlight(multiselect);
			effects.addFocus(next);
		}
		else if (o instanceof Scene scene) {
			songs.getSongView().setCurrent(scene);
			hq.sceneText();
			songs.update();
		}
		else if (o instanceof TrackList trax) {
			MidiTrack current = trax.getCurrent();

			if (trax == seq.getTracks()) {
				miniSeq.update();
				if (knobMode == KnobMode.Track)
					knobPanel(seq.getKnobs(current));
			}
			else if (trax == drums.getTracks()) {
				DrumTrack d = ((DrumTrack)drums.getTracks().getCurrent());
				focus(d.getKit());
				focus(d.getKit().getGui());
				if (knobMode == KnobMode.Kitz)
					knobPanel(d.getKit().getKnobs());
				drumZone.update(d);
			}
			else if (trax == seq.getSynthTracks()) {

			}
		}

    }

    public static void update(Object o) {
    	updates.offer(o);
    }

    /** GUI feed updates off Real-Time thread */
	@Override public void run() {
		Object o = null;
		while (true) {

			o = updates.poll();
			if (o == null) {
				Threads.sleep(Constants.GUI_REFRESH);
				continue;
			}
			try { // to keep the GUI thread running

			if (updates.contains(o))
				continue;


			if (o instanceof FxChange delta) {
				if (delta.fx instanceof LFO)
					delta.ch.getLfoKnobs().update(delta.fx);
				else if (delta.fx instanceof Compressor)
					delta.ch.getLfoKnobs().getCompressor().update();
			}
			else if (o instanceof Channel ch) {
				mixer.update(ch);
				if (effects.getChannel() == ch)
					effects.getChannel().getGui().update();
				if (knobs instanceof LFOKnobs && ((LFOKnobs)knobs).getChannel() == ch)
					knobs.update();
				if (ch instanceof DrumKit kit) {
					TrackGain.update(kit);
					if (knobMode == KnobMode.Kitz && ((KitKnobs)knobs).getKit() == kit)
						((KitKnobs)knobs).update();
				}
//					else if (synthBox.isVisible() && synthBox.getCurrent().getActives() == a)
//						synthBox.getView().getInstrumentPanel().repaint();
				if (ch instanceof Sampler)
					midiGui.update(); // stepSampler
			}
			else if (o instanceof Actives a) {
				if (a.getChannel() >= MidiConstants.DRUM_CH && knobMode == KnobMode.Kitz && ((KitKnobs)knobs).getKit().getChannel() == a.getChannel())
					((KitKnobs)knobs).update(a);
			}
			else if (o instanceof MidiTrack t) {
				PlayWidget.update(t);
				miniSeq.update(t);
				seq.getKnobs(t).update();
				tabs.update(t);
				if (t instanceof DrumTrack d)
					drumZone.update(d);
				// songs.update(t);
				Programmer.update(t);
			}
			else if (o instanceof Sample samp) {
				if (knobMode == KnobMode.Samplez)
					sampler.getView().update(samp);
			}
			else if (o instanceof JudahClock) {
				hq.length();
				hq.metronome();
				mixer.update(looper.getLoopA());
			}
			else if (o instanceof Program prog) {
				Program.update(prog);
				if (prog.getMidiOut() instanceof TacoSynth taco)
					taco.getSynthKnobs().update();
			}
			else if (o instanceof Scene) {
				hq.sceneText();
				songs.getSongView().update((Scene)o);
			}
			else if (o == seq || o == songs)
				songs.update();
			else if (o == mixer)
				mixer.updateAll();
			else if (o instanceof SceneLauncher launcher)
				launcher.fill();
			else if (o instanceof Updateable updateable)
				updateable.update();
			else if (o instanceof KnobData data)
				doKnob(data.idx(), data.data2());
			else if (o instanceof LFO) {
				if (effects.getChannel().getLfo() == o)
					effects.getChannel().getGui().update(); // heavy?
			}
			else if (o instanceof SongView songView) {
				songView.getLauncher().update();
				hq.sceneText();
			}
			else if (o instanceof Chord chord) {
				chords.getView().update(chord);
				chords.getChordSheet().update(chord);
				ChordScroll.scroll();
			}
			else if (o instanceof Section sec) {
				chords.getView().setSection(sec);
				chords.getChordSheet().setSection(sec);
				SectionCombo.setSection(sec);
				ChordPlay.update();
			}
			else if (o instanceof ChordTrack) {
				chords.getView().updateDirectives();
				chords.getChordSheet().updateDirectives();
			}
			else if (o instanceof LiveWaveData dat) {
				dat.waveform().update(dat.buf());
				if (knobs instanceof WaveKnobs waves)
					waves.repaint();
			}
			else if (o instanceof Tuning tuning) {
				tuning.tuner().update(tuning.buffer());
			}
			else if (o instanceof String name) {
				MidiTrack t = seq.byName(name);
				miniSeq.update(t);
				PlayWidget.update(t);
			}

			else RTLogger.log(this, "unknown " + o.getClass().getSimpleName() + " update: " + o.toString());
			} catch (Throwable t) {
				RTLogger.warn(this, t);
			}
		}
	}

	private void knobMode(KnobMode knobs) {
    	knobMode = knobs;
		mode.setSelectedItem(knobMode);
		knobPanel(switch(knobs) {
    		case MIDI -> midiGui;
			case Kitz -> drums.getKnobs();
			case Track -> seq.getKnobs(seq.getCurrent());
			case LFOz -> effects.getChannel().getLfoKnobs();
			case Taco -> JudahZone.getTacos().taco.getSynthKnobs();
			case Samplez -> {focus(sampler); yield sampler.getView();}
			case Presets -> presets;
			case Setlist -> setlists;
			case Wavez -> new WaveKnobs(JudahZone.getSelected());
			case Log -> RTLogger.instance;
    	});
    }

    private void knobPanel(final KnobPanel view) {
    	if (knobs == view) {
    		knobs.update();
    		return;
    	}

    	knobs = view;
    	if (knobMode != view.getKnobMode()) {
    		knobMode = view.getKnobMode();
    		mode.setSelectedItem(knobMode);
    	}

    	Gui.resize(knobs, KNOB_PANEL);
    	JComponent knobTitle = Box.createHorizontalBox();
        knobTitle.add(Box.createHorizontalStrut(5));
        knobTitle.add(mode);
    	knobTitle.add(Gui.resize(view.getTitle(), KNOB_TITLE));
    	knobHolder.removeAll();
    	knobHolder.add(knobTitle);
    	knobHolder.add(knobs);
    	knobHolder.validate();
    	knobHolder.repaint();
    }

	private void doKnob(int idx, int data2) {
		if (knobs != null && knobs.doKnob(idx, data2))
			update(knobs);
	}

	public static void startNimbus() {
		try {
			UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("ScrollBar.buttonSize", new Dimension(8,8));
            UIManager.put("nimbusBase", Pastels.EGGSHELL);
            UIManager.put("control", Pastels.EGGSHELL);
            UIManager.put("nimbusBlueGrey", Pastels.MY_GRAY);

            UIManager.getLookAndFeel().getDefaults().put("Button.contentMargins", new Insets(5, 5, 5, 5));
            UIManager.getLookAndFeel().getDefaults().put("JToggleButton.contentMargins", new Insets(1, 1, 1, 1));

            Thread.sleep(10); // let nimbus start up
		} catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }
	}

	public static void set() {
		if (ModalDialog.getInstance() != null) {
			ModalDialog.getInstance().dispatchEvent(new WindowEvent(
                ModalDialog.getInstance(), WindowEvent.WINDOW_CLOSING));
		}
		else if (SetCombo.getSet() != null)
			SetCombo.set();
//		else if (instance.tabs.getSelectedComponent() instanceof MidiTab) {
//			((MidiTab)instance.tabs.getSelectedComponent()).getMusician().delete();
//		}
	}

	public static MiniSeq miniSeq() { return instance.miniSeq; }

}