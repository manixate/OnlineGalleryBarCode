package com.manixate.OnlineGallery;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.manixate.OnlineGallery.Utils.NetworkManager;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class AuthenticationActivity extends Activity {
    @InjectView(R.id.usernameTextEdit)
    EditText mUsernameTextEdit;
    @InjectView(R.id.passwordTextEdit)
    EditText mPasswordTextEdit;
    @InjectView(R.id.loginBtn)
    Button mLoginBtn;
    @InjectView(R.id.signupBtn)
    Button mSignupBtn;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentication);

        ButterKnife.inject(this);

        try {
            NetworkManager.getInstance().loadCredentials(this);

            if (NetworkManager.getInstance().isAuthenticated(this))
                startMainActivity();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        finish();
    }

    @OnClick(R.id.loginBtn)
    public void loginBtnPressed(Button button) {
        String userName = mUsernameTextEdit.getText().toString();
        String password = mPasswordTextEdit.getText().toString();

        if (userName.equals("") || password.equals("")) {
            Toast.makeText(this, "Please fill the required fields", Toast.LENGTH_LONG).show();

            return;
        }

        new AuthenticationTask(this, TaskType.LOGIN).execute(userName, password);
    }

    @OnClick(R.id.signupBtn)
    public void signupBtnPressed(Button button) {
        String userName = mUsernameTextEdit.getText().toString();
        String password = mPasswordTextEdit.getText().toString();

        if (userName.equals("") || password.equals("")) {
            Toast.makeText(this, "Please fill the required fields", Toast.LENGTH_LONG).show();

            return;
        }

        new AuthenticationTask(this, TaskType.SIGNUP).execute(userName, password);
    }

    enum TaskType {
        SIGNUP,
        LOGIN
    }

    class AuthenticationTask extends AsyncTask<String, String, String> {

        ProgressDialog progressDialog;
        final TaskType taskType;
        final Context context;

        public AuthenticationTask(Context context, TaskType taskType) {
            this.context = context;
            this.taskType = taskType;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);

            if (TaskType.SIGNUP == taskType)
                progressDialog.setMessage("Please wait. Signing Up.");
            else
                progressDialog.setMessage("Please wait. Logging In.");

            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            String userName = strings[0];
            String password = strings[1];

            AndroidHttpClient httpClient = NetworkManager.getInstance().getHttpClient();

            HttpResponse httpResponse;
            HttpPost loginRequest;

            if (TaskType.SIGNUP == taskType)
                loginRequest = new HttpPost(Constants.SignupURLString);
            else
                loginRequest = new HttpPost(Constants.LoginURLString);

            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>(2);
            nameValuePairList.add(new BasicNameValuePair("username", userName));
            nameValuePairList.add(new BasicNameValuePair("password", password));

            try {
                loginRequest.setEntity(new UrlEncodedFormEntity(nameValuePairList));
                httpResponse = httpClient.execute(loginRequest, NetworkManager.getInstance().getHttpContext());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();

                return null;
            } catch (IOException e) {
                e.printStackTrace();

                return null;
            }

            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                try {
                    return EntityUtils.toString(httpResponse.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();

                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressDialog.dismiss();

            if (s == null) {
                return;
            }

            try {
                JSONObject result = new JSONObject(s);

                Toast.makeText(context, result.getString("message"), Toast.LENGTH_LONG).show();

                if (result.getBoolean("success")) {
                    NetworkManager.getInstance().saveCredentials(context);

                    startMainActivity();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
