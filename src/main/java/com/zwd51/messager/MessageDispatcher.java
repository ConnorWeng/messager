package com.zwd51.messager;

import com.taobao.api.internal.tmc.Message;
import com.taobao.api.internal.tmc.MessageHandler;
import com.taobao.api.internal.tmc.MessageStatus;
import com.taobao.api.internal.tmc.TmcClient;
import com.taobao.top.link.LinkException;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Connor on 6/14/15.
 */
public class MessageDispatcher extends HttpServlet {

    public MessageDispatcher() throws LinkException {
        super();
        System.out.println("i am message dispatcher");
        TmcClient client = new TmcClient(System.getenv("APP_KEY"), System.getenv("APP_SECRET"), "default");
        client.setMessageHandler(new MessageHandler() {
            public void onMessage(Message message, MessageStatus messageStatus) {
                try {
                    System.out.println(message.getTopic());
                    System.out.println(message.getContent());
                    switch (message.getTopic()) {
                        case "taobao_item_ItemAdd":
                        case "taobao_item_ItemUpshelf":
                            handleAdd(message.getContent());
                            break;
                        case "taobao_item_ItemDownshelf":
                        case "taobao_item_ItemDelete":
                            handleDelete(message.getContent());
                            break;
                        case "taobao_item_ItemUpdate":
                            handleUpdate(message.getContent());
                            break;
                        default:
                            System.out.println("not handled");
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    messageStatus.fail();
                }
            }
        });
        client.connect();
        System.out.println("connected");
    }

    public void handleAdd(String content) throws IOException {
        System.out.println("handle add");
        handle(content, "add");
    }

    public void handleDelete(String content) throws IOException {
        System.out.println("handle delete");
        handle(content, "delete");
    }

    public void handleUpdate(String content) throws IOException {
        System.out.println("handle update");
        JSONObject jsonObject = new JSONObject(content);
        String changedFields = jsonObject.getString("changed_fields");
        if (changedFields.contains("price") ||
                changedFields.contains("title") ||
                changedFields.contains("desc") ||
                changedFields.contains("sku") ||
                changedFields.contains("item_img") ||
                changedFields.contains("prop_img")) {
            handle(content, "change");
        }
    }

    private void handle(String content, String action) throws IOException {
        JSONObject jsonObject = new JSONObject(content);
        String numIid = String.valueOf(jsonObject.get("num_iid"));
        HttpURLConnection connection = (HttpURLConnection)new URL("http://121.41.170.236:30005/"+action+"?numIid=" + numIid).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        System.out.println(reader.readLine());
        reader.close();
        connection.disconnect();
    }
}
