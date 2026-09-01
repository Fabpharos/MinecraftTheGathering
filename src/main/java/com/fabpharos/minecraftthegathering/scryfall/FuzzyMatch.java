package com.fabpharos.minecraftthegathering.scryfall;

/**
 * A small fzf-style fuzzy text matcher, shared by anything that needs to pick "the local pool entry whose
 * name best resembles what the user typed" - card names ({@link MagicCardPool}), set names
 * ({@link MagicSetPool}), and so on.
 */
public final class FuzzyMatch {
    private FuzzyMatch() {
    }

    /**
     * Scores how well {@code candidate} matches {@code query} (both assumed already lowercased). Higher is
     * a better match; exact, prefix, and substring matches always outscore a fuzzy one. Negative means
     * "not even a fuzzy (subsequence) match" - {@code query}'s characters don't all appear, in order, in
     * {@code candidate} at all.
     */
    public static int score(String query, String candidate) {
        if (candidate.equals(query)) {
            return 10_000;
        }
        if (candidate.startsWith(query)) {
            return 9_000 - (candidate.length() - query.length());
        }

        int substringIndex = candidate.indexOf(query);
        if (substringIndex >= 0) {
            return 7_000 - substringIndex;
        }

        return subsequenceScore(query, candidate);
    }

    // fzf-style subsequence match: every character of the query must appear in candidate, in the same
    // order, though not necessarily contiguously. Rewards runs of consecutive matches so e.g. "bolt"
    // scores higher against "Lightning Bolt" than a query whose letters are scattered further apart.
    // Returns -1 if query isn't a subsequence of candidate at all.
    private static int subsequenceScore(String query, String candidate) {
        int queryIndex = 0;
        int score = 0;
        int consecutive = 0;
        for (int i = 0; i < candidate.length() && queryIndex < query.length(); i++) {
            if (candidate.charAt(i) == query.charAt(queryIndex)) {
                queryIndex++;
                consecutive++;
                score += consecutive;
            } else {
                consecutive = 0;
            }
        }

        return queryIndex == query.length() ? score : -1;
    }
}
