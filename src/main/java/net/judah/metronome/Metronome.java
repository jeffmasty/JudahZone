package net.judah.metronome;

import static net.judah.util.Constants.Gui.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.midi.JudahReceiver;
import net.judah.midi.Midi;
import net.judah.midi.MidiClient;
import net.judah.midi.MidiPlayer;
import net.judah.settings.Command;
import net.judah.settings.DynamicCommand;
import net.judah.settings.Service;
import net.judah.util.Constants;
import net.judah.util.Tab;

@Log4j 
public class Metronome extends JPanel implements Service, ActionListener, ChangeListener {
	
	@Getter private final String serviceName = Metronome.class.getSimpleName();
	@Getter private final ArrayList<Command> commands = new ArrayList<>();
	@Getter private final Command start, stop, settings, tempoCmd, volumeCmd, clicktrack;

	/** if null, generate midi notes ourself */
    //public static final File midiFile = new File(JudahZone.class.getClassLoader().getResource("metronome/Rock1.mid").getFile());
	private File midiFile = new File("/home/judah/git/JudahZone/resources/metronome/Latin16.mid");
	private int measure = 4;
	private float tempo = 100f;
	private float gain = 1f;
	
	private JudahReceiver receiver;
	private MidiPlayer playa;
	private TickTock ticktock;
	
	private Midi midiStart, midiStop;
	private final AtomicBoolean changed = new AtomicBoolean(false);

	private JToggleButton playBtn;
    private JToggleButton stopBtn;
    private JButton bpbBtn;
    private JButton fileBtn;
    
    // private JTextField bpb;
    private JTextField bpmText;
    private JSlider bpm;
    private JSlider volume;

	class TickTock {
    	
		private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		private final MidiClient midi;
		boolean firsttime = true;
    	private Midi downbeatOn, downbeatOff, beatOn, beatOff; //:>
		
		AtomicInteger current = new AtomicInteger(0);
		int currentBeat = 0;
		ScheduledFuture<?> beeperHandle;
        final WakeUp wakeUp = new WakeUp();
        
		class WakeUp implements Runnable {
			@Override public void run() {
				if (changed.compareAndSet(true, false)) {
					beeperHandle.cancel(true);
			        beeperHandle = scheduler.scheduleAtFixedRate(
			        		wakeUp, 0, Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
			        scheduler.schedule(
			        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
			        		24, TimeUnit.HOURS);
			        return;
				}
				
				midi.queue(currentBeat == 0 ? downbeatOn : beatOn);
				midi.queue(currentBeat == 0 ? downbeatOff : beatOff);
				if (++currentBeat >= measure)
					currentBeat = 0;
			}
		}

		TickTock(MidiClient midi) {
    		this.midi = midi;
	    	try {
	    		midiStart = new Midi(ByteBuffer.allocate(4).putInt(ShortMessage.START).array());
	    		midiStop = new Midi(ByteBuffer.allocate(4).putInt(ShortMessage.STOP).array());
	    		downbeatOn = new Midi(ShortMessage.NOTE_ON, 9, 34, Constants.gain2midi(gain)); // bell
	    		downbeatOff = new Midi(ShortMessage.NOTE_OFF, 9, 34);
	    		beatOn = new Midi(ShortMessage.NOTE_ON, 9, 33, Constants.gain2midi(gain)); // wood block
	    		beatOff = new Midi(ShortMessage.NOTE_OFF, 9, 33);

	    	} catch (InvalidMidiDataException e) {
	    		log.error(e.getMessage(), e);
	    	}
		}

		public boolean isRunning() {
			return beeperHandle != null;
		}

		public void start() {
			if (isRunning()) return;

			long cycle = Constants.millisPerBeat(tempo);
			log.debug("Metronome starting with a cycle of " + cycle + " for bpm: " + bpm);
			midi.queue(midiStart);

			beeperHandle = scheduler.scheduleAtFixedRate(
        		wakeUp, 0, Constants.millisPerBeat(tempo), TimeUnit.MILLISECONDS);
			scheduler.schedule(
        		new Runnable() {@Override public void run() {beeperHandle.cancel(true);}},
        		24, TimeUnit.HOURS);
		}

		public void stop() {
			if (!isRunning()) return;
			scheduler.shutdown();
			midi.queue(midiStop);
			beeperHandle = null;
		}

