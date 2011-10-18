/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

/**
 * This class produces the RSS samples we use in our tests. Right now we're
 * using excerpts from a real life feed, but that could change to a more
 * lightweight approach.
 *
 * Feed components are stored as inline string to minimize overhead while
 * testing.
 *
 * @author Mihai Balan
 */
public class FeedFactory {
    /**
     * String representing the header of a RSS feed.
     */
    private static String rssHeader =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    +"<rss version=\"2.0\""
    +"  xmlns:content=\"http://purl.org/rss/1.0/modules/content/\""
    +"  xmlns:wfw=\"http://wellformedweb.org/CommentAPI/\""
    +"  xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
    +"  <channel>"
    +"    <title>miChou's photoblog</title>"
    +"    <link>http://mihaibalan.wordpress.com</link>"
    +"    <description>The world through my eyes</description>"
    +"    <pubDate>Mon, 20 Aug 2007 22:20:57 +0000</pubDate>"
    +"    <generator>http://wordpress.org/?v=MU</generator>"
    +"    <language>ro</language>";

    /**
     * String containing the body of a RSS feed.
     */
    private static String rssBody =
    "    <item>"
    +"      <title>Milky wave</title>"
    +"      <link>http://mihaibalan.wordpress.com/2007/08/15/milky-wave/</link>"
    +"      <comments>http://mihaibalan.wordpress.com/2007/08/15/milky-wave/#co"
    +"mments</comments>"
    +"      <pubDate>Tue, 14 Aug 2007 22:02:48 +0000</pubDate>"
    +"      <dc:creator>mich0u</dc:creator>"
    +"      <category><![CDATA[flash]]></category>"
    +"      <guid isPermaLink=\"false\">http://mihaibalan.wordpress.com/2007/08"
    +"/15/milky-wave/</guid>"
    +"      <description><![CDATA["
    +"]]></description>"
    +"      <content:encoded><![CDATA[<div class='snap_preview'><p align=\"cent"
    +"er\"><a href=\"http://www.flickr.com/photos/mi-chou/1092690810/\" title="
    +"\"Photo Sharing\"><img src=\"http://farm2.static.flickr.com/1419/10926908"
    +"10_79510d9695.jpg\" alt=\"Milky wave\" style=\"border:medium none;\" heig"
    +"ht=\"288\" width=\"500\" /></a></p>"
    +"</div>]]></content:encoded>"
    +"      <wfw:commentRss>http://mihaibalan.wordpress.com/2007/08/15/milky-"
    +"wave/feed/</wfw:commentRss>"
    +"    </item>"
    +"    <item>"
    +"      <title>Light patterns</title>"
    +"      <link>http://mihaibalan.wordpress.com/2007/08/14/light-patterns/</"
    +"link>"
    +"      <comments>http://mihaibalan.wordpress.com/2007/08/14/light-pattern"
    +"s/#comments</comments>"
    +"      <pubDate>Mon, 13 Aug 2007 22:02:32 +0000</pubDate>"
    +"      <dc:creator>mich0u</dc:creator>"
    +"      <category><![CDATA[dark]]></category>"
    +"      <category><![CDATA[architecture]]></category>"
    +"      <guid isPermaLink=\"false\">http://mihaibalan.wordpress.com/2007/08"
    +"/14/light-patterns/</guid>"
    +"      <description><![CDATA["
    +"]]></description>"
    +"      <content:encoded><![CDATA[<div class='snap_preview'><p align=\"cent"
    +"er\"><a href=\"http://www.flickr.com/photos/mi-chou/1091949941/\" title="
    +"\"Photo Sharing\"><img src=\"http://farm2.static.flickr.com/1055/10919499"
    +"41_34853b5130.jpg\" alt=\"Light patterns\" style=\"border:medium none;\" "
    +"height=\"386\" width=\"500\" /></a></p>"
    +"</div>]]></content:encoded>"
    +"      <wfw:commentRss>http://mihaibalan.wordpress.com/2007/08/14/light-p"
    +"atterns/feed/</wfw:commentRss>"
    +"    </item>";

