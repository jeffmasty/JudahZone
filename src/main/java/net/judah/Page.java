package net.judah;

import java.awt.Dimension;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.apache.commons.io.FilenameUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.sequencer.Sequencer;
import net.judah.song.LinkTable;
import net.judah.song.RouterTable;
import net.judah.song.Song;
import net.judah.song.SonglistTab;
import net.judah.song.TriggersTable;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JsonUtil;
import net.judah.util.PropertiesTable;

@Log4j
public class Page extends JSplitPane {

	public static final Dimension MIN = new Dimension(75, 75);

	private final JPanel NW = new JPanel();
	private final Console NE;
	private final JTabbedPane SW = new JTabbedPane();
	private final JTabbedPane SE = new JTabbedPane();
	
	@Getter private final Sequencer sequencer; 
	private final PropertiesTable properties;
	private final LinkTable links;
	private final TriggersTable triggers;
	private final RouterTable router;
	private final JButton save, close;
	
	public Page(Sequencer sequencer) {
		super(JSplitPane.VERTICAL_SPLIT, true);
		this.sequencer = sequencer;
		setName(FilenameUtils.removeExtension(sequencer.getSongfile().getName()));
		Song song = sequencer.getSong();
		NE = Console.getInstance();
		
		JComponent mixer = sequencer.getMixer().getGui();
		int mixer_height = 360;
		mixer.setMinimumSize(new Dimension(150, mixer_height));
		
		save = new JButton("Save");
		save.addActionListener( (event) -> save(sequencer.getSongfile()));
		close = new JButton("Close");
		close.addActionListener( (event) -> MainFrame.get().closeTab(this));

		JPanel songBtns = new JPanel();
		songBtns.setLayout(new BoxLayout(songBtns, BoxLayout.LINE_AXIS));
		songBtns.add(save);
		songBtns.add(close);
		JComponent songlist = new SonglistTab(JudahZone.defaultSetlist);
		songlist.setMinimumSize(new Dimension(75, 75));
		songlist.setPreferredSize(new Dimension(125, mixer_height));
		NW.setLayout(new BoxLayout(NW, BoxLayout.X_AXIS));
		JPanel firstSection = new JPanel();
		firstSection.setLayout(new BoxLayout(firstSection, BoxLayout.PAGE_AXIS));
		firstSection.add(songBtns);
		firstSection.add(songlist);
		
		NW.add(firstSection);
		NW.add(mixer);
		
		properties = new PropertiesTable(song.getProps());
		links = new LinkTable(song.getLinks(), sequencer.getCommander());
		triggers = new TriggersTable(song.getSequencer(), sequencer.getCommander());
		router = new RouterTable(song.getRouter());
		
		SW.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		SW.setMinimumSize(new Dimension(500, 75));
		SW.addTab("Router", router);
		SW.addTab("Properties", properties);
		
		SE.addTab("Triggers", triggers);
		SE.addTab("Midi Map", links);
		
		JSplitPane north = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		north.setLeftComponent(NW);
		north.setRightComponent(NE);
		JSplitPane south = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		south.setLeftComponent(SW);
		south.setRightComponent(SE);
		
		setTopComponent(north);
		setBottomComponent(south);

	}
	
	public void save(File file) {
		
		Song song = sequencer.getSong();
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
}