		public void setGain(float gain) {
			int midiVolume = Constants.gain2midi(gain);
			try {
				downbeatOn = new Midi(downbeatOn.getCommand(), downbeatOn.getChannel(), downbeatOn.getData1(), midiVolume);
				beatOn = new Midi(beatOn.getCommand(), beatOn.getChannel(), beatOn.getData1(), midiVolume);
			} catch (InvalidMidiDataException e) {
				log.error(e.getMessage(),e);
			}
		}

	}

	public boolean isRunning() {
		if (ticktock != null && ticktock.isRunning()) return true;
		if (playa != null && playa.getSequencer().isRunning()) return true;
		return false;
	}
    
	public Metronome() {
		start = new Command("tick", this, "Start the metronome.");
		stop = new Command("tock", this, "Stop the metronome.");

		HashMap<String, Class<?>> params = new HashMap<String, Class<?>>();
		params.put("bpm", Float.class);
		tempoCmd = new DynamicCommand("Metro tempo", this, params, "Tempo of metronome") {
			@Override public void processMidi(int data2, HashMap<String, Object> props) {
				props.put("bpm", (data2 + 40) * 1.1f); }};

		params = new HashMap<String, Class<?>>();
		params.put("volume", Float.class);
		volumeCmd = new DynamicCommand("Metro volume", this, params, "Volume of metronome") {
			@Override public void processMidi(int data2, java.util.HashMap<String,Object> props) {
				props.put("volume", ((data2 - 1))* 0.01f); }};

		// todo remove gui command?
		params = new HashMap<String, Class<?>>();
		params.put("bpm", Float.class);
		params.put("bpb", Integer.class);
		params.put("volume", Float.class);
		settings = new Command("Metronome settings", this, params, "Adjust metronome settings.");
		
		params = new HashMap<String, Class<?>>();
		params.put("intro.bars", Integer.class);
		params.put("duration.bars", Integer.class);
		params.put("file", String.class);
		clicktrack = new Command("Clicktrack", this, params, "start a clicktrack");
		
		commands.addAll(Arrays.asList(new Command[] {start, stop, settings, tempoCmd, volumeCmd, clicktrack}));

		createGui();
	}
	
	private void createGui() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel banner = new JPanel();
		playBtn = new JToggleButton("Play");
		playBtn.setFont(BOLD);
		playBtn.addActionListener(this);
		stopBtn = new JToggleButton("Stop");
		stopBtn.setFont(BOLD);
		stopBtn.addActionListener(this);
		stopBtn.setSelected(true);
		
		JPanel bpmPnl = new JPanel();
		bpmPnl.setLayout(new BoxLayout(bpmPnl, BoxLayout.Y_AXIS));
		JLabel bpmLbl = new JLabel("BPM");
		bpmLbl.setFont(FONT12);
		bpmText = new JTextField(2);
		bpmText.setFont(FONT12);
		bpmText.addActionListener(this);
		bpmPnl.add(bpmLbl);
		bpmPnl.add(bpmText);
		
		JPanel indicatorPanel = new JPanel();
		indicatorPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		
		bpbBtn = new JButton("BPB");
		bpbBtn.setMargin(BTN_MARGIN);
		bpbBtn.setFont(FONT11);
		
		fileBtn = new JButton(" File ");
		fileBtn.setMargin(BTN_MARGIN);
		fileBtn.setFont(FONT11);
		indicatorPanel.add(bpbBtn);
		indicatorPanel.add(fileBtn);
		
		banner.add(playBtn);
		banner.add(stopBtn);
		banner.add(bpmPnl);
		banner.add(indicatorPanel);
		add(banner);

		JPanel tempoPanel = new JPanel();
		JLabel tempoLbl = new JLabel("Tempo");
		tempoLbl.setFont(FONT13);
		tempoPanel.add(tempoLbl);
		
		bpm = new JSlider(JSlider.HORIZONTAL, 60, 180, Math.round(tempo));
		bpm.setFont(FONT9);
		bpm.setMajorTickSpacing(20);
		bpm.setPaintTicks(false);
		bpm.setPaintLabels(true);
		bpm.addChangeListener(this);
		tempoPanel.add(bpm);
		add(tempoPanel);

