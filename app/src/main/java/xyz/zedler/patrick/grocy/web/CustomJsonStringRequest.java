package xyz.zedler.patrick.grocy.web;

import android.util.Base64;

import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomJsonStringRequest extends Request<String> {
    private final Listener<String> mListener;
    private final JSONObject mRequestBody;
    private final String apiKey;
    private final String homeAssistantIngressSessionKey;

    public CustomJsonStringRequest(
            int method,
            String url,
            String apiKey,
            String homeAssistantIngressSessionKey,
            @Nullable JSONObject requestBody,
            Response.Listener<String> listener,
            @Nullable Response.ErrorListener errorListener,
            int timeoutSeconds,
            String tag) {
        super(method, url, errorListener);

        mListener = listener;
        mRequestBody = requestBody;

        this.apiKey = apiKey;
        this.homeAssistantIngressSessionKey = homeAssistantIngressSessionKey;

        if (tag != null) {
            setTag(tag);
        }
        setShouldCache(false);
        RetryPolicy policy = new DefaultRetryPolicy(
                timeoutSeconds * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        setRetryPolicy(policy);
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = Pattern.compile("(http|https)://(\\S+):(\\S+)@(\\S+)").matcher(getUrl());
        if (matcher.matches()) {
            String user = matcher.group(2);
            String password = matcher.group(3);
            byte[] combination = (user + ":" + password).getBytes();
            String encoded = Base64.encodeToString(combination, Base64.DEFAULT);
            params.put("Authorization", "Basic " + encoded);
        }
        if (apiKey != null && !apiKey.isEmpty()) {
            params.put("GROCY-API-KEY", apiKey);
        }
        if (homeAssistantIngressSessionKey != null) {
            params.put("Cookie", "ingress_session=" + homeAssistantIngressSessionKey);
        }
        params.put("Content-Type", "application/json");
        return params.isEmpty() ? Collections.emptyMap() : params;
    }

    @Override
    public String getBodyContentType() {
        return "application/json; charset=utf-8";
    }

    @Override
    public byte[] getBody() {
        if (mRequestBody != null) {
            return mRequestBody.toString().getBytes();
        }
        return null;
    }

    @Override
    protected void deliverResponse(String response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
}

