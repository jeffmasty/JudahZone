package net.judah.sequencer;
import static net.judah.util.Constants.Param.*;
import static org.jaudiolibs.jnajack.JackTransportState.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.sound.midi.ShortMessage;

import org.apache.commons.lang3.StringUtils;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.SongPane;
import net.judah.api.*;
import net.judah.api.Notification.Property;
import net.judah.effects.api.Preset;
import net.judah.effects.api.PresetsHandler.Raw;
import net.judah.looper.Loop;
import net.judah.looper.Recording;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiListener;
import net.judah.midi.MidiListener.PassThrough;
import net.judah.midi.Route;
import net.judah.midi.Router;
import net.judah.mixer.DrumTrack;
import net.judah.mixer.LineIn;
import net.judah.song.Song;
import net.judah.song.Trigger;
import net.judah.util.CommandWrapper;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;

@Log4j @Getter
public class Sequencer implements Service, Runnable, TimeListener {

    /** CC3 messages inserted in midi clicktrack file define bars, see {@link #controlChange(ShortMessage)} */
    public static final String PARAM_CONTROLLED = "pulse.controlled";
    public static final String PARAM_PULSE = "beats.per.pulse";
    private static final String CONTROL_ERROR = "External control error: ";

    public static enum TimeBase {
        BEATS, PULSE, TICKS
    }

    public static enum ControlMode {
        /** internal clock */
        INTERNAL,
        /** external clock (from looper, cc3 inserted into click tracks) */
        EXTERNAL};

        @Getter private static Sequencer current;

        private final String serviceName = Sequencer.class.getSimpleName();
        private final Song song;
        private File songfile;
        private final ArrayList<MidiListener> listeners = new ArrayList<>();
        private final Mappings mappings;

        private final JudahZone mixer;
        private final Metronome metronome;
        private final SongPane page;
        private SeqCommands commands = new SeqCommands();
        /** see {@link #properties(HashMap)} */
        @Getter private File sheetMusic;

        /** external = looper (or deprecated cc3 events) sets the time */
        @Setter(value = AccessLevel.PACKAGE) @Getter
        private ControlMode control = ControlMode.INTERNAL;
        /** internal clock */
        TimeProvider clock;
        // private SeqClock clock;

        /** a sense of the current beat */
        private int count = -1;
        /** the next command sitting in the sequencer awaiting execution. */
        private Trigger active;
        /** current sequencer command index */
        private int index = 0;
        private final Stack<CommandWrapper> queue = new Stack<>();
        /** number of TimeBase units (beats) per pulse */
        @Setter(value = AccessLevel.PACKAGE) @Getter
        private int pulse = 4;
        // 	private TimeBase unit = TimeBase.BEATS;

        public Sequencer(File songfile) throws IOException, JackException {
            this.mixer = JudahZone.getInstance();
            this.songfile = songfile;
            song = (Song)JsonUtil.readJson(songfile, Song.class);
            mappings = new Mappings(song.getLinks());
            metronome = JudahZone.getMetronome();

            handlePrevious();
            current = this;
            initializeProperties();
            initializeFiles();
            initializeTriggers();
            Router midiRouter = JudahMidi.getInstance().getRouter();

            song.getRouter().forEach( pair -> { midiRouter.add(
                    new Route(pair.getFromMidi(), pair.getToMidi()));});

            page = new SongPane(this);
            MainFrame.get().openPage(page);
        }

        private Sequencer handlePrevious() {
            JudahMidi.getInstance().getRouter().clear();
            Sequencer previous = Sequencer.getCurrent();
            if (previous != null) {
                JudahZone.getMetronome().removeListener(previous);
                previous.close();
                current = null;
                MainFrame.get().sheetMusicOff();
            }
            // metronome.addListener(this);
            JudahZone.getLooper().clear();
            log.debug("loaded song: " + songfile.getAbsolutePath());
            return this;
        }

        private void initializeProperties() {
            final HashMap<String, Object> props = song.getProps();
            if (props == null) return;

            JudahZone.getServices().forEach(service -> {service.properties(props);});
            properties(props);
        }

        private void initializeTriggers() {
            List<Trigger> triggers = song.getSequencer();
            if (triggers.isEmpty()) return;
            active = triggers.get(0);
            while (active != null && active.go(count))
                execute(active);
        }

        private void initializeFiles() {
            song.getSequencer().forEach(trigger -> {
                if(trigger.getCmd() instanceof Loadable)
                    try {
                        ((Loadable)trigger.getCmd()).load(trigger.getParams());
                    } catch (Exception e) {
                        Console.warn(e.getMessage(), e);
                    }});

            song.getLinks().forEach(link -> {
                if(link.getCmd() instanceof Loadable)
                    try {
                        ((Loadable)link.getCmd()).load(link.getProps());
                    } catch (Exception e) {
                        Console.warn(e.getMessage(), e);
                    }});

        }
        ///////////////////////////////////////////////////////////////////////////////////////////

