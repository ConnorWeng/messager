package com.zwd51.messager;

import com.taobao.api.internal.tmc.Message;
import com.taobao.api.internal.tmc.MessageHandler;
import com.taobao.api.internal.tmc.MessageStatus;
import com.taobao.api.internal.tmc.TmcClient;
import com.taobao.top.link.LinkException;

import javax.servlet.http.HttpServlet;

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

    public void handleAdd(String content) {
        System.out.println("handle add");
    }

    public void handleDelete(String content) {
        System.out.println("handle delete");
    }

    public void handleUpdate(String content) {
        System.out.println("handle update");
    }
}
