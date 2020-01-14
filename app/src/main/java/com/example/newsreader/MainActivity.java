package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDB;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,content VARCHAR)");


        DownloadTask downloadTask = new DownloadTask();

        try{
            downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

        listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(i));

                startActivity(intent);
            }
        });
        updateListView();

    }

    public void updateListView(){
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles",null);
        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        if(c.moveToFirst()){
            titles.clear();
            content.clear();

            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            }while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();

        }

    }

    public class DownloadTask extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... urls) {

            String result="";
            URL url;
            HttpURLConnection urlConnection = null;

            try{
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = reader.read();

                while(data!=-1){
                    char ch = (char) data;
                    result+=ch;
                    data=reader.read();

                }

                JSONArray jsonArray = new JSONArray(result);

                int numberOfItem=20;
                if(numberOfItem<20){
                    numberOfItem = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM articles");
                for(int i=0;i<numberOfItem;i++){

                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" +articleId+ ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);

                    String articleInfo = "";

                    data = reader.read();

                    while(data!=-1){
                        char ch = (char) data;
                        articleInfo+=ch;
                        data=reader.read();
                    }

//                    Log.i("Article Info",articleInfo);

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if(!jsonObject.isNull("title")&&!jsonObject.isNull("url")){

                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        urlConnection =(HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);

                        data=reader.read();
                        String articleContent = "";

                        while(data!=-1){
                            char current =(char)data;
                            articleContent+=current;
                            data=reader.read();
                        }

                        Log.i("Article content",articleContent);

                        String sql = "INSERT INTO Articles(articleId,title,content) VALUES(?,?,?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);
                        statement.execute();

                    }

                }

                Log.i("Website Content",result);
                return result;

            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

}
