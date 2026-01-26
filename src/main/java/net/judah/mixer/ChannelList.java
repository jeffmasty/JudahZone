package net.judah.mixer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import judahzone.api.Custom;
import judahzone.gui.DialogManager;
import judahzone.gui.Icons;
import net.judah.channel.LineIn;

/**CRUD table for user channel registrations. Binds directly to Channels */
public class ChannelList extends JPanel implements Channels.MixBus {
	private static final Dimension DEFAULT_DIALOG_SIZE = new Dimension(780, 485);
	// columns
	private static final int ICON = 0;
	private static final int NAME = 1;
	private static final int ONMIXER = 2;
	private static final int STEREO = 3;
	private static final int MIDI = 4;
	private static final int PREAMP = 5;
	private static final int ACTIONS = 6;

	private final Channels channels;
	private final CustomTableModel model;
	private final JTable table;
	private final JFrame frame;
	private JCheckBox autosave;

	public static void open(Channels channels) {
	    final String key = "ChannelList:" + System.identityHashCode(channels);
	    DialogManager.open(key, () -> {
	        ChannelList cl = new ChannelList(channels);
	        return cl.frame; // static method in same class can access private instance field
	    });
	}

	private ChannelList(Channels channels) {
	    this.channels = channels;
	    setName("Channels");
	    setLayout(new BorderLayout(6, 6));

	    // Backing list is the live list from Registry
	    model = new CustomTableModel(getLoaded());
	    table = new JTable(model);
	    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	    // Use icon thumb height as row height
	    table.setRowHeight(Icons.THUMB.height);
	    table.setIntercellSpacing(new Dimension(8, 6));

	    // Replace last column with real buttons
	    final int actionsCol = model.getColumnCount() - 1; // last column
	    table.getColumnModel().getColumn(actionsCol).setCellRenderer(new ActionCellRenderer());
	    table.getColumnModel().getColumn(actionsCol).setCellEditor(new ActionCellEditor());

	    // Preamp editor/renderer
	    table.getColumnModel().getColumn(PREAMP).setCellRenderer(new Preamp.Renderer());
	    // pass autosave behavior into editor; only trigger save once when editing stops
	    table.getColumnModel().getColumn(PREAMP).setCellEditor(new Preamp.Editor(model, () -> {
	        if (autosave.isSelected()) {
	            try {
	            	// make sure the preamp change is in Custom before saving.
	                channels.save();
	            } catch (IOException e) {
	                JOptionPane.showMessageDialog(ChannelList.this, "Auto-save failed: " + e.getMessage());
	            }
	        }
	    }));

	    // Icon column at index 0: fixed width from Icons.THUMB
	    if (table.getColumnModel().getColumnCount() >= 1) {
	        table.getColumnModel().getColumn(ICON).setPreferredWidth(Icons.THUMB.width); // Icon
	        table.getColumnModel().getColumn(ICON).setMinWidth(Icons.THUMB.width);
	        table.getColumnModel().getColumn(ICON).setMaxWidth(Icons.THUMB.width + 10);
	    }
	    // Name column: target ~16 chars but flexible
	    if (table.getColumnModel().getColumnCount() > NAME) {
	        int approxCharWidth = 9; // conservative per-char px
	        int preferred = Math.max(120, 16 * approxCharWidth); // ~144
	        table.getColumnModel().getColumn(NAME).setPreferredWidth(preferred);
	        table.getColumnModel().getColumn(NAME).setMinWidth(80);
	        table.getColumnModel().getColumn(NAME).setMaxWidth(400);
	    }

	    // Show / Stereo / Midi columns: allow giving up space (preferred small, resizable)
	    if (table.getColumnModel().getColumnCount() > ONMIXER) {
	        int smallPref = 70;
	        table.getColumnModel().getColumn(ONMIXER).setPreferredWidth(smallPref);
	        table.getColumnModel().getColumn(ONMIXER).setMinWidth(40);
	        table.getColumnModel().getColumn(ONMIXER).setMaxWidth(120);
	    }
	    if (table.getColumnModel().getColumnCount() > STEREO) {
	        int smallPref = 70;
	        table.getColumnModel().getColumn(STEREO).setPreferredWidth(smallPref);
	        table.getColumnModel().getColumn(STEREO).setMinWidth(40);
	        table.getColumnModel().getColumn(STEREO).setMaxWidth(120);
	    }
	    if (table.getColumnModel().getColumnCount() > MIDI) {
	        int smallPref = 70;
	        table.getColumnModel().getColumn(MIDI).setPreferredWidth(smallPref);
	        table.getColumnModel().getColumn(MIDI).setMinWidth(40);
	        table.getColumnModel().getColumn(MIDI).setMaxWidth(120);
	    }

	    // Preamp column: give decent space for widget
	    if (table.getColumnModel().getColumnCount() > PREAMP) {
	        table.getColumnModel().getColumn(PREAMP).setPreferredWidth(220); // wider by default
	        table.getColumnModel().getColumn(PREAMP).setMinWidth(140);
	        table.getColumnModel().getColumn(PREAMP).setMaxWidth(400);
	    }

	    // Actions column: ensure both buttons fit on one row
	    if (table.getColumnModel().getColumnCount() > actionsCol) {
	        table.getColumnModel().getColumn(actionsCol).setPreferredWidth(120);
	        table.getColumnModel().getColumn(actionsCol).setMinWidth(100);
	        table.getColumnModel().getColumn(actionsCol).setMaxWidth(200);
	    }

	    // Ensure a single click on the actions cell activates the editor and delivers
	    // the click
	    table.addMouseListener(new MouseAdapter() {
	        @Override public void mousePressed(MouseEvent e) {
	            int col = table.columnAtPoint(e.getPoint());
	            int row = table.rowAtPoint(e.getPoint());
	            if (col == actionsCol && row >= 0) {
	                if (!table.isRowSelected(row))
	                    table.getSelectionModel().setSelectionInterval(row, row);
	                if (!table.editCellAt(row, col))
	                    return;
	                // forward the mouse event into the editor component so the button receives it
	                Component editor = table.getEditorComponent();
	                if (editor != null) {
	                    editor.requestFocusInWindow();
	                    Point p = SwingUtilities.convertPoint(table, e.getPoint(), editor);
	                    editor.dispatchEvent(new MouseEvent(editor, e.getID(), e.getWhen(), e.getModifiersEx(), p.x,
	                            p.y, e.getClickCount(), e.isPopupTrigger(), e.getButton()));
	                }
	            }
	        }

	        @Override public void mouseClicked(MouseEvent e) {
	            // Double-click row to edit, but ignore actions and Show (ONMIXER) columns
	            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
	                int col = table.columnAtPoint(e.getPoint());
	                int row = table.rowAtPoint(e.getPoint());
	                if (row < 0 || col < 0)
	                    return;
	                if (col == actionsCol || col == ONMIXER)
	                    return;
	                if (!table.isRowSelected(row))
	                    table.getSelectionModel().setSelectionInterval(row, row);
	                performEdit(row);
	            }
	        }
	    });

