package net.judah.metronome;

import static net.judah.Constants.NL;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPosition;
import org.jaudiolibs.jnajack.JackSyncCallback;
import org.jaudiolibs.jnajack.JackTimebaseCallback;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.Constants;
import net.judah.RTLogger;
import net.judah.Tab;
import net.judah.midi.Midi;
import net.judah.settings.Command;
import net.judah.settings.Service;

@Log4j
public class Metronome implements Service, JackTimebaseCallback, JackSyncCallback { /* extends BasicClient  implements JackProcessCallback, JackShutdownCallback, JackTimebaseCallback, JackSyncCallback */ 

	private final ArrayList<Command> commands = new ArrayList<>();

	private TickTock ticktock;
	private MetroUI metroUi;
	private JackPort out;
	@Getter private final Properties props;
    ConcurrentLinkedQueue<Midi> midiToSend = new ConcurrentLinkedQueue<>();

	final AtomicBoolean changed = new AtomicBoolean(false);

	Command start, stop, settings;

	Midi midiStart, midiStop, downbeatOn, downbeatOff, beatOn, beatOff; //:>

	public Metronome() throws JackException {
		// super(Metronome.class.getSimpleName());

		start = new Command("tick", this, "Start the metronome.");
		stop = new Command("tock", this, "Stop the metronome.");

		HashMap<String, Class<?>> params = new HashMap<String, Class<?>>();
		params.put("bpm", Float.class);
		params.put("bpb", Integer.class);
		params.put("volume", Float.class);
		settings = new Command("Metronome settings", this, params, "Adjust metronome settings.");
		commands.addAll(Arrays.asList(new Command[] {start, stop, settings}));
		props = new Properties();
		props.put("bpm", 70f);
		props.put("bpb", 4);
		props.put("volume", 0.7f);
		ticktock = new TickTock();

    	try {
    		midiStart = new Midi(ByteBuffer.allocate(4).putInt(ShortMessage.START).array());
    		midiStop = new Midi(ByteBuffer.allocate(4).putInt(ShortMessage.STOP).array());
    		downbeatOn = new Midi(ShortMessage.NOTE_ON, 9, 34, Constants.gain2midi((Float)props.get("volume"))); // bell
    		downbeatOff = new Midi(ShortMessage.NOTE_OFF, 9, 34);
    		beatOn = new Midi(ShortMessage.NOTE_ON, 9, 33, Constants.gain2midi((Float)props.get("volume"))); // wood block
    		beatOff = new Midi(ShortMessage.NOTE_OFF, 9, 33);
    	} catch (InvalidMidiDataException e) {
    		e.printStackTrace();
    	}

    	metroUi = new MetroUI(this);
    	// start();
	}

	class TickTock {
		boolean firsttime = true;
		private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		float bpm;
		int bpb;
		float volume;
		AtomicInteger current = new AtomicInteger(0);
		int currentBeat = 0;
		ScheduledFuture<?> beeperHandle;
        final WakeUp wakeUp = new WakeUp();
		class WakeUp implements Runnable {
			@Override public void run() {
				if (changed.compareAndSet(true, false)) {
					beeperHandle.cancel(true);
			        beeperHandle = scheduler.scheduleAtFixedRate(
			        		wakeUp, 0, calculateMillis(), TimeUnit.MILLISECONDS);
			        scheduler.schedule(
			        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
			        		24, TimeUnit.HOURS);
			        return;
				}
				midiToSend.offer(currentBeat == 0 ? downbeatOn : beatOn);
				midiToSend.offer(currentBeat == 0 ? downbeatOff : beatOff);
				if (++currentBeat >= bpb)
					currentBeat = 0;
			}
		}

		public boolean isRunning() {
			return beeperHandle != null;
		}

		TickTock() {
			bpm = (Float)props.get("bpm");
			bpb = (Integer)props.get("bpb");
			volume = (Float)props.get("volume");
		}

