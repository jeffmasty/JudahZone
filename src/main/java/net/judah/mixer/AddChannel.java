package net.judah.mixer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import judahzone.api.Custom;
import judahzone.api.Ports;
import judahzone.gui.DialogManager;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.util.Constants;
import judahzone.widgets.RangeSlider;
import net.judah.JudahZone;
import net.judah.bridge.AudioEngine;
import net.judah.gui.Size;
import net.judah.gui.widgets.ModalDialog;

/**
 * Form to create or edit a custom channel registration. Titled border toggles
 * between "New Channel" and "Edit Channel". Supports internal engine or
 * external LineIn with mono/stereo UX, RangeSlider for low/high cut in mono
 * mode, and MIDI options.
 */
public class AddChannel extends JPanel implements Ports.PortData {

	private static final java.awt.Dimension DEFAULT_DIALOG_SIZE = new java.awt.Dimension(520, 420);

	static final String[] THUMBS = { "Guitar.png", "Drums.png", "Microphone.png", "Fluid.png", "Synth.png",
			"Violin.png", "DrumMachine.png", "Abacus.png", "Waveform.png", "LoopA.png", "LoopB.png", "Gear.png",
			"PlayActive.png", "MuteRecordInactive.png", "left.png" };

	private static JFrame frame;
	private final Channels channels;
	private final Consumer<Custom> onOk;
	private final Ports.Provider provider = AudioEngine.getPorts();

	private final JTextField name;
	private final JCheckBox onMixer;
	private final JRadioButton internal;
	private final JRadioButton external;
	private final JRadioButton mono;
	private final JRadioButton stereo;
	private final JComboBox<String> leftPort;
	private final JComboBox<String> rightPort;
	private final JComboBox<String> midiOut;
	private final JCheckBox midiEnabled;
	private final JCheckBox midiClock;
	//percent-based RangeSlider (1..100) maps to Hz via Constants.logarithmic
	private final RangeSlider filterRange;
	private final JLabel lowLabel;
	private final JLabel highLabel;
	private final JComboBox<Channels.RegisteredSynths> engineBox;
	private final JCheckBox autosave;
	private final JButton ok;
	private final JButton cancel;
	private final ButtonGroup icons = new ButtonGroup();
	private JToggleButton selectedIcon;
	//Swap-able container for either the filter UI or the right-port combo
	private final JPanel stereoOrFilter = new JPanel(new BorderLayout());
	private final JLabel stereoOrFilterLabel;
	//Keep range panel as a field so we can swap it in/out
	private final JPanel rangePanel;
	private static final int MIN_HZ = 30;
	private static final int MAX_HZ = 16000;
	// Preamp UI component (always visible row)
	private final Preamp preamp;

	// Pending selections retained while awaiting async port query results (edit mode)
	private String pendingLeftPort = null;
	private String pendingRightPort = null;
	private String pendingMidiPort = null;

	//populate the new form for edit flows.
	public static void open(Channels channels) {
		open(channels, null, null);
	}

	public static void open(Channels channels, Consumer<Custom> onOk) {
		open(channels, onOk, null);
	}

	public static void open(Channels channels, Consumer<Custom> onOk, Custom toPopulate) {
		if (frame != null) {
			frame.dispose();
			frame = null;
		}
		AddChannel ac = new AddChannel(channels, onOk);
		if (toPopulate != null)
			ac.populate(toPopulate);
	}

	AddChannel(Channels channels, Consumer<Custom> caller) {
		this.onOk = caller;
		this.channels = channels;

		setName(JudahZone.JUDAHZONE);
		setLayout(new BorderLayout());
		// initialize with titled border matching "create new" title
		setBorder(BorderFactory.createTitledBorder("New Channel"));

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 6, 4, 6);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;

		int row = 0;

		// Name + OnMixer on same row
		name = new JTextField();
		onMixer = new JCheckBox("Show in Mixer", true);
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
		namePanel.add(name);
		namePanel.add(Box.createHorizontalStrut(8));
		namePanel.add(onMixer);
		addRow(form, c, row++, new JLabel("Name:"), namePanel);

