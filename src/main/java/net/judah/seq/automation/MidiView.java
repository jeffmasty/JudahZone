package net.judah.seq.automation;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.gui.Gui;
import judahzone.widgets.Btn;
import net.judah.gui.Size;
import net.judah.midi.JudahMidi;
import net.judah.seq.automation.Automation.AutoBox;
import net.judah.seq.track.Edit;
import net.judah.seq.track.MidiTools;
import net.judah.seq.track.Edit.Type;
import net.judah.seq.track.Editor.Selection;

public class MidiView extends AutoBox {
	private static final Dimension SZ = new Dimension(Size.WIDTH_KNOBS - 40, Size.HEIGHT_KNOBS - 100);

	private final Automation automation;
	private final JTable table = new JTable();
	private MidiModel model;

	private final JButton edit = new Btn("Edit", l->edit()); // pad1/doubleClick
	private final JButton delete = new Btn("Delete", l->delete()); // pad2
	private final JButton exe = new Btn("Exe", l->exe());
	private final JButton copy = new Btn("Copy", l->copy());

	private final JButton[] btns = {edit, delete, exe, copy};
	private boolean isUpdatingSelection = false;

	MidiView(Automation auto) {
	    super(BoxLayout.PAGE_AXIS, auto.getTrack());
	    this.automation = auto;

	    table.setRowSelectionAllowed(true);
	    table.setColumnSelectionAllowed(false);
	    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    table.setAutoCreateRowSorter(true);
	    table.getSelectionModel().addListSelectionListener(l -> {
	        if (!l.getValueIsAdjusting()) {
	            selected();
	        }
	    });
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

	    add(Gui.box(edit, delete, copy, exe));
	    add(Gui.resize(scroll, SZ));
	    add(Box.createVerticalStrut(10));
	    add(Box.createVerticalGlue());

	    model = new MidiModel(track, table);
		selected();

	}

	public void refill() { // TODO crude
	    model = new MidiModel(track, table);
		selected();
	}

	private void selected() {
		if (isUpdatingSelection) return;

	    boolean hasSelection = table.getSelectedRowCount() > 0;
	    for (JButton btn : btns) {
	        btn.setEnabled(hasSelection);
	    }
		if (track != null) {
			track.getEditor().publish(this, getSelectedEvents());
		}
	}

	@Override protected AutoBox init(long tick) {
	    int modelIdx = MidiTools.find(track.getT(), tick);
	    if (modelIdx > 0 && modelIdx < table.getRowCount()) {
			int viewIdx = table.convertRowIndexToView(modelIdx);
			if (viewIdx >= 0) {
				table.setRowSelectionInterval(viewIdx, viewIdx);
				table.scrollRectToVisible(table.getCellRect(viewIdx, 0, true));
			}
		}
	    return this;
	}

	private List<MidiEvent> getSelectedEvents() {
		if (model == null) return Collections.emptyList();

		ArrayList<MidiEvent> events = new ArrayList<>();
		int[] selection = table.getSelectedRows();
		for (int viewRow : selection) {
			int modelRow = table.convertRowIndexToModel(viewRow);
			MidiEvent evt = model.getEventAt(modelRow);
			if (evt != null) {
				events.add(evt);
			}
		}
		return events;
	}

	/** edit first selected */
	private void edit() {
	    List<MidiEvent> selectedEvents = getSelectedEvents();
	    if (selectedEvents.isEmpty()) return;

	    MidiEvent evt = selectedEvents.getFirst();
	    MidiMessage m = evt.getMessage();
	    if (Midi.isCC(m) || Midi.isProgChange(m) || Midi.isPitchBend(m) || Midi.isNote(m))
	        automation.edit(evt);
	}

	private void  exe() {
	    for (MidiEvent evt : getSelectedEvents()) {
	        MidiMessage m = evt.getMessage();
	        if (Midi.isCC(m) || Midi.isProgChange(m) || Midi.isPitchBend(m))
	            track.send(m, JudahMidi.ticker());
	    }
	}

	/** delete all selected */
	private void delete() {
		List<MidiEvent> events = getSelectedEvents();
	    if (!events.isEmpty()) {
	        Edit e = new Edit(Type.DEL, new ArrayList<>(events));
	        track.getEditor().push(e);
	    }
	}

	private void copy() {
		if (track != null) {
			track.getEditor().copy();
		}
	}

	@Override protected void pad1() { edit(); }
	@Override protected void pad2() { delete(); }
	@Override protected boolean doKnob(int idx, int value) { return false; }

//	@Override
//	protected void updateSelection(Selection selection) {
//		if (selection.originId() == this) {
//			return; // Do not react to own events
//		}
//
//		SwingUtilities.invokeLater(() -> {
//			isUpdatingSelection = true;
//			try {
//				table.clearSelection();
//				if (model == null) return;
//
//				List<MidiEvent> events = selection.events();
//				if (events.isEmpty()) return;
//
//				ListSelectionModel selModel = table.getSelectionModel();
//				for (MidiEvent event : events) {
//					int modelRow = model.getRowForEvent(event);
//					if (modelRow != -1) {
//						int viewRow = table.convertRowIndexToView(modelRow);
//						if (viewRow != -1) {
//							selModel.addSelectionInterval(viewRow, viewRow);
//						}
//					}
//				}
//				// Scroll to the first selected item
//				if (table.getSelectedRowCount() > 0) {
//					int firstSelected = table.getSelectedRows()[0];
//					table.scrollRectToVisible(table.getCellRect(firstSelected, 0, true));
//				}
//			} finally {
//				isUpdatingSelection = false;
//				// Manually update button state after programmatic selection
//				boolean hasSelection = table.getSelectedRowCount() > 0;
//				for (JButton btn : btns) {
//					btn.setEnabled(hasSelection);
//				}
//			}
//		});
//	}
	@Override
	protected void updateSelection(Selection selection) {
	    if (selection.originId() == Automation.ORIGIN_ID) {
	        return; // Do not react to own events
	    }

	    SwingUtilities.invokeLater(() -> {
	        isUpdatingSelection = true;
	        try {
	            table.clearSelection();
	            if (model == null) return;

	            List<MidiEvent> events = selection.events();
	            if (events.isEmpty()) return;

	            ListSelectionModel selModel = table.getSelectionModel();
	            for (MidiEvent event : events) {
	                // Find the row for this event (note-on for piano, any event for drums)
	                int modelRow = model.getRowForEvent(event);
	                if (modelRow != -1) {
	                    int viewRow = table.convertRowIndexToView(modelRow);
	                    if (viewRow != -1) {
	                        selModel.addSelectionInterval(viewRow, viewRow);
	                    }
	                }
	            }

	            // Scroll to the first selected item
	            if (table.getSelectedRowCount() > 0) {
	                int firstSelected = table.getSelectedRows()[0];
	                table.scrollRectToVisible(table.getCellRect(firstSelected, 0, true));
	            }
	        } finally {
	            isUpdatingSelection = false;
	            // Manually update button state after programmatic selection
	            boolean hasSelection = table.getSelectedRowCount() > 0;
	            for (JButton btn : btns) {
	                btn.setEnabled(hasSelection);
	            }
	        }
	    });
	}

}