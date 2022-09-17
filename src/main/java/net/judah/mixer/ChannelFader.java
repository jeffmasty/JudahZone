package net.judah.mixer;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.looper.Loop;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RainbowFader;

/**Mixer view
 * <pre>
  each Channel has:
		Menu
		Volume Fader 
		LEDs  {fx status}
		Channel Icon </pre>
 * @author judah */
public class ChannelFader extends JPanel implements Pastels {

	@Getter private final Channel channel;
	private final Loop loop;
	private final Instrument in;
	JudahMidi midi = JudahMidi.getInstance();

	@Getter private final JToggleButton mute = new JToggleButton("mute");
	@Getter private final JToggleButton fx = new JToggleButton("fx");
	@Getter private final JToggleButton sync = new JToggleButton("sync");
	@Getter private final JPanel banner = new JPanel();
	
	private RainbowFader volume;
	private final JLabel title = new JLabel("", CENTER);
	private final JPanel sidecar = new JPanel(new GridLayout(3, 1));
	private final LEDs indicators;
	
	public ChannelFader(Channel channel) {
		this.channel = channel;
		loop = (channel instanceof Loop) ? (Loop)channel : null;
		in = (channel instanceof Instrument) ? (Instrument)channel : null;
		setBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BUTTONS);
		
		indicators = new LEDs(channel);
		volume = new RainbowFader(vol -> {
			channel.getGain().setVol(volume.getValue());
			MainFrame.update(channel);
		});
		volume.setOpaque(true);
		doBanner();

		add(banner);
		add(indicators);
		add(volume);
	}

	private void doBanner() {
		banner.setLayout(new BoxLayout(banner, BoxLayout.LINE_AXIS));
		banner.setOpaque(true);
		
		title.setFont(Constants.Gui.BOLD);
		if (channel == JudahZone.getLooper().getLoopA()) {
			title.setFont(Constants.Gui.BOLD13);
			title.setText("-" + JudahClock.getLength() + "-");
		}
		else if (channel.getIcon() == null) 
            title.setText(channel.getName());
		else 
            title.setIcon(channel.getIcon());

		if (in != null) {
			mute.setText("rec");
			mute.setSelected(!in.isOnMute());
			
			if (JudahMidi.getInstance().getPath(in) == null) 
				sync.setEnabled(false);
		}
		
		sidecar.add(font(mute));
		sidecar.add(font(fx));
		sidecar.add(font(sync));
		sidecar.setOpaque(false);
		
		banner.add(loop != null ? loop.getSync() : title);
		banner.add(sidecar);
		
		title.addMouseListener(new MouseAdapter() {
			// TODO right/double click menu
			@Override public void mouseClicked(MouseEvent e) {
				MainFrame.setFocus(channel);}});
		
		mute.addActionListener(e -> mute());
	    fx.addActionListener(e -> fx()); 
	    sync.addActionListener(e -> sync());
		
	}
	
	private Component font(Component c) {
		c.setFont(Constants.Gui.FONT9);
		return c;
	}

	
	public void update() {
		indicators.sync();		
		background();
		updateVolume();
		if (loop != null) {
			loop.getSync().update();
			if (loop.isArmed() != sync.isSelected())
				sync.setSelected(loop.isArmed());
		}
		else if (in != null) {
			JudahMidi sys = JudahMidi.getInstance();
			sys.getPath(in);
			if (sys.getPath(in) != null) {
				boolean active = sys.getSync().contains(sys.getPath(in).getPort());
				if (active != sync.isSelected())
					sync.setSelected(active);
			}
		}
		
	}
	
	public void updateVolume() {
		if (channel.getVolume() != volume.getValue()) {
			volume.setValue(channel.getVolume());
			volume.repaint();
		}
		if (channel.isOnMute())
			volume.setBackground(PURPLE);
		else if (channel instanceof Instrument && ((Instrument)channel).isMuteRecord())
			volume.setBackground(BLUE);
		else {
			volume.setBackground(null);
		}
	}
	
	public void background() {
		Color bg = EGGSHELL;
		// fx.setBackground();
		
		if (loop != null) {
			mute.setBackground(loop.isOnMute() ? PURPLE : null);
			bg = BLUE;
			if (loop.isRecording() == AudioMode.RUNNING) 
				bg = RED;
			else if (JudahClock.getInstance().getSynchronized() == loop.getSync())
				bg = YELLOW;
			else if (loop.hasRecording() && loop.isPlaying() == AudioMode.STOPPED)
				bg = Color.DARK_GRAY;
			else if (loop.isPlaying() == AudioMode.RUNNING && loop.isActive())
				bg = GREEN;
			else if (loop.isArmed()) 
				bg = PINK;
			else {
				Loop a = JudahZone.getLooper().getLoopA();
				if (a == channel && !JudahClock.isLoopSync())
					bg = YELLOW;
				else if (a != channel && loop.getPrimary() != null) {
					bg = PINK;
				}
			}
		}
		else if (channel.isOnMute())  // line in/master track 
			bg = Color.BLACK;
		else if (channel.getGain().getVol() < 5)
			bg = Color.DARK_GRAY;
		else if (in != null) {
			JackPort port = in.getSync();
			mute.setBackground(in.isMuteRecord() ? null : GREEN);
			if (in.isSolo())
				bg = YELLOW;
			else if (port != null && JudahMidi.getInstance().getSync().contains(port))
				bg = PINK;
		}
		else bg = Color.WHITE; // i.e. MAINS
		
		if (false == banner.getBackground().equals(bg)) {
			banner.setBackground(bg);
		}
	}

	private void mute() {
		
		if (in != null) {
			boolean target = !in.isMuteRecord();
			boolean selected = mute.isSelected();
			if (target != selected) {
				in.setMuteRecord(selected);
			}
			mute.setBackground( selected ? GREEN : null);
		}
		else {
			boolean target = channel.isOnMute();
			boolean selected = mute.isSelected();
			if (target != selected)
				channel.setOnMute(selected);
            mute.setBackground(selected ? PURPLE : null);
		}
	}
	
	private void sync() {
		if (in != null && midi.getPath(in) != null) {
			if (sync.isSelected()) 
				midi.getSync().add(midi.getPath(in).getPort());
			else 
				midi.getSync().remove(midi.getPath(in).getPort());
			background();
		}
		else if (loop != null) {
			// sync channel
			if (loop == JudahZone.getLooper().getDrumTrack()) 
				JudahClock.getInstance().listen(JudahZone.getLooper().getLoopA());
			else { 
				loop.setArmed(sync.isSelected());
				background();
			}
		}
	}
	
	private void fx() { 
		// TODO
		fx.setBackground(fx.isSelected() ? BLUE : null);
	}

	
}
