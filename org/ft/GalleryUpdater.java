/*
 * Decompiled with CFR 0_101.
 */
package org.ft;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.FailedLoginException;
import org.fbot.Fbot;
import org.wikipedia.Wiki;

public class GalleryUpdater {
    public static void main(String[] args) throws FailedLoginException, FileNotFoundException, IOException, CredentialNotFoundException {
        Wiki wiki = new Wiki("commons.wikimedia.org");
        Fbot.loginPX(wiki, "Fastily");
        String[] f = wiki.getRawWatchlist();
        wiki = new Wiki("commons.wikimedia.org");
        Fbot.loginPX(wiki, "FSII");
        for (String s : f) {
            try {
                if (!s.startsWith("File:")) continue;
                wiki.edit(s, String.valueOf(wiki.getPageText(s)) + "[[Category:Files by Fastily]]", "Adding [[:Category:Files by Fastily]]");
                continue;
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}

