package codeminders.yfrog.twitter;

import java.io.*;
import java.util.*;
import net.rim.device.api.util.*;
import net.rim.device.api.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import codeminders.yfrog.utils.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.lib.network.*;
import codeminders.yfrog.lib.utils.*;

public class TwitterManager {

    public static final int STATUSES_PAGE_SIZE = 20;
    public static final int USERS_PAGE_SIZE = 100;
    public static final int MAX_STATUS_TEXT_SIZE = 140;

    private static TwitterManager _instance = null;

    public static synchronized TwitterManager getInstance() {
        if (_instance == null)
            _instance = new TwitterManager();
        return _instance;
    }

    private static final String API_URL = "http://twitter.com/";

    private Authorization _auth = null;

    private TwitterManager() {
    }

    public void setAuthorization(Authorization auth) {
        _auth = auth;
    }

    /*********/
    /* Utils */
    /*********/

    public static String[] getUsersReferences(String text) {
        if (StringUtils.isNullOrEmpty(text))
            return new String[0];
        Vector resList = new Vector();
        String s = text;
        while (s.length() > 0) {
            int sLength = s.length();
            int start = s.indexOf('@');
            if (start < 0)
                break;
            start++;
            if (start >= sLength)
                break;
            int end = start;
            while ((end < sLength) && StringUtils.isNameChar(s.charAt(end)))
                end++;
            if (end > start) {
                String screenName = s.substring(start, end);
                if (!resList.contains(screenName))
                    resList.addElement(screenName);
            }
            s = s.substring(end);
        }
        String[] res = new String[resList.size()];
        resList.copyInto(res);
        return res;
    }

    /************/
    /* Requests */
    /************/

    private static long startRequestCommon(String method, int httpMethod,
        ArgumentsList args, Authorization auth
    ) {
        ContentProcessor processor = new ContentProcessorCommon();
        return startRequest(method + ".xml", httpMethod, args, auth, processor);
    }

    private static String getRequestUrlCommon(String method,
        ArgumentsList args, Authorization auth
    ) {
        return getRequestUrl(method + ".xml", args, auth);
    }

    private static long startRequestOAuth(String method, int httpMethod,
        ArgumentsList args, Authorization auth
    ) {
        ContentProcessor processor = new ContentProcessorFormData();
        return startRequest(method, httpMethod, args, auth, processor);
    }

    private static long startRequest(String method, int httpMethod, ArgumentsList args,
        Authorization auth, ContentProcessor processor
    ) {
        String url = API_URL + method;

        if (args == null)
            args = new ArgumentsList();
        if (auth != null)
            auth.applyToArgs(httpMethod, url, args);

        return NetworkManager.getInstance().startRequest(httpMethod, url,
            (httpMethod == HttpMethod.GET) ? args : null,
            (httpMethod == HttpMethod.POST) ? new ContentXWwwFormUrlEncoded(args) : null,
            auth, processor
        );
    }

    private static String getRequestUrl(String method, ArgumentsList args,
        Authorization auth
    ) {
        String url = API_URL + method;

        if (args == null)
            args = new ArgumentsList();
        if (auth != null)
            auth.applyToArgs(HttpMethod.GET, url, args);

        return NetworkManager.formatRequestUrl(url, args);
    }

    private static class ContentProcessorFormData implements ContentProcessor {
        public Object process(
            NetworkRequest request,
            int responseCode, Hashtable responseHeaders, InputStream responseData
        ) throws Exception {
            String data = StreamUtils.readStreamContentToString(responseData, "UTF-8");
            ArgumentsList args = new ArgumentsList();
            args.parse(data);
            return args;
        }
    }

    /*******/
    /* XML */
    /*******/

    private static class ContentProcessorCommon implements ContentProcessor {
        public Object process(
            NetworkRequest request,
            int responseCode, Hashtable responseHeaders, InputStream responseData
        ) throws Exception {
            DefaultHandlerImpl handler = new DefaultHandlerImpl();
            try { SAXParserFactory.newInstance().newSAXParser().parse(responseData, handler); }
            catch (XmlUtils.SAXStopException ex) { }
            return handler.getResult();
        }
    }

