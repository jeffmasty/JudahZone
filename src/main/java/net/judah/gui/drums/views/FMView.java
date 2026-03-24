package net.judah.gui.drums.views;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import judahzone.drums.DrumUpdate;
import judahzone.drums.DrumUpdate.Mode;
import judahzone.drums.fm.Algorithm;
import judahzone.drums.fm.DX9;
import judahzone.drums.fm.FMOsc;
import judahzone.drums.fm.Operator.Params;
import judahzone.gui.Gui;
import net.judah.gui.Size;

/** a complete FMOsc, operaters(6) and all */
public class FMView extends DrumView<FMOsc> implements ActionListener {

	private final DX9Grid opPanel;
	private final Box combos = new Box(BoxLayout.X_AXIS);

	private final JComboBox<String> algo = new JComboBox<>(DX9.names);
	private final JComboBox<Params> param = new JComboBox<>(Params.values());
	private final JLabel algoLabel = new JLabel("Algorithm");

	public FMView(FMOsc fm) {
		super(fm);
		opPanel = new DX9Grid(this);
		param.setFont(Gui.BOLD11);

		combos.add(Box.createHorizontalStrut(3));
		combos.add(new JLabel("Algorithm"));
		combos.add(Gui.resize(algo, Size.MEDIUM));
		combos.add(Box.createHorizontalStrut(6));
		combos.add(new JLabel("Param"));
		combos.add(Gui.resize(param, Size.WIDE_SIZE));
		combos.add(Box.createHorizontalStrut(3));

		child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));
		child.add(Gui.wrap(combos));
		child.add(Gui.wrap(opPanel));
		child.add(Gui.wrap(algoLabel));

		algo.addActionListener(this);
		param.addActionListener(this);
		revalidate();

		update();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
//		if (updating) return;
		Object src = e.getSource();
		if (src == algo)
			algoSet();
		else if (src == param)
			opPanel.updateAll();
	}

	public int encode(int opIdx) {
		return encode((Params) param.getSelectedItem(), opIdx);
	}

	public static int encode(Params p, int opIdx) {
		return p.ordinal() + (opIdx + 1) * 10;
	}

	public static Object[] decode(int fmIndex) {
		int opIdx = (fmIndex / 10) - 1;
		int paramIdx = fmIndex % 10;
		if (opIdx < 0 || opIdx >= 6 || paramIdx < 0 || paramIdx >= Params.values().length)
			throw new IllegalArgumentException("Invalid FM index: " + fmIndex);
		return new Object[] {Params.values()[paramIdx], opIdx};
	}

	// called from algo view
	public void opKnobEvent(int opIdx,	int val) {
		Params p = (Params) param.getSelectedItem();
		if (p == null) return;

		// Encode op index and param ordinal into a single int for DrumUpdate
		int fmIndex = p.ordinal() + (opIdx + 1) * 10;
		drum.getEdit().set(new DrumUpdate(drum, Mode.FM, fmIndex, val));
	}

	private void algoSet() {
		int selected = algo.getSelectedIndex();
		int algoCount = DX9.ALGO_COUNT;
		int idx = (algoCount <= 1) ? 0 : Math.round((selected / (float)(algoCount - 1)) * 100f);
		idx = Math.max(0, Math.min(idx, 100));
		drum.getEdit().set(new DrumUpdate(drum, Mode.FM, 0, idx));
	}

//	private void updateOpKnobs() {
//		Params p = (Params) param.getSelectedItem();
//		if (p == null) return;
//
//		opPanel.updateAll();
//
//		for (int i = 0; i < fm.getOpCount(); i++) {
//			// Correctly encode index to fetch value from FMOsc
//			int fmIndex = p.ordinal() + (i + 1) * 10;
//			int val = drum.get(fmIndex);
//
//			if (val != -1) {
//				opKnobs[i].getSlider().setValue(val);
//			}
//		}
//	}

	@Override protected void updateChild() {
		Algorithm target = drum.getAlgo();
		int targetIdx = DX9.indexOf(target);
		if (targetIdx != -1 && targetIdx != algo.getSelectedIndex())
			algo.setSelectedIndex(targetIdx);

		opPanel.setAlgo(target); // updateAll()
		if (targetIdx != -1)
			algoLabel.setText(DX9.descriptions[targetIdx]);
	}
}
