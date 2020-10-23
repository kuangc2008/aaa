package co.allconnected.lib.livechat.util;

import com.allconnected.spkv.SpKV;

public class LiveChatSp {
    public static final String SP_NAME = "feedback";
    public static final String UNREAD_CHAT = "unread_chat";
    public static final String UNREAD_REQUEST = "unread_request";
    public static final String UNREAD_ID = "unread_request_id";

    public static final String GO_LIVE_CHAT = "go_live_chat";

    private static SpKV spKV = null;

    public static void saveUnReadRequest(int size) {
        initSp();
        spKV.putInt(UNREAD_REQUEST, size);
    }

    public static int getUnReadRequest() {
        initSp();
        return spKV.getInt(UNREAD_REQUEST, 0);
    }

    public static String getUnReadRequestId() {
        initSp();
        return spKV.getString(UNREAD_ID, "");
    }


    public static void saveUnReadRequestId(String id) {
        initSp();
        spKV.encode(UNREAD_ID, id);
    }


    public static void setUnReadChat(boolean unRead) {
        initSp();
        spKV.putBoolean(UNREAD_CHAT, unRead);
    }

    public static boolean isUnReadChat() {
        initSp();
        return spKV.getBoolean(UNREAD_CHAT, false);
    }


    public static boolean isGoLiveChat() {
        initSp();
        return spKV.getBoolean(GO_LIVE_CHAT, false);
    }

    public static void setGoLiveChat(boolean goLiveChat) {
        initSp();
        spKV.putBoolean(GO_LIVE_CHAT, goLiveChat);
    }


    private static void initSp() {
        if (spKV == null) {
            spKV = SpKV.mmkvWithID(SP_NAME);
        }
    }
}
