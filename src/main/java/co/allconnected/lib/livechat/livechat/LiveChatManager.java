package co.allconnected.lib.livechat.livechat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import co.allconnected.lib.livechat.util.LiveChatSp;
import co.allconnected.lib.livechat.util.PushUtils;
import co.allconnected.lib.livechat.util.Util;
import co.allconnected.lib.stat.BuildConfig;
import co.allconnected.lib.stat.StatAgent;
import co.allconnected.lib.stat.config.FirebaseConfigManager;
import co.allconnected.lib.stat.util.BLog;
import co.allconnected.lib.stat.util.StatUtils;
import zendesk.answerbot.AnswerBot;
import zendesk.answerbot.AnswerBotEngine;
import zendesk.chat.Chat;
import zendesk.chat.ChatConfiguration;
import zendesk.chat.ChatEngine;
import zendesk.chat.PreChatFormFieldStatus;
import zendesk.chat.ProfileProvider;
import zendesk.chat.VisitorInfo;
import zendesk.core.AnonymousIdentity;
import zendesk.core.Zendesk;
import zendesk.messaging.Engine;
import zendesk.messaging.MessagingActivity;
import zendesk.support.ProviderStore;
import zendesk.support.Request;
import zendesk.support.RequestProvider;
import zendesk.support.RequestUpdates;
import zendesk.support.Support;
import zendesk.support.SupportEngine;
import zendesk.support.request.RequestActivity;
import zendesk.support.requestlist.RequestListActivity;

public class LiveChatManager {

    public static final String TAG = "liveChat";
    private static boolean isSdkInit = false;
    private static LiveChat liveChat;
    public static int messageActivityCount = 0;

    public static final String ACTION_CHAT = "action_chat";
    public static final String KEY_PUSH_MESSAGE = "key_push_message";

    public static boolean isMessagingPaused = false;

    public static boolean init(Context context, boolean isVip, String userName, String notificationClass) {
        initConfig();
        if ((liveChat == null || liveChat.isCanChat(isVip))) {
            initSdk(context, userName, notificationClass);
            return true;
        }
        return false;
    }

    private static void initConfig() {
        String fileName = BLog.logEnable(Log.DEBUG) ? "debug_feedback_entry" : "feedback_entry";
        JSONObject feedback = FirebaseConfigManager.getFirebaseConfigs(fileName);
        if (BuildConfig.DEBUG) {
            feedback = null;
        }
        if (feedback != null) {
            liveChat = new LiveChat();
            liveChat.vipType = feedback.optInt("vip_type", LiveChat.TYPE_CHAT_FEEDBACK);
            liveChat.freeType = feedback.optInt("free_type", LiveChat.TYPE_CHAT_FEEDBACK);
            liveChat.liveChatLabel = feedback.optString("live_chat_label");
            liveChat.feedBackLabel = feedback.optString("feed_back_label");
        }
    }

    public static boolean isCanChat(boolean isVip) {
        return (liveChat == null || liveChat.isCanChat(isVip));
    }


    public static boolean isCanFeedBack(boolean isVip) {
        return (liveChat == null) || (liveChat.isCanFeedBack(isVip));
    }

    public static void getUnReadRequest(final Runnable haveUpdateRunnable) {
        final ProviderStore provider = Support.INSTANCE.provider();
        if (provider != null) {
            final RequestProvider requestProvider =provider.requestProvider();
            if (requestProvider == null) {
                return;
            }
            requestProvider.getUpdatesForDevice(new ZendeskCallback<RequestUpdates>() {
                @Override
                public void onSuccess(RequestUpdates requestUpdates) {
                    BLog.i(LiveChatManager.TAG, "requestUpdate:" + requestUpdates.getRequestUpdates().size());
                    if (requestUpdates.hasUpdatedRequests()) {
                        Set<String> unReadIds = requestUpdates.getRequestUpdates().keySet();

                        String oneKey = null;
                        if (unReadIds.size() == 1) {
                            for (String key : unReadIds) {
                                oneKey = key;
                                BLog.i(TAG, "request id:" + key + "  value:" + requestUpdates.getRequestUpdates().get(key));
                            }
                        }
                        if (oneKey != null && isMessagingPaused && LiveChatSp.getUnReadRequest() == 0 && unReadIds.size() == 1) {
                            requestProvider.markRequestAsRead(oneKey, 1);
                            if (haveUpdateRunnable != null) {
                                haveUpdateRunnable.run();
                            }
                            isMessagingPaused = false;
                            return;
                        }

                        if (oneKey != null) {
                            LiveChatSp.saveUnReadRequestId(oneKey);
                        }
                        LiveChatSp.saveUnReadRequest( requestUpdates.getRequestUpdates().size() );
                    } else {
                        LiveChatSp.saveUnReadRequest( 0 );
                        LiveChatSp.saveUnReadRequestId("");
                    }
                    if (haveUpdateRunnable != null) {
                        haveUpdateRunnable.run();
                    }
                    isMessagingPaused = false;

                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    LiveChatSp.saveUnReadRequest( 0 );
                    LiveChatSp.saveUnReadRequestId("");

                    if (haveUpdateRunnable != null) {
                        haveUpdateRunnable.run();
                    }
                }
            });
        }
    }

