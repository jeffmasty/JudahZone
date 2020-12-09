package net.judah.util;

import static net.judah.util.Constants.*;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Level;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Midi;
import net.judah.fluid.FluidSynth;
import net.judah.jack.ProcessAudio;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.metronome.JMidiPlay;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiListener;
import net.judah.midi.Route;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
import net.judah.sequencer.Sequencer;

@Log4j
public class Console implements ActionListener, ConsoleParticipant, MidiListener {
	
	private static final String midiPlay = "midi filename (play a midi file)";
	private static final String listenHelp = "midilisten (toggle midi output to console)";
	private static final String saveHelp = "save loop_num filename (saves looper to disc)"; 
	private static final String readHelp = "read loop_num filename (reads Sample audio from disk into looper)";
	private static final String playHelp = "play filename or sample index (play a sample)";
	private static final String stopHelp = "stop index (stop a sample)";
	private static final String samples = "samples : list samples";
	private static final String routerHelp = "router - prints current midi translations";
	private static final String routeHelp = "route/unroute channel fromChannel# toChannel#";
	private static final String activeHelp ="active - prints the current sequencer command from the top of the stack.";
	
	private static Console instance;
	public static Console getInstance() {
		if (instance == null) instance = new Console();
		return instance;
	}
	@Getter @Setter private static Level level = Level.DEBUG;
	private final JTextArea textarea;
	@Getter private final JPanel output;
	@Getter private final JScrollPane scroller;
	@Getter private final JTextField input;
	private boolean midiListen = false;
	private String history = null;
	@Getter private ArrayList<ConsoleParticipant> participants = new ArrayList<>();
	
	private Console() {

        textarea = new JTextArea(8, 60);
        textarea.setEditable(false);
        
        scroller = new JScrollPane(textarea);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        output = new JPanel();
        output.setLayout(new BoxLayout(output, BoxLayout.Y_AXIS));
        output.add(scroller);
        
        
		input = new JTextField(70);
		input.setMaximumSize(new Dimension(625, 75));
        input.addActionListener(this);
        input.grabFocus();
        
        participants.add(this);
        participants.add(FluidSynth.getInstance().getConsole());
        
        instance = this;
	}

	protected String parseInputBox(ActionEvent e) {
		if (e.getSource() instanceof JTextField == false) return null;
		JTextField widget = (JTextField)e.getSource();
		String text = widget.getText();
		widget.setText("");
		if (text.isEmpty() && history != null) {
			addText(history);
			return history;
		}
		addText("> " + text);
		history = text;
		return text;
	}

	//** output to console */
	public static void addText(String s) {
		
		if (instance == null || instance.textarea == null) {
			return;
		}
		if (s == null) {
			s = "null" + NL;
		}
		
		if (false == s.endsWith(NL))
			s = s + NL;

		instance.textarea.append(s);
		
		instance.textarea.setCaretPosition(instance.textarea.getDocument().getLength() - 1);
		instance.scroller.getVerticalScrollBar().setValue( instance.scroller.getVerticalScrollBar().getMaximum() - 1 );
		instance.scroller.getHorizontalScrollBar().setValue(0);
		instance.output.invalidate();
	}

	public static void newLine() {
		addText("" + NL);
	}

	public static void warn(String s) {
		addText("WARN " + s);
	}
	
	public static void info(String s) {
		if (level == Level.DEBUG || level == Level.INFO || level == Level.TRACE)
			addText(s);
	}
	
	public static void debug(String s) {
		if (level == Level.DEBUG || level == Level.TRACE)
			addText("debug " + s);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		String text = parseInputBox(event);
		
		if (text.isEmpty() && history != null) {
			addText(history);
			text = history;
		}
		else {
			addText(text);
			history = text;
		}
			
		String[] split = text.split(" ");
		
		for (ConsoleParticipant p : participants) {
			p.process(split);
		}
	}

	@Override
	public void process(String[] input) {
		String text = input[0];
		if (text.equalsIgnoreCase("help")) {
			help();
			return;
		}
		//  set_active	set_parameter_value set_volume
		try {
			if (text.equals("volume") && input.length == 3) 
				getCarla().setVolume(Integer.parseInt(input[1]), Float.parseFloat(input[2]));
			else if (text.equals("active") && input.length == 3) 
				getCarla().setActive(Integer.parseInt(input[1]), 
						Integer.parseInt(input[2]));
			else if (text.equals("parameter") && input.length == 4) 
				getCarla().setParameterValue(Integer.parseInt(input[1]), 
						Integer.parseInt(input[2]), Float.parseFloat(input[3]));
		} catch (Exception e) {
			Console.warn(e.getMessage());
		}
		
		if (text.equals("midi")) 
			midiPlay(input);
		else if (text.equals("midilisten"))
			midiListen();
		else if (text.equals("save"))
			save(input);
		else if (text.equals("read")) 
			read(input);
		else if (text.equals("play"))
			play(input);
		else if (text.equals("stop"))
			if (input.length == 2)
				mixer().stopAll();
			else 
				stop(input);
			
		else if (text.equals("samples")) 
			addText( Arrays.toString(Sequencer.getCurrent().getMixer().getSamples().toArray()));
		else if (text.equals("router")) 
			for (Route r : JudahMidi.getInstance().getRouter())
				addText("" + r);
		
		else if (text.equals("route")) 
			route(input);
		else if (text.equals("unroute"))
			unroute(input);
		else if (text.equals("active"))
			Console.info("" + Sequencer.getCurrent().getActive());
		else if (text.equals("test")) {
			test();
		}

	}
	
