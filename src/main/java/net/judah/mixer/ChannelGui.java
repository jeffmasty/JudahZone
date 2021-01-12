package net.judah.mixer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.jack.AudioMode;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Icons;
import net.judah.util.Knob;
import net.judah.util.MenuBar;

public abstract class ChannelGui extends JPanel {
	final static Dimension lbl = new Dimension(65, 38);
	public static final Font BOLD = new Font("Arial", Font.BOLD, 11);
	
	@Getter protected final Channel channel;
	
	@Getter protected final JToggleButton labelButton;
	protected final Knob volume;
	protected final MixButton onMute;
	protected final List<MixButton> customActions;

	public ChannelGui(Channel channel) {
		this.channel = channel;
		
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		
		labelButton = new JToggleButton();
		if (channel.getIcon() == null) 
			labelButton.setText(channel.getName());
		else 
			labelButton.setIcon(channel.getIcon());
		
		labelButton.setPreferredSize(lbl);
		
		labelButton.setFont(BOLD);
		labelButton.setForeground(Color.DARK_GRAY);
		labelButton.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
		labelButton.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "none");
	
		volume = new Knob(vol -> {channel.setVolume(vol);});

		if (this instanceof Input)
			add(labelButton, BorderLayout.LINE_START);
		add(volume, BorderLayout.CENTER);
		JPanel boxes = new JPanel();
		
		onMute = new MixButton(Icons.MUTE, channel);
		onMute.setToolTipText("Mute Audio");
		onMute.addMouseListener(new MouseAdapter() {
			@Override public void mouseReleased(MouseEvent e) {
				channel.setOnMute(!channel.isOnMute());
		}});
		
		boxes.add(onMute);
		customActions = customInit();
		for (MixButton btn : customActions) {
			assert btn != null;
			btn.addMouseListener(new MouseAdapter() {
				@Override public void mouseReleased(MouseEvent e) {
					customAction(btn);
			}});
			boxes.add(btn);
		}
		add(boxes, BorderLayout.LINE_END);

		labelButton.addActionListener(listener -> {
			if (labelButton.isSelected())
				MainFrame.get().getRight().setFocus(channel);
		});
		
		if (this instanceof Output || this instanceof Master)
			add(labelButton, BorderLayout.LINE_END);

		update();
		Constants.Gui.attachKeyListener(this, MenuBar.getInstance());

	}

	protected abstract void customAction(MixButton btn);
	protected abstract List<MixButton> customInit();

	public void update() {
		volume.setValue(channel.getVolume());
		onMute.update();
		for (MixButton btn : customActions)
			btn.update();
		if (channel == SoloTrack.getInstance().getFocus()) 
			SoloTrack.getInstance().update();
	}
	
	public void setVolume(int vol) {
		volume.setValue(vol);
	}

	public static class Input extends ChannelGui {
		
		public Input(LineIn channel) { 
			super(channel);
		}
		
		@Override protected List<MixButton> customInit() {
			MixButton muteRecord = new MixButton(Icons.MUTE_RECORD, channel);
			muteRecord.setToolTipText("Mute Recording");
			return Arrays.asList(new MixButton[] {muteRecord});
		}

		@Override protected void customAction(MixButton customAction) {
			((LineIn)channel).setMuteRecord(customAction.isSelected());
		}

	}
	
	public static class Output extends ChannelGui {
		
		public Output(Channel channel) { 
			super(channel); 
		}
		
		@Override protected List<MixButton> customInit() {
			MixButton recordBtn = new MixButton(Icons.MICROPHONE, channel);
			recordBtn.setToolTipText("Record on this loop");
			MixButton playBtn = new MixButton(Icons.PLAY, channel);
			playBtn.setToolTipText("Play this loop");
			return Arrays.asList(new MixButton[] {recordBtn, playBtn});
		}

		@Override protected void customAction(MixButton customAction) {
			if (Icons.PLAY.getName().equals(customAction.getName())) {
				Sample sample = (Sample)channel;
				sample.play(AudioMode.RUNNING != sample.isPlaying());
			}
			else if (Icons.MICROPHONE.getName().equals(customAction.getName()) && channel instanceof Recorder) {
				Recorder recorder = (Recorder)channel;
				recorder.record(AudioMode.RUNNING != recorder.isRecording());
			}
			else 
				Console.info("skipping custom: " + customAction);
				
		}

	}

	public static class Drums extends Output {
		public Drums(Channel bus) {
			super(bus);
		}
	}

	public static class Master extends ChannelGui {
		
		public Master(MasterTrack master) {
			super(master);
		}

		@Override
		protected List<MixButton> customInit() {
			MixButton transport = new MixButton(Icons.PLAY, channel);
			transport.setToolTipText("Transport start/stop");
			return Arrays.asList(new MixButton[] {transport});
		}

		@Override
		protected void customAction(MixButton btn) {
			Sequencer.transport();
		}
	}
	
	public static ChannelGui create(Channel ch) {
		if (ch instanceof DrumTrack) return new ChannelGui.Drums(ch);
		if (ch instanceof MasterTrack) return new ChannelGui.Master((MasterTrack)ch);
		if (ch instanceof LineIn) return new ChannelGui.Input((LineIn)ch);
		if (ch instanceof Sample) return new ChannelGui.Output(ch);
		throw new InvalidParameterException(ch.getClass().getCanonicalName());
	}
	
}

