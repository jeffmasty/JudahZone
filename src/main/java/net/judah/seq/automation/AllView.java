package net.judah.seq.automation;

import static net.judah.seq.automation.AllModel.TICK;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JScrollPane;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.midi.JudahMidi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.automation.AllModel.Time;
import net.judah.seq.automation.Automation.AutoBox;
import net.judah.seq.track.MidiTrack;

public class AllView extends AutoBox{

	private static AllView instance;
	public static AllView getInstance() {
		if (instance == null)
			instance = new AllView();
		return instance;
	}
	private static final Dimension SZ = new Dimension(Size.WIDTH_KNOBS - 40, Size.HEIGHT_KNOBS - 100);

	private ArrayList<Time> clipboard = new ArrayList<Time>();
	private final AllTable table = new AllTable();
	private AllModel model;
	private final JButton edit = new Btn("Edit", l->edit()); // pad1/doubleClick
	private final JButton delete = new Btn("Delete", l->delete()); // pad2
	private final JButton exe = new Btn("Exe", l->exe());
	private final JButton copy = new Btn("Copy", l->copy());
	private final JButton paste = new Btn("Paste", l->paste());
	private final JButton[] btns = {edit, delete, exe, copy, paste};

	AllView() {
		super(BoxLayout.PAGE_AXIS);
		for (JButton btn : btns)
			btn.setEnabled(false);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		add(Gui.wrap(edit, delete, copy, paste));
		add(Gui.resize(scroll, SZ));
		add(Box.createVerticalStrut(20));
		add(Box.createVerticalGlue());

		table.getSelectionModel().addListSelectionListener(l->selected());
		table.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					edit();
				// popup?
			}});
	}

	private void selected() {
		boolean ok = table.getSelectedRowCount() > 0;
		for (JButton btn : btns)
			btn.setEnabled(ok);
		paste.setEnabled(!clipboard.isEmpty());
	}

	@Override protected void setTrack(MidiTrack t) {
		track = t;
		model = new AllModel(track);
		table.setModel(model);
	}

	@Override
	protected AutoBox init(long tick) {

		// select tick
		return this;
	}


	/** edit first selected */
	private void edit() {
		int row = table.getSelectedRow();
		if (row < 0)
			return;
		row = table.convertRowIndexToModel(row);
		Time tick = (Time) table.getModel().getValueAt(row, TICK);
		Automation.getInstance().edit(track, tick.e());
	}

	private void  exe() {

		int[] selection = table.getSelectedRows();
		for (int i = 0; i < selection.length; i++) // convert selection to underlying TableModel
			selection[i] = table.convertRowIndexToModel(selection[i]);
		for (int row : selection) {
			Time tick = (Time) model.getValueAt(row, AllModel.TICK);
			track.send(tick.e().getMessage(), JudahMidi.ticker()); // non real-time on msgs to Fluid/Crave?
		}

	}


	/** delete all selected */
	private void delete() {
		ArrayList<MidiPair> notes = new ArrayList<MidiPair>();
		int[] selection = table.getSelectedRows();
		for (int i = 0; i < selection.length; i++) // convert selection to underlying TableModel
			selection[i] = table.convertRowIndexToModel(selection[i]);
		for (int row : selection)
			notes.add(new MidiPair( ((Time) table.getModel().getValueAt(row, TICK)).e(), null));
		Edit e = new Edit(Type.DEL, notes);
		getMusician(track).push(e);
		for (int row : selection)
			model.removeRow(row);
	}

	private void copy() {
		clipboard.clear();
		int[] selection = table.getSelectedRows();
		for (int i = 0; i < selection.length; i++) // convert selection to underlying TableModel
			selection[i] = table.convertRowIndexToModel(selection[i]);
		for (int row : selection)
			clipboard.add((Time)table.getModel().getValueAt(row, TICK));
		paste.setEnabled(clipboard.size() > 0);
	}

	private void paste() {
		// offset/absolute
		// [     Tick     ]
		// [paste] [cancel]
	}

	@Override protected void pad1() {
		edit();
	}

	@Override protected void pad2() {
		delete();
	}

	@Override protected boolean doKnob(int idx, int value) {
		// scroll msgs
		return false;
	}

}
