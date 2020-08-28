package de.tuebingen.sfs.utils;

/**
 * Faster String split() and join() implementations.
 */
public class StringUtils {
	
    public static String[] split(String s, char c) {
        String[] res = new String[count(s, c) + 1];
        int i = s.indexOf(c);
        int j = -1;
        int n = 0;
        while (i >= 0) {
            res[n] = s.substring(j+1, i);
            n++;
            j = i;
            i = s.indexOf(c, j+1);
        }
        res[n] = s.substring(j+1);

        return res;
    }

    public static String[] split(String s, String c) {
        String[] res = new String[count(s, c) + 1];
        int i = s.indexOf(c);
        int j = 0;
        int n = 0;
        while (i >= 0) {
            res[n] = s.substring(j, i);
            n++;
            j = i + c.length();
            i = s.indexOf(c, j);
        }
        res[n] = s.substring(j);

        return res;
    }

    public static int count(String s, char c) {
        int n = 0;
        int i = s.indexOf(c);
        while (i >= 0) {
            n++;
            i = s.indexOf(c, i+1);
        }
        return n;
    }

    public static int count(String s, String c) {
        int n = 0;
        int i = s.indexOf(c);
        while (i >= 0) {
            n++;
            i = s.indexOf(c, i+1);
        }
        return n;
    }

    public static String join(String[] a, char c) {
        StringBuilder s = new StringBuilder();
        for (String p : a)
            s.append(p).append(c);
        return s.deleteCharAt(s.length()-1).toString();
    }

    public static String join(String[] a, String c) {
        StringBuilder s = new StringBuilder();
        for (String p : a)
            s.append(p).append(c);
        return s.delete(s.length()-c.length(), s.length()).toString();
    }

    public static void main(String[] args) {
        for (String s : split("aaaabbaaabbaabba", "bb"))
            System.out.println(s);
        System.out.println(join(split("aaaabbaaabbaabba", "bb"), "bb"));
    }
}
