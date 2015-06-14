/*
 * Decompiled with CFR 0_101.
 */
package org.fbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.wikipedia.Wiki;

public class FbotParse {
    private FbotParse() {
    }

    public static String getRedirectsAsRegex(String template, Wiki wiki) throws IOException {
        String r = "(?si)\\{{2}?\\s*?(Template:)??\\s*?(" + FbotParse.namespaceStrip(template);
        for (String str : wiki.whatLinksHere(template, true, 10)) {
            r = r + "|" + FbotParse.namespaceStrip(str);
        }
        r = r + ").*?\\}{2}?";
        return r;
    }

    public static boolean allowBots(String text, String user) {
        return !text.matches("(?i).*?\\{\\{(nobots|bots\\|(allow=none|deny=(.*?" + user + ".*?|all)|optout=all))\\}\\}.*?");
    }

    public static void templateReplace(String template, String replacementText, String reason, Wiki wiki) throws IOException {
        String[] list = wiki.whatTranscludesHere(template, new int[0]);
        if (template.startsWith("Template:")) {
            template = FbotParse.namespaceStrip(template);
        }
        for (String page : list) {
            try {
                wiki.edit(page, wiki.getPageText(page).replaceAll("(?i)(" + template + ")", replacementText), reason);
                continue;
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static String namespaceStrip(String title) {
        int i = title.indexOf(":");
        if (i > 0) {
            return title.substring(i + 1);
        }
        return title;
    }

    public static String getTemplateParam(String template, int number) {
        try {
            return FbotParse.templateParamStrip(template.split("\\|")[number]);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public static String getTemplateParam(String template, String param) {
        ArrayList<String> f = new ArrayList<String>();
        for (String s : template.split("\\|")) {
            f.add(s.trim());
        }
        for (String p : f) {
            if (!p.startsWith(param)) continue;
            return FbotParse.templateParamStrip(p);
        }
        return null;
    }

    public static String templateParamStrip(String p) {
        int i = p.indexOf("=");
        if (i == -1) {
            return p;
        }
        return p.substring(i + 1).replace((CharSequence)"}}", (CharSequence)"").trim();
    }

    public static String parseTemplateFromPage(String text, String template, boolean redirects, Wiki wiki) throws IOException {
        if (redirects) {
            return FbotParse.parseFromPageRegex(text, FbotParse.getRedirectsAsRegex("Template:" + template, wiki));
        }
        return FbotParse.parseFromPageRegex(text, "(?si)\\{\\{\\s*?(Template:)??\\s*?(" + template + ").*?\\}\\}");
    }

    public static String parseFromPageRegex(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher((CharSequence)text);
        if (m.find()) {
            return text.substring(m.start(), m.end());
        }
        return null;
    }
}

