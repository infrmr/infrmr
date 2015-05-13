package infrmr.newsapp.github.com.ifrmr;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import infrmr.newsapp.github.com.ifrmr.article.ArticleActivity;

/**
 * The custom adapter for placing article data into cardviews
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    // The Array which will hold the Article objects
    public static ArrayList<TheVergeXmlParser.Entry> articles = new ArrayList<>();

    // For easy access to Activity context
    private static Context mContext;

    /**
     * Initialize method
     */
    public RecyclerAdapter(Context context) {
        // Save context reference for future
        mContext = context;
    }

    // Initialize View Holder references
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate card layout
        LinearLayout cardLayout = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.article_card_list_item, parent, false);

        // Set textView References
        TextView title = (TextView) cardLayout.findViewById(R.id.textViewTitle);
        TextView content = (TextView) cardLayout.findViewById(R.id.textViewContent);
        TextView updated = (TextView) cardLayout.findViewById(R.id.textViewUpdated);

        // Return new ViewHolder with references
        return new ViewHolder(cardLayout, title, content, updated);
    }

    // Replace the contents of a view with new data from Article
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Get the current article
        TheVergeXmlParser.Entry e = articles.get(position);
        // Update recycled view with new information
        holder.title.setText(e.title);
        holder.content.setText(formatContentFromHtml(e));
        holder.updated.setText(formatTime(e.updated));
    }

    // Return the size of Array
    @Override
    public int getItemCount() {
        return articles.size();
    }

    /**
     * Format content html to a single paragraph.
     */
    private String formatContentFromHtml(TheVergeXmlParser.Entry entry) {

        String content = entry.content;

        // Create new StingBuilder object
        StringBuilder stringBuilder = new StringBuilder();

        // Locations of paragraph start, end
        int paragraphStart = content.indexOf("<p");
        int paragraphEnd = content.indexOf("</p");

        // int which ensures the first tag is ignored while searching for <p> tags
        int ignoreFirstParagraph = 5;

        // Article is too small if less than 150 character's
        int shortArticleLength = 150;

        // Shorten to single paragraph...
        String news = content.substring(paragraphStart, (paragraphEnd));

        // ... if the paragraph is too short, and a second paragraph exists...
        if ((news.length() < shortArticleLength) && content.substring(paragraphEnd + ignoreFirstParagraph).contains("</p")) {
            // ... create new string including second paragraph
            news = content.substring(paragraphStart, content.indexOf("</p", paragraphEnd + ignoreFirstParagraph));
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
        if (endOfParagraph > shortArticleLength) {
            stringBuilder.setLength((endOfParagraph + 1));
        }

        entry.formattedContent = stringBuilder.toString();

        // Return newly built paragraph
        return stringBuilder.toString();
    }

    /**
     * Helper method which builds the time/date string
     */
    public String formatTime(String updated) {
        int separator = updated.indexOf("T");
        int timeDivider = updated.lastIndexOf("-");

        String date = updated.substring(0, separator);
        String time = updated.substring(separator + 1, timeDivider);
        String timeDiff = updated.substring(timeDivider);

        return time.substring(0, 5) + "  -  " + date;
    }


    /**
     * View Holder Class for recycling views
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // References
        public LinearLayout mRelLayout;
        TextView title;
        TextView content;
        TextView updated;

        public ViewHolder(LinearLayout cardLayout, TextView tvTitle, TextView tvContent, TextView tvUpdated) {
            super(cardLayout);
            // Setup card click listener
            cardLayout.setOnClickListener(this);

            // Initialise references
            mRelLayout = cardLayout;
            title = tvTitle;
            content = tvContent;
            updated = tvUpdated;
        }

        @Override
        public void onClick(View view) {
            // Get Article ready for new Fragment
            TheVergeXmlParser.Entry e = articles.get(getPosition());

            Intent i = new Intent(mContext, ArticleActivity.class);
            i.putExtra("title", e.title);
            i.putExtra("content", e.formattedContent);
            i.putExtra("link", e.link);
            i.putExtra("updated", e.updated);
            mContext.startActivity(i);

        }
    }

}
