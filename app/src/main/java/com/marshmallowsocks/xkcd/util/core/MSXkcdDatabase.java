package com.marshmallowsocks.xkcd.util.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.marshmallowsocks.xkcd.util.xkcd.XKCDComicBean;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.List;

public class MSXkcdDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "marshmallowsocks_xkcd.db";
    private static final String TABLE_NAME = "comics";
    private static final int DATABASE_VERSION = 1;

    public MSXkcdDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        // you can use an alternate constructor to specify a database location
        // (such as a folder on the sd card)
        // you must ensure that this folder is available and you have permission
        // to write to it
        //super(context, DATABASE_NAME, context.getExternalFilesDir(null).getAbsolutePath(), null, DATABASE_VERSION);

    }

    public List<XKCDComicBean> searchComic(String queryString) {

        SQLiteDatabase db = getReadableDatabase();
        //SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        List<XKCDComicBean> result = new ArrayList<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String [] sqlSelect = {"num", Constants.COMIC_URL, Constants.COMIC_TITLE};
        String [] whereArgs = {"%" + queryString + "%", queryString };

        qb.setTables(TABLE_NAME);
        Cursor c = qb.query(db, sqlSelect, Constants.SEARCH_QUERY, whereArgs, null, null, null);

        c.moveToFirst();
        if(c.getCount() != 0) {
            do {
                XKCDComicBean resultRow = new XKCDComicBean();
                resultRow.setNumber(Integer.parseInt(c.getString(c.getColumnIndex("num"))));
                resultRow.setTitle(c.getString(c.getColumnIndex(Constants.COMIC_TITLE)).toUpperCase());
                resultRow.setImageUrl(c.getString(c.getColumnIndex(Constants.COMIC_URL)));
                result.add(resultRow);
            } while (c.moveToNext());
        }
        return result;
    }

    public boolean addNewMetadata(XKCDComicBean comicData) {

        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues newValues = new ContentValues();

            newValues.put("num", comicData.getNumber());
            newValues.put(Constants.COMIC_URL, comicData.getImageUrl());
            newValues.put("alt", comicData.getAltText());
            newValues.put(Constants.COMIC_TITLE, comicData.getTitle());
            newValues.put("year", comicData.getDate().split("-")[2]);
            newValues.put("day", comicData.getDate().split("-")[1]);
            newValues.put("month", comicData.getDate().split("-")[0]);

            db.insert(DATABASE_NAME, null, newValues);
            db.close();

        }
        catch(SQLException e) {
            return false;
        }
        return true;
    }

    public boolean contains(Integer num) {
        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String [] sqlSelect = {"num"};
        String whereClause = "num = ?";
        String [] whereArgs = { num.toString() };

        qb.setTables(TABLE_NAME);
        Cursor c = qb.query(db, sqlSelect, whereClause, whereArgs, null, null, null);

        c.moveToFirst();
        return c.getCount() != 0;
    }

    public XKCDComicBean getComic(Integer index) {
        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        XKCDComicBean result = new XKCDComicBean();

        String [] sqlSelect = {Constants.COMIC_INDEX, Constants.COMIC_TITLE, Constants.COMIC_URL, "alt", "day", "month", "year"};
        String whereClause = "num = ?";
        String [] whereArgs = { index.toString() };

        qb.setTables(TABLE_NAME);
        Cursor c = qb.query(db, sqlSelect, whereClause, whereArgs, null, null, null);

        c.moveToFirst();
        if(c.getCount() != 0) {
            do {
                String date;
                result.setNumber(Integer.parseInt(c.getString(c.getColumnIndex(Constants.COMIC_INDEX))));
                result.setTitle(c.getString(c.getColumnIndex(Constants.COMIC_TITLE)).toUpperCase());
                result.setImageUrl(c.getString(c.getColumnIndex(Constants.COMIC_URL)));
                result.setAltText(c.getString(c.getColumnIndex(Constants.COMIC_EXTRA)).toUpperCase());

                date = c.getString(c.getColumnIndex(Constants.COMIC_MONTH));
                date += "-" + c.getString(c.getColumnIndex(Constants.COMIC_DAY));
                date += "-" + c.getString(c.getColumnIndex(Constants.COMIC_YEAR));

                result.setDate(date);

            } while (c.moveToNext());
        }
        return result;
    }

}