    /**
     * String containing the updates to the RSS feed.
     */
    private static String rssBodyUpdate =
    "    <item>"
    +"      <title>Tuig - The show goes on</title>"
    +"      <link>http://mihaibalan.wordpress.com/2007/08/17/tuig-the-show-goes"
    +"-on/</link>"
    +"      <comments>http://mihaibalan.wordpress.com/2007/08/17/tuig-the-show"
    +"-goes-on/#comments</comments>"
    +"      <pubDate>Thu, 16 Aug 2007 22:05:53 +0000</pubDate>"
    +"      <dc:creator>mich0u</dc:creator>"
    +"      <category><![CDATA[water]]></category>"
    +"      <category><![CDATA[people]]></category>"
    +"      <guid isPermaLink=\"false\">http://mihaibalan.wordpress.com/2007/08"
    +"/17/tuig-the-show-goes-on/</guid>"
    +"      <description><![CDATA["
    +"]]></description>"
    +"      <content:encoded><![CDATA[<div class='snap_preview'><p align=\"cen"
    +"ter\"><a href=\"http://www.flickr.com/photos/mi-chou/1030251126/\" title="
    +"\"Photo Sharing\"><img src=\"http://farm2.static.flickr.com/1361/10302511"
    +"26_9a5d311039.jpg\" alt=\"Tuig - The show goes on\" style=\"border:medium"
    +" none;\" height=\"340\" width=\"500\" /></a></p>"
    +"</div>]]></content:encoded>"
    +"      <wfw:commentRss>http://mihaibalan.wordpress.com/2007/08/17/tuig-the"
    +"-show-goes-on/feed/</wfw:commentRss>"
    +"    </item>"
    +"    <item>"
    +"      <title>Tuig - Patiently waiting</title>"
    +"      <link>http://mihaibalan.wordpress.com/2007/08/16/tuig-patiently-wai"
    +"ting/</link>"
    +"      <comments>http://mihaibalan.wordpress.com/2007/08/16/tuig-patiently"
    +"-waiting/#comments</comments>"
    +"      <pubDate>Wed, 15 Aug 2007 22:03:55 +0000</pubDate>"
    +"      <dc:creator>mich0u</dc:creator>"
    +"      <category><![CDATA[sky]]></category>"
    +"      <category><![CDATA[urban]]></category>"
    +"      <category><![CDATA[dark]]></category>"
    +"      <guid isPermaLink=\"false\">http://mihaibalan.wordpress.com/2007/0"
    +"8/16/tuig-patiently-waiting/</guid>"
    +"      <description><![CDATA["
    +"]]></description>"
    +"      <content:encoded><![CDATA[<div class='snap_preview'><p align=\"cent"
    +"er\"><a href=\"http://www.flickr.com/photos/mi-chou/1030250394/\" title="
    +"\"Photo Sharing\"><img src=\"http://farm2.static.flickr.com/1029/10302503"
    +"94_687df58297.jpg\" alt=\"Tuig - Patiently waiting\" style=\"border:mediu"
    +"m none;\" height=\"500\" width=\"410\" /></a></p>"
    +"<p> <a href=\"http://mihaibalan.wordpress.com/2007/08/16/tuig-patiently-w"
    +"aiting/#more-118\" class=\"more-link\">(more&#8230;)</a></p>"
    +"</div>]]></content:encoded>"
    +"      <wfw:commentRss>http://mihaibalan.wordpress.com/2007/08/16/tuig-pa"
    +"tiently-waiting/feed/</wfw:commentRss>"
    +"    </item>";

