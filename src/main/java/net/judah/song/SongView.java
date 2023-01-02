package net.judah.song;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.util.Folders;
import net.judah.widgets.Btn;
import net.judah.widgets.FileChooser;

public class SongView extends JPanel implements TimeListener {
	
	private final Song song;
	private final SongTab tab;
	@Getter private final ScenesView launcher;
	@Getter private Scene onDeck;
	private int count;
	
	private final JComboBox<File> songs = new JComboBox<>();
	private final JComboBox<Trigger> sceneType = new JComboBox<>(Trigger.values());
	private final JPanel menu = new JPanel();
	private final JPanel paramMenu = new JPanel();
	private final ParamView params;
	private final JTextField notes = new JTextField(20);
	private final ActionListener songSelect = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			JudahZone.loadSong((File)songs.getSelectedItem()); }};

	public SongView(Song smashHit, SongTab tab) {
		this.song = smashHit;
		this.tab = tab;
		params = new ParamView(song.getScenes().get(0).getParams());
		launcher = new ScenesView(song, tab);
		songs.setRenderer(FileChooser.RENDERER);

		fill();
		menus();

		JScrollPane scroll = new JScrollPane(params);
		Dimension props = new Dimension((int)(Size.WIDTH_TAB * 0.3f), (int)(Size.HEIGHT_TAB * 0.27f));
		scroll.setMaximumSize(props);
		scroll.setPreferredSize(props);
		Dimension btns = new Dimension((int)(Size.WIDTH_TAB * 0.35f), (int)(Size.WIDTH_TAB * 0.27f));
		launcher.setPreferredSize(btns);
		launcher.setMaximumSize(btns);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(menu);
		add(launcher);
		add(paramMenu);
		add(scroll);
		add(new JLabel(" "));

	}

	private void menus() {

		sceneType.addActionListener(e->triggerType());
		menu.add(new Btn("Save", e->save()));
		menu.add(songs); 
		menu.add(new Btn("<-", e->{}));
		menu.add(new Btn("Create", e->tab.newScene()));
		menu.add(new Btn("Copy", e->tab.copy())); 
		menu.add(new Btn("Del", e->tab.delete()));
		menu.add(new Btn("->", e->{}));
		menu.add(sceneType);
		menu.add(Box.createHorizontalGlue());

		paramMenu.add(new Btn("Add", e->addParam()));
		paramMenu.add(new Btn("Del", e->removeParam()));
		paramMenu.add(new JLabel(" notes:"));
		notes.addKeyListener(new KeyAdapter() {
			@Override public void keyTyped(KeyEvent e) {
				tab.getCurrent().setNotes(notes.getText());}});
		paramMenu.add(notes);
	}

	private void triggerType() {
		
		tab.getCurrent().setType((Trigger)sceneType.getSelectedItem());
		MainFrame.update(tab);
	}

	public void setOnDeck(Scene scene) {
		if (!JudahZone.getClock().isActive() || scene.getType() == Trigger.CUE)
			tab.launchScene(scene);
		else {
			onDeck = scene;
			MainFrame.update(tab);
		}
	}
	
	public void setCurrent(Scene s) {
		notes.setText(s.getNotes());
		new Thread(() -> 
			params.setModel(new ParamModel(s.getParams()))).start();
		if (sceneType.getSelectedItem() != s.getType())
			sceneType.setSelectedItem(s.getType());
		update();
	}
	public void update() {
		launcher.update();
	}
	
	/** save + reload from disk, re-set current scene */
	private void save() { 
		int idx = song.getScenes().indexOf(tab.getCurrent());
		JudahZone.save();
		Song newSong = JudahZone.loadSong(song.getFile());
		MainFrame.setFocus(newSong.getScenes().get(idx));
	}
	
	public void fill() {
		songs.removeActionListener(songSelect);
		songs.removeAllItems();
		for (File f : Folders.getSetlist().listFiles()) {
			songs.addItem(f);
			if (f.equals(song.getFile()))
				songs.setSelectedItem(f);
		}
		songs.addActionListener(songSelect);
	}
	
	
	private void addParam() {
		tab.getCurrent().getParams().add(new Param());
		params.setModel(new ParamModel(tab.getCurrent().getParams()));
	}
	
	private void removeParam() {
		int selected = params.getSelectedRow();
		if (selected == -1) 
			selected = 1;
		
		if (selected < tab.getCurrent().getParams().size())
			tab.getCurrent().getParams().remove(selected);
		params.setModel(new ParamModel(tab.getCurrent().getParams()));
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (onDeck == null)
			return;
		if (onDeck.type == Trigger.INIT || onDeck.type == Trigger.BAR) {
			if (prop == Property.BARS)	
				go();
		}
		else if (prop == Property.LOOP && onDeck.type == Trigger.LOOP) {
			go();
		}
		else if (onDeck.type == Trigger.ABS && prop == Property.BEAT) {
			if ((int)value >= onDeck.getParams().getTimeCode())
				go();
		}
		else if (onDeck.type == Trigger.REL && prop == Property.BEAT) {
			if (++count >= (int)value)
				go();
		}
	}

	private void go() { 
		count = JudahZone.getClock().getBeat();
		tab.launchScene(onDeck);
		onDeck = null; 
	}
	
	
	
}
