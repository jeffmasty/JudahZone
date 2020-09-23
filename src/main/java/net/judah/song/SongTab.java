package net.judah.song;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.commons.io.FilenameUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.metronome.Sequencer;
import net.judah.util.Constants;
import net.judah.util.JsonUtil;
import net.judah.util.PropertiesTable;
import net.judah.util.Tab;

@Log4j
public class SongTab extends Tab {

	private Song song;
	@Getter private final Sequencer sequencer;
	
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JButton save, close, add, delete, copy;

	private final PropertiesTable properties;
	private final LinkTable links;
	private final TriggersTable triggers;
	private final RouterTable router;
	
	public SongTab(Sequencer sequencer) {
		super(true);
		this.sequencer = sequencer;
		this.song = sequencer.getSong();

		setLayout(new BorderLayout());
		properties = new PropertiesTable(song.getProps());
		links = new LinkTable(song.getLinks(), sequencer.getCommander());
		triggers = new TriggersTable(song.getSequencer(), sequencer.getCommander());
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
		
	}

	private void save() {
		File file = sequencer.getSongfile();
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
		return Constants.CUTE_NOTE + FilenameUtils.removeExtension(sequencer.getSongfile().getName());
	}
	@Override public void actionPerformed(ActionEvent e) { }
	@Override public void setProperties(Properties p) { }
	
}

