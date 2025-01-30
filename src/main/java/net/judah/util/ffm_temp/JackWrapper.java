package net.judah.util.ffm_temp;

public class JackWrapper {
    static final String JACK_LIBRARY = "libjack.so.0"; // Name of the JACK library

    public static void main(String[] args) throws Throwable {
        // Load the native library

//    	SymbolLookup lookup = SymbolLookup.libraryLookup(JACK_LIBRARY, (Arena)null);


//        SymbolLookup nativeLookup = Linker.nativeLinker().defaultLookup();
//        SymbolLookup.libraryLookup("howdy", (Arena)null);
//
//        // Lookup the 'jack_client_open' function
//        SymbolLookup symbolLookup = nativeLookup.lookup("jack_client_open").orElseThrow();
//
//        // Define the method type for jack_client_open
//        MethodType methodType = MethodType.methodType(MemorySegment.class, MemorySegment.class, int.class, MemorySegment.class);
//
//        // Get a method handle for jack_client_open
//        MethodHandle jackClientOpenHandle = Linker.nativeLinker().downcallHandle(
//                symbolLookup,
//                methodType,
//                FunctionDescriptor.of(MemorySegment.class, MemorySegment.class, int.class, MemorySegment.class)
//        );
//
//        // Call jack_client_open
//        String clientName = "JavaClient";
//        int options = 0; // Use appropriate options for your use case
//
//        // Create a segment allocator
//        SegmentAllocator allocator = SegmentAllocator.nativeAllocator();
//
//        try (MemorySegment status = allocator.allocate(4);
//             MemorySegment clientNameSegment = allocator.allocateUtf8String(clientName)) {
//
//            MemorySegment clientAddress = (MemorySegment) jackClientOpenHandle.invoke(clientNameSegment, options, status);
//            if (clientAddress != MemorySegment.NULL) {
//                System.out.println("JACK client opened successfully.");
//                // Proceed with activating the client and other operations
//            } else {
//                System.err.println("Failed to open JACK client.");
//            }
//        }
    }
}