		JPanel volumePanel = new JPanel();
		JLabel volumeLbl = new JLabel("Volume");
		volumeLbl.setFont(FONT13);
		volumePanel.add(volumeLbl);
		
		volume = new JSlider(JSlider.HORIZONTAL, 0, 100, Math.round(gain * 100));
		volume.setFont(FONT9);
		volume.setMajorTickSpacing(20);
		volume.setPaintTicks(true);
		volume.setPaintLabels(false);
		volume.addChangeListener(this);
		volumePanel.add(volume);
		add(volumePanel);
		update();
	}

	public void update() {
		// update UI
		// bpb.setText("" + props.get("bpb"));
		bpmText.setText("" + tempo);
		bpm.setValue(Math.round(tempo));
		volume.setValue(Math.round(100f * gain));
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		try {
			if (e.getSource() == volume) {
				HashMap<String, Object> p = makeProps();
				p.put("volume", volume.getValue() / 100f);
				execute(settings, p);
			}
			if (e.getSource() == bpm) {
				HashMap<String, Object> p = makeProps();
				p.put("bpm", bpm.getValue() * 1f);
				execute(settings, p);
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (e.getSource() == playBtn) {
				execute(start, null);
				playBtn.setSelected(true);
			}
			if (e.getSource() == stopBtn) {
				execute(stop, null);
				stopBtn.setSelected(true);
			}
//			if (e.getSource() == bpb) {
//				try {
//					props.put("bpb", Integer.parseInt(bpb.getText()));
//					execute(settings, props);
//				} catch (Throwable t) {
//					log.info(t.getMessage());
//					bpb.setText("" + props.get("bpb"));
//				}
//			}
//			if (e.getSource() == bpmText) {
//				try {
//					props.put("bpm", Float.parseFloat(bpmText.getText()));
//					execute(settings, props);
//				} catch (Throwable t) {
//					log.info(t.getMessage());
//					bpmText.setText("" + props.get("bpb"));
//				}
//			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

	private HashMap<String, Object> makeProps() {
		HashMap<String, Object> p = new HashMap<String, Object>();
		p.put("bpm", tempo);
		p.put("bpb", measure);
		p.put("volume", gain);
		return p;
	}
	
	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {

		if (cmd.equals(start)) {
			if (ticktock != null) {
				ticktock.stop();
			}
			if (playa != null) {
				playa.stop();
			}
			
			if (midiFile == null) {
				ticktock = new TickTock(MidiClient.getInstance());
				ticktock.start();
			}
			else {
				receiver = new JudahReceiver(MidiClient.getInstance());
				playa = new MidiPlayer(midiFile, tempo, MidiPlayer.LOOP, receiver);
				playa.start();
			}
			
			playBtn.setSelected(true);
			stopBtn.setSelected(false);
		}

		if (cmd.equals(stop)) {
			if (ticktock != null) {
				ticktock.stop();
				ticktock = null;
			}
			if (playa != null) {
				playa.stop();
				playa = null;
			}
			playBtn.setSelected(false);
			stopBtn.setSelected(true);
		}

		if (cmd.equals(settings)) 
			settings(props);
		if (cmd.equals(tempoCmd))
			settings(props);
		if (cmd.equals(volumeCmd)) {
			settings(props);
		}
		
	}

	public void settings(HashMap<String, Object> props) {
		Object param = null;
		try {
			param = props.get("bpm");
			tempo = (Float)param;
			if (playa != null) {
				playa.getSequencer().setTempoFactor(tempo/100f);
			}
		} catch (Throwable t) { }
		
		try {
			param = props.get("bpb");
			measure = (Integer)param;
			if (measure <= 0) throw new InvalidParameterException("beats per bar has to be positive.");
		} catch (Throwable t) {	}
		
		try {
			param = props.get("volume");
			gain = (Float)param;
			if (gain > 1 || gain < 0) throw new InvalidParameterException("volume between 0 and 1: " + gain);
	
			if (ticktock != null)
				ticktock.setGain(gain);
			if (receiver != null) 
				receiver.setGain(gain);
		} catch (Throwable t) {	}
		
		update();
		if (isRunning()) {
			changed.set(true);
		}
	}

	@Override
	public void close() {
		if (ticktock != null)
			ticktock.stop();
		if (playa != null) {
			playa.close();
		}
	}

	@Override
	public Tab getGui() {
		return null;
	}

	
}
