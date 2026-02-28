package net.judah.drums.fm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import judahzone.prism.PrismUI;

// “geometry” (operator counts, per-operator mod lists, index/scale values, feedback flags)
// build the graph (runs a topo‑sort (Kahn) ignoring explicit feedback edges)
// validate the graph (detect cycles, allow explicit one‑sample feedback)
// publish the graph (thread-safe).

/** validate adjacency, allow marked feedback edges, compute evalOrder. */
public final class Topology {

	/** Build an immutable Algorithm from compact description.
   		modulatorsFor[dst] = list of src indices that modulate dst.
	   	scaleFor must match modulatorsFor shape.
	   	feedbackFlags may be null; if provided, same shape as modulatorsFor and true
	   	means "this edge is a feedback edge" (ignored for topological sort). */
	public static Algorithm build(int operatorCount,
                             int[][] modulatorsFor,
                             float[][] scaleFor,
                             boolean[][] feedbackFlags) {
		Objects.requireNonNull(modulatorsFor);
		Objects.requireNonNull(scaleFor);
		if (operatorCount <= 0)
			throw new IllegalArgumentException("operatorCount>0");
		if (modulatorsFor.length != operatorCount || scaleFor.length != operatorCount)
			throw new IllegalArgumentException("shape mismatch");

		// Basic shape checks
		for (int dst = 0; dst < operatorCount; dst++) {
			if (modulatorsFor[dst].length != scaleFor[dst].length)
				throw new IllegalArgumentException("scaleFor must match modulatorsFor shape");
			if (feedbackFlags != null && feedbackFlags[dst].length != modulatorsFor[dst].length)
				throw new IllegalArgumentException("feedbackFlags shape mismatch");
		}

		// If caller didn't provide feedback flags, synthesize them:
		// mark any self-modulation edge (src == dst) as feedback by default.
		boolean[][] fb = feedbackFlags;
		if (fb == null) {
			fb = new boolean[operatorCount][];
			for (int dst = 0; dst < operatorCount; dst++) {
				int len = modulatorsFor[dst].length;
				fb[dst] = new boolean[len];
				for (int j = 0; j < len; j++)
					fb[dst][j] = (modulatorsFor[dst][j] == dst);
			}
		}

		int[] evalOrder = topoSort(operatorCount, modulatorsFor, fb);
		// Return compact Algorithm (immutable by convention)
		return new Algorithm(evalOrder, modulatorsFor, scaleFor, fb);
	}

//Kahn's algorithm: treat edges marked as feedback as absent for ordering.
	private static int[] topoSort(int n, int[][] modulatorsFor, boolean[][] feedbackFlags) {
		int[] indeg = new int[n];
		// build adjacency lists (src -> list of dst)
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] adj = new ArrayList[n];
		for (int i = 0; i < n; i++)
			adj[i] = new ArrayList<>();

		for (int dst = 0; dst < n; dst++) {
			int[] srcs = modulatorsFor[dst];
			for (int j = 0; j < srcs.length; j++) {
				int src = srcs[j];
				if (src < 0 || src >= n)
					throw new IllegalArgumentException("invalid index");
				boolean isFb = (feedbackFlags != null && feedbackFlags[dst][j]);
				if (isFb)
					continue; // ignore feedback edges for topo ordering
				adj[src].add(dst);
				indeg[dst]++;
			}
		}

		Deque<Integer> q = new ArrayDeque<>();
		for (int i = 0; i < n; i++)
			if (indeg[i] == 0)
				q.add(i);

		int[] order = new int[n];
		int idx = 0;
		while (!q.isEmpty()) {
			int u = q.remove();
			order[idx++] = u;
			for (int v : adj[u])
				if (--indeg[v] == 0)
					q.add(v);
		}

		if (idx < n) {
			// Remaining nodes are part of cycles (not explained by feedbackFlags)
			throw new IllegalArgumentException("graph contains cycle(s) not marked as feedback");
		}
		return order;
	}

	/*
	 * Return true if opIndex does not modulate any other operator (no outgoing
	 * edges).
	 */
	public static boolean isCarrier(Algorithm algo, int opIndex) {
		int[][] mods = algo.modulatorsFor();
		if (mods == null)
			return true; // defensive
		for (int dst = 0; dst < mods.length; dst++) {
			int[] srcs = mods[dst];
			if (srcs == null)
				continue;
			for (int s : srcs)
				if (s == opIndex)
					return false;
		}
		return true; // never found as a source -> carrier }
	}

	public static boolean isModulator(Algorithm algo, int opIndex) {
		return !isCarrier(algo, opIndex);
	}

	// @PrismUI section can mutate the graph
  ///////////////////////
	/* non-RT Algorithm validator*/
	@PrismUI
	/** @return list of validation errors */
	public static List<String> validate(Algorithm a, int opCount) {
	    List<String> errors = new ArrayList<String>();
	    if (a == null) { errors.add("Algorithm is null"); return errors; }

	    // evalOrder indices
	    for (int i = 0; i < a.evalOrder().length; i++) {
	        int idx = a.evalOrder()[i];
	        if (idx < 0 || idx >= opCount)
	            errors.add("evalOrder[" + i + "] = " + idx + " out of range 0.." + (opCount-1));
	    }

	    // modulators and scales
	    int outer = a.modulatorsFor().length;
	    for (int dst = 0; dst < outer; dst++) {
	        int[] srcs = a.modulatorsFor()[dst];
	        float[] scales = a.scaleFor().length > dst ? a.scaleFor()[dst] : null;
	        if (scales == null)
	            errors.add("scaleFor missing for dst=" + dst);
	        else if (srcs.length != scales.length)
	            errors.add("mismatch modulators/scale length for dst=" + dst + " (" + srcs.length + " vs " + scales.length + ")");

	        for (int j = 0; j < srcs.length; j++) {
	            int s = srcs[j];
	            if (s < 0 || s >= opCount)
	                errors.add("modulatorsFor[" + dst + "][" + j + "] = " + s + " out of range 0.." + (opCount-1));
	        }
	    }

	    // optional: detect duplicates in evalOrder (warn) and missing operators (warn)
	    boolean[] seen = new boolean[opCount];
	    for (int v : a.evalOrder()) if (v >= 0 && v < opCount) seen[v] = true;
	    for (int i = 0; i < opCount; i++) if (!seen[i]) errors.add("warning: operator " + i + " not present in evalOrder (may be unused)");

	    return errors;
	}


}

///** Compute topological evaluation order and carrier set from modulation graph. */
//public static Algorithm compute(int[][] modulatorsFor, float[][] scaleFor) {
//	// invert modulatorsFor (dst<-src) to targetsFor[src] = list of destinations that src modulates
//	// simple builder: count targets per source
//	// topological sort (DFS: mods before consumers). small graph, recursion is fine.
//	// Identify carriers: ops that do not modulate any other op (no outgoing targets)
//	// carriers are !hasOutgoing (left to Algorithm for storage if desired)
//	return new Algorithm(evalOrder, modulatorsFor, scaleFor);
//private static int dfs(int op, int[][] targetsFor, boolean[] visited, int[] evalOrder, int evalIndex) {
