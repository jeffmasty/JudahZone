package net.judah.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.judah.api.Midi;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiListener;
import net.judah.sequencer.Sequencer;

@Log4j
/** Utility Swing class to select MIDI messages */
public class MidiForm extends JPanel implements MidiListener {

	public static final int MAX_CHANNELS = 16;
	public static final int MAX_DATA = 128;

	@RequiredArgsConstructor
	/** handled commands */
	public static enum MidiCommands {
		CONTROL_CHANGE(0xB0),  // 176
		PROGRAM_CHANGE(0xC0),  // 192
		NOTE_ON(0x90),  // 144
		NOTE_OFF(0x80) // 128
		;
		@Getter private final int val;
	}

	private static final int LABELS = 0;
	private static final int DATA = 1;

	private final JComboBox<MidiCommands> command;
	private final JComboBox<Integer> data1, channel;
	private final JComboBox<String> port;
	private final JTextField parsed;

	private final JToggleButton midiRecord;
	private final JButton midiPlay;
	private final JLabel comment;

	public MidiForm(Midi midi, String[] ports) {
	    command = new JComboBox<>(MidiCommands.values());
	    data1 = new JComboBox<>(generateCombo(MAX_DATA));
	    channel = new JComboBox<>(generateCombo(MAX_CHANNELS));
        port = new JComboBox<>(ports);

	    midiRecord = new JToggleButton("⚫ Rec");
        midiRecord.addActionListener( (event) -> midiLearn());
        midiPlay = new JButton("▶ Play");
        midiPlay.addActionListener( (event) -> midiPlay());
        comment = new JLabel();

        if (midi != null) {
            for(MidiCommands cmd : MidiCommands.values())
                if (cmd.getVal() == midi.getCommand())
                    command.setSelectedItem(cmd);
            channel.setSelectedItem(midi.getChannel());
            data1.setSelectedItem(midi.getData1());
            if (midi.getPort() != null)
                port.setSelectedItem(midi.getPort());
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel buttons = new JPanel();
        buttons.add(midiRecord);
        buttons.add(midiPlay);
        buttons.add(new JLabel(" parsed:"));
        parsed = new JTextField(24);
        buttons.add(parsed);
        add(buttons);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridy = LABELS;
        form.add(new JLabel("Port"), c);
        c.gridy = DATA;
        form.add(port, c);

        c.gridy = LABELS;
        form.add(new JLabel("Channel"), c);
        c.gridy = DATA;
        form.add(channel, c);

        c.gridx = 2;
        c.gridy = LABELS;
        c.gridwidth = 3;
        form.add(comment, c);
        c.gridwidth = 1;
        c.gridy = LABELS;
        form.add(new JLabel("Command"), c);
        c.gridy = DATA;
        form.add(command, c);

        c.gridx = 3;
        c.gridy = LABELS;
        form.add(new JLabel("Data1"), c);
        c.gridy = DATA;
        form.add(data1, c);
        add(form);

        command.addActionListener(l -> {parsed.setText(getMidi().toString());});
        data1.addActionListener(l -> {parsed.setText(getMidi().toString());});
        channel.addActionListener(l -> {parsed.setText(getMidi().toString());});
        port.addActionListener(l -> {parsed.setText(getMidi().toString());});
	}

	public MidiForm(Midi midi) {
	    this(midi, JudahMidi.getInstance().getSources());
	}

	private void midiLearn() {
		ArrayList<MidiListener> listeners = Sequencer.getCurrent().getListeners();
		if (midiRecord.isSelected())
			listeners.add(this);
		else
			listeners.remove(this);
	}

	private void midiPlay() {
		JudahMidi.queue(getMidi(), JudahMidi.getInstance().getKeyboardSynth());
	}

	private Integer[] generateCombo(int size) {
		Integer[] result = new Integer[size];
		for (int i = 0; i < size; i++) {
			result[i] = i;
		}
		return result;
	}

	public void hideMidiRecord() {
		midiRecord.setVisible(false);
	}

	public void hideMidiPlay() {
		midiPlay.setVisible(false);
	}

	private Midi getMidi() {
		Sequencer.getCurrent().getListeners().remove(this);
		int cmd = ((MidiCommands)command.getSelectedItem()).getVal();
		int chan = (int)channel.getSelectedItem();
		int dat1 = (int)data1.getSelectedItem();

		try {
		    if (port.getSelectedIndex() == 0)
		        return new Midi(cmd, chan, dat1);
		    return new Midi(cmd, chan, dat1, port.getSelectedItem().toString());
		} catch (InvalidMidiDataException e) {
			log.error(e.getMessage() + " " + Arrays.toString(new Object[] {cmd, chan, dat1}), e);
			Constants.infoBox(e.getMessage(), "MIDI Error");
			return null;
		}

	}

	public void setChannel(int i) {
		channel.setSelectedItem(i);
	}

	@Override
	public void feed(Midi midi) {
		for (MidiCommands cmd : MidiCommands.values())
			if (cmd.getVal() == midi.getCommand()) {
				command.setSelectedItem(cmd);
				channel.setSelectedItem(midi.getChannel());
				data1.setSelectedItem(midi.getData1());
				port.setSelectedItem(midi.getPort());
				parsed.setText(midi.toString());
			}
	}

	@Override
	public PassThrough getPassThroughMode() {
		return PassThrough.NOTES;
	}

    public Midi getParsed() {
        return Midi.deserialize(parsed.getText());
    }

}
