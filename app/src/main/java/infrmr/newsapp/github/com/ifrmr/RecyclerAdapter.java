package infrmr.newsapp.github.com.ifrmr;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    public static ArrayList<TheVergeXmlParser.Entry> articles = new ArrayList<>();

    private static LayoutInflater inflater;
    private static Context mContext;

    public RecyclerAdapter(Context c) {
        mContext = c;
        inflater = LayoutInflater.from(mContext.getApplicationContext());
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
        TextView updated = (TextView) v.findViewById(R.id.textViewDate); // todo - rename to updated
        ViewHolder mViewHolder = new ViewHolder(v, title, content, updated);
        return mViewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from entry at this position
        // - replace the contents of the view with that element
        TheVergeXmlParser.Entry e = articles.get(position);
        holder.title.setText(e.title);
        holder.content.setText(formatContentFromHtml(e.content));
        holder.updated.setText(e.updated);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return articles.size();
    }


    public TheVergeXmlParser.Entry getItem(int i) {
        return articles.get(i);
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

        // Shorten to single paragraph...
        String news = content.substring(content.indexOf("<p"), (content.indexOf("</p")));

        // ... if the paragraph is too short, and a second paragraph exists...
        if ((news.length() < 150) && content.substring(content.indexOf("</p") + 5).contains("</p")) {
            // ... create new substring with second paragraph
            news = content.substring(content.indexOf("<p"), content.indexOf("</p", content.indexOf("</p") + 5));
        }

        // Catch embedded tags inside paragraph
        if (news.contains("<img") || news.contains("<div")) {

            String newNews = news;
            if (newNews.contains("<img")) {
                newNews = news.substring(0, news.indexOf("<img"));
            }
            if (newNews.contains("<div")) {
                newNews = news.substring(0, news.indexOf("<div"));
            }
            news = newNews;

        }

        stringBuilder.append(Html.fromHtml(news, null, null));

        // Find the last full stop in paragraph, and shorten string to this length
        int endOfParagraph = stringBuilder.toString().lastIndexOf(".");

        if (endOfParagraph > 0) {
            stringBuilder.setLength((endOfParagraph + 1));
        }

        return stringBuilder.toString();
    }
}
