package net.judah.drums.fm;

/** 32 Yamaha DX7 algorithms: static routing definitions for 6-operator FM. */
public class DX7 {

	public static final int OP_COUNT = 6;
	private static final Algorithm[] CACHE = new Algorithm[32];

	public static Algorithm get(int preset) {
		if (preset < 1 || preset > 32) throw new IllegalArgumentException("Alg 1-32");
		if (CACHE[preset - 1] == null) CACHE[preset - 1] = build(preset);
		return CACHE[preset - 1];
	}

	private static Algorithm build(int preset) {
		return switch (preset) {
		case 1 -> // 6->5->4->3->2->1, FB on 6
			define(new int[][]{ {1}, {2}, {3}, {4}, {5}, {5} });
		case 2 -> // 6->5, 5->(4,3), 4->2, 3->2, 2->1, FB on 6
			define(new int[][]{ {1}, {2, 3}, {4}, {4}, {5}, {5} });
		case 3 -> // 6->5, 5->4, 4->3, 3->(2,1), FB on 6
			define(new int[][]{ {2}, {2}, {3}, {4}, {5}, {5} });
		case 4 -> // 6->5, 5->4, 4->(3,2,1), FB on 6
			define(new int[][]{ {3}, {3}, {3}, {4}, {5}, {5} });
		case 5 -> // 6->5->(2,1), 4->3->(2,1), FB on 6
			define(new int[][]{ {1, 2}, {4}, {4}, {5}, {5}, {5} });
		case 6 -> // 6->(5,4,3,2), 5->1, 4->1, 3->1, 2->1, FB on 6
			define(new int[][]{ {1, 2, 3, 4}, {5}, {5}, {5}, {5}, {5} });
		case 7 -> // (6,5,4)->3, (3,2)->1, FB on 6
			define(new int[][]{ {1, 2}, {2}, {3, 4, 5}, {}, {}, {5} });
		case 8 -> // (6,5)->4, (4,3)->2, (2,1)->out, FB on 6
			define(new int[][]{ {1}, {}, {3}, {4, 5}, {}, {5} });
		case 9 -> // 6->5->4->2->1, 3->1, FB on 6
			define(new int[][]{ {1, 2}, {3}, {}, {4}, {5}, {5} });
		case 10 -> // 6->5, 5->(4,2), 4->3, 2->1, FB on 6
			define(new int[][]{ {1}, {4}, {3}, {4}, {5}, {5} });
		case 11 -> // Series 3 stacks: (6->5),(4->3),(2->1), FB on 6
			define(new int[][]{ {1}, {}, {3}, {}, {5}, {5} });
		case 12 -> // 6->(5,4,3), (5,4,3)->2, 2->1, FB on 6
			define(new int[][]{ {1}, {2, 3, 4}, {5}, {5}, {5}, {5} });
		case 13 -> // 6->(5,4,3), 5->2, 4->2, 3->1, FB on 6
			define(new int[][]{ {2}, {3, 4}, {5}, {5}, {5}, {5} });
		case 14 -> // 6->5->4, 4->(2,1), 3->(2,1), FB on 6
			define(new int[][]{ {2, 3}, {2, 3}, {}, {4}, {5}, {5} });
		case 15 -> // 6->5, 5->2, 4->3, (4,3,2)->1, FB on 6? (Image: FB on 6)
			define(new int[][]{ {1, 2, 3}, {4}, {}, {4}, {5}, {5} });
		case 16 -> // (6,5)->4->1, (3,2)->1, FB on 6
			define(new int[][]{ {1, 3}, {2}, {}, {4, 5}, {}, {5} });
		case 17 -> // (6,5)->4->1, (3,2)->1, FB on 6 (Double branch)
			define(new int[][]{ {1, 3}, {2}, {}, {4, 5}, {}, {5} });
		case 18 -> // 6->5->(4,3,2)->1, FB on 6
			define(new int[][]{ {1, 2, 3}, {4}, {4}, {4}, {5}, {5} });
		case 19 -> // 6->(5,4)->3->1, 2->1, FB on 6
			define(new int[][]{ {1, 2}, {}, {3, 4}, {5}, {5}, {5} });
		case 20 -> // 6->5, 4->3, 2->1 (All Carriers), 5->(4,2), FB on 6
			define(new int[][]{ {1}, {4}, {3}, {4}, {5}, {5} });
		case 21 -> // 6->(5,4,3,2)->carriers, FB on 6
			define(new int[][]{ {5}, {5}, {5}, {5}, {5}, {5} });
		case 32 -> // Image 32: 6 Parallel Carriers
			define(new int[][]{ {}, {}, {}, {}, {}, {5} });
		default -> // Placeholder for 22-31
			define(new int[][]{ {}, {}, {}, {}, {}, {5} });
		};
	}

