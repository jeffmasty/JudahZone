package net.judah.song;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.song.cmd.Param;
import net.judah.song.cmd.ParamModel;
import net.judah.song.cmd.ParamTable;
import net.judahzone.gui.Gui;
import net.judahzone.gui.Pastels;

/** Holds launch buttons for Song's Scenes and Cmds/Params */
public class SongView extends JPanel {

	@Getter private final SceneLauncher launcher;
	private final ParamTable params;
	private final JComboBox<Trigger> sceneType = new JComboBox<>(Trigger.values());
	private final JTextField notes = new JTextField(12);
	private final Overview songs;

	public SongView(Song smashHit, Overview tab, Dimension props, Dimension btns) {
		songs = tab;
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
		paramBtns.add(new Btn("Save", e->tab.save()));
		paramBtns.setBackground(Pastels.BUTTONS);
		JPanel bottom = new JPanel();
		bottom.setBackground(Pastels.BUTTONS);
		bottom.add(scroll);
		bottom.add(paramBtns);
		add(bottom);
	}

	private JPanel sceneBtns1() {
		sceneType.addActionListener(e->triggerType());
		notes.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				songs.getScene().setNotes(notes.getText());
				launcher.update(songs.getScene());
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
		songs.getScene().setType((Trigger)sceneType.getSelectedItem());
		MainFrame.update(songs.getScene());
	}

	public void setCurrent(Scene s) {
		notes.setText(s.getNotes());
		if (sceneType.getSelectedItem() != s.getType())
			sceneType.setSelectedItem(s.getType());
		reload(s);
	}

	private void reload(Scene s) {
		params.setModel(new ParamModel(s.getCommands()));
		launcher.update(s);
	}

	private void addParam() {
		Scene s = songs.getScene();
		s.getCommands().add(new Param());
		reload(s);
	}

	private void removeParam() {
		int selected = params.getSelectedRow();
		if (selected == -1)
			selected = 1;

		if (selected < songs.getScene().getCommands().size())
			songs.getScene().getCommands().remove(selected);
		reload(songs.getScene());
	}

	public void addScene(Scene add) {
		songs.getSong().getScenes().add(add);
		songs.setOnDeck(null);
		MainFrame.update(launcher);
		songs.setScene(add);
	}

	public void copy() {
		addScene(songs.getScene().clone());
	}

	public void newScene() {
		addScene(new Scene(JudahZone.getInstance().getSeq()));
	}

	public void delete() {
		Song song = songs.getSong();
		Scene current = songs.getScene();
		if (current == song.getScenes().get(0))
			return; // don't remove initial scene
		song.getScenes().remove(current);
		launcher.fill();
		songs.setScene(song.getScenes().get(0));
	}

	public void shift(boolean left) {
		List<Scene> scenes = songs.getSong().getScenes();
		int old = scenes.indexOf(songs.getScene());
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
		MainFrame.setFocus(songs.getScene());
	}

	public void updatePad(Scene onDeck) {
		Threads.execute(()->launcher.update(onDeck));
	}



}