        public static void transport() {
            Sequencer seq = getCurrent();
            if (seq == null) return;
            Console.info("transport " + !seq.isRunning());
            seq.update(Property.TRANSPORT, seq.isRunning() ? JackTransportStopped : JackTransportStarting);

        }

        public boolean isRunning() {
            return count >= 0;
        }

        @Override public void close() {
            stop();
            song.getSequencer().forEach(trigger -> {
                if (trigger.getCmd() instanceof Loadable)
                    ((Loadable)trigger.getCmd()).close();});

            song.getLinks().forEach(link -> {
                if (link.getCmd() instanceof Loadable)
                    ((Loadable)link.getCmd()).close();});
        }

        // dispose Song's services (loops, etc)
        public void stop() {
            // if (!isRunning()) return;
            if (clock != null) {
                clock.end();
            }
            JudahZone.getLooper().stopAll();
            count = -1;
        }

        @Override public void run() { // Thread
            ++count;
            while (active != null && active.go(count))
                execute(active);
            checkQueue();
        }

        public void setClock(TimeProvider clock) {
            if (this.clock != null && this.clock instanceof SeqClock)
                this.clock.end();
            this.clock = clock;
        }

        @Override // TimeListener
        public void update(Property prop, Object value) {

            if (Property.TRANSPORT == prop) {
                if (value == JackTransportState.JackTransportStarting) {
                    if (control == ControlMode.INTERNAL) {
                        JudahZone.getMetronome().removeListener(this);
                        try {
                            if (clock == null) {
                                clock = new SeqClock(this);
                                clock.begin();
                            }
                            else if (clock instanceof SeqClock) {
                                clock = new SeqClock(this, (SeqClock)clock);
                                clock.begin();
                            }
                        } catch (Exception e) { Console.warn(e); }
                    }
                }
                else if (value == JackTransportState.JackTransportStopped)
                    stop();
            }
            if (Property.BEAT == prop) {
                // if (!isRunning()) return;
                if (control == ControlMode.INTERNAL) {
                    log.trace("beat update: " + value);
                    count = (int)value;
                    while (active != null && active.go(count))
                        execute(active);
                    checkQueue();
                }
            }
            if (Property.LOOP == prop) {
                //long period = (System.currentTimeMillis() - timer) / pulse;
                if (control == ControlMode.EXTERNAL) {
                    count++;
                    Console.info("loop update: " + count);
                    while (active.go(count))
                        execute(active);
                    checkQueue();
                    if (clock instanceof SeqClock)
                        ((SeqClock)clock).pulse();
                }
            }
        }

        private void checkQueue() {
            if (queue.isEmpty()) return;

            CommandWrapper c = queue.peek();
            while (c != null && c.getInternalCount() == count) {
                queue.pop();
                try {
                    c.getCommand().setSeq(this);
                    c.getCommand().execute(c.getProps(), -1);
                } catch (Throwable t) {
                    Console.warn("queue error for " + c + " " + t.getMessage(), t);
                    return;
                }
                c = queue.peek();
            }
        }

