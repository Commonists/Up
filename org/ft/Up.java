/*
 * Decompiled with CFR 0_101.
 */
package org.ft;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.fbot.Fbot;
import org.fbot.FbotUtil;
import org.wikipedia.Wiki;

public class Up {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Wiki wiki = new Wiki("commons.wikimedia.org");
    private static final String text = "== {{int:filedesc}} ==\n{{Information\n|Description=%s\n|Source=%s\n|Date=%s\n|Author=%s\n|Permission=\n|other_versions=\n}}\n\n== {{int:license-header}} ==\n%s\n%s";
    private static final ArrayList<String> fails = new ArrayList();
    private static final double version = 0.2;
    private static final JFrame f = new JFrame("Up! v0.2 - The *Best* Wikimedia Commons Mass Uploader");
    private static int cnt = 0;
    private static int total = 0;
    private static JProgressBar b = new JProgressBar();
    private static SmallBox[] smbl;
    private static JScrollPane scp;

    private Up() {
    }

    public static void main(String[] args) throws Throwable {
        if (args.length == 1 && args[0].equals("-f")) {
            Fbot.loginPX(wiki, "FSII");
        } else {
            Fbot.guiLogin(wiki);
        }
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                Up.setupFrame();
                Up.setupAndShowGUI();
            }
        });
    }

    private static void doUpload() {
        for (SmallBox sb2 : smbl) {
            total+=sb2.getFiles().length;
        }
        b.setMinimum(cnt);
        b.setMaximum(total);
        for (SmallBox sb2 : smbl) {
            int i;
            File[] fl = sb2.getFiles();
            HashMap<String, String> m = sb2.getEntries();
            String[] filenames = new String[fl.length];
            Object[] dates = new String[fl.length];
            if (m.get("fname").length() > 0) {
                String base = m.get("fname");
                for (int i2 = 0; i2 < fl.length; ++i2) {
                    filenames[i2] = base + " " + i2 + "." + FbotUtil.getFileExt(fl[i2].getName());
                }
            } else {
                for (i = 0; i < fl.length; ++i) {
                    filenames[i] = fl[i].getName();
                }
            }
            if (m.get("date").length() > 0) {
                Arrays.fill(dates, m.get("date"));
            } else {
                for (int i3 = 0; i3 < fl.length; ++i3) {
                    dates[i3] = sdf.format(new Date(fl[i3].lastModified()));
                }
            }
            for (i = 0; i < fl.length; ++i) {
                String tt = String.format("== {{int:filedesc}} ==\n{{Information\n|Description=%s\n|Source=%s\n|Date=%s\n|Author=%s\n|Permission=\n|other_versions=\n}}\n\n== {{int:license-header}} ==\n%s\n%s", m.get("desc"), m.get("source"), dates[i], m.get("author"), m.get("lic"), m.get("cat"));
                b.setString("Uploading " + filenames[i] + " (" + ++cnt + " of " + total + ")");
                try {
                    wiki.upload(fl[i], filenames[i], tt, "");
                }
                catch (Exception e) {
                    fails.add(fl[i].getAbsolutePath());
                }
                catch (Error e) {
                    FbotUtil.showStackTrace(e);
                    System.exit(1);
                }
                b.setValue(cnt);
            }
        }
        b.setString("Completed upload of " + cnt + " files");
        if (fails.size() > 0) {
            String tt = "Failed to Upload:";
            for (String s : fails) {
                tt = tt + "\n" + s;
            }
            tt = tt + "\nCheck to make sure the pages you tried to were not protected and that you have permission to upload files,";
            JOptionPane.showMessageDialog(null, tt);
        }
        total = 0;
        cnt = 0;
    }

    private static void setupAndShowGUI() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, 1));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Up! v0.2"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        fc.setFileSelectionMode(1);
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Select Folder(s) to Use");
        if (fc.showOpenDialog(null) != 0) {
            System.exit(1);
        }
        File[] fl = fc.getSelectedFiles();
        smbl = new SmallBox[fl.length];
        for (int i = 0; i < fl.length; ++i) {
            Up.smbl[i] = new SmallBox(fl[i]);
        }
        for (SmallBox s : smbl) {
            p.add(s.getPanel());
            p.add(new JSeparator());
        }
        scp = new JScrollPane(p);
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        scp.setPreferredSize(new Dimension(ss.width / 4, ss.height / 2));
        f.add((Component)scp, "Center");
        b.setString("");
        b.setValue(0);
        f.pack();
        f.setVisible(true);
    }

    private static void setupFrame() {
        b.setStringPainted(true);
        f.setDefaultCloseOperation(3);
        f.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        f.setResizable(true);
        JPanel south = new JPanel(new BorderLayout());
        south.add((Component)b, "North");
        JPanel buttons = new JPanel();
        JButton upload = new JButton("Upload");
        upload.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(){

                    @Override
                    public void run() {
                        Up.doUpload();
                    }
                }.start();
            }

        });
        buttons.add(upload);
        JButton reselect = new JButton("Reselect Folders");
        reselect.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(f, "Warning: You will lose any unsaved form data if you reselect folders.\nAre you sure you wish to proceed?", "Warning", 0, 2) == 0) {
                    f.setVisible(false);
                    f.remove(scp);
                    Up.setupAndShowGUI();
                }
            }
        });
        buttons.add(reselect);
        JButton exit = new JButton("Exit");
        exit.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("User exited program");
                System.exit(0);
            }
        });
        buttons.add(exit);
        south.add((Component)buttons, "South");
        f.add((Component)south, "South");
    }

    private static class SmallBox {
        private JTextField cat = new JTextField(20);
        private JTextArea desc = new JTextArea(3, 20);
        private JTextField fname = new JTextField(20);
        private JTextField source = new JTextField("{{own}}", 20);
        private JTextField author = new JTextField("~~~", 20);
        private JTextField lic = new JTextField("{{self|GFDL|cc-by-sa-3.0,2.5,2.0,1.0}}", 20);
        private JTextField date = new JTextField(20);
        private File[] files;
        private JPanel p;

        protected SmallBox(File dir) {
            this.desc.setToolTipText("Hello World!");
            JScrollPane sp = new JScrollPane(this.desc);
            sp.setPreferredSize(this.desc.getPreferredSize());
            JComponent[] jl = new JComponent[]{new JLabel("Category", 0), this.cat, new JLabel("Description", 0), sp, new JLabel("Source", 0), this.source, new JLabel("Author", 0), this.author, new JLabel("License", 0), this.lic, this.checkboxFactory(this.fname, "Diff Filename?"), this.fname, this.checkboxFactory(this.date, "Date Override?"), this.date};
            this.p = FbotUtil.buildForm(dir.getName(), jl);
            ArrayList<File> fl = new ArrayList<File>();
            FbotUtil.listFilesR(dir, fl);
            this.files = fl.toArray(new File[0]);
        }

        protected HashMap<String, String> getEntries() {
            HashMap<String, String> d = new HashMap<String, String>();
            if (this.desc.getText().trim().length() > 0) {
                d.put("desc", this.desc.getText().trim());
            } else {
                d.put("desc", this.cat.getText().trim());
            }
            String cl = "";
            String l = this.cat.getText().trim();
            if (l.length() > 0) {
                for (String s : l.split("\\|{2}")) {
                    cl = cl + "\n[[Category:" + s + "]]";
                }
            }
            cl = cl + "\n[[Category:Uploaded with Up!]]";
            d.put("cat", cl);
            d.put("source", this.source.getText().trim());
            d.put("author", this.author.getText().trim());
            d.put("lic", this.lic.getText().trim());
            d.put("date", this.date.getText().trim());
            d.put("fname", this.fname.getText().trim());
            return d;
        }

        protected File[] getFiles() {
            return this.files;
        }

        private JCheckBox checkboxFactory(final JTextComponent bx, String t) {
            JCheckBox b = new JCheckBox(t);
            bx.setEditable(false);
            b.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!bx.isEditable()) {
                        bx.setEditable(true);
                    } else {
                        bx.setEditable(false);
                    }
                }
            });
            return b;
        }

        protected JPanel getPanel() {
            return this.p;
        }

    }

}

