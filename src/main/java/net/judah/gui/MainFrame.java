package net.judah.gui;

import static net.judah.gui.Size.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
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

import judahzone.api.Chord;
import judahzone.api.FX;
import judahzone.api.MidiConstants;
import judahzone.api.Tuning;
import judahzone.fx.Compressor;
import judahzone.fx.Convolution;
import judahzone.fx.Gain;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.gui.Updateable;
import judahzone.scope.JudahScope;
import judahzone.util.AudioMetrics.RMS;
import judahzone.util.Circular;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.KitSetup;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.fx.PresetsView;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.knobs.TunerKnobs;
import net.judah.gui.midiimport.ImportView;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.ModalDialog;
import net.judah.looper.Looper;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.LFO;
import net.judah.mixer.DJJefe;
import net.judah.mixer.LoopMix;
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;
import net.judah.seq.Seq;
import net.judah.seq.SynthRack;
import net.judah.seq.TrackList;
import net.judah.seq.automation.Automation;
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

/** over-all layout and background updates thread */
public class MainFrame extends JFrame implements Runnable {

	private static MainFrame instance;
	private static KnobMode knobMode = KnobMode.MIDI;
	private static KnobPanel knobs;
    private static boolean bundle;
    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
    private static final Circular<TrackUpdate> trackUpdates = new Circular<>(96, () -> new TrackUpdate());
	private static final Circular<FxUpdate> fxUpdates = new Circular<>(192, () -> new FxUpdate());
	private static final Circular<ChUpdate> chUpdates = new Circular<>(128, () -> new ChUpdate());
	private static final Circular<KnobUpdate> knobUpdates = new Circular<>(96, () -> new KnobUpdate());

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
    private TunerKnobs tuner;
    private final JudahZone zone;
    private final Feedback feedback;

    public static void updateFx(Channel ch, FX fx) {
    	FxUpdate send = fxUpdates.get();
    	send.ch = ch;
    	send.fx = fx; // null = preset
    	update(send);
    }

    public static void updateChannel(Channel.Update type, Channel ch) {
    	ChUpdate send = chUpdates.get();
    	send.type = type;
    	send.ch = ch;
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
        presets = new PresetsView(zone);
        setlists = new SetlistView(zone.getSetlists(), overview);
        mode = new JComboBox<>(KnobMode.values());
        mode.setFont(Gui.BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());});
        knobHolder = Box.createVerticalBox();
        mixer.updateAll();

        beatBox = new DrumZone(drums.getTracks());
        tabs = new TabZone(zone, beatBox);
        menu = new JudahMenu(WIDTH_KNOBS, zone, tabs, zone.getMidi());

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
    		if (knobMode == KnobMode.LFO)
    			knobPanel(ch.getLfoKnobs());
    		if (ch == drums)
    			knobPanel(drums.getKnobs());
    		else if (ch instanceof TacoTruck synth)
    			focus(synth.getTrack().getKnobs());
    		update(o); // loopback update();
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
		else if (o instanceof Automation a) {
			knobPanel(a);
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

            if (o instanceof FxUpdate fx) {
            	fx.ch.getGui().update(fx.fx); // FXRack + preset

            	if (fx.fx == null || fx.fx instanceof Gain)
            		overview.update(fx.ch, fx.fx); // preset and gain

                if (fx.fx == null)   // presets
            		mixer.update(fx.ch);
                else {
                    mixer.update(fx.ch, fx.fx); // LEDs
                    if (fx.fx instanceof LFO) // lfo panel
                        fx.ch.getLfoKnobs().update(fx.fx);
                    else if (fx.fx instanceof Compressor)
                        fx.ch.getLfoKnobs().getCompressor().update();
                    else if (fx.fx instanceof Convolution)
                        fx.ch.getLfoKnobs().update(fx.fx);
                }
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
            else if (o instanceof LoopMix)
            	mixer.updateAll(); // also update onTape feedbacks
            else if (o instanceof Updateable updateable)
                updateable.update();
            else if (o instanceof KnobUpdate data) { // doKnob()
        		if (knobs != null && knobs.doKnob(data.idx, data.data2))
        			update(knobs);
            }
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

            else if (o instanceof Tuning tuning && knobs instanceof TunerKnobs tuner)
            	tuner.update(tuning);
            else if (o instanceof RMS rms && knobs instanceof TunerKnobs tuner)
            	tuner.update(rms);
            else if (o instanceof KitSetup)
                drums.getKnobs().update();
            else if (o instanceof Seq)
                overview.refill(); // # of Tracks has changed
            else if (o instanceof FocusUpdate focus)
    			focus(focus.focus); // !
            else // once mature, ignore final in-flight analysis
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
			case Tuner -> tuner();
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
    	if (knobs instanceof TunerKnobs tuner)
    		tuner.turnOff();
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

        // If the newly installed panel is the tuner, ensure its analyzer is active
        if (knobs instanceof TunerKnobs tunerOn)
            tunerOn.turnOn();

    }

    private TunerKnobs tuner() {
    	if (tuner == null)
    		tuner = new TunerKnobs(zone);
    	return tuner;
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
    	FX fx;
    }

    @SuppressWarnings("unused")
	private static class ChUpdate {
    	Channel.Update type;
    	Channel ch;
    }

    private static class KnobUpdate {
    	int idx;
    	int data2;
    }

    public static void feedback(String s) {
    	instance.feedback.addText(s);
    }

}