    /**
     * String containing the body of a new RSS feed.
     */
    private static String rssBodyNew = "    <item>"
    +"      <title>I see trees of green and skies of blue</title>"
    +"      <link>http://mihaibalan.wordpress.com/2007/08/21/i-see-trees-of-gr"
    +"een-and-skies-of-blue/</link>"
    +"      <comments>http://mihaibalan.wordpress.com/2007/08/21/i-see-trees-of"
    +"-green-and-skies-of-blue/#comments</comments>"
    +"      <pubDate>Mon, 20 Aug 2007 22:20:57 +0000</pubDate>"
    +"      <dc:creator>mich0u</dc:creator>"
    +"      <category><![CDATA[tree]]></category>"
    +"      <category><![CDATA[bucharest]]></category>"
    +"      <category><![CDATA[architecture]]></category>"
    +"      <guid isPermaLink=\"false\">http://mihaibalan.wordpress.com/2007"
    +"/08/21/i-see-trees-of-green-and-skies-of-blue/</guid>"
    +"      <description><![CDATA["
    +"]]></description>"
    +"      <content:encoded><![CDATA[<div class='snap_preview'><p align=\"cent"
    +"er\"><a href=\"http://www.zooomr.com/photos/michou/2997760/\" title=\"Ph"
    +"oto Sharing\"><img src=\"http://static.zooomr.com/images/2997760_f5a39d2a"
    +"db.jpg\" alt=\"I see trees of green, and skies of blue\" style=\"border:"
    +"medium none;\" height=\"340\" width=\"500\" /></a></p>"
    +"</div>]]></content:encoded>"
    +"      <wfw:commentRss>http://mihaibalan.wordpress.com/2007/08/21/i-see"
    +"-trees-of-green-and-skies-of-blue/feed/</wfw:commentRss>"
    +"    </item>"
    +"    <item>"
    +"      <title>Tuig - If I could turn back time</title>"
    +"      <link>http://mihaibalan.wordpress.com/2007/08/20/tuig-if-i-could-"
    +"turn-back-time/</link>"
    +"      <comments>http://mihaibalan.wordpress.com/2007/08/20/tuig-if-i-"
    +"could-turn-back-time/#comments</comments>"
    +"      <pubDate>Sun, 19 Aug 2007 22:05:53 +0000</pubDate>"
    +"      <dc:creator>mich0u</dc:creator>"
    +"      <category><![CDATA[light]]></category>"
    +"      <category><![CDATA[long exposure]]></category>"
    +"      <category><![CDATA[dark]]></category>"
    +"      <guid isPermaLink=\"false\">http://mihaibalan.wordpress.com/2007/"
    +"08/20/tuig-if-i-could-turn-back-time/</guid>"
    +"      <description><![CDATA["
    +"]]></description>"
    +"      <content:encoded><![CDATA[<div class='snap_preview'><p align=\"cent"
    +"er\"><a href=\"http://www.flickr.com/photos/mi-chou/1029399905/\" title="
    +"\"Photo Sharing\"><img src=\"http://farm2.static.flickr.com/1191/10293999"
    +"05_08e5d41b89.jpg\" alt=\"Tuig - If I could roll back time\" style=\""
    +"border:medium none;\" height=\"383\" width=\"500\" /></a></p>"
    +"<p> <a href=\"http://mihaibalan.wordpress.com/2007/08/20/tuig-if-i-could-"
    +"turn-back-time/#more-122\" class=\"more-link\">(more&#8230;)</a></p>"
    +"</div>]]></content:encoded>"
    +"      <wfw:commentRss>http://mihaibalan.wordpress.com/2007/08/20/tuig-if"
    +"-i-could-turn-back-time/feed/</wfw:commentRss>"
    +"    </item>";

    /**
     * String containing some invalid mark-up in the RSS feed.
     */
    private static String rssBodyInvalid =
    "<some-invalid-tag attr='value1'>Lorem ipsum</some-invalid-tg>";

    /**
     * String containing the footer of a RSS feed.
     */
    private static String rssFooter = "</channel></rss>";

