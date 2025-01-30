package net.judah.mixer;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.widgets.RainbowFader;
import net.judah.gui.widgets.TogglePreset;
import net.judah.looper.Looper;

/**Mixer view <br/>
 * Each mixer channel has:
<pre>		
Icon/Btns
FX LEDs
Vol Fader/Gain</pre>
 * @author judah */
public abstract class MixWidget extends JPanel implements Pastels {
	private static final Dimension SIDECAR = new Dimension(50, 60);

	protected final Channel channel;
	protected final JLabel title = new JLabel("", CENTER);
	protected final JButton mute = new JButton("mute");
	protected final TogglePreset fx;
	protected final JToggleButton sync = new JToggleButton("sync");
	protected final JPanel banner = new JPanel();
	protected final JPanel sidecar = new JPanel(new GridLayout(3, 1, 0, 0));
	protected final FxLEDs indicators;
	protected final RMSIndicator gain;
	protected RainbowFader fader;
	
	public MixWidget(Channel channel, Looper looper) {
		this.channel = channel;
		
		fx = new TogglePreset(channel, looper);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BUTTONS);
		
		indicators = new FxLEDs(channel);
		fader = new RainbowFader(vol -> {
			channel.getGain().set(Gain.VOLUME, fader.getValue());
			MainFrame.update(channel);
		});
		gain = new RMSIndicator(channel);
		banner.setLayout(new BoxLayout(banner, BoxLayout.LINE_AXIS));
		banner.setOpaque(true);
		title.setFont(Gui.BOLD13);
		banner.add(title);
		banner.add(sidecar);
		sidecar.setOpaque(false);
		Gui.resize(sidecar, SIDECAR);
		Gui.resize(title, SIDECAR);
		
		add(banner);
		add(indicators);
		add(Box.createVerticalStrut(1));
		
		add(Gui.wrap(gain, fader));
		
		add(Box.createVerticalStrut(1));
		
		addMouseListener(new MouseAdapter() {
		// TODO right/double click menu
		@Override public void mouseClicked(MouseEvent e) {
			MainFrame.setFocus(channel);}});
	}
	
	protected abstract Color thisUpdate(); 
	
	protected boolean quiet() {
		return channel.getGain().getGain() < 0.05f;
	}
	
	public final void update() {
		Color bg = thisUpdate();
		if (false == banner.getBackground().equals(bg)) 
			banner.setBackground(bg);
		indicators.sync();		
		updateVolume();
		fx.update();
	}
	
	public void updateVolume() {
		if (channel.getVolume() != fader.getValue()) 
			fader.setValue(channel.getVolume());
		if (channel.isOnMute())
			fader.setBackground(PURPLE);
		else 
			fader.setBackground(null);
	}
	
}
