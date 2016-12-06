package com.sam_chordas.android.stockhawk;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by jmd on 12/5/2016.
 */

public class StockWidgetService extends RemoteViewsService {
    Context mContext;
    Cursor mCursor;
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockViewFactory(getApplicationContext());
    }

    public class StockViewFactory implements RemoteViewsService.RemoteViewsFactory {
        public StockViewFactory(Context context) {
            mContext = context;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onCreate() {
            initData();
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }
        @Override
        public void onDataSetChanged() {
            initData();
        }

        public void initData()
        {
            mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                    QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
        }
        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews view = null;
            view = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
            mCursor.moveToPosition(position);

            String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
            String price  = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
            String change = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));

            view.setTextViewText(R.id.stock_symbol, symbol);
            view.setTextViewText(R.id.bid_price, price);
            view.setTextViewText(R.id.change, change);

            return view;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }
    }
}
