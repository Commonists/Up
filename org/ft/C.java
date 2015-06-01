/*
 * Decompiled with CFR 0_101.
 */
package org.ft;

import org.fbot.Fbot;
import org.fbot.FbotParse;
import org.wikipedia.Wiki;

public class C {
    public static void main(String[] args) throws Throwable {
        Wiki wiki = new Wiki("commons.wikimedia.org");
        Fbot.loginPX(wiki, "Fastily");
        for (String s : Fbot.listNamespaceSort(wiki.getLinksOnPage("Commons:Categories for discussion"), 14, wiki)) {
            if (wiki.getCategoryMembers(FbotParse.namespaceStrip(s), new int[0]).length != 0) continue;
            wiki.delete(s, "Empty [[COM:CAT|category]]");
        }
    }
}

