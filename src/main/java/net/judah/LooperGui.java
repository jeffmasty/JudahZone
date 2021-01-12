package net.judah;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JPanel;

import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelGui;
import net.judah.util.Console;

public class LooperGui extends JPanel {

	public LooperGui(Looper looper) {
		setLayout(new GridLayout(0, 2));
		
		add(JudahZone.getMasterTrack().getGui());
		add(looper.getDrumTrack().getGui());
		
		for (Sample loop : JudahZone.getLooper()) 
			add(loop.getGui());
		looper.registerListener(this);
	}

	public void addSample(Sample s) {
		Console.info("Add Sample gui " + s.getName());
		add(s.getGui());
		doLayout();
		for (Component c : getComponents()) 
			c.doLayout();
		MixerPane.getInstance().doLayout();
	}

	public void removeSample(Sample s) {
		remove(s.getGui());
		doLayout();
	}

	public void setSelected(Channel bus) {
		for (Component c : getComponents()) 
			if (c instanceof ChannelGui.Output) 
				((ChannelGui.Output)c).getLabelButton().setSelected(bus == ((ChannelGui.Output)c).getChannel());
	}

}

