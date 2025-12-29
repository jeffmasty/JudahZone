package net.judah.gui;

import static net.judah.gui.Size.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.ConcurrentModificationException;
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
import net.judah.api.Chord;
import net.judah.api.Effect;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.KitSetup;
import net.judah.fx.Compressor;
import net.judah.fx.Convolution;
import net.judah.fx.LFO;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.fx.PresetsView;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.knobs.TunerKnobs;
import net.judah.gui.midiimport.ImportView;
import net.judah.gui.scope.JudahScope;
import net.judah.gui.scope.Live;
import net.judah.gui.settable.PresetsHandler;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.ModalDialog;
import net.judah.gui.widgets.Tuner.Tuning;
import net.judah.looper.Looper;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;
import net.judah.seq.MidiConstants;
import net.judah.seq.Seq;
import net.judah.seq.SynthRack;
import net.judah.seq.TrackList;
import net.judah.seq.beatbox.DrumZone;
import net.judah.seq.beatbox.RemapView;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.ChordScroll;
import net.judah.seq.chords.Chords;
import net.judah.seq.chords.Section;
import net.judah.seq.chords.SectionCombo;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.song.Overview;
import net.judah.song.Scene;
import net.judah.song.SceneLauncher;
import net.judah.song.SongView;
import net.judah.song.setlist.SetlistView;
import net.judah.synth.taco.TacoSynth;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.Circular;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.util.Threads;

/** over-all layout and background updates thread */
public class MainFrame extends JFrame implements Runnable {

	private static MainFrame instance;
	private static KnobMode knobMode = KnobMode.MIDI;
	private static KnobPanel knobs;
    private static boolean bundle;
    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
    private static final Circular<TrackUpdate> trackUpdates = new Circular<>(64, () -> new TrackUpdate());
	private static final Circular<FxUpdate> channelUpdates = new Circular<>(192, () -> new FxUpdate());
	private static final Circular<KnobUpdate> knobUpdates = new Circular<>(64, () -> new KnobUpdate());

	private final TabZone tabs;
    @Getter private final DrumZone beatBox;
	@Getter private final JudahMenu menu;
    private final DJJefe mixer;
	private final FxPanel effects;
	private final Overview overview;
    private final HQ hq;
	private final Seq seq;
	private final DrumMachine drums;
    private final Looper looper;
    private final MiniLooper loops;
    private final Chords chords;
    private final MidiGui midiGui;
    private final Sampler sampler;
	private final JComboBox<KnobMode> mode;
    private final JComponent knobHolder;
    private final PresetsView presets;
    private final SetlistView setlists;
    private final JudahZone zone;
    private final Feedback feedback;

    public static void updateChannel(Channel ch, Effect fx) {
    	FxUpdate send = channelUpdates.get();
    	send.ch = ch;
    	send.fx = fx; // null = preset
    	update(send);
    }

    public static void updateTrack(Update t, Computer track) {
    	TrackUpdate send = trackUpdates.get();
    	send.type = t;
    	send.update = track;
    	update(send);
    }

    public static void updateKnob(int idx, int data2) {
    	KnobUpdate send = knobUpdates.get();
    	send.idx = idx;
    	send.data2 = data2;
    	update(send);
    }

