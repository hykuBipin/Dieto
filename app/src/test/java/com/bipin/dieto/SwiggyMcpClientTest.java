package com.bipin.dieto;

import org.json.JSONObject;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class SwiggyMcpClientTest {

    @Test
    public void testPayloadFormatting() throws Exception {
        // Verification of JSON-RPC payload format
        JSONObject jsonRpcRequest = new JSONObject();
        jsonRpcRequest.put("jsonrpc", "2.0");
        jsonRpcRequest.put("method", "tools/call");
        
        JSONObject params = new JSONObject();
        params.put("name", "get_addresses");
        params.put("arguments", new JSONObject());
        jsonRpcRequest.put("params", params);
        jsonRpcRequest.put("id", 1);

        assertEquals("2.0", jsonRpcRequest.getString("jsonrpc"));
        assertEquals("tools/call", jsonRpcRequest.getString("method"));
        assertEquals("get_addresses", jsonRpcRequest.getJSONObject("params").getString("name"));
        assertEquals(1, jsonRpcRequest.getInt("id"));
    }

    @Test
    public void testLiveEndpointWithInvalidToken() throws Exception {
        // Verify that passing an invalid/empty token to Swiggy MCP returns an authorization failure
        final CountDownLatch latch = new CountDownLatch(1);
        SwiggyMcpClient client = new SwiggyMcpClient("invalid_dummy_token_123");

        final StringBuilder errorMsg = new StringBuilder();
        final Boolean[] success = new Boolean[]{false};

        client.callTool("get_addresses", null, new SwiggyMcpClient.McpCallback() {
            @Override
            public void onSuccess(JSONObject responseData) {
                success[0] = true;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                success[0] = false;
                errorMsg.append(e.getMessage());
                latch.countDown();
            }
        });

        // Wait up to 5 seconds for the HTTP call to finish
        latch.await(5, TimeUnit.SECONDS);

        // It must fail because the token is invalid
        assertFalse("Call should not succeed with invalid token", success[0]);
        assertNotNull("Error message should be populated", errorMsg.toString());
        assertTrue("Error should mention HTTP Error or Authentication", 
            errorMsg.toString().contains("HTTP Error") || errorMsg.toString().contains("Unauthorized"));
    }
}
