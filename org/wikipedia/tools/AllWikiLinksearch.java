/*
 * Decompiled with CFR 0_101.
 */
package org.wikipedia.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.wikipedia.WMFWiki;
import org.wikipedia.Wiki;

public class AllWikiLinksearch {
    private static Queue<Wiki> queue = new ConcurrentLinkedQueue<Wiki>();
    private static FileWriter out = null;
    private static ProgressMonitor monitor;
    private static int progress;

    static {
        progress = 0;
    }

    public static void main(String[] args) throws IOException {
        ArrayList<WMFWiki> temp = new ArrayList<WMFWiki>(Arrays.asList(WMFWiki.getSiteMatrix()));
        for (Wiki wiki : temp) {
            String domain = wiki.getDomain();
            if (domain.contains((CharSequence)"wikimania")) continue;
            queue.add(wiki);
        }
        String domain = JOptionPane.showInputDialog(null, "Enter domain to search", "All wiki linksearch", 3);
        monitor = new ProgressMonitor(null, "Searching for links to " + domain, null, 0, queue.size());
        monitor.setMillisToPopup(0);
        out = new FileWriter(domain + ".wiki");
        AllWikiLinksearch.writeOutput("*{{LinkSummary|" + domain + "}}\nSearching " + queue.size() + " wikis at " + new Date().toString() + ".\n\n");
        for (int i = 0; i < 3; ++i) {
            new LinksearchThread(domain).start();
        }
    }

    public static synchronized void writeOutput(String output) {
        try {
            out.write(output);
        }
        catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error writing to file!", "All wiki linksearch", 0);
        }
    }

    public static synchronized void updateProgress() {
        monitor.setProgress(++progress);
    }

    private static class LinksearchThread
    extends Thread {
        private String domain;

        public LinksearchThread(String domain) {
            this.domain = domain;
        }

        @Override
        public void run() {
            while (!queue.isEmpty()) {
                StringBuilder builder;
                int linknumber;
                linknumber = 0;
                builder = new StringBuilder(1000);
                try {
                    try {
                        Wiki wiki = (Wiki)queue.poll();
                        wiki.setMaxLag(0);
                        builder.append("=== Results for ");
                        builder.append(wiki.getDomain());
                        builder.append(" ===\n");
                        List[] links = wiki.linksearch("*." + this.domain);
                        linknumber = links[0].size();
                        if (linknumber != 0) {
                            for (int i = 0; i < linknumber; ++i) {
                                builder.append("# [http://");
                                builder.append(wiki.getDomain());
                                builder.append("/wiki/");
                                builder.append(((String)links[0].get(i)).replace(' ', '_'));
                                builder.append(" ");
                                builder.append(links[0].get(i));
                                builder.append("] uses link <nowiki>");
                                builder.append(links[1].get(i));
                                builder.append("</nowiki>\n");
                            }
                            builder.append(linknumber);
                            builder.append(" links found. ([http://");
                            builder.append(wiki.getDomain());
                            builder.append("/wiki/Special:Linksearch/*.");
                            builder.append(this.domain);
                            builder.append(" Linksearch])");
                        }
                    }
                    catch (IOException ex) {
                        builder.append("<font color=red>An error occurred: ");
                        linknumber = -1;
                        builder.append(ex.getMessage());
                        builder.append("\n\n");
                        if (linknumber != 0) {
                            AllWikiLinksearch.writeOutput(builder.toString());
                        }
                        AllWikiLinksearch.updateProgress();
                        continue;
                    }
                }
                catch (Throwable var6_8) {
                    builder.append("\n\n");
                    if (linknumber != 0) {
                        AllWikiLinksearch.writeOutput(builder.toString());
                    }
                    AllWikiLinksearch.updateProgress();
                    throw var6_8;
                }
                builder.append("\n\n");
                if (linknumber != 0) {
                    AllWikiLinksearch.writeOutput(builder.toString());
                }
                AllWikiLinksearch.updateProgress();
            }
            try {
                out.flush();
                LinksearchThread.sleep(5000);
            }
            catch (Exception linknumber) {
                // empty catch block
            }
        }
    }

}

