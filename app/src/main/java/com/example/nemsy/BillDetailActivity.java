package com.example.nemsy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class BillDetailActivity extends AppCompatActivity {
    private ImageButton back_button, send_button, like_button, like_button2, dislike_button;
    private TextView bill_name, propose, all_propose, age, propose_date, status, bill_content, likeNum, dislikeNum;
    private EditText comment;
    private ScrollView scrollView;
    String billId;
    BillCommentAdapter adapter;
    private String responseString;
    private int likeCount, dislikeCount;
    private String isLikeClicked;
    private String isDisLikeClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);

        like_button = (ImageButton) findViewById(R.id.like_button);
        like_button2 = (ImageButton) findViewById(R.id.like_button2);
        likeNum = (TextView) findViewById(R.id.like_num);
        dislikeNum = (TextView) findViewById(R.id.dislike_num);
        dislike_button = (ImageButton) findViewById(R.id.dislike_button);
        back_button = (ImageButton) findViewById(R.id.back_button);
        bill_name = (TextView) findViewById(R.id.bill_name);
        propose = (TextView) findViewById(R.id.propose);
        all_propose = (TextView) findViewById(R.id.all_propose);
        age = (TextView) findViewById(R.id.age);
        propose_date = (TextView) findViewById(R.id.propose_date);
        status = (TextView) findViewById(R.id.status);
        bill_content = (TextView) findViewById(R.id.bill_content);
        comment = (EditText) findViewById(R.id.comment);
        send_button = (ImageButton) findViewById(R.id.send_button);
        RecyclerView recyclerView = findViewById(R.id.comments_recyclerView);
        scrollView = findViewById(R.id.scrollView);

        Intent inIntent = getIntent();
        billId = inIntent.getStringExtra("BILL_ID");
        propose.setText(inIntent.getStringExtra("RST_PROPOSER"));
        all_propose.setText(inIntent.getStringExtra("PUBL_PROPOSER"));
        age.setText(inIntent.getStringExtra("AGE"));
        propose_date.setText(inIntent.getStringExtra("PROPOSE_DT"));
        status.setText(inIntent.getStringExtra("PROC_RESULT"));
        if(inIntent.getStringExtra("PROC_RESULT") == null) {
            status.setText("접수");
        }

        // 서버 통신
        if(AppHelper.requestQueue == null) {
            AppHelper.requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        getRequest();


        // 액션바 제거
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        // 웹 크롤링
        String detailLink = inIntent.getStringExtra("DETAIL_LINK");
        final Bundle bundle = new Bundle();
        new Thread() {
            @Override
            public void run() {
                Document doc = null;
                try {
                    doc = Jsoup.connect(detailLink).get();
                    Elements title = doc.select(".titCont");
                    int idxNo = title.text().indexOf("]");
                    int idxproposer = title.text().indexOf("(");
                    String billName = title.text().substring(idxNo+1, idxproposer);
                    bundle.putString("title", billName);

                    Elements content = doc.select("#summaryContentDiv");
                    String contents = content.html().replace("<br> ", "");
                    bundle.putString("content", contents);

                    Message msg = handler.obtainMessage();
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // recyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new BillCommentAdapter();
        recyclerView.setAdapter(adapter);

        new Thread(() -> {
            getLike();
            getDisLike();
            getBill();
        }).start();

        like_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("postLike:", "isLikeClicked" + isLikeClicked);
                if ((isLikeClicked.equals("false")) && (isDisLikeClicked.equals("false"))) {
                    like_button.setSelected(true);
                    new Thread(() -> {
                        postLike();
                        isLikeClicked="true";
                        getBill();

                    }).start();
                } else if((isLikeClicked.equals("false")) && (isDisLikeClicked.equals("true"))){
                    like_button.setSelected(true);
                    dislike_button.setSelected(false);
                    new Thread(() -> {
                        postLike();
                        deleteDisLike();
                        isLikeClicked="true";
                        isDisLikeClicked="false";
                        getBill();

                    }).start();
                } else{
                    like_button.setSelected(false);
                    new Thread(() -> {
                        deleteLike();
                        isLikeClicked="false";
                        getBill();
                    }).start();
                }
            }
        });

        dislike_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("postDisLike:", "isDisLikeClicked" + isDisLikeClicked);
                if ((isDisLikeClicked.equals("false"))&&(isLikeClicked.equals("false"))){
                    dislike_button.setSelected(true);
                    new Thread(() -> {
                        postDisLike();
                        isDisLikeClicked="true";
                        getBill();
                    }).start();
                } else if((isDisLikeClicked.equals("false"))&&(isLikeClicked.equals("true"))){
                    dislike_button.setSelected(true);
                    like_button.setSelected(false);
                    new Thread(() -> {
                        postDisLike();
                        deleteLike();
                        isDisLikeClicked="true";
                        isLikeClicked="false";
                        getBill();
                    }).start();
                }else{
                    dislike_button.setSelected(false);
                    new Thread(() -> {
                        deleteDisLike();
                        isDisLikeClicked="false";
                        getBill();
                    }).start();
                }
            }
        });

        // 뒤로가기 버튼
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 댓글 전송 버튼
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(() -> {
                    postRequest(comment.getText().toString());
                    comment.getText().clear();
                }).start();
            }
        });

    }

    // 크롤링한 데이터 set
    Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle bundle = msg.getData();
            bill_name.setText(bundle.getString("title"));
            bill_content.setText(bundle.getString("content"));
        }
    };

    // 댓글 가져오기
    public void getRequest() {
        String url = "http://54.250.154.173:8080/api/bill/"+billId+"/comments";
        Log.d("발의법률안 id", billId);
        Log.d("발의법률안 url", url);

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("응답", response);
                        processResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("에러", error.getMessage());
                    }
                }
        ) {
            @Override // response를 UTF8로 변경 (한글 깨짐 해결)
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    String utf8String = new String(response.data, "UTF-8");
                    return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                } catch (Exception e) {
                    return Response.error(new ParseError(e));
                }
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                return params;
            }
        };
        request.setShouldCache(false);
        AppHelper.requestQueue.add(request);
    }

    private void getLike(){
        SharedPreferences pref = getSharedPreferences("person_info", 0);
        String userId = pref.getString("currUID", "");
        try{
            OkHttpClient client = new OkHttpClient();
            String strURL = String.format("http://54.250.154.173:8080/api/bill/%s/%s/likes", billId, userId);
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(strURL).get();
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            okhttp3.Response response = client.newCall(request).execute();
            if(response.isSuccessful()) {
                ResponseBody body = response.body();
                isLikeClicked = body.string();
                Log.d("getLike", isLikeClicked);
                body.close();
                setLike();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getDisLike() {
        SharedPreferences pref = getSharedPreferences("person_info", 0);
        String userId = pref.getString("currUID", "");
        try {
            OkHttpClient client = new OkHttpClient();
            String strURL = String.format("http://54.250.154.173:8080/api/bill/%s/%s/dislikes", billId, userId);
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(strURL).get();
            Log.d("getDisLike", "billId: " + billId);
            Log.d("getDisLike", "strURL" + strURL);
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            Log.d("getDisLike", "request: " + request);
            okhttp3.Response response = client.newCall(request).execute();
            Log.d("getDisLike", "response: " + response);
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                isDisLikeClicked = body.string();
                Log.d("getDisLike", isDisLikeClicked);
                body.close();
                setDisLike();
            }
        } catch (IOException e) {
            Log.d("getDisLike", "왜 안됨?");
            e.printStackTrace();
        }
    }

    private void postLike(){
        SharedPreferences pref = getSharedPreferences("person_info", 0);
        String userId = pref.getString("currUID", "");
        try{
            OkHttpClient client = new OkHttpClient();
            String strURL = String.format("http://54.250.154.173:8080/api/bill/%s/%s/likes", billId,userId);
            String strBody = "{}";
            Log.d("postLike","strURL" + strURL);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), strBody);
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(strURL).post(requestBody);
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            Log.d("postLike","request: " +request);
            okhttp3.Response response = client.newCall(request).execute();
            Log.d("postLike","response: " +response);
            if(response.isSuccessful()) {
                Log.d("postLike", " response: success");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void postDisLike(){
        SharedPreferences pref = getSharedPreferences("person_info", 0);
        String userId = pref.getString("currUID", "");
        try{
            OkHttpClient client = new OkHttpClient();
            String strURL = String.format("http://54.250.154.173:8080/api/bill/%s/%s/dislikes", billId,userId);
            String strBody = "{}";
            Log.d("postDisLike","strURL" + strURL);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), strBody);
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(strURL).post(requestBody);
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            Log.d("postDisLike","request: " +request);
            okhttp3.Response response = client.newCall(request).execute();
            Log.d("postDisLike","response: " +response);
            if(response.isSuccessful()) {
                Log.d("postDisLike", " response: success");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteLike(){
        SharedPreferences pref = getSharedPreferences("person_info", 0);
        String userId = pref.getString("currUID", "");
        try{
            OkHttpClient client = new OkHttpClient();
            String strURL = String.format("http://54.250.154.173:8080/api/bill/%s/%s/likes", billId,userId);
            String strBody = "{}";
            Log.d("deleteLike","strURL" + strURL);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), strBody);
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(strURL).delete();
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            Log.d("deleteLike","request: " +request);
            okhttp3.Response response = client.newCall(request).execute();
            Log.d("deleteLike","response: " +response);
            if(response.isSuccessful()) {
                Log.d("deleteLike", " response: success");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDisLike(){
        SharedPreferences pref = getSharedPreferences("person_info", 0);
        String userId = pref.getString("currUID", "");
        try{
            OkHttpClient client = new OkHttpClient();
            String strURL = String.format("http://54.250.154.173:8080/api/bill/%s/%s/dislikes", billId,userId);
            String strBody = "{}";
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), strBody);
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(strURL).delete();
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            okhttp3.Response response = client.newCall(request).execute();
            if(response.isSuccessful()) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 댓글 작성
    public void postRequest(String content) {
        try {
            OkHttpClient client = new OkHttpClient();

            SharedPreferences prefs = getSharedPreferences("person_info", 0);
            String authorId = prefs.getString("currUID", "");
            Log.d("authorId",authorId);

            String url = "http://54.250.154.173:8080/api/bill/"+billId+"/"+authorId+"/comments";
            Log.d("url",url);

            String strBody = String.format("{\"content\" : \"%s\"}", content);
            Log.d("strBody",strBody);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), strBody);

            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url).post(requestBody);
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            okhttp3.Response response = client.newCall(request).execute();
            if(response.isSuccessful()) {
                getRequest();
                scrollView.post(new Runnable() {  // 스크롤 맨 밑으로 내리기
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // JSON -> 객체 변환
    public void processResponse(String response) {
        response = "{\"commentList\":"+response+"}";

        Gson gson = new Gson();
        CommentResult commentResult = gson.fromJson(response, CommentResult.class);

        adapter.clear();
        for(int i=0; i<commentResult.commentList.size(); i++) {
            BillComment comment = commentResult.commentList.get(i);
            adapter.addItem(comment);
        }
        adapter.notifyDataSetChanged();
    }

    public void setLike(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        Log.d("debug","isLikeClicked"+isLikeClicked);
                        if (isLikeClicked.equals("false")) {
                            like_button.setSelected(false);
                        }else {
                            like_button.setSelected(true);
                        }
                    }
                });
            }
        }).start();
    }

    public void setDisLike(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        Log.d("debug","isDisLikeClicked"+isDisLikeClicked);
                        if (isDisLikeClicked.equals("false")) {
                            dislike_button.setSelected(false);
                        }else {
                            dislike_button.setSelected(true);
                        }
                    }
                });
            }
        }).start();
    }

    //법률안 가져오기
    public void getBill(){
        String responseString = null;
        try{
            OkHttpClient client = new OkHttpClient();
            String strURL = "http://54.250.154.173:8080/api/bill/"+billId;
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(strURL).get();
            builder.addHeader("Content-type", "application/json");
            okhttp3.Request request = builder.build();
            okhttp3.Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                responseString = body.string();
                Log.d("responseString", responseString);
                body.close();
            }
        }

        catch(Exception e) {
            e.printStackTrace();
        }

        try {
            JSONObject ob = new JSONObject(responseString);
            likeCount = ob.getInt("likeCount");
            dislikeCount = ob.getInt("dislikeCount");
            setData();

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setData() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                likeNum.setText(String.valueOf(likeCount));
                dislikeNum.setText(String.valueOf(dislikeCount));
            }
        });
    }
}