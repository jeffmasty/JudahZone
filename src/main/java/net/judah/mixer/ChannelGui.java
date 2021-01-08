package net.judah.mixer;

import static net.judah.util.Constants.Gui.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Knob;

public class ChannelGui extends MixerGui {
	final static Dimension lbl = new Dimension(80, STD_HEIGHT);
	final static Dimension btn = new Dimension(18, STD_HEIGHT);

	@Getter final Channel channel;
	@Getter final JToggleButton labelButton;
	final Knob volume;
	final JCheckBox onMute = new JCheckBox(); 
	final JCheckBox muteRecord = new JCheckBox(); 
	final JCheckBox compression = new JCheckBox();
	final JCheckBox reverb = new JCheckBox();
	
	public ChannelGui(Channel channel) {
		this.channel = channel;
		
		setLayout(new BorderLayout(1, 1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		
		labelButton = new JToggleButton(channel.getName());
		labelButton.setPreferredSize(lbl);
		labelButton.setFont(BOLD);
		labelButton.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
		labelButton.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "none");

		volume = new Knob(val -> {channel.setVolume(val);});
		
		add(labelButton, BorderLayout.LINE_START);
		add(volume, BorderLayout.CENTER);
		JPanel boxes = new JPanel();
		onMute.setToolTipText("Mute Audio");
		muteRecord.setToolTipText("Mute Recording");
		compression.setToolTipText("Compression");
		reverb.setToolTipText("Reverb");
		boxes.add(onMute);
		boxes.add(muteRecord);
		boxes.add(compression);
		boxes.add(reverb);

		add(boxes, BorderLayout.LINE_END);

		labelButton.addActionListener(listener -> {
			if (labelButton.isSelected())
				MainFrame.get().getRight().setFocus(channel);
		});
		
		onMute.addActionListener(e -> {channel.setOnMute(onMute.isSelected());});
		muteRecord.addActionListener(e -> {channel.setMuteRecord(muteRecord.isSelected());});
		compression.addActionListener(listener -> {
			Compression comp = channel.getCompression();
			comp.setActive(compression.isSelected());
			EffectsGui.compression(channel);
			Console.info(channel.getName() + " Compression: " + (comp.isActive() ? "On " : "Off ") + comp.getPreset());
		});
		reverb.addActionListener(listener -> {
			Reverb rev = channel.getReverb();
			rev.setActive(reverb.isSelected());
			EffectsGui.reverb(channel);
			Console.info(channel.getName() + " Reverb: " + (rev.isActive() ? " On" : " Off"));
		});
		
		update();
		
		reverb.addKeyListener(null);
		Constants.Gui.attachKeyListener(this, MainFrame.get().getMenu());
	}
	
	@Override
	public void update() {
		onMute.setSelected(channel.isOnMute());
		muteRecord.setSelected(channel.isMuteRecord());
		compression.setSelected(channel.getCompression().isActive());
		reverb.setSelected(channel.getReverb().isActive());
		volume.setValue(
				Math.round(channel.getGain() * 100 / channel.getGainFactor()));
	}

	@Override
	public void setVolume(int vol) {
		volume.setValue(vol);
	}

}
