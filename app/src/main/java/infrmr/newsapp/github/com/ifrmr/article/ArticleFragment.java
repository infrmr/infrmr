package infrmr.newsapp.github.com.ifrmr.article;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import infrmr.newsapp.github.com.ifrmr.R;


public class ArticleFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.article_fragment, container, false);

        // Get article strings from intent
        String title = getActivity().getIntent().getExtras().getString("title", "No title");
        String content = getActivity().getIntent().getExtras().getString("content", "No content");

        // Reference WebView
        WebView mWebView = (WebView) layout.findViewById(R.id.webView);
        // Option to resize large images
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        // Add the content to the view
        mWebView.loadData(getHtmlContent(title, content), "text/html", null);

        return layout;
    }

    /**
     * Method for creating the article html code
     *
     * @param title - title of article
     * @param content - article content
     * @return - The completed string to be passed to WebView
     */
    private String getHtmlContent(String title, String content) {

        // Create StringBuilder object, used to build html string
        StringBuilder htmlString = new StringBuilder();
        // Used to get the updated time
        Calendar rightNow = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");

        // Append string with title and article content
        htmlString.append("<h3>" + title + "</h3>");
        htmlString.append("<em>" + getResources().getString(R.string.updated) + " " +
                formatter.format(rightNow.getTime()) + "</em>");
        htmlString.append("<p>" + content + "</p><br><br>");

        return htmlString.toString();
    }


}
