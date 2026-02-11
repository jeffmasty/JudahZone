// java
package net.judah.mixer;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import judahzone.gui.Gui;
import judahzone.util.Constants;
import net.judah.gui.Size;
import net.judah.mixer.ChannelList.CustomTableModel;

/**
 * Preamp UI component with logarithmic mapping between a slider (1..100) and a
 * preamp range (MIN..MAX). Exposes get/set for integration with models and
 * table cell editors/renderers.
 */
public class Preamp extends JPanel {

	private static final float MIN = 0.1f;
	private static final float MAX = 5f;

	private final JSlider slider;
	private final JLabel label;

	public Preamp() {
		label = new JLabel("");
		// Use 1..100 to match Constants' logarithmic helper which expects 1..100
		slider = new JSlider(1, 100);
		slider.setToolTipText("Preamp Level: " + MIN + " to " + MAX);
		Gui.resize(slider, Size.WIDE_SIZE);

		// initialize slider to represent 1.0f preamp
		int initPercent = Constants.reverseLog(1.0f, MIN, MAX);
		slider.setValue(initPercent);

		updateLabelFromSlider();

		slider.addChangeListener(e -> updateLabelFromSlider());

		this.add(slider);
		this.add(label);
	}

	private void updateLabelFromSlider() {
		int value = slider.getValue();
		float mapped = Constants.logarithmic(value, MIN, MAX);
		label.setText(String.format("%.2f X", mapped));
	}

	/** Return the current preamp multiplier (MIN..MAX). */
	public float getPreamp() {
		return Constants.logarithmic(slider.getValue(), MIN, MAX);
	}

	/** Set preamp multiplier; slider will be moved to the corresponding percent. */
	public void setPreamp(float preamp) {
		int pct = Constants.reverseLog(preamp, MIN, MAX);
		// ensure pct in slider range
		if (pct < slider.getMinimum())
			pct = slider.getMinimum();
		if (pct > slider.getMaximum())
			pct = slider.getMaximum();
		slider.setValue(pct);
		updateLabelFromSlider();
	}

	/**
	 * Allow external listeners (e.g., table cell editor) to react to UI changes.
	 */
	public void addChangeListener(ChangeListener l) {
		slider.addChangeListener(l);
	}

	/*
	 * Replace the existing PreampCellRenderer in ChannelList.java with this
	 * implementation. Renders an always-visible preamp slider per row (disabled for
	 * non-editing state).
	 */
	public static class Renderer implements TableCellRenderer {
		private final Preamp preampComp = new Preamp();

		Renderer() {
			// renderer should not be interactive; show the slider for visual feedback only
			preampComp.setEnabled(false);
			preampComp.setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (isSelected)
				preampComp.setBackground(table.getSelectionBackground());
			else
				preampComp.setBackground(table.getBackground());

			Float p = value instanceof Float ? (Float) value : null;
			preampComp.setPreamp(p == null ? 1f : p);

			// keep it non-interactive in renderer; user can click to edit (cell editor)
			preampComp.setEnabled(false);
			return preampComp;
		}
	}

	/*
	 * Editor: editable Preamp component embedded in the cell Now defers updating
	 * the table/model until editing stops to avoid noisy autosaves.
	 */
	public static class Editor extends AbstractCellEditor implements TableCellEditor {
		private final Preamp preampComp = new Preamp();
		private int editingRow = -1;
		private boolean internalChange = false;
		private boolean dirty = false;
		private Float lastValue = null;
		private final CustomTableModel model;
		private final Runnable onStop; // optional callback (e.g., trigger autosave)

		/** Construct editor with model and optional onStop callback. */
		Editor(CustomTableModel model) {
			this(model, null);
		}

		Editor(CustomTableModel model, Runnable onStop) {
			this.model = model;
			this.onStop = onStop;

			// mark dirty and stash latest value on slider moves; do NOT update model here.
			preampComp.addChangeListener(e -> {
				if (editingRow < 0 || internalChange)
					return;
				dirty = true;
				lastValue = preampComp.getPreamp();
			});
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			this.editingRow = row;
			Float p = value instanceof Float ? (Float) value : null;
			internalChange = true;
			preampComp.setPreamp(p == null ? 1f : p);
			internalChange = false;
			dirty = false;
			lastValue = null;
			return preampComp;
		}

		@Override
		public Object getCellEditorValue() {
			return preampComp.getPreamp();
		}

		/** When editing stops, apply the final value once and run onStop callback. */
		@Override
		public boolean stopCellEditing() {
			if (editingRow >= 0 && dirty) {
				Float newVal = lastValue == null ? preampComp.getPreamp() : lastValue;
				// find Preamp column index by name to avoid hard-coded constants
				int col = -1;
				for (int i = 0; i < model.getColumnCount(); i++)
					if ("Preamp".equalsIgnoreCase(model.getColumnName(i))) {
						col = i;
						break;
					}
				if (col == -1)
					col = 5; // fallback consistent with previous layout

				model.setValueAt(newVal, editingRow, col);
				if (onStop != null)
					onStop.run();
				dirty = false;
			}
			editingRow = -1;
			return super.stopCellEditing();
		}
	}

}