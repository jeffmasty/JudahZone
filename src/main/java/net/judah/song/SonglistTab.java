package net.judah.song;

import static net.judah.util.Constants.Gui.BTN_MARGIN;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import lombok.extern.log4j.Log4j;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.FileCellRenderer;
import net.judah.util.FileChooser;
import net.judah.util.JsonUtil;

@Log4j
public class SonglistTab extends JComponent implements ListSelectionListener {

	public static final String SUFFIX = ".songs";
	public static final String TABNAME = "Songs";
	
	private File file;
	private SonglistModel model;
	
	JPanel listPanel, buttonsPanel;
	
	private final JList<File> jsongs = new JList<File>();
	private JButton setlistLabel;
	private JButton newSonglist, saveSonglist;
	
	private JButton deleteSong; 
	// private JButton upSong, downSong, copySong; 
	
	public SonglistTab(File setlist) {
		this.file = setlist;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		filePanel();
		buttonsPanel();
		
		loadSetlist(setlist);
		if (setlist == null) {
			Console.addText("No Set List");
			return;
		}

		jsongs.setCellRenderer(new FileCellRenderer());
		jsongs.addListSelectionListener(this);
		jsongs.addMouseListener(new MouseAdapter() {
			 @Override public void mouseClicked(MouseEvent mouseEvent) {
				 if (mouseEvent.getClickCount() == 2 && jsongs.getSelectedIndex() >= 0 && !mouseEvent.isConsumed()) { 
					 openSong(jsongs.getSelectedIndex());
					 mouseEvent.consume();
				 }}});
		add(new JScrollPane(jsongs));

	}

	private void filePanel() {
		
		JPanel filePanel = new JPanel(new FlowLayout());
		
		setlistLabel = new JButton(setlistLabel(file));
		setlistLabel.setToolTipText(file.getAbsolutePath());
		setlistLabel.addActionListener((event) -> loadSetlist(null));
		setlistLabel.setMinimumSize(new Dimension(100, 30));

		// https://en-human-begin.blogspot.com/2007/11/javas-icons-by-default.html
		newSonglist = new JButton(UIManager.getIcon("Tree.leafIcon"));
		newSonglist.setMargin(BTN_MARGIN);
		newSonglist.addActionListener((event) -> newSetlist());
		newSonglist.setToolTipText("New List");
		
		saveSonglist = new JButton(UIManager.getIcon("FileView.floppyDriveIcon"));
		saveSonglist.setMargin(BTN_MARGIN);
		saveSonglist.addActionListener((event) -> saveSetlist());
		saveSonglist.setToolTipText("Save List");
		
		filePanel.add(setlistLabel);
		filePanel.add(newSonglist);
		filePanel.add(saveSonglist);
		add(filePanel);
	}
	
	private String setlistLabel(File file) {
		int i = file.getName().lastIndexOf(SUFFIX);
		return "Set: " + ((i < 0) ? file.getName() : file.getName().substring(0, i));
	}
	
	enum Type {
		NEW, ADD, DELETE, COPY, UP, DOWN;
		
		private String lbl; 
		Type() {
			lbl = name().charAt(0) + name().substring(1).toLowerCase(); 
		}
		
		@Override
		public String toString() {
			return lbl;
		}
	};
	
	private void buttonsPanel() {
		buttonsPanel = new JPanel(new FlowLayout());
		button(Type.NEW);
		button(Type.ADD);
		deleteSong = button(Type.DELETE);
		// copySong = button(CMD.COPY);
		// upSong = button(CMD.UP);
		// downSong = button(CMD.DOWN);
		buttonsPanel.doLayout();
		add(buttonsPanel);
	}
	
	private JButton button(Type cmd) {
		JButton result = new JButton(cmd.lbl);
		result.setMargin(BTN_MARGIN);
		result.setActionCommand(cmd.name());
		result.addActionListener((event) -> songButton(event.getActionCommand()));
		buttonsPanel.add(result);
		return result;
	}
	
