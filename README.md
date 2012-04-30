mwsync-lib
==========

About
----------
__mwsync__ is a framework for creating a live, one-way 'mirror' of one MediaWiki onto another. It can be used for anything from maintaining a live mirror of the entire English Wikipedia to integrating multiple MediaWiki sites into one.
It was created as part of the [GeneWiki+](http://genewikiplus.org) project, which takes the 10,400+ human gene pages on [Wikipedia](http://en.wikipedia.org) and integrates them with a [mutation database](http://snpedia.com) and an ontology of human diseases. We've extracted the core of that project for general use.

Dependencies
------------
There are no dependencies! 
    
Installation
------------
As a library, you will use __mwsync__ to build your own custom Sync programs. We recommend using it as a Maven dependency, but it's not necessary. Use of a Java IDE such as [Eclipse](http://eclipse.org) with a Maven plugin can simplify things greatly.

To install __mwsync__ as a Maven module, follow the same instructions as above:

```bash
hg clone https://bitbucket.org/sulab/mwsync
cd mwsync
mvn install
```
    
From Eclipse, you might alternatively import it as a Maven project, right-click the project folder, and choose "Run as -> Maven Install".

Alternatively, you may choose to export it as a .jar file if you do not wish to use Maven in your own projects. From Eclipse, choose Export -> Jar File and follow the instructions provided.

Finally, in your own projects, import the final product into your classpath.
__As a maven module:__
Find and add `edu.scripps.mwsync` as a Maven dependency
__As a standalone .jar file__:
In Eclipse, add mwsync.jar (or whatever you named it during export) to the Java Build Path (Project -> Properties... Java Build Path)

Usage
------
__To create a bare-bones, one-way mirror of a selection of pages on Wikipedia:__

1. Create an account on Wikipedia and add the selection of pages to your watchlist. One way to add a large number of pages at once is to generate a list of titles (one per line) and add them to the [raw watchlist editor](http://en.wikipedia.org/wiki/Special:EditWatchlist/raw). You can also do this programmatically using the API.

2. With your account information for both Wikipedia and the target MediaWiki site you wish to use as a mirror, edit `mwsync.example.conf`, filling in the information for each field. Save this file as `mwsync.conf` somewhere on the filesystem. That location will be where temporary and log files are written, so ensure the permissions are correct.

3. Create a new Java project (if using Eclipse) and ensure __mwsync-lib__ is in your classpath and importable by your code. The code below builds a basic sync app:

Basic Sync App:

```java
    import edu.scripps.mwsync.*
    
    public class MySyncApp
    {
        public static void main(String[] args) throws Exception
        {
            Sync sync = Sync.newFromConfigFile("/etc/mysyncapp/sync.conf");
            sync.run();
        }
    }
```
	
Add the following directive to your `crontab` file (on *nix systems) to run every 5 minutes:

    */5 * *   *   *   root  java -jar <PATH_TO_SYNC_JAR>/sync.jar

For Windows, use Scheduled Tasks or the `at` command.

__To create a mirror that _alters content_ before posting to target:__

1. Follow the directions as above to set up your accounts.

2. Create a class that implements the Rewriter interface (see example below)

3. Attach your custom Rewriter object to the Sync object, then run.

Sync App with Custom Rewriter:

    ```java
    import edu.scripps.mwsync.*
    import edu.scripps.genewiki.common.Wiki;

    public class MySyncApp
    {
        static class CustomRewriter implements Rewriter
        {
            /*
             * Replace all instances of 'cats' with 'dogs'
             */
            public String process(String text, String title, Wiki source,
                Wiki target)
            {
                return text.replaceAll("cats", "dogs");
            }
        }
        
        public static void main(String[] args) 
        {
            try {
                Sync sync = Sync.newFromConfigFile("/etc/mysyncapp/sync.conf");
                CustomRewriter rewriter = new CustomRewriter();
                sync.addRewriter(rewriter);
                sync.run();
            // of course, your error handling will be more specific...
            catch (Exception e) {  
                system.out.println("Problem running sync!");
                system.exit(1)
        }
    }
	```
	
Running on a schedule:
---------------------
The Sync library itself does not handle repeated runs, so in order to maintain up-to-date data, you have to schedule it with your OS's task scheduler. The easiest is simply to use Unix `cron`, but alternatives exist.
The sync checks for the last time it was run. If it can't find a previous time, it skips back a day and pulls all changes since that time. To force this reset, delete _lastChecked.cal.ser_ in the mwsync folder.

Common Rewrite Rules
----------
Below are some generic rewrite rules you may want to use if porting a MediaWiki site to a Semantic MediaWiki mirror:

Appending a 'Mirrored' banner to each page:
    
    ```java
    public static String prependMirroredTemplate(String src){
        if (src.startsWith("{{Mirrored")) {
            return src;
        } else {
            return "{{Mirrored | {{PAGENAME}} }} \n" + src;
        }
    }
	```
	
Adding a semantic property to all links, except already-typed semantic links, category links, and namespaces, or, if a link does not exist on the source Wiki, making the link point back to Wikipedia:

    ```java
    public static String fixLinks(String src, Wiki target) {
        
        // add a marker to all the links so we can iterate through them (except the ones that point to wikipedia)
        String src2 = src.replaceAll("\\[\\[(?!(wikipedia))", "[[%");
        
        try {
            while (src2.contains("[[%")) {
                
                // extract the link text
                int a = src2.indexOf("[[%")+3;  // left inner bound
                int b = src2.indexOf("]]", a);  // right inner bound
                String link = src2.substring(a, b);
                
                // remove the label, if present
                int pipe = link.indexOf('|');
                link = (pipe == -1) ? link : src2.substring(a, a+pipe);

                // If the link does not exist (and is not a semantic wikilink or category), append 'wikipedia:'
                if (!link.contains("::") && !link.contains(":") && !link.contains("Category:") && !target.exists(link)[0]) {
                    src2 = src2.substring(0, a-1) + "wikipedia:" + src2.substring(a);
                // else if it's not a special link (like File: or en:), make it a semantic link
                } else if (!link.contains(":")) {
                    src2 = src2.substring(0, a-1) + "is_associated_with::" + src2.substring(a);
                // otherwise just remove the marker and move on
                } else {
                    src2 = src2.substring(0, a-1) + src2.substring(a);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return src;
        }
        return src2;
    }
	```

To see the full suite of rewrite rules that we use for building genewiki+, visit [our gwsync repository](http://bitbucket.org/sulab/gwsync) and find the GeneWikiEditor class.

Bugs and Errata
-----------------
The included MediaWiki Java API has been forked and customized to use the SMW Ask API extension. Note, however, that this is not identical to the Ask API incorporated into Semantic Mediawiki > 1.7. To adapt the Wiki class, update the "ask" functions to use '&query=' instead of '&q=' (and possibly other changes... we haven't upgraded to 1.7 yet so this is untested.)

Contact
------------
Please feel free to email the developer for support or questions at eclarke \at\ scripps \dot\ edu.