    private static class ItemBaseStub extends ItemBase {
        public ItemBaseStub() { }
    }

    private static class DefaultHandlerImpl extends DefaultHandler {

        private Object result = null;

        private Vector items = new Vector();
        private StringBuffer nodeText = null;

        public DefaultHandlerImpl() {
        }

        public Object getResult() {
            return result;
        }

        private ItemBase getCurrentItem() {
            for (int i = items.size() - 1; i >= 0; i--) {
                ItemBase item = (ItemBase)items.elementAt(i);
                if (!(item instanceof ItemBaseStub))
                    return item;
            }
            return null;
        }

        // DefaultHandler

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            nodeText = null;

            ItemBase item = null;
            if (result == null) {
                if ("user".equals(localName)) item = new UserItem();
                else if ("status".equals(localName)) item = new StatusItem();
                else if ("users".equals(localName)) item = new UsersItem();
                else if ("users_list".equals(localName)) item = new UsersListItem();
                else if ("statuses".equals(localName)) item = new StatusesItem();
                else if ("relationship".equals(localName)) item = new RelationshipItem();
                else if ("direct_message".equals(localName)) item = new DirectMessageItem();
                else if ("direct-messages".equals(localName)) item = new DirectMessagesItem();
                else if ("saved_search".equals(localName)) item = new SavedSearchItem();
                else if ("saved_searches".equals(localName)) item = new SavedSearchesItem();
                // twitter bug
                else if ("nilclasses".equals(localName)) throw new XmlUtils.SAXStopException();
                // unsupported root element
                else throw new SAXException("Unsupported Twitter response: " + localName);
                result = item;
            }
            else
                item = getCurrentItem().newChildren(localName);
            if (item == null)
                item = new ItemBaseStub();
            items.addElement(item);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ((nodeText != null) && (nodeText.length() > 0))
                try { getCurrentItem().setField(localName, nodeText.toString()); } catch (Exception ignored) {}
            nodeText = null;