		JPanel ribbon = new JPanel(new GridLayout(1, THUMBS.length, 5, 0));
		ribbon.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		for (String thumb : THUMBS) {
			JToggleButton btn = new JToggleButton(Icons.get(thumb));
			Gui.resize(btn, Icons.THUMB);
			btn.setName(thumb);
			btn.setToolTipText(thumb);
			btn.addActionListener(e -> selectedIcon = btn);
			icons.add(btn);
			ribbon.add(btn);
			if (selectedIcon == null) {
				btn.setSelected(true);
				selectedIcon = btn;
			}
		}

		JScrollPane scroll = new JScrollPane(ribbon);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		// Ensure the viewport is at least 32px tall so icons are visible
		scroll.getViewport().setMinimumSize(new Dimension(200, 50));
		addRow(form, c, row++, new JLabel("Icon:"), scroll);

		// Type: internal (engine) vs external (LineIn)
		// Default to LineIn (external) per requested change.
		internal = new JRadioButton("Engine", false);
		external = new JRadioButton("LineIn", true);
		ButtonGroup bg = new ButtonGroup();
		bg.add(internal);
		bg.add(external);

		// Engine selector (internal)
		engineBox = new JComboBox<>(Channels.RegisteredSynths.values());

		mono = new JRadioButton("Mono", true);
		stereo = new JRadioButton("Stereo", false);
		ButtonGroup cs = new ButtonGroup();
		cs.add(mono);
		cs.add(stereo);

		// Mono / Stereo radio buttons
		JPanel linePanel = new JPanel();
		linePanel.add(mono);
		linePanel.add(stereo);

		addRow(form, c, row++, internal, engineBox);
		addRow(form, c, row++, external, linePanel);

		leftPort = new JComboBox<>(new String[] { "default" });
		rightPort = new JComboBox<>(new String[] { "default" });
		addRow(form, c, row++, new JLabel("Left / Mono:"), leftPort);

		// RangeSlider now works in percent (1..100); logarithmic mapping to Hz
		filterRange = new RangeSlider(1, 100);
		// initialize percent values from desired Hz defaults
		filterRange.setValue(Constants.reverseLog(85, MIN_HZ, MAX_HZ));
		filterRange.setUpperValue(Constants.reverseLog(12000, MIN_HZ, MAX_HZ));
		lowLabel = new JLabel(
				Integer.toString(Math.round(Constants.logarithmic(filterRange.getValue(), MIN_HZ, MAX_HZ))) + "Hz",
				JLabel.CENTER);
		highLabel = new JLabel(
				Integer.toString(Math.round(Constants.logarithmic(filterRange.getUpperValue(), MIN_HZ, MAX_HZ))) + "Hz",
				JLabel.CENTER);
		Gui.resize(lowLabel, Size.MEDIUM);
		Gui.resize(highLabel, Size.MEDIUM);
		rangePanel = new JPanel();
		rangePanel.setLayout(new BoxLayout(rangePanel, BoxLayout.X_AXIS));
		rangePanel.add(Box.createHorizontalStrut(8));
		rangePanel.add(lowLabel);
		rangePanel.add(filterRange);
		rangePanel.add(highLabel);
		rangePanel.add(Box.createHorizontalStrut(8));

		// a single swappable row for Filter <-> Stereo Right
		stereoOrFilterLabel = new JLabel("Filter:");
		addRow(form, c, row++, stereoOrFilterLabel, stereoOrFilter);
		// start with filter visible because default is mono
		stereoOrFilter.add(rangePanel, BorderLayout.CENTER);

		// Preamp row (always visible)
		preamp = new Preamp();
		addRow(form, c, row++, new JLabel("Preamp:"), preamp);

		// MIDI controls:
		midiEnabled = new JCheckBox("MIDI", false);
		midiOut = new JComboBox<>(new String[] { "none", "default" });
		midiClock = new JCheckBox("Clock", false);
		JPanel midiPanel = new JPanel();
		midiPanel.setLayout(new BoxLayout(midiPanel, BoxLayout.X_AXIS));
		Gui.resize(midiOut, new Dimension(DEFAULT_DIALOG_SIZE.width - 200, midiOut.getPreferredSize().height));
		midiOut.setAlignmentX(Component.LEFT_ALIGNMENT);
		midiPanel.add(midiOut);
		midiPanel.add(Box.createHorizontalStrut(8));
		midiPanel.add(midiClock);
		addRow(form, c, row++, midiEnabled, midiPanel);

