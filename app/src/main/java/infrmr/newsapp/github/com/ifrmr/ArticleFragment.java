package infrmr.newsapp.github.com.ifrmr;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class ArticleFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    public TheVergeXmlParser.Entry mArticle;

    TextView title;
    TextView content;


    /**
     * Return new Fragment
     */
    public static ArticleFragment newInstance() {
        return new ArticleFragment();
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ArticleFragment newInstance(int sectionNumber) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.article_fragment, container, false);

        getActivity().getSupportFragmentManager().findFragmentById(R.id.container);

        title = (TextView) layout.findViewById(R.id.articleLayoutTitle);
        content = (TextView) layout.findViewById(R.id.articleLayoutContent);

        // Get article strings from intent
        // String title = getActivity().getIntent().getExtras().getString("title", "No title");
        // String content = getActivity().getIntent().getExtras().getString("content", "No content");

        // Reference WebView
        //WebView mWebView = (WebView) layout.findViewById(R.id.webView);
        // Option to resize large images
        //mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        // Add the content to the view
        //mWebView.loadData(getHtmlContent(title, content), "text/html", n"ull);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mArticle == null) {
                    Log.i("WHILE", "WAITING");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                (new ParseURL()).execute(mArticle.link);
            }
        }).start();


    }


    public void sendData(TheVergeXmlParser.Entry article) {

        mArticle = article;

        updateArticleInfo(article);
    }

    private void updateArticleInfo(TheVergeXmlParser.Entry article) {
        title.setText(article.title);


    }

    /**
     * AsyncTask for downloading article
     */
    class ParseURL extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            StringBuilder buffer = new StringBuilder();
            try {
                Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                Document doc = Jsoup.connect(strings[0]).get();
                //Log.d("JSwa", "Connected to [" + strings[0] + "]");
                // Get document (HTML page) title
                String title = doc.title();
                Log.d("JSwA", "Title [" + title + "]");
                buffer.append("Title: " + title + "\r\n");


                Elements paragraphs = doc.select("p");
                for (Element p : paragraphs) {
                    Log.i("JSOUP", "<p> :: " + p.text());
                    buffer.append("\n" + p.text());
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
            return buffer.toString();
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("OPE", "JSOUP FINAL STRINGS: " + s);
            content.setText(s);
        }
    }
}