    public static boolean isSdkInit() {
        return isSdkInit;
    }

    public static void initSdk(Context context, String userName, String notificationClass) {
        if (!isSdkInit) {
            BLog.i(LiveChatManager.TAG, "init sdk start");
            Zendesk.INSTANCE.init(context, "https://inconnecting.zendesk.com",
                    "efeeafc761394f64383852234b9f7637e5884b2ee029502f",
                    "mobile_sdk_client_12aaea3d214a5a372988");
            Support.INSTANCE.init(Zendesk.INSTANCE);
            AnswerBot.INSTANCE.init(Zendesk.INSTANCE, Support.INSTANCE);

            Chat.INSTANCE.init(context, "AF3ACT4bUFWOQpGwNpGiWsidafW6FoNy", "224054056782888961");

            Zendesk.INSTANCE.setIdentity(new AnonymousIdentity.Builder()
                    .withNameIdentifier(userName)
                    .build());

            PushUtils.sNotificationClass = notificationClass;
            PushUtils.registerWithZendesk(notificationClass);


            isSdkInit = true;
            BLog.i(LiveChatManager.TAG, "init sdk end");

            ((Application) (context.getApplicationContext())).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                private boolean enterLiveChat = false;
                private boolean enterMessageList = false;
                private WeakReference<Activity> currentActivity = null;

                @Override
                public void onActivityCreated(@NonNull final  Activity activity, @Nullable Bundle savedInstanceState) {
                    if (activity.getComponentName().getClassName().equals("zendesk.support.request.RequestActivity")) {
                        StatAgent.onEvent(activity, "user_livechat_mess_show", "source", enterMessageList ? "user_click" : "auto");
                    } else if (activity.getComponentName().getClassName().equals("zendesk.support.requestlist.RequestListActivity")) {
                        final ProviderStore provider = Support.INSTANCE.provider();
                        if (provider != null) {
                            final RequestProvider requestProvider = provider.requestProvider();
                            if (requestProvider == null) {
                                return;
                            }
                            requestProvider.getAllRequests(new ZendeskCallback<List<Request>>() {
                                @Override
                                public void onSuccess(List<Request> requests) {
                                    int len = requests != null ? requests.size() : 0;
                                    StatAgent.onEvent(activity, "user_livechat_messlist_show", "result", String.valueOf(len));
                                }

                                @Override
                                public void onError(ErrorResponse errorResponse) {

                                }
                            });
                        }
                    }

                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                    BLog.i(LiveChatManager.TAG,  activity.getComponentName().getClassName());
                    if (activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")) {
                        messageActivityCount = messageActivityCount + 1;
                    }

                    // 键盘问题
                    currentActivity = new WeakReference<>(activity);
                    if (activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")
                            || activity.getComponentName().getClassName().equals("zendesk.support.request.RequestActivity")
                            || activity.getComponentName().getClassName().equals("zendesk.support.requestlist.RequestListActivity")) {
                        enterLiveChat = true;
                    } else if (enterLiveChat) {
                        if (currentActivity.get().getWindow() != null) {
                            currentActivity.get().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                        }
                        enterLiveChat = false;
                    }

                    if (activity.getComponentName().getClassName().equals("zendesk.support.requestlist.RequestListActivity")
                            || activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")) {
                        enterMessageList = true;
                    } else {
                        enterMessageList = false;
                    }

                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    if (activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")) {
                        PushUtils.sendLiveMessageBroadcast(activity, false, true);
                    }
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                    if (activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")) {
                        isMessagingPaused = true;
                    }

                    // 键盘问题
                    if (activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")
                            || activity.getComponentName().getClassName().equals("zendesk.support.request.RequestActivity")
                            || activity.getComponentName().getClassName().equals("zendesk.support.requestlist.RequestListActivity")) {
                        if (activity.isFinishing()) {
                            Util.hideKeyboard(activity);
                            View view = activity.getCurrentFocus();
                            if (view != null) {
                                ViewParent parent = view.getParent();
                                if (parent instanceof View) {
                                    ((View) parent).requestFocus();
                                }
                            }
                        }
                    }
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                    if (activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")) {
                        messageActivityCount = messageActivityCount - 1;

                        if (LiveChatSp.getUnReadRequest() == 0) {
                            ProviderStore provider = Support.INSTANCE.provider();
                            if (provider != null) {
                                final RequestProvider requestProvider = provider.requestProvider();
                                if (requestProvider == null) {
                                    return;
                                }
                                requestProvider.getUpdatesForDevice(new ZendeskCallback<RequestUpdates>() {
                                    @Override
                                    public void onSuccess(RequestUpdates requestUpdates) {
                                        BLog.i(LiveChatManager.TAG, "requestUpdate:" + requestUpdates.getRequestUpdates().size());
                                        if (requestUpdates.hasUpdatedRequests()) {
                                            Set<String> unReadIds = requestUpdates.getRequestUpdates().keySet();
                                            if (unReadIds.size() == 1) {
                                                for (String key : unReadIds) {
                                                    BLog.i(TAG, "livechat as read");
                                                    requestProvider.markRequestAsRead(key, 1);
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onError(ErrorResponse errorResponse) {
                                    }
                                });
                            }
                        }
                    }


                    // 键盘问题
                    if (activity.getComponentName().getClassName().equals("zendesk.messaging.MessagingActivity")
                            || activity.getComponentName().getClassName().equals("zendesk.support.request.RequestActivity")
                            || activity.getComponentName().getClassName().equals("zendesk.support.requestlist.RequestListActivity")) {
                        if (currentActivity != null  && currentActivity.get() != null) {
                            if (currentActivity.get().getWindow() != null) {
                                currentActivity.get().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                            }
                        }
                        Util.hideKeyboard(activity);
                        View view = activity.getCurrentFocus();
                        if (view != null) {
                            ViewParent parent = view.getParent();
                            if (parent instanceof View) {
                                ((View) parent).requestFocus();
                            }
                        }
                    }

                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {
                }
            });
        }
    }

    public static LiveChat getLiveChat() {
        return liveChat;
    }

    public static void startLiveChat(Activity activity, String title, String chatNode, String email) {
        PushUtils.cancelNotification(activity);
        if (LiveChatSp.isUnReadChat()) {
            startChat(activity, title, chatNode, email);
        } else {
            int size = LiveChatSp.getUnReadRequest();
            String unReadId = LiveChatSp.getUnReadRequestId();
            if (size == 1 && !TextUtils.isEmpty(unReadId)) {
                RequestActivity.builder()
                        .withRequestId(unReadId)
                        .show(activity);
            }  else if (size >  1) {
                RequestListActivity.builder().show(activity);
                StatAgent.onEvent(activity, "user_livechat_show", "page", "mess_list");
            } else {
                startChat(activity, title, chatNode, email);
                StatAgent.onEvent(activity, "user_livechat_show", "page", "mess");
            }
        }
    }

    private static void startChat(Activity activity, String title, String chatNode, String email) {
        try {
            LiveChatSp.setUnReadChat(false);

            Engine answerBotEngine = AnswerBotEngine.engine();
            Engine supportEngine = null;
            try {
                supportEngine = SupportEngine.engine();
            } catch (Exception e) {
            }
            Engine chatEngine = ChatEngine.engine();

            StatAgent.onEvent(activity, "user_livechat_show", "page", "chat");

            ChatConfiguration chatConfiguration = ChatConfiguration.builder()
                    .withNameFieldStatus(PreChatFormFieldStatus.HIDDEN)
                    .withEmailFieldStatus(PreChatFormFieldStatus.HIDDEN)
                    .withPhoneFieldStatus(PreChatFormFieldStatus.HIDDEN)
                    .build();

            if (Chat.INSTANCE.providers() != null) {
                ProfileProvider profileProvider = Chat.INSTANCE.providers().profileProvider();
                if (!TextUtils.isEmpty(email)) {
                    VisitorInfo visitorInfo = VisitorInfo.builder()
                            .withEmail(email)
                            .build();
                    profileProvider.setVisitorInfo(visitorInfo, null);
                    profileProvider.setVisitorNote(chatNode);
                }
            }
            Engine[] engines = supportEngine == null ? new Engine[]{answerBotEngine, chatEngine} : new Engine[]{answerBotEngine, supportEngine, chatEngine};
            MessagingActivity.builder()
                    .withToolbarTitle(title)
                    .withEngines(engines)
                    .show(activity, chatConfiguration);
        } catch (Exception e) {
            StatUtils.recordException(e);
        }
    }
}
