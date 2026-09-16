package io.github.hunter1712.infusedmobs.command;

import java.util.List;

/**
 * Typo hints for unknown Ability ids.
 * Exact prefix wins immediately, otherwise closest edit distance within two.
 */
final class FuzzyMatcher {
    private FuzzyMatcher() {}

    static String closest(String input, List<String> candidates) {
        if (input == null || candidates == null || candidates.isEmpty()) return null;
        String lower = input.toLowerCase();
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            String cLower = candidate.toLowerCase();
            if (cLower.startsWith(lower)) return candidate;
            int dist = distance(lower, cLower);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return bestDist <= 2 ? best : null;
    }

    static int distance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            }
        }
        return dp[a.length()][b.length()];
    }
}