    /**
     * String containing the header of a ATOM feed.
     */
    private static String atomHeader =
    "<?xml version='1.0' encoding='UTF-8'?>"
    +"<?xml-stylesheet href=\"http://www.blogger.com/styles/atom.css\" type=\""
    +"text/css\"?>"
    +"<feed xmlns='http://www.w3.org/2005/Atom' xmlns:openSearch='http://a9.co"
    +"m/-/spec/opensearchrss/1.0/'>"
    +"    <id>tag:blogger.com,1999:blog-3490925879145756058</id><updated>2007-"
    +"08-08T10:24:42.424-07:00</updated><title type='text'>miChou's Summer of "
    +"Code blog</title><link rel='alternate' type='text/html' href='http://ete"
    +"-de-code-a-la-chou.blogspot.com/'/>"
    +"    <link rel='http://schemas.google.com/g/2005#feed' type='application/"
    +"atom+xml' href='http://ete-de-code-a-la-chou.blogspot.com/feeds/posts/de"
    +"fault'/>"
    +"    <link rel='self' type='application/atom+xml' href='http://ete-de-cod"
    +"e-a-la-chou.blogspot.com/feeds/posts/default'/>"
    +"    <author>"
    +"        <name>Mihai Balan</name>"
    +"    </author>"
    +"    <generator version='7.00' uri='http://www.blogger.com'>Blogger</gene"
    +"rator><openSearch:totalResults>6</openSearch:totalResults><openSearch:st"
    +"artIndex>1</openSearch:startIndex><openSearch:itemsPerPage>25</openSearc"
    +"h:itemsPerPage>";

    /**
     * String containing the body of an ATOM feed.
     */
    private static String atomBody =
    "<entry><id>tag:blogger.com,1999:blog-3490925879145756058.post-445569517"
    +"5541848605</id><published>2007-06-05T02:15:00.000-07:00</published><upda"
    +"ted>2007-06-05T03:34:35.892-07:00</updated><category scheme='http://www."
    +"blogger.com/atom/ns#' term='soc'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='todo'/>"
    +""
    +"        <title type='text'>Happy hacking!</title><content type='html'>Fi"
    +"nally the second semester is over, and the summer exams sessions is abou"
    +"t to begin (that means I have the first exam tomorrow - Digital Computer"
    +"s 2).And in between all this, I should also get my exams going :)</conte"
    +"nt><link rel='alternate' type='text/html' href='http://ete-de-code-a-la-"
    +"chou.blogspot.com/2007/06/happy-hacking.html' title='Happy hacking!'/>"
    +"        <link rel='replies' type='text/html' href='http://www.blogger.co"
    +"m/comment.g?blogID=3490925879145756058&amp;postID=4455695175541848605' t"
    +"itle='0 Comments'/>"
    +"        <link rel='replies' type='application/atom+xml' href='http://ete"
    +"-de-code-a-la-chou.blogspot.com/feeds/4455695175541848605/comments/defau"
    +"lt' title='Post Comments'/>"
    +"        <link rel='self' type='application/atom+xml' href='http://ete-de"
    +"-code-a-la-chou.blogspot.com/feeds/posts/default/4455695175541848605'/>"
    +"        <link rel='edit' type='application/atom+xml' href='http://www.bl"
    +"ogger.com/feeds/3490925879145756058/posts/default/4455695175541848605'/>"
    +""
    +"        <author>"
    +"            <name>Mihai Balan</name>"
    +"        </author>"
    +"    </entry>"
    +"    <entry>"
    +"        <id>tag:blogger.com,1999:blog-3490925879145756058.post-160551373"
    +"861169805</id><published>2007-05-31T13:34:00.000-07:00</published><updat"
    +"ed>2007-06-01T04:21:00.167-07:00</updated><category scheme='http://www.b"
    +"logger.com/atom/ns#' term='soc'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='francai"
    +"s'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='bla-bla"
    +"'/>"
    +"        <title type='text'>Un debut attarde</title><content type='html'>"
    +"&lt;span style=\"font-style: italic;font-size:100%;\" &gt;&lt;span style="
    +"\"font-family:georgia;\"&gt;Bien qu'on m'ait dit que toute communication "
    +"regardant SIP se fera en anglais je prends le risque d'ecrire ce premier "
    +"post en francais.</content><link rel='alternate' type='text/html' href='h"
    +"ttp://ete-de-code-a-la-chou.blogspot.com/2007/05/un-debut-attarde.html' "
    +"title='Un debut attarde'/>"
    +"        <link rel='replies' type='text/html' href='http://www.blogger.co"
    +"m/comment.g?blogID=3490925879145756058&amp;postID=160551373861169805' ti"
    +"tle='0 Comments'/>"
    +"        <link rel='replies' type='application/atom+xml' href='http://ete"
    +"-de-code-a-la-chou.blogspot.com/feeds/160551373861169805/comments/defaul"
    +"t' title='Post Comments'/>"
    +"        <link rel='self' type='application/atom+xml' href='http://ete-de"
    +"-code-a-la-chou.blogspot.com/feeds/posts/default/160551373861169805'/>"
    +"        <link rel='edit' type='application/atom+xml' href='http://www.bl"
    +"ogger.com/feeds/3490925879145756058/posts/default/160551373861169805'/>"
    +"        <author>"
    +"            <name>Mihai Balan</name>"
    +"        </author>"
    +"    </entry>";

