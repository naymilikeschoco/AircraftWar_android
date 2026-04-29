package edu.hitsz.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import androidx.annotation.NonNull;
import edu.hitsz.auth.GameConfigState;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CloudApiClient {

    public interface SimpleCallback {
        void onSuccess(String value);

        void onError(String message);
    }

    public interface ConfigCallback {
        void onSuccess(GameConfigState state);

        void onError(String message);
    }

    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl;

    public CloudApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void register(String username, String password, SimpleCallback callback) {
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url(baseUrl + "/register")
                .post(body)
                .build();
        sendSimple(request, callback, null);
    }

    public void login(String username, String password, SimpleCallback callback) {
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url(baseUrl + "/login")
                .post(body)
                .build();
        sendSimple(request, callback, "token");
    }

    public void fetchConfig(String token, ConfigCallback callback) {
        HttpUrl parsed = HttpUrl.parse(baseUrl + "/config");
        if (parsed == null) {
            callback.onError("Invalid base url");
            return;
        }
        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("token", token)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String payload = readBody(response);
                if (!response.isSuccessful()) {
                    callback.onError(payload);
                    return;
                }
                try {
                    JSONObject json = new JSONObject(payload);
                    GameConfigState state = new GameConfigState(
                            json.optInt("unlockedDifficulty", 1),
                            json.optInt("coins", 0),
                            json.optBoolean("audioEnabled", true)
                    );
                    callback.onSuccess(state);
                } catch (JSONException e) {
                    callback.onError("Invalid config response");
                }
            }
        });
    }

    public void pushConfig(String token, GameConfigState state, SimpleCallback callback) {
        RequestBody body = new FormBody.Builder()
                .add("token", token)
                .add("unlockedDifficulty", String.valueOf(state.unlockedDifficulty))
                .add("coins", String.valueOf(state.coins))
                .add("audioEnabled", String.valueOf(state.audioEnabled))
                .build();
        Request request = new Request.Builder()
                .url(baseUrl + "/config")
                .post(body)
                .build();
        sendSimple(request, callback, null);
    }

    private void sendSimple(Request request, SimpleCallback callback, String resultField) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String payload = readBody(response);
                if (!response.isSuccessful()) {
                    callback.onError(payload);
                    return;
                }
                if (resultField == null) {
                    callback.onSuccess("ok");
                    return;
                }
                try {
                    JSONObject json = new JSONObject(payload);
                    callback.onSuccess(json.optString(resultField, ""));
                } catch (JSONException e) {
                    callback.onError("Invalid response");
                }
            }
        });
    }

    private String readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            return "";
        }
        return body.string();
    }
}
