package infrmr.newsapp.github.com.ifrmr;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ArticleArrayAdapter extends BaseAdapter {

    public static ArrayList<TheVergeXmlParser.Entry> articles = new ArrayList<TheVergeXmlParser.Entry>();
    private static LayoutInflater inflater;
    private final Context mContext;

    public ArticleArrayAdapter(Context mContext) {
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext.getApplicationContext());

    }

    @Override
    public int getCount() {
        return articles.size();
    }

    @Override
    public TheVergeXmlParser.Entry getItem(int i) {
        return articles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        final TheVergeXmlParser.Entry e = articles.get(position);

        // Init view if null
        if (null == convertView) {
            // Init view holder
            holder = new ViewHolder();

            // Inflate view
            convertView = inflater.inflate(R.layout.article_card_list_item, null);

            // Reference TextViews
            holder.title = (TextView) convertView.findViewById(R.id.textViewTitle);
            holder.content = (TextView) convertView.findViewById(R.id.textViewContent);
            holder.cardLayout = (RelativeLayout) convertView.findViewById(R.id.relativeCardLayout);

            // Set tag
            convertView.setTag(holder);

        } else {
            // Recycle view
            holder = (ViewHolder) convertView.getTag();
        }

        // Update view's widgets
        holder.title.setText(e.title);
        holder.content.setText(formatContentFromHtml(e.content));
        holder.cardLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Crouton.makeText((Activity) mContext,
                        "onClick: " + e.title.substring(0, 25), Style.INFO).show();
            }
        });

        return convertView;
    }

    private String formatContentFromHtml(String content) {

        // Find the start of the first paragraph, make a substring from its location
        String cleanHtml = content.substring(content.indexOf("<p"));

        // Uses Html class and TagSoup to extract text from Html
        return Html.fromHtml(cleanHtml).toString();
    }

    /**
     * ViewHolder class
     */
    static class ViewHolder {
        TextView title;
        TextView content;
        RelativeLayout cardLayout;
    }
}