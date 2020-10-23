package co.allconnected.lib.livechat.util;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.zendesk.util.StringUtils;

import co.allconnected.lib.livechat.R;
import co.allconnected.lib.livechat.livechat.LiveChatManager;
import co.allconnected.lib.stat.util.BLog;
import co.allconnected.lib.stat.util.StatUtils;
import zendesk.chat.Chat;
import zendesk.chat.Providers;
import zendesk.chat.PushNotificationsProvider;


public class PushUtils {
    private static final int LIVE_CHAT_NOTIFICATION_ID = 1000123;

    public static final String ACTION_LIVE_CHAT = "free.vpn.unblock.proxy.vpn.master.pro.livechat";

    public static String sNotificationClass = null;


    public static void checkPlayServices(Activity activity) {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int errorCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (errorCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(errorCode)) {
                apiAvailability.makeGooglePlayServicesAvailable(activity);
                BLog.i(LiveChatManager.TAG, "success");
            }
        }
    }

    public static void registerWithZendesk(final String notificationClass) {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        Providers providers = Chat.INSTANCE.providers();
                        if (providers == null) {
                            BLog.i(LiveChatManager.TAG, "Zendesk Support SDK is not initialized");
                            return;
                        }
                        try {
                            PushNotificationsProvider pushProvider = providers.pushNotificationsProvider();
                            if (pushProvider == null) {
                                BLog.i(LiveChatManager.TAG, "Zendesk Support SDK is not initialized");
                                return;
                            }

                            sNotificationClass = notificationClass;

                            InstanceIdResult result = task.getResult();
                            if (result == null)
                                return;
                            final String pushToken = result.getToken();
                            if (StringUtils.hasLength(pushToken)) {
                                BLog.i(LiveChatManager.TAG, pushToken);
                                pushProvider.registerPushToken(pushToken, null);
                            }
                        } catch (Exception e) {
                            StatUtils.recordException(e);
                        }
                    }
                });
    }


    public static void deliveryChatNotification(Context context, String author, String message, long timestamp, String title, int notification) {
//        Engine answerBotEngine = AnswerBotEngine.engine();
//        Engine supportEngine = SupportEngine.engine();
//        Engine chatEngine = ChatEngine.engine();
//        Intent intent = MessagingActivity.builder()
//                .withToolbarTitle(title)
//                .withEngines(answerBotEngine, supportEngine, chatEngine)
//                .intent(context);

        if (sNotificationClass == null) {
            return;
        }

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context.getPackageName(), sNotificationClass));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0 /* Request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = context.getString(R.string.live_chat_notification_channel_id);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setStyle(new NotificationCompat.MessagingStyle(author)
                                .setConversationTitle(title)
                                .addMessage(message, timestamp, author)) // Pass in null for user.

                        .setSmallIcon(notification)
                        .setContentTitle(author)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    context.getString(R.string.live_chat_notification_channel_id),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(LIVE_CHAT_NOTIFICATION_ID, notificationBuilder.build());
    }

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(LIVE_CHAT_NOTIFICATION_ID);
    }

    public static void sendLiveMessageBroadcast(Context context, boolean haveMessage, boolean includeLiveActivity) {
        if (includeLiveActivity || LiveChatManager.messageActivityCount <= 0) {
            LiveChatSp.setUnReadChat(haveMessage);
            Intent intent = new Intent(LiveChatManager.ACTION_CHAT);
            intent.putExtra(LiveChatManager.KEY_PUSH_MESSAGE, haveMessage);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }
}
