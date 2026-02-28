package net.judah.gui.drums.knobs;

import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import judahzone.api.RTNotes;
import judahzone.gui.Gui;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.drums.Drum;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumType;
import net.judah.drums.DrumUpdate;
import net.judah.gui.Size;
import net.judah.gui.knobs.Knobs;
import net.judah.mixer.Channels.RegisteredDrums;

/** drum parameter UI for all kit types (Noise, Wav, FM, Pluck).
 *  Tabs: 0=Base (Vol/Pan/Atk/Decay), 1=Filters, 2=Extra params.
 *  ParamCombo on tabBar reflects current selection. */
public class KitKnobs extends Knobs {

	@Getter private final DrumKit kit;

	private final JPanel tabBar = new JPanel();
	private final JComboBox<String> paramCombo = new JComboBox<>();
	private final JPanel knobGrid = new JPanel(new GridLayout(2, 4, 2, 2));

	private int currentTab = 0;
	private int currentParam = 0;

	private final ActionListener paramListener;
	private final DrumPad[] pads;

	public KitKnobs(DrumKit kit) {
	    this.kit = kit;
	    this.pads = new DrumPad[DrumType.values().length];

	    paramListener = e -> {
	        int selected = paramCombo.getSelectedIndex();
	        if (selected >= 0) {
	            currentParam = selected;

	            DrumUpdate.Mode mode = switch (currentTab) {
	                case 0 -> DrumUpdate.Mode.BASE;
	                case 1 -> DrumUpdate.Mode.FILTER;
	                case 2 -> {
	                    RegisteredDrums type = kit.getType().engine;
	                    yield switch (type) {
	                        case Noise -> DrumUpdate.Mode.NOISE;
	                        case FM -> DrumUpdate.Mode.FM;
	                        case Pluck -> DrumUpdate.Mode.PLUCK;
	                        default -> DrumUpdate.Mode.BASE;
	                    };
	                }
	                default -> DrumUpdate.Mode.BASE;
	            };

	            for (DrumPad pad : pads)
	                pad.paramIdx(mode, currentParam);
	            updateMode(currentTab, currentParam);
	            update();
	        }
	    };

	    initLayout();
	    initPads();
	    initTabs();
	    paramCombo.addActionListener(paramListener);
	    updateMode(currentTab, currentParam);
	}

	private void initLayout() {
	    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	    add(Box.createVerticalStrut(2));
	    add(knobGrid);
	    add(Box.createVerticalStrut(2));
	    add(Gui.wrap(tabBar));
	    add(Box.createVerticalStrut(1));
	}

	private void initPads() {
	    for (int i = 0; i < DrumType.values().length; i++) {
	        Drum drum = kit.get(i);
	        DrumPad pad = new DrumPad(drum, kit);
	        pads[i] = pad;
	        knobGrid.add(pad);
	    }
	}

	private void initTabs() {
	    tabBar.setLayout(new BoxLayout(tabBar, BoxLayout.X_AXIS));

	    addTabButton("Base", 0);
	    addTabButton("Filters", 1);
	    RegisteredDrums type = kit.getType().engine;
	    if (type != RegisteredDrums.Wav)
	        addTabButton(type.name(), 2);

	    tabBar.add(Box.createHorizontalStrut(8));
	    tabBar.add(Gui.resize(paramCombo, Size.COMBO_SIZE));
	}

	private void addTabButton(String label, int tabIdx) {
	    JToggleButton btn = new JToggleButton(label);
	    btn.addActionListener(e -> {
	        currentTab = tabIdx;
	        currentParam = 0;
	        updateMode(currentTab, currentParam);
	        refreshTabs();
	    });
	    if (tabIdx == 0)
	        btn.setSelected(true);
	    tabBar.add(btn);
	}


	private void refreshTabs() {
	    for (var comp : tabBar.getComponents()) {
	        if (comp instanceof JToggleButton btn) {
	            btn.setSelected(tabBar.getComponentZOrder(btn) == currentTab);
	        }
	    }
	}

	/** Update param combos for current tab/drum. */
	// In DrumKnobs.java - update the updateMode method

	public void updateMode(int tabIdx, int paramIdx) {
	    paramCombo.removeAllItems();

	    if (pads.length == 0) {
	        paramCombo.addActionListener(paramListener);
	        return;
	    }

	    Drum drum = kit.get(0);


	    // Map tabIdx to DrumUpdate.Mode
	    DrumUpdate.Mode mode = switch (tabIdx) {
	        case 0 -> DrumUpdate.Mode.BASE;
	        case 1 -> DrumUpdate.Mode.FILTER;
	        case 2 -> {
	            RegisteredDrums type = kit.getType().engine;
	            yield switch (type) {
	                case Noise -> DrumUpdate.Mode.NOISE;
	                case FM -> DrumUpdate.Mode.FM;
	                case Pluck -> DrumUpdate.Mode.PLUCK;
	                default -> DrumUpdate.Mode.BASE;
	            };
	        }
	        default -> DrumUpdate.Mode.BASE;
	    };
	    String[] paramNames = drum.getDrama().getParamNames(drum, mode);

	    // Update all pads with the correct mode
	    for (DrumPad pad : pads)
	        pad.paramIdx(mode, paramIdx);

	    for (String name : paramNames)
	        paramCombo.addItem(name);

	    if (paramIdx < paramNames.length)
	        paramCombo.setSelectedIndex(paramIdx);

	    paramCombo.addActionListener(paramListener);
	}

	public void update(RTNotes a) {
		// repaint pad backgrounds based on actives
	}

	public void update(Object o) {
	    if (o == null)
	        return;
	    // ton of actives updates coming in
	    RTLogger.log(this, o.getClass().getSimpleName() + " update: " + o);

	}

	public void update() {
	    for (DrumPad pad : pads)
	        pad.update();
	}

	@Override
	public void doKnob(int drumIdx, int value) {
	    pads[drumIdx].knobChanged(value);
	}

	public void pad1() {
	    // TODO Auto-generated method stub
	}

	public void pad2() {
	    // TODO Auto-generated method stub
	}
	/** Cycle to next tab (pad1). */

//		public void pad1() { //	int next = (activeTab.idx + 1) % tabs.size(); //	tabs.get(next).doClick(); //	}

	/** Cycle to next param in active tab (pad2). */

//		public void pad2() { //	JComboBox<?> combo = indexed.get(activeTab.idx); //	int next = (combo.getSelectedIndex() + 1) % combo.getItemCount(); //	combo.setSelectedIndex(next); //	updateKnobsForTab(); //	}

	}