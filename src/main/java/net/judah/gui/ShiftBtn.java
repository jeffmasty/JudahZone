package net.judah.gui;

import judahzone.gui.Pastels;
import judahzone.widgets.Click;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.fx.EffectsRack;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.LFOKnobs;

/** Shift modifier toggle button. Updates UI elements that respond to shift state:
    midiShift:true vetoes mouseShift events.
    Fires GUI updates on change. */
public class ShiftBtn extends Click {

	private static boolean midiShift = false;
	private static boolean mouseShift = false;
	@Getter private static boolean active;

	public ShiftBtn() {
		super(" Shift ");
		addActionListener(e -> toggleMouseShift());
		setOpaque(true);
		setBorder(null);
	}

	private void toggleMouseShift() {
		if (midiShift) // vetoed.
			return;
		mouseShift = !mouseShift;
		updateShiftState();
	}

	public static void setMidiShift(boolean on) {
		midiShift = on;
		updateShiftState();
	}

	private static void updateShiftState() {
		boolean newActive = midiShift || mouseShift;
		if (active != newActive) {
			active = newActive;
			applyShiftEffects();
		}
	}

	private static void applyShiftEffects() {

		JudahZone.getInstance().getFrame().getMenu().shift(active);

		EffectsRack fx = JudahZone.getInstance().getFxRack().getChannel().getGui();
		fx.getEq().toggle();
		fx.getReverb().toggle();

		if (MainFrame.getKnobMode() == KnobMode.LFO)
			((LFOKnobs)MainFrame.getKnobs()).upperLower();
	}

	public void updateAppearance() {
		setBackground(active ? Pastels.YELLOW : null);
	}
}
