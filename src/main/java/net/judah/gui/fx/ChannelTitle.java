package net.judah.gui.fx;

import javax.swing.*;

import net.judah.drumkit.DrumKit;
import net.judah.drumkit.Sample;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.widgets.TogglePreset;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;

public class ChannelTitle extends JPanel {

	protected final JButton mute = new JButton("");
	protected final TogglePreset fx;
	protected final JButton lfo = new JButton("lfo");
	private final Channel channel;
	private final JLabel name;
	
	public ChannelTitle(Channel ch, Looper looper) {
		fx = new TogglePreset(ch, looper);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setOpaque(true);
		this.channel = ch;
		name = new JLabel(standard(), JLabel.CENTER);
		name.setFont(Gui.BOLD13);
		add(Box.createHorizontalGlue());
		add(name);
		add(Box.createHorizontalGlue());
		
		JComponent[] btns;
		if (channel instanceof DrumKit || channel instanceof Sample) 
			btns = new JComponent[] {fx, lfo};
		else btns = new JComponent[]{fx, lfo, mute};
		
		for(JComponent c : btns)
			add(Gui.resize(Gui.font(c, Gui.FONT10), Size.TINY));
		
		if (channel instanceof LineIn) {
			mute.setText("tape");
			mute.addActionListener(e-> ((LineIn)channel).setMuteRecord(! ((LineIn)channel).isMuteRecord()));
		}
		else {
			mute.setText("mute");
			mute.addActionListener(e->channel.setOnMute(!channel.isOnMute()));
		}
		lfo.addActionListener(e-> {
			if (MainFrame.getKnobMode() == KnobMode.LFO) 
				channel.getLfo().setActive(!channel.getLfo().isActive());
			else MainFrame.setFocus(KnobMode.LFO);
		});
		update();
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
		fx.update();
		if (channel instanceof LineIn) 
			mute.setBackground(((LineIn)channel).isMuteRecord() ? null : Pastels.ONTAPE);
		else mute.setBackground(channel.isOnMute() ? Pastels.PURPLE : null);
		lfo.setBackground(channel.getLfo().isActive() ? Pastels.BLUE : null);
	}	
}
