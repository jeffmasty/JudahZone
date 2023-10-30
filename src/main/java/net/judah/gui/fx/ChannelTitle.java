package net.judah.gui.fx;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.drumkit.Sample;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;

public class ChannelTitle extends JPanel {

	protected final JButton mute;
	protected final JButton lfo;
	private final Channel channel;
	private final JLabel name;
	
	public ChannelTitle(Channel ch, Looper looper) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setOpaque(true);
		this.channel = ch;
		name = new JLabel(standard(), JLabel.CENTER);
		name.setFont(Gui.BOLD13);
		mute = new JButton("");
		lfo = new JButton("lfo");
		
		add(Box.createHorizontalGlue());
		add(Gui.resize(name, new Dimension(Size.WIDTH_KNOBS - 3 * Size.TINY.width, Size.TINY.height)));
		add(Box.createHorizontalGlue());
		add(Gui.resize(Gui.font(mute, Gui.FONT10), Size.TINY));
		add(Gui.resize(Gui.font(lfo, Gui.FONT10), Size.TINY));

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
		if (channel instanceof LineIn) 
			mute.setBackground(((LineIn)channel).isMuteRecord() ? null : Pastels.ONTAPE);
		else mute.setBackground(channel.isOnMute() ? Pastels.PURPLE : null);
		lfo.setBackground(channel.getLfo().isActive() ? Pastels.BLUE : null);
	}	
}
