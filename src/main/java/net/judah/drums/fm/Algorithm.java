package net.judah.drums.fm;

public record Algorithm(int[] evalOrder,
                        int[][] modulatorsFor,
                        float[][] scaleFor,
                        boolean[][] feedbackFlags) {

    public Algorithm(int[] evalOrder,
            int[][] modulatorsFor,
            float[][] scaleFor) {
        this(evalOrder, modulatorsFor, scaleFor, null);
    }

    public Algorithm {
        if (evalOrder == null || modulatorsFor == null || scaleFor == null)
            throw new NullPointerException("Algorithm components must not be null");
        evalOrder = evalOrder.clone();
        modulatorsFor = deepCopy(modulatorsFor);
        scaleFor = deepCopy(scaleFor);
        feedbackFlags = feedbackFlags == null ? null : deepCopy(feedbackFlags);
        if (modulatorsFor.length != scaleFor.length ||
            (feedbackFlags != null && feedbackFlags.length != modulatorsFor.length))
            throw new IllegalArgumentException("shape mismatch");
    }

    private static int[][] deepCopy(int[][] src) {
        int[][] dst = new int[src.length][];
        for (int i = 0; i < src.length; i++)
            dst[i] = (src[i] == null) ? new int[0] : src[i].clone();
        return dst;
    }

    private static float[][] deepCopy(float[][] src) {
        float[][] dst = new float[src.length][];
        for (int i = 0; i < src.length; i++)
            dst[i] = (src[i] == null) ? new float[0] : src[i].clone();
        return dst;
    }

    private static boolean[][] deepCopy(boolean[][] src) {
        boolean[][] dst = new boolean[src.length][];
        for (int i = 0; i < src.length; i++)
            dst[i] = (src[i] == null) ? new boolean[0] : src[i].clone();
        return dst;
    }

}
