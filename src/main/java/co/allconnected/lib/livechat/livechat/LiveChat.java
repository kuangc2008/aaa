package co.allconnected.lib.livechat.livechat;

public class LiveChat {
    public static final int TYPE_FEEDBACK = 0;
    public static final int TYPE_CHAT_FEEDBACK = 1;
    public static final int TYPE_CHAT = 2;
    public static final int TYPE_NO = 3;

    public int vipType = TYPE_CHAT_FEEDBACK;

    public int freeType = TYPE_CHAT_FEEDBACK;

    public String liveChatLabel = null;

    public String feedBackLabel = null;

    public boolean isCanChat(boolean isVip) {
        if (isVip) {
            return vipType == TYPE_CHAT_FEEDBACK || vipType == TYPE_CHAT;
        } else {
            return freeType == TYPE_CHAT_FEEDBACK || freeType == TYPE_CHAT;
        }
    }

    public boolean isCanFeedBack(boolean isVip) {
        if (isVip) {
            return vipType == TYPE_CHAT_FEEDBACK || vipType == TYPE_FEEDBACK;
        } else {
            return freeType == TYPE_CHAT_FEEDBACK || freeType == TYPE_FEEDBACK;
        }
    }
}