	    // keep cursor change when hovering actions column
	    table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
	        @Override
	        public void mouseMoved(java.awt.event.MouseEvent e) {
	            int col = table.columnAtPoint(e.getPoint());
	            if (col == actionsCol)
	                table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	            else
	                table.setCursor(Cursor.getDefaultCursor());
	        }
	    });

	    add(new JScrollPane(table), BorderLayout.CENTER);
	    add(buttonPanel(), BorderLayout.SOUTH);

	    frame = DialogManager.show(this, DEFAULT_DIALOG_SIZE);

	    // subscribe to channel lifecycle events and ensure we unsubscribe when the
	    // window closes
	    channels.subscribe(this);
	    if (frame != null) {
	        frame.addWindowListener(new WindowAdapter() {
	            @Override
	            public void windowClosed(WindowEvent e) {
	                channels.unsubscribe(ChannelList.this);
	            }
	        });
	    }
	}


	private List<Custom> getLoaded() {
	    List<LineIn> users = channels.getUserChannels();
	    List<Custom> readOnly = new ArrayList<>(users.size());
	    for (int i = 0; i < users.size(); i++) {
	        LineIn li = users.get(i);
	        if (li == null)
	            throw new IllegalStateException("Null LineIn in channels.getUserChannels() at index " + i);
	        Custom user = li.getUser();
	        if (user == null)
	            throw new IllegalStateException(li.getName() + "Null LineIn.getUser() at index " + i);
	        readOnly.add(user);
	    }
	    return readOnly;
	}

	private JPanel buttonPanel() {
	    // Use BorderLayout: left = autosave + conditional Save, right = Add/Close
	    JPanel p = new JPanel(new BorderLayout());
	    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
	    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));

	    JButton add = new JButton("Add");
	    JButton close = new JButton("Close");
	    // Save button is only visible when autosave is OFF
	    JButton save = new JButton("Save");

	    // autosave checkbox (field)
	    // initialize checkbox instance
	    // note: setSelected(true) below in constructor ensures default ON
	    autosave = new JCheckBox("Autosave");
	    add.addActionListener(e -> onAdd());
	    close.addActionListener(e -> frame.dispose());
	    save.addActionListener(e -> onSave());
	    // Autosave toggles Save button visibility
	    autosave.setSelected(true);  // TODO debug
	    save.setVisible(!autosave.isSelected());
	    autosave.addActionListener(e -> save.setVisible(!autosave.isSelected()));

	    // Left: Save (conditionally visible) then Autosave checkbox
	    left.add(save);
	    left.add(Box.createHorizontalStrut(6));
	    left.add(autosave);

	    // Right: Add and Close
	    right.add(add);
	    right.add(close);

	    p.add(left, BorderLayout.WEST);
	    p.add(right, BorderLayout.EAST);
	    return p;
	}

	private void onSave() {
	    try {
	        channels.save();
	        JOptionPane.showMessageDialog(this, "Saved");
	    } catch (IOException e) {
	        JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage());
	    }
	}

	/* Table model for Custom records */
	public class CustomTableModel extends AbstractTableModel {
	    final List<Custom> rows;
	    final String[] cols = { "Icon", "Name", "Show", "Stereo", "Midi", "Preamp", "Actions" };

	    CustomTableModel(List<Custom> rows) { this.rows = rows; }
	    Custom get(int r) { return rows.get(r); }
	    @Override public int getRowCount() { return rows.size(); }
	    @Override public int getColumnCount() { return cols.length; }
	    @Override public String getColumnName(int c) { return cols[c]; }
	    @Override public Object getValueAt(int r, int c) {
	        Custom it = rows.get(r);
	        return switch (c) {
	        case ICON -> {
	            String iconName = it.iconName();
	            yield iconName == null ? Icons.get("left.png") : Icons.get(iconName);
	        }
	        case NAME -> it.name();
	        case ONMIXER -> it.onMixer(); // boolean -> checkbox
	        case STEREO -> it.stereo() ? "Yes" : "No"; // render as Yes/No
	        case MIDI -> {
	            String midi = it.midiPort();
	            yield (midi != null && !"none".equalsIgnoreCase(midi) && !midi.isBlank()) ? "Yes" : "No";
	        }
	        case PREAMP -> it.preamp(); // Float or null
	        case ACTIONS -> "actions";
	        default -> null;
	        };
	    }

	    @Override public Class<?> getColumnClass(int columnIndex) {
	        return switch (columnIndex) {
	        case ICON -> Icon.class;
	        case ONMIXER -> Boolean.class; // editable checkbox for Show
	        case PREAMP -> Float.class; // preamp value
	        case STEREO, MIDI -> String.class; // render Yes/No or value
	        default -> String.class;
	        };
	    }

	    @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
	        // Actions (last), Show (ONMIXER), and Preamp columns editable
	        return columnIndex == ACTIONS || columnIndex == ONMIXER || columnIndex == PREAMP;
	    }

	    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	        if (rowIndex < 0 || rowIndex >= rows.size())
	            return;
	        Custom old = rows.get(rowIndex);
	        if (columnIndex == ONMIXER) { // Show toggled
	            boolean onMixer = Boolean.TRUE.equals(aValue);
	            int low = old.lowCutHz() == null ? 85 : old.lowCutHz();
	            int high = old.highCutHz() == null ? 12000 : old.highCutHz();
	            Custom updated = new Custom(old.name(), old.stereo(), onMixer, old.leftPort(), old.rightPort(),
	                    old.midiPort(), old.iconName(), low, high, old.engine(), old.clocked(), old.preamp());
	            rows.set(rowIndex, updated);
	            fireTableRowsUpdated(rowIndex, rowIndex);

	            // Persist immediately if autosave enabled
	            if (autosave.isSelected()) {
	                try {
	                    channels.save();
	                } catch (IOException e) {
	                    JOptionPane.showMessageDialog(ChannelList.this, "Auto-save failed: " + e.getMessage());
	                }
	            }
	        } else if (columnIndex == PREAMP) {
	            Float p = (aValue instanceof Float) ? (Float) aValue : null;
	            Custom updated = new Custom(old.name(), old.stereo(), old.onMixer(), old.leftPort(), old.rightPort(),
	                    old.midiPort(), old.iconName(), old.lowCutHz(), old.highCutHz(), old.engine(), old.clocked(),
	                    p);
	            rows.set(rowIndex, updated);
	            fireTableRowsUpdated(rowIndex, rowIndex);
	            if (autosave.isSelected()) {
	                try {
	                    channels.save();
	                } catch (IOException e) {
	                    JOptionPane.showMessageDialog(ChannelList.this, "Auto-save failed: " + e.getMessage());
	                }
	            }
	        }
	    }
	}

	/* Selection + convenience helpers for external controllers */
	public void addTableSelectionListener(javax.swing.event.ListSelectionListener l) {
	    table.getSelectionModel().addListSelectionListener(l);
	}

	public int getSelectedIndex() {
	    return table.getSelectedRow();
	}

	public Custom getSelectedCustom() {
	    int idx = getSelectedIndex();
	    return idx >= 0 ? model.get(idx) : null;
	}

	public void refresh() {
	    model.rows.clear();
	    model.rows.addAll(getLoaded());
	    model.fireTableDataChanged();
	}

	public void updateRow(int idx) {
	    if (idx >= 0 && idx < model.rows.size()) {
	        LineIn line = channels.getUserChannels().get(idx);
	        model.rows.set(idx, line.getUser());
	        model.fireTableRowsUpdated(idx, idx);
	    }
	}

	/* Renderer: shows two buttons */
	private class ActionCellRenderer extends JPanel implements TableCellRenderer {
	    private final JButton edit = new JButton("Edit");
	    private final JButton del = new JButton("Delete");

	    ActionCellRenderer() {
	        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
	        // renderer buttons are not interactive; they just show visuals
	        edit.setFocusable(false);
	        del.setFocusable(false);
	        add(edit);
	        add(del);
	        setOpaque(true);
	    }

	    @Override public Component getTableCellRendererComponent(JTable table, Object value,
	    		boolean isSelected, boolean hasFocus, int row, int column) {
	        if (isSelected)
	            setBackground(table.getSelectionBackground());
	        else
	            setBackground(table.getBackground());
	        return this;
	    }
	}

	/* Editor: actual clickable buttons wired to controller or inline handlers */
	private class ActionCellEditor extends AbstractCellEditor implements TableCellEditor {
	    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
	    private final JButton editBtn = new JButton("Edit");
	    private final JButton delBtn = new JButton("Delete");
	    private int editingRow = -1;

	    ActionCellEditor() {
	        panel.add(editBtn);
	        panel.add(delBtn);
	        editBtn.addActionListener(e -> {
	            fireEditingStopped();
	            performEdit(editingRow);
	        });
	        delBtn.addActionListener(e -> {
	            fireEditingStopped();
	            performDelete(editingRow);
	        });
	    }

	    @Override public Component getTableCellEditorComponent(JTable table, Object value,
	    		boolean isSelected, int row, int column) {
	        this.editingRow = row;
	        return panel;
	    }

	    @Override public Object getCellEditorValue() {
	        return null;
	    }
	}

	/* edit = delete original, add updated */
    private Custom original;
	private void performEdit(int row) {
	    if (row < 0 || row >= model.rows.size())
	        return;
	    original = model.get(row);
	    AddChannel.open(channels, updated-> backFromEdit(updated), original);
	}

	public void backFromEdit(Custom updated) {
		// we have an original, what has changed?
		// if any port or name changed then regen as below, else new data() method
		if (regen(original, updated)) {
	        channels.remove(original);
	        channels.createChannel(updated, autosave.isSelected());
		} else
			channels.swap(original, updated, autosave.isSelected());
	}

	private boolean regen(Custom original, Custom updated) {
		/*Edit types of Custom fields:
regen    String name,
regen    boolean stereo,
live    boolean onMixer,
regen    String leftPort,
regen    String rightPort,
regen    String midiPort,
live    String iconName,
live    Integer lowCutHz,
live    Integer highCutHz,
regen	String engine, // internal TacoTrucks? FluidSynthEngine?
regen	boolean clocked, // TODO
live	Float preamp // null: goto to Gain defaults */

		return true;

	}

	private void performDelete(int row) {
	    if (row < 0 || row >= model.rows.size())
	        return;
	    int confirm = JOptionPane.showConfirmDialog(this, "Remove selected channel?");
	    if (confirm != JOptionPane.YES_OPTION)
	        return;

	    // remove the Custom associated with the given row (use model to avoid relying
	    // on table selection)
	    Custom target = model.get(row);
	    channels.remove(target);

	    // Auto-save if enabled
	    if (autosave.isSelected()) {
	        try {
	            channels.save();
	        } catch (IOException e) {
	            JOptionPane.showMessageDialog(this, "Auto-save failed: " + e.getMessage());
	        }
	    }

	    // reselect an adjacent row if possible
	    int newSel = Math.min(row, model.rows.size() - 1);
	    if (newSel >= 0)
	        table.getSelectionModel().setSelectionInterval(newSel, newSel);
	}

	// Create AddChannel panel with embedded callback
	private void onAdd() {
	    AddChannel.open(channels, updated -> {
	        channels.createChannel(updated, autosave.isSelected());
	        if (autosave.isSelected()) {
	            try {
	                channels.save();
	            } catch (IOException e) {
	                JOptionPane.showMessageDialog(ChannelList.this, "Auto-save failed: " + e.getMessage());
	            }
	        }
	    });
	}

	/* Channels.MixBus callbacks — update UI on EDT and preserve selection. */
	@Override public void reordered() {
		SwingUtilities.invokeLater(() -> {
			Custom old = getSelectedCustom();
			refresh();
			// re-select same user: only order changed
			for (int i = 0; i < model.rows.size(); i++) {
			Custom candidate = model.rows.get(i);
				if (old.equals(candidate)) {
					table.getSelectionModel().setSelectionInterval(i, i);
					break;
				}
			}
		});
	}

	@Override public void channelAdded(LineIn ch) {
	    if (ch == null || ch.getUser() == null)
	        return;
	    SwingUtilities.invokeLater(() -> {
	        // refresh then select the newly added user if present
	        refresh();
	        Custom user = ch.getUser();
	        int idx = -1;
	        for (int i = 0; i < model.rows.size(); i++)
	            if (user.equals(model.rows.get(i))) {
	                idx = i;
	                break;
	            }
	        if (idx >= 0)
	            table.getSelectionModel().setSelectionInterval(idx, idx);
	    });
	}

	@Override public void channelRemoved(LineIn ch) {
	    if (ch == null || ch.getUser() == null)
	        return;
	    // find previous index so we can pick an adjacent row after refresh
	    int prevIdx = -1;
	    for (int i = 0; i < model.rows.size(); i++)
	        if (ch.getUser().equals(model.rows.get(i))) {
	            prevIdx = i;
	            break;
	        }

	    final int preserveIdx = prevIdx;
	    SwingUtilities.invokeLater(() -> {
	        refresh();
	        int newSel = Math.min(preserveIdx, Math.max(0, model.rows.size() - 1));
	        if (newSel >= 0 && !model.rows.isEmpty())
	            table.getSelectionModel().setSelectionInterval(newSel, newSel);
	        else
	            table.clearSelection();
	    });
	}


	// ch might be on table, ch might have changed it's icon, visibility or preamp parameter.
	@Override public void update(LineIn ch) {
		if (ch == null || ch.getUser() == null)
			return;
		SwingUtilities.invokeLater(() -> {
			Custom updated = ch.getUser();
			for (int i = 0; i < model.rows.size(); i++) {
				if (updated.equals(model.rows.get(i))) {
					model.rows.set(i, updated);
					model.fireTableRowsUpdated(i, i);
					return;
				}
			}
		});
	}


}

