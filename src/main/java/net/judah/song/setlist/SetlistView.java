package net.judah.song.setlist;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.PresetsView.Button;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.FileRender;
import net.judah.gui.widgets.LengthCombo;
import net.judah.midi.JudahClock;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.song.Overview;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class SetlistView extends KnobPanel /* fwd knob input to MidiGui */ implements ListSelectionListener {
	public static final String TABNAME = "Setlists";

	private final Setlists setlists;
	private Setlist setlist;
	private final Overview tab;
	private final JList<File> jsongs = new JList<File>();
	private final JComboBox<Setlist> custom  = new JComboBox<>();
	private final ActionListener setlister;
	private File memory = Setlists.ROOT;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

	private final JButton upSong = 		new Button(" up ", e->moveSong(true));
	private final JButton downSong = 	new Button("down", e->moveSong(false));
	private final JButton deleteSong = 	new Button("delete", e->deleteSong());
	private final JButton addSong = 	new Button(" add ", e->addSong());
	@Getter private final KnobMode knobMode = KnobMode.Setlist;

	private void loadSong() {
		if (jsongs.getSelectedValue() != null)
			tab.loadSong(jsongs.getSelectedValue());
	}

	public SetlistView(Setlists setlists, Overview overview) {
		this.tab = overview;
		this.setlists = setlists;
		setlister = e->setSetlist((Setlist)custom.getSelectedItem());
		custom.setRenderer(new ListCellRenderer<Setlist>() {
			private final JLabel render = new JLabel("", JLabel.CENTER);
			@Override public Component getListCellRendererComponent(JList<? extends Setlist> list, Setlist setlist,
					int index, boolean isSelected, boolean cellHasFocus) {
			    render.setHorizontalAlignment(JLabel.CENTER);
			    render.setFont(Gui.BOLD);
				File file = setlist.getSource();
				if (file == null) {
					render.setText("?");
					return render;
				}
				if (file.getName().indexOf('.') > 1)
					render.setText(file.getName().substring(0, file.getName().lastIndexOf('.')));
				else
					render.setText(file.getName());
				return render;
			}});
		refill();
		jsongs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jsongs.setCellRenderer(new DefaultListCellRenderer() {
			@Override public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(FileRender.defix((File)value));
				setHorizontalAlignment(JLabel.CENTER);
				return this;
			}});
		jsongs.addListSelectionListener(this);
		jsongs.addMouseListener(new MouseAdapter() {
			 @Override public void mouseClicked(MouseEvent mouseEvent) {
				 if (mouseEvent.getClickCount() == 2 && jsongs.getSelectedIndex() >= 0 && !mouseEvent.isConsumed()) {
					 loadSong();
					 mouseEvent.consume();
				 }}});
		if (custom.getSelectedItem() != null)
			setSetlist((Setlist)custom.getSelectedItem());

		Component scroll = new JScrollPane(jsongs);
		JPanel btns = new JPanel();
		btns.setLayout(new BoxLayout(btns, BoxLayout.PAGE_AXIS));
		btns.add(Box.createVerticalStrut(3));
		btns.add(addSong); btns.add(deleteSong); btns.add(upSong); btns.add(downSong);
		btns.add(new Button("open", e->loadSong())); btns.add(Box.createVerticalGlue());

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(Box.createHorizontalStrut(6));
		add(Gui.resize(btns, new Dimension(92, HEIGHT_KNOBS - 30)));
		add(scroll);
		add(Box.createHorizontalStrut(13));

		title.add(Gui.resize(custom, WIDE_SIZE));
		title.add(new Btn(Icons.SAVE, e-> {if (setlist != null) setlist.save();}));
		title.add(new Btn(Icons.NEW_FILE, e->newSetlist()));
		title.invalidate();
		invalidate();
	}

	private void setSetlist(Setlist s) {
		if (setlist == s)
			return;
		setlist = s;
		renew();
	}

	private void refill() { // setlists
		custom.removeActionListener(setlister);
		custom.removeAllItems();
		Setlist select = setlists.getCurrent();
		List<Setlist> list = setlists.getCustom();
		Setlist current;
		for (int i = 0; i < list.size(); i++) {
			current = list.get(i);
			custom.addItem(current);
			if (current == select)
				custom.setSelectedItem(current);
		}
		custom.addActionListener(setlister);
	}

	private void renew() {
		DefaultListModel<File> model = new DefaultListModel<>();
		model.addAll(setlist.list());
		jsongs.setModel(model);
	}

	private void addSong() {
		File choose = Folders.choose(memory);
		if (choose == null || choose.isFile() == false)
			return;
		memory = choose.getParentFile();
		int idx = jsongs.getSelectedIndex() + 1;
		setlist.add(idx, choose);
		renew();
		jsongs.setSelectedIndex(idx);
	}

	private void deleteSong() {
		if (jsongs.getSelectedIndex() < 0) return;
		setlist.remove(jsongs.getSelectedIndex());
		renew();
	}

	private void moveSong(boolean up) {
		if (jsongs.getSelectedIndex() < 0) return;
		int move = jsongs.getSelectedIndex() + (up ? -1 : 1);
		if (move < 0) return;
		File f = setlist.remove(jsongs.getSelectedIndex());
		if (move > setlist.list().size())
			move = setlist.list().size();
		setlist.add(move, f);
		renew();
		jsongs.setSelectedIndex(move);
	}

	private void newSetlist( ) {
		String input = Gui.inputBox("Setlist Name");
		if (input == null || input.isBlank())
			return;
		if (!input.endsWith(Setlists.SUFFIX))
			input += Setlists.SUFFIX;
		File file = new File(Setlists.ROOT, input);
		if (file.exists()) {
			RTLogger.warn(this, "Already exists: " + file.getAbsolutePath());
			return;
		}

		Setlist created = new Setlist();
		created.setSource(file);
		created.save();
		setlists.add(created);
		setlists.setCurrent(file);
		refill();
		renew();
		RTLogger.log(this, input + " setlist created");
	}

	@Override public void valueChanged(ListSelectionEvent e) {
		int selected[] = jsongs.getSelectedIndices();

		if (selected.length == 0) {
			deleteSong.setEnabled(false);
			upSong.setEnabled(false);
			downSong.setEnabled(false);
		} else {
			deleteSong.setEnabled(true);
			upSong.setEnabled(true);
			downSong.setEnabled(true);
		}
	}

	@Override
	public boolean doKnob(int idx, int data2) {
		switch(idx) {
		case 0: JudahClock clock = JudahZone.getClock();
			if (data2 == 0)
				clock.setLength(1);
			else
				clock.setLength((int) Constants.ratio(data2, LengthCombo.LENGTHS));
			break;
    	case 1: // Select song
    		Threads.execute(()->jsongs.setSelectedIndex(Constants.ratio(data2, jsongs.getModel().getSize() - 1)));
    		break;
    	case 2: // Select StepSample
    		Threads.execute(()->custom.setSelectedIndex(Constants.ratio(data2, custom.getItemCount() - 1)));
    		break;
    	case 3:
    		JudahZone.getMains().getGain().set(Gain.VOLUME, data2);
    		MainFrame.update(JudahZone.getMains());
    		break;
		}
		return true;
	}

	@Override
	public void pad1() {
		loadSong();
	}

	@Override
	public void update() {
	}

}
