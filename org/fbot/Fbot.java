/*
 * Decompiled with CFR 0_101.
 */
package org.fbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.fbot.FbotUtil;
import org.wikipedia.Wiki;

public class Fbot {
    private Fbot() {
    }

    public static void loginAndSetPrefs(Wiki wiki, String user, char[] p) {
        wiki.setThrottle(1);
        wiki.setMaxLag(-1);
        int i = 0;
        boolean success = false;
        do {
            try {
                wiki.login(user, p);
                success = true;
                continue;
            }
            catch (IOException e) {
                System.out.println("Encountered IOException.  This was try #" + i);
                if (++i <= 8) continue;
                System.exit(1);
                continue;
            }
            catch (FailedLoginException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } while (!success);
    }

    public static void loginPX(Wiki wiki, String user) throws FileNotFoundException {
        HashMap<String, String> c = FbotUtil.buildReasonCollection("./px");
        String px = c.get(user);
        if (px == null) {
            throw new UnsupportedOperationException("Did not find a Username in the specified file matching String value in user param");
        }
        Fbot.loginAndSetPrefs(wiki, user, px.toCharArray());
    }

    public static void guiLogin(Wiki wiki) {
        JTextField u = new JTextField(12);
        JPasswordField px = new JPasswordField(12);
        JPanel pl = FbotUtil.buildForm("Login", new JLabel("Username:", 11), u, new JLabel("Password:", 11), px);
        if (JOptionPane.showConfirmDialog(null, pl, "Login", 2, -1) != 0) {
            System.exit(0);
        }
        Fbot.loginAndSetPrefs(wiki, u.getText().trim(), px.getPassword());
        wiki.setThrottle(5);
    }

    public static String getRedirectTarget(String redirect, Wiki wiki) throws IOException {
        String text = wiki.getPageText(redirect).trim();
        if (text.matches("(?si)^#(redirect)\\s*?\\[\\[.+?\\]\\].*?")) {
            return text.substring(text.indexOf("[[") + 2, text.indexOf("]]"));
        }
        throw new UnsupportedOperationException("Parameter passed in is not a redirect page!");
    }

    public static boolean exists(String page, Wiki wiki) throws IOException {
        return (Boolean)wiki.getPageInfo(page).get("exists");
    }

    public static void dbrDump(String page, String[] list, String headerText, String footerText, Wiki wiki) throws LoginException, IOException {
        String dump = headerText + "  This report last updated as of ~~~~~\n";
        for (String s : list) {
            dump = dump + "*[[:" + s + "]]\n";
        }
        dump = dump + "\n" + footerText;
        wiki.edit(page, dump, "Updating list");
    }

    public static String[] listNamespaceSort(String[] list, int namespace, Wiki wiki) throws IOException {
        ArrayList<String> l = new ArrayList<String>();
        for (String s : list) {
            if (wiki.namespace(s) != namespace) continue;
            l.add(s);
        }
        return l.toArray(new String[0]);
    }

    public static String[] arrayNuke(String[] list, String reason, String talkReason, Wiki wiki) {
        ArrayList<String> f = new ArrayList<String>();
        for (String s : list) {
            try {
                wiki.delete(s, reason);
            }
            catch (Throwable e) {
                f.add(s);
                continue;
            }
            if (talkReason == null) continue;
            try {
                wiki.delete(wiki.getTalkPage(s), talkReason);
                continue;
            }
            catch (Throwable e) {
                // empty catch block
            }
        }
        return f.toArray(new String[0]);
    }

    public static void addTextList(String[] pages, String text, String summary, Wiki wiki) {
        for (String page : pages) {
            try {
                wiki.edit(page, wiki.getPageText(text) + text, summary);
                continue;
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void fileReplace(String[] list, String file, String replacement, String summary, Wiki wiki) {
        file = file.replace((CharSequence)"_", (CharSequence)" ");
        String regex = "(?i)(" + file + "|" + file.replace((CharSequence)" ", (CharSequence)"_") + ")";
        for (String page : list) {
            try {
                wiki.edit(page, wiki.getPageText(page).replaceAll(regex, replacement), summary);
                continue;
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static Wiki wikiFactory(String u, char[] p, String domain) {
        Wiki wiki = new Wiki(domain);
        Fbot.loginAndSetPrefs(wiki, u, p);
        return wiki;
    }

    public static void downloadFile(String title, String localpath, Wiki wiki) throws IOException, FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(localpath);
        fos.write(wiki.getImage(title));
        fos.close();
    }

    public static void superAction(Wiki wiki, String page, String reason, String code) throws LoginException, IOException {
        boolean success = false;
        int i = 0;
        do {
            try {
                if (code.equals("delete")) {
                    wiki.delete(page, reason);
                } else if (code.equals("upload")) {
                    wiki.upload(new File(page), page, reason, "");
                } else {
                    throw new UnsupportedOperationException(code + " is not a valid code!");
                }
                success = true;
                continue;
            }
            catch (LoginException e) {
                throw e;
            }
            catch (IOException e) {
                if (i++ > 4) {
                    throw e;
                }
                e.printStackTrace();
                System.err.println("Network error? Try: " + i + " of 5");
                continue;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } while (!success);
    }
}

