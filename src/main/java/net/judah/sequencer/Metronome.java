package net.judah.sequencer;

import static javax.sound.midi.Sequencer.*;
import static net.judah.util.Constants.Gui.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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
import net.judah.JudahZone;
import net.judah.midi.JudahReceiver;
import net.judah.midi.MidiClient;
import net.judah.midi.MidiPlayer;
import net.judah.settings.Command;
import net.judah.settings.DynamicCommand;
import net.judah.settings.Service;
import net.judah.util.FileChooser;

@Log4j 
public class Metronome extends JPanel implements Service, ActionListener, ChangeListener {
	
	public static final String PARAM_GAIN = "volume";
	public static final String PARAM_TEMPO = "bpm";
	public static final String PARAM_MEASURE = "bpb";
	public static final String PARAM_FILE = "midi.file";
	
	@Getter private final String serviceName = Metronome.class.getSimpleName();
	@Getter private final ArrayList<Command> commands = new ArrayList<>();

	private final Command start, stop, settings;
	private final DynamicCommand tempoCmd, volumeCmd;
	
	private final ClickTrack clicktrack;
	private final Sequencer sequencer;

	/** if null, generate midi notes ourself */
    public static File midiFile = new File(JudahZone.class.getClassLoader().getResource("metronome/Latin16.mid").getFile());
	private float gain = 0.8f;
	
	private MetroPlayer playa;
	
	private final AtomicBoolean changed = new AtomicBoolean(false);

	private JToggleButton playBtn;
    private JToggleButton stopBtn;
    private JButton bpbBtn;
    private JButton fileBtn;
    
    private JTextField bpmText;
    private JSlider bpm;
    private JSlider volume;

    private boolean suppress = false;
    
	public Metronome(Sequencer sequencer) {
		this.sequencer = sequencer;
		
		Object o = sequencer.getSong().getProps().get(PARAM_FILE);
		if (new File("" + o).isFile()) {
			midiFile = new File("" + o);
		}
		
		start = new Command("tick", this, "Start the metronome.");
		stop = new Command("tock", this, "Stop the metronome.");

		HashMap<String, Class<?>> params = new HashMap<String, Class<?>>();
		params.put("bpm", Float.class);
		tempoCmd = new DynamicCommand("Metro tempo", this, params, "Tempo of metronome") {
			@Override public void processMidi(int data2, HashMap<String, Object> props) {
				props.put("bpm", (data2 + 35) * 1.15f); }};

		params = new HashMap<String, Class<?>>();
		params.put("volume", Float.class);
		volumeCmd = new DynamicCommand("Metro volume", this, params, "Volume of metronome") {
			@Override public void processMidi(int data2, java.util.HashMap<String,Object> props) {
				props.put("volume", ((data2))* 0.01f); }};
				
		
		// todo remove gui command?
		params = new HashMap<String, Class<?>>();
		params.put(PARAM_TEMPO, Float.class);
		params.put(PARAM_MEASURE, Integer.class);
		params.put(PARAM_GAIN, Float.class);
		settings = new Command("Metronome settings", this, params, "Adjust metronome settings.");
		clicktrack = new ClickTrack(sequencer, this);
		commands.addAll(Arrays.asList(new Command[] {start, stop, settings, tempoCmd, volumeCmd, clicktrack}));
		sequencer.getServices().add(this);

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
		fileBtn.addActionListener( (event) -> loadMidi());
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

		bpm = new JSlider(JSlider.HORIZONTAL, 45, 155, Math.round(sequencer.getTempo()));
		bpm.setFont(FONT9);
		bpm.setMajorTickSpacing(25);
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
		volume.setMajorTickSpacing(25);
		volume.setPaintTicks(true);
		volume.setPaintLabels(false);
		volume.addChangeListener(this);
		volumePanel.add(volume);
		add(volumePanel);
		update();
	}

	public void loadMidi() {
		File file = FileChooser.choose(JFileChooser.FILES_ONLY, "mid", "Midi files (*.mid)");
		if (file == null) ;
		midiFile = file;
	}

	public boolean isRunning() {
		return playa != null && playa.isRunning();
	}
	
