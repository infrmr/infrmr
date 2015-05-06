package infrmr.newsapp.github.com.ifrmr.adapters;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import infrmr.newsapp.github.com.ifrmr.R;
import infrmr.newsapp.github.com.ifrmr.TheVergeXmlParser;

public class ArticleArrayAdapter extends BaseAdapter {

    public static ArrayList<TheVergeXmlParser.Entry> articles = new ArrayList<TheVergeXmlParser.Entry>();
    private static LayoutInflater inflater;
    private final Context mContext;

    public ArticleArrayAdapter(Context mContext) {
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);

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
            convertView = inflater.inflate(R.layout.article_list_item, null); // todo - Why does this crash with 'parent' ?

            // Reference TextViews
            holder.title = (TextView) convertView.findViewById(R.id.textViewTitle);
            holder.content = (TextView) convertView.findViewById(R.id.textViewContent);

            // Set tag
            convertView.setTag(holder);

        } else {
            // Recycle view
            holder = (ViewHolder) convertView.getTag();
        }

        // Update view's widgets
        holder.title.setText(e.title);
        holder.content.setText(formatContentFromHtml(e.content));

        return convertView;
    }

    private String formatContentFromHtml(String content) {
        // Uses Html class and TagSoup to extract text from Html

        Log.i("html", "SHOW: " + Html.fromHtml(content).toString());
        return Html.fromHtml(content).toString();
    }

    /**
     * ViewHolder class
     */
    static class ViewHolder {
        TextView title;
        TextView content;
    }
}