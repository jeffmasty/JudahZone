package net.judah.mixer;

import static net.judah.util.Constants.Gui.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.judah.MainFrame;
import net.judah.util.Console;
import net.judah.util.Knob;

public class ChannelGui extends JPanel {
	final static Dimension lbl = new Dimension(80, STD_HEIGHT);
	final static Dimension btn = new Dimension(18, STD_HEIGHT);

	final Channel channel;
	final JButton label;
	final Knob volume;
	final JCheckBox onMute = new JCheckBox(); 
	final JCheckBox muteRecord = new JCheckBox(); 
	final JCheckBox compression = new JCheckBox();
	final JCheckBox reverb = new JCheckBox();
	
	public ChannelGui(Channel channel) {
		this.channel = channel;
		
		setLayout(new BorderLayout(1, 1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		
		label = new JButton(channel.getName());
		label.setPreferredSize(lbl);
		label.setFont(BOLD);
		volume = new Knob(val -> {channel.setVolume(val);});
		
		add(label, BorderLayout.LINE_START);
		add(volume, BorderLayout.CENTER);
		JPanel boxes = new JPanel();
		onMute.setToolTipText("Mute Audio");
		muteRecord.setToolTipText("Mute Recording");
		reverb.setToolTipText("Reverb");
		compression.setToolTipText("Compression");
		boxes.add(onMute);
		boxes.add(muteRecord);
		boxes.add(reverb);
		boxes.add(compression);
		add(boxes, BorderLayout.LINE_END);

		label.addActionListener(listener -> {
			MainFrame.get().getRight().setFocus(channel);
		});
		onMute.addActionListener(e -> {channel.setOnMute(onMute.isSelected());});
		muteRecord.addActionListener(e -> {channel.setMuteRecord(muteRecord.isSelected());});
		reverb.addActionListener(listener -> {
			Reverb rev = channel.getReverb();
			rev.setActive(reverb.isSelected());
			Console.info(channel.getName() + " Reverb: " + (rev.isActive() ? " On" : " Off"));
		});
		compression.addActionListener(listener -> {
			Compression comp = channel.getCompression();
			comp.setActive(compression.isSelected());
			Console.info(channel.getName() + " Compression: " + (comp.isActive() ? "On " : "Off ") + comp.getPreset());
		});
		
		update();
		
	}
	
	public void update() {
		onMute.setSelected(channel.isOnMute());
		muteRecord.setSelected(channel.isMuteRecord());
		compression.setSelected(channel.getCompression().isActive());
		reverb.setSelected(channel.getReverb().isActive());
		volume.setValue(
				Math.round(channel.getGain() * 100 / channel.getGainFactor()));
	}

	public void setVolume(int vol) {
		volume.setValue(vol);
	}

}
