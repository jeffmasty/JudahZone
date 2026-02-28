package net.judah.midi;

import java.util.Set;

import javax.sound.midi.ShortMessage;

import judahzone.gui.Updateable;
import judahzone.prism.PrismRT;
import judahzone.prism.PrismUI;

public interface Notes extends Updateable {

	int getChannel();

	@PrismRT
	void removeData1(int data1);

	int indexOf(int data1);

	boolean isPedal();

	/** NOTES_X: notes laid out horizontally (left to right). */
	@PrismUI
	void data1(Set<Integer> ref);


	@PrismUI
	boolean receive(ShortMessage msg);



}
