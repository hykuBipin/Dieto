package com.bipin.dieto;

import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SwiggyMcpClient {

    private static final String BASE_URL = "https://mcp.swiggy.com/food";
    private final OkHttpClient client = new OkHttpClient();
    private final String accessToken;

    public SwiggyMcpClient(String accessToken) {
        this.accessToken = accessToken;
    }

    public interface McpCallback {
        void onSuccess(JSONObject responseData);
        void onFailure(Exception e);
    }

    /**
     * Calls a Swiggy MCP Tool using JSON-RPC 2.0
     */
    public void callTool(String toolName, JSONObject arguments, McpCallback callback) {
        new Thread(() -> {
            try {
                // Construct standard JSON-RPC 2.0 Payload
                JSONObject jsonRpcRequest = new JSONObject();
                jsonRpcRequest.put("jsonrpc", "2.0");
                jsonRpcRequest.put("method", "tools/call");
                
                JSONObject params = new JSONObject();
                params.put("name", toolName);
                params.put("arguments", arguments != null ? arguments : new JSONObject());
                
                jsonRpcRequest.put("params", params);
                jsonRpcRequest.put("id", 1);

                RequestBody body = RequestBody.create(
                    jsonRpcRequest.toString(),
                    MediaType.get("application/json; charset=utf-8")
                );

                Request.Builder builder = new Request.Builder()
                    .url(BASE_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json");

                if (accessToken != null && !accessToken.trim().isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + accessToken);
                }

                Request request = builder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new Exception("HTTP Error " + response.code() + ": " + response.message());
                    }
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (jsonResponse.has("error")) {
                        throw new Exception(jsonResponse.getJSONObject("error").getString("message"));
                    }
                    
                    // Return result payload
                    callback.onSuccess(jsonResponse.getJSONObject("result"));
                }
            } catch (Exception e) {
                callback.onFailure(e);
            }
        }).start();
    }
}