    /**
     * String containing the updates to the ATOM feed.
     */
    private static String atomBodyUpdate =
    "<entry><id>tag:blogger.com,1999:blog-3490925879145756058.post-722978588"
    +"3825532193</id><published>2007-06-25T14:29:00.000-07:00</published><upda"
    +"ted>2007-06-26T06:12:44.479-07:00</updated><category scheme='http://www."
    +"blogger.com/atom/ns#' term='bugzilla'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='todo'/>"
    +""
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='google'"
    +"/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='rss'/>"
    +"        <title type='text'>Google politics</title><content type='html'>O"
    +"ne of the problems well known in the current implementation of the RSS s"
    +"upport in SIP Communicator was the inability to retrieve feeds from news"
    +".google.com (and you must admit, news.google.com is quite a source of ne"
    +"ws ;) ).&lt;br /&gt;&lt;br /&gt;  A brief look at the exception returned"
    +" by the plugin points out the problem. The server is sending a HTTP/403 "
    +"(Forbidden) response code instead of the HTTP/200 (OK) response code.</c"
    +"ontent><link rel='alternate' type='text/html' href='http://ete-de-code-a"
    +"-la-chou.blogspot.com/2007/06/google-politics.html' title='Google politi"
    +"cs'/>"
    +"        <link rel='replies' type='text/html' href='http://www.blogger.co"
    +"m/comment.g?blogID=3490925879145756058&amp;postID=7229785883825532193' t"
    +"itle='0 Comments'/>"
    +"        <link rel='replies' type='application/atom+xml' href='http://ete"
    +"-de-code-a-la-chou.blogspot.com/feeds/7229785883825532193/comments/defau"
    +"lt' title='Post Comments'/>"
    +"        <link rel='self' type='application/atom+xml' href='http://ete-de"
    +"-code-a-la-chou.blogspot.com/feeds/posts/default/7229785883825532193'/>"
    +"        <link rel='edit' type='application/atom+xml' href='http://www.bl"
    +"ogger.com/feeds/3490925879145756058/posts/default/7229785883825532193'/>"
    +""
    +"        <author>"
    +"            <name>Mihai Balan</name>"
    +"        </author>"
    +"    </entry>";

