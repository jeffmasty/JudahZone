package net.judah.song;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.*;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.Btn;
import net.judah.util.Constants;

public class SongView extends JPanel {
	
	private final Song song;
	private final SongTab tab;
	@Getter private final ScenesView launcher;
	private final ParamView params;
	private final JComboBox<Trigger> sceneType = new JComboBox<>(Trigger.values());
	private final JPanel sceneBtns = new JPanel();
	private final JPanel paramBtns = new JPanel();
	private final JTextField notes = new JTextField(12);

	public SongView(Song smashHit, SongTab tab, Dimension props, Dimension btns) {
		this.song = smashHit;
		this.tab = tab;
		params = new ParamView(song.getScenes().get(0).getCommands());
		launcher = new ScenesView(song, tab);
		menus();

		JScrollPane scroll = new JScrollPane(params);
		scroll.setMaximumSize(props);
		scroll.setPreferredSize(props);
		launcher.setPreferredSize(btns);
		launcher.setMaximumSize(btns);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(launcher);
		add(sceneBtns);
		add(paramBtns);
		add(scroll);
		add(new JLabel(" "));
	}

	private void menus() {
		sceneType.addActionListener(e->triggerType());
		sceneBtns.add(new JLabel("Type"));
		sceneBtns.add(sceneType);
		sceneBtns.add(new Btn("<--", e->tab.shift(true)));
		sceneBtns.add(new Btn("Create", e->tab.newScene()));
		sceneBtns.add(new Btn("Copy", e->tab.copy())); 
		sceneBtns.add(new Btn("Del", e->tab.delete()));
		sceneBtns.add(new Btn("-->", e->tab.shift(false)));

		paramBtns.add(new JLabel("Commands"));
		paramBtns.add(new Btn("Add", e->addParam()));
		paramBtns.add(new Btn("Del", e->removeParam()));
		paramBtns.add(new JLabel(" notes:"));
		notes.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				tab.getCurrent().setNotes(notes.getText());}});
		paramBtns.add(notes);
	}
	
	private void triggerType() {
		tab.getCurrent().setType((Trigger)sceneType.getSelectedItem());
		MainFrame.update(tab);
	}
	
	public void setCurrent(Scene s) {
		Constants.execute(() -> 
			params.setModel(new ParamModel(s.getCommands())));
		notes.setText(s.getNotes());
		if (sceneType.getSelectedItem() != s.getType())
			sceneType.setSelectedItem(s.getType());
	}

	public void update() {
		launcher.update();
	}

	private void addParam() {
		tab.getCurrent().getCommands().add(new Param());
		params.setModel(new ParamModel(tab.getCurrent().getCommands())); //overkill?
	}

	private void removeParam() {
		int selected = params.getSelectedRow();
		if (selected == -1) 
			selected = 1;
		
		if (selected < tab.getCurrent().getCommands().size())
			tab.getCurrent().getCommands().remove(selected);
		params.setModel(new ParamModel(tab.getCurrent().getCommands()));
	}


}
