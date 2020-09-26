package net.judah.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import lombok.extern.log4j.Log4j;
import net.judah.song.Edits;
import net.judah.song.Edits.Copyable;

@Log4j
public class PopupMenu extends JPopupMenu {

	private final List<Copyable> clipboard = new ArrayList<Copyable>();
	
    JMenuItem copy = new JMenuItem("Copy");
    JMenuItem cut = new JMenuItem("Cut");
    JMenuItem paste = new JMenuItem("Paste");
    JMenuItem add = new JMenuItem("New");
    JMenuItem delete = new JMenuItem("Delete");
    
	public PopupMenu(Edits client) {

		copy.addActionListener( (event) -> stash(client.copy()) );
		add(copy);
		cut.addActionListener( (event) -> stash(client.cut()) );
		add(cut);
		paste.addActionListener( (event) -> client.paste(clipboard) );
		add(paste);
		add.addActionListener( (event) -> client.editAdd() );
		add(add);
		delete.addActionListener( (event) -> client.editDelete() );
		add(delete);
	}

	private void stash(List<Copyable> source) {
		if (source != null && !source.isEmpty()) {
			clipboard.clear();
			try {
				for (Copyable o : source) 
					clipboard.add(o.clone());
			} catch (CloneNotSupportedException e) {
				log.error(e.getMessage(), e);
				
			}
		}
	}
	
}

