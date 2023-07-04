package net.judah.song;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.Btn;
import net.judah.util.Constants;

public class SongView extends JPanel {
	
	private final Song song;
	private final SongTab tab;
	@Getter private final SceneLauncher launcher;
	private final ParamTable params;
	private final JComboBox<Trigger> sceneType = new JComboBox<>(Trigger.values());
	private final JTextField notes = new JTextField(12);

	public SongView(Song smashHit, SongTab tab, Dimension props, Dimension btns) {
		this.song = smashHit;
		this.tab = tab;
		launcher = new SceneLauncher(song, tab);
		params = new ParamTable(song.getScenes().get(0).getCommands());
		JScrollPane scroll = new JScrollPane(params);
		scroll.setMaximumSize(props);
		scroll.setPreferredSize(props);
		launcher.setPreferredSize(btns);
		launcher.setMaximumSize(btns);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(sceneBtns1());
		add(sceneBtns2());
		add(launcher);

		JPanel paramBtns = new JPanel(new GridLayout(0, 1));
		paramBtns.add(new Btn("Add", e->addParam()));
		paramBtns.add(new Btn("Del", e->removeParam()));
		paramBtns.add(new Btn("Save", e->JudahZone.save()));
		
		add(Gui.wrap(scroll, paramBtns));
	}

	private JPanel sceneBtns1() {
		sceneType.addActionListener(e->triggerType());
		notes.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				tab.getCurrent().setNotes(notes.getText());
				launcher.update(tab.getCurrent());
			}});

		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.LINE_AXIS));
		result.add(new JLabel("Scene "));
		result.add(notes);
		result.add(new JLabel("Cue"));
		result.add(sceneType);
		result.add(Box.createHorizontalGlue());
		return result;
	}

	private JPanel sceneBtns2() {
		JPanel result = new JPanel(new GridLayout(1, 5));
		result.add(new Btn("<--", e->tab.shift(true)));
		result.add(new Btn("Create", e->tab.newScene()));
		result.add(new Btn("Copy", e->tab.copy())); 
		result.add(new Btn("Del", e->tab.delete()));
		result.add(new Btn("-->", e->tab.shift(false)));
		return result;
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

	public void update(Scene s) {
		params.setModel(new ParamModel(tab.getCurrent().getCommands())); //overkill?
		launcher.update(s);
	}
	
	private void addParam() {
		tab.getCurrent().getCommands().add(new Param());
		update(tab.getCurrent());
	}

	private void removeParam() {
		int selected = params.getSelectedRow();
		if (selected == -1) 
			selected = 1;
		
		if (selected < tab.getCurrent().getCommands().size())
			tab.getCurrent().getCommands().remove(selected);
		update(tab.getCurrent());
	}


}
