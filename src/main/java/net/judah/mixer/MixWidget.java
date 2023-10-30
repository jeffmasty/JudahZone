package net.judah.mixer;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import lombok.Getter;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.widgets.RainbowFader;
import net.judah.gui.widgets.TogglePreset;
import net.judah.looper.Looper;

/**Mixer view
 * <pre>
  each Channel has:
		Menu
		Volume Fader 
		LEDs  {fx status}
		Channel Icon </pre>
 * @author judah */
public abstract class MixWidget extends JPanel implements Pastels {
	
	@Getter protected final Channel channel;
	@Getter protected final JPanel banner = new JPanel();
	
	private static final Dimension BTNS = new Dimension(50, 63);

	@Getter protected final JButton mute = new JButton("mute");
	@Getter protected final TogglePreset fx;
	@Getter protected final JToggleButton sync = new JToggleButton("sync");

	protected RainbowFader volume;
	protected final JLabel title = new JLabel("", CENTER);
	protected JPanel sidecar = new JPanel(new GridLayout(3, 1, 0, 0));
	private final LEDs indicators;
	
	public MixWidget(Channel channel, Looper looper) {
		this.channel = channel;
		
		fx = new TogglePreset(channel, looper);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BUTTONS);
		
		indicators = new LEDs(channel);
		volume = new RainbowFader(vol -> {
			channel.getGain().set(Gain.VOLUME, volume.getValue());
			MainFrame.update(channel);
		});
		volume.setOpaque(true);
		banner.setLayout(new BoxLayout(banner, BoxLayout.LINE_AXIS));
		banner.setOpaque(true);
		title.setFont(Gui.BOLD13);
		banner.add(title);
		banner.add(sidecar);
		sidecar.setOpaque(false);
		Gui.resize(sidecar, BTNS);
		Gui.resize(title, BTNS);
		
		add(banner);
		add(indicators);
		add(Box.createVerticalStrut(1));
		add(volume);
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
		if (channel.getVolume() != volume.getValue()) 
			volume.setValue(channel.getVolume());
		if (channel.isOnMute())
			volume.setBackground(PURPLE);
		else 
			volume.setBackground(null);
	}
	
	
}