	/** update UI */
	public void update() {
		float tempo = sequencer.getTempo();
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
				if (suppress) {
					suppress = false;
					return;
				}
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
			
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

	private HashMap<String, Object> makeProps() {
		HashMap<String, Object> p = new HashMap<String, Object>();
		p.put(PARAM_TEMPO, sequencer.getTempo());
		p.put(PARAM_MEASURE, sequencer.getMeasure());
		p.put(PARAM_GAIN, gain);
		return p;
	}
	
	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {

		if (cmd.equals(start)) {
			if (playa != null && playa.isRunning())
				playa.stop();
			
			if (playa == null) {
				if (midiFile == null) {
					playa = new TickTock(sequencer);
				} else {
					playa = new MidiPlayer(midiFile, LOOP_CONTINUOUSLY, 
							new JudahReceiver(MidiClient.getInstance()), sequencer);
				}
			}
			else 
				log.info("Using our queued up MetroPlayer: " + playa);
			
			playa.start();
			playBtn.setSelected(true);
			stopBtn.setSelected(false);
		}

		if (cmd.equals(stop)) {
			
			if (playa != null && playa.isRunning()) {
				playa.stop();
				playa = null;
			}
			else {
				// forward on to sequencer
				sequencer.stop();
				sequencer.getMixer().stopAll();
			}
			
			playBtn.setSelected(false);
			stopBtn.setSelected(true);
		}

		if (cmd.equals(settings)) 
			settings(props);
		if (cmd.equals(tempoCmd)) {
			suppress = true;
			settings(props);
		}
		if (cmd.equals(volumeCmd)) 
			settings(props);
		if (cmd.equals(clicktrack)) 
			clicktrack.execute(props);
	}

	public void mute() {
		if (playa != null) {
			playa.setGain(0f);
		}
	}
	
	public void unMute() {
		if (playa != null) {
			playa.setGain(gain);
		}
	}
	
	public void setVolume(float gain) {
		if (gain > 1 || gain < 0) throw new InvalidParameterException("volume between 0 and 1: " + gain);
		this.gain = gain;
		if (playa != null) {
			playa.setGain(gain);
		}
	}
	
	private void settings(HashMap<String, Object> props) {
		
		Object param = null;
		
		param = props.get(PARAM_GAIN);
		if (param != null && !param.toString().isEmpty())
			setVolume((Float)param);
		
		param = props.get(PARAM_TEMPO);
		if (param != null && !param.toString().isEmpty())
			sequencer.setTempo((Float)param);
		
		update();
		if (isRunning()) {
			changed.set(true);
		}
	}

	@Override
	public void close() {
		if (playa != null) {
			playa.close();
			playa = null;
		}
		
	}

	/** store a ticktock object configured and ready to begin transport */
	void setPlayer(MetroPlayer ticktock) {
		playa = ticktock;
	}

	public void setTempo(float tempo) {
		if (playa != null)
			if (playa instanceof MidiPlayer)
				((MidiPlayer)playa).getSequencer().setTempoFactor(tempo/100f);
		bpmText.setText("" + tempo);
		bpm.setValue(Math.round(tempo));
	}
	
}

//private JTextField bpb;

//if (e.getSource() == bpb) {
//	try {
//		props.put("bpb", Integer.parseInt(bpb.getText()));
//		execute(settings, props);
//	} catch (Throwable t) {
//		log.info(t.getMessage());
//		bpb.setText("" + props.get("bpb"));
//	}
//}
//if (e.getSource() == bpmText) {
//	try {
//		props.put("bpm", Float.parseFloat(bpmText.getText()));
//		execute(settings, props);
//	} catch (Throwable t) {
//		log.info(t.getMessage());
//		bpmText.setText("" + props.get("bpb"));
//	}
//}
//try {
//param = props.get("bpm");
//tempo = (Float)param;
//if (playa != null) {
//	playa.getSequencer().setTempoFactor(tempo/100f);
//}
//} catch (Throwable t) { }
//try {
//param = props.get("bpb");
//measure = (Integer)param;
//if (measure <= 0) throw new InvalidParameterException("beats per bar has to be positive.");
//} catch (Throwable t) {	}




