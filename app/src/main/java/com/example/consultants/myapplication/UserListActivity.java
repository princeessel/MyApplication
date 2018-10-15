package com.example.consultants.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.satti.randomuser.model.User;
import com.satti.randomuser.util.RandomuserConstants;
import com.satti.randomuser.util.RestAPIAsync;
import com.satti.randomuser.util.RestAPIUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class UserListActivity extends AppCompatActivity implements RestAPIAsync.ProgressCallback, RestAPIAsync.ResponseCallback {

    private final static String TAG = UserListActivity.class.getSimpleName();

    private boolean mTwoPane;

    private ProgressDialog progressDialog;
    //private View recyclerView;
    private RecyclerView recyclerView;
    private SimpleItemRecyclerViewAdapter adapter;

    ArrayList<User> userList = new ArrayList<User>();

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        recyclerView = (RecyclerView)findViewById(R.id.user_list);
        assert recyclerView != null;

        adapter = new SimpleItemRecyclerViewAdapter(userList);
        this.recyclerView.setAdapter(adapter);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        if (findViewById(R.id.user_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        callRandomuserAPI(RandomuserConstants.REQ_PARAM_PAGE_START_VALUE);
    }


    public void callRandomuserAPI(int page) {
         // Create the request
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(RandomuserConstants.RANDOMUSER_URI);
            sb.append("?");
            sb.append(RandomuserConstants.REQ_PARAM_PAGE + "=" + page);
            sb.append("&");
            sb.append(RandomuserConstants.REQ_PARAM_RESULT + "=" + RandomuserConstants.REQ_PARAM_RESULT_DEFAULT_VALUE);

            Log.d(TAG,  "loading page " + page +": "+ sb.toString());

            RestAPIAsync getTask = RestAPIUtil.obtainRestAPIAsync(sb.toString());
            getTask.setResponseCallback(this);
            getTask.setProgressCallback(this);
            getTask.execute();

            // Display progress to the user
            progressDialog = ProgressDialog.show(this, "", getString(R.string.mesg_processing), true);
        }
        catch (IOException e) {
            showErrorMesage(getString(R.string.error_io));
            Log.e(TAG, "IO Exception", e);
        }
    }

    @Override
    public void onProgressUpdate(int progress) {
        if (progress >= 0) {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }


    @Override
    public void onRequestError(Exception error) {

        //Clear progress indicator
        if(progressDialog != null) {
            progressDialog.dismiss();
        }
        showErrorMesage(getString(R.string.error_api));
    }


    @Override
    public void onRequestSuccess(Object resp) {

        if (resp instanceof String) {

            String response = (String) resp;

            //Clear progress indicator
            if(progressDialog != null) {
                progressDialog.dismiss();
            }

            //Process the response data
            try {
                JSONObject responseObj = new JSONObject(response);

                if (responseObj.has(RandomuserConstants.RESP_PARAM_ERROR)) {
                    String error = responseObj.getString(RandomuserConstants.RESP_PARAM_ERROR);
                    showErrorMesage(error);
                }
                else if (responseObj.has(RandomuserConstants.RESP_PARAM_RESULTS)) {
                    // populate RecycleView data source with newly received data

                    //JSONObject info = responseObj.getJSONObject(RandomuserConstants.RESP_PARAM_RESULTS_INFO);
                    JSONArray results = responseObj.getJSONArray(RandomuserConstants.RESP_PARAM_RESULTS);
                    for(int i=0; i < results.length(); i++) {
                        String user = results.getJSONObject(i).toString();
                        // convert JSON string to object
                        User userObj = objectMapper.readValue(user, User.class);
                        userList.add(userObj);
                    }
                    adapter.notifyDataSetChanged();
                }
                else {
                    // Invalid response sent by Randomuser API
                    showErrorMesage(getString(R.string.error_invalid_response));
                }
            }
            catch (JSONException e) {
                showErrorMesage(getString(R.string.error_parsing_json));
                Log.e(TAG, "JSONException: ", e);
            }
            catch (JsonParseException e) {
                showErrorMesage(getString(R.string.error_parsing_json));
                Log.e(TAG, "JsonMappingException: ", e);
            }
            catch (JsonMappingException e) {
                showErrorMesage(getString(R.string.error_parsing_json));
                Log.e(TAG, "JsonMappingException: ", e);
            }
            catch (IOException e) {
                showErrorMesage(getString(R.string.error_io));
                Log.e(TAG, "JsonMappingException: ", e);
            }
        }
    }

    /**
     * Display message
     *
     */
    public void showErrorMesage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * RecycleViewAdapter
     *
     */
    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<User> users;

        public SimpleItemRecyclerViewAdapter(List<User> users) {
            this.users = users;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.user_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.user = users.get(position);

            if(position==(getItemCount()-RandomuserConstants.VISIBLE_THRESHOLD)) {
                int nextPage = getItemCount()/RandomuserConstants.REQ_PARAM_RESULT_DEFAULT_VALUE+1;
                Log.d(TAG, "Next page #:" + nextPage + ", loaded items: "+ getItemCount());
                callRandomuserAPI(nextPage);
            }

            User user = users.get(position);
            holder.nameView.setText(user.getName().toString());
            holder.mobileView.setText(user.getCell());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        // Convert to JSON
                        String selectedUer = objectMapper.writeValueAsString(holder.user);

                        if (mTwoPane) {
                            Log.d(TAG, "two pane");
                            // Replace NestedScrollView with fragment for selected user
                            Bundle arguments = new Bundle();
                            arguments.putString(RandomuserConstants.FRAGMENT_ARG_SELECTED_USER, selectedUer);
                            UserDetailFragment fragment = new UserDetailFragment();
                            fragment.setArguments(arguments);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.user_detail_container, fragment)
                                    .commit();
                        }
                        else {
                            Log.d(TAG, "one pane");
                            // Launch new activity for selected user
                            Context context = v.getContext();
                            Intent intent = new Intent(context, UserDetailActivity.class);
                            intent.putExtra(RandomuserConstants.FRAGMENT_ARG_SELECTED_USER, selectedUer);
                            context.startActivity(intent);
                        }
                    }
                    catch (JsonProcessingException e) {
                        showErrorMesage(getString(R.string.error_parsing_json));
                        Log.e(TAG, "JsonProcessingException: ", e);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {

            return users.size();
        }

        /**
         *
         * ViewHolder
         *
         */
        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView nameView;
            public final TextView mobileView;
            public User user;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                nameView = (TextView) view.findViewById(R.id.list_item_user_name);
                mobileView = (TextView) view.findViewById(R.id.list_item_user_mobile);
            }

            @Override
            public String toString() {
                return new StringBuilder().append(super.toString()).
                        append(" '").append(mobileView.getText()).append("'").toString();

            }
        }
    }
}
