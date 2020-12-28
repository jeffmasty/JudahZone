package net.judah.mixer;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.judah.looper.Sample;
import net.judah.util.Constants;
import net.judah.util.Knob;
import net.judah.util.Knob.KnobListener;

public class LoopGui extends JPanel implements KnobListener {

	final Sample loop;
	final JButton label;
	final Knob knob = new Knob(this);

	public LoopGui(Sample loop) {
		this.loop = loop;
		setLayout(new BorderLayout(1, 1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		
		label = new JButton(loop.getName());
		label.setFont(Constants.Gui.BOLD);
		label.setPreferredSize(ChannelGui.lbl);
		
		add(label, BorderLayout.LINE_START);
		add(knob, BorderLayout.CENTER);
	}
	
	public void setVolume(int volume) {
		knob.getHandle(0).setValue(volume);
	}

	@Override
	public void knobChanged(int val) {
		loop.setVolume(val);
	}

}
