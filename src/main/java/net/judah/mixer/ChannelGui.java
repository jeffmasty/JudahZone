package net.judah.mixer;

import static net.judah.util.Constants.Gui.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.judah.util.Knob;
import net.judah.util.Knob.KnobListener;

public class ChannelGui extends JPanel implements KnobListener {
	final static Dimension lbl = new Dimension(80, STD_HEIGHT);
	final static Dimension btn = new Dimension(18, STD_HEIGHT);

	final Channel channel;
	final JButton label;
	final Knob knob = new Knob(this);
	final JCheckBox onMute = new JCheckBox(); 
	final JCheckBox muteRecord = new JCheckBox(); 
	
	public ChannelGui(Channel channel) {
		this.channel = channel;
		
		setLayout(new BorderLayout(1, 1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		
		label = new JButton("  " + channel.getName());
		label.setPreferredSize(lbl);
		label.setFont(BOLD);
		
		add(label, BorderLayout.LINE_START);
		add(knob, BorderLayout.CENTER);
		JPanel boxes = new JPanel();
		onMute.addActionListener(e -> {channel.setOnMute(onMute.isSelected());});
		onMute.setToolTipText("Mute Audio");
		muteRecord.addActionListener(e -> {channel.setMuteRecord(muteRecord.isSelected());});
		muteRecord.setToolTipText("Mute Recording");
		boxes.add(onMute);
		boxes.add(muteRecord);
		add(boxes, BorderLayout.LINE_END);
		update();
	}
	
	public void update() {
		onMute.setSelected(channel.isOnMute());
		muteRecord.setSelected(channel.isMuteRecord());
		knob.getHandle(0).setValue(
				Math.round(channel.getGain() * 100 / channel.getGainFactor()));
	}

	public void setVolume(int volume) {
		knob.getHandle(0).setValue(volume);
	}

	@Override
	public void knobChanged(int val) {
		channel.setVolume(val);
	}
	
}
