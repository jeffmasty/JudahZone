package net.judah.util;

import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.sequencer.Sequencer;
import net.judah.song.Song;

@Log4j
public class MenuBar extends JMenuBar {

	
    JMenu fileMenu = new JMenu("Song");

	JMenuItem load = new JMenuItem("Open...");
    JMenuItem create = new JMenuItem("New...");
    JMenuItem save = new JMenuItem("Save");
    JMenuItem saveAs = new JMenuItem("Save As...");
    JMenuItem close = new JMenuItem("Close Song");
    JMenuItem exit = new JMenuItem("Exit");
	
	public MenuBar() {

		fileMenu.setMnemonic(KeyEvent.VK_F);
        load.addActionListener( (event) -> load());
        fileMenu.add(load);
        create.addActionListener( (event) -> create());
        fileMenu.add(create);
        save.addActionListener( (event) -> save());
        fileMenu.add(save);
        save.addActionListener( (event) -> saveAs());
        fileMenu.add(saveAs);
        close.addActionListener( (event) -> MainFrame.get().closeTab(JudahZone.getCurrentSong().getPage()));
        fileMenu.add(close);
        
		exit.setMnemonic(KeyEvent.VK_E);
        exit.setToolTipText("Exit application");
        exit.addActionListener((event) -> System.exit(0));
        fileMenu.add(exit);
        add(fileMenu);
        
//		editMenu.setMnemonic(KeyEvent.VK_E);
//		copy.addActionListener( (event) -> copy() );
//		editMenu.add(copy);
//		cut.addActionListener( (event) -> cut() );
//		editMenu.add(cut);
//		paste.addActionListener( (event) -> paste() );
//		editMenu.add(paste);
//		add.addActionListener( (event) -> add() );
//		editMenu.add(add);
//		delete.addActionListener( (event) -> delete() );
//		editMenu.add(delete);
//		add(editMenu);
        
	}
	
	public void save() {
		Sequencer s = JudahZone.getCurrentSong();
		if (s == null) return;
		s.getPage().save(s.getSongfile());
	}
	
	public void saveAs() {
		Sequencer s = JudahZone.getCurrentSong();
		if (s == null) return;
		File file = FileChooser.choose();
		if (file == null) return;
		s.getPage().save(file);
	}
	
	public void load() {
		File file = FileChooser.choose();
		if (file == null) return;

		try { 
			Song song = (Song)JsonUtil.readJson(file, Song.class);
			new Sequencer(song, file);
		} catch (Exception e) {
			log.error(e.getMessage() + " - " + file.getAbsolutePath());
			Constants.infoBox(file.getAbsolutePath() + ": " + e.getMessage(), "Error");
		}

	}
	
	public void create() {
		File file = FileChooser.choose();
		if (file == null) return;
		Song song = new Song();
		try {
			JsonUtil.saveString(JsonUtil.MAPPER.writeValueAsString(song), file);
			new Sequencer(song, file);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Constants.infoBox(e.getMessage(), "Error");
		}
	}
	
	
}