		public void start() {
			if (isRunning()) return;

			long cycle = calculateMillis();
			log.debug("Metronome starting with a cycle of " + cycle + " for bpm: " + bpm);
			midiToSend.offer(midiStart);
			//  try { client.transportStart(); }
			// 	catch (JackException e) { log.error(e.getMessage(), e); }
			metroUi.start();
			beeperHandle = scheduler.scheduleAtFixedRate(
        		wakeUp, 0, calculateMillis(), TimeUnit.MILLISECONDS);
			scheduler.schedule(
        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
        		24, TimeUnit.HOURS);
		}

		public void stop() {
			if (!isRunning()) return;

			scheduler.shutdown();
			midiToSend.offer(midiStop);
			metroUi.stop();
			beeperHandle = null;
			//	try { client.transportStop(); }
			// catch (JackException e) {log.error(e.getMessage(), e);}
		}
		public void settings(Properties props) {
			Object param = null;
			float bpm = this.bpm;
			int bpb = this.bpb;
			float gain = this.volume;

			try {
				param = props.get("bpm");
				bpm = (Float)param;
			} catch (Throwable t) {
				// log.debug(t.getMessage() + " " + param);
			}
			try {
				param = props.get("bpb");
				bpb = (Integer)param;
				if (bpb <= 0) throw new InvalidParameterException("beats per bar has to be positive.");
			} catch (Throwable t) {
				// log.debug(t.getMessage() + " " + param);
			}
			try {
				param = props.get("volume");
				gain = (Float)param;
				if (gain > 1 || gain < 0) throw new InvalidParameterException("volume between 0 and 1: " + gain);
				int midiVolume = Constants.gain2midi(volume);
				downbeatOn = new Midi(downbeatOn.getCommand(), downbeatOn.getChannel(), downbeatOn.getData1(), midiVolume);
				beatOn = new Midi(beatOn.getCommand(), beatOn.getChannel(), beatOn.getData1(), midiVolume);
			} catch (Throwable t) {
				// log.debug(t.getMessage() + " " + param);
			}
			this.bpm = bpm;
			this.bpb = bpb;
			this.volume = gain;
			Metronome.this.props.put("bpm", bpm);
			Metronome.this.props.put("bpb", bpb);
			Metronome.this.props.put("volume", gain);
			metroUi.update();
			log.debug("Settings: bpm " + bpm + " bpb " + bpb + " volume " + gain + " running: " + isRunning());
			if (isRunning()) {
				changed.set(true);
			}
		}
		private long calculateMillis() {
			return Math.round(60000/ bpm); //  millis per minute / beats per minute
		}

	}

	public boolean isRunning() {
		if (ticktock == null) return false;
		return ticktock.isRunning();
	}

	boolean report = true;
	@Override
	public boolean syncPosition(JackClient client, JackPosition position, JackTransportState state) {
		if (report) {
			RTLogger.log(this, "position sync for " + state + NL + position);
			report = false;
		}
		return true;
	}

	@Override
	public void updatePosition(JackClient invokingClient, JackTransportState state, int nframes, JackPosition position,
			boolean newPosition) {
			// judah.debug("TIMEBASE UPDATE. Client: " + invokingClient.getName() + " state: " + state + " frames: " + nframes + " position: \n" + toString(position));
	}

	@Override
	public List<Command> getCommands() {
		return commands;
	}

	@Override
	public void execute(Command cmd, Properties props) throws Exception {

		if (cmd.equals(start)) {
			ticktock.stop();
			ticktock = new TickTock();
			ticktock.start();
		}

		if (cmd.equals(stop)) {
			ticktock.stop();
		}

		if (cmd.equals(settings)) {
			ticktock.settings(props);
		}
	}

	@Override
	public void close() {
		ticktock.stop();
	}


	@Override
	public Tab getGui() {
		return metroUi;
	}

	public Midi poll() {
		return midiToSend.poll();
	}
	

	public boolean process(JackClient client, int nframes) {
		// if (state.get() != ACTIVE) return false;

		Midi send = midiToSend.poll();
		if (send == null) return true;
		try {
			JackMidi.clearBuffer(out);
			JackMidi.eventWrite(out, 0, send.getMessage(), send.getLength());
		} catch (JackException e) {
			RTLogger.warn(this, e);
		}
		return true;
	}


	
//    /** NOTE: blocks while Midi jack client is initialized */
//	public JackPort getOutPort() {
//    	while (out == null) { // wait for initialization
//    		try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) { }
//    	}
//    	return out;
//    }

