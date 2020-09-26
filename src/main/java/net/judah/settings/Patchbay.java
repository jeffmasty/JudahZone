package net.judah.settings;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.mixer.MixerPort.PortDescriptor;

/*													  (R/O)	
 		output client/port  --->  input client/port    midi  startup [remove] 
 		pedal					  judahzone             x      x
 		keyboard				  judahzone             x      x
 		sys_1                     judahzone                    x
 		sys_2                     judahzone                    x
 		                                                       x     [connect]
 */
@Getter @RequiredArgsConstructor
public class Patchbay {
	
	private final String clientName;
	private final List<PortDescriptor> ports;
	private final List<Patch> connections;
	
//	/** copy constructor TODO */
//	public Patchbay(Patchbay patchbay) {
//		this.clientName = patchbay.clientName;
//		this.ports = patchbay.ports;
//		this.connections = patchbay.connections;
//	}
}
