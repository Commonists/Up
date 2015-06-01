/*
 * Decompiled with CFR 0_101.
 */
package org.ft;

import java.awt.Component;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class FileRenamer {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private static String[] xls = new String[]{"jpg", "jpeg", "ogv", "png", "gif", "xcf", "mid", "ogg", "svg", "djvu", "tiff", "tif", "oga", "pdf"};
    private static String ext;
    private static String startingDir;

    static {
        startingDir = System.getProperty("user.home");
    }

    public static void main(String[] args) {
        ext = FileRenamer.getUserSelection();
        do {
            File[] dirs;
            if ((dirs = FileRenamer.directoryFetch()).length == 1) {
                FileRenamer.manualRename(dirs[0]);
                continue;
            }
            FileRenamer.automatedRename(dirs);
        } while (JOptionPane.showConfirmDialog(null, "Would you like to continue re-naming files?") == 0);
    }

    protected static void manualRename(File dir) {
        FileRenamer.renameFilesInDir(FileRenamer.getBase(), dir);
        startingDir = dir.getParent();
    }

    protected static void automatedRename(File[] dirlist) {
        for (File d : dirlist) {
            FileRenamer.renameFilesInDir(d.getName(), d);
        }
        JOptionPane.showMessageDialog(null, "Requested rename operation was successful.");
    }

    protected static void renameFilesInDir(String base, File dir) {
        String bDir = dir.getAbsolutePath();
        HashMap<String, Integer> db = new HashMap<String, Integer>();
        for (File f : dir.listFiles()) {
            int i;
            if (!f.getName().matches("(?i).*?\\.(" + ext + ")")) continue;
            String date = FileRenamer.getDate(f);
            if (!db.containsKey(date)) {
                i = 0;
                db.put(date, new Integer(i));
            } else {
                i = (Integer)db.get(date);
                db.put(date, new Integer(++i));
            }
            f.renameTo(new File(String.valueOf(bDir) + "/" + base + date + "p" + i + "." + ext));
        }
    }

    protected static String getUserSelection() {
        JComboBox<String> cb = new JComboBox<String>(xls);
        if (JOptionPane.showConfirmDialog(null, cb, "Choose file type to rename", 2, -1) != 0) {
            System.exit(1);
        }
        return (String)cb.getSelectedItem();
    }

    protected static File[] directoryFetch() {
        JFileChooser fc = new JFileChooser(startingDir);
        fc.setFileSelectionMode(1);
        fc.setDialogTitle("Select a Directory to Use");
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(null) != 0) {
            System.exit(1);
        }
        return fc.getSelectedFiles();
    }

    protected static String getBase() {
        do {
            String base;
            if ((base = JOptionPane.showInputDialog(null, (Object)"Enter the filename base:")) == null) {
                System.exit(0);
            } else if ((base = base.trim()).length() > 0) {
                return base;
            }
            JOptionPane.showMessageDialog(null, "Base cannot be blank.");
        } while (true);
    }

    protected static String getDate(File f) {
        return sdf.format(new Date(f.lastModified()));
    }
}

