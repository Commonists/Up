/*
 * Decompiled with CFR 0_101.
 */
package org.wikipedia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wikipedia.Wiki;

public class WMFWiki
extends Wiki {
    private static final Logger logger = Logger.getLogger("wiki");

    public WMFWiki() {
        super("en.wikipedia.org");
    }

    public WMFWiki(String domain) {
        super(domain);
    }

    public static WMFWiki[] getSiteMatrix() throws IOException {
        WMFWiki wiki = new WMFWiki("en.wikipedia.org");
        wiki.setMaxLag(0);
        String line = wiki.fetch("http://en.wikipedia.org/w/api.php?format=xml&action=sitematrix", "WMFWiki.getSiteMatrix");
        ArrayList<WMFWiki> wikis = new ArrayList<WMFWiki>(1000);
        int x = line.indexOf("url=\"");
        while (x >= 0) {
            int c;
            int a = line.indexOf("http://", x) + 7;
            int b = line.indexOf(34, a);
            x = c = line.indexOf("/>", b);
            String temp = line.substring(b, c);
            if (!(temp.contains((CharSequence)"closed=\"\"") || temp.contains((CharSequence)"private=\"\""))) {
                if (!temp.contains((CharSequence)"fishbowl=\"\"")) {
                    wikis.add(new WMFWiki(line.substring(a, b)));
                }
            }
            x = line.indexOf("url=\"", x);
        }
        logger.log(Level.INFO, "Successfully retrieved site matrix ({0} wikis).", wikis.size());
        return wikis.toArray(new WMFWiki[0]);
    }
}

