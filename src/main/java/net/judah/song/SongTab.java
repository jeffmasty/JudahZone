package net.judah.song;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.metronome.Metronome;
import net.judah.metronome.Sequencer;
import net.judah.midi.MidiClient;
import net.judah.midi.MidiPair;
import net.judah.midi.Route;
import net.judah.plugin.Carla;
import net.judah.plugin.Drumkv1;
import net.judah.settings.Services;
import net.judah.util.Constants;
import net.judah.util.JsonUtil;
import net.judah.util.PropertiesTable;
import net.judah.util.Tab;

@Log4j
public class SongTab extends Tab {

	@Getter private Song song;
	private File file;
	private Sequencer sequencer = Sequencer.getInstance();
	
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JButton save, close, add, delete, copy;

	private final PropertiesTable properties;
	private final LinkTable links;
	private final TriggersTable triggers;
	private final RouterTable router;
	
	public SongTab(@NonNull Song song, @NonNull File file) {
		super(true);
		this.song = song;
		this.file = file;

		setLayout(new BorderLayout());
		properties = new PropertiesTable(song.getProps()); 
		links = new LinkTable(song.getLinks());
		triggers = new TriggersTable(song.getSequencer());
		router = new RouterTable(song.getRouter());
		
		tabbedPane.addTab("Router", router);
		tabbedPane.addTab("Midi Map", links);
		tabbedPane.addTab("Triggers", triggers);
		tabbedPane.addTab("Properties", properties);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        add(tabbedPane, BorderLayout.CENTER);
        
		JPanel footer = new JPanel();
		footer.setLayout(new BoxLayout(footer, BoxLayout.LINE_AXIS));
		JPanel buttons = new JPanel();
		add = new JButton("new");
		add.addActionListener( (event) -> ((Edits)tabbedPane.getSelectedComponent()).add());
		delete = new JButton("delete");
		delete.addActionListener( (event) -> ((Edits)tabbedPane.getSelectedComponent()).delete());
		copy = new JButton("copy");
		copy.addActionListener( (event) -> ((Edits)tabbedPane.getSelectedComponent()).copy());
		
		buttons.add(add);
		buttons.add(delete);
		buttons.add(copy);
		
        add(footer, BorderLayout.PAGE_END);

		save = new JButton("Save");
		save.addActionListener( (event) -> save());
		close = new JButton("Close");
		close.addActionListener( (event) -> JudahZone.closeTab(this));

		footer.add(save);
		footer.add(buttons);
		footer.add(close);
		
		// Song system setup
		((Metronome)Services.byClass(Metronome.class)).close();
		CommandHandler.clearMappings();
		CommandHandler.addMappings(song.getLinks());
		MidiClient midi = MidiClient.getInstance();

		midi.getRouter().clear();
		if (song.getRouter() != null) {
			for (MidiPair pair : song.getRouter()) 
				midi.getRouter().add(new Route(pair.getFromMidi(), pair.getToMidi()));
			log.debug("midi router handling " + midi.getRouter().size() + " translations");
		}		
		
		initializeProperties();
		sequencer.initialize(song.getSequencer());
	}
	
	private void initializeProperties() {
		final HashMap<String, Object> props = song.getProps();
		if (props == null) return;
		
		if (StringUtils.isNumeric("" + props.get("bpm"))) 
			sequencer.setTempo(Float.parseFloat("" + props.get("bpm")));
		if (StringUtils.isNumeric("" + props.get("bpb")))
			sequencer.setMeasure(Integer.parseInt("" + props.get("bpb")));
		
		if (props.containsKey("fluid")) {
			FluidSynth fluid = (FluidSynth)Services.byClass(FluidSynth.class);
			String[] split = props.get("fluid").toString().split(";");
			for (String cmd : split)
				fluid.sendCommand(cmd);
		}
		if (props.containsKey(Drumkv1.FILE_PARAM) && props.containsKey(Drumkv1.PORT_PARAM)) {
			MidiClient.getInstance().disconnectDrums();
			try {
				new Drumkv1(new File("" + props.get(Drumkv1.FILE_PARAM)), "" + props.get(Drumkv1.PORT_PARAM), false);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		if (props.containsKey("Carla")) {
			try {
				Carla carla = (Carla)Services.byClass(Carla.class);	
				File file = new File("" + props.get("Carla"));
				if (carla == null)
					new Carla(file);
				else 
					carla.reload(file); 
				
			} catch (Throwable t) {
				log.error(file + ": " + t.getMessage(), t);
				Constants.infoBox(t.getMessage() + " for " + file, "Song Error");
			}
		}
		
	}
	

	private void save() {
		try {
			song.setProps(properties.getMap());
			song.setLinks(links.getLinks());
			song.setSequencer(triggers.getSequence());
			song.setRouter(router.getRoutes());
			JsonUtil.saveString(JsonUtil.MAPPER.writeValueAsString(song), file);
			
			log.info("Saved: " + file.getAbsolutePath());
		} catch (Throwable e) {
			log.error(file.getAbsolutePath() + ": " + e.getMessage(), e);
			Constants.infoBox(e.getMessage() + Constants.NL + file.getAbsolutePath(), "Save Failed");
		}
	}

	@Override public String getTabName() {
		return Constants.CUTE_NOTE + FilenameUtils.removeExtension(file.getName());
	}
	@Override public void actionPerformed(ActionEvent e) { }
	@Override public void setProperties(Properties p) { }
	
}