            items.removeElementAt(items.size() - 1);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (nodeText == null)
                nodeText = new StringBuffer();
            nodeText.append(ch, start, length);
        }
    }

    /*********/
    /* OAuth */
    /*********/

    public String getAuthorizationUrl(String token) {
        return API_URL + "oauth/authorize?oauth_token=" + ArgumentsList.encodeURL(token);
    }

    /*************/
    /* API Calls */
    /*************/

    ///////////////
    // Search
    ///////////////

    private static class ContentProcessorSearch implements ContentProcessor {
        public Object process(
            NetworkRequest request,
            int responseCode, Hashtable responseHeaders, InputStream responseData
        ) throws Exception {
            Object res = JsonParser.parse(responseData);
            if ((res == null) || (!(res instanceof Hashtable)))
                return null;
            SearchResultsItem results = new SearchResultsItem();
            results.setFields((Hashtable)res);
            return results;
        }
    }

    // returns SearchResultsItem
    public long search(String query) {
        return search(query, -1, -1);
    }

    // returns SearchResultsItem
    public long search(String query, int page, long maxID) {
        ArgumentsList args = new ArgumentsList();
        args.set("q", query);
        args.set("rpp", Integer.toString(STATUSES_PAGE_SIZE));
        if (page > 0)
            args.set("page", Integer.toString(page));
        if (maxID > 0)
            args.set("max_id", Long.toString(maxID));
        ContentProcessor processor = new ContentProcessorSearch();
        return startRequest("search.json", HttpMethod.GET, args, null, processor);
    }

    ///////////////
    // Timeline
    ///////////////

    // returns statuses
    public long statusesPublicTimeline() {
        return startRequestCommon("statuses/public_timeline", HttpMethod.GET, null, null);
    }

    // returns statuses
    public long statusesFriendsTimeline() {
        return statusesFriendsTimeline(-1);
    }

    // returns statuses
    public long statusesFriendsTimeline(long maxID) {
        ArgumentsList args = new ArgumentsList();
        if (maxID > 0)
            args.set("max_id", Long.toString(maxID));
        args.set("count", Integer.toString(STATUSES_PAGE_SIZE));
        return startRequestCommon("statuses/friends_timeline", HttpMethod.GET, args, _auth);
    }

    // returns statuses
    public long statusesUserTimeline(String screenName) {
        return statusesUserTimeline(screenName, -1);
    }

    // returns statuses
    public long statusesUserTimeline(String screenName, long maxID) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        if (maxID > 0)
            args.set("max_id", Long.toString(maxID));
        args.set("count", Integer.toString(STATUSES_PAGE_SIZE));
        return startRequestCommon("statuses/user_timeline", HttpMethod.GET, args, _auth);
    }

    // returns statuses
    public long statusesMentions() {
        return statusesMentions(-1);
    }

    // returns statuses
    public long statusesMentions(long maxID) {
        ArgumentsList args = new ArgumentsList();
        if (maxID > 0)
            args.set("max_id", Long.toString(maxID));
        args.set("count", Integer.toString(STATUSES_PAGE_SIZE));
        return startRequestCommon("statuses/mentions", HttpMethod.GET, args, _auth);
    }

    ///////////////
    // Status
    ///////////////

    // returns status
    public long statusesUpdate(String status, Double latitude, Double longitude) {
        return statusesUpdate(status, latitude, longitude, -1);
    }

    // returns status
    public long statusesUpdate(String status, Double latitude, Double longitude, long inReplyToStatusID) {
        ArgumentsList args = new ArgumentsList();
        args.set("status", status);
        if ((latitude != null) && (longitude != null)) {
            args.set("lat", StringUtils.formatDouble(latitude.doubleValue(), 10, 0));
            args.set("long", StringUtils.formatDouble(longitude.doubleValue(), 10, 0));
        }
        if (inReplyToStatusID > 0)
            args.set("in_reply_to_status_id", Long.toString(inReplyToStatusID));
        return startRequestCommon("statuses/update", HttpMethod.POST, args, _auth);
    }

    // returns status
    public long statusesDestroy(long id) {
        ArgumentsList args = new ArgumentsList();
        args.set("id", Long.toString(id));
        return startRequestCommon("statuses/destroy", HttpMethod.POST, args, _auth);
    }

    // returns status
    public long statusesShow(long id) {
        ArgumentsList args = new ArgumentsList();
        args.set("id", Long.toString(id));
        return startRequestCommon("statuses/show", HttpMethod.GET, args, _auth);
    }

    ///////////////
    // User
    ///////////////

    // returns user
    public long usersShow(String screenName) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        return startRequestCommon("users/show", HttpMethod.GET, args, _auth);
    }

    // returns users_list
    public long statusesFriends(String screenName) {
        return statusesFriends(screenName, -1);
    }

    // returns users_list
    public long statusesFriends(String screenName, long cursor) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        args.set("cursor", Long.toString(cursor));
        return startRequestCommon("statuses/friends", HttpMethod.GET, args, _auth);
    }

    // returns users_list
    public long statusesFollowers(String screenName) {
        return statusesFollowers(screenName, -1);
    }

    // returns users_list
    public long statusesFollowers(String screenName, long cursor) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        args.set("cursor", Long.toString(cursor));
        return startRequestCommon("statuses/followers", HttpMethod.GET, args, _auth);
    }

    ///////////////
    // Direct Message
    ///////////////

    // returns direct-messages
    public long directMessages() {
        return directMessages(-1);
    }

    // returns direct-messages
    public long directMessages(long maxID) {
        ArgumentsList args = new ArgumentsList();
        if (maxID > 0)
            args.set("max_id", Long.toString(maxID));
        args.set("count", Integer.toString(STATUSES_PAGE_SIZE));
        return startRequestCommon("direct_messages", HttpMethod.GET, args, _auth);
    }

    // returns direct-messages
    public long directMessagesSent() {
        return directMessagesSent(-1);
    }

    // returns direct-messages
    public long directMessagesSent(long maxID) {
        ArgumentsList args = new ArgumentsList();
        if (maxID > 0)
            args.set("max_id", Long.toString(maxID));
        args.set("count", Integer.toString(STATUSES_PAGE_SIZE));
        return startRequestCommon("direct_messages/sent", HttpMethod.GET, args, _auth);
    }

    // returns direct_message
    public long directMessagesNew(String screenName, String text) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        args.set("text", text);
        return startRequestCommon("direct_messages/new", HttpMethod.POST, args, _auth);
    }

    // returns direct_message
    public long directMessagesDestroy(long id) {
        ArgumentsList args = new ArgumentsList();
        args.set("id", Long.toString(id));
        return startRequestCommon("direct_messages/destroy", HttpMethod.POST, args, _auth);
    }

    ///////////////
    // Friendship
    ///////////////

    // returns user
    public long friendshipsCreate(String screenName, boolean follow) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        if (follow)
            args.set("follow", "true");
        return startRequestCommon("friendships/create", HttpMethod.POST, args, _auth);
    }

    // returns user
    public long friendshipsDestroy(String screenName) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        return startRequestCommon("friendships/destroy", HttpMethod.POST, args, _auth);
    }

    // returns relationship
    public long friendshipsShow(String sourceScreenName, String targetScreenName) {
        ArgumentsList args = new ArgumentsList();
        args.set("source_screen_name", sourceScreenName);
        args.set("target_screen_name", targetScreenName);
        return startRequestCommon("friendships/show", HttpMethod.GET, args, _auth);
    }

    ///////////////
    // Account
    ///////////////

    // returns user
    public long accountVerifyCredentials(Authorization auth) {
        return startRequestCommon("account/verify_credentials", HttpMethod.GET, null, auth);
    }

    // returns user
    public String getUrlAccountVerifyCredentials() {
        return getRequestUrlCommon("account/verify_credentials", null, _auth);
    }

    // returns user
    public long accountUpdateProfile(String name, String url, String location, String description) {
        ArgumentsList args = new ArgumentsList();
        if (name != null)
            args.set("name", name);
        if (url != null)
            args.set("url", url);
        if (location != null)
            args.set("location", location);
        if (description != null)
            args.set("description", description);
        return startRequestCommon("account/update_profile", HttpMethod.POST, args, _auth);
    }

    ///////////////
    // Notification
    ///////////////

    // returns user
    public long notificationsFollow(String screenName) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        return startRequestCommon("notifications/follow", HttpMethod.POST, args, _auth);
    }

    // returns user
    public long notificationsLeave(String screenName) {
        ArgumentsList args = new ArgumentsList();
        args.set("screen_name", screenName);
        return startRequestCommon("notifications/leave", HttpMethod.POST, args, _auth);
    }

    ///////////////
    // Saved Searches
    ///////////////

    // returns saved_searches
    public long savedSearches() {
        return startRequestCommon("saved_searches", HttpMethod.GET, null, _auth);
    }

    // returns saved_search
    public long savedSearchesShow(long id) {
        ArgumentsList args = new ArgumentsList();
        args.set("id", Long.toString(id));
        return startRequestCommon("saved_searches/show", HttpMethod.GET, args, _auth);
    }

    // returns saved_search
    public long savedSearchesCreate(String query) {
        ArgumentsList args = new ArgumentsList();
        args.set("query", query);
        return startRequestCommon("saved_searches/create", HttpMethod.POST, args, _auth);
    }

    // returns saved_search
    public long savedSearchesDestroy(long id) {
        return startRequestCommon("saved_searches/destroy/" + id, HttpMethod.POST, null, _auth);
    }

    ///////////////
    // OAuth
    ///////////////

    // returns ArgumentsList
    public long oauthRequestToken(String consumerKey, String consumerSecret, String callback) {
        Authorization auth = new OAuthAuthorization(consumerKey, consumerSecret);

        ArgumentsList args = new ArgumentsList();
        if (!StringUtils.isNullOrEmpty(callback))
            args.set("oauth_callback", callback);

        return startRequestOAuth("oauth/request_token", HttpMethod.GET, args, auth);
    }

    // returns ArgumentsList
    public long oauthAccessToken(String consumerKey, String consumerSecret,
        String requestToken, String verifier
    ) {
        Authorization auth = new OAuthAuthorization(consumerKey, consumerSecret);

        ArgumentsList args = new ArgumentsList();
        args.set("oauth_token", requestToken);
        args.set("oauth_verifier", verifier);

        return startRequestOAuth("oauth/access_token", HttpMethod.POST, args, auth);
    }
}

