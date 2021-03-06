package com.project.markpollution;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.markpollution.CustomAdapter.CircleTransform;
import com.project.markpollution.CustomAdapter.CommentRecyclerViewAdapter;
import com.project.markpollution.Objects.Comment;
import com.project.markpollution.Objects.PollutionPoint;
import com.project.markpollution.Objects.Report;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailReportActivity extends AppCompatActivity implements OnMapReadyCallback {
    private SupportMapFragment mapFragment;
    private ImageView ivPicture, ivAvatar, ivSpam, ivResolved, ivSendComment, ivDelete_Admin, ivResolved_Admin;
    private RatingBar ratingBar;
    private TextView tvTitle, tvDesc, tvRate, tvTime, tvEmail, tvCate;
    private EditText etComment;
    private String title;   // title of marker
    private RecyclerView recyclerViewComment;
    private String url_RetrieveUserById = "http://indi.com.vn/dev/markpollution/RetrieveUserById.php?id_user=";
    private String url_InsertComment = "http://indi.com.vn/dev/markpollution/InsertComment.php";
    private String url_RetrieveCommentById = "http://indi.com.vn/dev/markpollution/RetrieveCommentById.php?id_po=";
    private String url_CheckUserRatedOrNot = "http://indi.com.vn/dev/markpollution/CheckUserRatedOrNot.php?id_user=";
    private String url_InsertRate = "http://indi.com.vn/dev/markpollution/InsertRate.php";
    private String url_UpdateRate = "http://indi.com.vn/dev/markpollution/UpdateRate.php";
    private String url_RetrieveRateByUser = "http://indi.com.vn/dev/markpollution/RetrieveRateByUser.php?id_user=";
    private String url_SumRate = "http://indi.com.vn/dev/markpollution/SumRateByPo.php?id_po=";
    private String url_InsertSpam = "http://indi.com.vn/dev/markpollution/InsertSpam.php";
    private String url_CheckUserSpamOrNot = "http://indi.com.vn/dev/markpollution/CheckUserSpamOrNot.php?id_user=";
    private String url_InsertResolve = "http://indi.com.vn/dev/markpollution/InsertResolve.php";
    private String url_CheckUserCheckResolveOrNot = "http://indi.com.vn/dev/markpollution/CheckUserCheckResolvedOrNot.php?id_user=";
    private String url_DeletePoById = "http://indi.com.vn/dev/markpollution//DeletePollutionById.php?id_po=";
    private GoogleMap gMap;
    private double lat, lng;
    private String id_po;
    private List<Comment> listComment = new ArrayList<>();
    private boolean isFirstTimeShowRate = false;
    private boolean is_admin;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private boolean isFirstTimeRun = true;
    private RelativeLayout relativeLayoutDetail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_report);

        initView();
        getPoInfo();
        checkIsAdminOrNotToModifyDetailReport();
        sendComment();
        retrieveComments();
        checkUserRatedOrNotToInsertOrUpdate();
        showRateByUser();
        sumRate();
        insertSpam();
        showSpamStatusWhenStartup();
        showResolveStatusWhenStartup();
        insertResolve();
        deleteReport();
        refreshDataWhenDeleteReport();
        ivPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DetailReportActivity.this, Fullscreenimage.class);

                ivPicture.buildDrawingCache();
                Bitmap image= ivPicture.getDrawingCache();

                Bundle extras = new Bundle();
                extras.putParcelable("imagebitmap", image);
                intent.putExtras(extras);
                startActivity(intent);
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
        });
        etComment.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (s.length()==0){
                    ivSendComment.setVisibility(View.GONE);
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                ivSendComment.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initView() {
        relativeLayoutDetail = (RelativeLayout) findViewById(R.id.relative_detail);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDetail);
        ivPicture = (ImageView) findViewById(R.id.imageViewDetail);
        ivAvatar = (ImageView) findViewById(R.id.imageViewAvatarDetail);
        ivSpam = (ImageView) findViewById(R.id.imageViewSpam);
        ivResolved = (ImageView) findViewById(R.id.imageViewResolved);
        ivSendComment = (ImageView) findViewById(R.id.imageViewSendComment);
        ivDelete_Admin = (ImageView) findViewById(R.id.imageViewDelete_Admin);
        ivResolved_Admin = (ImageView) findViewById(R.id.imageViewResolved_Admin);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        tvTitle = (TextView) findViewById(R.id.textViewTitleDetail);
        tvDesc = (TextView) findViewById(R.id.textViewDescDetail);
        tvRate = (TextView) findViewById(R.id.textViewRateDetail);
        tvTime = (TextView) findViewById(R.id.textViewTimeDetail);
        tvEmail = (TextView) findViewById(R.id.textViewEmailDetail);
        tvCate = (TextView) findViewById(R.id.textViewCategoryDetail);
        etComment = (EditText) findViewById(R.id.editTextComment);

        recyclerViewComment = (RecyclerView) findViewById(R.id.recyclerViewComment);
        LinearLayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerViewComment.setLayoutManager(layout);

        mapFragment.getMapAsync(this);
        // initialize Firebase;
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    private void getPoInfo() {
        Intent intent = getIntent();
        id_po = intent.getStringExtra("id_po");

        String id_user = "";
        String id_cate = "";
        title = "";
        String desc = "";
        String image = "";
        String time = "";

        for(PollutionPoint po: MainActivity.listPo){
            if(po.getId().equals(id_po)){
                id_user = po.getId_user();
                id_cate = po.getId_cate();
                lat = po.getLat();
                lng = po.getLng();
                title = po.getTitle();
                desc = po.getDesc();
                image = po.getImage();
                time = po.getTime();
                break;
            }
        }

        // pass values into Views
        tvTitle.setText(title);
        tvDesc.setText(desc);
        Picasso.with(this).load(Uri.parse(image))
                .placeholder(R.drawable.placeholder)
                .into(ivPicture);
        tvTime.setText(formatDateTime(time));

        // set email & avatar of user
        setEmailnAvatar(id_user);
        // set category
        tvCate.setText(getCategoryName(Integer.parseInt(id_cate)));
    }

    private String formatDateTime(String time){
        SimpleDateFormat originFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat resultFormat = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy");

        Date datetime = null;
        try {
            datetime = originFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return resultFormat.format(datetime);
    }

    private void setEmailnAvatar(String UserID){
        String finalUrl = url_RetrieveUserById + UserID;
        JsonObjectRequest objReq = new JsonObjectRequest(Request.Method.GET, finalUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getString("status").equals("success")){
                        JSONArray arr = response.getJSONArray("response");
                        JSONObject user = arr.getJSONObject(0);
                        String email = user.getString("email");
                        String avatar = user.getString("avatar");

                        tvEmail.setText(email);
                        Picasso.with(DetailReportActivity.this).load(Uri.parse(avatar)).resize(70,70).transform(new CircleTransform()).into(ivAvatar);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Volley", error.getMessage());
            }
        });

        Volley.newRequestQueue(this).add(objReq);
    }

    private String getCategoryName(int CateID){
        String value = "";
        switch (CateID){
            case 1:
                value = "Plan Pollution";
                break;
            case 2:
                value = "Water Pollution";
                break;
            case 3:
                value = "Air Pollution";
                break;
            case 4:
                value = "Thermal Pollution";
                break;
            case 5:
                value = "Light Pollution";
                break;
            case 6:
                value = "Noise Pollution";
                break;
        }
        return value;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        LatLng point = new LatLng(lat, lng);
        Marker marker = googleMap.addMarker(new MarkerOptions().position(point).title(title));
        marker.showInfoWindow();

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 12));
        googleMap.getUiSettings().setMapToolbarEnabled(false);
    }

    private void sendComment() {
        ivSendComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!etComment.getText().toString().isEmpty()){
                    StringRequest stringReq = new StringRequest(Request.Method.POST, url_InsertComment, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
//                            Toast.makeText(DetailReportActivity.this, response, Toast.LENGTH_SHORT).show();
                            etComment.setText(null);
                            retrieveComments();
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
//                            Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("Volley", error.getMessage());
                            etComment.setText(null);
                        }
                    }){
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            HashMap<String, String> params = new HashMap<>();
                            params.put("id_po", id_po);
                            params.put("id_user", getUserID());
                            params.put("comment", etComment.getText().toString());
                            return params;
                        }
                    };

                    Volley.newRequestQueue(DetailReportActivity.this).add(stringReq);
                    hideKeyboard(v);
                }
            }
        });
    }
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    private String getUserID(){
        SharedPreferences sharedPreferences = getSharedPreferences("sharedpref_id_user",MODE_PRIVATE);
        return sharedPreferences.getString("sharedpref_id_user","");
    }

    private void retrieveComments(){
        JsonObjectRequest objReq = new JsonObjectRequest(Request.Method.GET, url_RetrieveCommentById + id_po, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getString("status").equals("success")){
                        JSONArray arr = response.getJSONArray("response");
                        listComment = new ArrayList<>();    // reinitialize list<Comment> when retrieve comments
                        for(int i=0; i<arr.length(); i++){
                            JSONObject comment = arr.getJSONObject(i);
                            listComment.add(new Comment(comment.getString("id_po"), comment.getString("id_user"), comment.getString("comment"), comment.getString("time")));
                        }
                        loadComments();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
//                Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        Volley.newRequestQueue(this).add(objReq);
    }

    private void loadComments(){
        recyclerViewComment.setAdapter(new CommentRecyclerViewAdapter(this, listComment));
    }

    private void checkUserRatedOrNotToInsertOrUpdate(){
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, final float rating, boolean fromUser) {
                // if it's not the first time show rate ==> insert OR update
                if(!isFirstTimeShowRate){
                    // Check user has rated or not
                    String completed_urlCheckUserRatedOrNot = url_CheckUserRatedOrNot + getUserID() + "&id_po=" + id_po;
                    StringRequest strReq = new StringRequest(Request.Method.GET, completed_urlCheckUserRatedOrNot, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(response.equals("User hasn't rated")){
                                // insert rate
                                insertRate(rating);
                            }else{
                                // edit rate
                                updateRate(rating);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
//                            Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
                            Log.e("Volley", error.getMessage());
                        }
                    });

                    Volley.newRequestQueue(DetailReportActivity.this).add(strReq);
                }
                isFirstTimeShowRate = false;
            }
        });
    }

    private void insertRate(final float rate){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url_InsertRate, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response.equals("rating failed")){
//                    Toast.makeText(DetailReportActivity.this, getResources().getString(R.string.rate_failed), Toast.LENGTH_SHORT).show();
                    Snackbar.make(relativeLayoutDetail,  getResources().getString(R.string.rate_failed), Snackbar.LENGTH_LONG).show();
                }
                if(response.equals("rating success")){
//                    Toast.makeText(DetailReportActivity.this, getResources().getString(R.string.rate_success), Toast.LENGTH_SHORT).show();
                    Snackbar.make(relativeLayoutDetail,  getResources().getString(R.string.rate_success), Snackbar.LENGTH_LONG).show();
                }
                sumRate();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
                Log.e("Volley", error.getMessage());
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("id_po", id_po);
                params.put("id_user", getUserID());
                params.put("rate",Integer.toString((int)rate));

                return params;
            }
        };

        Volley.newRequestQueue(DetailReportActivity.this).add(stringRequest);
    }

    private void updateRate(final float rate){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url_UpdateRate, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response.equals("The rate was updated")){
//                    Toast.makeText(DetailReportActivity.this, getResources().getString(R.string.rate_was_update), Toast.LENGTH_SHORT).show();
                    Snackbar.make(relativeLayoutDetail,  getResources().getString(R.string.rate_was_update), Snackbar.LENGTH_LONG).show();
                }
                if(response.equals("The rate wasn't updated")){
                    Snackbar.make(relativeLayoutDetail,  getResources().getString(R.string.rate_wasnot_update), Snackbar.LENGTH_LONG).show();
//                    Toast.makeText(DetailReportActivity.this, getResources().getString(R.string.rate_wasnot_update), Toast.LENGTH_SHORT).show();
                }                sumRate();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
                Log.e("Volley", error.getMessage());
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("id_po", id_po);
                params.put("id_user", getUserID());
                params.put("rate",Integer.toString((int)rate));

                return params;
            }
        };

        Volley.newRequestQueue(DetailReportActivity.this).add(stringRequest);
    }

    private void showRateByUser() {
        String completed_url_RetrieveRateByUser = url_RetrieveRateByUser + getUserID() + "&id_po=" + id_po;
        StringRequest strReq = new StringRequest(Request.Method.GET, completed_url_RetrieveRateByUser, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(!response.equals("no row match")){
                    isFirstTimeShowRate = true;
                    ratingBar.setRating(Float.valueOf(response));
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
                Log.e("Volley", error.getMessage());
            }
        });

        Volley.newRequestQueue(DetailReportActivity.this).add(strReq);
    }

    private void sumRate(){
        StringRequest stringReq = new StringRequest(Request.Method.GET, url_SumRate + id_po, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                tvRate.setText(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
                Log.e("Volley_SumRate", error.getMessage());
            }
        });

        Volley.newRequestQueue(DetailReportActivity.this).add(stringReq);
    }

    private void insertSpam() {
        ivSpam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringRequest strReq = new StringRequest(Request.Method.POST, url_InsertSpam, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.equals("You have unchecked spam")){
                            setIconSpam(false);
//                            Toast.makeText(DetailReportActivity.this, R.string.uncheck_spam, Toast.LENGTH_SHORT).show();
                            Snackbar.make(relativeLayoutDetail,  R.string.uncheck_spam, Snackbar.LENGTH_LONG).show();
                        }else if(response.equals("Spam successful")){
                            setIconSpam(true);
//                            Toast.makeText(DetailReportActivity.this, R.string.check_spam, Toast.LENGTH_SHORT).show();
                            Snackbar.make(relativeLayoutDetail,  R.string.uncheck_spam, Snackbar.LENGTH_LONG).show();

                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        Snackbar.make(relativeLayoutDetail,  error.getMessage(), Snackbar.LENGTH_LONG).show();
                        Log.e("Volley_insertSpam", error.getMessage());
                    }
                }){
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        HashMap<String, String> params = new HashMap<>();
                        params.put("id_po", id_po);
                        params.put("id_user", getUserID());
                        return params;
                    }
                };

                Volley.newRequestQueue(DetailReportActivity.this).add(strReq);
            }
        });
    }

    private void setIconSpam(boolean spam){
        if(spam){
            ivSpam.setImageResource(R.drawable.ic_spam);
        }else{
            ivSpam.setImageResource(R.drawable.ic_spam_grey);
        }
    }

    private void setIconResolve(boolean resolved){
        if(resolved){
            ivResolved.setImageResource(R.drawable.ic_has_resolved_admin);
        }else{
            ivResolved.setImageResource(R.drawable.ic_has_resolved_grey);
        }
    }

    private void showSpamStatusWhenStartup() {
        String completed_url_CheckUserSpamOrNot = url_CheckUserSpamOrNot + getUserID() + "&id_po=" + id_po;
        StringRequest strReq = new StringRequest(Request.Method.GET, completed_url_CheckUserSpamOrNot, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response.equals("User has checked spam")){
                    setIconSpam(true);
                }else if(response.equals("User hasn't checked spam")){
                    setIconSpam(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        Volley.newRequestQueue(DetailReportActivity.this).add(strReq);
    }

    private void insertResolve(){
        ivResolved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringRequest strReq = new StringRequest(Request.Method.POST, url_InsertResolve, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.equals("You have unchecked resolve")){
//                            Toast.makeText(DetailReportActivity.this, R.string.uncheck_resolve, Toast.LENGTH_SHORT).show();
                            Snackbar.make(relativeLayoutDetail, R.string.uncheck_resolve, Snackbar.LENGTH_LONG).show();
                            setIconResolve(false);
                        }else if(response.equals("Check resolved successful")){
//                            Toast.makeText(DetailReportActivity.this, R.string.check_resolve, Toast.LENGTH_SHORT).show();
                            Snackbar.make(relativeLayoutDetail, R.string.check_resolve, Snackbar.LENGTH_LONG).show();
                            setIconResolve(true);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        Snackbar.make(relativeLayoutDetail, error.getMessage(), Snackbar.LENGTH_LONG).show();
                        Log.e("Volley_InsertResolve", error.getMessage());
                    }
                }){
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        HashMap<String, String> params = new HashMap<>();
                        params.put("id_user", getUserID());
                        params.put("id_po", id_po);
                        return params;
                    }
                };

                Volley.newRequestQueue(DetailReportActivity.this).add(strReq);
            }
        });
    }

    private void showResolveStatusWhenStartup() {
        String completed_url_CheckUserCheckResolveorNot = url_CheckUserCheckResolveOrNot + getUserID() + "&id_po=" + id_po;
        StringRequest strReq = new StringRequest(Request.Method.GET, completed_url_CheckUserCheckResolveorNot, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response.equals("User has checked resolve")){
                    setIconResolve(true);
                }else if(response.equals("User hasn't checked resolve")){
                    setIconResolve(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                Snackbar.make(relativeLayoutDetail, error.getMessage(), Snackbar.LENGTH_LONG).show();
                Log.e("Volley_CheckResolve", error.getMessage());
            }
        });

        Volley.newRequestQueue(DetailReportActivity.this).add(strReq);
    }

    private void checkIsAdminOrNotToModifyDetailReport(){
        Intent intent = getIntent();
        is_admin = intent.getBooleanExtra("admin", false);
        if(is_admin){
            ivSpam.setVisibility(View.INVISIBLE);
            ivResolved.setVisibility(View.INVISIBLE);

            ivDelete_Admin.setVisibility(View.VISIBLE);
            ivResolved_Admin.setVisibility(View.VISIBLE);
        }
    }

    private void deleteReport(){
        ivDelete_Admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.askdelete_byadmin, Snackbar.LENGTH_INDEFINITE).setAction("OK",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                StringRequest strReq = new StringRequest(Request.Method.GET, url_DeletePoById +
                                        id_po, new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        if(response.equals("Delete pollution successful")){
//                                            Toast.makeText(DetailReportActivity.this, response, Toast.LENGTH_SHORT).show();
                                            Snackbar.make(relativeLayoutDetail, response, Snackbar.LENGTH_LONG).show();
                                            notifyRefreshData();    // put a trigger on firebase

                                            // From twice data changed. Toggle trigger refresh data in MainActivity
                                            MainActivity.triggerRefreshData = true;

                                            // return AdminActivity to reload data
                                            finish();
                                        }else {
//                                            Toast.makeText(DetailReportActivity.this, response, Toast.LENGTH_SHORT).show();
                                            Snackbar.make(relativeLayoutDetail, response, Snackbar.LENGTH_LONG).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
//                                        Toast.makeText(DetailReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                        Snackbar.make(relativeLayoutDetail, error.getMessage(), Snackbar.LENGTH_LONG).show();

                                    }
                                });

                                Volley.newRequestQueue(DetailReportActivity.this).add(strReq);
                            }
                        }).show();
            }
        });
    }

    // Put a trigger on Firebase to notify to another users
    private void notifyRefreshData(){
        DatabaseReference refDeleteReport = databaseReference.child("DeleteReports");
        refDeleteReport.setValue(new Report(id_po, getUserID()));
    }

    private void refreshDataWhenDeleteReport(){
            DatabaseReference refDeleteReport = databaseReference.child("DeleteReports");
            refDeleteReport.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(!isFirstTimeRun){
                    Report report = dataSnapshot.getValue(Report.class);
                        if(!report.getId_user().equals(getUserID()) && report.getId_report().equals(id_po)){
                            alertDialog(getResources().getString(R.string.delete_byadmin));
                        }
                    }
                    isFirstTimeRun = false;
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
    }

    private void alertDialog(String msg){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getResources().getString(R.string.warning));
        dialog.setMessage(msg);
        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Toggle trigger refresh data in MainActivity
                MainActivity.triggerRefreshData = true;
                finish();
            }
        }).show();
    }
}