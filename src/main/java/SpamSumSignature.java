/*
 * Copyright (C) 2013 Marius Mailat http://fastlink2.com/contact.htm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified levenshtein distance calculation
 * <p/>
 * This program can be used, redistributed or modified under any of
 * Boost Software License 1.0, GPL v2 or GPL v3
 * See the file COPYING for details.
 * <p/>
 * $Id$
 * <p/>
 * Copyright (C) 2014 kikairoya <kikairoya@gmail.com>
 * Copyright (C) 2014 Jesse Kornblum <research@jessekornblum.com>
 */

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SpamSumSignature {

    public static final int SPAMSUM_LENGTH = 64;
    public static final int REMOVE_COST = 1;
    public static final int INSERT_COST = 1;
    public static final int REPLACE_COST = 2;
    public static final int MIN_BLOCKSIZE = 3;
    public static final int ROLLING_WINDOW = 7;

    private long blockSize;
    private byte[] hash1;
    private byte[] hash2;
    private boolean deDup;

    public SpamSumSignature(long blockSize, byte[] hash1, byte[] hash2, boolean deDup) {
        this.blockSize = blockSize;
        this.deDup = deDup;
        this.hash1 = deDup ? eliminateSequences(hash1) : hash1;
        this.hash2 = deDup ? eliminateSequences(hash2) : hash2;
    }

    public SpamSumSignature(long blockSize, byte[] hash1, byte[] hash2) {
        this(blockSize, hash1, hash2, false);
    }

    public SpamSumSignature(String[] signature, boolean deDup) {
        this(Long.parseUnsignedLong(signature[0]), signature[1].getBytes(), signature[2].getBytes(), deDup);
    }

    public SpamSumSignature(String[] signature) {
        this(signature, false);
    }

    public SpamSumSignature(String signature, boolean deDup) {
        this(signature.split(":"), deDup);
    }

    public SpamSumSignature(String signature) {
        this(signature, false);
    }

    private static class RollingState {
        public int[] windows;
        public long h1, h2, h3;
        public long n;

        public RollingState() {
            windows = new int[ROLLING_WINDOW];
            h1 = h2 = h3 = n = 0;
        }

        public long rollHash(int c) {

            this.h2 = this.h2 - this.h1;
            this.h2 = this.h2 + (ROLLING_WINDOW * c);

            this.h1 = this.h1 + c;
            this.h1 = this.h1 - this.windows[(int) (Long.remainderUnsigned(this.n, ROLLING_WINDOW))];
            this.windows[(int) (Long.remainderUnsigned(this.n, ROLLING_WINDOW))] = c;
            this.n++;
            if (this.n == ROLLING_WINDOW)
                this.n = 0;
            // The original spamsum AND'ed this value with 0xFFFFFFFF which
            // in theory should have no effect. This AND has been removed for performance
            this.h3 = this.h3 << 5;
            this.h3 = this.h3 ^ c;
            //this.h3 = (this.h3 ^ c) & 0xffffffffL;

            return this.h1 + this.h2 + this.h3;
        }

    }

    private static boolean hasCommonSubstring(byte[] s1, byte[] s2) {
        int i, j;
        int numHashes;
        long[] hashes = new long[SPAMSUM_LENGTH];

        // there are many possible algorithms for common substring
        // detection. In this case I am re-using the rolling hash code
        // to act as a filter for possible substring matches
        RollingState rollState = new RollingState();

        // first compute the windowed rolling hash at each offset in the first string
        for (i = 0; i < s1.length; i++) {
            hashes[i] = rollState.rollHash(s1[i]);
        }
        numHashes = i;

        rollState = new RollingState();

        // now for each offset in the second string compute the rolling hash and compare it to all of the rolling hashes
        // for the first string. If one matches then we have a
        // candidate substring match. We then confirm that match with a direct string comparison

        for (i = 0; i < s2.length; i++) {
            long h = rollState.rollHash(s2[i]);
            if (i < ROLLING_WINDOW - 1) continue;
            for (j = ROLLING_WINDOW - 1; j < numHashes; j++) {
                if (hashes[j] == h)
                {
                    if (s2.length >= i + 1) {

                        byte[] tmp2 = Arrays.copyOfRange(s2, i - ROLLING_WINDOW + 1, i + 1);
                        byte[] tmp1 = Arrays.copyOfRange(s1, j - ROLLING_WINDOW + 1, j + 1);
                        if (Arrays.equals(tmp1, tmp2)) return true;
                    }

                }
            }
        }

        return false;
    }


    private static int editDistance(byte[] s1, byte[] s2) {

        int[][] t = new int[2][SPAMSUM_LENGTH+1];
        int[] temp;

        int i1, i2;
        for (i2 = 0; i2 <= s2.length; i2++)
            t[0][i2] = i2 * REMOVE_COST;
        for (i1 = 0; i1 < s1.length; i1++) {
            t[1][0] = (i1 + 1) * INSERT_COST;
            for (i2 = 0; i2 < s2.length; i2++) {
                int cost_a = t[0][i2+1] + INSERT_COST;
                int cost_d = t[1][i2] + REMOVE_COST;
                int cost_r = t[0][i2] + (s1[i1] == s2[i2] ? 0 : REPLACE_COST);
                t[1][i2+1] = Math.min(Math.min(cost_a, cost_d), cost_r);
            }
            temp = t[0];
            t[0] = t[1];
            t[1] = temp;
        }
        return t[0][s2.length];
    }


    private static int scoreStrings(byte[] s1, byte[] s2, long block_size) {
        int score;

        // not a real spamsum signature?
        if (s1.length > SPAMSUM_LENGTH || s2.length > SPAMSUM_LENGTH) return 0;

        // the two strings must have a common substring of length ROLLING_WINDOW to be candidates
        if (!hasCommonSubstring(s1, s2)) return 0;

        // compute the edit distance between the two strings. The edit distance gives
        // us a pretty good idea of how closely related the two strings are
        score = editDistance(s1, s2);

        // scale the edit distance by the lengths of the two
        // strings. This changes the score to be a measure of the
        // proportion of the message that has changed rather than an
        // absolute quantity. It also copes with the variability of the string lengths
        score = (score * SPAMSUM_LENGTH) / (s1.length + s2.length);

        // at this stage the score occurs roughly on a 0-64 scale,
        // with 0 being a good match and 64 being a complete mismatch

        // rescale to a 0-100 scale (friendlier to humans)
        score = (100 * score) / SPAMSUM_LENGTH;

        // now re-scale on a 0-100 scale with 0 being a poor match and 100 being a excellent match
        score = 100 - score;

        // when the blocksize is small we don't want to exaggerate the match size
        if (block_size >= (99 + ROLLING_WINDOW) / ROLLING_WINDOW * MIN_BLOCKSIZE)
            return score;

        if (score > Long.divideUnsigned(block_size, MIN_BLOCKSIZE) * Math.min(s1.length, s2.length)) {
            score = (int)Long.divideUnsigned(block_size, MIN_BLOCKSIZE) * Math.min(s1.length, s2.length);
        }
        return score;
    }



    public boolean isComparable(SpamSumSignature signature) {
        long blockSize1 = signature.getBlockSize();
        long blockSize2 = this.getBlockSize();

        if (blockSize1 != blockSize2 &&
                (blockSize1 > Long.MAX_VALUE / 2 || blockSize1 * 2 != blockSize2) &&
                (Long.remainderUnsigned(blockSize1, 2) == 1  || Long.divideUnsigned(blockSize1, 2) != blockSize2)) {
            return false;
        } else {
            return true;
        }
    }

    public int compareTo(SpamSumSignature other) {

        if (null == other) {
            return -1;
        }

        // if the blocksizes don't match then we are comparing
        // apples to oranges. This isn't an 'error' per se. We could
        // have two valid signatures, but they can't be compared.
        if (!this.isComparable(other))
            return 0;

        // Now that we know the strings are both well formed, are they
        // identical? We could save ourselves some work here
        if (this.isPerfectMatch(other))
            return 100;

        // each spamsum is prefixed by its block size
        long blockSize1 = this.getBlockSize();
        long blockSize2 = other.getBlockSize();

        // there is very little information content is sequences of
        // the same character like 'LLLLL'. Eliminate any sequences
        // longer than 3. This is especially important when combined
        // with the hasCommonSubstring() test below.
        byte[] h11 = this.getHash1(true);
        byte[] h21 = other.getHash1(true);
        byte[] h12 = this.getHash2(true);
        byte[] h22 = other.getHash2(true);

        // each signature has a string for two block sizes. We now
        // choose how to combine the two block sizes. We checked above
        // that they have at least one block size in common
        if (blockSize1 <= Long.MAX_VALUE / 2) {
            if (blockSize1 == blockSize2) {
                int score1, score2;
                score1 = scoreStrings(h11, h21, blockSize1);
                score2 = scoreStrings(h12, h22, blockSize1 * 2);
                return Math.max(score1, score2);
            } else if (blockSize1 * 2 == blockSize2) {
                return scoreStrings(h12, h21, blockSize2);
            } else {
                return scoreStrings(h11, h22, blockSize1);
            }
        } else {
            if (blockSize1 == blockSize2) {
                return scoreStrings(h11, h21, blockSize1);
            } else if (Long.remainderUnsigned(blockSize1, 2) == 1  || Long.divideUnsigned(blockSize1, 2) != blockSize2) {
                return scoreStrings(h11, h22, blockSize1);
            } else {
                return 0;
            }
        }

    }

    public static int fuzzyCompare(String signature1, String signature2) {

        if (signature1.length() < 1 || signature2.length() <1) {
            return -1;
        }
        SpamSumSignature sig1 = new SpamSumSignature(signature1);
        SpamSumSignature sig2 = new SpamSumSignature(signature2);

        return sig1.compareTo(sig2);
    }

    public String getSignature() {
        return Long.toUnsignedString(blockSize) + ':' + Arrays.toString(hash1) + ':' + Arrays.toString(hash2);
    }

    public boolean equals(Object obj) {
        return obj instanceof SpamSumSignature && this.equals((SpamSumSignature) obj);

    }

    public boolean equals(SpamSumSignature other) {
        return this.isDeDupped() == other.isDeDupped() &&
                this.getBlockSize() == other.getBlockSize() &&
                Arrays.equals(this.getHash1(), other.getHash1()) &&
                Arrays.equals(this.getHash2(), other.getHash2());
    }


    public boolean isPerfectMatch(SpamSumSignature other) {
        return this.blockSize == other.blockSize && (Arrays.equals(this.hash1, other.hash1) || Arrays.equals(this.hash2, other.hash2));

    }

    public void deDup() {
        if (!deDup) {
            hash1 = eliminateSequences(hash1);
            hash2 = eliminateSequences(hash2);
            this.deDup = true;
        }
    }

    public boolean isDeDupped() {
        return this.deDup;
    }

    public byte[] toByteArray() {
        byte[] blockSize = Long.toUnsignedString(this.blockSize).getBytes();
        return ByteBuffer.allocate(hash1.length + hash2.length + blockSize.length + 2)
                .put(blockSize)
                .put((byte) ':')
                .put(hash1)
                .put((byte)':')
                .put(hash2)
                .array();
    }

    public String toString() {
        return getSignature();
    }

    public long getBlockSize() {
        return blockSize;
    }

    public long[] getBlockSizes() {
        return new long[] {blockSize, blockSize * 2};
    }

    private byte[] eliminateSequences(byte[] str) {
        int i, j, len;
        len = str.length;
        byte[] ret = new byte[len];

        for (i = j = 3; i < len; i++) {
            if (str[i] != str[i - 1] || str[i] != str[i - 2] || str[i] != str[i - 3]) {
                ret[j++] = str[i];
            }
        }

        return ret;
    }

    public byte[] getHash1(boolean deDup) {
        return ((deDup && !this.isDeDupped()) ? eliminateSequences(hash1) : hash1);
    }

    public byte[] getHash1() { return getHash1(false); }


    public byte[] getHash2(boolean deDup) {
        return ((deDup && !this.isDeDupped()) ? eliminateSequences(hash2) : hash2);
    }

    public byte[] getHash2() { return getHash2(false); }

}