package net.judah.gui.drums.knobs;

import java.awt.Color;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import judahzone.api.Midi;
import judahzone.drums.Drum;
import judahzone.drums.DrumEdit;
import judahzone.drums.DrumUpdate;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.widgets.Knob;
import judahzone.widgets.Knob.KnobListener;
import net.judah.drums.Drumset;
import net.judah.gui.MainFrame;
import net.judah.gui.drums.views.DrumFrame;
import net.judah.midi.JudahMidi;

/** Single drum pad: shows status and syncs with kit knob. */
class DrumPad extends Gui.Opaque implements KnobListener, Gui.Mouse {

	private static final Color ACTIVE_COLOR = Pastels.PURPLE;
	private static final Color BORDER = Color.DARK_GRAY;

	private final Drum drum;
	private final DrumEdit edit;
	private final Drumset kit;

	private String unit = "%";


	private DrumUpdate.Mode mode = DrumUpdate.Mode.BASE;
	private int paramIdx;

	private final JLabel header;

	private final Knob knob = new Knob(this);
	private final JLabel dataLabel = new JLabel("--", JLabel.CENTER);
	private final JLabel unitLabel = new JLabel(unit, JLabel.CENTER);

	public DrumPad(Drum drum, Drumset kit) {
		this.drum = drum;
		this.edit = drum.getEdit();
		this.kit = kit;
		header = new JLabel(drum.getType().name(), JLabel.CENTER);

		setBorder(new LineBorder(BORDER, 1));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(2));
		add(header);
		add(Box.createVerticalStrut(12));
		add(Gui.wrap(knob));
		Box labels = new Box(BoxLayout.Y_AXIS);
		labels.add(dataLabel);
		labels.add(unitLabel);
		add(labels);
		add(Box.createVerticalStrut(3));
		addMouseListener(this);
	}

	@Override public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			new DrumFrame(kit, drum);
			return;
		}
		Midi click = Midi.create(Midi.NOTE_ON, drum.getType().getData1(), 100);
		kit.send(click, JudahMidi.ticker());
	}



	public void background(boolean active) {
		setBackground(active ? ACTIVE_COLOR : null);
	}

	public void update() {
		// Reflect active play state
//		UINotes actives = kit.getActives();
//		boolean isActive = actives.find(drum.getType().getData1()) != null;
//		setBackground(isActive ? ACTIVE_COLOR : null);

		// Update param display
		int val = edit.get(paramIdx, mode);
		if (knob.getValue() != val)
			knob.setValue(val);
		dataLabel.setText(edit.val(paramIdx, mode));
	}

	public void paramIdx(DrumUpdate.Mode m, int i) {
		mode = m;
		paramIdx = i;
		int target = drum.getEdit().get(i, mode);
		if (knob.getValue() != target)
			knob.setValue(target);
		unit = drum.getEdit().getUnit(mode, i);
		unitLabel.setText(unit);
		dataLabel.setText("" + target);
	}

	@Override
	public void knobChanged(int value) { // this might be on RT thread, route through MF
		DrumUpdate data = new DrumUpdate(drum, mode, paramIdx, value);
		MainFrame.update(data);
	}

}



/* Legacy style, need to reproduce functionality:

public void knobChanged(int value) {

// Forward local pad knob moves to the kit's ZoneDrums doKnob handler
// so the change becomes the single source-of-truth update (DrumParams.set).
kit.getKnobs().doKnob(drum.getType().ordinal(), value);
}


public void updateMode(int tabIdx, int paramIdx) {
currentTab = tabIdx;
currentParam = paramIdx;
label();
// Sync knob to current parameter value
// Called when tab/param selection changes
update(NoiseParams.get(drum, tabIdx, paramIdx));
}


// Update pad background to reflect active play state.
@Override
public void update(Actives actives) {
ShortMessage msg = actives.find(drum.getType().getData1());
top.setBackground(msg == null ? null : Pastels.GREEN);
if (choke != null && choke.isSelected() != kit.isChoked())
	choke.setSelected(kit.isChoked());
}

public void update() {
// knobs stale i.e., progChange
int update = NoiseParams.get(drum, currentTab, currentParam);
update(update);
}

@Override
public void update(int value) {
if (knob.getValue() != value)
	knob.setValue(value);
data(value);
}

private void data(int value) {
data.setForeground(Color.BLACK);
if (currentTab == 0)
	data.setText(value + "");

else if (currentTab == 1) {
	if (currentParam == 0) // cutoff freq
		hzDuo(value, EqBand.Bass);
	else if (currentParam == 1) // cutoff freq
		hzDuo(value, EqBand.High);

	else if (currentParam == 2 || currentParam == 3)  // resonance
		data.setText(String.format("%.1f", value * 0.25f));
	else
		data.setText(value + " %");
}
else if (currentTab == 2) {
	if (currentParam > 2)  // pitch
		hzDuo(value, EqBand.Mid);
	else {
		if (currentParam == 0 && drum instanceof Snare snare) // snare noise type
			data.setText(snare.getNoiseGen().getColour().name());
		else if (currentParam == 2 && drum instanceof Bongo bongo) // bongo skin
			data.setText(bongo.getMembrane().name());
		else
			data.setText(value + "");
	}
}
}

private void hzDuo(int data1, EqBand type) {
Key key = Key.key(data1);
int octave = data1 / 12;
float freq = Key.toFrequency(key, octave);
data.setText("" + (int)freq);
label.setText(key.toString() + octave);

switch(type) {
	case Bass -> {
	if (freq > drum.getHz(EqBand.High))
		data.setForeground(Color.RED);
	}
	case Mid -> {
		if (freq < drum.getHz(EqBand.Bass) || freq > drum.getHz(EqBand.High))
			data.setForeground(Color.RED);
	}
	case High ->{
		if (freq < drum.getHz(EqBand.Bass))
			data.setForeground(Color.RED);
	}
}
}

@Override
protected void label() {
// Update label() to show current param name
if (currentTab == 0) {
	label.setText("%");
}
else if (currentTab == 1) {
	label.setText(FilterParam.values()[currentParam].name());
}
else if (currentTab == 2) {
	if (currentParam > 2)
		label.setText(""); // Midi1 data
	else
		label.setText(drum.getNames()[currentParam]);
}
}
*/