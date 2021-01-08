package net.judah;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import lombok.Getter;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelGui;
import net.judah.mixer.MixerBus;
import net.judah.mixer.EffectsGui;
import net.judah.song.SonglistTab;
import net.judah.util.Constants;

public class MixerPane extends JPanel {
	
	public static final int WIDTH = 360;
	
	@Getter private static MixerPane instance;
	
	private final JComponent songlist;
	private final JTabbedPane tabs; 
	private final JPanel mixer = new JPanel();
	private final LooperGui looper;
	@Getter private final EffectsGui highlight = new EffectsGui();
	
	public MixerPane() {
		
		instance = this;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));		

        tabs = new JTabbedPane();
		songlist = new SonglistTab(Constants.defaultSetlist);
		tabs.add("Setlist", songlist);
		tabs.add("Metronome", JudahZone.getMetronome().getGui());
		tabs.add("Channel", highlight);
		tabs.setSelectedIndex(tabs.getComponentCount() - 1);
		add(tabs);
		
		mixer.setLayout(new GridLayout(0,2));
		for (Channel channel : JudahZone.getChannels()) 
			mixer.add(channel.getGui());
		add(mixer);
		// looper = new JList<Sample>(JudahZone.getLooper().toArray(new Sample[JudahZone.getLooper().size()]));
		looper = new LooperGui(JudahZone.getLooper());
		add(looper); 
		setSize(WIDTH, getPreferredSize().height);
		setFocus(JudahZone.getChannels().getGuitar());
		
	}

	public void setFocus(MixerBus bus) {
		highlight.setFocus(bus);
		tabs.setSelectedComponent(highlight);
		tabs.setTitleAt(tabs.indexOfComponent(highlight), bus.getName());
		bus.getGui().getLabelButton().requestFocus();
		for(Component c : mixer.getComponents()) {
			if (c instanceof ChannelGui) {
				ChannelGui gui = (ChannelGui)c;
				gui.getLabelButton().setSelected(gui.getChannel() == bus);
			}
		}
		looper.setSelected(bus);
	}

}
