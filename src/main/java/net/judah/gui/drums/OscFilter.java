package net.judah.gui.drums;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;

import judahzone.drums.Drum;
import judahzone.drums.DrumUpdate;
import judahzone.fx.EQ.EqBand;
import judahzone.gui.Gui;
import judahzone.util.Frequency;
import judahzone.widgets.Knob;
import judahzone.widgets.RangeSlider;
import net.judah.gui.Size;
import net.judah.gui.drums.views.DrumView;
import net.judah.gui.drums.views.DrumView.DataLbl;
import net.judah.gui.drums.views.DrumView.SZ;
import net.judah.gui.widgets.Slider;


/*
	      "Gain"       "Filter"     "Width"
	     gainK      <---- x ----->    bwK
    +/-25.0           hZ /Note        #.# (0.2 - 3)
			"Low         Pass          High"

##55  < -------x-------y----------> 10001

	   loresK   "LoRes"    "HiRes"  hiresK
    25.0                         25.0				*/


/*
		private final JToggleButton tracking = new JToggleButton("Sync");
		tracking.setToolTipText("Peaking filter follows fundamental");
		tracking.addChangeListener(this);
		add(tracking);
		tracking.setSelected(drum.isTracking());

		if (e.getSource() == tracking)
			drum.getDrama().setTracking(tracking.isSelected());

 */

public class OscFilter extends JPanel implements DrumView.SZ {

	private final Drum drum;

	private final RangeSlider hilo;
	private final Knob gainK = new Knob();
	private final Knob bwK = new Knob();
	private final Knob loResK = new Knob();
	private final Knob hiResK = new Knob();
	private final Slider mid = new Slider(0, DrumView.SZ.SLIDER_WIDTH, null);

	private final DataLbl gainD = new DataLbl();
	private final DataLbl bwD = new DataLbl();
	private final DataLbl loResVal = new DataLbl();
	private final DataLbl hiResVal = new DataLbl();

	private final JLabel loCutVal = new JLabel("", JLabel.RIGHT);
	private final JLabel hiCutVal = new JLabel();
	private final JLabel midVal = new JLabel("", JLabel.CENTER);

	public OscFilter(Drum drum) {
		this.drum = drum;
		hilo = new RangeSlider(0, 100, drum.getHzKnob(EqBand.Bass), drum.getHzKnob(EqBand.High));
		hilo.addChangeListener(l -> hilo(l));

		loResK.addListener(val -> drum.getEdit().set(new DrumUpdate(drum, DrumUpdate.Mode.FILTER, 2, val)));
		hiResK.addListener(val -> drum.getEdit().set(new DrumUpdate(drum, DrumUpdate.Mode.FILTER, 3, val)));
		mid.addChangeListener(l -> drum.getEdit().set(new DrumUpdate(drum, DrumUpdate.Mode.FILTER, 4, mid.getValue())));
		gainK.addListener(val -> drum.getEdit().set(new DrumUpdate(drum, DrumUpdate.Mode.FILTER, 5, val)));
		bwK.addListener(val -> drum.getEdit().set(new DrumUpdate(drum, DrumUpdate.Mode.FILTER, 6, val)));

		Gui.resize(loCutVal, Size.MEDIUM);
		Gui.resize(hiCutVal, Size.MEDIUM);
		Gui.resize(midVal, Size.COMBO_SIZE);


		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		Gui.resize(mid, SZ.SLIDER_DIM);

		add(Gui.box(
				new JLabel("Gain"),
				Box.createHorizontalStrut(45),
				new JLabel("Peak Filter"),
				Box.createHorizontalStrut(45),
				new JLabel("Width")));
		add(Gui.box(
				gainK,
				Box.createHorizontalStrut(15),
				mid,
				Box.createHorizontalStrut(15),
				bwK));
		add(Gui.box(
				Box.createHorizontalStrut(5),
				gainD,
				Box.createHorizontalStrut(20),
				midVal,
				Box.createHorizontalStrut(20),
				bwD,
				Box.createHorizontalStrut(5)));
		add(Gui.box(
				Box.createHorizontalStrut(6),
				loCutVal,
				Box.createHorizontalStrut(12),
				Gui.resize(hilo, SZ.SLIDER_DIM),
				hilo,
				Box.createHorizontalStrut(12),
				hiCutVal,
				Box.createHorizontalStrut(6)));
		add(Gui.box(
				loResK,
				Box.createHorizontalStrut(15),
				new JLabel("LoRes"),
				Box.createHorizontalStrut(55),
				new JLabel("HiRes"),
				Box.createHorizontalStrut(15),
				hiResK));
		add(Gui.box(
				loResVal,
				Box.createHorizontalStrut(140),
				hiResVal));

	}

	private void hilo(ChangeEvent l) {
		int loKnob = hilo.getValue();
		int lo = drum.getHzKnob(EqBand.Bass);
		if (lo != loKnob)
			drum.getEdit().set(new DrumUpdate(drum, DrumUpdate.Mode.FILTER, 0, loKnob));

		int hiKnob = hilo.getUpperValue();
		int hi = drum.getHzKnob(EqBand.High);
		if (hi != hiKnob)
			drum.getEdit().set(new DrumUpdate(drum, DrumUpdate.Mode.FILTER, 1, hiKnob));
	}

	public void update() {
		int loCut = drum.getHzKnob(EqBand.Bass);
		if (hilo.getValue() != loCut)
			hilo.setValue(loCut);
		loCutVal.setText(String.format("%.1f", drum.getHz(EqBand.Bass)));

		int hiCut = drum.getHzKnob(EqBand.High);
		if (hilo.getUpperValue() != hiCut)
			hilo.setUpperValue(hiCut);
		hiCutVal.setText(String.format("%.1f", drum.getHz(EqBand.High)));

		int hiRes = drum.getResonanceKnob(EqBand.High);
		if (hiResK.getValue() != hiRes)
			hiResK.setValue(hiRes);
		hiResVal.setText(String.format("%.1f", drum.getResonance(EqBand.High)));

		int loRes = drum.getResonanceKnob(EqBand.Bass);
		if (loResK.getValue() != loRes)
			loResK.setValue(loRes);
		loResVal.setText(String.format("%.1f", drum.getResonance(EqBand.Bass)));

		int peakVal = drum.getResonanceKnob(EqBand.Mid);
		if (gainK.getValue() != peakVal)
			gainK.setValue(peakVal);
		gainD.setText(String.format("%.1f", drum.getResonance(EqBand.Mid)));

		// peak hz, peak b/w
		int midHz = drum.getHzKnob(EqBand.Mid);
		if (mid.getValue() != midHz)
			mid.setValue(midHz);
		float hz = drum.getHz(EqBand.Mid);
		midVal.setText(String.format("%.1f", drum.getHz(EqBand.Mid)) + " : " + Frequency.toNote(hz).toString());
		int bwKnob = drum.getMid().getBandwidthKnob();
		if (bwK.getValue() != bwKnob)
			bwK.setValue(bwKnob);
		bwD.setText(String.format("%.1f", drum.getMid().getBandwidth()));

	}


}
