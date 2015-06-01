/*
 * Decompiled with CFR 0_101.
 */
package org.ft;

import java.io.File;
import java.util.ArrayList;
import org.fbot.Fbot;
import org.fbot.FbotUtil;
import org.fbot.MBot;
import org.wikipedia.Wiki;

public class FastilyTest {
    public static void main(String[] args) throws Throwable {
        if (args.length > 0 && args[0].equals("-u")) {
            ArrayList<String> l = new ArrayList<String>();
            for (File f : new File("x").listFiles()) {
                if (!FbotUtil.isUploadable(f.getName())) continue;
                l.add(f.getPath());
            }
            new MBot("FSII", FbotUtil.buildReasonCollection("px").get("FSII").toCharArray(), "commons.wikimedia.org", 20, l.toArray(new String[0]), "{{pd-self}}\n[[Category:Fastily Test]]").upload();
        } else {
            Wiki wiki = new Wiki("commons.wikimedia.org");
            Fbot.loginPX(wiki, "FSII");
            new MBot("Fastily", FbotUtil.buildReasonCollection("px").get("Fastily").toCharArray(), "commons.wikimedia.org", 10, wiki.getCategoryMembers("Fastily Test", new int[0]), "Uploader requested deletion of a recently uploaded unused file").delete();
        }
    }
}

