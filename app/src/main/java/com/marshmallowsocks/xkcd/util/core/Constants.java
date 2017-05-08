package com.marshmallowsocks.xkcd.util.core;

/**
 * Created by marshmallowsocks on 5/3/17.
 * Constants file for xkcd viewer.
 */

public final class Constants {
    public final static String APP_NAME = "MSXKCD";
    public final static String URL_PATTERN = "https://xkcd.com/%s/info.0.json";
    public final static String LATEST_URL = "https://xkcd.com/info.0.json";
    public final static String EXPLAIN_URL = "http://www.explainxkcd.com/wiki/index.php/%s";
    public final static String WHAT_IF_LATEST_URL = "https://what-if.xkcd.com";
    public final static String WHAT_IF_URL = "https://what-if.xkcd.com/%s";
    public final static String LOADING_URL = "https://vignette1.wikia.nocookie.net/xkcd-time/images/6/6d/Waiting-intensifies.gif/revision/latest?cb=20150414215843";
    public final static String FAVORITE_KEY = "xkcd_favorite_%s";
    public final static String SEARCH_TO_PAGE_ACTION = "search_to_page_action";
    public final static String SHARED_PREFERENCES_FILE = "com.marshmallowsocks.xkcd";
    public final static String NEW_XKCD = "msxkcd_xkcd_new";
    public final static String NEW_COMIC_ADDED = "New comic added: ";
    public final static String ALL_COMICS = "ALL_COMICS";
    public final static String ALL_WHAT_IF = "ALL_WHAT_IF";
    public final static String LAST = "last";
    public final static String MAX = "max";

    public final static String COMIC_TITLE = "title";
    public final static String COMIC_URL = "img";
    public final static String COMIC_INDEX = "num";
    public final static String COMIC_EXTRA = "alt";
    public final static String COMIC_MONTH = "month";
    public final static String COMIC_DAY = "day";
    public final static String COMIC_YEAR = "year";

    public final static String WHAT_IF_TITLE = "title";
    public final static String WHAT_IF_QUESTION = "question";
    public final static String WHAT_IF_ENTRY = "entry";
    public final static String WHAT_IF_ATTRIBUTE = "attribute";
    public final static String WHAT_IF_ANSWER_BODY_TEXT = "text";
    public final static String WHAT_IF_ANSWER_BODY_LIST = "list";
    public final static String WHAT_IF_ANSWER_BODY_HTML = "html";
    public final static String WHAT_IF_ILLUSTRATION = "illustration";
    public final static String WHAT_IF_BLOCKQUOTE = "blockquote";
    public final static String WHAT_IF_LATEX_IMAGE = "latex_image";

    public final static String PARAGRAPH = "p";
    public final static String IMAGE = "img";
    public final static String ANCHOR = "a";
    public final static String BLOCKQUOTE = "blockquote";
    public final static String UNORDERED_LIST = "ul";
    public final static String SUP_WRAP = "<sup></sup>";
    public final static String ANCHOR_WRAP = "<a href=\"%s\"></a>";

    public final static String CLASS_REF = ".ref";
    public final static String CLASS_REF_NUM = ".refnum";

    public final static String SEARCH_QUERY = "title like ? or num = ?";
}
