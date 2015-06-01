/*
 * Decompiled with CFR 0_101.
 */
package org.fbot;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import org.fbot.Fbot;
import org.fbot.FbotUtil;
import org.wikipedia.Wiki;

public class MBot {
    private static final short UPLOAD = 101;
    private static final short DELETE = 102;
    private static final short ADD_TEXT = 103;
    private Wiki[] wikis;
    private String[][] lists;
    private String[] reason;

    public /* varargs */ MBot(String user, char[] px, String domain, int instances, String[] list, String ... reason) throws FailedLoginException, IOException {
        Wiki t = new Wiki(domain);
        t.setMaxLag(-1);
        t.login(user, px);
        this.lists = FbotUtil.arraySplitter(list, instances);
        this.wikis = new Wiki[this.lists.length];
        for (int i = 0; i < this.lists.length; ++i) {
            this.wikis[i] = Fbot.wikiFactory(user, px, domain);
        }
        this.reason = reason;
    }

    public int getInstances() {
        return this.wikis.length;
    }

    public synchronized void upload() {
        if (this.reason.length < 1) {
            throw new UnsupportedOperationException("You must provide at LEAST one arg in 'reason' for upload().");
        }
        this.generateThreadsAndRun((short)101);
    }

    public synchronized void addText() {
        if (this.reason.length < 2) {
            throw new UnsupportedOperationException("You must provide at LEAST one arg in 'reason' for addText()");
        }
        this.generateThreadsAndRun((short)103);
    }

    public synchronized void delete() {
        if (this.reason.length < 1) {
            throw new UnsupportedOperationException("You must provide at LEAST one arg in 'reason' for delete()");
        }
        this.generateThreadsAndRun((short)102);
    }

    private synchronized void generateThreadsAndRun(short mode) {
        for (int i = 0; i < this.lists.length; ++i) {
            new Thread(new MBotT(mode, this.wikis[i], this.lists[i])).start();
        }
    }

    private class MBotT
    implements Runnable {
        private short option;
        private String[] l;
        private Wiki wiki;

        protected MBotT(short option, Wiki wiki, String[] l) {
            this.option = option;
            this.l = l;
            this.wiki = wiki;
        }

        @Override
        public void run() {
            switch (this.option) {
                case 101: {
                    this.upload();
                    break;
                }
                case 102: {
                    this.delete();
                    break;
                }
                case 103: {
                    this.addText();
                    break;
                }
                default: {
                    throw new UnsupportedOperationException("Invalid option used!");
                }
            }
        }

        private void upload() {
            for (String f : this.l) {
                boolean success = false;
                do {
                    try {
                        File x = new File(f);
                        this.wiki.upload(x, x.getName(), MBot.this.reason[0], "");
                        success = true;
                        continue;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("IOException encountered, trying again.");
                        continue;
                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                    }
                } while (!success);
            }
        }

        private void delete() {
            for (String s : this.l) {
                try {
                    Fbot.superAction(this.wiki, s, MBot.this.reason[0], "delete");
                    continue;
                }
                catch (LoginException e2) {
                    e2.printStackTrace();
                    System.exit(1);
                    continue;
                }
                catch (IOException e2) {
                    // empty catch block
                }
            }
        }

        private void addText() {
            for (String s : this.l) {
                try {
                    this.wiki.edit(s, String.valueOf(MBot.this.reason[1]) + "\n" + this.wiki.getPageText(s), MBot.this.reason[0]);
                    continue;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    System.err.println("Encountered an issue of some sort, skipping " + s);
                }
            }
        }
    }

}

