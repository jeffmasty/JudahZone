package net.judah.gui.fx;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.drumkit.DrumKit;
import net.judah.drumkit.Sample;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.knobs.KnobMode;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;

public class ChannelTitle extends JPanel {

	protected final JToggleButton mute = new Toggle("");
	protected final JToggleButton fx = new Toggle("fx");
	protected final JToggleButton lfo = new Toggle("lfo");

	class Toggle extends JToggleButton {
		Toggle(String s) {
			super(s);
			setFont(Gui.FONT10);
		}
	}
	
	private final Channel channel;
	private final JLabel name;
	
	public ChannelTitle(Channel channel) {
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.LINE_AXIS));
		
		this.channel = channel;
		name = new JLabel(standard(), JLabel.CENTER);
		name.setFont(Gui.BOLD13);
		main.add(Box.createHorizontalGlue());
		main.add(name);
		main.add(Box.createHorizontalGlue());
		
		Component[] btns;
		if (channel instanceof DrumKit || channel instanceof Sample) 
			btns = new Component[] {lfo, fx};
		else btns = new Component[]{lfo, fx, mute};
		main.add(Gui.wrap(btns));
		
		update();
		
		if (channel instanceof LineIn) {
			mute.setText("tape");
			mute.addActionListener(e-> ((LineIn)channel).setMuteRecord(mute.isSelected()));
		}
		else {
			mute.setText("mute");
			mute.addActionListener(e->channel.setOnMute(mute.isSelected()));
		}
		fx.addActionListener(e-> channel.setPresetActive(!channel.isPresetActive()));
		lfo.addActionListener(e-> {
			if (!lfo.isSelected()) return;
			MainFrame.setFocus(KnobMode.LFO);
			lfo.setSelected(false);
		});
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(main);
	}

	private String standard() {
		return channel instanceof Sample ? "wav " + channel.getName() : channel instanceof Loop ? "" : channel.getName();
	}
	
	public void name(MultiSelect selected) {
		if (selected.size() == 1) {
			if (channel.getIcon() != null) 
				name.setIcon(channel.getIcon());
			name.setText(standard());
			return;
		}
		name.setIcon(null);
		StringBuffer buf = null;
		for (Channel c : selected) {
			if (buf == null) 
				buf = new StringBuffer(c.getName());
			else buf.append(" ").append(c.getName());
		}
		name.setText(buf.toString());
	}

	public void update() {
		if (fx.isSelected() != channel.isPresetActive())
			fx.setSelected(!fx.isSelected());
		fx.setBackground(fx.isSelected() ? Pastels.YELLOW: null);
		if (channel instanceof LineIn) {
			if (mute.isSelected() != ((LineIn)channel).isMuteRecord())
				mute.setSelected(!mute.isSelected());
			mute.setBackground(((LineIn)channel).isMuteRecord()? null : Pastels.BLUE);
		}
		else if (mute.isSelected() != channel.isOnMute()) {
			mute.setSelected(!mute.isSelected());
			mute.setBackground(mute.isSelected() ? null : Pastels.PURPLE);
		}
		lfo.setBackground(channel.getLfo().isActive() ? Pastels.BLUE : null);
	}	
}
