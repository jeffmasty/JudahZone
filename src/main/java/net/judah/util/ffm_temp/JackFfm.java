package net.judah.util.ffm_temp;

public class JackFfm {
    public static void main(String[] args) throws Throwable {
        // Load JACK library
//        SymbolLookup nativeLookup = Linker.nativeLinker().defaultLookup();
//        SegmentAllocator allocator = SegmentAllocator.nativeAllocator();
//        SegmentAllocator autoAllocator = (byteSize, byteAlignment) -> Arena.ofAuto().allocate(byteSize, byteAlignment);



//        // Lookup jack_client_open
//        MethodHandle jackClientOpenHandle = Linker.nativeLinker().downcallHandle(
//                nativeLookup.lookup("jack_client_open").orElseThrow(),
//                MethodType.methodType(MemorySegment.class, MemorySegment.class, int.class, MemorySegment.class),
//                FunctionDescriptor.of(MemorySegment.class, MemorySegment.class, int.class, MemorySegment.class)
//        );
//
//        // Lookup jack_activate
//        MethodHandle jackActivateHandle = Linker.nativeLinker().downcallHandle(
//                nativeLookup.lookup("jack_activate").orElseThrow(),
//                MethodType.methodType(int.class, MemorySegment.class),
//                FunctionDescriptor.of(int.class, MemorySegment.class)
//        );
//
//        // Lookup jack_client_close
//        MethodHandle jackClientCloseHandle = Linker.nativeLinker().downcallHandle(
//                nativeLookup.lookup("jack_client_close").orElseThrow(),
//                MethodType.methodType(void.class, MemorySegment.class),
//                FunctionDescriptor.ofVoid(MemorySegment.class)
//        );
//
//        // Measure jack_client_open
//        String clientName = "JavaClient";
//        MemorySegment clientNameSegment = allocator.allocateUtf8String(clientName);
//        MemorySegment status = allocator.allocate(4);
        long startTime = System.nanoTime();
//        MemorySegment client = (MemorySegment) jackClientOpenHandle.invoke(clientNameSegment, 0, status);
        long endTime = System.nanoTime();
//        System.out.println("FFM jack_client_open time: " + (endTime - startTime) + " ns");

        // Measure jack_activate
        startTime = System.nanoTime();
//        jackActivateHandle.invoke(client);
//        endTime = System.nanoTime();
//        System.out.println("FFM jack_activate time: " + (endTime - startTime) + " ns");

        // Measure jack_client_close
 //       startTime = System.nanoTime();
        // jackClientCloseHandle.invoke(client);
 //       endTime = System.nanoTime();
        System.out.println("FFM jack_client_close time: " + (endTime - startTime) + " ns");
    }
}