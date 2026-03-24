package net.judah.gui.drums.views;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.event.ChangeEvent;

import judahzone.drums.DrumEdit;
import judahzone.drums.DrumUpdate;
import judahzone.drums.DrumUpdate.Mode;
import judahzone.drums.pluck.PluckOsc;
import judahzone.drums.pluck.PluckOsc.Params;
import judahzone.util.Constants;
import judahzone.widgets.SingleRow;
import net.judah.gui.widgets.Slider;

/** Full params of a Pluck Osc in a single view */
public class PluckView extends DrumView<PluckOsc> {

	private final PluckOsc pluck; // voices
	private final DrumEdit edit;

	private final Box burstPnl = new Box(BoxLayout.Y_AXIS); // burst [samples, preamp, colour, pitch]
	private final Box bodyPnl = new Box(BoxLayout.Y_AXIS); // ad fb damp  [a/d, f/b, damp, harmony, combs]

	private final Slider burst = new Slider(0, Constants.NORMAL, e->change(e));
	private final Slider amp = new Slider(0, Constants.NORMAL, e->change(e));
	private final Slider colour = new Slider(0, Constants.NORMAL, e->change(e));
	private final Slider tap = new Slider(0, Constants.NORMAL, e->change(e));
	private final Slider feedback = new Slider(0, Constants.NORMAL, e->change(e));
	private final Slider bright = new Slider(0, Constants.NORMAL, e->change(e));
	private final Slider room = new Slider(0, Constants.NORMAL, e->change(e));
	private final Slider combs = new Slider(0, Constants.NORMAL, e->change(e));

	private final DataLbl burstData = new DataLbl();
	private final DataLbl ampData = new DataLbl();
	private final DataLbl colourData = new DataLbl();
	private final DataLbl tapData = new DataLbl();
	private final DataLbl feedbackData = new DataLbl();
	private final DataLbl dampData = new DataLbl();
	private final DataLbl roomData = new DataLbl();
	private final DataLbl combsData = new DataLbl();

	public PluckView(PluckOsc pluck) {
		super(pluck);
		this.pluck = pluck;
		this.edit = pluck.getEdit();

		burstPnl.setBorder(BorderFactory.createTitledBorder("Burst"));
		burstPnl.add(new SingleRow("Burst", burst, burstData));
		burstPnl.add(new SingleRow("Colour", colour, colourData));
		burstPnl.add(new SingleRow("Gain", amp, ampData));
		burstPnl.add(new SingleRow("Pickup", tap, tapData));

		bodyPnl.setBorder(BorderFactory.createTitledBorder("Loop"));
		bodyPnl.add(new SingleRow("Feedback", feedback, feedbackData));
		bodyPnl.add(new SingleRow("Bright", bright, dampData));
		bodyPnl.add(new SingleRow("Metal", combs, combsData));
		bodyPnl.add(new SingleRow("Room", room, roomData));

		child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));
		child.add(burstPnl);
		child.add(bodyPnl);
		update();
	}

	private void change(ChangeEvent e) {

		Object src = e.getSource();

		if (src == burst)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 0, burst.getValue()));
		else if (src == amp)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 1, amp.getValue()));
		else if (src == tap)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 2, tap.getValue()));
		else if (src == feedback)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 3, feedback.getValue()));
		else if (src == bright)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 4, bright.getValue()));
		else if (src == room)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 5, room.getValue()));
		else if (src == combs)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 6, combs.getValue()));
		else if (src == colour)
			edit.set(new DrumUpdate(pluck, Mode.PLUCK, 7, colour.getValue()));
	}

	@Override protected void updateChild() {

		for (int idx = 0; idx < Params.values().length; idx++) {
			Slider slider = switch(idx) {
				case 0 -> burst;
				case 1 -> amp;
				case 2 -> tap;
				case 3 -> feedback;
				case 4 -> bright;
				case 5 -> room;
				case 6 -> combs;
				case 7 -> colour;
				default -> null;
			};
			DataLbl d = switch(idx) {
				case 0 -> burstData;
				case 1 -> ampData;
				case 2 -> tapData;
				case 3 -> feedbackData;
				case 4 -> dampData;
				case 5 -> roomData;
				case 6 -> combsData;
				case 7 -> colourData;
				default -> null;
			};

			int val = pluck.get(idx);
			if (slider.getValue() != val)
				slider.setValue(val);
			d.setText(pluck.val(idx));
		}
	}

}
