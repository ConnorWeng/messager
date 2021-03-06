package com.zwd51.messager;

import com.taobao.api.internal.tmc.Message;
import com.taobao.api.internal.tmc.MessageHandler;
import com.taobao.api.internal.tmc.MessageStatus;
import com.taobao.api.internal.tmc.TmcClient;
import com.taobao.top.link.LinkException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Connor on 6/14/15.
 */
public class MessageDispatcher extends HttpServlet {

    private static Log logger = LogFactory.getLog(MessageDispatcher.class);

    private ArrayList<String> changeNumIids = new ArrayList<String>();

    public MessageDispatcher() throws LinkException {
        super();
        logger.info("i am message dispatcher");
        TmcClient client = new TmcClient(System.getenv("APP_KEY"), System.getenv("APP_SECRET"), "default");
        client.setMessageHandler(new MessageHandler() {
            public void onMessage(Message message, MessageStatus messageStatus) {
                try {
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
                            logger.error("message not handled");
                            break;
                    }
                } catch (Exception e) {
                    messageStatus.fail();
                    logger.error("onMessage exception " + message.getContent(), e);
                }
            }
        });
        client.connect();
        logger.info("tmc client connected");
    }

    public void handleAdd(String content) throws IOException {
        handle(content, "add");
    }

    public void handleDelete(String content) throws IOException {
        handle(content, "delete");
    }

    public void handleUpdate(String content) throws IOException {
        JSONObject jsonObject = new JSONObject(content);
        String numIid = String.valueOf(jsonObject.get("num_iid"));
        String changedFields = jsonObject.getString("changed_fields");
        if (changedFields.contains("price") ||
                changedFields.contains("title") ||
                changedFields.contains("desc") ||
                changedFields.contains("sku") ||
                changedFields.contains("item_img") ||
                changedFields.contains("prop_img")) {
            boolean doIt = false;
            synchronized (this) {
                if (!changeNumIids.contains(numIid)) {
                    changeNumIids.add(numIid);
                    doIt = true;
                }
            }
            if (doIt) {
                handle(content, "change");
            }
        }
    }

    private void handle(String content, String action) throws IOException {
        JSONObject jsonObject = new JSONObject(content);
        String numIid = String.valueOf(jsonObject.get("num_iid"));
        String nick = "";
        String url = System.getenv("API_URL") + "/" + action + "?numIid=" + numIid;
        if (action == "add") {
            if (!jsonObject.has("nick")) {
                logger.error("add action lack nick " + content);
                return;
            }
            if (!jsonObject.has("title")) {
                logger.error("add action lack title " + content);
                return;
            }
            if (!jsonObject.has("price")) {
                logger.error("add action lack price " + content);
                return;
            }
            nick = jsonObject.getString("nick");
            String title = jsonObject.getString("title");
            String price = jsonObject.getString("price");
            url += "&nick=" + URLEncoder.encode(nick, "UTF-8") + "&price=" + URLEncoder.encode(price, "UTF-8") + "&title=" + URLEncoder.encode(title, "UTF-8");
        }
        HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        String resp = reader.readLine();
        reader.close();
        connection.disconnect();
        if (resp.contains("ok")) {
            logger.info(action + " success: " + content + " resp:" + resp);
        } else {
            logger.error(action + " fail: " + content + " resp:" + resp);
        }
    }
}
