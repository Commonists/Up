/*
 * Decompiled with CFR 0_101.
 */
package org.fbot;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import org.wikipedia.Wiki;

public class FbotUtil {
    private FbotUtil() {
    }

    public static String generateRandomString(int len) {
        Random r = new Random();
        String s = "";
        for (int i = 0; i < len; ++i) {
            s = String.valueOf(s) + (char)(r.nextInt(25) + 65);
        }
        return s;
    }

    public static String[][] arraySplitter(String[] z, int splits) {
        if (splits > z.length) {
            splits = z.length;
        }
        if (splits == 0) {
            String[][] xf = new String[][]{z};
            return xf;
        }
        String[][] xf = new String[splits][];
        for (int i = 0; i < splits; ++i) {
            String[] temp = i == 0 ? Arrays.copyOfRange(z, 0, z.length / splits) : (i == splits - 1 ? Arrays.copyOfRange(z, z.length / splits * (splits - 1), z.length) : Arrays.copyOfRange(z, z.length / splits * i, z.length / splits * (i + 1)));
            xf[i] = temp;
        }
        return xf;
    }

    public static void listFilesR(File dir, ArrayList<File> fl) {
        if (!(dir.exists() && dir.isDirectory())) {
            throw new UnsupportedOperationException("Not a directory:  " + dir.getName());
        }
        for (File f : dir.listFiles()) {
            String fn = f.getName();
            if (f.isDirectory() && !fn.startsWith(".")) {
                FbotUtil.listFilesR(f, fl);
                continue;
            }
            if (!FbotUtil.isUploadable(fn)) continue;
            fl.add(f);
        }
    }

    public static boolean arrayContains(Object[] array, Object el) {
        return Arrays.asList(array).contains(el);
    }

    public static HashMap<String, String> buildReasonCollection(String path) throws FileNotFoundException {
        HashMap<String, String> l = new HashMap<String, String>();
        for (String s : FbotUtil.loadFromFile(path, "")) {
            int i = s.indexOf(":");
            l.put(s.substring(0, i), s.substring(i + 1));
        }
        return l;
    }

    public static boolean listElementContains(String[] list, String substring, boolean caseinsensitive) {
        if (caseinsensitive) {
            substring = substring.toLowerCase();
            for (String s : list) {
                if (!s.toLowerCase().contains((CharSequence)substring)) continue;
                return true;
            }
            return false;
        }
        for (String s : list) {
            if (!s.contains((CharSequence)substring)) continue;
            return true;
        }
        return false;
    }

    public static boolean containsIgnoreCase(String text, String s2) {
        return text.toUpperCase().contains((CharSequence)s2.toUpperCase());
    }

    public static String[] loadFromFile(String file, String prefix) throws FileNotFoundException {
        Scanner m = new Scanner(new File(file));
        ArrayList<String> l = new ArrayList<String>();
        while (m.hasNextLine()) {
            l.add(String.valueOf(prefix) + m.nextLine().trim());
        }
        return l.toArray(new String[0]);
    }

    public static void copyFile(String src, String dest) throws FileNotFoundException, IOException {
        int len;
        FileInputStream in = new FileInputStream(new File(src));
        FileOutputStream out = new FileOutputStream(new File(dest));
        byte[] buf = new byte[1024];
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void writeByte(String f, int a) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(new File(f), true);
        out.write(a);
        out.close();
    }

    public static boolean isUploadable(String f) {
        return f.matches("(?i).*?\\.(png|gif|jpg|jpeg|xcf|mid|ogg|ogv|svg|djvu|tiff|tif|oga|pdf)");
    }

    public static Object[] reverseArray(Object[] array) {
        Object[] l = new Object[array.length];
        for (int i = 0; i < array.length; ++i) {
            l[i] = array[array.length - 1 - i];
        }
        return l;
    }

    public static boolean arraysShareElement(String[] a1, String[] a2) {
        if (Wiki.intersection(a1, a2).length > 0) {
            return true;
        }
        return false;
    }

    public static GregorianCalendar offsetTime(int days) {
        GregorianCalendar utc = (GregorianCalendar)new Wiki().makeCalendar();
        utc.setTimeInMillis(utc.getTime().getTime() + 86400000 * (long)days);
        return utc;
    }

    public static String fetchDateUTC(String format, int offset) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(FbotUtil.offsetTime(offset).getTime());
    }

    public static String fileToString(File f) throws FileNotFoundException {
        Scanner m = new Scanner(f);
        String s = "";
        while (m.hasNextLine()) {
            s = String.valueOf(s) + m.nextLine().trim() + "\n";
        }
        m.close();
        return s.trim();
    }

    public static /* varargs */ String[] listify(String s, String prefix, String ... ignorelist) {
        ArrayList<String> l = new ArrayList<String>();
        Scanner m = new Scanner(s);
        while (m.hasNextLine()) {
            String b = m.nextLine().trim();
            if (b.length() <= 0) continue;
            l.add(String.valueOf(prefix) + b);
        }
        if (ignorelist.length > 0) {
            ArrayList<String> x = new ArrayList<String>();
            for (String a : l) {
                boolean good = true;
                for (String bad : ignorelist) {
                    if (!a.contains((CharSequence)bad)) continue;
                    good = false;
                }
                if (!good) continue;
                x.add(a);
            }
            l = x;
        }
        return l.toArray(new String[0]);
    }

    public static String getFileExt(String fn) {
        int i = fn.lastIndexOf(46);
        if (i == -1) {
            return null;
        }
        return fn.substring(i + 1);
    }

    public static void showStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        JTextArea t = new JTextArea(sw.toString());
        t.setEditable(false);
        JOptionPane.showMessageDialog(null, t, "Critical Error!", -1);
    }

    public static /* varargs */ JPanel buildForm(String title, JComponent ... cl) {
        JPanel pl = new JPanel(new GridBagLayout());
        if (cl.length == 0 || cl.length % 2 == 1) {
            throw new UnsupportedOperationException("Either cl is empty or has an odd number of elements!");
        }
        if (title != null) {
            pl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        }
        GridBagConstraints c = new GridBagConstraints();
        c.fill = 2;
        for (int i = 0; i < cl.length; i+=2) {
            c.gridx = 0;
            c.gridy = i;
            c.anchor = 13;
            pl.add((Component)cl[i], c);
            c.anchor = 10;
            c.weightx = 0.5;
            c.gridx = 1;
            c.gridy = i;
            c.ipady = 5;
            pl.add((Component)cl[i + 1], c);
            c.weightx = 0.0;
            c.ipady = 0;
        }
        return pl;
    }
}

