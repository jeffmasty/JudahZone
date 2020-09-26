package net.judah.util;

import static net.judah.util.Constants.*;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.jack.ProcessAudio.Type;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.midi.JudahReceiver;
import net.judah.midi.Midi;
import net.judah.midi.MidiClient;
import net.judah.midi.MidiListener;
import net.judah.midi.MidiPlayer;
import net.judah.midi.Route;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;

@Log4j
public class Console extends JComponent implements ActionListener, ConsoleParticipant, MidiListener {
	
	
	private static final String midiPlay = "midi filename (play a midi file)";
	private static final String listenHelp = "midilisten (toggle midi output to console)";
	private static final String saveHelp = "save loop_num filename (saves looper to disc)"; 
	private static final String readHelp = "read loop_num filename (reads bytes from disc into looper)";
	private static final String playHelp = "play filename or sample index (play a sample)";
	private static final String stopHelp = "stop index (stop a sample)";
	private static final String samples = "samples : list samples";
	private static final String routerHelp = "router - prints current midi translations";
	private static final String routeHelp = "route/unroute channel fromChannel# toChannel#";
	
	private static Console me;
	@Getter @Setter private static Level level = Level.DEBUG;
	private final JTextArea output;
	private final JScrollPane listScroller;
	private boolean midiListen = false;
	private String history = null;
	@Getter private ArrayList<ConsoleParticipant> participants = new ArrayList<>();
	
	public static Console getInstance() {
		if (me == null) me = new Console();
		return me;
	}
	
	Console() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        output = new JTextArea();
        output.setEditable(false);
        listScroller = new JScrollPane(output);
        listScroller.setBorder(new EtchedBorder());
        listScroller.setPreferredSize(new Dimension(680, 350));
        listScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listScroller.setAlignmentX(LEFT_ALIGNMENT);
        add(listScroller);

		JTextField input = new JTextField(70);
		input.setMaximumSize(new Dimension(660, 75));
        add(input);
        input.addActionListener(this);
        input.grabFocus();
        
        participants.add(this);
        participants.add(FluidSynth.getInstance().getConsole());
        
        me = this;
        
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

	public static void addText(String s) {
		
		if (me == null || me.output == null) {
			return;
		}
		if (s == null) {
			s = "null" + NL;
			Logger.getLogger(Tab.class).info("null addText() to terminal");
		}
		
		if (false == s.endsWith(NL))
			s = s + NL;
		me.output.append(s);
		Rectangle r = me.output.getBounds();
		me.listScroller.getViewport().setViewPosition(new Point(0, r.height));
	}

	//** output to console */
	public static void newLine() {
		addText("" + NL);
	}

	public static void warn(String s) {
		
	}
	
	public static void info(String s) {
		if (level == Level.DEBUG || level == Level.INFO)
			addText(s);
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
		if (text.startsWith("xruns") || text.equalsIgnoreCase("xrun")) {
			addText(MidiClient.getInstance().xrunsToString());
			return;
		}
		
		//  set_active	set_parameter_value set_volume 
		if (text.startsWith("volume ") && input.length == 3) 
			getCarla().setVolume(Integer.parseInt(input[1]), Float.parseFloat(input[2]));
		else if (text.startsWith("active ") && input.length == 3) 
			getCarla().setActive(Integer.parseInt(input[1]), 
					Integer.parseInt(input[2]));
		else if (text.startsWith("parameter ") && input.length == 4) 
			getCarla().setParameterValue(Integer.parseInt(input[1]), 
					Integer.parseInt(input[2]), Float.parseFloat(input[3]));
		
		else if (text.equals("midi")) 
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
			addText( Arrays.toString(JudahZone.getCurrentSong().getMixer().getSamples().toArray()));
		else if (text.equals("router")) 
			for (Route r : MidiClient.getInstance().getRouter())
				addText("" + r);
		
		else if (text.equals("route")) 
			route(input);
		else if (text.equals("unroute "))
			unroute(input);

	}
	
	private void help() {
		addText("xrun");
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
	}
	private void midiListen() {
		midiListen = !midiListen;
		JudahZone.getCurrentSong().getCommander().setMidiListener(midiListen ? this : null);
	}
	
	private void midiPlay(String[] split) {
		try {
			File midiFile = null;
			if (split.length == 2) {
				try {
					midiFile = new File(split[1]);
					if (!midiFile.isFile()) {
						throw new FileNotFoundException(split[1]);
					}
				} catch (Throwable t) {
					addText(t.getMessage());
					log.info(t.getMessage(), t);
				}
			}
			if (midiFile == null) {
				addText("uh-oh, no midi file.");
				midiFile = new File("/home/judah/Tracks/midi/dance/dance21.mid");
			}
			
			MidiPlayer playa = new MidiPlayer(midiFile, 8, 
					new JudahReceiver(MidiClient.getInstance()), null);
			playa.start();
		} catch (Throwable t) {
			addText(t.getMessage());
			log.error(t.getMessage(), t);
		}
	}

	private Mixer mixer() {
		return JudahZone.getCurrentSong().getMixer();
	}
	
	private void play(String[] split) {
		if (split.length != 2) {
			addText("usage: " + playHelp);
			return;
		}
		String file = split[1];
		try {
			Sample sample = new Sample(new File(file).getName(), Recording.readAudio(file), Type.ONE_TIME);
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
			float seconds = recording.size() / MidiClient.getInstance().getSampleRate(); 
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
			float seconds = recording.size() / MidiClient.getInstance().getSampleRate();
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
				MidiClient.getInstance().getRouter().add(new Route(from, to));				
				
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
				MidiClient.getInstance().getRouter().remove(new Route(from, to));				
				
			} catch (NumberFormatException e) {
				addText(routeHelp + " (" + Arrays.toString(split) + ")");
			}
		}
		
	}

	private Carla getCarla() {
		return JudahZone.getCurrentSong().getCarla();
	}

	@Override
	public void feed(Midi midi) {
		addText("midilisten: " + midi);
	}
	
	@Override
	public PassThrough getPassThroughMode() {
		return PassThrough.ALL;
	}

	
}
