package net.judah.midi;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.util.Constants;

@Log4j
/** Utility Swing class to select MIDI messages */
public class MidiForm extends JPanel implements MidiListener {

	public static final int MAX_CHANNELS = 16;
	public static final int MAX_DATA = 128;

	@RequiredArgsConstructor
	/** handled commands (TODO) */
	public static enum MidiCommands {
		CONTROL_CHANGE(0xB0),  // 176
		PROGRAM_CHANGE(0xC0),  // 192
		NOTE_ON(0x90),  // 144
		NOTE_OFF(0x80) // 128
		;
		@Getter private final int val;
	}
	
	private static final int COMMENTS = 0;
	private static final int LABELS = 1;
	private static final int DATA = 2;
	
	private final JComboBox<MidiCommands> command; 
	private final JComboBox<Integer> data1, data2, channel;
	private final JToggleButton midiRecord;
	private final JButton midiPlay;
	private final JLabel comment;
	private final JCheckBox dynamic;
	
	public MidiForm(Midi midi) {
		this(midi, false);
	}
	
	public MidiForm(Midi midi, boolean isDynamic) {
		
		command = new JComboBox<MidiCommands>(MidiCommands.values());
		data1 = new JComboBox<Integer>(generateCombo(MAX_DATA));
		data2 = new JComboBox<Integer>(generateCombo(MAX_DATA));
		data2.setVisible(!isDynamic);
		channel = new JComboBox<Integer>(generateCombo(MAX_CHANNELS));
		midiRecord = new JToggleButton("⚫ Rec");
		midiRecord.addActionListener( (event) -> midiLearn());
		midiPlay = new JButton("▶ Play");
		midiPlay.addActionListener( (event) -> midiPlay());
		comment = new JLabel();
		dynamic = new JCheckBox("", isDynamic);
		dynamic.setEnabled(false);

		if (midi != null) {
			for(MidiCommands cmd : MidiCommands.values()) 
				if (cmd.getVal() == midi.getCommand())
					command.setSelectedItem(cmd);
			channel.setSelectedItem(midi.getChannel());
			data1.setSelectedItem(midi.getData1());
			data2.setSelectedItem(midi.getData2());
		}
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = COMMENTS;
		add(midiRecord, c);
		c.gridy = LABELS;
		add(new JLabel("Dynamic"), c);
		c.gridy = DATA;
		add(dynamic, c);
		
		c.gridx = 1;
		c.gridy = COMMENTS;
		add(midiPlay, c);
		c.gridy = LABELS;
		add(new JLabel("Channel"), c);
		c.gridy = DATA;
		add(channel, c);
		
		c.gridx = 2;
		c.gridy = LABELS;
		c.gridwidth = 3;
		add(comment, c);
		c.gridwidth = 1;
		c.gridy = LABELS;
		add(new JLabel("Command"), c);
		c.gridy = DATA;
		add(command, c);
		
		c.gridx = 3;
		c.gridy = LABELS;
		add(new JLabel("Data1"), c);
		c.gridy = DATA;
		add(data1, c);

		c.gridx = 4;
		c.gridy = LABELS;
		add(new JLabel(isDynamic ? "" : "Data2"), c);
		c.gridy = DATA;
		add(data2, c);
	}
	
	private void midiLearn() {
		CommandHandler.getInstance().setMidiListener(midiRecord.isSelected() ? this : null);
	}

	private void midiPlay() {
		MidiClient.getInstance().queue(getMidi());
	}

	private Integer[] generateCombo(int size) {
		Integer[] result = new Integer[size];
		for (int i = 0; i < size; i++) {
			result[i] = i;
		}
		return result;
	}

	public void hideDynamic() {
		dynamic.setVisible(false);
	}
	
	public void hideMidiRecord() {
		midiRecord.setVisible(false);
	}
	
	public void hideMidiPlay() {
		midiPlay.setVisible(false);
	}
	
	public void hideData2() {
		data2.setVisible(false);
	}
	
	public Midi getMidi() {
		int cmd = ((MidiCommands)command.getSelectedItem()).getVal();
		int chan = (int)channel.getSelectedItem();
		int dat1 = (int)data1.getSelectedItem();
		try {
			if (data2 == null || data2.isVisible() == false)
				return new Midi(cmd, chan, dat1);
			else {
				int dat2 = (int)data2.getSelectedItem();
				return new Midi(cmd, chan, dat1, dat2);
			}
		} catch (InvalidMidiDataException e) {
			log.error(e.getMessage() + " " + Arrays.toString(new Object[] {cmd, chan, dat1}), e);
			Constants.infoBox(e.getMessage(), "MIDI Error");
			return null;
		}
	}

	@Override
	public void feed(Midi midi) {
		for (MidiCommands cmd : MidiCommands.values()) 
			if (cmd.getVal() == midi.getCommand()) {
				command.setSelectedItem(cmd);
				channel.setSelectedItem(midi.getChannel());
				data1.setSelectedItem(midi.getData1());
				data2.setSelectedItem(midi.getData2());
			}
	}

	public void setChannel(int i) {
		channel.setSelectedItem(i);
	}

}
