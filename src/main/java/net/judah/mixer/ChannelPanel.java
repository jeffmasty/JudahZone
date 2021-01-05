package net.judah.mixer;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Knob;


public class ChannelPanel extends JPanel {

	private Channel channel;
	private Reverb reverb;
	private Compression compression;
	
	private final JPanel revEffects;
	private final JPanel compEffects;
	private final JLabel name = new JLabel();
	private final JPanel header = new JPanel();
	private final Knob volume;
	
	private JToggleButton revActive;
	private Knob revRoom, revDamp, revWidth;
	private JToggleButton compActive;
	
	private Knob compAtt, compRel;
	private Knob compRatio, compThresh;
	
	public ChannelPanel() {
		// TODO mute/record checkboxes panel
		// TODO sync volume to listen to channel volume
		volume = new Knob(val -> {channel.setVolume(val);});
		revEffects = reverbPanel();
		compEffects = compressionPanel();

		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		name.setFont(Constants.Gui.BOLD);
		name.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		header.add(name);
		header.add(volume);
		add(header);

		JPanel btns = new JPanel();
		btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
		btns.add(revActive);
		btns.add(compActive);
		add(btns);
		
		JPanel effects = new JPanel();
		effects.setLayout(new BoxLayout(effects, BoxLayout.Y_AXIS));
		effects.add(revEffects);
		effects.add(compEffects);
		add(effects);

	}
	
	public void setChannel(Channel ch) {
		if (ch == channel) return;
		channel = ch;
		reverb = channel.getReverb();
		compression = channel.getCompression();
		update();
		Console.info("channel " + channel.getName());
	}
	
	private JPanel reverbPanel() {
		revActive = new JToggleButton("Reverb");
		revActive.addActionListener(listener -> { 
			reverb.setActive(!reverb.isActive());
			update();
			channel.getGui().update();
			Console.info(channel.getName() + " Reverb: " + (reverb.isActive() ? " On" : " Off"));
		});
		
		JPanel result = new JPanel();
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				reverb.initialize(Constants._SAMPLERATE, Constants._BUFSIZE);
				update();
				Console.info("reverb init()");
		}});
		revRoom = new Knob(val -> {reverb.setRoomSize(val / 100f);});
		revDamp = new Knob(val -> {reverb.setDamp(val / 100f);});
		revWidth = new Knob(val -> {reverb.setWidth(val / 100f);});
		result.add(knobPanel("room", revRoom));
		result.add(knobPanel("damp", revDamp));
		result.add(knobPanel("width", revWidth));
		return result;
	}
	
	private JPanel compressionPanel() {
		
		compActive = new JToggleButton("Compression");
		compActive.addActionListener(listener -> { 
			compression.setActive(!compression.isActive());
			update();
			channel.getGui().update();
			Console.info(channel.getName() + " Compression: " + (compression.isActive() ? " On" : " Off"));
		});
		
		JPanel result = new JPanel();
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				compression.setPreset(compression.getPreset());
				update();
				Console.info("compression init()");
		}});
		result.setLayout(new GridLayout(2, 4));
		
		compRatio = new Knob(val -> {compression.setRatio(Math.round( (val + 20) / 10));});
		compRatio.setToolTipText("2 to 12");
		compThresh = new Knob(val -> {compression.setThreshold((int)((val - 100) / 2.5));});
		compThresh.setToolTipText("-40 to 0");
		compAtt = new Knob(val -> {compression.setAttack((int)Math.round(val * 1.5));});
		compAtt.setToolTipText("0 to 150 milliseconds");
		compRel = new Knob(val -> {compression.setRelease(Math.round(val * 3));});
		compRel.setToolTipText("0 to 300 milliseconds");
		
		result.add(new JLabel("ratio"));
		result.add(compRatio);
		result.add(new JLabel("thresh"));
		result.add(compThresh);
		result.add(new JLabel("attack"));
		result.add(compAtt);
		result.add(new JLabel("release"));
		result.add(compRel);
		
		return result;
	}
	
	private JPanel knobPanel(String name, Knob knob) {
		JPanel pnl = new JPanel();
		JLabel lbl = new JLabel(name);
		pnl.add(lbl);
		pnl.add(knob);
		return pnl;
	}
	
	public void incrementCompression() {
		Compression comp = channel.getCompression();
		//boolean active = channel.getCompression().isActive();
		if (!comp.isActive()) {
			channel.getCompression().setPreset(0);
			channel.getCompression().setActive(true);
		}
		else {
			int preset = 1 + channel.getCompression().getPreset();
			if (preset < Compression.COMPRESSION_PRESETS)
				channel.getCompression().setPreset(preset);
			else
				channel.getCompression().setActive(false);
		}
		
		Console.info(channel.getName() + " Compression on: " + comp.isActive() + " / " + comp.getPreset());
	}
	
	public void update() {

		name.setText(channel.getName());

		revActive.setSelected(reverb.isActive());
		revRoom.setValue(Math.round(reverb.getRoomSize() * 100)); 
		revDamp.setValue(Math.round(reverb.getDamp() * 100)); 
		revWidth.setValue(Math.round(reverb.getWidth() * 100));

		compActive.setSelected(compression.isActive());
		compRatio.setValue(compression.getRatio() * 10);
		compThresh.setValue((int) ((compression.getThreshold() + 40) * 2.5));
		int attack = (int)Math.round(compression.getAttack() / 0.75);
		if (attack > 100) attack = 100;
		compAtt.setValue(attack);
		int release = (int)Math.round(compression.getRelease() * 0.333);
		if (release > 100) release = 100;
		compRel.setValue(release);
		
	}
	
}
