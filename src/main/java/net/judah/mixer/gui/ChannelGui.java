package net.judah.mixer.gui;

import static net.judah.Constants.Gui.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.judah.mixer.Channel;
import net.judah.mixer.instrument.InstType;

public class ChannelGui extends JPanel implements  ChangeListener {
	final static Dimension lbl = new Dimension(75, STD_HEIGHT);
	final static Dimension btn = new Dimension(18, STD_HEIGHT);

	final Channel channel;
	final JSlider slider;
	final JLabel label;
	final JToggleButton mute;
	final JToggleButton loop;
	
	public ChannelGui(Channel channel) {
		this.channel = channel;
		final boolean isLooper = channel.getInstrument().getType() == InstType.Looper;
		
		
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		setLayout(new BorderLayout(1, 1));
		
		label = new JLabel("  " + channel.getName());
		label.setFont(BOLD);
		label.setPreferredSize(lbl);
		
		add(label, BorderLayout.LINE_START);
		
		slider = new JSlider(0, 100);
		slider.addChangeListener(this);
		add(slider, BorderLayout.CENTER);

		JPanel buttons = new JPanel();

		mute = new JToggleButton(isLooper ? "P" : "M");
		mute.setPreferredSize(btn);
		mute.setMargin(BTN_MARGIN);
		mute.setFont(FONT9);
		buttons.add(mute);
		
		
		loop = new JToggleButton(isLooper ? "R" : "L");
		loop.setPreferredSize(btn);
		loop.setMargin(BTN_MARGIN);
		loop.setFont(FONT9);
		buttons.add(loop);
		add(buttons, BorderLayout.LINE_END);
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		channel.setGain(slider.getValue() / 100f);
	}

	public void update() {
		slider.setValue(Math.round(channel.getGain() * 100));
	}
	
}
