package org.wikipedia.tools;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.wikipedia.Wiki;

public class IndianEducationCCI
{
  public static void main(String[] args)
    throws IOException
  {
    Wiki enWiki = new Wiki("en.wikipedia.org");
    Wiki commons = new Wiki("commons.wikimedia.org");
    
    GregorianCalendar cal = new GregorianCalendar(2011, 5, 1);
    

    ArrayList<String> users = new ArrayList(1500);
    BufferedReader in = new BufferedReader(new InputStreamReader(
      IndianEducationCCI.class.getResourceAsStream("iepstudents.txt")));
    String line;
    while ((line = in.readLine()) != null)
    {
      users.add(line.substring(5));
    }
    FileWriter out = new FileWriter("iep.txt");
    for (String user : users)
    {
      Wiki.User u = enWiki.getUser(user);
      int editcount = 0;
      if (u != null)
      {
        editcount = u.countEdits();
      }
      else
      {
        System.out.println(user + " is not a registered user.");
        continue;
      }
      Wiki.Revision[] contribs = enWiki.contribs(user, new int[0]);
      out.write("===" + user + "===\n");
      out.write("*{{user5|" + user + "}}\n");
      out.write("*Total edits: " + editcount + ", Live edits: " + contribs.length + 
        ", Deleted edits: " + (editcount - contribs.length) + "\n\n");
      if (contribs.length >= 200)
      {
        out.write("User has too many live edits for this hack. Use the [http:// toolserver.org/~dcoetzee/contributionsurveyor/index.php Contribution Surveyor].\n\n");
      }
      else
      {
        out.write(";Mainspace edits");
        

        HashMap<String, StringBuilder> diffs = new HashMap(60);
        for (Wiki.Revision revision : contribs)
        {
          String title = revision.getPage();
          
          int ns = enWiki.namespace(title);
          if (ns == 0)
          {
            Wiki.Revision[] history = enWiki.getPageHistory(title, revision.getTimestamp(), cal);
            if (history.length == 0)
            {
              System.out.println(user + " has contributions prior to the IEP.");
            }
            else
            {
              int size = history.length == 1 ? revision.getSize() : revision.getSize() - history[1].getSize();
              if (size >= 150)
              {
                if (!diffs.containsKey(title))
                {
                  StringBuilder temp = new StringBuilder(500);
                  temp.append("\n*[[:");
                  temp.append(title);
                  temp.append("]]: ");
                  diffs.put(title, temp);
                }
                StringBuilder temp = (StringBuilder)diffs.get(title);
                temp.append("{{dif|");
                temp.append(revision.getRevid());
                temp.append("|(+");
                temp.append(size);
                temp.append(")}}");
                diffs.put(title, temp);
              }
            }
          }
        }
        for (Map.Entry<String, StringBuilder> entry : diffs.entrySet()) {
          out.write(((StringBuilder)entry.getValue()).toString());
        }
        if (diffs.isEmpty()) {
          out.write("\nNo major mainspace contributions.");
        }
        out.write("\n\n");
        

        out.write(";Userspace edits\n");
        HashSet<String> temp = new HashSet(50);
        for (Wiki.Revision revision : contribs)
        {
          String title = revision.getPage();
          
          int ns = enWiki.namespace(title);
          if (ns == 2) {
            temp.add(title);
          }
        }
        if (temp.isEmpty()) {
          out.write("No userspace edits.\n");
        } else {
          out.write(Wiki.formatList((String[])temp.toArray(new String[0])));
        }
        out.write("\n");
        

        out.write(";Local uploads\n");
        Wiki.LogEntry[] uploads = enWiki.getLogEntries(null, null, 2147483647, "upload", u, "", 167317762);
        Object list = new HashSet(10000);
        for (int i = 0; i < uploads.length; i++) {
          ((HashSet)list).add(uploads[i].getTarget());
        }
        if (uploads.length == 0) {
          out.write("No local uploads.\n");
        } else {
          out.write(Wiki.formatList((String[])((HashSet)list).toArray(new String[0])));
        }
        out.write("\n");
        

        out.write(";Commons uploads\n");
        uploads = commons.getLogEntries(null, null, 2147483647, "upload", u, "", 167317762);
        ((HashSet)list).clear();
        for (int i = 0; i < uploads.length; i++) {
          ((HashSet)list).add(uploads[i].getTarget());
        }
        if (uploads.length == 0) {
          out.write("No Commons uploads.\n");
        } else {
          out.write(Wiki.formatList((String[])((HashSet)list).toArray(new String[0])));
        }
        out.write("\n");
      }
    }
    out.flush();
    out.close();
  }
}


/* Location:           S:\github\Up\Upv0.2.jar
 * Qualified Name:     org.wikipedia.tools.IndianEducationCCI
 * JD-Core Version:    0.7.0.1
 */