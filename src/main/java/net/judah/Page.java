package net.judah;

import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.commons.io.FilenameUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.sequencer.Sequencer;
import net.judah.sequencer.SeqWidget;
import net.judah.song.LinkTable;
import net.judah.song.RouterTable;
import net.judah.song.Song;
import net.judah.song.TriggersTable;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JsonUtil;
import net.judah.util.PropertiesTable;

@Log4j
public class Page extends JPanel {

	@Getter private final Sequencer sequencer;
	private final PropertiesTable properties;
	private final LinkTable links;
	private final TriggersTable triggers;
	private final RouterTable router;
	private final JButton save, close, reload;
	private final JTabbedPane cards = new JTabbedPane();
	
	public Page(Sequencer sequencer) {
		
		this.sequencer = sequencer;
		setName(FilenameUtils.removeExtension(sequencer.getSongfile().getName()));
		Song song = sequencer.getSong();
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		save = new JButton("Save");
		save.addActionListener( (event) -> save(sequencer.getSongfile()));
		close = new JButton("Close");
		close.addActionListener( (event) -> MainFrame.get().closeTab(this));
		reload = new JButton("Reload");
		reload.addActionListener( (event) -> reload());
		buttons.add(save);
		buttons.add(close);
		buttons.add(reload);
		
		properties = new PropertiesTable(song.getProps());
		links = new LinkTable(song.getLinks(), sequencer.getCommander());
		triggers = new TriggersTable(song.getSequencer(), sequencer.getCommander());
		router = new RouterTable(song.getRouter());

		cards.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		cards.addTab("Midi Map", links);
		cards.addTab("Router", router);
		cards.addTab("Properties", properties);
		cards.addTab("Sequencer", new SeqWidget());
		
		add(buttons);
		add(triggers);
		add(cards);
	}
	
	public void reload() {
		MainFrame.get().closeTab(this);
		try {
			new Sequencer(sequencer.getSongfile());
		} catch (Exception e) {
			Console.warn(e.getMessage() + " for " + sequencer.getSongfile());
			Constants.infoBox(e.getMessage(), "reload");
		}
	}

	public void save(File file) {
		
		Song song = sequencer.getSong();
		try {
			song.setProps(properties.getMap());
			song.setLinks(links.getLinks());
			song.setSequencer(triggers.getSequence());
			song.setRouter(router.getRoutes());
			JsonUtil.saveString(JsonUtil.MAPPER.writeValueAsString(song), file);
			
			Console.info("Saved: " + file.getAbsolutePath());
		} catch (Throwable e) {
			log.error(file.getAbsolutePath() + ": " + e.getMessage(), e);
			Constants.infoBox(e.getMessage() + Constants.NL + file.getAbsolutePath(), "Save Failed");
		}
	}

}
