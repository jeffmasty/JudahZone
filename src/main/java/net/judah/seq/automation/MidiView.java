package net.judah.seq.automation;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import judahzone.api.Midi;
import judahzone.gui.Gui;
import judahzone.widgets.Btn;
import net.judah.gui.Size;
import net.judah.midi.JudahMidi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiNote;
import net.judah.seq.MidiTools;
import net.judah.seq.automation.Automation.AutoBox;
import net.judah.seq.track.MidiTrack;

public class MidiView extends AutoBox {
	private static final Dimension SZ = new Dimension(Size.WIDTH_KNOBS - 40, Size.HEIGHT_KNOBS - 100);

	private final Automation automation;
	private ArrayList<Integer> clipboard = new ArrayList<Integer>();
	private final JTable table = new JTable();
	private MidiModel model;
	private final JButton edit = new Btn("Edit", l->edit()); // pad1/doubleClick
	private final JButton delete = new Btn("Delete", l->delete()); // pad2
	private final JButton exe = new Btn("Exe", l->exe());
	private final JButton copy = new Btn("Copy", l->copy());
	private final JButton paste = new Btn("Paste", l->paste());
	private final JButton[] btns = {edit, delete, exe, copy, paste};

	MidiView(Automation auto) {
	    super(BoxLayout.PAGE_AXIS);
	    this.automation = auto;

	    table.setRowSelectionAllowed(true);
	    table.setColumnSelectionAllowed(false);
	    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    table.setAutoCreateRowSorter(true);
	    table.getSelectionModel().addListSelectionListener(l->selected());
	    table.addMouseListener(new MouseAdapter() {
	        @Override public void mouseClicked(MouseEvent e) {
	            if (e.getClickCount() == 2)
	                edit();
	        }});
	    JScrollPane scroll = new JScrollPane(table);
	    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

	    for (JButton btn : btns)
	        btn.setEnabled(false);

	    add(Gui.box(edit, delete, copy, paste));
	    add(Gui.resize(scroll, SZ));
	    add(Box.createVerticalStrut(10));
	    add(Box.createVerticalGlue());

	}

	private void selected() {
	    boolean ok = table.getSelectedRowCount() > 0;
	    for (JButton btn : btns)
	        btn.setEnabled(ok);
	    paste.setEnabled(!clipboard.isEmpty());
	}

	@Override protected void setTrack(MidiTrack t) {
	    track = t;
	    model = new MidiModel(track, table);
	}

	@Override protected AutoBox init(long tick) {
	    int idx = MidiTools.find(track.getT(), tick);
	    if (idx > 0 && idx < table.getRowCount())
	        table.setRowSelectionInterval(idx, idx);
	    return this;
	}

	/** Helper: get a MidiEvent for a model value; prefer existing MidiEvent, else build from MidiNote. */
	private MidiEvent toEvent(Object evtObj) {
	    if (evtObj == null) return null;
	    if (evtObj instanceof MidiEvent me) return me;
	    if (evtObj instanceof MidiNote mn) {
	        // construct a MidiEvent from the MidiNote's message and tick
	        try {
	            return new MidiEvent(mn.getMessage(), mn.getTick());
	        } catch (Throwable t) {
	            return null;
	        }
	    }
	    return null;
	}

	/** edit first selected */
	private void edit() {
	    int row = table.getSelectedRow();
	    if (row < 0)
	        return;
	    row = table.convertRowIndexToModel(row);
	    Object val = model.getValueAt(row, MidiModel.EVENT);
	    MidiEvent evt = toEvent(val);
	    if (evt == null)
	        return;
	    MidiMessage m = evt.getMessage();
	    if (Midi.isCC(m) || Midi.isProgChange(m) || Midi.isPitchBend(m))
	        automation.edit(track, evt);
	}

	private void  exe() {
	    int[] selection = table.getSelectedRows();
	    for (int i = 0; i < selection.length; i++) // convert selection to underlying TableModel
	        selection[i] = table.convertRowIndexToModel(selection[i]);
	    for (int row : selection) {
	        Object val = table.getModel().getValueAt(row, MidiModel.EVENT);
	        MidiEvent evt = toEvent(val);
	        if (evt == null)
	            return;
	        MidiMessage m = evt.getMessage();
	        if (Midi.isCC(m) || Midi.isProgChange(m) || Midi.isPitchBend(m))
	            track.send(m, JudahMidi.ticker());
	    }
	}

	/** delete all selected */
	private void delete() {
	    ArrayList<MidiEvent> events = new ArrayList<MidiEvent>();
	    int[] selection = table.getSelectedRows();
	    for (int i = 0; i < selection.length; i++) // convert selection to underlying TableModel
	        selection[i] = table.convertRowIndexToModel(selection[i]);
	    for (int row : selection) {
	        Object val = table.getModel().getValueAt(row, MidiModel.EVENT);
	        MidiEvent evt = toEvent(val);
	        if (evt != null)
	            events.add(evt);
	    }
	    if (!events.isEmpty()) {
	        Edit e = new Edit(Type.DEL, events);
	        track.getEditor().push(e);
	    }
	}

	private void copy() {
	    clipboard.clear();
	    int[] selection = table.getSelectedRows();
	    for (int i = 0; i < selection.length; i++) // convert selection to underlying TableModel
	        clipboard.add(table.convertRowIndexToModel(selection[i]));
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

	public void refill() {
	    // TODO Auto-generated method stub
	}
}