	private static Algorithm define(int[][] modulators) {
		float[][] scales = new float[OP_COUNT][];
		boolean[][] fb = new boolean[OP_COUNT][];
		for (int i = 0; i < OP_COUNT; i++) {
			scales[i] = new float[modulators[i].length];
			fb[i] = new boolean[modulators[i].length];
			for (int j = 0; j < modulators[i].length; j++) {
				scales[i][j] = 1.0f;
				if (modulators[i][j] == i) fb[i][j] = true;
			}
		}
		return Topology.build(OP_COUNT, modulators, scales, fb);
	}

	public static final String[] names = {
	    "Series6", "DualCars", "DualCar3", "TriCars", "SerialMod", "SingleCar", "QuadCars", "SelfFbOp",
	    "FbOpOne", "Chain3", "PureCar", "ModBoth", "ModTree", "SplitCar", "FbSerial", "ThreeCar",
	    "HeavyFb", "DualStack", "ModClust", "FbAndCar", "Serial4", "FourCars", "ClusterA", "MixMods",
	    "Par3Car", "FbRing", "DualWide", "MixFb", "Nested4", "StarMod", "CrossMod", "AllToOne"
	};

	public static final String[] descriptions = {
	    "1: 6-Op Series Stack (Carrier 1)",
	    "2: 6-Op Series Branched (Carrier 1)",
	    "3: 4-Op Stack split to Dual Carriers (1,2)",
	    "4: 3-Op Stack split to Triple Carriers (1,2,3)",
	    "5: Three Parallel 2-Op Stacks (Carrier 1)",
	    "6: Three Parallel 2-Op Stacks (FB, Carrier 1)",
	    "7: 4-Op Shared Modulator into Dual Carriers (1,2)",
	    "8: Dual 3-Op Stacks (Carrier 1,2)",
	    "9: Dual 3-Op Stacks (Shared FB, Carrier 1,2)",
	    "10: Two 3-Op Stacks with Shared root (Carrier 1,3)",
	    "11: Three 2-Op Stacks (Shared FB, Carrier 1,2,4)",
	    "12: Five Parallel Modulators into Carrier 1",
	    "13: Five Parallel Modulators (Shared FB) into Carrier 1",
	    "14: Dual Carrier Stacks with Parallel Mods (1,3)",
	    "15: Dual Carrier Stacks (Shared FB, 1,3)",
	    "16: Single Carrier with Two 2-Op Mod Stacks",
	    "17: Single Carrier with Two 2-Op Mod Stacks (FB)",
	    "18: Single Carrier with Three 2-Op Mod Stacks",
	    "19: Triple Carriers with Single Modulators",
	    "20: Triple Carriers with Single Modulators (Alt)",
	    "21: Triple Carriers with Single Modulators (FB)",
	    "22: Single Carrier modulated by 5-Op Stack",
	    "23: Three Carriers (1,2,3) with Shared Mods",
	    "24: Three Carriers (1,2,3) with Shared Mods (Alt)",
	    "25: Three Carriers (1,2,3) with Shared Mods (Parallel)",
	    "26: Three Parallel 2-Op Stacks into Three Carriers",
	    "27: Three Parallel 2-Op Stacks (Shared FB)",
	    "28: Four Carriers with single 2-Op stack",
	    "29: Five Carriers with Single Modulator",
	    "30: Five Carriers with Shared Modulator",
	    "31: Five Carriers (One with Feedback)",
	    "32: Six Parallel Sine Carriers"
	};
}
