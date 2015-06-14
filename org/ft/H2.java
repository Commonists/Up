/*
 * Decompiled with CFR 0_101.
 */
package org.ft;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import javax.security.auth.login.LoginException;
import org.fbot.Fbot;
import org.fbot.FbotUtil;
import org.wikipedia.Wiki;

public class H2 {
    private static final Random r = new Random();

    public static void main(String[] args) throws FileNotFoundException {
        String base = "Test" + FbotUtil.generateRandomString(3) + r.nextInt(10000000) + "th";
        int i = 0;
        for (File f : new File(".").listFiles()) {
            if (!FbotUtil.isUploadable(f.getName())) continue;
            new Thread(new SmallUpload(f, base + i++)).start();
        }
    }

    private static class SmallUpload
    implements Runnable {
        private Wiki wiki;
        private File f;
        private String base;

        protected SmallUpload(File f, String base) throws FileNotFoundException {
            this.base = base + "cnt";
            this.f = f;
            this.wiki = Fbot.wikiFactory("FSII", FbotUtil.buildReasonCollection("px").get("FSII").toCharArray(), "commons.wikimedia.org");
        }

        @Override
        public void run() {
            int cnt = 0;
            String name = this.f.getName();
            String ext = name.substring(name.lastIndexOf("."));
            do {
                try {
                    do {
                        FbotUtil.writeByte(this.f.getAbsolutePath(), r.nextInt(129));
                        this.wiki.upload(this.f, this.base + cnt++ + ext, "This upload is part of a test for an application platform I've been developing. This file shall be deleted shortly.\n{{Self|CC-BY-SA-3.0}}\n[[Category:Fastily Test]]", "");
                    } while (true);
                }
                catch (LoginException e) {
                    System.err.println("Critical Error");
                    System.exit(1);
                    continue;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    System.err.println("Trying again");
                    continue;
                }
            } while (true);
        }
    }

}

