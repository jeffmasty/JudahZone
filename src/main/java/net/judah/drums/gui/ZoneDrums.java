package net.judah.drums.gui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.drums.DrumType;
import net.judah.drums.KitDB.BaseParam;
import net.judah.drums.synth.DrumParams;
import net.judah.drums.synth.DrumParams.DrumParam;
import net.judah.drums.synth.DrumParams.FilterParam;
import net.judah.drums.synth.DrumSynth;
import net.judah.midi.Actives;

/** GUI for 8 DrumOsc pads with parameter tabs and mode switching. */
public class ZoneDrums extends DrumKnobs {

	@Getter private final DrumSynth kit;
	private final List<DrumOscPad> pads = new ArrayList<>(8);
	private final JComboBox<BaseParam> baseParams = new JComboBox<>(BaseParam.values());
	private final JComboBox<FilterParam> filterParams = new JComboBox<>(FilterParam.values());
	private final JComboBox<DrumParam> customParams = new JComboBox<>(DrumParam.values());
	private final List<JComboBox<?>> indexed;
	private final List<TabButton> tabs = new ArrayList<>(3);
	private final ButtonGroup tabBtns = new ButtonGroup();
	private final JPanel header = new JPanel();
	private final JPanel main = new JPanel(new GridLayout(2, 4, 1, 1));

	private TabButton activeTab;

	private class TabButton extends JToggleButton {
		private final int idx;
		public TabButton(int idx, String label) {
			super(label);
			this.idx = idx;
			addActionListener(e -> switchTab(idx));
		}
	}

	public ZoneDrums(DrumSynth synth) {
		this.kit = synth;
		baseParams.setSelectedIndex(0);
		filterParams.setSelectedIndex(0);
		customParams.setSelectedIndex(0);
		baseParams.addActionListener(e -> updateKnobsForTab());
		filterParams.addActionListener(e -> updateKnobsForTab());
		customParams.addActionListener(e -> updateKnobsForTab());
		indexed = List.of(baseParams, filterParams, customParams);

		// Build tabs
		TabButton base = new TabButton(0, "Base");
		tabs.add(base);
		tabBtns.add(base);
		base.setSelected(true);
		activeTab = base;

		TabButton filter = new TabButton(1, "Filter");
		tabs.add(filter);
		tabBtns.add(filter);

		TabButton custom = new TabButton(2, "Custom");
		tabs.add(custom);
		tabBtns.add(custom);

		header.add(base);
		header.add(filter);
		header.add(custom);
		header.add(baseParams);

		// Build pads 2x4 grid
		for (DrumType t : DrumType.values()) {
			DrumOscPad pad = new DrumOscPad(synth.getDrum(t), synth);
			pads.add(pad);
			main.add(pad);
		}

		// Layout
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(1));
		add(main);
		add(Box.createVerticalStrut(1));
		add(header);
		add(Box.createVerticalStrut(1));
	}

	// a param might have changed via midi controller.  Make sure pointed to knob updates to data */
	public void update(DrumParams p) {
		if (p.tabIdx() == activeTab.idx) {
			// same tab, just update knob
			int tabIdx = activeTab.idx;
			int paramIdx = indexed.get(tabIdx).getSelectedIndex();
			int value = DrumParams.get(p.drum(), tabIdx, paramIdx);

			for (DrumOscPad pad : pads)
				if (pad.getDrum() == p.drum()) {
					pad.update(value);
					return;
				}
			return;
		}
	}

	private void switchTab(int idx) {
		activeTab = tabs.get(idx);
		header.remove(header.getComponentCount() - 1);
		header.add(indexed.get(idx));
		header.revalidate();
		header.repaint();
		updateKnobsForTab();
	}

	private void updateKnobsForTab() {
		int paramIdx = indexed.get(activeTab.idx).getSelectedIndex();
		for (int i = 0; i < pads.size(); i++)
			pads.get(i).updateMode(activeTab.idx, paramIdx);
	}

	@Override public void doKnob(int padIdx, int value) {
		if (padIdx < pads.size()) {
			DrumOscPad pad = pads.get(padIdx);
			// Knob drives parameter based on active tab + param
			int tabIdx = activeTab.idx;
			int paramIdx = indexed.get(tabIdx).getSelectedIndex();
			DrumParams.set(new DrumParams(pad.getDrum(), tabIdx, paramIdx, value));
		}
	}

	/** Cycle to next tab (pad1). */
	public void pad1() {
		int next = (activeTab.idx + 1) % tabs.size();
		tabs.get(next).doClick();
	}

	/** Cycle to next param in active tab (pad2). */
	public void pad2() {
		JComboBox<?> combo = indexed.get(activeTab.idx);
		int next = (combo.getSelectedIndex() + 1) % combo.getItemCount();
		combo.setSelectedIndex(next);
		updateKnobsForTab();
	}

	/** Update pad backgrounds when drums play. */
	public void update(Actives actives) {
		pads.forEach(pad -> pad.update(actives));
	}

	@Override public void update(Object o) {
		if (o instanceof Actives a)
			update(a);
		else if (o instanceof DrumParams dp)
			update(dp);
	}

	@Override public void update() {
		pads.forEach(p -> p.update());
	}

}
