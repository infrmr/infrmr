package infrmr.newsapp.github.com.ifrmr.article;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import infrmr.newsapp.github.com.ifrmr.R;


public class ArticleFragment extends Fragment {

    // private static final String ARG_SECTION_NUMBER = "section_number";

    TextView title;
    TextView content;
    TextView updated;
    ProgressBar loadingSpinner;

    String titleText;
    String contentText;
    String linkText;
    String updatedText;

    ParseURL parseTask;

    int paragraphToShortLength = 115;


    /**
     * Return new Fragment
     */
    public static ArticleFragment newInstance() {
        return new ArticleFragment();
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number. TODO - Keep this method for possible future use
     *//*
    public static ArticleFragment newInstance(int sectionNumber) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    } */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get article strings from intent
        titleText = getActivity().getIntent().getExtras().getString("title", "No title");
        contentText = getActivity().getIntent().getExtras().getString("content", "No content");
        linkText = getActivity().getIntent().getExtras().getString("link", "No content");
        updatedText = getActivity().getIntent().getExtras().getString("updated", "No content");

        parseTask = new ParseURL();
        parseTask.execute(linkText);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.article_fragment, container, false);

        // Reference views
        title = (TextView) layout.findViewById(R.id.articleLayoutTitle);
        content = (TextView) layout.findViewById(R.id.articleLayoutContent);
        updated = (TextView) layout.findViewById(R.id.articleViewUpdated);
        loadingSpinner = (ProgressBar) layout.findViewById(R.id.loadingSpinner);

        // Set up default view
        title.setText(titleText);
        content.setText(contentText);
        return layout;
    }

    /**
     * Cancel AsyncTask if in progress, as its no longer needed
     */
    @Override
    public void onStop() {
        super.onStop();
        if (parseTask != null) {
            parseTask.cancel(true);
        }

    }

    /**
     * AsyncTask for downloading article
     */
    class ParseURL extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            StringBuilder buffer = new StringBuilder();
            try {
                Log.d(getClass().getSimpleName(), "JSoup - Connecting to [" + strings[0] + "]");
                Document doc = Jsoup.connect(strings[0]).get();

                int i = 0;
                Elements paragraphs = doc.select("p");
                for (Element paragraph : paragraphs) {
                    if (paragraph.text().length() > paragraphToShortLength) {
                        Log.i("JSOUP", "<p> - " + i + " - " + paragraph.text());
                        buffer.append(paragraph.text()).append("\n\n");
                    } else if (paragraph.text().contains("2015 Vox Media, Inc. All rights reserved")) {
                        break;
                    }

                    i++;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return buffer.toString();
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // Hide loading spinner
            loadingSpinner.setVisibility(View.GONE);

            // if returned text isn't empty
            if (s.length() > 0) {

                // Find the last full stop in paragraph
                int endOfParagraph = s.lastIndexOf(".");

                // Shorten string to this length
                if (endOfParagraph > paragraphToShortLength) {
                    s = s.substring(0, endOfParagraph + 1);
                }

                content.setText(s);
                updated.setText(updatedText);
            } else {
                Log.i(getClass().getSimpleName(), "Connection error, please try again");
                Crouton.makeText(getActivity(), "Connection Timeout", Style.ALERT).show();
            }


        }
    }
}
