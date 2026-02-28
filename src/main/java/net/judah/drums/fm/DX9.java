package net.judah.drums.fm;

import lombok.Getter;

/** DX-9 4-operator style, 4 additional algos from Reface DX. */
public class DX9 {

	public static final int OP_COUNT = 4;

	public static final String[] names = {
			"Series", "Stack", "Dual", "4Car",
			"FbCar", "EzPair", "Mod3", "Split2",
			"Alt4", "Ring4", "Single", "Chain"
		};
	public static final int ALGO_COUNT = names.length;

	@Getter private static final Algorithm[] algorithms = new Algorithm[ALGO_COUNT];



	static {

		// 1: Series chain 3 -> 2 -> 1 -> 0 (single carrier op0)
		algorithms[0] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{1}, {2}, {3}, {}},
				new float[][]{{1f}, {1f}, {1f}, {}});

		// 2: Single carrier (op0) with three parallel modulators (op1,op2,op3)
		algorithms[1] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{1, 2, 3}, {}, {}, {}},
				new float[][]{{1f, 1f, 1f}, {}, {}, {}});

		// 3: Two carriers (op0,op1) both modulated by op2+op3 (shared mods)
		algorithms[2] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{2, 3}, {2, 3}, {}, {}},
				new float[][]{{1f, 1f}, {1f, 1f}, {}, {}});

		// 4: Four parallel carriers (no modulators)
		algorithms[3] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{}, {}, {}, {}},
				new float[][]{{}, {}, {}, {}});

		// 5: Carrier op0 modulated by op3 which also self-feedbacks (fb on op3)
		algorithms[4] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{3}, {}, {}, {3}},
				new float[][]{{1f}, {}, {}, {1f}},
				new boolean[][]{{true}, {}, {}, {true}});

		// 6: Simple pair: op1 -> op0 (carrier), ops 2+3 independent
		algorithms[5] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{1}, {}, {}, {}},
				new float[][]{{1f}, {}, {}, {}});

		// 7: Single carrier op0 with three parallel modulators
		algorithms[6] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{1, 2, 3}, {}, {}, {}},
				new float[][]{{1f, 1f, 1f}, {}, {}, {}});

		// 8: Split carriers: op0 <- op2, op1 <- op3
		algorithms[7] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{2}, {3}, {}, {}},
				new float[][]{{1f}, {1f}, {}, {}});

		// 9: Series chain 3 -> 2 -> 1 -> 0 (full cascade)
		algorithms[8] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{1}, {2}, {3}, {}},
				new float[][]{{1f}, {1f}, {1f}, {}});

		// 10: Ring-like cross-mod: op0<-op1, op1<-op2, op2<-op3, op3<-op0
		algorithms[9] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{1}, {2}, {3}, {0}},
				new float[][]{{1f}, {1f}, {1f}, {1f}},
				new boolean[][]{{false}, {false}, {false}, {true}});

		// 11: Single carrier (op0) with ops 1..3 as parallel modulators.
	    // Evaluate modulators before the carrier so their outputs are available.
	    algorithms[10] = new Algorithm(new int[]{3, 2, 1, 0},
	            new int[][]{{1, 2, 3}, {}, {}, {}},
	            new float[][]{{1f, 1f, 1f}, {}, {}, {}});

		// 12: Heavy feedback chain: op3 self-fb -> op2 -> op1 -> op0
		algorithms[11] = new Algorithm(new int[]{3, 2, 1, 0},
				new int[][]{{1}, {2}, {3}, {3}},
				new float[][]{{1f}, {1f}, {1f}, {1f}},
				new boolean[][]{{false}, {false}, {false}, {true}});
	}

	public static int indexOf(Algorithm algo) {
		for (int i = 0; i < algorithms.length; i++)
			if (algorithms[i].equals(algo))
				return i;
		return -1;
	}

	public static final String[] descriptions = {
		"Series chain (4 → 3 → 2 → 1 carrier)",
		"Single carrier (1) with three parallel mods (2–4)",
		"Two carriers (1+2) both modulated by 3+4",
		"Four parallel carriers (independent 1–4)",
		"Carrier 1 modulated by 4; 4 self-feedback",
		"Simple pair (2→1 carrier); 3–4 independent",
		"Single carrier with three mods; reduced level on 2",
		"Split: 1 from 3, 2 from 4 (two independent pairs)",
		"Series chain (4→3→2→1 carrier, alternate eval)",
		"Cross-modulation ring (1←2←3←4←1)",
		"Pure single operator carrier (1 only)",
		"Heavy feedback on 4 feeding down to carrier 1"
	};

}