	private void help() {
		
		addText("volume carla_plugin_index value");
		addText("active carla_plugin_index 1_or_0");
		addText("parameter carla_plugin_index parameter_index value");
		addText(saveHelp);
		addText(readHelp);
		addText(playHelp);
		addText(stopHelp);
		addText(samples);
		addText(playHelp);
		addText(routeHelp);
		addText(routerHelp);
		addText(midiPlay);
		addText(listenHelp);
		addText(activeHelp);
		addText("fluid help");
		
	}
	private void midiListen() {
		midiListen = !midiListen;
		Sequencer.getCurrent().getCommander().setMidiListener(midiListen ? this : null);
	}
	
	private void midiPlay(String[] split) {
		try {
			File midiFile = null;
			if (split.length == 2) {
					midiFile = new File(split[1]);
					if (!midiFile.isFile()) {
						throw new FileNotFoundException(split[1]);
					}
			}
			if (midiFile == null) {
				addText("uh-oh, no midi file.");
				midiFile = new File("/home/judah/Tracks/midi/dance/dance21.mid");
			}
			
			new JMidiPlay(midiFile); 
					
		} catch (Throwable t) {
			addText(t.getMessage());
			log.error(t.getMessage(), t);
		}
	}

	private Mixer mixer() {
		return Sequencer.getCurrent().getMixer();
	}
	
	private void play(String[] split) {
		if (split.length != 2) {
			addText("usage: " + playHelp);
			return;
		}
		String file = split[1];
		try {
			Sample sample = new Sample(new File(file).getName(), Recording.readAudio(file), ProcessAudio.Type.ONE_TIME);
			mixer().addSample(sample);
			sample.play(true);
		} catch (Throwable t) {
			addText(t.getMessage());
			log.error(t.getMessage(), t);
		}
	}

	private void stop(String[] split) {
		if (split.length != 2) {
			addText("usage: " + stopHelp);
			return;
		}
		Integer idx = Integer.parseInt(split[1]);
		mixer().removeSample(idx);
		addText("Sample removed");
		
	}
	private void read(String[] split) {
		if (split.length != 3 || !NumberUtils.isDigits(split[1])) {
			addText("Format: " + readHelp);
			return;
		}
		int loopNum = Integer.parseInt(split[1]);
		String filename = split[2];
		int loopMax = mixer().getSamples().size();
		if (loopNum < 0 || loopNum >= loopMax) {
			addText("loop " + loopNum + " does not exist.");
			return;
		}
		Sample loop = mixer().getSamples().get(loopNum);

		try {
			Recording recording = Recording.readAudio(filename);
			loop.setRecording(recording);
			float seconds = recording.size() / JudahMidi.getInstance().getSampleRate(); 
			addText(seconds + " of " + filename + " read, stored in loop " + loopNum + ". " + recording.getNotes());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			addText(e.getMessage());
		}
	}

	private void save(String[] split) {
		if (split.length != 3 || !NumberUtils.isDigits(split[1])) {
			addText("Format: " + saveHelp);
			return;
		}
		int loopNum = Integer.parseInt(split[1]);
		String filename = split[2];
		int loopMax = mixer().getSamples().size();
		if (loopNum < 0 || loopNum >= loopMax) {
			addText("loop " + loopNum + " does not exist.");
			return;
		}
		Sample loop = mixer().getSamples().get(loopNum);
		if (!loop.hasRecording()) {
			addText("Nothing in Loop " + loopNum);
			return;
		}
		
		Recording recording = loop.getRecording();
		if (recording == null || recording.isEmpty()) {
			addText("No recording in looper " + loopNum + ".");
			return;
		}
		try {
			recording.setNotes("Hello, world");
			recording.saveAudio(filename);
			float seconds = recording.size() / JudahMidi.getInstance().getSampleRate();
			addText(seconds + " of loop " + loopNum + " saved to " + filename);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			addText(e.getMessage());
		}
	}
	
	private void route(String[] split) {
		if (split.length == 4 && split[1].equals("channel")) {
			try {
				int from = Integer.parseInt(split[2]);
				int to = Integer.parseInt(split[3]);
				JudahMidi.getInstance().getRouter().add(new Route(from, to));				
				
			} catch (NumberFormatException e) {
				addText(routeHelp + " (" + Arrays.toString(split) + ")");
			}
		}
	}

	private void unroute(String[] split) {
		if (split.length == 4 && split[1].equals("channel")) {
			try {
				int from = Integer.parseInt(split[2]);
				int to = Integer.parseInt(split[3]);
				JudahMidi.getInstance().getRouter().remove(new Route(from, to));				
				
			} catch (NumberFormatException e) {
				addText(routeHelp + " (" + Arrays.toString(split) + ")");
			}
		}
	}

	private Carla getCarla() {
		return Sequencer.getCarla();
	}

	@Override
	public void feed(Midi midi) {
		addText("midilisten: " + midi);
	}
	
	@Override
	public PassThrough getPassThroughMode() {
		return PassThrough.ALL;
	}

	public void test() {
		
	}
	
}