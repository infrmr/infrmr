package infrmr.newsapp.github.com.ifrmr;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    public static ArrayList<TheVergeXmlParser.Entry> articles = new ArrayList<>();

    private static Context mContext;

    public RecyclerAdapter(Context c) {
        mContext = c;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public LinearLayout mRelLayout;
        TextView title;
        TextView content;
        TextView updated;

        public ViewHolder(LinearLayout v, TextView t, TextView c, TextView u) {
            super(v);
            mRelLayout = v;
            title = t;
            content = c;
            updated = u;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            TheVergeXmlParser.Entry e = articles.get(getPosition());
            Crouton.makeText((MainActivity) mContext, "onClick: " + e.title, Style.INFO).show();

        }
    }


    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
        // create a new view
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.article_card_list_item, parent, false);



        // set the view's size, margins, padding and layout parameters
        TextView title = (TextView) v.findViewById(R.id.textViewTitle);
        TextView content = (TextView) v.findViewById(R.id.textViewContent);
        TextView updated = (TextView) v.findViewById(R.id.textViewUpdated);
        return new ViewHolder(v, title, content, updated);
    }

    // Replace the contents of a view with new data from Article
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TheVergeXmlParser.Entry e = articles.get(position);
        holder.title.setText(e.title);
        holder.content.setText(formatContentFromHtml(e.content));
        holder.updated.setText(e.updated);

    }

    // Return the size of Array
    @Override
    public int getItemCount() {
        return articles.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    /**
     * Format content html to a single paragraph.
     */
    private String formatContentFromHtml(String content) {

        // Create new StingBuilder object
        StringBuilder stringBuilder = new StringBuilder();

        int p1 = content.indexOf("<p");
        int p2 = content.indexOf("</p");

        // Shorten to single paragraph...
        String news = content.substring(p1, (p2));

        // ... if the paragraph is too short, and a second paragraph exists...
        if ((news.length() < 150) && content.substring(p2 + 5).contains("</p")) {
            // ... create new string including second paragraph
            news = content.substring(p1, content.indexOf("</p", p2 + 5));
        }

        // Catch embedded tags inside paragraph
        if (news.contains("<img") || news.contains("<div")) {
            // Create temporary string to modify
            String newNews = news;
            if (newNews.contains("<img")) {
                newNews = news.substring(0, news.indexOf("<img"));
            }
            if (newNews.contains("<div")) {
                newNews = news.substring(0, news.indexOf("<div"));
            }
            news = newNews;
        }

        // Format paragraph string to plain text (null values might be replaced with image and tag listeners)
        stringBuilder.append(Html.fromHtml(news, null, null));

        // Find the last full stop in paragraph
        int endOfParagraph = stringBuilder.toString().lastIndexOf(".");

        // Shorten string to this length
        if (endOfParagraph > 100) {
            stringBuilder.setLength((endOfParagraph + 1));
        }

        // Return newly built paragraph
        return stringBuilder.toString();
    }
}
