package net.judah.mixer;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.looper.Sample;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Knob;

public class LoopGui extends MixerGui {

	@Getter final Sample sample;
	@Getter final JToggleButton labelButton;
	final Knob volume;
	final JCheckBox onMute = new JCheckBox(); 
	final JCheckBox hasRecording = new JCheckBox(); 
	final JCheckBox compression = new JCheckBox();
	final JCheckBox reverb = new JCheckBox();
	
	public LoopGui(Sample loop) {
		this.sample = loop;
		setLayout(new BorderLayout(1, 1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		
		labelButton = new JToggleButton(loop.getName());
		labelButton.setFont(Constants.Gui.BOLD);
		labelButton.setPreferredSize(ChannelGui.lbl);
		labelButton.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
		labelButton.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "none");
		
		add(labelButton, BorderLayout.LINE_START);
		volume = new Knob(val -> {loop.setVolume(val);});
		add(volume, BorderLayout.CENTER);

		JPanel boxes = new JPanel();
		onMute.setToolTipText("Mute Audio");
		hasRecording.setToolTipText("Has Recording");
		hasRecording.setEnabled(false);
		compression.setToolTipText("Compression");
		reverb.setToolTipText("Reverb");
		boxes.add(onMute);
		boxes.add(hasRecording);
		boxes.add(compression);
		boxes.add(reverb);
		add(boxes, BorderLayout.LINE_END);
		
		labelButton.addActionListener(listener -> {
			if (labelButton.isSelected())
				MainFrame.get().getRight().setFocus(loop);
		});
		
		compression.addActionListener(listener -> {
			Compression comp = loop.getCompression();
			comp.setActive(compression.isSelected());
			EffectsGui.compression(loop);
			Console.info(loop.getName() + " Compression: " + (comp.isActive() ? "On " : "Off ") + comp.getPreset());
		});
		reverb.addActionListener(listener -> {
			Reverb rev = loop.getReverb();
			rev.setActive(reverb.isSelected());
			EffectsGui.reverb(loop);
			Console.info(loop.getName() + " Reverb: " + (rev.isActive() ? " On" : " Off"));
		});

		update();
	}
	
	@Override
	public void setVolume(int vol) {
		volume.setValue(vol);
	}

	@Override
	public void update() {
		onMute.setSelected(sample.isOnMute());
		hasRecording.setSelected(sample.hasRecording());
		compression.setSelected(sample.getCompression().isActive());
		reverb.setSelected(sample.getReverb().isActive());
		volume.setValue(
				Math.round(sample.getGain() * 100 / sample.getGainFactor()));
	}


	
}
