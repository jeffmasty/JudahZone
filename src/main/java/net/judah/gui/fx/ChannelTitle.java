package net.judah.gui.fx;

import java.awt.Dimension;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.widgets.Btn;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.omni.Icons;

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
		mute = (channel instanceof LineIn) ? new Btn("tape", e->((LineIn)channel).setMuteRecord(!((LineIn)channel).isMuteRecord())) :
			new Btn("mute", e->channel.setOnMute(!channel.isOnMute()));
		lfo = new Btn("lfo", e->{
			if (MainFrame.getKnobMode() == KnobMode.LFO)
				channel.getLfo().setActive(!channel.getLfo().isActive());
			else MainFrame.setFocus(KnobMode.LFO);});
		mute.setFont(Gui.FONT10);
		lfo.setFont(Gui.FONT10);

		add(Box.createHorizontalGlue());
		add(Gui.resize(name, new Dimension(Size.WIDTH_KNOBS - 3 * Size.TINY.width, Size.TINY.height)));
		add(Box.createHorizontalGlue());

		add(Gui.resize(mute, Size.TINY));
		add(Gui.resize(lfo, Size.TINY));
		add(new Btn(Icons.DETAILS_VEW, e->MainFrame.setFocus(KnobMode.Tools)));
		update();
	}

	private String standard() {
		return channel instanceof Loop ? "" : channel.getName();
	}

	public void name(List<Channel> selected) {
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
		repaint();
	}

	public void update() {
		if (channel instanceof LineIn)
			mute.setBackground(((LineIn)channel).isMuteRecord() ? null : Pastels.ONTAPE);
		else mute.setBackground(channel.isOnMute() ? Pastels.PURPLE : null);
		lfo.setBackground(channel.getLfo().isActive() ? Pastels.BLUE : null);
	}
}
