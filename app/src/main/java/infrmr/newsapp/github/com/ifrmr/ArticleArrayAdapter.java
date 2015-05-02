package infrmr.newsapp.github.com.ifrmr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ArticleArrayAdapter extends BaseAdapter {

    private static LayoutInflater inflater;
    private final Context mContext;
    public static  ArrayList<TheVergeXmlParser.Entry> articles = new ArrayList<TheVergeXmlParser.Entry>();

    public ArticleArrayAdapter (Context mContext) {
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

        if (null == convertView) {
            holder = new ViewHolder();

            convertView = inflater.inflate(R.layout.article_list_item, null);

            holder.title = (TextView) convertView.findViewById(R.id.textViewTitle);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        holder.title.setText(e.title);



        return convertView;
    }


    /**
     * ViewHolder class
     */
    static class ViewHolder {
        public View date;
        //TextView month;
        TextView title;
        //ImageView image;
    }
}