    /**
     * String containing a new ATOM feed.
     */
    private static String atomBodyNew =
    "<entry><id>tag:blogger.com,1999:blog-3490925879145756058.post-325829288"
    +"5225110686</id><published>2007-08-08T09:59:00.000-07:00</published><upda"
    +"ted>2007-08-08T10:24:42.470-07:00</updated><category scheme='http://www."
    +"blogger.com/atom/ns#' term='todo'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='new fea"
    +"ture'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='emil'/>"
    +""
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='rss'/>"
    +"        <title type='text'>check out. again...</title><content type='htm"
    +"l'>&lt;div style=\"text-align: justify;\"&gt;It seems I do have a strange"
    +" problem when it comes to using CVS. It's not version control systems in "
    +"general, but CVS. Or at least, the CVS I'm using here, on Sip Communicat"
    +"or. Somehow, I never get it right.</content><link rel='alternate' type='"
    +"text/html' href='http://ete-de-code-a-la-chou.blogspot.com/2007/08/check"
    +"-out-again.html' title='check out. again...'/>"
    +"        <link rel='replies' type='text/html' href='http://www.blogger.co"
    +"m/comment.g?blogID=3490925879145756058&amp;postID=3258292885225110686' t"
    +"itle='0 Comments'/>"
    +"        <link rel='replies' type='application/atom+xml' href='http://ete"
    +"-de-code-a-la-chou.blogspot.com/feeds/3258292885225110686/comments/defau"
    +"lt' title='Post Comments'/>"
    +"        <link rel='self' type='application/atom+xml' href='http://ete-de"
    +"-code-a-la-chou.blogspot.com/feeds/posts/default/3258292885225110686'/>"
    +"        <link rel='edit' type='application/atom+xml' href='http://www.bl"
    +"ogger.com/feeds/3490925879145756058/posts/default/3258292885225110686'/>"
    +""
    +"        <author>"
    +"            <name>Mihai Balan</name>"
    +"        </author>"
    +"    </entry>"
    +"    <entry>"
    +"        <id>tag:blogger.com,1999:blog-3490925879145756058.post-186104691"
    +"5714479007</id><published>2007-07-06T13:23:00.000-07:00</published><upda"
    +"ted>2007-07-06T13:54:46.478-07:00</updated><category scheme='http://www."
    +"blogger.com/atom/ns#' term='new feature'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='questio"
    +"n'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='rss'/>"
    +"        <title type='text'>Favicon - check!</title><content type='html'>"
    +"&lt;div style=\"text-align: justify;\"&gt;Finally, favicon retrieval supp"
    +"ort is here. What is this all about? Almost all IM protocols out there al"
    +"low you to specify and associate an image to your account (a so called a"
    +"vatar), either natively (e.g. Y!M), either through extensions (e.g. XMPP"
    +"/Jabber).</content><link rel='alternate' type='text/html' href='http://e"
    +"te-de-code-a-la-chou.blogspot.com/2007/07/favicon-check.html' title='Fav"
    +"icon - check!'/>"
    +"        <link rel='replies' type='text/html' href='http://www.blogger.co"
    +"m/comment.g?blogID=3490925879145756058&amp;postID=1861046915714479007' t"
    +"itle='0 Comments'/>"
    +"        <link rel='replies' type='application/atom+xml' href='http://ete"
    +"-de-code-a-la-chou.blogspot.com/feeds/1861046915714479007/comments/defau"
    +"lt' title='Post Comments'/>"
    +"        <link rel='self' type='application/atom+xml' href='http://ete-de"
    +"-code-a-la-chou.blogspot.com/feeds/posts/default/1861046915714479007'/>"
    +"        <link rel='edit' type='application/atom+xml' href='http://www.bl"
    +"ogger.com/feeds/3490925879145756058/posts/default/1861046915714479007'/>"
    +""
    +"        <author>"
    +"            <name>Mihai Balan</name>"
    +"        </author>"
    +"    </entry>"
    +"    <entry>"
    +"        <id>tag:blogger.com,1999:blog-3490925879145756058.post-717349866"
    +"079669848</id><published>2007-06-29T12:35:00.000-07:00</published><updat"
    +"ed>2007-06-29T12:39:34.816-07:00</updated><category scheme='http://www.b"
    +"logger.com/atom/ns#' term='bugzilla'/>"
    +"        <category scheme='http://www.blogger.com/atom/ns#' term='todo'/>"
    +""
    +"        <title type='text'>Interim objectives</title><content type='html"
    +"'>I know I've been a little late setting these, but now it's kinda' sett"
    +"led.</content><link rel='alternate' type='text/html' href='http://ete-de"
    +"-code-a-la-chou.blogspot.com/2007/06/interim-objectives.html' title='Int"
    +"erim objectives'/>"
    +"        <link rel='replies' type='text/html' href='http://www.blogger.co"
    +"m/comment.g?blogID=3490925879145756058&amp;postID=717349866079669848' ti"
    +"tle='0 Comments'/>"
    +"        <link rel='replies' type='application/atom+xml' href='http://ete"
    +"-de-code-a-la-chou.blogspot.com/feeds/717349866079669848/comments/defaul"
    +"t' title='Post Comments'/>"
    +"        <link rel='self' type='application/atom+xml' href='http://ete-de"
    +"-code-a-la-chou.blogspot.com/feeds/posts/default/717349866079669848'/>"
    +"        <link rel='edit' type='application/atom+xml' href='http://www.bl"
    +"ogger.com/feeds/3490925879145756058/posts/default/717349866079669848'/>"
    +"        <author>"
    +"            <name>Mihai Balan</name>"
    +"        </author>"
    +"    </entry>";

