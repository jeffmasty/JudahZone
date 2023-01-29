package net.judah.song;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.*;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;

public class SongView extends JPanel implements TimeListener {
	
	private final Song song;
	private final SongTab tab;

	@Getter private static Scene onDeck;
	@Getter private final ScenesView launcher;
	@Getter private int count;
	private final JComboBox<Trigger> sceneType = new JComboBox<>(Trigger.values());

	private final JPanel sceneMenu = new JPanel();
	private final JPanel paramMenu = new JPanel();
	private final ParamView params;
	private final JTextField notes = new JTextField(12);

	public SongView(Song smashHit, SongTab tab) {
		this.song = smashHit;
		this.tab = tab;
		params = new ParamView(song.getScenes().get(0).getCommands());
		launcher = new ScenesView(song, tab);

		menus();

		JScrollPane scroll = new JScrollPane(params);
		Dimension props = new Dimension((int)(Size.WIDTH_TAB * 0.3f), (int)(Size.HEIGHT_TAB * 0.27f));
		scroll.setMaximumSize(props);
		scroll.setPreferredSize(props);
		Dimension btns = new Dimension((int)(Size.WIDTH_TAB * 0.39f), (int)(Size.WIDTH_TAB * 0.27f));
		launcher.setPreferredSize(btns);
		launcher.setMaximumSize(btns);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(launcher);
		add(sceneMenu);
		add(paramMenu);
		add(scroll);
		add(new JLabel(" "));

	}

	private void menus() {

		sceneType.addActionListener(e->triggerType());
		sceneMenu.add(new JLabel("Type"));
		sceneMenu.add(sceneType);
		sceneMenu.add(new Btn("<--", e->tab.shift(true)));
		sceneMenu.add(new Btn("Create", e->tab.newScene()));
		sceneMenu.add(new Btn("Copy", e->tab.copy())); 
		sceneMenu.add(new Btn("Del", e->tab.delete()));
		sceneMenu.add(new Btn("-->", e->tab.shift(false)));

		paramMenu.add(new JLabel("Commands"));
		paramMenu.add(new Btn("Add", e->addParam()));
		paramMenu.add(new Btn("Del", e->removeParam()));
		paramMenu.add(new JLabel(" notes:"));
		notes.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				tab.getCurrent().setNotes(notes.getText());}});
		paramMenu.add(notes);
	}

	
	
	private void triggerType() {
		tab.getCurrent().setType((Trigger)sceneType.getSelectedItem());
		MainFrame.update(tab);
	}

	private void peek(Scene current) {
		int next = 1 + song.getScenes().indexOf(current); 
		if (song.getScenes().size() <= next /* && getClock().isActive() */) {
			setOnDeck(null);
			return;
		}
		Scene peek = song.getScenes().get(next);
		if (peek.getType() == Trigger.REL) {
			count = 0;
			setOnDeck(peek);
		}
		else if (peek.getType() == Trigger.ABS) 
			setOnDeck(peek);
		else setOnDeck(null);;
	}
	
	public void setOnDeck(Scene scene) {
		
		if (scene == null) 
			onDeck = null;
		else if (scene.getType() == Trigger.HOT)
			tab.launchScene(scene);
		else if (scene.getType() == Trigger.JUMP) {
			int idx = (int)scene.getCommands().getTimeCode();
			if (song.getScenes().size() > idx) 
				setOnDeck(song.getScenes().get(idx));
		}
		else if (onDeck == scene)  // force
			tab.launchScene(scene);
		else 
			onDeck = scene;
		
		MainFrame.update(this);
		
	}
	
	public void setCurrent(Scene s) {
		new Thread(() -> 
			params.setModel(new ParamModel(s.getCommands()))).start();
		notes.setText(s.getNotes());
		if (sceneType.getSelectedItem() != s.getType())
			sceneType.setSelectedItem(s.getType());
		peek(s);
	}

	public void update() {
		launcher.update();
	}

	private void addParam() {
		tab.getCurrent().getCommands().add(new Param());
		params.setModel(new ParamModel(tab.getCurrent().getCommands()));
	}

	private void removeParam() {
		int selected = params.getSelectedRow();
		if (selected == -1) 
			selected = 1;
		
		if (selected < tab.getCurrent().getCommands().size())
			tab.getCurrent().getCommands().remove(selected);
		params.setModel(new ParamModel(tab.getCurrent().getCommands()));
	}

	@Override
	public void update(Property prop, Object value) {
		if (onDeck == null) 
			return;
		
		if (prop == Property.BARS && onDeck.type == Trigger.BAR)	
			go();
		else if (prop == Property.LOOP && onDeck.type == Trigger.LOOP) 
			go();
		else if (onDeck.type == Trigger.ABS && prop == Property.BEAT) {
			if ((int)value >= onDeck.getCommands().getTimeCode())
				go();
		}
		else if (onDeck.type == Trigger.REL && prop == Property.BEAT) {
			if (++count >= onDeck.getCommands().getTimeCode()) 
				go();
			else 
				MainFrame.update(onDeck);
		}
	}

	private void go() { 
		if (onDeck != null)  
			tab.launchScene(onDeck);
	}


}