		add(form, BorderLayout.CENTER);

		JPanel buttons = new JPanel();

		ok = new JButton("Create");
		cancel = new JButton("Cancel");
		autosave = new JCheckBox("Autosave", false);
		autosave.setSelected(true);

		// [x] save to the left and buttons to the right
		buttons.setLayout(new BorderLayout());
		JPanel left = new JPanel();
		left.add(autosave);
		buttons.add(left, BorderLayout.WEST);
		JPanel right = new JPanel();
		right.add(ok);
		right.add(cancel);
		buttons.add(right, BorderLayout.EAST);
		add(buttons, BorderLayout.SOUTH);

		// Interactivity: mono/stereo selects right port vs range slider
		mono.addActionListener(e -> updatePortRangeVisibility());
		stereo.addActionListener(e -> updatePortRangeVisibility());
		updatePortRangeVisibility();

		// type selection toggles engine vs ports
		internal.addActionListener(e -> updateModeVisibility());
		external.addActionListener(e -> updateModeVisibility());
		updateModeVisibility();

		// MIDI toggle
		midiEnabled.addActionListener(e -> {
			midiOut.setEnabled(midiEnabled.isSelected());
			midiClock.setEnabled(midiEnabled.isSelected());
		});
		midiOut.setEnabled(false);
		midiClock.setEnabled(false);

		// Range slider updates numeric labels (convert percent -> Hz)
		filterRange.addChangeListener(e -> {
			int lowHz = Math.round(Constants.logarithmic(filterRange.getValue(), MIN_HZ, MAX_HZ));
			int highHz = Math.round(Constants.logarithmic(filterRange.getUpperValue(), MIN_HZ, MAX_HZ));
			lowLabel.setText(lowHz + "Hz");
			highLabel.setText(highHz + "Hz");
		});

		// Query ports (will update combo models on EDT via accept callback)
		if (provider != null)
			provider.query(this);