    /**
     * String containing some invalid markup in the ATOM feed.
     */
    private static String atomBodyInvalid =
        "<some-invalid-tag attr='value1'>Lorem ipsum</some-invalid-tg>";

    /**
     * String containing the footer of the ATOM feed.
     */
    private static String atomFooter = "</feed>";

    /**
     * Returns a <code>String</code> representing an RSS feed.
     * @return textual representation of the RSS feed.
     */
    public static String getRss()
    {
        return rssHeader + rssBody + rssFooter;
    }

    /**
     * Returns a <code>String</code> representing the updated RSS feed.
     * @return textual representation of the RSS feed.
     */
    public static String getRssUpdated()
    {
        return rssHeader + rssBodyUpdate + rssBody + rssFooter;
    }

    /**
     * Returns a <code>String</code> representing a new version of the RSS feed.
     * @return textual representation of the RSS feed.
     */
    public static String getRssNew()
    {
        return rssHeader + rssBodyNew + rssFooter;
    }

    /**
     * Returns a <code>String</code> representing an invalid RSS feed.
     * @return textual representation of the RSS feed.
     */
    public static String getRssInvalid()
    {
        return rssHeader + rssBodyInvalid + rssFooter;
    }

    /**
     * Returns a <code>String</code> representing an ATOM feed.
     * @return textual representation of the RSS feed.
     */
    public static String getAtom()
    {
        return atomHeader + atomBody + atomFooter;
    }

    /**
     * Returns a <code>String</code> representing the updated ATOM feed.
     * @return textual representation of the RSS feed.
     */
    public static String getAtomUpdated()
    {
        return atomHeader + atomBodyUpdate + atomBody + atomFooter;
    }

    /**
     * Returns a <code>String</code> representing a new version of the ATOM
     * feed.
     * @return textual representation of the RSS feed.
     */
    public static String getAtomNew()
    {
        return atomHeader + atomBodyNew + atomFooter;
    }

    /**
     * Returns a <code>String</code> representing an invalid ATOM feed.
     * @return textual representation of the RSS feed.
     */
    public static String getAtomInvalid()
    {
        return atomHeader + atomBodyInvalid + atomFooter;
    }
}
