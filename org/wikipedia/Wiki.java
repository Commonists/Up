/*
 * Decompiled with CFR 0_101.
 */
package org.wikipedia;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.net.HttpRetryException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.*;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

public class Wiki
implements Serializable {
    public static final int MEDIA_NAMESPACE = -2;
    public static final int SPECIAL_NAMESPACE = -1;
    public static final int MAIN_NAMESPACE = 0;
    public static final int TALK_NAMESPACE = 1;
    public static final int USER_NAMESPACE = 2;
    public static final int USER_TALK_NAMESPACE = 3;
    public static final int PROJECT_NAMESPACE = 4;
    public static final int PROJECT_TALK_NAMESPACE = 5;
    public static final int FILE_NAMESPACE = 6;
    public static final int FILE_TALK_NAMESPACE = 7;
    @Deprecated
    public static final int IMAGE_NAMESPACE = 6;
    @Deprecated
    public static final int IMAGE_TALK_NAMESPACE = 7;
    public static final int MEDIAWIKI_NAMESPACE = 8;
    public static final int MEDIAWIKI_TALK_NAMESPACE = 9;
    public static final int TEMPLATE_NAMESPACE = 10;
    public static final int TEMPLATE_TALK_NAMESPACE = 11;
    public static final int HELP_NAMESPACE = 12;
    public static final int HELP_TALK_NAMESPACE = 13;
    public static final int CATEGORY_NAMESPACE = 14;
    public static final int CATEGORY_TALK_NAMESPACE = 15;
    public static final int ALL_NAMESPACES = 167317762;
    public static final String ALL_LOGS = "";
    public static final String USER_CREATION_LOG = "newusers";
    public static final String UPLOAD_LOG = "upload";
    public static final String DELETION_LOG = "delete";
    public static final String MOVE_LOG = "move";
    public static final String BLOCK_LOG = "block";
    public static final String PROTECTION_LOG = "protect";
    public static final String USER_RIGHTS_LOG = "rights";
    public static final String USER_RENAME_LOG = "renameuser";
    public static final String IMPORT_LOG = "import";
    public static final String PATROL_LOG = "patrol";
    public static final int NO_PROTECTION = -1;
    public static final int SEMI_PROTECTION = 1;
    public static final int FULL_PROTECTION = 2;
    public static final int MOVE_PROTECTION = 3;
    public static final int SEMI_AND_MOVE_PROTECTION = 4;
    public static final int PROTECTED_DELETED_PAGE = 5;
    public static final int UPLOAD_PROTECTION = 6;
    public static final int ASSERT_NONE = 0;
    public static final int ASSERT_LOGGED_IN = 1;
    public static final int ASSERT_BOT = 2;
    public static final int ASSERT_NO_MESSAGES = 4;
    public static final int HIDE_ANON = 1;
    public static final int HIDE_BOT = 2;
    public static final int HIDE_SELF = 4;
    public static final int HIDE_MINOR = 8;
    public static final int HIDE_PATROLLED = 16;
    public static final long NEXT_REVISION = -1;
    public static final long CURRENT_REVISION = -2;
    public static final long PREVIOUS_REVISION = -3;
    private static final String version = "0.26";
    private String domain;
    protected String query;
    protected String base;
    protected String apiUrl;
    protected String scriptPath = "/w";
    private HashMap<String, String> cookies = new HashMap(12);
    private User user;
    private int statuscounter = 0;
    private HashMap<String, Integer> namespaces = null;
    private ArrayList<String> watchlist = null;
    private int max = 500;
    protected static final Logger logger = Logger.getLogger("wiki");
    private int throttle = 10000;
    private int maxlag = 5;
    private int assertion = 0;
    private int statusinterval = 100;
    private String useragent = "Wiki.java 0.26";
    private boolean zipped = true;
    private boolean markminor = false;
    private boolean markbot = false;
    private boolean resolveredirect = false;
    private boolean retry = true;
    private static final long serialVersionUID = -8745212681497644126L;
    private static final int CONNECTION_CONNECT_TIMEOUT_MSEC = 30000;
    private static final int CONNECTION_READ_TIMEOUT_MSEC = 180000;
    private static final int LOG2_CHUNK_SIZE = 22;

    static {
        logger.logp(Level.CONFIG, "Wiki", "<init>", "Using Wiki.java 0.26");
    }

    public Wiki() {
        this("");
    }

    public Wiki(String domain) {
        if (domain == null || domain.isEmpty()) {
            domain = "en.wikipedia.org";
        }
        this.domain = domain;
        this.initVars();
    }

    public Wiki(String domain, String scriptPath) {
        this.domain = domain;
        this.scriptPath = scriptPath;
        this.initVars();
    }

    protected void initVars() {
        String temp = "http://" + this.domain + this.scriptPath;
        if (this.maxlag >= 0) {
            this.apiUrl = String.valueOf(temp) + "/api.php?maxlag=" + this.maxlag + "&format=xml&";
            this.base = String.valueOf(temp) + "/index.php?maxlag=" + this.maxlag + "&title=";
        } else {
            this.apiUrl = String.valueOf(temp) + "/api.php?format=xml&";
            this.base = String.valueOf(temp) + "/index.php?title=";
        }
        this.query = String.valueOf(this.apiUrl) + "action=query&";
        if (this.resolveredirect) {
            this.query = String.valueOf(this.query) + "redirects&";
        }
    }

    public String getDomain() {
        return this.domain;
    }

    public int getThrottle() {
        return this.throttle;
    }

    public void setThrottle(int throttle) {
        this.throttle = throttle;
        this.log(Level.CONFIG, "Throttle set to " + throttle + " milliseconds", "setThrottle");
    }

    public String getScriptPath() throws IOException {
        this.scriptPath = this.parseAndCleanup("{{SCRIPTPATH}}");
        this.initVars();
        return this.scriptPath;
    }

    public void setUserAgent(String useragent) {
        this.useragent = useragent;
    }

    public String getUserAgent() {
        return this.useragent;
    }

    public void setUsingCompressedRequests(boolean zipped) {
        this.zipped = zipped;
    }

    public boolean isUsingCompressedRequests() {
        return this.zipped;
    }

    public boolean isResolvingRedirects() {
        return this.resolveredirect;
    }

    public void setResolveRedirects(boolean b) {
        this.resolveredirect = b;
        this.initVars();
    }

    public void setMarkBot(boolean markbot) {
        this.markbot = markbot;
    }

    public boolean isMarkBot() {
        return this.markbot;
    }

    public void setMarkMinor(boolean minor) {
        this.markminor = minor;
    }

    public boolean isMarkMinor() {
        return this.markminor;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Wiki)) {
            return false;
        }
        return this.domain.equals(((Wiki)obj).domain);
    }

    public int hashCode() {
        return this.domain.hashCode() * this.maxlag - this.throttle;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder("Wiki[domain=");
        buffer.append(this.domain);
        buffer.append(",user=");
        buffer.append(this.user != null ? this.user.toString() : "null");
        buffer.append(",");
        buffer.append("throttle=");
        buffer.append(this.throttle);
        buffer.append(",maxlag=");
        buffer.append(this.maxlag);
        buffer.append(",assertionMode=");
        buffer.append(this.assertion);
        buffer.append(",statusCheckInterval=");
        buffer.append(this.statusinterval);
        buffer.append(",cookies=");
        buffer.append(this.cookies);
        buffer.append("]");
        return buffer.toString();
    }

    public int getMaxLag() {
        return this.maxlag;
    }

    public void setMaxLag(int lag) {
        this.maxlag = lag;
        this.log(Level.CONFIG, "Setting maximum allowable database lag to " + lag, "setMaxLag");
        this.initVars();
    }

    public int getAssertionMode() {
        return this.assertion;
    }

    public void setAssertionMode(int mode) {
        this.assertion = mode;
        this.log(Level.CONFIG, "Set assertion mode to " + mode, "setAssertionMode");
    }

    public int getStatusCheckInterval() {
        return this.statusinterval;
    }

    public void setStatusCheckInterval(int interval) {
        this.statusinterval = interval;
        this.log(Level.CONFIG, "Status check interval set to " + interval, "setStatusCheckInterval");
    }

    public synchronized void login(String username, char[] password) throws IOException, FailedLoginException {
		StringBuilder buffer = new StringBuilder(500);
		buffer.append("lgname=");
		username = this.normalize(username);
		buffer.append(URLEncoder.encode(username, "UTF-8"));
        String line = this.post(String.valueOf(this.apiUrl) + "action=login", buffer.toString(), "login");

		Pattern pattern = Pattern.compile("token=\"([^\"]+)\"");
		Matcher m = pattern.matcher(line);
		String wpLoginToken = "\\+";
		if (m.find()) {
			wpLoginToken = m.group(1);
		}
        
        buffer = new StringBuilder(500);
        buffer.append("lgname=");
        buffer.append(URLEncoder.encode(username, "UTF-8"));
        buffer.append("&lgpassword=");
        buffer.append(URLEncoder.encode(new String(password), "UTF-8"));
        buffer.append("&lgtoken=");
        buffer.append(URLEncoder.encode(wpLoginToken, "UTF-8"));
        line = this.post(String.valueOf(this.apiUrl) + "action=login", buffer.toString(), "login");
        buffer = null;
        if (!line.contains((CharSequence)"result=\"Success\"")) {
            this.log(Level.WARNING, "Failed to log in as " + username, "login");
            try {
                Thread.sleep(20000);
            }
            catch (InterruptedException apihighlimit) {
                // empty catch block
            }
            if (line.contains((CharSequence)"WrongPass") || line.contains((CharSequence)"WrongPluginPass")) {
                throw new FailedLoginException("Login failed: incorrect password.");
            }
            if (line.contains((CharSequence)"NotExists")) {
                throw new FailedLoginException("Login failed: user does not exist.");
            }
            throw new FailedLoginException("Login failed: unknown reason.");
        }
        this.user = new User(username);
        boolean apihighlimit = this.user.isAllowedTo("apihighlimits");
        this.max = apihighlimit ? 5000 : 500;
        this.log(Level.INFO, "Successfully logged in as " + username + ", highLimit = " + apihighlimit, "login");
    }

    public synchronized void logout() {
        this.cookies.clear();
        this.user = null;
        this.max = 500;
        this.log(Level.INFO, "Logged out", "logout");
    }

    public synchronized void logoutServerSide() throws IOException {
        this.fetch(String.valueOf(this.apiUrl) + "action=logout", "logoutServerSide");
        this.logout();
    }

    public boolean hasNewMessages() throws IOException {
        String url = String.valueOf(this.query) + "meta=userinfo&uiprop=hasmsg";
        return this.fetch(url, "hasNewMessages").contains((CharSequence)"messages=\"\"");
    }

    public int getCurrentDatabaseLag() throws IOException {
        String line = this.fetch(String.valueOf(this.query) + "meta=siteinfo&siprop=dbrepllag", "getCurrentDatabaseLag");
        int z = line.indexOf("lag=\"") + 5;
        String lag = line.substring(z, line.indexOf("\" />", z));
        this.log(Level.INFO, "Current database replication lag is " + lag + " seconds", "getCurrentDatabaseLag");
        return Integer.parseInt(lag);
    }

    public HashMap<String, Integer> getSiteStatistics() throws IOException {
        String text = this.parseAndCleanup("{{NUMBEROFARTICLES:R}} {{NUMBEROFPAGES:R}} {{NUMBEROFFILES:R}} {{NUMBEROFEDITS:R}} {{NUMBEROFUSERS:R}} {{NUMBEROFADMINS:R}}");
        String[] values = text.split("\\s");
        HashMap<String, Integer> ret = new HashMap<String, Integer>(12);
        String[] keys = new String[]{"articles", "pages", "files", "edits", "users", "admins"};
        for (int i = 0; i < values.length; ++i) {
            Integer value = new Integer(values[i]);
            ret.put(keys[i], value);
        }
        return ret;
    }

    public String version() throws IOException {
        return this.parseAndCleanup("{{CURRENTVERSION}}");
    }

    public String parse(String markup) throws IOException {
        String response = this.post(String.valueOf(this.apiUrl) + "action=parse", "prop=text&text=" + URLEncoder.encode(markup, "UTF-8"), "parse");
        int y = response.indexOf(62, response.indexOf("<text")) + 1;
        int z = response.indexOf("</text>");
        return this.decode(response.substring(y, z));
    }

    protected String parseAndCleanup(String in) throws IOException {
        String output = this.parse(in);
        output = output.replace((CharSequence)"<p>", (CharSequence)"").replace((CharSequence)"</p>", (CharSequence)"");
        output = output.replace((CharSequence)"\n", (CharSequence)"");
        int a = output.indexOf("<!--");
        return output.substring(0, a);
    }

    public String random() throws IOException {
        return this.random(0);
    }

    public /* varargs */ String random(int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=random");
        this.constructNamespaceString(url, "rn", ns);
        String line = this.fetch(url.toString(), "random");
        int a = line.indexOf("title=\"") + 7;
        int b = line.indexOf(34, a);
        return line.substring(a, b);
    }

    public static String[] parseList(String list) {
        StringTokenizer tokenizer = new StringTokenizer(list, "[]");
        ArrayList<String> titles = new ArrayList<String>(667);
        tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.contains((CharSequence)"\n") || token.isEmpty()) continue;
            if (token.startsWith(":")) {
                token = token.substring(1);
            }
            titles.add(token);
        }
        return titles.toArray(new String[0]);
    }

    public static String formatList(String[] pages) {
        StringBuilder buffer = new StringBuilder(10000);
        for (int i = 0; i < pages.length; ++i) {
            buffer.append("*[[:");
            buffer.append(pages[i]);
            buffer.append("]]\n");
        }
        return buffer.toString();
    }

    public static String[] intersection(String[] a, String[] b) {
        ArrayList<String> aa = new ArrayList<String>(5000);
        aa.addAll(Arrays.asList(a));
        aa.retainAll(Arrays.asList(b));
        return aa.toArray(new String[0]);
    }

    public static String[] relativeComplement(String[] a, String[] b) {
        ArrayList<String> aa = new ArrayList<String>(5000);
        aa.addAll(Arrays.asList(a));
        aa.removeAll(Arrays.asList(b));
        return aa.toArray(new String[0]);
    }

    public String getTalkPage(String title) throws IOException {
        int namespace = this.namespace(title);
        if (namespace % 2 == 1) {
            throw new IllegalArgumentException("Cannot fetch talk page of a talk page!");
        }
        if (namespace < 0) {
            throw new IllegalArgumentException("Special: and Media: pages do not have talk pages!");
        }
        if (namespace != 0) {
            title = title.substring(title.indexOf(58) + 1);
        }
        return String.valueOf(this.namespaceIdentifier(namespace + 1)) + ":" + title;
    }

    public HashMap<String, Object> getPageInfo(String page) throws IOException {
        int b;
        int a;
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=info&intoken=edit%7Cwatch&inprop=protection%7Cdisplaytitle&titles=");
        url.append(URLEncoder.encode(this.normalize(page), "UTF-8"));
        String line = this.fetch(url.toString(), "getPageInfo");
        HashMap<String, Object> info = new HashMap<String, Object>(15);
        boolean exists = !line.contains((CharSequence)"missing=\"\"");
        info.put("exists", exists);
        if (exists) {
            a = line.indexOf("touched=\"") + 9;
            b = line.indexOf(34, a);
            info.put("lastpurged", this.timestampToCalendar(this.convertTimestamp(line.substring(a, b))));
            a = line.indexOf("lastrevid=\"") + 11;
            b = line.indexOf(34, a);
            info.put("lastrevid", Long.parseLong(line.substring(a, b)));
            a = line.indexOf("length=\"") + 8;
            b = line.indexOf(34, a);
            info.put("size", Integer.parseInt(line.substring(a, b)));
            int z = line.indexOf("type=\"edit\"");
            if (z != -1) {
                String s = line.substring(z, z + 30);
                if (s.contains((CharSequence)"sysop")) {
                    info.put("protection", 2);
                } else {
                    s = line.substring(z + 30);
                    info.put("protection", line.contains((CharSequence)"level=\"sysop\"") ? 4 : 1);
                }
            } else {
                info.put("protection", line.contains((CharSequence)"type=\"move\"") ? 3 : -1);
            }
        } else {
            info.put("lastedited", null);
            info.put("lastrevid", -1);
            info.put("size", -1);
            info.put("protection", line.contains((CharSequence)"type=\"create\"") ? 5 : -1);
        }
        info.put("cascade", line.contains((CharSequence)"cascade=\"\""));
        a = line.indexOf("displaytitle=\"") + 14;
        b = line.indexOf(34, a);
        info.put("displaytitle", line.substring(a, b));
        a = line.indexOf("edittoken=\"") + 11;
        b = line.indexOf(34, a);
        info.put("token", line.substring(a, b));
        a = line.indexOf("watchtoken=\"") + 12;
        b = line.indexOf(34, a);
        info.put("watchtoken", line.substring(a, b));
        info.put("timestamp", this.makeCalendar());
        this.log(Level.INFO, "Successfully retrieved page info for " + page, "getPageInfo");
        return info;
    }

    public int getProtectionLevel(String title) throws IOException {
        HashMap<String, Object> info = this.getPageInfo(title);
        if (((Boolean)info.get("cascade")).booleanValue()) {
            return 2;
        }
        return (Integer)info.get("protection");
    }

    public int namespace(String title) throws IOException {
        if (!(title = this.normalize(title)).contains((CharSequence)":")) {
            return 0;
        }
        String namespace = title.substring(0, title.indexOf(58));
        if (namespace.equals("Project_talk")) {
            return 5;
        }
        if (namespace.equals("Project")) {
            return 4;
        }
        if (this.namespaces == null) {
            this.populateNamespaceCache();
        }
        if (!this.namespaces.containsKey(namespace)) {
            return 0;
        }
        return this.namespaces.get(namespace);
    }

    public String namespaceIdentifier(int namespace) throws IOException {
        if (this.namespaces == null) {
            this.populateNamespaceCache();
        }
        if (!this.namespaces.containsValue(namespace)) {
            return "";
        }
        for (Map.Entry<String, Integer> entry : this.namespaces.entrySet()) {
            if (!entry.getValue().equals(namespace)) continue;
            return entry.getKey();
        }
        return "";
    }

    protected void populateNamespaceCache() throws IOException {
        String line = this.fetch(String.valueOf(this.query) + "meta=siteinfo&siprop=namespaces", "namespace");
        this.namespaces = new HashMap(30);
        while (line.contains((CharSequence)"<ns")) {
            int x = line.indexOf("<ns id=");
            if (line.charAt(x + 8) == '0') {
                line = line.substring(13);
                continue;
            }
            int y = line.indexOf("</ns>");
            String working = line.substring(x + 8, y);
            int ns = Integer.parseInt(working.substring(0, working.indexOf(34)));
            String name = working.substring(working.indexOf(62) + 1);
            this.namespaces.put(name, new Integer(ns));
            line = line.substring(y + 5);
        }
        this.log(Level.INFO, "Successfully retrieved namespace list (" + (this.namespaces.size() + 1) + " namespaces)", "namespace");
    }

    public /* varargs */ boolean[] exists(String ... titles) throws IOException {
        StringBuilder wikitext = new StringBuilder(15000);
        StringBuilder parsed = new StringBuilder(1000);
        for (int i = 0; i < titles.length; ++i) {
            wikitext.append("{{#ifexist:");
            wikitext.append(titles[i]);
            wikitext.append("|1|0}}");
            if (i % 500 != 499 && i != titles.length - 1) continue;
            parsed.append(this.parseAndCleanup(wikitext.toString()));
            wikitext = new StringBuilder(15000);
        }
        char[] characters = parsed.toString().toCharArray();
        boolean[] ret = new boolean[characters.length];
        for (int i2 = 0; i2 < characters.length; ++i2) {
            if (characters[i2] != '1' && characters[i2] != '0') {
                throw new UnknownError("Unable to parse output. Perhaps the ParserFunctions extension is not installed, or this is a bug.");
            }
            ret[i2] = characters[i2] == '1';
        }
        return ret;
    }

    public String getPageText(String title) throws IOException {
        if (this.namespace(title) < 0) {
            throw new UnsupportedOperationException("Cannot retrieve Special: or Media: pages!");
        }
        String url = String.valueOf(this.base) + URLEncoder.encode(this.normalize(title), "UTF-8") + "&action=raw";
        String temp = this.fetch(url, "getPageText");
        this.log(Level.INFO, "Successfully retrieved text of " + title, "getPageText");
        return this.decode(temp);
    }

    public String getSectionText(String title, int number) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=revisions&rvprop=content&titles=");
        url.append(URLEncoder.encode(title, "UTF-8"));
        url.append("&rvsection=");
        url.append(number);
        String text = this.fetch(url.toString(), "getSectionText");
        if (text.contains((CharSequence)"<error code=\"rvnosuchsection\"")) {
            throw new IllegalArgumentException("There is no section " + number + " in the page " + title);
        }
        int a = text.indexOf("<rev xml:space=\"preserve\">") + 26;
        int b = text.indexOf("</rev>", a);
        return this.decode(text.substring(a, b));
    }

    public String getRenderedText(String title) throws IOException {
        return this.parse("{{:" + title + "}}");
    }

    public void edit(String title, String text, String summary) throws IOException, LoginException {
        this.edit(title, text, summary, this.markminor, this.markbot, -2, null);
    }

    public void edit(String title, String text, String summary, Calendar basetime) throws IOException, LoginException {
        this.edit(title, text, summary, this.markminor, this.markbot, -2, basetime);
    }

    public void edit(String title, String text, String summary, int section) throws IOException, LoginException {
        this.edit(title, text, summary, this.markminor, this.markbot, section, null);
    }

    public void edit(String title, String text, String summary, int section, Calendar basetime) throws IOException, LoginException {
        this.edit(title, text, summary, this.markminor, this.markbot, section, basetime);
    }

    public synchronized void edit(String title, String text, String summary, boolean minor, boolean bot, int section, Calendar basetime) throws IOException, LoginException {
        long start = System.currentTimeMillis();
        this.statusCheck();
        HashMap<String, Object> info = this.getPageInfo(title);
        int level = (Integer)info.get("protection");
        if (!this.checkRights(level, false)) {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            logger.logp(Level.WARNING, "Wiki", "edit()", "[" + this.getDomain() + "] Cannot edit - permission denied.", ex);
            throw ex;
        }
        String wpEditToken = (String)info.get("token");
        StringBuilder buffer = new StringBuilder(300000);
        buffer.append("title=");
        buffer.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        buffer.append("&text=");
        buffer.append(URLEncoder.encode(text, "UTF-8"));
        buffer.append("&summary=");
        buffer.append(URLEncoder.encode(summary, "UTF-8"));
        buffer.append("&token=");
        buffer.append(URLEncoder.encode(wpEditToken, "UTF-8"));
        if (basetime != null) {
            buffer.append("&starttimestamp=");
            buffer.append(this.calendarToTimestamp((Calendar)info.get("timestamp")));
            buffer.append("&basetimestamp=");
            buffer.append(this.calendarToTimestamp(basetime));
        }
        if (minor) {
            buffer.append("&minor=1");
        }
        if (bot && this.user.isAllowedTo("bot")) {
            buffer.append("&bot=1");
        }
        if (section == -1) {
            buffer.append("&section=new");
        } else if (section != -2) {
            buffer.append("&section=");
            buffer.append(section);
        }
        String response = this.post(String.valueOf(this.apiUrl) + "action=edit", buffer.toString(), "edit");
        if (response.contains((CharSequence)"error code=\"editconflict\"")) {
            this.log(Level.WARNING, "Edit conflict on " + title, "edit");
            return;
        }
        try {
            this.checkErrors(response, "edit");
        }
        catch (IOException e) {
            if (this.retry) {
                this.retry = false;
                this.log(Level.WARNING, "Exception: " + e.getMessage() + " Retrying...", "edit");
                this.edit(title, text, summary, minor, bot, section, basetime);
            }
            logger.logp(Level.SEVERE, "Wiki", "edit()", "[" + this.domain + "] EXCEPTION:  ", e);
            throw e;
        }
        if (this.retry) {
            this.log(Level.INFO, "Successfully edited " + title, "edit");
        }
        this.retry = true;
        try {
            long time = (long)this.throttle - System.currentTimeMillis() + start;
            if (time > 0) {
                Thread.sleep(time);
            }
        }
        catch (InterruptedException time) {
            // empty catch block
        }
    }

    public void newSection(String title, String subject, String text, boolean minor, boolean bot) throws IOException, LoginException {
        this.edit(title, text, subject, minor, bot, -1, null);
    }

    public void prepend(String title, String stuff, String summary, boolean minor, boolean bot) throws IOException, LoginException {
        StringBuilder text = new StringBuilder(100000);
        text.append(stuff);
        text.append(this.getSectionText(title, 0));
        this.edit(title, text.toString(), summary, minor, bot, 0, null);
    }

    public synchronized void delete(String title, String reason) throws IOException, LoginException {
        long start = System.currentTimeMillis();
        this.statusCheck();
        if (!(this.user != null && this.user.isAllowedTo("delete"))) {
            throw new CredentialNotFoundException("Cannot delete: Permission denied");
        }
        HashMap<String, Object> info = this.getPageInfo(title);
        if (!((Boolean)info.get("exists")).booleanValue()) {
            logger.log(Level.INFO, "Page \"{0}\" does not exist.", title);
            return;
        }
        String deleteToken = (String)info.get("token");
        StringBuilder buffer = new StringBuilder(500);
        buffer.append("title=");
        buffer.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        buffer.append("&reason=");
        buffer.append(URLEncoder.encode(reason, "UTF-8"));
        buffer.append("&token=");
        buffer.append(URLEncoder.encode(deleteToken, "UTF-8"));
        String response = this.post(String.valueOf(this.apiUrl) + "action=delete", buffer.toString(), "delete");
        try {
            if (!response.contains((CharSequence)"<delete title=")) {
                this.checkErrors(response, "delete");
            }
        }
        catch (IOException e) {
            if (this.retry) {
                this.retry = false;
                this.log(Level.WARNING, "Exception: " + e.getMessage() + " Retrying...", "delete");
                this.delete(title, reason);
            }
            logger.logp(Level.SEVERE, "Wiki", "delete()", "[" + this.domain + "] EXCEPTION:  ", e);
            throw e;
        }
        if (this.retry) {
            this.log(Level.INFO, "Successfully deleted " + title, "delete");
        }
        this.retry = true;
        try {
            long time = (long)this.throttle - System.currentTimeMillis() + start;
            if (time > 0) {
                Thread.sleep(time);
            }
        }
        catch (InterruptedException time) {
            // empty catch block
        }
    }

    public /* varargs */ void purge(boolean links, String ... titles) throws IOException, CredentialNotFoundException {
        if (this.user == null) {
            throw new CredentialNotFoundException("You need to be logged in to purge pages via the API.");
        }
        StringBuilder url = new StringBuilder(this.apiUrl);
        StringBuilder log = new StringBuilder("Successfully purged { \"");
        url.append("action=purge");
        if (links) {
            url.append("&forcelinkupdate");
        }
        url.append("&titles=");
        for (int i = 0; i < titles.length; ++i) {
            url.append(URLEncoder.encode(titles[i], "UTF-8"));
            log.append(titles[i]);
            if (i != titles.length - 1) {
                url.append("%7C");
                log.append("\", ");
                continue;
            }
            log.append("\" }");
        }
        this.fetch(url.toString(), "purge");
        this.log(Level.INFO, log.toString(), "purge");
    }

    public String[] getImagesOnPage(String title) throws IOException {
        String url = String.valueOf(this.query) + "prop=images&imlimit=max&titles=" + URLEncoder.encode(this.normalize(title), "UTF-8");
        String line = this.fetch(url, "getImagesOnPage");
        ArrayList<String> images = new ArrayList<String>(750);
        int a = line.indexOf("title=\"");
        while (a >= 0) {
            int b = line.indexOf("\" ", a);
            images.add(this.decode(line.substring(a + 7, b)));
            a = b;
            a = line.indexOf("title=\"", a);
        }
        images.remove(0);
        this.log(Level.INFO, "Successfully retrieved images used on " + title + " (" + images.size() + " images)", "getImagesOnPage");
        return images.toArray(new String[0]);
    }

    public String[] getCategories(String title) throws IOException {
        String url = String.valueOf(this.query) + "prop=categories&cllimit=max&titles=" + URLEncoder.encode(title, "UTF-8");
        String line = this.fetch(url, "getCategories");
        ArrayList<String> categories = new ArrayList<String>(750);
        while (line.contains((CharSequence)"title=\"Category:")) {
            int a = line.indexOf("title=\"Category:") + 7;
            int b = line.indexOf(34, a);
            categories.add(line.substring(a, b));
            line = line.substring(b);
        }
        this.log(Level.INFO, "Successfully retrieved categories of " + title + " (" + categories.size() + " categories)", "getCategories");
        return categories.toArray(new String[0]);
    }

    public /* varargs */ String[] getTemplates(String title, int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=templates&tllimit=max&titles=");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        this.constructNamespaceString(url, "tl", ns);
        String line = this.fetch(url.toString(), "getTemplates");
        ArrayList<String> templates = new ArrayList<String>(750);
        line = line.substring(line.indexOf("<templates>"));
        while (line.contains((CharSequence)"title=\"")) {
            int a = line.indexOf("title=\"") + 7;
            int b = line.indexOf(34, a);
            templates.add(this.decode(line.substring(a, b)));
            line = line.substring(b);
        }
        this.log(Level.INFO, "Successfully retrieved templates used on " + title + " (" + templates.size() + " templates)", "getTemplates");
        return templates.toArray(new String[0]);
    }

    public HashMap<String, String> getInterwikiLinks(String title) throws IOException {
        String url = String.valueOf(this.apiUrl) + "action=parse&prop=langlinks&llimit=max&page=" + URLEncoder.encode(title, "UTF-8");
        String line = this.fetch(url, "getInterwikiLinks");
        HashMap<String, String> interwikis = new HashMap<String, String>(750);
        while (line.contains((CharSequence)"lang=\"")) {
            int a = line.indexOf("lang=\"") + 6;
            int b = line.indexOf(34, a);
            String language = line.substring(a, b);
            a = line.indexOf(62, a) + 1;
            b = line.indexOf(60, a);
            String page = this.decode(line.substring(a, b));
            interwikis.put(language, page);
            line = line.substring(b);
        }
        this.log(Level.INFO, "Successfully retrieved categories used on " + title, "getCategories");
        return interwikis;
    }

    public String[] getLinksOnPage(String title) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=links&pllimit=max&titles=");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        String plcontinue = "";
        ArrayList<String> links = new ArrayList<String>(750);
        do {
            String line;
            if (!plcontinue.isEmpty()) {
                url.append("&plcontinue=");
                url.append(plcontinue);
            }
            if ((line = this.fetch(url.toString(), "getLinksOnPage")).contains((CharSequence)"plcontinue")) {
                int x = line.indexOf("plcontinue=\"") + 12;
                int y = line.indexOf(34, x);
                plcontinue = URLEncoder.encode(line.substring(x, y), "UTF-8");
            } else {
                plcontinue = "";
            }
            int a = line.indexOf("title=\"");
            while (a >= 0) {
                int b = line.indexOf("\" ", a);
                links.add(this.decode(line.substring(a + 7, b)));
                a = b;
                a = line.indexOf("title=\"", a);
            }
        } while (!plcontinue.isEmpty());
        links.remove(0);
        this.log(Level.INFO, "Successfully retrieved links used on " + title + " (" + links.size() + " links)", "getLinksOnPage");
        return links.toArray(new String[0]);
    }

    public LinkedHashMap<String, String> getSectionMap(String page) throws IOException {
        String url = String.valueOf(this.apiUrl) + "action=parse&text={{:" + URLEncoder.encode(page, "UTF-8") + "}}__TOC__&prop=sections";
        String line = this.fetch(url, "getSectionMap");
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(30);
        while (line.contains((CharSequence)"<s ")) {
            int a = line.indexOf("line=\"") + 6;
            int b = line.indexOf(34, a);
            String title = this.decode(line.substring(a, b));
            a = line.indexOf("number=") + 8;
            b = line.indexOf(34, a);
            String number = line.substring(a, b);
            map.put(number, title);
            line = line.substring(b);
        }
        this.log(Level.INFO, "Successfully retrieved section map for " + page, "getSectionMap");
        return map;
    }

    public Revision getTopRevision(String title) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=revisions&rvlimit=1&rvtoken=rollback&titles=");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        url.append("&rvprop=timestamp%7Cuser%7Cids%7Cflags%7Csize%7Ccomment");
        String line = this.fetch(url.toString(), "getTopRevision");
        int a = line.indexOf("<rev");
        int b = line.indexOf("/>", a);
        if (a < 0) {
            return null;
        }
        return this.parseRevision(line.substring(a, b), title);
    }

    public Revision getFirstRevision(String title) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=revisions&rvlimit=1&rvdir=newer&titles=");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        url.append("&rvprop=timestamp%7Cuser%7Cids%7Cflags%7Csize%7Ccomment");
        String line = this.fetch(url.toString(), "getFirstRevision");
        int a = line.indexOf("<rev");
        int b = line.indexOf("/>", a);
        if (a < 0) {
            return null;
        }
        return this.parseRevision(line.substring(a, b), title);
    }

    public Revision[] getPageHistory(String title) throws IOException {
        return this.getPageHistory(title, null, null);
    }

    public Revision[] getPageHistory(String title, Calendar start, Calendar end) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=revisions&rvlimit=max&titles=");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        url.append("&rvprop=timestamp%7Cuser%7Cids%7Cflags%7Csize%7Ccomment");
        if (end != null) {
            url.append("&rvend=");
            url.append(this.calendarToTimestamp(end));
        }
        url.append("&rvstart");
        String rvstart = "=" + this.calendarToTimestamp(start == null ? this.makeCalendar() : start);
        ArrayList<Revision> revisions = new ArrayList<Revision>(1500);
        do {
            int a;
            int b;
            String temp = url.toString();
            temp = rvstart.charAt(0) != '=' ? String.valueOf(temp) + "id=" + rvstart : String.valueOf(temp) + rvstart;
            String line = this.fetch(temp, "getPageHistory");
            if (line.contains((CharSequence)"rvstartid=\"")) {
                a = line.indexOf("rvstartid") + 11;
                b = line.indexOf(34, a);
                rvstart = line.substring(a, b);
            } else {
                rvstart = "done";
            }
            while (line.contains((CharSequence)"<rev ")) {
                a = line.indexOf("<rev");
                b = line.indexOf("/>", a);
                revisions.add(this.parseRevision(line.substring(a, b), title));
                line = line.substring(b);
            }
        } while (!rvstart.equals("done"));
        this.log(Level.INFO, "Successfully retrieved page history of " + title + " (" + revisions.size() + " revisions)", "getPageHistory");
        return revisions.toArray(new Revision[0]);
    }

    public void move(String title, String newTitle, String reason) throws IOException, LoginException {
        this.move(title, newTitle, reason, false, true, false);
    }

    public synchronized void move(String title, String newTitle, String reason, boolean noredirect, boolean movetalk, boolean movesubpages) throws IOException, LoginException {
        long start = System.currentTimeMillis();
        if (!(this.user != null && this.user.isAllowedTo("move"))) {
            CredentialNotFoundException ex = new CredentialNotFoundException("Permission denied: cannot move pages.");
            logger.logp(Level.SEVERE, "Wiki", "move()", "[" + this.domain + "] Cannot move - permission denied.", ex);
            throw ex;
        }
        this.statusCheck();
        int ns = this.namespace(title);
        if (ns == 6 || ns == 14) {
            throw new UnsupportedOperationException("Tried to move a category/image.");
        }
        HashMap<String, Object> info = this.getPageInfo(title);
        if (!((Boolean)info.get("exists")).booleanValue()) {
            throw new IllegalArgumentException("Tried to move a non-existant page!");
        }
        int level = (Integer)info.get("protection");
        if (!this.checkRights(level, true)) {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            logger.logp(Level.WARNING, "Wiki", "move()", "[" + this.getDomain() + "] Cannot move - permission denied.", ex);
            throw ex;
        }
        String wpMoveToken = (String)info.get("token");
        StringBuilder buffer = new StringBuilder(10000);
        buffer.append("from=");
        buffer.append(URLEncoder.encode(title, "UTF-8"));
        buffer.append("&to=");
        buffer.append(URLEncoder.encode(newTitle, "UTF-8"));
        buffer.append("&reason=");
        buffer.append(URLEncoder.encode(reason, "UTF-8"));
        buffer.append("&token=");
        buffer.append(URLEncoder.encode(wpMoveToken, "UTF-8"));
        if (movetalk) {
            buffer.append("&movetalk=1");
        }
        if (noredirect && this.user.isAllowedTo("suppressredirect")) {
            buffer.append("&noredirect=1");
        }
        if (movesubpages && this.user.isAllowedTo("move-subpages")) {
            buffer.append("&movesubpages=1");
        }
        String response = this.post(String.valueOf(this.apiUrl) + "action=move", buffer.toString(), "move");
        try {
            if (!response.contains((CharSequence)"move from")) {
                this.checkErrors(response, "move");
            }
        }
        catch (IOException e) {
            if (this.retry) {
                this.retry = false;
                this.log(Level.WARNING, "Exception: " + e.getMessage() + " Retrying...", "move");
                this.move(title, newTitle, reason, noredirect, movetalk, movesubpages);
            }
            logger.logp(Level.SEVERE, "Wiki", "move()", "[" + this.domain + "] EXCEPTION:  ", e);
            throw e;
        }
        if (this.retry) {
            this.log(Level.INFO, "Successfully moved " + title + " to " + newTitle, "move");
        }
        this.retry = true;
        try {
            long time = (long)this.throttle - System.currentTimeMillis() + start;
            if (time > 0) {
                Thread.sleep(time);
            }
        }
        catch (InterruptedException time) {
            // empty catch block
        }
    }

    public String export(String title) throws IOException {
        return this.fetch(String.valueOf(this.query) + "export&exportnowrap&titles=" + URLEncoder.encode(this.normalize(title), "UTF-8"), "export");
    }

    public Revision getRevision(long oldid) throws IOException {
        String url = String.valueOf(this.query) + "prop=revisions&rvprop=ids%7Ctimestamp%7Cuser%7Ccomment%7Cflags%7Csize&revids=" + oldid;
        String line = this.fetch(url, "getRevision");
        if (line.contains((CharSequence)"<badrevids>")) {
            return null;
        }
        return this.parseRevision(line, "");
    }

    public void rollback(Revision revision) throws IOException, LoginException {
        this.rollback(revision, this.markbot, "");
    }

    public synchronized void rollback(Revision revision, boolean bot, String reason) throws IOException, LoginException {
        if (!(this.user != null && this.user.isAllowedTo("rollback"))) {
            throw new CredentialNotFoundException("Permission denied: cannot rollback.");
        }
        this.statusCheck();
        if (!this.cookies.containsValue(this.user.getUsername())) {
            logger.log(Level.SEVERE, "Cookies have expired.");
            this.logout();
            throw new CredentialExpiredException("Cookies have expired.");
        }
        Revision top = this.getTopRevision(revision.getPage());
        if (!top.equals(revision)) {
            this.log(Level.INFO, "Rollback failed: revision is not the most recent", "rollback");
            return;
        }
        String token = URLEncoder.encode(top.getRollbackToken(), "UTF-8");
        StringBuilder buffer = new StringBuilder(10000);
        buffer.append("title=");
        buffer.append(revision.getPage());
        buffer.append("&user=");
        buffer.append(revision.getUser());
        buffer.append("&token=");
        buffer.append(token);
        if (bot && this.user.isAllowedTo("markbotedits")) {
            buffer.append("&markbot=1");
        }
        if (!reason.isEmpty()) {
            buffer.append("&summary=");
            buffer.append(reason);
        }
        String response = this.post(String.valueOf(this.apiUrl) + "action=rollback", buffer.toString(), "rollback");
        try {
            if (response.contains((CharSequence)"alreadyrolled")) {
                this.log(Level.INFO, "Edit has already been rolled back.", "rollback");
            } else if (response.contains((CharSequence)"onlyauthor")) {
                this.log(Level.INFO, "Cannot rollback as the page only has one author.", "rollback");
            } else if (!response.contains((CharSequence)"rollback title=")) {
                this.checkErrors(response, "rollback");
            }
        }
        catch (IOException e) {
            if (this.retry) {
                this.retry = false;
                this.log(Level.WARNING, "Exception: " + e.getMessage() + " Retrying...", "rollback");
                this.rollback(revision, bot, reason);
            }
            logger.logp(Level.SEVERE, "Wiki", "rollback()", "[" + this.domain + "] EXCEPTION:  ", e);
            throw e;
        }
        if (this.retry) {
            this.log(Level.INFO, "Successfully reverted edits by " + this.user + " on " + revision.getPage(), "rollback");
        }
        this.retry = true;
    }

    public synchronized void undo(Revision rev, Revision to, String reason, boolean minor, boolean bot) throws IOException, LoginException {
        long start = System.currentTimeMillis();
        this.statusCheck();
        if (!(to == null || rev.getPage().equals(to.getPage()))) {
            throw new IllegalArgumentException("Cannot undo - the revisions supplied are not on the same page!");
        }
        HashMap<String, Object> info = this.getPageInfo(rev.getPage());
        int level = (Integer)info.get("protection");
        if (!this.checkRights(level, false)) {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            logger.logp(Level.WARNING, "Wiki", "undo()", "[" + this.getDomain() + "] Cannot edit - permission denied.", ex);
            throw ex;
        }
        String wpEditToken = (String)info.get("token");
        StringBuilder buffer = new StringBuilder(10000);
        buffer.append("title=");
        buffer.append(rev.getPage());
        if (!reason.isEmpty()) {
            buffer.append("&summary=");
            buffer.append(reason);
        }
        buffer.append("&undo=");
        buffer.append(rev.getRevid());
        if (to != null) {
            buffer.append("&undoafter=");
            buffer.append(to.getRevid());
        }
        if (minor) {
            buffer.append("&minor=1");
        }
        if (bot) {
            buffer.append("&bot=1");
        }
        buffer.append("&token=");
        buffer.append(URLEncoder.encode(wpEditToken, "UTF-8"));
        String response = this.post(String.valueOf(this.apiUrl) + "action=edit", buffer.toString(), "undo");
        try {
            this.checkErrors(response, "undo");
        }
        catch (IOException e) {
            if (this.retry) {
                this.retry = false;
                this.log(Level.WARNING, "Exception: " + e.getMessage() + " Retrying...", "undo");
                this.undo(rev, to, reason, minor, bot);
            }
            logger.logp(Level.SEVERE, "Wiki", "undo()", "[" + this.domain + "] EXCEPTION:  ", e);
            throw e;
        }
        if (this.retry) {
            String log = "Successfully undid revision(s) " + rev.getRevid();
            if (to != null) {
                log = String.valueOf(log) + " - " + to.getRevid();
            }
            this.log(Level.INFO, log, "undo");
        }
        this.retry = true;
        try {
            long time = (long)this.throttle - System.currentTimeMillis() + start;
            if (time > 0) {
                Thread.sleep(time);
            }
        }
        catch (InterruptedException time) {
            // empty catch block
        }
    }

    protected Revision parseRevision(String xml, String title) {
        int size;
        int a = xml.indexOf(" revid=\"") + 8;
        int b = xml.indexOf(34, a);
        long oldid = Long.parseLong(xml.substring(a, b));
        a = xml.indexOf("timestamp=\"") + 11;
        b = xml.indexOf(34, a);
        Calendar timestamp = this.timestampToCalendar(this.convertTimestamp(xml.substring(a, b)));
        if (title.isEmpty()) {
            a = xml.indexOf("title=\"") + 7;
            b = xml.indexOf(34, a);
            title = this.decode(xml.substring(a, b));
        }
        String summary = null;
        if (!xml.contains((CharSequence)"commenthidden=\"")) {
            a = xml.indexOf("comment=\"") + 9;
            b = xml.indexOf(34, a);
            summary = a == 8 ? "" : this.decode(xml.substring(a, b));
        }
        String user2 = null;
        if (xml.contains((CharSequence)"user=\"")) {
            a = xml.indexOf("user=\"") + 6;
            b = xml.indexOf(34, a);
            user2 = this.decode(xml.substring(a, b));
        }
        boolean minor = xml.contains((CharSequence)"minor=\"\"");
        boolean bot = xml.contains((CharSequence)"bot=\"\"");
        boolean rvnew = xml.contains((CharSequence)"new=\"\"");
        if (xml.contains((CharSequence)"newlen=")) {
            a = xml.indexOf("newlen=\"") + 8;
            b = xml.indexOf(34, a);
            size = Integer.parseInt(xml.substring(a, b));
        } else if (xml.contains((CharSequence)"size=\"")) {
            a = xml.indexOf("size=\"") + 6;
            b = xml.indexOf(34, a);
            size = Integer.parseInt(xml.substring(a, b));
        } else {
            size = 0;
        }
        Revision revision = new Revision(oldid, timestamp, title, summary, user2, minor, bot, rvnew, size);
        if (xml.contains((CharSequence)"rcid=\"")) {
            a = xml.indexOf("rcid=\"") + 6;
            b = xml.indexOf(34, a);
            revision.setRcid(Long.parseLong(xml.substring(a, b)));
        }
        if (xml.contains((CharSequence)"rollbacktoken=\"")) {
            a = xml.indexOf("rollbacktoken=\"") + 15;
            b = xml.indexOf(34, a);
            revision.setRollbackToken(xml.substring(a, b));
        }
        return revision;
    }

    public String revisionsToWikitext(Revision[] revisions) {
        StringBuilder sb = new StringBuilder(revisions.length * 100);
        for (int i = 0; i < revisions.length; ++i) {
            StringBuilder base2 = new StringBuilder(50);
            base2.append("<span class=\"plainlinks\">[");
            base2.append(this.base);
            base2.append(revisions[i].getPage().replace((CharSequence)" ", (CharSequence)"_"));
            base2.append("&oldid=");
            base2.append(revisions[i].getRevid());
            sb.append("*(");
            sb.append((CharSequence)base2);
            sb.append("&diff=prev diff]</span>) ");
            Calendar timestamp = revisions[i].getTimestamp();
            sb.append((CharSequence)base2);
            sb.append(" ");
            sb.append(timestamp.get(1));
            sb.append("-");
            int month = timestamp.get(2) + 1;
            if (month < 9) {
                sb.append("0");
            }
            sb.append(month);
            sb.append("-");
            int day = timestamp.get(5);
            if (day < 10) {
                sb.append("0");
            }
            sb.append(day);
            sb.append(" ");
            int hour = timestamp.get(10);
            if (hour < 10) {
                sb.append("0");
            }
            sb.append(hour);
            sb.append(":");
            int minute = timestamp.get(12);
            if (minute < 10) {
                sb.append("0");
            }
            sb.append(minute);
            sb.append("]</span> ");
            String user2 = revisions[i].getUser();
            sb.append("[[User:");
            sb.append(user2);
            sb.append("|");
            sb.append(user2);
            sb.append("]] ([[User talk:");
            sb.append(user2);
            sb.append("|talk]] | [[Special:Contributions/");
            sb.append(user2);
            sb.append("|contribs]]) (");
            String summary = revisions[i].getSummary();
            if (summary.contains((CharSequence)"}}")) {
                int a = summary.indexOf("}}");
                sb.append(summary.substring(0, a));
                sb.append("<nowiki>}}</nowiki>");
                sb.append(summary.substring(a + 2));
            } else {
                sb.append(summary);
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    public byte[] getImage(String title) throws IOException {
        return this.getImage(title, -1, -1);
    }

    public byte[] getImage(String title, int width, int height) throws IOException {
        int c;
        StringBuilder url = new StringBuilder(this.query);
        url.append("prop=imageinfo&iiprop=url&titles=File:");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        url.append("&iiurlwidth=");
        url.append(width);
        url.append("&iiurlheight=");
        url.append(height);
        String line = this.fetch(url.toString(), "getImage");
        int a = line.indexOf("url=\"") + 5;
        int b = line.indexOf(34, a);
        String url2 = line.substring(a, b);
        this.logurl(url2, "getImage");
        URLConnection connection = new URL(url2).openConnection();
        this.setCookies(connection);
        connection.connect();
        BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((c = in.read()) != -1) {
            out.write(c);
        }
        this.log(Level.INFO, "Successfully retrieved image \"" + title + "\"", "getImage");
        return out.toByteArray();
    }

    public HashMap<String, Object> getFileMetadata(String file) throws IOException {
        String url = String.valueOf(this.query) + "prop=imageinfo&iiprop=size%7Cmime%7Cmetadata&titles=File:" + URLEncoder.encode(this.normalize(file), "UTF-8");
        String line = this.fetch(url, "getFileMetadata");
        HashMap<String, Object> metadata = new HashMap<String, Object>(30);
        int a = line.indexOf("size=\"") + 6;
        int b = line.indexOf(34, a);
        metadata.put("size", new Integer(line.substring(a, b)));
        a = line.indexOf("width=\"") + 7;
        b = line.indexOf(34, a);
        metadata.put("width", new Integer(line.substring(a, b)));
        a = line.indexOf("height=\"") + 8;
        b = line.indexOf(34, a);
        metadata.put("height", new Integer(line.substring(a, b)));
        a = line.indexOf("mime=\"") + 6;
        b = line.indexOf(34, a);
        metadata.put("mime", line.substring(a, b));
        while (line.contains((CharSequence)"metadata name=\"")) {
            a = line.indexOf("name=\"") + 6;
            b = line.indexOf(34, a);
            String name = line.substring(a, b);
            a = line.indexOf("value=\"") + 7;
            b = line.indexOf(34, a);
            String value = line.substring(a, b);
            metadata.put(name, value);
            line = line.substring(b);
        }
        return metadata;
    }

    public String[] getDuplicates(String file) throws IOException {
        String url = String.valueOf(this.query) + "prop=duplicatefiles&dflimit=max&titles=File:" + URLEncoder.encode(file, "UTF-8");
        String line = this.fetch(url, "getDuplicates");
        ArrayList<String> duplicates = new ArrayList<String>(10);
        while (line.contains((CharSequence)"<df ")) {
            int a = line.indexOf("name=") + 6;
            int b = line.indexOf(34, a);
            duplicates.add("File:" + line.substring(a, b));
            line = line.substring(b);
        }
        this.log(Level.INFO, "Successfully retrieved duplicates of File:" + file + " (" + duplicates.size() + " files)", "getDuplicates");
        return duplicates.toArray(new String[0]);
    }

    public LogEntry[] getImageHistory(String title) throws IOException {
        String url = String.valueOf(this.query) + "prop=imageinfo&iiprop=timestamp%7Cuser%7Ccomment&iilimit=max&titles=File:" + URLEncoder.encode(this.normalize(title), "UTF-8");
        String line = this.fetch(url, "getImageHistory");
        ArrayList<LogEntry> history = new ArrayList<LogEntry>(40);
        while (line.contains((CharSequence)"<ii ")) {
            int a = line.indexOf("<ii");
            int b = line.indexOf(62, a);
            LogEntry entry = this.parseLogEntry(line.substring(a, b), 2);
            entry.setTarget(title);
            history.add(entry);
            line = line.substring(b);
        }
        LogEntry last = (LogEntry)history.get(history.size() - 1);
        last.setAction("upload");
        history.set(history.size() - 1, last);
        return history.toArray(new LogEntry[0]);
    }

    public byte[] getOldImage(LogEntry entry) throws IOException {
        if (!entry.getType().equals("upload")) {
            throw new IllegalArgumentException("You must provide an upload log entry!");
        }
        String title = entry.getTarget();
        String url = String.valueOf(this.query) + "prop=imageinfo&iilimit=max&iiprop=timestamp%7Curl%7Carchivename&titles=File:" + title;
        String line = this.fetch(url, "getOldImage");
        while (line.contains((CharSequence)"<ii ")) {
            int b;
            int a = line.indexOf("timestamp=") + 11;
            String timestamp = this.convertTimestamp(line.substring(a, b = line.indexOf(34, a)));
            if (timestamp.equals(this.calendarToTimestamp(entry.getTimestamp()))) {
                int c;
                a = line.indexOf(" url=\"") + 6;
                b = line.indexOf(34, a);
                url = line.substring(a, b);
                this.logurl(url, "getOldImage");
                URLConnection connection = new URL(url).openConnection();
                this.setCookies(connection);
                connection.connect();
                BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
                a = line.indexOf("archivename=\"") + 13;
                b = line.indexOf(34, a);
                String archive = line.substring(a, b);
                this.log(Level.INFO, "Successfully retrieved old image \"" + archive + "\"", "getImage");
                return out.toByteArray();
            }
            line = line.substring(b + 10);
        }
        return null;
    }

    public synchronized void upload(File file, String filename, String contents, String reason) throws IOException, LoginException {
        long start = System.currentTimeMillis();
        if (!(this.user != null && this.user.isAllowedTo("upload"))) {
            CredentialNotFoundException ex = new CredentialNotFoundException("Permission denied: cannot upload files.");
            logger.logp(Level.SEVERE, "Wiki", "upload()", "[" + this.domain + "] Cannot upload - permission denied.", ex);
            throw ex;
        }
        this.statusCheck();
        HashMap<String, Object> info = this.getPageInfo("File:" + filename);
        int level = (Integer)info.get("protection");
        if (!this.checkRights(level, false)) {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            logger.logp(Level.WARNING, "Wiki", "upload()", "[" + this.getDomain() + "] Cannot upload - permission denied.", ex);
            throw ex;
        }
        String wpEditToken = (String)info.get("token");
        long filesize = file.length();
        long chunks = (filesize >> 22) + 1;
        FileInputStream fi = new FileInputStream(file);
        String filekey = "";
        int i = 0;
        while ((long)i < chunks) {
            HashMap<String, Object> params = new HashMap<String, Object>(50);
            params.put("filename", filename);
            params.put("token", wpEditToken);
            params.put("ignorewarnings", "true");
            if (chunks == 1) {
                params.put("text", contents);
                if (!reason.isEmpty()) {
                    params.put("comment", reason);
                }
                byte[] by = new byte[fi.available()];
                fi.read(by);
                params.put("file\"; filename=\"" + file.getName(), by);
            } else {
                long offset = i << 22;
                params.put("stash", "1");
                params.put("offset", "" + offset);
                params.put("filesize", "" + filesize);
                if (i != 0) {
                    params.put("filekey", filekey);
                }
                long buffersize = Math.min(0x400000, filesize - offset);
                byte[] by = new byte[(int)buffersize];
                fi.read(by);
                params.put("chunk\"; filename=\"" + file.getName(), by);
                wpEditToken = (String)this.getPageInfo("File:" + filename).get("token");
            }
            String response = this.multipartPost(String.valueOf(this.apiUrl) + "action=upload", params, "upload");
            try {
                if (chunks > 1) {
                    int a = response.indexOf("filekey=\"") + 9;
                    if (a < 0) {
                        throw new IOException("No filekey present! Server response was " + response);
                    }
                    filekey = response.substring(a, response.indexOf(34, a));
                } else {
                    this.checkErrors(response, "upload");
                }
            }
            catch (IOException e) {
                fi.close();
                logger.logp(Level.SEVERE, "Wiki", "upload()", "[" + this.domain + "] EXCEPTION:  ", e);
                throw e;
            }
            ++i;
        }
        fi.close();
        if (chunks > 1) {
            HashMap<String, String> params = new HashMap<String, String>(50);
            params.put("filename", filename);
            params.put("token", wpEditToken);
            params.put("text", contents);
            if (!reason.isEmpty()) {
                params.put("comment", reason);
            }
            params.put("ignorewarnings", "true");
            params.put("filekey", filekey);
            String response = this.multipartPost(String.valueOf(this.apiUrl) + "action=upload", params, "upload");
            this.checkErrors(response, "upload");
        }
        try {
            long time = (long)this.throttle - System.currentTimeMillis() + start;
            if (time > 0) {
                Thread.sleep(time);
            }
        }
        catch (InterruptedException time) {
            // empty catch block
        }
        this.log(Level.INFO, "Successfully uploaded to File:" + filename + ".", "upload");
    }

    public boolean userExists(String username) throws IOException {
        username = URLEncoder.encode(this.normalize(username), "UTF-8");
        return this.fetch(String.valueOf(this.query) + "list=users&ususers=" + username, "userExists").contains((CharSequence)"userid=\"");
    }

    public String[] allUsers(String start, int number) throws IOException {
        String url = String.valueOf(this.query) + "list=allusers&aulimit=" + (number > this.max ? this.max : number) + "&aufrom=";
        ArrayList<String> members = new ArrayList<String>(6667);
        String next = URLEncoder.encode(start, "UTF-8");
        do {
            next = URLEncoder.encode(next, "UTF-8");
            String line = this.fetch(String.valueOf(url) + next, "allUsers");
            int a = line.indexOf("aufrom=\"") + 8;
            next = line.substring(a, line.indexOf("\" />", a));
            int w = line.indexOf("<u ");
            while (w >= 0 && members.size() < number) {
                int x = line.indexOf("name=\"", w) + 6;
                int y = line.indexOf("\"", x);
                members.add(line.substring(x, y));
                w = y;
                w = line.indexOf("<u ", w);
            }
        } while (members.size() < number);
        this.log(Level.INFO, "Successfully retrieved user list (" + number + " users starting at " + start + ")", "allUsers");
        return members.toArray(new String[0]);
    }

    public User getUser(String username) throws IOException {
        return this.userExists(username) ? new User(this.normalize(username)) : null;
    }

    public User getCurrentUser() {
        return this.user;
    }

    public /* varargs */ Revision[] contribs(String user, int ... ns) throws IOException {
        return this.contribs(user, "", null, ns);
    }

    public Revision[] rangeContribs(String range) throws IOException {
        int a = range.indexOf(47);
        if (a < 7) {
            throw new NumberFormatException("Not a valid CIDR range!");
        }
        int size = Integer.parseInt(range.substring(a + 1));
        String[] numbers = range.substring(0, a).split("\\.");
        if (numbers.length != 4) {
            throw new NumberFormatException("Not a valid CIDR range!");
        }
        switch (size) {
            case 8: {
                return this.contribs("", String.valueOf(numbers[0]) + ".", null, new int[0]);
            }
            case 16: {
                return this.contribs("", String.valueOf(numbers[0]) + "." + numbers[1] + ".", null, new int[0]);
            }
            case 24: {
                return this.contribs("", String.valueOf(numbers[0]) + "." + numbers[1] + "." + numbers[2] + ".", null, new int[0]);
            }
            case 32: {
                return this.contribs(range.substring(0, range.length() - 3), "", null, new int[0]);
            }
        }
        throw new NumberFormatException("Range is not supported.");
    }

    public /* varargs */ Revision[] contribs(String user, String prefix, Calendar offset, int ... ns) throws IOException {
        StringBuilder temp = new StringBuilder(this.query);
        temp.append("list=usercontribs&uclimit=max&ucprop=title%7Ctimestamp%7Cflags%7Ccomment%7Cids%7Csize&");
        if (prefix.isEmpty()) {
            temp.append("ucuser=");
            temp.append(URLEncoder.encode(this.normalize(user), "UTF-8"));
        } else {
            temp.append("ucuserprefix=");
            temp.append(prefix);
        }
        this.constructNamespaceString(temp, "uc", ns);
        temp.append("&ucstart=");
        ArrayList<Revision> revisions = new ArrayList<Revision>(7500);
        String ucstart = this.calendarToTimestamp(offset == null ? this.makeCalendar() : offset);
        do {
            String line;
            int aa;
            if ((aa = (line = this.fetch(String.valueOf(temp.toString()) + ucstart, "contribs")).indexOf("ucstart=\"") + 9) < 9) {
                ucstart = "done";
            } else {
                int bb = line.indexOf(34, aa);
                ucstart = line.substring(aa, bb);
            }
            while (line.contains((CharSequence)"<item")) {
                int a = line.indexOf("<item");
                int b = line.indexOf(" />", a);
                revisions.add(this.parseRevision(line.substring(a, b), ""));
                line = line.substring(b);
            }
        } while (!ucstart.equals("done"));
        this.log(Level.INFO, "Successfully retrived contributions for " + (prefix.isEmpty() ? user : prefix) + " (" + revisions.size() + " edits)", "contribs");
        return revisions.toArray(new Revision[0]);
    }

    public synchronized void emailUser(User user, String message, String subject, boolean emailme) throws IOException, LoginException {
        long start = System.currentTimeMillis();
        if (!(this.user != null && this.user.isAllowedTo("sendemail"))) {
            throw new CredentialNotFoundException("Permission denied: cannot email.");
        }
        if (!((Boolean)user.getUserInfo().get("emailable")).booleanValue()) {
            logger.log(Level.WARNING, "User {0} is not emailable", user.getUsername());
            return;
        }
        String token = (String)this.getPageInfo("User:" + user.getUsername()).get("token");
        if (!this.cookies.containsValue(user.getUsername())) {
            logger.log(Level.SEVERE, "Cookies have expired.");
            this.logout();
            throw new CredentialExpiredException("Cookies have expired.");
        }
        StringBuilder buffer = new StringBuilder(20000);
        buffer.append("token=");
        buffer.append(URLEncoder.encode(token, "UTF-8"));
        buffer.append("&target=");
        buffer.append(URLEncoder.encode(user.getUsername(), "UTF-8"));
        if (emailme) {
            buffer.append("&ccme=true");
        }
        buffer.append("&text=");
        buffer.append(URLEncoder.encode(message, "UTF-8"));
        buffer.append("&subject=");
        buffer.append(URLEncoder.encode(subject, "UTF-8"));
        String response = this.post(String.valueOf(this.apiUrl) + "action=emailuser", buffer.toString(), "emailUser");
        this.checkErrors(response, "email");
        if (response.contains((CharSequence)"error code=\"cantsend\"")) {
            throw new UnsupportedOperationException("Email is disabled for this wiki or you do not have a confirmed email address.");
        }
        try {
            long time = (long)this.throttle - System.currentTimeMillis() + start;
            if (time > 0) {
                Thread.sleep(time);
            }
        }
        catch (InterruptedException time) {
            // empty catch block
        }
        this.log(Level.INFO, "Successfully emailed " + user.getUsername() + ".", "emailUser");
    }

    public void watch(String title) throws IOException, CredentialNotFoundException {
        this.watchInternal(title, false);
        this.watchlist.add(title);
    }

    public void unwatch(String title) throws IOException, CredentialNotFoundException {
        this.watchInternal(title, true);
        this.watchlist.remove(title);
    }

    protected void watchInternal(String title, boolean unwatch) throws IOException, CredentialNotFoundException {
        String state;
        String string = state = unwatch ? "unwatch" : "watch";
        if (this.watchlist == null) {
            this.getRawWatchlist();
        }
        StringBuilder data = new StringBuilder("title=");
        data.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        if (unwatch) {
            data.append("&unwatch");
        }
        String watchToken = (String)this.getPageInfo(title).get("watchtoken");
        data.append("&token=");
        data.append(URLEncoder.encode(watchToken, "UTF-8"));
        this.post(String.valueOf(this.apiUrl) + "action=watch", data.toString(), state);
        this.log(Level.INFO, "Successfully " + state + "ed " + title, state);
    }

    public String[] getRawWatchlist() throws IOException, CredentialNotFoundException {
        return this.getRawWatchlist(true);
    }

    public String[] getRawWatchlist(boolean cache) throws IOException, CredentialNotFoundException {
        if (this.user == null) {
            throw new CredentialNotFoundException("The watchlist is available for registered users only.");
        }
        if (this.watchlist != null && cache) {
            return this.watchlist.toArray(new String[0]);
        }
        String url = String.valueOf(this.query) + "list=watchlistraw&wrlimit=max";
        String wrcontinue = "";
        this.watchlist = new ArrayList(750);
        do {
            int a;
            int b;
            String line;
            if ((a = (line = this.fetch(String.valueOf(url) + wrcontinue, "getRawWatchlist")).indexOf("wrcontinue=\"") + 12) > 12) {
                b = line.indexOf(34, a);
                wrcontinue = "&wrcontinue=" + URLEncoder.encode(line.substring(a, b), "UTF-8");
            } else {
                wrcontinue = "done";
            }
            while (line.contains((CharSequence)"<wr ")) {
                a = line.indexOf("title=\"") + 7;
                String title = line.substring(a, b = line.indexOf(34, a));
                if (this.namespace(title) % 2 == 0) {
                    this.watchlist.add(title);
                }
                line = line.substring(b);
            }
        } while (!wrcontinue.equals("done"));
        this.log(Level.INFO, "Successfully retrieved raw watchlist (" + this.watchlist.size() + " items)", "getRawWatchlist");
        return this.watchlist.toArray(new String[0]);
    }

    public boolean isWatched(String title) throws IOException, CredentialNotFoundException {
        if (this.watchlist == null) {
            this.getRawWatchlist();
        }
        return this.watchlist.contains(title);
    }

    public Revision[] watchlist() throws IOException, CredentialNotFoundException {
        return this.watchlist(false, new int[0]);
    }

    public /* varargs */ Revision[] watchlist(boolean allrev, int ... ns) throws IOException, CredentialNotFoundException {
        if (this.user == null) {
            throw new CredentialNotFoundException("Not logged in");
        }
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=watchlist&wlprop=ids%7Ctitle%7Ctimestamp%7Cuser%7Ccomment%7Csizes&wllimit=max");
        if (allrev) {
            url.append("&wlallrev=true");
        }
        this.constructNamespaceString(url, "wl", ns);
        ArrayList<Revision> wl = new ArrayList<Revision>(667);
        boolean done = false;
        String wlstart = "";
        do {
            String line;
            if ((line = this.fetch(String.valueOf(url.toString()) + "&wlstart=" + wlstart, "watchlist")).contains((CharSequence)"wlstart")) {
                int a = line.indexOf("wlstart") + 9;
                wlstart = line.substring(a, line.indexOf(34, a));
            } else {
                done = true;
            }
            int i = line.indexOf("<item ");
            while (i >= 0) {
                int j = line.indexOf("/>", i);
                wl.add(this.parseRevision(line.substring(i, j), ""));
                i = j;
                i = line.indexOf("<item ", i);
            }
        } while (!done);
        this.log(Level.INFO, "Successfully retrieved watchlist (" + wl.size() + " items)", "watchlist");
        return wl.toArray(new Revision[0]);
    }

    public /* varargs */ String[][] search(String search, int ... namespaces) throws IOException {
        if (namespaces.length == 0) {
            namespaces = new int[1];
        }
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=search&srwhat=text&srprop=snippet%7Csectionsnippet&srlimit=max&srsearch=");
        url.append(URLEncoder.encode(search, "UTF-8"));
        this.constructNamespaceString(url, "sr", namespaces);
        url.append("&sroffset=");
        boolean done = false;
        ArrayList<String[]> results = new ArrayList<String[]>(5000);
        while (!done) {
            String line = this.fetch(String.valueOf(url.toString()) + results.size(), "search");
            if (!line.contains((CharSequence)"sroffset=\"")) {
                done = true;
            }
            int x = line.indexOf(" title=\"");
            while (x >= 0) {
                String[] result = new String[3];
                int a = x + 8;
                int b = line.indexOf(34, a);
                result[0] = line.substring(a, b);
                if (line.contains((CharSequence)"sectionsnippet=\"")) {
                    a = line.indexOf("sectionsnippet=\"", x) + 16;
                    b = line.indexOf(34, a);
                    result[1] = this.decode(line.substring(a, b));
                } else {
                    result[1] = "";
                }
                a = line.indexOf("snippet=\"", x) + 9;
                b = line.indexOf(34, a);
                result[2] = this.decode(line.substring(a, b));
                results.add(result);
                x = a;
                x = line.indexOf(" title=\"", x);
            }
        }
        this.log(Level.INFO, "Successfully searched for string \"" + search + "\" (" + results.size() + " items found)", "search");
        return (String[][])results.toArray(new String[0][0]);
    }

    public /* varargs */ String[] imageUsage(String image, int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=imageusage&iulimit=max&iutitle=File:");
        url.append(URLEncoder.encode(this.normalize(image), "UTF-8"));
        this.constructNamespaceString(url, "iu", ns);
        ArrayList<String> pages = new ArrayList<String>(1333);
        String next = "";
        do {
            String line;
            if (!pages.isEmpty()) {
                next = "&iucontinue=" + next;
            }
            if ((line = this.fetch(url + next, "imageUsage")).contains((CharSequence)"iucontinue")) {
                int a = line.indexOf("iucontinue") + 12;
                next = line.substring(a, line.indexOf("\" />", a));
            } else {
                next = "done";
            }
            while (line.contains((CharSequence)"title")) {
                int x = line.indexOf("title=\"");
                int y = line.indexOf("\" />", x);
                pages.add(this.decode(line.substring(x + 7, y)));
                line = line.substring(y + 4);
            }
        } while (!next.equals("done"));
        this.log(Level.INFO, "Successfully retrieved usages of File:" + image + " (" + pages.size() + " items)", "imageUsage");
        return pages.toArray(new String[0]);
    }

    public /* varargs */ String[] whatLinksHere(String title, int ... ns) throws IOException {
        return this.whatLinksHere(title, false, ns);
    }

    public /* varargs */ String[] whatLinksHere(String title, boolean redirects, int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=backlinks&bllimit=max&bltitle=");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        this.constructNamespaceString(url, "bl", ns);
        if (redirects) {
            url.append("&blfilterredir=redirects");
        }
        ArrayList<String> pages = new ArrayList<String>(6667);
        String temp = url.toString();
        String next = "";
        do {
            String line;
            if ((line = this.fetch(String.valueOf(temp) + next, "whatLinksHere")).contains((CharSequence)"blcontinue")) {
                int a = line.indexOf("blcontinue=\"") + 12;
                int b = line.indexOf(34, a);
                next = "&blcontinue=" + line.substring(a, b);
            } else {
                next = "done";
            }
            while (line.contains((CharSequence)"title")) {
                int x = line.indexOf("title=\"");
                int y = line.indexOf("\" ", x);
                pages.add(this.decode(line.substring(x + 7, y)));
                line = line.substring(y + 4);
            }
        } while (!next.equals("done"));
        this.log(Level.INFO, "Successfully retrieved " + (redirects ? "redirects to " : "links to ") + title + " (" + pages.size() + " items)", "whatLinksHere");
        return pages.toArray(new String[0]);
    }

    public /* varargs */ String[] whatTranscludesHere(String title, int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=embeddedin&eilimit=max&eititle=");
        url.append(URLEncoder.encode(this.normalize(title), "UTF-8"));
        this.constructNamespaceString(url, "ei", ns);
        ArrayList<String> pages = new ArrayList<String>(6667);
        String next = "";
        do {
            String line;
            if ((line = this.fetch(url + next, "whatTranscludesHere")).contains((CharSequence)"eicontinue")) {
                int a = line.indexOf("eicontinue=\"") + 12;
                int b = line.indexOf(34, a);
                next = "&eicontinue=" + line.substring(a, b);
            } else {
                next = "done";
            }
            while (line.contains((CharSequence)"title")) {
                int x = line.indexOf("title=\"");
                int y = line.indexOf("\" ", x);
                pages.add(this.decode(line.substring(x + 7, y)));
                line = line.substring(y + 4);
            }
        } while (!next.equals("done"));
        this.log(Level.INFO, "Successfully retrieved transclusions of " + title + " (" + pages.size() + " items)", "whatTranscludesHere");
        return pages.toArray(new String[0]);
    }

    public /* varargs */ String[] getCategoryMembers(String name, int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=categorymembers&cmprop=title&cmlimit=max&cmtitle=Category:");
        url.append(URLEncoder.encode(this.normalize(name), "UTF-8"));
        this.constructNamespaceString(url, "cm", ns);
        ArrayList<String> members = new ArrayList<String>(6667);
        String next = "";
        do {
            String line;
            if (!members.isEmpty()) {
                next = "&cmcontinue=" + URLEncoder.encode(next, "UTF-8");
            }
            if ((line = this.fetch(String.valueOf(url.toString()) + next, "getCategoryMembers")).contains((CharSequence)"cmcontinue")) {
                int a = line.indexOf("cmcontinue") + 12;
                next = line.substring(a, line.indexOf("\" />", a));
            } else {
                next = "done";
            }
            int x = line.indexOf("title=\"");
            while (x >= 0) {
                int y = line.indexOf("\" />", x);
                members.add(this.decode(line.substring(x + 7, y)));
                x = y;
                x = line.indexOf("title=\"", x);
            }
        } while (!next.equals("done"));
        this.log(Level.INFO, "Successfully retrieved contents of Category:" + name + " (" + members.size() + " items)", "getCategoryMembers");
        return members.toArray(new String[0]);
    }

    public ArrayList[] linksearch(String pattern) throws IOException {
        return this.linksearch(pattern, "http", new int[0]);
    }

    public /* varargs */ ArrayList[] linksearch(String pattern, String protocol, int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=exturlusage&euprop=title%7curl&euquery=");
        url.append(pattern);
        url.append("&euprotocol=");
        url.append(protocol);
        url.append("&eulimit=max");
        this.constructNamespaceString(url, "eu", ns);
        url.append("&euoffset=");
        boolean done = false;
        ArrayList[] ret = new ArrayList[]{new ArrayList(667), new ArrayList(667)};
        while (!done) {
            String line = this.fetch(String.valueOf(url.toString()) + ret[0].size(), "linksearch");
            if (!line.contains((CharSequence)"euoffset=\"")) {
                done = true;
            }
            int x = line.indexOf("title=\"");
            while (x >= 0) {
                int y = line.indexOf("\" url=\"", x);
                int z = line.indexOf("\" />", y);
                String title = line.substring(x + 7, y);
                String link = line.substring(y + 7, z);
                ret[0].add(this.decode(title));
                ret[1].add(new URL(link));
                x = z;
                x = line.indexOf("title=\"", x);
            }
        }
        this.log(Level.INFO, "Successfully returned instances of external link " + pattern + " (" + ret[0].size() + " links)", "linksearch");
        return ret;
    }

    public LogEntry[] getIPBlockList(String user) throws IOException {
        return this.getIPBlockList(user, null, null, 1);
    }

    public LogEntry[] getIPBlockList(Calendar start, Calendar end) throws IOException {
        return this.getIPBlockList("", start, end, Integer.MAX_VALUE);
    }

    protected LogEntry[] getIPBlockList(String user, Calendar start, Calendar end, int amount) throws IOException {
        if (start != null && end != null && start.before(end)) {
            throw new IllegalArgumentException("Specified start date is before specified end date!");
        }
        String bkstart = this.calendarToTimestamp(start == null ? this.makeCalendar() : start);
        StringBuilder urlBase = new StringBuilder(this.query);
        urlBase.append("list=blocks");
        if (end != null) {
            urlBase.append("&bkend=");
            urlBase.append(this.calendarToTimestamp(end));
        }
        if (!user.isEmpty()) {
            urlBase.append("&bkusers=");
            urlBase.append(user);
        }
        urlBase.append("&bklimit=");
        urlBase.append(amount < this.max ? amount : this.max);
        urlBase.append("&bkstart=");
        ArrayList<LogEntry> entries = new ArrayList<LogEntry>(1333);
        do {
            String line;
            int a;
            if ((line = this.fetch(String.valueOf(urlBase.toString()) + bkstart, "getIPBlockList")).contains((CharSequence)"bkstart")) {
                a = line.indexOf("bkstart=\"") + 9;
                bkstart = line.substring(a, line.indexOf(34, a));
            } else {
                bkstart = "done";
            }
            while (entries.size() < amount && line.contains((CharSequence)"<block ")) {
                a = line.indexOf("<block ");
                int b = line.indexOf("/>", a);
                entries.add(this.parseLogEntry(line.substring(a, b), 1));
                line = line.substring(b);
            }
        } while (!bkstart.equals("done") && entries.size() < amount);
        StringBuilder logRecord = new StringBuilder("Successfully fetched IP block list ");
        if (!user.isEmpty()) {
            logRecord.append(" for ");
            logRecord.append(user);
        }
        if (start != null) {
            logRecord.append(" from ");
            logRecord.append(start.getTime().toString());
        }
        if (end != null) {
            logRecord.append(" to ");
            logRecord.append(end.getTime().toString());
        }
        logRecord.append(" (");
        logRecord.append(entries.size());
        logRecord.append(" entries)");
        this.log(Level.INFO, logRecord.toString(), "getIPBlockList");
        return entries.toArray(new LogEntry[0]);
    }

    public LogEntry[] getLogEntries(int amount) throws IOException {
        return this.getLogEntries(null, null, amount, "", null, "", 167317762);
    }

    public LogEntry[] getLogEntries(User user) throws IOException {
        return this.getLogEntries(null, null, Integer.MAX_VALUE, "", user, "", 167317762);
    }

    public LogEntry[] getLogEntries(String target) throws IOException {
        return this.getLogEntries(null, null, Integer.MAX_VALUE, "", null, target, 167317762);
    }

    public LogEntry[] getLogEntries(Calendar start, Calendar end) throws IOException {
        return this.getLogEntries(start, end, Integer.MAX_VALUE, "", null, "", 167317762);
    }

    public LogEntry[] getLogEntries(int amount, String type) throws IOException {
        return this.getLogEntries(null, null, amount, type, null, "", 167317762);
    }

    public LogEntry[] getLogEntries(Calendar start, Calendar end, int amount, String log, User user, String target, int namespace) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=logevents&leprop=title%7Ctype%7Cuser%7Ctimestamp%7Ccomment%7Cdetails");
        StringBuilder console = new StringBuilder("Successfully retrieved ");
        if (amount < 1) {
            throw new IllegalArgumentException("Tried to retrieve less than one log entry!");
        }
        if (!log.equals("")) {
            url.append("&letype=");
            url.append(log);
        }
        if (log.equals("newusers")) {
            console.append("user creation");
        } else if (log.equals("delete")) {
            console.append("deletion");
        } else if (log.equals("protect")) {
            console.append("protection");
        } else if (log.equals("rights")) {
            console.append("user rights");
        } else if (log.equals("renameuser")) {
            console.append("user rename");
        } else {
            console.append(" ");
            console.append(log);
        }
        console.append(" log ");
        if (user != null) {
            url.append("&leuser=");
            url.append(URLEncoder.encode(user.getUsername(), "UTF-8"));
            console.append("for ");
            console.append(user.getUsername());
            console.append(" ");
        }
        if (!target.isEmpty()) {
            url.append("&letitle=");
            url.append(URLEncoder.encode(this.normalize(target), "UTF-8"));
            console.append("on ");
            console.append(target);
            console.append(" ");
        }
        url.append("&lelimit=");
        url.append(amount > this.max || namespace != 167317762 ? this.max : amount);
        String lestart = "";
        if (start != null) {
            if (end != null && start.before(end)) {
                throw new IllegalArgumentException("Specified start date is before specified end date!");
            }
            lestart = this.calendarToTimestamp(start).toString();
            console.append("from ");
            console.append(start.getTime().toString());
            console.append(" ");
        }
        if (end != null) {
            url.append("&leend=");
            url.append(this.calendarToTimestamp(end));
            console.append("to ");
            console.append(end.getTime().toString());
            console.append(" ");
        }
        ArrayList<LogEntry> entries = new ArrayList<LogEntry>(6667);
        do {
            String line;
            if ((line = this.fetch(String.valueOf(url.toString()) + "&lestart=" + lestart, "getLogEntries")).contains((CharSequence)"lestart=\"")) {
                int ab = line.indexOf("lestart=\"") + 9;
                lestart = line.substring(ab, line.indexOf(34, ab));
            } else {
                lestart = "done";
            }
            while (line.contains((CharSequence)"<item") && entries.size() < amount) {
                int a = line.indexOf("<item");
                int b = line.indexOf("><item", a);
                if (b < 0) {
                    b = line.length();
                }
                LogEntry entry = this.parseLogEntry(line.substring(a, b), 0);
                line = line.substring(b);
                if (namespace != 167317762 && this.namespace(entry.getTarget()) != namespace) continue;
                entries.add(entry);
            }
        } while (entries.size() < amount && !lestart.equals("done"));
        console.append(" (");
        console.append(entries.size());
        console.append(" entries)");
        this.log(Level.INFO, console.toString(), "getLogEntries");
        return entries.toArray(new LogEntry[0]);
    }

    protected LogEntry parseLogEntry(String xml, int caller) {
        String type;
        String reason;
        int b;
        int a;
        String action = null;
        if (caller == 1) {
            type = "block";
            action = "block";
        } else if (caller == 2) {
            type = "upload";
            action = "overwrite";
        } else {
            int a2 = xml.indexOf("type=\"") + 6;
            int b2 = xml.indexOf("\" ", a2);
            type = xml.substring(a2, b2);
            if (!xml.contains((CharSequence)"actionhidden=\"")) {
                a2 = xml.indexOf("action=\"") + 8;
                b2 = xml.indexOf("\" ", a2);
                action = xml.substring(a2, b2);
            }
        }
        if (xml.contains((CharSequence)"commenthidden=\"")) {
            reason = null;
        } else if (type.equals("newusers")) {
            reason = "";
        } else {
            int a3 = caller == 1 ? xml.indexOf("reason=\"") + 8 : xml.indexOf("comment=\"") + 9;
            int b3 = xml.indexOf(34, a3);
            reason = this.decode(xml.substring(a3, b3));
        }
        String target = null;
        User performer = null;
        if (caller == 1) {
            a = xml.indexOf("by=\"") + 4;
            b = xml.indexOf(34, a);
            performer = new User(this.decode(xml.substring(a, b)));
            a = xml.indexOf("user=\"") + 6;
            if (a < 6) {
                a = xml.indexOf("id=\"") + 4;
                b = xml.indexOf("\" ", a);
                target = "#" + xml.substring(a, b);
            } else {
                b = xml.indexOf("\" ", a);
                target = this.decode(xml.substring(a, b));
            }
        } else if (!xml.contains((CharSequence)"userhidden=\"") && xml.contains((CharSequence)"title=\"")) {
            a = xml.indexOf("user=\"") + 6;
            b = xml.indexOf("\" ", a);
            performer = new User(this.decode(xml.substring(a, b)));
            a = xml.indexOf("title=\"") + 7;
            b = xml.indexOf("\" ", a);
            target = this.decode(xml.substring(a, b));
        } else if (caller == 2) {
            a = xml.indexOf("user=\"") + 6;
            b = xml.indexOf("\" ", a);
            performer = new User(this.decode(xml.substring(a, b)));
        }
        a = xml.indexOf("timestamp=\"") + 11;
        b = a + 20;
        String timestamp = this.convertTimestamp(xml.substring(a, b));
        Object details = null;
        if (xml.contains((CharSequence)"commenthidden")) {
            details = null;
        } else if (type.equals("move")) {
            a = xml.indexOf("new_title=\"") + 11;
            b = xml.indexOf("\" />", a);
            details = this.decode(this.decode(xml.substring(a, b)));
        } else if (type.equals("block")) {
            int c;
            a = xml.indexOf("<block") + 7;
            String s = xml.substring(a);
            int n = c = caller == 1 ? s.indexOf("expiry=") + 8 : s.indexOf("duration=") + 10;
            if (c > 10) {
                int d = s.indexOf(34, c);
                details = new Object[]{s.contains((CharSequence)"anononly"), s.contains((CharSequence)"nocreate"), s.contains((CharSequence)"noautoblock"), s.contains((CharSequence)"noemail"), s.contains((CharSequence)"nousertalk"), s.substring(c, d)};
            }
        } else if (type.equals("protect")) {
            if (action.equals("unprotect")) {
                details = null;
            } else {
                a = xml.indexOf("<param>") + 7;
                b = xml.indexOf("</param>", a);
                String temp = xml.substring(a, b);
                if (action.equals("move_prot")) {
                    details = temp;
                } else if (action.equals("protect") || action.equals("modify")) {
                    details = temp.contains((CharSequence)"create=sysop") ? Integer.valueOf(5) : (temp.contains((CharSequence)"edit=sysop") ? Integer.valueOf(2) : (temp.contains((CharSequence)"move=autoconfirmed") ? Integer.valueOf(1) : (temp.contains((CharSequence)"edit=autoconfirmed") ? Integer.valueOf(4) : (temp.contains((CharSequence)"move=sysop") ? Integer.valueOf(3) : Integer.valueOf(-2)))));
                }
            }
        } else if (type.equals("renameuser")) {
            a = xml.indexOf("<param>") + 7;
            b = xml.indexOf("</param>", a);
            details = this.decode(xml.substring(a, b));
        } else if (type.equals("rights")) {
            a = xml.indexOf("new=\"") + 5;
            b = xml.indexOf(34, a);
            StringTokenizer tk = new StringTokenizer(xml.substring(a, b), ", ");
            ArrayList<String> temp = new ArrayList<String>(10);
            while (tk.hasMoreTokens()) {
                temp.add(tk.nextToken());
            }
            details = temp.toArray(new String[0]);
        }
        return new LogEntry(type, action, reason, performer, target, timestamp, details);
    }

    public String[] prefixIndex(String prefix) throws IOException {
        return this.listPages(prefix, -1, 167317762, -1, -1);
    }

    public String[] shortPages(int cutoff) throws IOException {
        return this.listPages("", -1, 0, -1, cutoff);
    }

    public String[] shortPages(int cutoff, int namespace) throws IOException {
        return this.listPages("", -1, namespace, -1, cutoff);
    }

    public String[] longPages(int cutoff) throws IOException {
        return this.listPages("", -1, 0, cutoff, -1);
    }

    public String[] longPages(int cutoff, int namespace) throws IOException {
        return this.listPages("", -1, namespace, cutoff, -1);
    }

    public String[] listPages(String prefix, int level, int namespace) throws IOException {
        return this.listPages(prefix, level, namespace, -1, -1);
    }

    public String[] listPages(String prefix, int level, int namespace, int minimum, int maximum) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=allpages&aplimit=max");
        if (!prefix.isEmpty()) {
            namespace = this.namespace(prefix);
            if (prefix.contains((CharSequence)":") && namespace != 0) {
                prefix = prefix.substring(prefix.indexOf(58) + 1);
            }
            url.append("&apprefix=");
            url.append(URLEncoder.encode(this.normalize(prefix), "UTF-8"));
        } else if (namespace == 167317762) {
            throw new UnsupportedOperationException("ALL_NAMESPACES not supported in MediaWiki API.");
        }
        url.append("&apnamespace=");
        url.append(namespace);
        switch (level) {
            case -1: {
                break;
            }
            case 1: {
                url.append("&apprlevel=autoconfirmed&apprtype=edit");
                break;
            }
            case 2: {
                url.append("&apprlevel=sysop&apprtype=edit");
                break;
            }
            case 3: {
                url.append("&apprlevel=sysop&apprtype=move");
                break;
            }
            case 4: {
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid protection level!");
            }
        }
        if (minimum != -1) {
            url.append("&apminsize=");
            url.append(minimum);
        }
        if (maximum != -1) {
            url.append("&apmaxsize=");
            url.append(maximum);
        }
        ArrayList<String> pages = new ArrayList<String>(6667);
        String next = "";
        do {
            int a;
            int b;
            String s = url.toString();
            if (!next.isEmpty()) {
                s = String.valueOf(s) + "&apfrom=" + next;
            }
            String line = this.fetch(s, "listPages");
            if (maximum < 0 && minimum < 0 && prefix.isEmpty() && level == -1) {
                next = "done";
            } else if (line.contains((CharSequence)"apfrom=")) {
                a = line.indexOf("apfrom=\"") + 8;
                b = line.indexOf(34, a);
                next = URLEncoder.encode(line.substring(a, b), "UTF-8");
            } else {
                next = "done";
            }
            while (line.contains((CharSequence)"<p ")) {
                a = line.indexOf("title=\"") + 7;
                b = line.indexOf("\" />", a);
                pages.add(this.decode(line.substring(a, b)));
                line = line.substring(b);
            }
        } while (!next.equals("done"));
        this.log(Level.INFO, "Successfully retrieved page list (" + pages.size() + " pages)", "listPages");
        return pages.toArray(new String[0]);
    }

    public Revision[] newPages(int amount) throws IOException {
        return this.recentChanges(amount, 0, true, 0);
    }

    public Revision[] newPages(int amount, int rcoptions) throws IOException {
        return this.recentChanges(amount, rcoptions, true, 0);
    }

    public /* varargs */ Revision[] newPages(int amount, int rcoptions, int ... ns) throws IOException {
        return this.recentChanges(amount, rcoptions, true, ns);
    }

    public Revision[] recentChanges(int amount) throws IOException {
        return this.recentChanges(amount, 0, false, 0);
    }

    public /* varargs */ Revision[] recentChanges(int amount, int ... ns) throws IOException {
        return this.recentChanges(amount, 0, false, ns);
    }

    public /* varargs */ Revision[] recentChanges(int amount, int rcoptions, int ... ns) throws IOException {
        return this.recentChanges(amount, rcoptions, false, ns);
    }

    protected /* varargs */ Revision[] recentChanges(int amount, int rcoptions, boolean newpages, int ... ns) throws IOException {
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=recentchanges&rcprop=title%7Cids%7Cuser%7Ctimestamp%7Cflags%7Ccomment%7Csizes&rclimit=max");
        this.constructNamespaceString(url, "rc", ns);
        if (newpages) {
            url.append("&rctype=new");
        }
        if (rcoptions > 0) {
            url.append("&rcshow=");
            if ((rcoptions & 1) == 1) {
                url.append("!anon%7C");
            }
            if ((rcoptions & 4) == 4) {
                url.append("!self%7C");
            }
            if ((rcoptions & 8) == 8) {
                url.append("!minor%7C");
            }
            if ((rcoptions & 16) == 16) {
                url.append("!patrolled%7C");
            }
            if ((rcoptions & 2) == 2) {
                url.append("!bot%7C");
            }
            url.delete(url.length() - 3, url.length());
        }
        url.append("&rcstart=");
        String rcstart = this.calendarToTimestamp(this.makeCalendar());
        ArrayList<Revision> revisions = new ArrayList<Revision>(750);
        do {
            String temp = url.toString();
            String line = this.fetch(String.valueOf(temp) + rcstart, newpages ? "newPages" : "recentChanges");
            int a = line.indexOf("rcstart=\"") + 9;
            int b = line.indexOf(34, a);
            rcstart = line.substring(a, b);
            int i = line.indexOf("<rc ");
            while (i >= 0 && revisions.size() < amount) {
                int j = line.indexOf("/>", i);
                revisions.add(this.parseRevision(line.substring(i, j), ""));
                i = j;
                i = line.indexOf("<rc ", i);
            }
        } while (revisions.size() < amount);
        return revisions.toArray(new Revision[0]);
    }

    public String[][] getInterWikiBacklinks(String prefix) throws IOException {
        return this.getInterWikiBacklinks(prefix, "|");
    }

    public String[][] getInterWikiBacklinks(String prefix, String title) throws IOException {
        if (title.equals("|") && prefix.isEmpty()) {
            throw new IllegalArgumentException("Interwiki backlinks: title specified without prefix!");
        }
        StringBuilder url = new StringBuilder(this.query);
        url.append("list=iwbacklinks&iwbllimit=max&iwblprefix=");
        url.append(prefix);
        if (!title.equals("|")) {
            url.append("&iwbltitle=");
            url.append(title);
        }
        url.append("&iwblprop=iwtitle%7Ciwprefix");
        String iwblcontinue = "";
        ArrayList<String[]> links = new ArrayList<String[]>(500);
        do {
            String line = "";
            line = iwblcontinue.isEmpty() ? this.fetch(url.toString(), "getInterWikiBacklinks") : this.fetch(String.valueOf(url.toString()) + "&iwblcontinue=" + iwblcontinue, "getInterWikiBacklinks");
            if (line.contains((CharSequence)"iwblcontinue")) {
                int a = line.indexOf("iwblcontinue=\"") + 14;
                int b = line.indexOf(34, a);
                iwblcontinue = line.substring(a, b);
            } else {
                iwblcontinue = "";
            }
            int x = line.indexOf("<iw ");
            while (x >= 0) {
                int a = line.indexOf("title=\"", x) + 7;
                int b = line.indexOf(34, a);
                int c = line.indexOf("iwprefix=\"", x) + 10;
                int d = line.indexOf(34, c);
                int e = line.indexOf("iwtitle=\"", x) + 9;
                int f = line.indexOf(34, e);
                links.add(new String[]{line.substring(a, b), String.valueOf(line.substring(c, d)) + ':' + line.substring(e, f)});
                x = f;
                x = line.indexOf("<iw ", x);
            }
        } while (!iwblcontinue.isEmpty());
        this.log(Level.INFO, "Successfully retrieved interwiki backlinks (" + links.size() + " interwikis)", "getInterWikiBacklinks");
        return (String[][])links.toArray(new String[0][0]);
    }

    protected String fetch(String url, String caller) throws IOException {
        String line;
        this.logurl(url, caller);
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(180000);
        this.setCookies(connection);
        connection.connect();
        this.grabCookies(connection);
        int lag = connection.getHeaderFieldInt("X-Database-Lag", -5);
        if (lag > this.maxlag) {
            try {
                Wiki wiki = this;
                synchronized (wiki) {
                    int time = connection.getHeaderFieldInt("Retry-After", 10);
                    this.log(Level.WARNING, "Current database lag " + lag + " s exceeds " + this.maxlag + " s, waiting " + time + " s.", caller);
                    Thread.sleep(time * 1000);
                }
            }
            catch (InterruptedException var5_6) {
                // empty catch block
            }
            return this.fetch(url, caller);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader((InputStream)(this.zipped ? new GZIPInputStream(connection.getInputStream()) : connection.getInputStream()), "UTF-8"));
        StringBuilder text = new StringBuilder(100000);
        while ((line = in.readLine()) != null) {
            text.append(line);
            text.append("\n");
        }
        in.close();
        String temp = text.toString();
        if (temp.contains((CharSequence)"<error code=")) {
            throw new UnknownError("MW API error. Server response was: " + temp);
        }
        return temp;
    }

    protected String post(String url, String text, String caller) throws IOException {
        String line;
        this.logurl(url, caller);
        URLConnection connection = new URL(url).openConnection();
        this.setCookies(connection);
        connection.setDoOutput(true);
        connection.connect();
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
        out.write(text);
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader((InputStream)(this.zipped ? new GZIPInputStream(connection.getInputStream()) : connection.getInputStream()), "UTF-8"));
        this.grabCookies(connection);
        StringBuilder temp = new StringBuilder(100000);
        while ((line = in.readLine()) != null) {
            temp.append(line);
            temp.append("\n");
        }
        in.close();
        return temp.toString();
    }

    protected String multipartPost(String url, Map<String, ?> params, String caller) throws IOException {
        String line;
        this.logurl(url, caller);
        URLConnection connection = new URL(url).openConnection();
        String boundary = "----------NEXT PART----------";
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        this.setCookies(connection);
        connection.setDoOutput(true);
        connection.connect();
        boundary = "--" + boundary + "\r\n";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bout);
        out.writeBytes(boundary);
        for (Map.Entry entry : params.entrySet()) {
            String name = (String)entry.getKey();
            Object value = entry.getValue();
            out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
            if (value instanceof String) {
                out.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                out.writeBytes((String)value);
            } else if (value instanceof byte[]) {
                out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                out.write((byte[])value);
            } else {
                throw new UnsupportedOperationException("Unrecognized data type");
            }
            out.writeBytes("\r\n");
            out.writeBytes(boundary);
        }
        out.writeBytes("--\r\n");
        out.close();
        OutputStream uout = connection.getOutputStream();
        uout.write(bout.toByteArray());
        uout.close();
        BufferedReader in = new BufferedReader(new InputStreamReader((InputStream)(this.zipped ? new GZIPInputStream(connection.getInputStream()) : connection.getInputStream()), "UTF-8"));
        this.grabCookies(connection);
        StringBuilder temp = new StringBuilder(100000);
        while ((line = in.readLine()) != null) {
            temp.append(line);
            temp.append("\n");
        }
        in.close();
        return temp.toString();
    }

    protected void checkErrors(String line, String caller) throws IOException, LoginException {
        if (line.isEmpty()) {
            throw new UnknownError("Received empty response from server!");
        }
        if (line.contains((CharSequence)"result=\"Success\"")) {
            return;
        }
        if (line.contains((CharSequence)"error code=\"ratelimited\"")) {
            this.log(Level.WARNING, "Server-side throttle hit.", caller);
            throw new HttpRetryException("Action throttled.", 503);
        }
        if (line.contains((CharSequence)"error code=\"blocked") || line.contains((CharSequence)"error code=\"autoblocked\"")) {
            this.log(Level.SEVERE, "Cannot " + caller + " - user is blocked!.", caller);
            throw new AccountLockedException("Current user is blocked!");
        }
        if (line.contains((CharSequence)"error code=\"cascadeprotected\"")) {
            this.log(Level.WARNING, "Cannot " + caller + " - page is subject to cascading protection.", caller);
            throw new CredentialException("Page is cascade protected");
        }
        if (line.contains((CharSequence)"error code=\"readonly\"")) {
            this.log(Level.WARNING, "Database locked!", caller);
            throw new HttpRetryException("Database locked!", 503);
        }
        if (line.contains((CharSequence)"error code=\"unknownerror\"")) {
            throw new UnknownError("Unknown MediaWiki API error, response was " + line);
        }
        throw new IOException("MediaWiki error, response was " + line);
    }

    protected String decode(String in) {
        in = in.replace((CharSequence)"&lt;", (CharSequence)"<").replace((CharSequence)"&gt;", (CharSequence)">");
        in = in.replace((CharSequence)"&amp;", (CharSequence)"&");
        in = in.replace((CharSequence)"&quot;", (CharSequence)"\"");
        in = in.replace((CharSequence)"&#039;", (CharSequence)"'");
        return in;
    }

    protected /* varargs */ void constructNamespaceString(StringBuilder sb, String id, int ... namespaces) {
        if (namespaces.length == 0) {
            return;
        }
        sb.append("&");
        sb.append(id);
        sb.append("namespace=");
        for (int namespace : namespaces) {
            sb.append(namespace);
            sb.append("%7C");
        }
        sb.delete(sb.length() - 3, sb.length());
    }

    public String normalize(String s) {
        char[] temp = s.toCharArray();
        temp[0] = Character.toUpperCase(temp[0]);
        for (int i = 0; i < temp.length; ++i) {
            switch (temp[i]) {
                case '#': 
                case '[': 
                case ']': 
                case '{': 
                case '|': 
                case '}': {
                    throw new IllegalArgumentException(String.valueOf(s) + " is an illegal title");
                }
                case ' ': {
                    temp[i] = 95;
                }
            }
        }
        return new String(temp);
    }

    protected boolean checkRights(int level, boolean move) throws IOException, CredentialException {
        String username = this.user.getUsername().replace('_', '+');
        if (!this.cookies.containsValue(username)) {
            logger.log(Level.SEVERE, "Cookies have expired");
            this.logout();
            throw new CredentialExpiredException("Cookies have expired.");
        }
        if (this.user.isA("sysop")) {
            return true;
        }
        switch (level) {
            case -1: {
                return true;
            }
            case 1: {
                if (this.user != null) {
                    return true;
                }
                return false;
            }
            case 3: 
            case 4: {
                return !move;
            }
        }
        return false;
    }

    protected void statusCheck() throws IOException, CredentialException {
        if (this.statuscounter > this.statusinterval) {
            if (this.user != null) {
                this.user.getUserInfo();
            }
            if ((this.assertion & 0x4) == 4) {
               assert (!this.hasNewMessages()) : "User has new messages";
            }
            this.statuscounter = 0;
        } else {
            ++this.statuscounter;
        }
        if ((this.assertion & 0x1) == 1) {
            assert (this.user != null) : "Not logged in";
        }
        if ((this.assertion & 0x2) != 2) {
            assert (this.user.isA("bot")) : "Not a bot";
        }
    }

    protected void setCookies(URLConnection u) {
        StringBuilder cookie = new StringBuilder(100);
        for (Map.Entry<String, String> entry : this.cookies.entrySet()) {
            cookie.append(entry.getKey());
            cookie.append("=");
            cookie.append(entry.getValue());
            cookie.append("; ");
        }
        u.setRequestProperty("Cookie", cookie.toString());
        if (this.zipped) {
            u.setRequestProperty("Accept-encoding", "gzip");
        }
        u.setRequestProperty("User-Agent", this.useragent);
    }

    private void grabCookies(URLConnection u) {
        String headerName = null;
        int i = 1;
        while ((headerName = u.getHeaderFieldKey(i)) != null) {
            if (headerName.equals("Set-Cookie")) {
                String cookie = u.getHeaderField(i);
                cookie = cookie.substring(0, cookie.indexOf(59));
                String name = cookie.substring(0, cookie.indexOf(61));
                String value = cookie.substring(cookie.indexOf(61) + 1, cookie.length());
                this.cookies.put(name, value);
            }
            ++i;
        }
    }

    protected void log(Level level, String text, String method) {
        StringBuilder sb = new StringBuilder(100);
        sb.append('[');
        sb.append(this.domain);
        sb.append("] ");
        sb.append(text);
        sb.append('.');
        logger.logp(level, "Wiki", String.valueOf(method) + "()", sb.toString());
    }

    @Deprecated
    public void setLogLevel(Level level) {
        logger.setLevel(level);
    }

    protected void logurl(String url, String method) {
        logger.logp(Level.FINE, "Wiki", String.valueOf(method) + "()", "Fetching URL " + url);
    }

    public Calendar makeCalendar() {
        return new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    }

    protected String calendarToTimestamp(Calendar c) {
        return String.format("%04d%02d%02d%02d%02d%02d", c.get(1), c.get(2) + 1, c.get(5), c.get(11), c.get(12), c.get(13));
    }

    protected final Calendar timestampToCalendar(String timestamp) {
        Calendar calendar = this.makeCalendar();
        int year = Integer.parseInt(timestamp.substring(0, 4));
        int month = Integer.parseInt(timestamp.substring(4, 6)) - 1;
        int day = Integer.parseInt(timestamp.substring(6, 8));
        int hour = Integer.parseInt(timestamp.substring(8, 10));
        int minute = Integer.parseInt(timestamp.substring(10, 12));
        int second = Integer.parseInt(timestamp.substring(12, 14));
        calendar.set(year, month, day, hour, minute, second);
        return calendar;
    }

    protected String convertTimestamp(String timestamp) {
        StringBuilder ts = new StringBuilder(timestamp.substring(0, 4));
        ts.append(timestamp.substring(5, 7));
        ts.append(timestamp.substring(8, 10));
        ts.append(timestamp.substring(11, 13));
        ts.append(timestamp.substring(14, 16));
        ts.append(timestamp.substring(17, 19));
        return ts.toString();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(this.user.getUsername());
        out.writeObject(this.cookies);
        out.writeInt(this.throttle);
        out.writeInt(this.maxlag);
        out.writeInt(this.assertion);
        out.writeObject(this.scriptPath);
        out.writeObject(this.domain);
        out.writeObject(this.namespaces);
        out.writeInt(this.statusinterval);
        out.writeObject(this.useragent);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String z = (String)in.readObject();
        this.user = new User(z);
        this.cookies = (HashMap)in.readObject();
        this.throttle = in.readInt();
        this.maxlag = in.readInt();
        this.assertion = in.readInt();
        this.scriptPath = (String)in.readObject();
        this.domain = (String)in.readObject();
        this.namespaces = (HashMap)in.readObject();
        this.statusinterval = in.readInt();
        this.useragent = (String)in.readObject();
        this.initVars();
        this.statuscounter = this.statusinterval;
    }

    public static enum Gender {
        male,
        female,
        unknown;
        

        /*private Gender(String string2, int n2) {
        }*/
    }

    public class LogEntry
    implements Comparable<LogEntry> {
        private String type;
        private String action;
        private String reason;
        private User user;
        private String target;
        private Calendar timestamp;
        private Object details;

        protected LogEntry(String type, String action, String reason, User user, String target, String timestamp, Object details) {
            this.type = type;
            this.action = action;
            this.reason = reason;
            this.user = user;
            this.target = target;
            this.timestamp = Wiki.this.timestampToCalendar(timestamp);
            this.details = details;
        }

        public String getType() {
            return this.type;
        }

        public String getAction() {
            return this.action;
        }

        public String getReason() {
            return this.reason;
        }

        public User getUser() {
            return this.user;
        }

        public String getTarget() {
            return this.target;
        }

        public Calendar getTimestamp() {
            return this.timestamp;
        }

        public Object getDetails() {
            return this.details;
        }

        public String toString() {
            StringBuilder s = new StringBuilder("LogEntry[type=");
            s.append(this.type);
            s.append(",action=");
            s.append(this.action == null ? "[hidden]" : this.action);
            s.append(",user=");
            s.append(this.user == null ? "[hidden]" : this.user.getUsername());
            s.append(",timestamp=");
            s.append(Wiki.this.calendarToTimestamp(this.timestamp));
            s.append(",target=");
            s.append(this.target == null ? "[hidden]" : this.target);
            s.append(",reason=");
            s.append(this.reason == null ? "[hidden]" : this.reason);
            s.append(",details=");
            if (this.details instanceof Object[]) {
                s.append(Arrays.asList((Object[])this.details));
            } else {
                s.append(this.details);
            }
            s.append("]");
            return s.toString();
        }

        @Override
        public int compareTo(LogEntry other) {
            if (this.timestamp.equals(other.timestamp)) {
                return 0;
            }
            return this.timestamp.after(other.timestamp) ? 1 : -1;
        }

        void setTarget(String string) {
            this.target = string;
        }

        void setAction(String string) {
            this.action = string;
        }
    }

    public class Revision
    implements Comparable<Revision> {
        private boolean minor;
        private boolean bot;
        private boolean rvnew;
        private String summary;
        private long revid;
        private long rcid;
        private Calendar timestamp;
        private String user;
        private String title;
        private String rollbacktoken;
        private int size;

        public Revision(long revid, Calendar timestamp, String title, String summary, String user, boolean minor, boolean bot, boolean rvnew, int size) {
            this.rcid = -1;
            this.rollbacktoken = null;
            this.size = 0;
            this.revid = revid;
            this.timestamp = timestamp;
            this.summary = summary;
            this.minor = minor;
            this.user = user;
            this.title = title;
            this.bot = bot;
            this.rvnew = rvnew;
            this.size = size;
        }

        public String getText() throws IOException {
            if (this.revid == 0) {
                throw new IllegalArgumentException("Log entries have no valid content!");
            }
            String url = String.valueOf(Wiki.this.base) + URLEncoder.encode(this.title, "UTF-8") + "&oldid=" + this.revid + "&action=raw";
            String temp = Wiki.this.fetch(url, "Revision.getText");
            Wiki.this.log(Level.INFO, "Successfully retrieved text of revision " + this.revid, "Revision.getText");
            return Wiki.this.decode(temp);
        }

        public String getRenderedText() throws IOException {
            if (this.revid == 0) {
                throw new IllegalArgumentException("Log entries have no valid content!");
            }
            String url = String.valueOf(Wiki.this.base) + URLEncoder.encode(this.title, "UTF-8") + "&oldid=" + this.revid + "&action=render";
            String temp = Wiki.this.fetch(url, "Revision.getRenderedText");
            Wiki.this.log(Level.INFO, "Successfully retrieved rendered text of revision " + this.revid, "Revision.getRenderedText");
            return Wiki.this.decode(temp);
        }

        public String diff(Revision other) throws IOException {
            return this.diff(other.revid, "");
        }

        public String diff(String text) throws IOException {
            return this.diff(0, text);
        }

        public String diff(long oldid) throws IOException {
            return this.diff(oldid, "");
        }

        protected String diff(long oldid, String text) throws IOException {
            StringBuilder temp = new StringBuilder("prop=revisions&revids=");
            temp.append(this.revid);
            if (oldid == -1) {
                temp.append("&rvdiffto=next");
            } else if (oldid == -2) {
                temp.append("&rvdiffto=cur");
            } else if (oldid == -3) {
                temp.append("&rvdiffto=prev");
            } else if (oldid == 0) {
                temp.append("&rvdifftotext=");
                temp.append(text);
            } else {
                temp.append("&rvdiffto=");
                temp.append(oldid);
            }
            String line = Wiki.this.post(Wiki.this.query, temp.toString(), "Revision.diff");
            int a = line.indexOf("<diff");
            a = line.indexOf(">", a) + 1;
            int b = line.indexOf("</diff>", a);
            return Wiki.this.decode(line.substring(a, b));
        }

        public boolean equals(Object o) {
            if (!(o instanceof Revision)) {
                return false;
            }
            return this.toString().equals(o.toString());
        }

        public int hashCode() {
            return (int)this.revid * 2 - Wiki.this.hashCode();
        }

        public boolean isMinor() {
            return this.minor;
        }

        public boolean isBot() {
            return this.bot;
        }

        public boolean isNew() {
            return this.rvnew;
        }

        public String getSummary() {
            return this.summary;
        }

        public String getUser() {
            return this.user;
        }

        public String getPage() {
            return this.title;
        }

        public long getRevid() {
            return this.revid;
        }

        public Calendar getTimestamp() {
            return this.timestamp;
        }

        public int getSize() {
            return this.size;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Revision[oldid=");
            sb.append(this.revid);
            sb.append(",page=\"");
            sb.append(this.title);
            sb.append("\",user=");
            sb.append(this.user == null ? "[hidden]" : this.user);
            sb.append(",timestamp=");
            sb.append(Wiki.this.calendarToTimestamp(this.timestamp));
            sb.append(",summary=\"");
            sb.append(this.summary == null ? "[hidden]" : this.summary);
            sb.append("\",minor=");
            sb.append(this.minor);
            sb.append(",bot=");
            sb.append(this.bot);
            sb.append(",size=");
            sb.append(this.size);
            sb.append(",rcid=");
            sb.append((Object)(this.rcid == -1 ? "unset" : Long.valueOf(this.rcid)));
            sb.append(",rollbacktoken=");
            sb.append(this.rollbacktoken == null ? "null" : this.rollbacktoken);
            sb.append("]");
            return sb.toString();
        }

        @Override
        public int compareTo(Revision other) {
            if (this.timestamp.equals(other.timestamp)) {
                return 0;
            }
            return this.timestamp.after(other.timestamp) ? 1 : -1;
        }

        public void setRcid(long rcid) {
            this.rcid = rcid;
        }

        public long getRcid() {
            return this.rcid;
        }

        public void setRollbackToken(String token) {
            this.rollbacktoken = token;
        }

        public String getRollbackToken() {
            return this.rollbacktoken;
        }

        public void rollback() throws IOException, LoginException {
            Wiki.this.rollback(this, false, "");
        }

        public void rollback(boolean bot, String reason) throws IOException, LoginException {
            Wiki.this.rollback(this, bot, reason);
        }
    }

    public class User
    implements Cloneable {
        private String username;
        private String[] rights;
        private String[] groups;

        protected User(String username) {
            this.rights = null;
            this.groups = null;
            this.username = username;
        }

        public String getUsername() {
            return this.username;
        }

        public HashMap<String, Object> getUserInfo() throws IOException {
            String info = Wiki.this.fetch(String.valueOf(Wiki.this.query) + "list=users&usprop=editcount%7Cgroups%7Crights%7Cemailable%7Cblockinfo%7Cgender&ususers=" + URLEncoder.encode(this.username, "UTF-8"), "getUserInfo");
            HashMap<String, Object> ret = new HashMap<String, Object>(10);
            ret.put("blocked", info.contains((CharSequence)"blockedby=\""));
            ret.put("emailable", info.contains((CharSequence)"emailable=\""));
            int a = info.indexOf("editcount=\"") + 11;
            int b = info.indexOf(34, a);
            ret.put("editcount", Integer.parseInt(info.substring(a, b)));
            a = info.indexOf("gender=\"") + 8;
            b = info.indexOf(34, a);
            ret.put("gender", (Object)Gender.valueOf(info.substring(a, b)));
            ArrayList<String> temp = new ArrayList<String>(50);
            int x = info.indexOf("<g>");
            while (x >= 0) {
                int y = info.indexOf("</g>", x);
                temp.add(info.substring(x + 3, y));
                x = y;
                x = info.indexOf("<g>", x);
            }
            String[] temp2 = temp.toArray(new String[0]);
            if (this.equals(Wiki.this.getCurrentUser())) {
                this.groups = temp2;
            }
            ret.put("groups", temp2);
            temp.clear();
            int x2 = info.indexOf("<r>");
            while (x2 >= 0) {
                int y = info.indexOf("</r>", x2);
                temp.add(info.substring(x2 + 3, y));
                x2 = y;
                x2 = info.indexOf("<r>", x2);
            }
            temp2 = temp.toArray(new String[0]);
            if (this.equals(Wiki.this.getCurrentUser())) {
                this.rights = temp2;
            }
            ret.put("rights", temp2);
            return ret;
        }

        public boolean isAllowedTo(String right) throws IOException {
            if (this.rights == null) {
                this.rights = (String[])this.getUserInfo().get("rights");
            }
            for (String r : this.rights) {
                if (!r.equals(right)) continue;
                return true;
            }
            return false;
        }

        public boolean isA(String group) throws IOException {
            if (this.groups == null) {
                this.groups = (String[])this.getUserInfo().get("groups");
            }
            for (String g : this.groups) {
                if (!g.equals(group)) continue;
                return true;
            }
            return false;
        }

        public LogEntry[] blockLog() throws IOException {
            return Wiki.this.getLogEntries(null, null, Integer.MAX_VALUE, "block", null, "User:" + this.username, 2);
        }

        public boolean isBlocked() throws IOException {
            if (Wiki.this.getIPBlockList(this.username, null, null, 1).length != 0) {
                return true;
            }
            return false;
        }

        public int countEdits() throws IOException {
            return (Integer)this.getUserInfo().get("editcount");
        }

        public /* varargs */ Revision[] contribs(int ... ns) throws IOException {
            return Wiki.this.contribs(this.username, ns);
        }

        public User clone() {
            try {
                return (User)super.clone();
            }
            catch (CloneNotSupportedException e) {
                return null;
            }
        }

        public boolean equals(Object x) {
            if (x instanceof User && this.username.equals(((User)x).username)) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder temp = new StringBuilder("User[username=");
            temp.append(this.username);
            temp.append("groups=");
            temp.append(this.groups != null ? Arrays.toString(this.groups) : "unset");
            temp.append("]");
            return temp.toString();
        }

        public int hashCode() {
            return this.username.hashCode() * 2 + 1;
        }
    }

}