    public MainFrame(String name, JudahZone judahZone) {
    	super(name);
        instance = this;
        this.zone = judahZone;
        this.effects = zone.getFxRack();
        this.mixer = zone.getMixer();
        this.overview = zone.getOverview();
        this.midiGui = zone.getMidiGui();
        this.looper = zone.getLooper();
        this.drums = zone.getDrumMachine();
        this.seq = zone.getSeq();
        this.chords = zone.getChords();
        this.sampler = zone.getSampler();

        this.feedback = new Feedback(Size.KNOB_PANEL);
        JudahClock clock = JudahMidi.getClock();


        hq = new HQ(clock, zone);
        loops = new MiniLooper(looper, clock);
        presets = new PresetsView(PresetsHandler.getPresets(), zone);
        setlists = new SetlistView(zone.getSetlists(), overview);
    	beatBox = new DrumZone(drums.getTracks(), seq.getAutomation());
        tabs = new TabZone(zone, beatBox);
        menu = new JudahMenu(WIDTH_KNOBS, zone, tabs, zone.getMidi());
        mode = new JComboBox<>(KnobMode.values());
        mode.setFont(Gui.BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());});
        mixer.updateAll();
        knobHolder = Box.createVerticalBox();

        Gui.resize(knobHolder, KNOB_PANEL);
        Gui.resize(mode, new Dimension(COMBO_SIZE.width, STD_HEIGHT + 2));
        Gui.resize(mixer, MIXER_SIZE);

        // clock, midi panel, fx controls
        JComponent left = Box.createVerticalBox();
        left.add(Gui.wrap(menu));
        left.add(Gui.wrap(hq));
        left.add(Gui.box(seq, loops));
        left.add(effects);
        left.add(Box.createVerticalStrut(1));
        left.add(knobHolder);

        Component ticker = Gui.resize(feedback.getTicker(), new Dimension(WIDTH_KNOBS - 10, STD_HEIGHT - 3));
        left.add(Gui.box( Box.createHorizontalStrut(5), ticker));

        // mixer & main view
        JComponent right = Box.createVerticalBox();
        right.add(tabs);
        right.add(mixer);
        right.add(Box.createVerticalGlue());

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
        content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
        content.add(Box.createHorizontalStrut(1));
        content.add(Gui.resize(left, new Dimension(WIDTH_KNOBS, HEIGHT_FRAME)));
        content.add(Gui.resize(right, new Dimension(WIDTH_TAB, HEIGHT_FRAME)));
        content.add(Box.createHorizontalGlue());
        content.doLayout();

        validate(); pack();

        EventQueue.invokeLater(()->{
            Thread.ofVirtual().start(this);
	        tabs.setSelectedIndex(0);
	        tabs.requestFocusInWindow();
	        seq.getTracks().setCurrent(seq.getTracks().getFirst());
	        knobMode(KnobMode.MIDI);
	        setVisible(true);
	        new Bindings(this, seq, mixer);
        });
    }

    public static void setFocus(Object o) {
    	update(new FocusUpdate(o));
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
    		if (knobMode == KnobMode.LFO)
    			knobPanel(ch.getLfoKnobs());
    		if (ch == drums)
    			knobPanel(drums.getKnobs());
    		else if (ch instanceof TacoTruck synth)
    			focus(synth.getTrack().getKnobs());
		}
		else if (o instanceof MultiSelect multiselect) { // TODO LFO/Compressor
			Channel next = multiselect.get(multiselect.size()-1);
			mixer.highlight(multiselect);
			effects.addFocus(next);
		}
		else if (o instanceof Scene scene) {
			overview.getSongView().setCurrent(scene);
			hq.sceneText();
			overview.update();
		}
		else if (o instanceof TrackList trax) {
			MidiTrack current = trax.getCurrent();
			if (trax == seq.getTracks())
				seq.update();
			else if (trax == drums.getTracks()) {
				DrumTrack d = (DrumTrack)current;
				focus(d.getKit());
				beatBox.update(d);
			}
		}

    }

    public static void update(Object o) {
    	updates.offer(o);
    }

    @Override
    public void run() {
        Object o = null;
        while (true) {
            o = updates.poll();
            if (o == null) {
                Threads.sleep(Constants.GUI_REFRESH);
                continue;
            }
            final Object event = o;
            EventQueue.invokeLater(() -> runOnEDT(event));
        }
    }

    /** GUI feed updates off Real-Time thread (transferred to Event Dispatch Thread) */
    private void runOnEDT(Object o) {

        try { // to keep the GUI thread running
            if (updates.contains(o)) // heavy? misses Circulars
                return;

            if (o instanceof FxUpdate fx) {
            	if (fx.fx == null) { // presets
            		fx.ch.getGui().updatePreset();
            		mixer.update(fx.ch);
            		return;
            	}
                mixer.update(fx.ch, fx.fx); // LEDs
                overview.update(fx.ch, fx.fx); // preset and gain
                fx.ch.getGui().update(fx.fx); // FxRack
                if (fx.fx instanceof LFO) // lfo panel
                    fx.ch.getLfoKnobs().update(fx.fx);
                else if (fx.fx instanceof Compressor)
                    fx.ch.getLfoKnobs().getCompressor().update();
                else if (fx.fx instanceof Convolution)
                    fx.ch.getLfoKnobs().update(fx.fx);
            }

            else if (o instanceof Channel ch) {
                mixer.update(ch);
                overview.update(ch);
                if (ch instanceof DrumKit && knobMode == KnobMode.Kitz)
                    knobs.update();
                else if (ch instanceof Sampler)
                    midiGui.update(); // stepSampler
            }
            else if (o instanceof Actives a) { // TODO
                if (a.getChannel() >= MidiConstants.DRUM_CH && knobMode == KnobMode.Kitz) {
                    ((KitKnobs)knobs).update(a);
                }
            }
            else if (o instanceof MidiTrack t) { // clipboard/editor/select
                tabs.update(t);
                overview.update(t);
            }
            else if (o instanceof TrackUpdate t) {
                seq.update(t.type, t.update); // including TrackKnobs
                overview.update(t.type, t.update);
                tabs.update(t.type, t.update);
                midiGui.update(t.type, t.update);
                if (JudahZone.isInitialized())
                    seq.getAutomation().update(t.type, t.update);
                if (t.update instanceof TacoSynth taco && taco.getKnobs() != null)
                    taco.getKnobs().update(t.type);
            }

            else if (o instanceof Sample samp) {
                if (knobMode == KnobMode.Sample)
                    sampler.getView().update(samp);
            }
            else if (o instanceof JudahScope)
                hq.metronome(); // running FFTs indicator
            else if (o instanceof JudahClock) {
                hq.length();
                hq.metronome();
                mixer.update(looper.getLoopA());
            }
            else if (o instanceof Scene scene) {
                hq.sceneText();
                overview.getSongView().getLauncher().update(scene);
            }
            else if (o == seq || o == overview)
                overview.update();
            else if (o == mixer)
                mixer.updateAll();
            else if (o instanceof SceneLauncher launcher)
                launcher.fill();
            else if (o instanceof Updateable updateable)
                updateable.update();
            else if (o instanceof KnobUpdate data)
                doKnob(data.idx, data.data2);
            else if (o instanceof LFO) {
                if (effects.getChannel().getLfo() == o)
                    effects.getChannel().getGui().update(); // heavy?
            }
            else if (o instanceof SongView songView) {
                songView.getLauncher().update();
                hq.sceneText();
            }
            else if (o instanceof Chord chord) {
                overview.getChords().update(chord);
                chords.getChordSheet().update(chord);
                ChordScroll.scroll();
            }
            else if (o instanceof Section sec) {
                overview.getChords().setSection(sec);
                chords.getChordSheet().setSection(sec);
                SectionCombo.setSection(sec);
                ChordPlay.update();
            }
            else if (o instanceof Chords) {
                overview.getChords().updateDirectives();
                chords.getChordSheet().updateDirectives();
            }
            else if (o instanceof Live.LiveData dat) {
                dat.processor().analyze(dat.stereo());
                if (knobs instanceof TunerKnobs waves)
                    waves.repaint();
            }
            else if (o instanceof Tuning tuning)
                tuning.tuner().update(tuning.buffer());
            else if (o instanceof KitSetup)
                drums.getKnobs().update();
            else if (o instanceof Seq)
                overview.refill(); // # of Tracks has changed
            else if (o instanceof FocusUpdate focus)
    			focus(focus.focus); // !
            else
                RTLogger.log(this, "unknown " + o.getClass().getSimpleName() + " update: " + o.toString());

        } catch (ConcurrentModificationException e) {
            RTLogger.log(e, o + ": " + e.getMessage());
        } catch (Throwable t) {
            RTLogger.warn(this, t);
        }
    }
	private void knobMode(KnobMode knobs) {
    	knobMode = knobs;
		mode.setSelectedItem(knobMode);
		knobPanel(switch(knobs) {
    		case MIDI -> midiGui;
			case Kitz -> drums.getKnobs();
			case Track -> seq.getKnobs();
			case LFO -> effects.getChannel().getLfoKnobs();
			case Taco -> SynthRack.getTacos()[0].getTrack().getKnobs(); // TODO
			case Sample -> {focus(sampler); yield sampler.getView();}
			case Presets -> presets;
			case Setlist -> setlists;
			case Tuner -> TunerKnobs.getInstance();
			case Import -> ImportView.getInstance();
			case Remap -> new RemapView();
			case Log -> feedback;
			case Autom8 -> seq.getAutomation();
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

	public static final int SCROLL_BTN = 8;
	public static void startNimbus() {
		try {
			UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("ScrollBar.buttonSize", new Dimension(SCROLL_BTN, SCROLL_BTN));
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
	}

	public static void kit() {
		if (instance.effects.getChannel() instanceof DrumKit)
			instance.drums.getTracks().next(true);
		else
			setFocus(instance.drums.getTracks());
	}

	public static KnobMode getKnobMode() 	 { return knobMode; }
	public static KnobPanel getKnobs() 		 { return knobs; }
	public static boolean isBundle() 		 { return bundle; }
	public static void setBundle(boolean tf) { bundle = tf; }

	private static class FocusUpdate {
		Object focus;
		FocusUpdate(Object o) { focus = o;}
	}

	private static class TrackUpdate {
    	Computer.Update type;
    	Computer update;
    }

    private static class FxUpdate {
    	Channel ch;
    	Effect fx;
    }

    private static class KnobUpdate{
    	int idx;
    	int data2;
    }

    public static void feedback(String s) {
    	instance.feedback.addText(s);
    }

}