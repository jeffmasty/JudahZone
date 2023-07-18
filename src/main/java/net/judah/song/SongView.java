package net.judah.song;

import static net.judah.JudahZone.getScene;
import static net.judah.JudahZone.getSong;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.song.cmd.Param;
import net.judah.song.cmd.ParamModel;
import net.judah.song.cmd.ParamTable;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class SongView extends JPanel {
	
	@Getter private final SceneLauncher launcher;
	private final ParamTable params;
	private final JComboBox<Trigger> sceneType = new JComboBox<>(Trigger.values());
	private final JTextField notes = new JTextField(12);

	public SongView(Song smashHit, Overview tab, Dimension props, Dimension btns) {
		launcher = new SceneLauncher(smashHit, tab);
		params = new ParamTable(smashHit.getScenes().get(0).getCommands());
		JScrollPane scroll = new JScrollPane(params);
		Gui.resize(scroll, props);
		Gui.resize(launcher, btns);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(sceneBtns1());
		add(sceneBtns2());
		add(launcher);

		JPanel paramBtns = new JPanel(new GridLayout(0, 1));
		paramBtns.add(new Btn("Add", e->addParam()));
		paramBtns.add(new Btn("Del", e->removeParam()));
		paramBtns.add(new Btn("Save", e->JudahZone.save()));
		paramBtns.setBackground(Pastels.BUTTONS);
		JPanel bottom = Gui.wrap(scroll, paramBtns);
		bottom.setBackground(Pastels.BUTTONS);
		add(bottom);
	}

	private JPanel sceneBtns1() {
		sceneType.addActionListener(e->triggerType());
		notes.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				getScene().setNotes(notes.getText());
				launcher.update(getScene());
			}});

		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.LINE_AXIS));
		result.add(new JLabel(" Scene  "));
		result.add(Gui.resize(notes, new Dimension(Size.TITLE_SIZE.width, Size.STD_HEIGHT)));
		result.add(Box.createHorizontalGlue());
		result.add(new JLabel("Cue "));
		result.add(sceneType);
		return result;
	}

	private JPanel sceneBtns2() {
		JPanel result = new JPanel(new GridLayout(1, 5));
		result.add(new Btn("<--", e->shift(true)));
		result.add(new Btn("Create", e->newScene()));
		result.add(new Btn("Copy", e->copy())); 
		result.add(new Btn("Del", e->delete()));
		result.add(new Btn("-->", e->shift(false)));
		return result;
	}
			
	private void triggerType() {
		getScene().setType((Trigger)sceneType.getSelectedItem());
		MainFrame.update(getScene());
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
		params.setModel(new ParamModel(getScene().getCommands())); //overkill?
		launcher.update(s);
	}
	
	private void addParam() {
		getScene().getCommands().add(new Param());
		update(getScene());
	}

	private void removeParam() {
		int selected = params.getSelectedRow();
		if (selected == -1) 
			selected = 1;
		
		if (selected < getScene().getCommands().size())
			getScene().getCommands().remove(selected);
		update(getScene());
	}

	public void addScene(Scene add) {
		getSong().getScenes().add(add);
		MainFrame.setFocus(launcher);
		MainFrame.setFocus(add);
		JudahZone.setOnDeck(null);
	}
	
	public void copy() {
		addScene(getScene().clone());
	}

	public void newScene() {
		addScene(new Scene(getScene().getTracks()));
	}

	public void delete() {
		Song song = getSong();
		Scene current = getScene();
		if (current == song.getScenes().get(0)) 
			return; // don't remove initial scene
		song.getScenes().remove(current);
		launcher.fill();
		JudahZone.setScene(0);
	}
	
	public void shift(boolean left) {
		List<Scene> scenes = getSong().getScenes();
		int old = scenes.indexOf(getScene());
		if (old == 0) { 
			RTLogger.log(this, "INIT Scene is fixed.");
			return; 
		}
		int idx = old + (left ? -1 : 1);
		if (idx == 0)
			idx = scenes.size() - 1;
		if (idx == scenes.size())
			idx = 1;
		Collections.swap(scenes, old, idx);
		launcher.fill();
		MainFrame.setFocus(getScene());
	}
	


}