		// bind Escape to close the dialog when shown
		this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"),
				"closeDialog");
		this.getActionMap().put("closeDialog", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var dlg = ModalDialog.getInstance();
				if (dlg != null)
					dlg.dispose();
			}
		});

		frame = DialogManager.show(this, DEFAULT_DIALOG_SIZE);

		ok.addActionListener(e -> doIt());
		cancel.addActionListener(e -> frame.dispose());

	}

	private void doIt() {
		String nm = name.getText().trim();
		if (nm.isEmpty()) {
			name.requestFocusInWindow();
			return;
		}

		boolean isStereo = stereo.isSelected();
		String l = (String) leftPort.getSelectedItem();
		String r = isStereo ? (String) rightPort.getSelectedItem() : null;
		String midi = (midiEnabled.isSelected()) ? (String) midiOut.getSelectedItem() : null;
		boolean clock = midiEnabled.isSelected() && midiClock.isSelected();
		icons.getSelection();
		// convert percent -> Hz
		int low = Math.round(Constants.logarithmic(filterRange.getValue(), MIN_HZ, MAX_HZ));
		int high = Math.round(Constants.logarithmic(filterRange.getUpperValue(), MIN_HZ, MAX_HZ));
		String engine = internal.isSelected() ? ((Channels.RegisteredSynths) engineBox.getSelectedItem()).name() : null;

		Float pre = preamp.getPreamp();

		Custom user = new Custom(nm, isStereo, onMixer.isSelected(), l, r, midi, selectedIcon.getName(), low, high,
				engine, clock, pre);

		if (autosave.isSelected())
			channels.saveOnCreate();

		if (onOk != null) {
			onOk.accept(user); // embedded: deliver to caller and keep form
		} else {
			// standalone: add directly to mixer then close modal if present
			channels.createChannel(user, autosave.isSelected());
		}
		frame.dispose();

	}

	private void updateModeVisibility() {
		boolean isInternal = internal.isSelected();
		engineBox.setEnabled(isInternal);
		leftPort.setEnabled(!isInternal);
		rightPort.setEnabled(!isInternal && stereo.isSelected());
		filterRange.setEnabled(!isInternal && !stereo.isSelected());
		// update titled border to reflect editing vs new
		var b = (TitledBorder) getBorder();
		if (b != null)
			b.setTitle(isEditing() ? "Edit Channel" : "New Channel");
		updateOkText();
		repaint();
	}

	//swap out filter row for right port row
	private void updatePortRangeVisibility() {
		boolean stereoOn = stereo.isSelected();
		boolean externalMode = external.isSelected();

		rightPort.setEnabled(stereoOn && externalMode);
		filterRange.setEnabled(!stereoOn && externalMode);

		// swap contents of the swappable panel
		stereoOrFilter.removeAll();
		if (stereoOn) {
			stereoOrFilterLabel.setText("Audio Right:");
			stereoOrFilter.add(rightPort, BorderLayout.CENTER);
		} else {
			stereoOrFilterLabel.setText("Filter:");
			stereoOrFilter.add(rangePanel, BorderLayout.CENTER);
		}
		stereoOrFilter.revalidate();
		stereoOrFilter.repaint();

		updateOkText();
	}

	private void updateOkText() {
		// primary button shows Create for new, Update for edit
		if (ok != null)
			ok.setText(isEditing() ? "Update" : "Create");
	}

	private boolean isEditing() {
		// heuristic: titled border indicates edit
		TitledBorder tb = (TitledBorder) getBorder();
		return tb != null && "Edit Channel".equals(tb.getTitle());
	}

	public void populate(Custom original) {
		if (original == null) {
			clearForm();
			return;
		}
		// set titled border to Edit
		var b = (TitledBorder) getBorder();
		if (b != null)
			b.setTitle("Edit Channel");
		name.setText(original.name());
		onMixer.setSelected(original.onMixer());
		stereo.setSelected(original.stereo());
		mono.setSelected(!original.stereo());
		// store pending selections so async port results can pick user's exact ports
		pendingLeftPort = original.leftPort();
		pendingRightPort = original.rightPort();
		pendingMidiPort = original.midiPort();

		// attempt immediate selection against current model (may not contain user's
		// port yet)
		leftPort.setSelectedItem(original.leftPort() == null ? leftPort.getItemAt(0) : original.leftPort());
		rightPort.setSelectedItem(original.rightPort() == null ? rightPort.getItemAt(0) : original.rightPort());
		rightPort.setEnabled(stereo.isSelected());
		midiEnabled.setSelected(original.midiPort() != null && !"none".equalsIgnoreCase(original.midiPort()));
		midiOut.setSelectedItem(original.midiPort() == null ? midiOut.getItemAt(0) : original.midiPort());
		midiOut.setEnabled(midiEnabled.isSelected());
		midiClock.setEnabled(midiEnabled.isSelected());
		midiClock.setSelected(original.clocked());
		if (original.iconName() != null) {
			for (Enumeration<AbstractButton> btns = icons.getElements(); btns.hasMoreElements();) {
				JToggleButton btn = (JToggleButton) btns.nextElement();
				if (original.iconName().equals(btn.getName())) {
					icons.setSelected(btn.getModel(), true);
					selectedIcon = btn;
					break;
				}
			}
		}
		// convert stored Hz -> percent for slider
		int lowHz = (original.lowCutHz() == null) ? 85 : original.lowCutHz();
		int highHz = (original.highCutHz() == null) ? 12000 : original.highCutHz();
		filterRange.setValue(Constants.reverseLog(lowHz, MIN_HZ, MAX_HZ));
		filterRange.setUpperValue(Constants.reverseLog(highHz, MIN_HZ, MAX_HZ));
		// update labels
		lowLabel.setText(Math.round(Constants.logarithmic(filterRange.getValue(), MIN_HZ, MAX_HZ)) + "Hz");
		highLabel.setText(Math.round(Constants.logarithmic(filterRange.getUpperValue(), MIN_HZ, MAX_HZ)) + "Hz");

		engineBox.setSelectedItem(original.engine() == null ? engineBox.getItemAt(0) : original.engine());
		internal.setSelected(original.engine() != null);
		external.setSelected(original.engine() == null);

		// populate preamp value (default to 1.0f if missing)
		preamp.setPreamp(original.preamp() == null ? 1f : original.preamp());

		autosave.setSelected(false);
		updateModeVisibility();
		updatePortRangeVisibility();
		updateOkText();
	}

	public void clearForm() {
		// reset title
		var b = (TitledBorder) getBorder();
		if (b != null)
			b.setTitle("New Channel");
		name.setText("");
		onMixer.setSelected(true);
		stereo.setSelected(true);
		mono.setSelected(false);
		leftPort.setSelectedIndex(0);
		rightPort.setSelectedIndex(0);
		rightPort.setEnabled(true);
		midiEnabled.setSelected(false);
		midiOut.setSelectedIndex(0);
		midiOut.setEnabled(false);
		midiClock.setSelected(false);
		selectedIcon = null;
		icons.clearSelection();
		// reset slider to default Hz mapped into percent
		filterRange.setValue(Constants.reverseLog(85, MIN_HZ, MAX_HZ));
		filterRange.setUpperValue(Constants.reverseLog(12000, MIN_HZ, MAX_HZ));
		filterRange.setEnabled(!stereo.isSelected() && external.isSelected());
		// internal.setSelected(false) etc.
		internal.setSelected(false);
		external.setSelected(true);
		midiClock.setSelected(false);

		// reset preamp to neutral 1.0x
		preamp.setPreamp(1f);

		// clear pending selection state
		pendingLeftPort = null;
		pendingRightPort = null;
		pendingMidiPort = null;

		updateModeVisibility();
		updatePortRangeVisibility();
		updateOkText();
	}

	/** PortsConsumer callback — update combo box models on the EDT. */
	@Override
	public void queried(List<String> audioPorts, List<String> midiPorts) {
		SwingUtilities.invokeLater(() -> setPorts(audioPorts, midiPorts));
	}

	/** Replace the audio/midi combo models. Must be called on EDT. */
	public void setPorts(List<String> audioPorts, List<String> midiPorts) {
		List<String> a = (audioPorts == null || audioPorts.isEmpty()) ? List.of("default") : audioPorts;
		List<String> m = (midiPorts == null || midiPorts.isEmpty()) ? List.of("none", "default") : midiPorts;

		// remember current selections (may be placeholders)
		Object leftSel = leftPort.getSelectedItem();
		Object rightSel = rightPort.getSelectedItem();
		Object midiSel = midiOut.getSelectedItem();

		DefaultComboBoxModel<String> aModel = new DefaultComboBoxModel<>(a.toArray(new String[0]));
		DefaultComboBoxModel<String> aModelR = new DefaultComboBoxModel<>(a.toArray(new String[0]));
		DefaultComboBoxModel<String> mModel = new DefaultComboBoxModel<>(m.toArray(new String[0]));

		leftPort.setModel(aModel);
		rightPort.setModel(aModelR);
		midiOut.setModel(mModel);

		// Prefer pending selections (from populate/edit mode) if they exist and match
		// the new lists.
		// Otherwise fall back to previously selected items if still available.
		if (pendingLeftPort != null && a.contains(pendingLeftPort)) {
			leftPort.setSelectedItem(pendingLeftPort);
		} else if (leftSel != null && a.contains(leftSel.toString())) {
			leftPort.setSelectedItem(leftSel);
		} else {
			leftPort.setSelectedIndex(0);
		}

		if (pendingRightPort != null && a.contains(pendingRightPort)) {
			rightPort.setSelectedItem(pendingRightPort);
		} else if (rightSel != null && a.contains(rightSel.toString())) {
			rightPort.setSelectedItem(rightSel);
		} else {
			rightPort.setSelectedIndex(0);
		}

		if (pendingMidiPort != null && m.contains(pendingMidiPort)) {
			midiOut.setSelectedItem(pendingMidiPort);
		} else if (midiSel != null && m.contains(midiSel.toString())) {
			midiOut.setSelectedItem(midiSel);
		} else {
			midiOut.setSelectedIndex(0);
		}

		// clear pending selections after applying them once
		pendingLeftPort = null;
		pendingRightPort = null;
		pendingMidiPort = null;

		rightPort.setEnabled(stereo.isSelected());
	}

	private static void addRow(JPanel panel, GridBagConstraints c, int row, JComponent label,
			java.awt.Component field) {
		c.gridy = row;
		c.gridx = 0;
		c.weightx = 0.0;
		panel.add(label, c);
		c.gridx = 1;
		c.weightx = 1.0;
		panel.add(field, c);
	}

}