	private void songButton(String cmd) {
		if (cmd == Type.NEW.name())
			newSong();
		else if (cmd == Type.ADD.name()) 
			addSong();
		else if (cmd == Type.DELETE.name())
			deleteSong();
		else if (cmd == Type.COPY.name())
			copySong();
		else if (cmd == Type.UP.name()) 
			moveSong(true);
		else if (cmd == Type.DOWN.name())
			moveSong(false);
		else log.warn("Tale of the unknown soldier: " + cmd);
	}
	
	private void newSong() {
		
		File file = FileChooser.choose();
		if (file == null) return;
		Song song = new Song();
		try {
			JsonUtil.saveString(JsonUtil.MAPPER.writeValueAsString(song), file);
			if (jsongs.getSelectedIndex() < 0)
				model.addElement(file);
			else
				model.insertElementAt(file, jsongs.getSelectedIndex() + 1);
			saveSetlist();
			new Sequencer(file);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Constants.infoBox(e.getMessage(), "Error");
		}

	}
	
	private void addSong() {
		File file = FileChooser.choose();
		if (file == null) return;

		try { // to validate file
			JsonUtil.readJson(file, Song.class);
			if (jsongs.getSelectedIndex() < 0)
				model.addElement(file);
			else
				model.insertElementAt(file, jsongs.getSelectedIndex() + 1);
			saveSetlist();
		} catch (IOException e) {
			log.error(e.getMessage() + " - " + file.getAbsolutePath());
			Constants.infoBox(file.getAbsolutePath() + ": " + e.getMessage(), "Error");
		}
	}
	
	private void deleteSong() {
		for (File f : jsongs.getSelectedValuesList()) {
			model.removeElement(f);
		}
		saveSetlist();
	}
	
	private void copySong() {
		int i = jsongs.getSelectedIndex();
		if (i < 0) return;
		File f = FileChooser.choose();
		if (f == null) return;
		model.addElement(f);
		log.warn("copying " + model.getElementAt(i).getAbsolutePath() + " to " + f.getAbsolutePath());
	}
	
	private void moveSong(boolean up) {
		
	}
	
	private void loadSetlist(File f) {
		if (f == null) f = FileChooser.choose();
		if (f == null) return;
		
		try {
			model = new SonglistModel(JsonUtil.loadSetlist(f));
			jsongs.setModel(model);
			file = f;
			setlistLabel.setText(setlistLabel(file));
		} catch (IOException e) {
			Constants.infoBox(e.getMessage(), "Error on Songlist");
		}
	}
	private void newSetlist( ) {
		File f = FileChooser.choose();
		if (f == null) return;
		
		String name = f.getName();
		if (!name.endsWith(SUFFIX)) {
			name = f.getName() + SUFFIX;
			f = new File(f.getParent(), name);
		}
		if (f.exists()) {
			Constants.infoBox(f.getName() + " already exists.", "Songlist");
			return;
		}

		model = new SonglistModel(new ArrayList<String>());
		jsongs.setModel(model);
		file = f;
		setlistLabel.setText(setlistLabel(file));
		log.warn("setlist created");	
	}
	
	private void saveSetlist() {
		try {
			JsonUtil.saveSetList(model.getAll(), file); 
			log.warn("setlist saved");
		} catch (IOException e) {
			Constants.infoBox(e.getMessage(), "Not Saved");
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		int selected[] = jsongs.getSelectedIndices();
		if (selected.length == 0) {
			deleteSong.setEnabled(false);
//			upSong.setEnabled(false);
//			downSong.setEnabled(false);
		} else {
			deleteSong.setEnabled(true);
//			upSong.setEnabled(true);
//			downSong.setEnabled(true);
		}
	}
	
	void openSong(int index) {
		if (index < 0 || index >= model.getSize())
			return;
		
		File file = model.getElementAt(index);
		try {
			new Sequencer(file);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			log.error(file.getAbsolutePath());
			Constants.infoBox(file.getAbsoluteFile() + " -- " + e.getMessage(), "Song Load Failed");
		}
	}
	
	
}