	@Override
	public String getServiceName() {
		return Metronome.class.getSimpleName();
	}


}


/*
JackLibrary$jack_position_t(auto-allocated@0x7f96b01a1bd0 (136 bytes)) {
  long unique_1@0=0
  long usecs@8=0
  int frame_rate@10=0
  int frame@14=0
  int valid@18=0
  int bar@1c=0
  int beat@20=0
  int tick@24=0
  double bar_start_tick@28=0.0
  float beats_per_bar@30=0.0
  float beat_type@34=0.0
  double ticks_per_beat@38=0.0
  double beats_per_minute@40=0.0
  double frame_time@48=0.0
  double next_time@50=0.0
  int bbt_offset@58=0
  float audio_frames_per_video_frame@5c=0.0
  int video_offset@60=0
  int padding[7]@64=[I@39f7722f
  long unique_2@80=0
}
 */

/*
    public void beepForAnHour() {
        final Runnable beeper = new Runnable() {
			@Override public void run() {
				current = System.currentTimeMillis();
				log.debug("beep, drift=" + (current - 1000 - old));
				old = current;
				if (beat == 0) {
					judah.offer(downbeat);
					judah.offer(bellOff);
				}
				else {
					judah.offer(woodOn);
					judah.offer(woodOff);
				}
				if (++beat == 4)
					beat = 0;
			}
        };

        old = System.currentTimeMillis();
        final ScheduledFuture<?> beeperHandle = scheduler.scheduleAtFixedRate(beeper, 1, 1, SECONDS);
        scheduler.schedule(new Runnable() {
                @Override public void run() {
                	beeperHandle.cancel(true);
                	}}, 60 * 60, SECONDS);
    }
*/

/* 1 second executor:
26:06 WARN  JudahZone:120 - ...:: TIME ::...
FrameTime: 388758019 drift: 48105
LastFrameTime: 388758016 drift: 48128
CurrentTransportFrame: 0 drift: 0
JackTime: 276811922748 drift: 1002137
System: 1575786366303 drift: 1002

26:07 WARN  JudahZone:120 - ...:: TIME ::...
FrameTime: 388805631 drift: 47612
LastFrameTime: 388805632 drift: 47616
CurrentTransportFrame: 0 drift: 0
JackTime: 276812914686 drift: 991938
System: 1575786367295 drift: 992
*/

// offer() {
//try {
//Timestamp current = new Timestamp();
//current.jt = judah.getJack().getTime();
//current.ft = l[0];
//current.lft = l[1];
//current.ctf = l[2];
//current.st = System.currentTimeMillis();
//if (previous != null)
//	judah.debug(previous.toString(current));
//previous = current;
//
//
//} catch (JackException e) {
//log.error(e.getMessage(), e);
//}}

//private String toString(JackPosition p) {
//StringBuffer sb = new StringBuffer();
//sb.append("Position current bar: ").append(p.getBar()).append(NL);
//sb.append(TAB).append("frame time: ").append(p.getFrameTime()).append(NL);
//sb.append(TAB).append("ticks/beat: ").append(p.getTicksPerBeat()).append(NL);
//sb.append(TAB).append("     ticks: ").append(p.getTick()).append(NL);
//return sb.toString();
//}
//class Timestamp {
//long ft;
//long lft;
//long ctf;
//long jt;
//long st;
//
//public String toString(Timestamp current) {
//StringBuffer sb = new StringBuffer("...:: TIME ::...").append(NL);
//sb.append("FrameTime: ").append(current.ft).append(" drift: ").append(current.ft - ft).append(NL);
////sb.append("LastFrameTime: ").append(current.lft).append(" drift: ").append(current.lft - lft).append(NL);
//sb.append("CurrentTransportFrame: ").append(current.ctf).append(" drift: ").append(current.ctf - ctf).append(NL);
////sb.append("JackTime: ").append(current.jt).append(" drift: ").append(current.jt - jt).append(NL);
////sb.append("System: ").append(current.st).append(" drift: ").append(current.st - st).append(NL);;
//return sb.toString();
//}
//}
//Timestamp previous = null;
