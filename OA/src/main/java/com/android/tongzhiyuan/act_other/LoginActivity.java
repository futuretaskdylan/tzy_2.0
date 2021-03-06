package com.android.tongzhiyuan.act_other;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tongzhiyuan.act_0.MainActivity;
import com.android.tongzhiyuan.util.DialogUtils;
import com.android.tongzhiyuan.util.Utils;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.tongzhiyuan.App;
import com.android.tongzhiyuan.R;
import com.android.tongzhiyuan.core.utils.Constant;
import com.android.tongzhiyuan.core.utils.DialogHelper;
import com.android.tongzhiyuan.core.utils.KeyConst;
import com.android.tongzhiyuan.core.utils.NetUtil;
import com.android.tongzhiyuan.core.utils.TextUtil;
import com.android.tongzhiyuan.util.ToastUtil;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Gool Lee
 */
public class LoginActivity extends BaseFgActivity implements View.OnClickListener {
    private MaterialEditText et_pwd, et_user;
    private TextView bt_find_pwd, bt_register;
    private Button bt_login;
    private SharedPreferences sp;
    private LoginActivity context;
    private DialogHelper dialogHelper;
    private String accessToken;
    private boolean isFirstLuncher;
    private String pwd, username;
    private ImageView welcomeIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_login);
        context = this;
        sp = getSharedPreferences(Constant.CONFIG_FILE_NAME, MODE_PRIVATE);
        isFirstLuncher = sp.getBoolean(KeyConst.IS_FIRST_LUNCHER_SP, true);

        et_user = (MaterialEditText) findViewById(R.id.et_login_user);
        et_user. setKeyListener(DigitsKeyListener.getInstance(getString(R.string.account_digits)));
        et_pwd = (MaterialEditText) findViewById(R.id.et_login_pwd);
        username = sp.getString(KeyConst.username, "");
        pwd = sp.getString(Constant.sp_pwd, "");

        welcomeIv = (ImageView) findViewById(R.id.welcome_iv);
        bt_find_pwd = (TextView) findViewById(R.id.tv_find_pwd);
        bt_find_pwd.setOnClickListener(this);
        bt_register = (TextView) findViewById(R.id.tv_register);
        bt_register.setOnClickListener(this);
        bt_login = (Button) findViewById(R.id.but_login);
        bt_login.setOnClickListener(this);

        dialogHelper = new DialogHelper(getSupportFragmentManager(), context);
        findViewById(R.id.login_qq_bt).setOnClickListener(this);
        findViewById(R.id.login_wechat_bt).setOnClickListener(this);
        findViewById(R.id.login_sina_bt).setOnClickListener(this);

        et_user.setText(username);
        et_user.setSelection(et_user.getText().length());
        if (!TextUtil.isEmpty(username) && !TextUtil.isEmpty(pwd)) {
            welcomeIv.setVisibility(View.VISIBLE);
            et_pwd.setText(pwd);
            doLogin(true);
        } else {
            Utils.requestStoragePermissions(context);
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    welcomeIv.setVisibility(View.GONE);
                    getWindow().getDecorView().setBackgroundColor(Color.WHITE);
                }
            }, 1500);

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.but_login:
                username = et_user.getText().toString();
                if (TextUtil.isEmpty(username)) {
                    ToastUtil.show(context, "请输入账号");
                    return;
                }
                pwd = et_pwd.getText().toString();
                if (TextUtil.isEmpty(pwd)) {
                    ToastUtil.show(context, "请输入密码");
                    return;
                }
                if (!NetUtil.isNetworkConnected(context)) {
                    ToastUtil.show(context, getString(R.string.no_network));
                    return;
                }
                dialogHelper.showAlert("登录中...", true);
                doLogin(false);
                break;
            case R.id.tv_find_pwd:
                startActivity(new Intent(this, FindPwdActivity.class));
                break;
            case R.id.tv_register:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
        }
    }

    private void doLogin(final boolean isAutoLogin) {
        String url = Constant.WEB_SITE + Constant.URL_USER_LOGIN;
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String result) {
                        if (result == null) {
                            if (isAutoLogin) {
                                startActivity(new Intent(context, MainActivity.class));
                                context.finish();
                                return;
                            }
                            if (null != context && !context.isFinishing()) {
                                dialogHelper.hideAlert();
                            }
                            ToastUtil.show(context, getString(R.string.server_exception));
                            return;
                        }
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            accessToken = jsonObject.getString(KeyConst.access_token);
                        } catch (JSONException e) {
                            accessToken = null;
                        }
                        if (TextUtil.isEmpty(accessToken)) {
                            if (isAutoLogin) {
                                startActivity(new Intent(context, MainActivity.class));
                                context.finish();
                                return;
                            }
                            if (null != context && !context.isFinishing()) {
                                dialogHelper.hideAlert();
                            }
                            ToastUtil.show(context, getString(R.string.server_exception));
                            return;
                        }
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(Constant.SP_TOKEN, accessToken);
                        editor.putString(KeyConst.username, username);
                        editor.putString(Constant.sp_pwd, pwd);
                        editor.apply();
                        App.token = accessToken;
                        App.passWord = pwd;
                        App.username = username;
                        App.phone = username;
                        startActivity(new Intent(context, MainActivity.class));
                        context.finish();
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (isAutoLogin) {
                    startActivity(new Intent(context, MainActivity.class));
                    context.finish();
                    return;
                }
                if (null != context && !context.isFinishing()) {
                    dialogHelper.hideAlert();
                }
                String errorMsg = TextUtil.getErrorMsg(error);
                try {
                    if (errorMsg != null) {
                        JSONObject obj = new JSONObject(errorMsg);
                        if (obj != null) {
                            int errInt = obj.getInt(KeyConst.error);
                            //账号密码错误
                            if (errInt == 10001) {
                                DialogUtils.showTipDialog(context, getString(R.string.account_pwd_error));
                                return;
                                //账号冻结
                            } else if (errInt == 10003) {
                                DialogUtils.showTipDialog(context, getString(R.string.contact_admin));
                                return;
                            }
                        }

                    }
                } catch (JSONException e) {
                }
                ToastUtil.show(context, R.string.server_exception);
            }
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put(KeyConst.Content_Type, Constant.application_form);
                params.put(KeyConst.Authorization, Constant.authorization);
                return params;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put(KeyConst.username, username);
                params.put(KeyConst.password, pwd);
                params.put(KeyConst.grant_type, KeyConst.password);
                return params;
            }

        };

        App.requestQueue.add(jsonObjRequest);
    }
}
