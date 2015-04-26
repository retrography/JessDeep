/**
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

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;


// {

public class EditDistance {

    public static final int NONE = 0;
    public static final int DELETE = 1;
    public static final int INSERT = 1;
    public static final int REPLACE = 2;

/*                public static void main (String[] args) throws java.lang.Exception
                {
                    int a;
                    a = computeLevenshteinDistance("Hello l", "Bello");
                    System.out.println(a);
                }

*/

    public static int edit_distn(byte[] s1, int _s1len, byte[] s2, int _s2len) {
        int i = -1;
        try {
            CharSequence seq1 = new String(s1, "US-ASCII");
            CharSequence seq2 = new String(s2, "US-ASCII");
            i = computeLevenshteinDistance(seq1, seq2);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return i;
    }

    public static int edit_distance(String s1, String s2) {
        return computeLevenshteinDistance(s1, s2);
    }

    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int[][] distanceMatrix(CharSequence str1,
                                         CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= str2.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= str1.length(); i++)
            for (int j = 1; j <= str2.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + DELETE,
                        distance[i][j - 1] + INSERT,
                        distance[i - 1][j - 1]
                                + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
                                : REPLACE));

        return distance;
    }


    public static int computeLevenshteinDistance(CharSequence str1,
                                                 CharSequence str2) {
        return distanceMatrix(str1, str2)[str1.length()][str2.length()];
    }

    public static int[] getOperations(CharSequence str1,
                                      CharSequence str2) {
        LinkedList<Integer> ops = new LinkedList<Integer>();

        int[][] matrix = distanceMatrix(str1, str2);

        int x = str1.length();
        int y = str2.length();
        while (x > 0 && y > 0) {
            if (x > 0 && matrix[x][y] == matrix[x - 1][y] + 1) {
                x--;
                ops.add(DELETE);
            } else if (y > 0 && matrix[x][y] == matrix[x][y - 1] + 1) {
                y--;
                ops.add(INSERT);
            } else if (matrix[x][y] == matrix[x - 1][y - 1] + 1) {
                x--;
                y--;
                ops.add(REPLACE); // from str1.charAt(x+1) to str2.charAt(y+1)
            } else {
                x--;
                y--;
                ops.add(NONE);
            }
        }


        int[] result = new int[ops.size()];
        for (int i = 0; i < result.length; i++) {
            result[result.length - i - 1] = ops.get(i);
        }
        return result;
    }

}