        @Override // Service
        public void properties(HashMap<String, Object> props) {
            Object o;
            o = props.get(IMAGE);
            if (o != null && new File(o.toString()).isFile()) {
        		Console.info("sheet music set");
        		sheetMusic = new File(o.toString());
            }
        
            // legacy
            o = props.get(PARAM_CONTROLLED);
            if (o != null)
                control =  Boolean.parseBoolean(o.toString()) ? ControlMode.EXTERNAL : ControlMode.INTERNAL;

            if (props.containsKey(PARAM_PULSE)) {
                Object o2 = props.get(PARAM_PULSE);
                if (StringUtils.isNumeric("" + o2))
                    pulse = Integer.parseInt(o2.toString());
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////////////
        public void songTrigger() {
        	Loop a = JudahZone.getLooper().getLoopA();
        	if (current == null || current.active == null) 
        		a.record(a.isRecording() != AudioMode.RUNNING);
        	else {
                current.execute(current.active);
                while (current.active.go(current.count)) {
                    current.execute(current.active);
                }
            }
        }


        void externalControl(HashMap<String, Object> props) {
            Object o = props.get(LOOP);

            if (!StringUtils.isNumeric("" + o)) {
                log.error(CONTROL_ERROR + o);
                return;
            }
            int loop = Integer.parseInt(o.toString());
            if (JudahZone.getLooper().size() <= loop) {
                log.error(CONTROL_ERROR + " loop " + loop + " doesn't exist.");
                return;
            }

            Object o2 = props.get(PARAM_PULSE);
            if (o2 != null && StringUtils.isNumeric(o2.toString()))
                pulse = Integer.parseInt(o2.toString());
            
            RTLogger.warn(this, "TODO: ARM LOOP??");
            // JudahZone.getLooper().get(loop).setSync(true);
            log.warn("Looper " + loop + " has time control with pulse of " + pulse + " beats.");
        }

        void queue(CommandWrapper command) {
            if (command != null)
                queue.push(command);
        }

        /** @param port 
         * @return true if consumed */
        public boolean midiProcessed(Midi midi, JackPort port) {
            PassThrough mode = PassThrough.ALL;
            for (MidiListener listener : listeners) {
                new Thread() {
                    @Override public void run() {
                        listener.feed(midi);};
                }.start();

                PassThrough current = listener.getPassThroughMode();
                if (current == PassThrough.NONE)
                    mode = PassThrough.NONE;
                else if (current == PassThrough.NOTES && mode != PassThrough.NONE)
                    mode = PassThrough.NOTES;
                else if (current == PassThrough.NOT_NOTES && mode != PassThrough.NONE)
                    mode = PassThrough.NOT_NOTES;
            }
            if (PassThrough.NONE == mode)
                return true;
            else if (PassThrough.NOTES == mode)
                return !Midi.isNote(midi);
            else if (PassThrough.NOT_NOTES == mode && Midi.isNote(midi))
                return true;
            return mappings.midiProcessed(midi);
        }

        void internal(String name, String param) {
//            if ("AndILoveHer".equals(name))
//                _andILoveHer();
//            if ("FeelGoodInc".equals(name))
//                _feelGoodInc();
//            if ("IFeelLove".equals(name))
//                _iFeelLove();
            if ("AutumnLeaves".equalsIgnoreCase(name))
                _autumnLeaves();
            // if ("bassdrum".equals(name)) _bassdrum(param);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////

        private void execute(Trigger trig) {
            try {
                Command cmd = trig.getCmd();
                Console.info("seq @" + count + "/" + index + " execute: " + cmd + " " + Constants.prettyPrint(trig.getParams()));
                cmd.setSeq(this);
                cmd.execute(trig.getParams(), -1);
            } catch (Exception e) {
                Console.warn(e.getMessage() + " for " + trig, e);
            }
            increment();
        }

        private void increment() {
            index++;
            if (index < song.getSequencer().size())
                active = song.getSequencer().get(index);
            else {
                log.warn("We've reached the end of the sequencer");
                active = null;
//                active = new Trigger(-2l, commands.transport);
//                if (clock != null)
//                    clock.end();
            }
        }

//        /**Play sample 0 and it becomes time master.
//         * sample 1 is 5 times longer and is empty */
//        private void _andILoveHer() {
//            Loop drums = (Loop)JudahZone.getLooper().get(0);
//            Recording sample = drums.getRecording();
//            JudahZone.getLooper().get(1).setRecording(new Recording(sample.size() * 5));
//            // if (clock != null) clock.removeListener(this);
//            pulse = 8;
//            Metronome.remove(this);
//            control = ControlMode.EXTERNAL;
//            clock.end();
//            clock.removeListener(this);
//            drums.addListener(this);
//            ((SeqClock)clock).setLength(drums.getRecordedLength(), drums.getSize());
//            Console.info("internal: _andILoveHer()");
//
//        }

//        private void _feelGoodInc() {
//            Loop drums = (Loop)JudahZone.getLooper().get(0);
//            ((SeqClock)clock).setLength(drums.getRecordedLength(), drums.getSize());
//        }

        private void _iFeelLove() {

        }

        private void _autumnLeaves() {
            control = ControlMode.EXTERNAL;
            DrumTrack drums = JudahZone.getLooper().getDrumTrack();
            Loop loopA = JudahZone.getLooper().getLoopA();
            Loop loopB = JudahZone.getLooper().getLoopB();

            drums.sync(true);
            loopA.play(false); // not armed (record bridge for later)
            loopA.addListener( (prop, value)-> {
                if (Property.STATUS != prop || Status.TERMINATED != value)
                    return;
                // JudahZone.getDrummachine().play(false);

                Constants.sleep(1); // let drumTrack listener process end of LoopA

                drums.setTapeCounter(0);
                loopA.setTapeCounter(0);
                loopB.setTapeCounter(0);

                int length = loopA.getRecording().size();
                if (length % 2 != 0) { // make even
                    loopA.getRecording().remove(loopA.getRecording().size() - 1);
                    length --;
                }
                if (length != drums.getRecording().size()) {
                    Recording reel = drums.getRecording();
                    if (length > reel.size()) {
                        Console.info("TOO SMALL by " + (length - reel.size()));
                        while (reel.size() < length)
                            reel.add(reel.get(reel.size() - 1));
                    }
                    else {
                        Console.info("cleaning up " + (reel.size() - length) + " frames on drumTrack");
                        while (reel.size() > length)
                            reel.remove(reel.size() -1);
                    }
                }

                length = length / 2;
                loopB.setRecording(new Recording(length));

                loopB.play(true);
                loopB.record(true);
                loopA.removeListener(this);
                RTLogger.log(this, "Sync'd B recording. buffers: " +
                        loopB.getRecording().size() + " vs. " + loopA.getRecording().size());
                new Thread() { @Override public void run() {
                    try {Thread.sleep(8);} catch(Throwable t) { }
                    //JudahZone.getDrummachine().play(false);
                    control = ControlMode.EXTERNAL;
                    Console.info("internal: _autumnLeaves2()");
                };}.start();

            });

            JudahZone.getLooper().getLoopB().addListener( (prop, value)-> {

                if (Property.LOOP != prop) return;
                LineIn guitar = JudahZone.getChannels().getGuitar();
                Preset crunch = JudahZone.getPresets().get(Raw.StdCrunch);
                if (crunch.equals(guitar.getPreset()) && guitar.isPresetActive())
                    return;
                if (guitar.getPreset() != null)
                    guitar.getPreset().applyPreset(guitar, false);
                crunch.applyPreset(guitar, true);

                JudahZone.getLooper().getLoopB().record(false);

                Console.info("internal: _autumnLeaves3()");
            });

            Console.info("internal: _autumnLeaves()");
        }



}

//void setTempo(float tempo2) {
//if (tempo2 < tempo || tempo2 > tempo) {
//	tempo = tempo2;
//	if (JudahZone.getMetronome() != null)
//		JudahZone.getMetronome().setTempo(tempo2);
//}
//}

//TODO handle dropDaBeat
//if (c.getCommand() == commands.dropBeat) {
//	Console.warn("UN-QUEUEU!!");
//	try { // TODO metronome.unMute();
//		mixer.restoreState(mixerState);
//	} catch (JudahException e) {
//		log.error(e.getMessage(), e);
//		Console.warn("restore state: " + e.getMessage());
//	}}
//else commander.fire(c.getCommand(), c.getProps(), -1);}

//private void _bassdrum(String param) {
//try {
//	if (ControlMode.INTERNAL != control)
//		throw new JudahException("bass drums on internal clock only");
//	int clicks = Integer.parseInt(param);
//	// clock.doBassDrum(clicks); TODO put in sequencer
//
//} catch (Throwable t) {
//	Console.warn("bassdrum " + t.getMessage() + " param=" + param);
//}
//}
//public void rollTransport() {
//if (isRunning()) return;
//count = 0;
//// Console.debug(active.getTimestamp() + " " + active.getNotes()+ " " + Constants.prettyPrint(active.getParams()));
//if (active != null && active.getTimestamp() < 0)
//	increment();
//while (active.go(count)) {
//	execute(active);
//	increment();
//}
//if (control == ControlMode.EXTERNAL) {
//	Console.info("Rolling external, pulse: " + pulse);
//	// will receive timing from CC3 messages in midi stream (ControlEventListener) or looper repeats (pulse)
//}
//else {
//	// start internal time
//	internal = new Internal(this);
//	log.warn("Internal Sequencer starting with a bpm of " + getTempo());
//		Console.addText("Internal Sequencer starting with a bpm of " + getTempo());
//	internal.start();
///** external clock */
//void pulse() {
//	count += pulse;
//	Console.addText("-- beat: " + count + " vs. " + active.getTimestamp());
//	while (active.go(count)) {
//		execute(active);
//		increment();
//	}
//	while (queue.isEmpty() == false) {
//		CommandPair c = queue.pop();
//		if (c.getCommand() == commands.dropBeat) {
//			Console.warn("UN-QUEUEU!!");
//			try {// metronome.unMute(); // TODO
//				mixer.restoreState(mixerState);
//			} catch (JudahException e) {
//				log.error(e.getMessage(), e);
//				Console.warn("restore state: " + e.getMessage());
//			}
//		}
//		else
//			commander.fire(c.getCommand(), c.getProps(), -1);
//	}//}
///** external clock */
//@Override // ControlChange Listener deprecate? see TimeListener
//public void controlChange(ShortMessage event) {
//	if (event.getData1() != 3) return;
//	if (isRunning()) pulse();}
