package com.ulesson.chat.groupchannel;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;
import com.ulesson.chat.R;
import com.ulesson.chat.main.SyncManagerUtils;
import com.ulesson.chat.utils.DateUtils;
import com.ulesson.chat.utils.ImageUtils;
import com.ulesson.chat.utils.PreferenceUtils;
import com.ulesson.chat.widget.MessageStatusView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

class GroupChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String URL_PREVIEW_CUSTOM_TYPE = "url_preview";

    private static final int VIEW_TYPE_USER_MESSAGE_ME = 10;
    private static final int VIEW_TYPE_USER_MESSAGE_OTHER = 11;
    private static final int VIEW_TYPE_FILE_MESSAGE_ME = 20;
    private static final int VIEW_TYPE_FILE_MESSAGE_OTHER = 21;
    private static final int VIEW_TYPE_FILE_MESSAGE_IMAGE_ME = 22;
    private static final int VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER = 23;
    private static final int VIEW_TYPE_FILE_MESSAGE_VIDEO_ME = 24;
    private static final int VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER = 25;
    private static final int VIEW_TYPE_ADMIN_MESSAGE = 30;
    private final List<BaseMessage> mFailedMessageList;
    private final List<BaseMessage> mMessageList;
    private final Set<String> mResendingMessageSet;
    private final Hashtable<String, Uri> mTempFileMessageUriTable = new Hashtable<>();
    private Context mContext;
    private GroupChannel mChannel;
    private OnItemClickListener mItemClickListener;
    private OnItemLongClickListener mItemLongClickListener;
    private boolean mIsMessageListLoading;

    GroupChatAdapter(Context context) {
        mContext = context;
        mMessageList = new ArrayList<>();
        mFailedMessageList = new ArrayList<>();
        mResendingMessageSet = new HashSet<>();
    }

    void setContext(Context context) {
        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                View myUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_user_me, parent, false);
                return new MyUserMessageHolder(myUserMsgView);
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                View otherUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_user_other, parent, false);
                return new OtherUserMessageHolder(otherUserMsgView);
            case VIEW_TYPE_ADMIN_MESSAGE:
                View adminMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_admin, parent, false);
                return new AdminMessageHolder(adminMsgView);
            case VIEW_TYPE_FILE_MESSAGE_ME:
                View myFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_me, parent, false);
                return new MyFileMessageHolder(myFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_OTHER:
                View otherFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_other, parent, false);
                return new OtherFileMessageHolder(otherFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_ME:
                View myImageFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_image_me, parent, false);
                return new MyImageFileMessageHolder(myImageFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER:
                View otherImageFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_image_other, parent, false);
                return new OtherImageFileMessageHolder(otherImageFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_ME:
                View myVideoFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_video_me, parent, false);
                return new MyVideoFileMessageHolder(myVideoFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER:
                View otherVideoFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_video_other, parent, false);
                return new OtherVideoFileMessageHolder(otherVideoFileMsgView);

            default:
                return null;

        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BaseMessage message = getMessage(position);
        boolean isContinuous = false;
        boolean isNewDay = false;
        boolean isTempMessage = false;
        Uri tempFileMessageUri = null;

        // If there is at least one item preceding the current one, check the previous message.
        if (position < mMessageList.size() + mFailedMessageList.size() - 1) {
            BaseMessage prevMessage = getMessage(position + 1);

            // If the date of the previous message is different, display the date before the message,
            // and also set isContinuous to false to show information such as the sender's nickname
            // and profile image.
            if (!DateUtils.hasSameDate(message.getCreatedAt(), prevMessage.getCreatedAt())) {
                isNewDay = true;
                isContinuous = false;
            } else {
                isContinuous = isContinuous(message, prevMessage);
            }
        } else if (position == mFailedMessageList.size() + mMessageList.size() - 1) {
            isNewDay = true;
        }

        isTempMessage = isTempMessage(message);
        tempFileMessageUri = getTempFileMessageUri(message);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                ((MyUserMessageHolder) holder).bind(mContext, (UserMessage) message, mChannel, isContinuous, isNewDay, mItemClickListener, mItemLongClickListener, position);
                break;
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                ((OtherUserMessageHolder) holder).bind(mContext, (UserMessage) message, mChannel, isNewDay, isContinuous, mItemClickListener, mItemLongClickListener, position);
                break;
            case VIEW_TYPE_ADMIN_MESSAGE:
                ((AdminMessageHolder) holder).bind(mContext, (AdminMessage) message, mChannel, isNewDay);
                break;
            case VIEW_TYPE_FILE_MESSAGE_ME:
                ((MyFileMessageHolder) holder).bind(mContext, (FileMessage) message, mChannel, isNewDay, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_OTHER:
                ((OtherFileMessageHolder) holder).bind(mContext, (FileMessage) message, mChannel, isNewDay, isContinuous, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_ME:
                ((MyImageFileMessageHolder) holder).bind(mContext, (FileMessage) message, mChannel, isNewDay, isTempMessage, tempFileMessageUri, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER:
                ((OtherImageFileMessageHolder) holder).bind(mContext, (FileMessage) message, mChannel, isNewDay, isContinuous, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_ME:
                ((MyVideoFileMessageHolder) holder).bind(mContext, (FileMessage) message, mChannel, isNewDay, isTempMessage, tempFileMessageUri, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER:
                ((OtherVideoFileMessageHolder) holder).bind(mContext, (FileMessage) message, mChannel, isNewDay, isContinuous, mItemClickListener);
                break;
            default:
                break;
        }
    }


    @Override
    public int getItemViewType(int position) {

        BaseMessage message = getMessage(position);

        if (message instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) message;
            // If the sender is current user
            if (userMessage.getSender() != null && userMessage.getSender().getUserId() != null) {
                if (userMessage.getSender().getUserId().equals(PreferenceUtils.getUserId())) {
                    return VIEW_TYPE_USER_MESSAGE_ME;
                } else {
                    return VIEW_TYPE_USER_MESSAGE_OTHER;
                }
            }

        } else if (message instanceof FileMessage) {
            FileMessage fileMessage = (FileMessage) message;

            if (fileMessage.getSender() != null && fileMessage.getSender().getUserId() != null) {

                if (fileMessage.getType().toLowerCase().startsWith("image")) {
                    // If the sender is current user
                    if (fileMessage.getSender().getUserId().equals(PreferenceUtils.getUserId())) {
                        return VIEW_TYPE_FILE_MESSAGE_IMAGE_ME;
                    } else {
                        return VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER;
                    }

                } else if (fileMessage.getType().toLowerCase().startsWith("video")) {
                    if (fileMessage.getSender().getUserId().equals(PreferenceUtils.getUserId())) {
                        return VIEW_TYPE_FILE_MESSAGE_VIDEO_ME;
                    } else {
                        return VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER;
                    }
                } else {
                    if (fileMessage.getSender().getUserId().equals(PreferenceUtils.getUserId())) {
                        return VIEW_TYPE_FILE_MESSAGE_ME;
                    } else {
                        return VIEW_TYPE_FILE_MESSAGE_OTHER;
                    }
                }
            }

        } else if (message instanceof AdminMessage) {
            return VIEW_TYPE_ADMIN_MESSAGE;
        }

        return VIEW_TYPE_USER_MESSAGE_ME;
    }

    @Override
    public int getItemCount() {
        return mMessageList.size() + mFailedMessageList.size();
    }

    private BaseMessage getMessage(int position) {
        if (position < mFailedMessageList.size()) {
            return mFailedMessageList.get(position);
        } else if (position < mFailedMessageList.size() + mMessageList.size()) {
            return mMessageList.get(position - mFailedMessageList.size());
        } else {
            return null;
        }
    }

    void setChannel(GroupChannel channel) {
        mChannel = channel;
    }

    public boolean isTempMessage(BaseMessage message) {
        return message.getMessageId() == 0;
    }

    public boolean isFailedMessage(BaseMessage message) {
        if (!isTempMessage(message)) {
            return false;
        }

        return mFailedMessageList.contains(message);
    }

    public boolean isResendingMessage(BaseMessage message) {
        if (message == null) {
            return false;
        }

        return mResendingMessageSet.contains(getRequestId(message));
    }

    private String getRequestId(BaseMessage message) {
        if (message instanceof UserMessage) {
            return message.getRequestId();
        } else if (message instanceof FileMessage) {
            return message.getRequestId();
        }

        return "";
    }

    public Uri getTempFileMessageUri(BaseMessage message) {
        if (!isTempMessage(message)) {
            return null;
        }

        if (!(message instanceof FileMessage)) {
            return null;
        }

        return mTempFileMessageUriTable.get(message.getRequestId());
    }

    public void markMessageFailed(BaseMessage message) {
        BaseMessage msg;
        for (int i = mMessageList.size() - 1; i >= 0; i--) {
            msg = mMessageList.get(i);
            if (msg.getRequestId().equals(message.getRequestId())) {
                mMessageList.set(i, message);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void removeFailedMessage(BaseMessage message) {
        mTempFileMessageUriTable.remove(message.getRequestId());
        mMessageList.remove(message);
        notifyDataSetChanged();
    }

    public void markMessageSent(BaseMessage message) {
        BaseMessage msg;
        for (int i = mMessageList.size() - 1; i >= 0; i--) {
            msg = mMessageList.get(i);
            if (message instanceof UserMessage && msg instanceof UserMessage) {
                if (msg.getRequestId().equals((message).getRequestId())) {
                    mMessageList.set(i, message);
                    notifyDataSetChanged();
                    return;
                }
            } else if (message instanceof FileMessage && msg instanceof FileMessage) {
                if (msg.getRequestId().equals(message.getRequestId())) {
                    mTempFileMessageUriTable.remove(message.getRequestId());
                    mMessageList.set(i, message);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    void addTempFileMessageInfo(FileMessage message, Uri uri) {
        mTempFileMessageUriTable.put(message.getRequestId(), uri);
    }

    void insertSucceededMessages(List<BaseMessage> messages) {
        for (BaseMessage message : messages) {
            int index = SyncManagerUtils.findIndexOfMessage(mMessageList, message);
            mMessageList.add(index, message);
        }
        notifyItemInserted(getItemCount() - 1);
    }

    void updateSucceededMessages(List<BaseMessage> messages) {
        for (BaseMessage message : messages) {
            int index = SyncManagerUtils.getIndexOfMessage(mMessageList, message);
            if (index != -1) {
                mMessageList.set(index, message);
                notifyItemChanged(index);
            }
        }
    }

    public void insertFailedMessages(List<BaseMessage> messages) {
        synchronized (mFailedMessageList) {
            for (BaseMessage message : messages) {
                String requestId = getRequestId(message);
                if (requestId.isEmpty()) {
                    continue;
                }

                mResendingMessageSet.add(requestId);
                mFailedMessageList.add(message);
            }

            Collections.sort(mFailedMessageList, new Comparator<BaseMessage>() {
                @Override
                public int compare(BaseMessage m1, BaseMessage m2) {
                    long x = m1.getCreatedAt();
                    long y = m2.getCreatedAt();

                    return (x < y) ? 1 : ((x == y) ? 0 : -1);
                }
            });
        }

        notifyDataSetChanged();
    }

    void updateFailedMessages(List<BaseMessage> messages) {
        synchronized (mFailedMessageList) {
            for (BaseMessage message : messages) {
                String requestId = getRequestId(message);
                if (requestId.isEmpty()) {
                    continue;
                }

                mResendingMessageSet.remove(requestId);
            }
        }

        notifyDataSetChanged();
    }

    void removeFailedMessages(List<BaseMessage> messages) {
        synchronized (mFailedMessageList) {
            for (BaseMessage message : messages) {
                String requestId = getRequestId(message);
                mResendingMessageSet.remove(requestId);
                mFailedMessageList.remove(message);
            }
        }

        notifyDataSetChanged();
    }

    public int getLastReadPosition(long lastRead) {
        for (int i = 0; i < mMessageList.size(); i++) {
            if (mMessageList.get(i).getCreatedAt() == lastRead) {
                return i + mFailedMessageList.size();
            }
        }

        return 0;
    }

    boolean failedMessageListContains(BaseMessage message) {
        if (mFailedMessageList.isEmpty()) {
            return false;
        }
        for (BaseMessage failedMessage : mFailedMessageList) {
            if (message instanceof UserMessage && failedMessage instanceof UserMessage) {
                if (message.getRequestId().equals(failedMessage.getRequestId())) {
                    return true;
                }
            } else if (message instanceof FileMessage && failedMessage instanceof FileMessage) {
                if (message.getRequestId().equals(failedMessage.getRequestId())) {
                    return true;
                }
            }
        }
        return false;
    }

    void clear() {
        mMessageList.clear();
        notifyDataSetChanged();
    }

    void removeSucceededMessages(List<BaseMessage> messages) {
        for (BaseMessage message : messages) {
            int index = SyncManagerUtils.getIndexOfMessage(mMessageList, message);
            if (index != -1) {
                mMessageList.remove(index);
            }
        }

        notifyDataSetChanged();
    }

    void delete(long msgId) {
        for (BaseMessage msg : mMessageList) {
            if (msg.getMessageId() == msgId) {
                mMessageList.remove(msg);
                notifyDataSetChanged();
                break;
            }
        }
    }

    void update(BaseMessage message) {
        BaseMessage baseMessage;
        for (int index = 0; index < mMessageList.size(); index++) {
            baseMessage = mMessageList.get(index);
            if (message.getMessageId() == baseMessage.getMessageId()) {
                mMessageList.remove(index);
                mMessageList.add(index, message);
                notifyDataSetChanged();
                break;
            }
        }
    }

    private synchronized boolean isMessageListLoading() {
        return mIsMessageListLoading;
    }

    private synchronized void setMessageListLoading(boolean tf) {
        mIsMessageListLoading = tf;
    }

    public void markAllMessagesAsRead() {
        if (mChannel != null) {
            mChannel.markAsRead();
        }
        notifyDataSetChanged();
    }

    /**
     * Load old message list.
     *
     * @param limit
     * @param handler
     */
    public void loadPreviousMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if (mChannel == null) {
            return;
        }

        if (isMessageListLoading()) {
            return;
        }

        long oldestMessageCreatedAt = Long.MAX_VALUE;
        if (mMessageList.size() > 0) {
            oldestMessageCreatedAt = mMessageList.get(mMessageList.size() - 1).getCreatedAt();
        }

        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(oldestMessageCreatedAt, false, limit, true, BaseChannel.MessageTypeFilter.ALL, null, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if (handler != null) {
                    handler.onResult(list, e);
                }

                setMessageListLoading(false);
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

                for (BaseMessage message : list) {
                    mMessageList.add(message);
                }

                notifyDataSetChanged();
            }
        });
    }

    /**
     * Replaces current message list with new list.
     * Should be used only on initial load or refresh.
     */
    public void loadLatestMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if (mChannel == null) {
            return;
        }

        if (isMessageListLoading()) {
            return;
        }

        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(Long.MAX_VALUE, true, limit, true, BaseChannel.MessageTypeFilter.ALL, null, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if (handler != null) {
                    handler.onResult(list, e);
                }

                setMessageListLoading(false);
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

                if (list.size() <= 0) {
                    return;
                }

                for (BaseMessage message : mMessageList) {
                    if (isTempMessage(message) || isFailedMessage(message)) {
                        list.add(0, message);
                    }
                }

                mMessageList.clear();

                for (BaseMessage message : list) {
                    mMessageList.add(message);
                }

                notifyDataSetChanged();
            }
        });
    }

    public void setItemLongClickListener(OnItemLongClickListener listener) {
        mItemLongClickListener = listener;
    }

    public void setItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    /**
     * Checks if the current message was sent by the same person that sent the preceding message.
     * <p>
     * This is done so that redundant UI, such as sender nickname and profile picture,
     * does not have to displayed when not necessary.
     */
    private boolean isContinuous(BaseMessage currentMsg, BaseMessage precedingMsg) {
        // null check
        if (currentMsg == null || precedingMsg == null) {
            return false;
        }

        if (currentMsg instanceof AdminMessage && precedingMsg instanceof AdminMessage) {
            return true;
        }

        User currentUser = null, precedingUser = null;

        if (currentMsg instanceof UserMessage) {
            currentUser = currentMsg.getSender();
        } else if (currentMsg instanceof FileMessage) {
            currentUser = currentMsg.getSender();
        }

        if (precedingMsg instanceof UserMessage) {
            precedingUser = precedingMsg.getSender();
        } else if (precedingMsg instanceof FileMessage) {
            precedingUser = precedingMsg.getSender();
        }

        // If admin message or
        return !(currentUser == null || precedingUser == null)
                && currentUser.getUserId().equals(precedingUser.getUserId());

    }

    interface OnItemLongClickListener {
        void onUserMessageItemLongClick(UserMessage message, int position);

        void onFileMessageItemLongClick(FileMessage message);

        void onAdminMessageItemLongClick(AdminMessage message);
    }

    interface OnItemClickListener {
        void onUserMessageItemClick(UserMessage message);

        void onFileMessageItemClick(FileMessage message);
    }

    private class AdminMessageHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView dateText;

        AdminMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_group_chat_message);
            dateText = itemView.findViewById(R.id.text_group_chat_date);
        }

        void bind(Context context, AdminMessage message, GroupChannel channel, boolean isNewDay) {
            messageText.setText(message.getMessage());

            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }
        }
    }

    private class MyUserMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, editedText, timeText, dateText;
        ViewGroup urlPreviewContainer;
        TextView urlPreviewSiteNameText, urlPreviewTitleText, urlPreviewDescriptionText;
        ImageView urlPreviewMainImageView, messageImage;
        View padding;
        MessageStatusView messageStatusView;

        MyUserMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_group_chat_message);
            messageImage = itemView.findViewById(R.id.text_image_chat_message);
            editedText = itemView.findViewById(R.id.text_group_chat_edited);
            timeText = itemView.findViewById(R.id.text_group_chat_time);
            dateText = itemView.findViewById(R.id.text_group_chat_date);
            messageStatusView = itemView.findViewById(R.id.message_status_group_chat);

            urlPreviewContainer = itemView.findViewById(R.id.url_preview_container);
            urlPreviewSiteNameText = itemView.findViewById(R.id.text_url_preview_site_name);
            urlPreviewTitleText = itemView.findViewById(R.id.text_url_preview_title);
            urlPreviewDescriptionText = itemView.findViewById(R.id.text_url_preview_description);
            urlPreviewMainImageView = itemView.findViewById(R.id.image_url_preview_main);

            // Dynamic padding that can be hidden or shown based on whether the message is continuous.
            padding = itemView.findViewById(R.id.view_group_chat_padding);
        }

        void bind(Context context, final UserMessage message, GroupChannel channel, boolean isContinuous, boolean isNewDay, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener, final int position) {
            messageText.setText(message.getMessage());
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            if (message.getUpdatedAt() > 0) {
                editedText.setVisibility(View.VISIBLE);
            } else {
                editedText.setVisibility(View.GONE);
            }

            // If the message is sent on a different date than the previous one, display the date.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            if (message.getCustomType().equals(URL_PREVIEW_CUSTOM_TYPE)) {
                ImageUtils.displayRoundCornerImageFromUrl(context, message.getData(), messageImage);
                messageImage.setVisibility(View.VISIBLE);
            } else {
                messageImage.setVisibility(View.GONE);
            }

            if (clickListener != null) {
                itemView.setOnClickListener(v -> clickListener.onUserMessageItemClick(message));
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener(v -> {
                    longClickListener.onUserMessageItemLongClick(message, position);
                    return true;
                });
            }

            messageStatusView.drawMessageStatus(channel, message);
        }

    }

    private class OtherUserMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, editedText, nicknameText, timeText, dateText;
        ImageView profileImage, messageImage;

        ViewGroup urlPreviewContainer;
        TextView urlPreviewSiteNameText, urlPreviewTitleText, urlPreviewDescriptionText;
        ImageView urlPreviewMainImageView;

        public OtherUserMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_group_chat_message);
            messageImage = itemView.findViewById(R.id.text_image_chat_message);
            editedText = itemView.findViewById(R.id.text_group_chat_edited);
            timeText = itemView.findViewById(R.id.text_group_chat_time);
            nicknameText = itemView.findViewById(R.id.text_group_chat_nickname);
            profileImage = itemView.findViewById(R.id.image_group_chat_profile);
            dateText = itemView.findViewById(R.id.text_group_chat_date);

            urlPreviewContainer = itemView.findViewById(R.id.url_preview_container);
            urlPreviewSiteNameText = itemView.findViewById(R.id.text_url_preview_site_name);
            urlPreviewTitleText = itemView.findViewById(R.id.text_url_preview_title);
            urlPreviewDescriptionText = itemView.findViewById(R.id.text_url_preview_description);
            urlPreviewMainImageView = itemView.findViewById(R.id.image_url_preview_main);
        }


        void bind(Context context, final UserMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener, final int position) {
            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            if (isContinuous) {
                nicknameText.setVisibility(View.GONE);
            } else {
                ImageUtils.displayRoundCornerImageFromUrl(context, message.getSender().getProfileUrl(), profileImage);

                nicknameText.setText(message.getSender().getNickname());
            }

            messageText.setText(message.getMessage());
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            if (message.getUpdatedAt() > 0) {
                editedText.setVisibility(View.VISIBLE);
            } else {
                editedText.setVisibility(View.GONE);
            }

            if (message.getCustomType().equals(URL_PREVIEW_CUSTOM_TYPE)) {
                ImageUtils.displayRoundCornerImageFromUrl(context, message.getData(), messageImage);
                messageImage.setVisibility(View.VISIBLE);
            } else {
                messageImage.setVisibility(View.GONE);
            }

            if (clickListener != null) {
                itemView.setOnClickListener(v -> clickListener.onUserMessageItemClick(message));
            }
            if (longClickListener != null) {
                itemView.setOnLongClickListener(v -> {
                    longClickListener.onUserMessageItemLongClick(message, position);
                    return true;
                });
            }
        }
    }

    private class MyFileMessageHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, timeText, dateText;
        MessageStatusView messageStatusView;

        public MyFileMessageHolder(View itemView) {
            super(itemView);

            timeText = itemView.findViewById(R.id.text_group_chat_time);
            fileNameText = itemView.findViewById(R.id.text_group_chat_file_name);
            dateText = itemView.findViewById(R.id.text_group_chat_date);
            messageStatusView = itemView.findViewById(R.id.message_status_group_chat);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, final OnItemClickListener listener) {
            fileNameText.setText(message.getName());
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onFileMessageItemClick(message));
            }

            messageStatusView.drawMessageStatus(channel, message);
        }
    }

    private class OtherFileMessageHolder extends RecyclerView.ViewHolder {
        TextView nicknameText, timeText, fileNameText, fileSizeText, dateText;
        ImageView profileImage;

        public OtherFileMessageHolder(View itemView) {
            super(itemView);

            nicknameText = itemView.findViewById(R.id.text_group_chat_nickname);
            timeText = itemView.findViewById(R.id.text_group_chat_time);
            fileNameText = itemView.findViewById(R.id.text_group_chat_file_name);
//            fileSizeText =  itemView.findViewById(R.id.text_group_chat_file_size);

            profileImage = itemView.findViewById(R.id.image_group_chat_profile);
            dateText = itemView.findViewById(R.id.text_group_chat_date);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener listener) {
            fileNameText.setText(message.getName());
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));
//            fileSizeText.setText(String.valueOf(message.getSize()));

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
//                profileImage.setVisibility(View.INVISIBLE);
                nicknameText.setVisibility(View.GONE);
            } else {
//                profileImage.setVisibility(View.VISIBLE);
                ImageUtils.displayRoundCornerImageFromUrl(context, message.getSender().getProfileUrl(), profileImage);

//                nicknameText.setVisibility(View.VISIBLE);
                nicknameText.setText(message.getSender().getNickname());
            }

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }

    private class MyImageFileMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText;
        ImageView fileThumbnailImage;
        MessageStatusView messageStatusView;

        public MyImageFileMessageHolder(View itemView) {
            super(itemView);

            timeText = itemView.findViewById(R.id.text_group_chat_time);
            fileThumbnailImage = itemView.findViewById(R.id.image_group_chat_file_thumbnail);
            dateText = itemView.findViewById(R.id.text_group_chat_date);
            messageStatusView = itemView.findViewById(R.id.message_status_group_chat);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isTempMessage, Uri tempFileMessageUri, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            if (isTempMessage && tempFileMessageUri != null) {
                ImageUtils.displayRoundCornerImageFromUrl(context, tempFileMessageUri.toString(), fileThumbnailImage);
            } else {
                // Get thumbnails from FileMessage
                ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size() > 0) {
                    if (message.getType().toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, thumbnails.get(0).getUrl());
                    } else {
                        ImageUtils.displayRoundCornerImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage);
                    }
                } else {
                    if (message.getType().toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, (String) null);
                    } else {
                        ImageUtils.displayRoundCornerImageFromUrl(context, message.getUrl(), fileThumbnailImage);
                    }
                }
            }

            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onFileMessageItemClick(message));
            }

            messageStatusView.drawMessageStatus(channel, message);
        }
    }

    private class OtherImageFileMessageHolder extends RecyclerView.ViewHolder {

        TextView timeText, nicknameText, dateText;
        ImageView profileImage, fileThumbnailImage;

        public OtherImageFileMessageHolder(View itemView) {
            super(itemView);

            timeText = itemView.findViewById(R.id.text_group_chat_time);
            nicknameText = itemView.findViewById(R.id.text_group_chat_nickname);
            fileThumbnailImage = itemView.findViewById(R.id.image_group_chat_file_thumbnail);
            profileImage = itemView.findViewById(R.id.image_group_chat_profile);
            dateText = itemView.findViewById(R.id.text_group_chat_date);


        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
//                profileImage.setVisibility(View.INVISIBLE);
                nicknameText.setVisibility(View.GONE);
            } else {
//                profileImage.setVisibility(View.VISIBLE);
                ImageUtils.displayRoundCornerImageFromUrl(context, message.getSender().getProfileUrl(), profileImage);

//                nicknameText.setVisibility(View.VISIBLE);
                nicknameText.setText(message.getSender().getNickname());
            }

            // Get thumbnails from FileMessage
            ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size() > 0) {
                if (message.getType().toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, thumbnails.get(0).getUrl());
                } else {
                    ImageUtils.displayRoundCornerImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage);
                }
            } else {
                if (message.getType().toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, (String) null);
                } else {
                    ImageUtils.displayRoundCornerImageFromUrl(context, message.getUrl(), fileThumbnailImage);
                }
            }

            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onFileMessageItemClick(message));
            }
        }
    }

    private class MyVideoFileMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText;
        ImageView fileThumbnailImage;
        MessageStatusView messageStatusView;

        public MyVideoFileMessageHolder(View itemView) {
            super(itemView);

            timeText = itemView.findViewById(R.id.text_group_chat_time);
            fileThumbnailImage = itemView.findViewById(R.id.image_group_chat_file_thumbnail);
            dateText = itemView.findViewById(R.id.text_group_chat_date);
            messageStatusView = itemView.findViewById(R.id.message_status_group_chat);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isTempMessage, Uri tempFileMessageUri, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            if (isTempMessage && tempFileMessageUri != null) {
                ImageUtils.displayRoundCornerImageFromUrl(context, tempFileMessageUri.toString(), fileThumbnailImage);
            } else {
                // Get thumbnails from FileMessage
                ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size() > 0) {
                    ImageUtils.displayRoundCornerImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage);
                }
            }

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }

            messageStatusView.drawMessageStatus(channel, message);
        }
    }

    private class OtherVideoFileMessageHolder extends RecyclerView.ViewHolder {

        TextView timeText, nicknameText, dateText;
        ImageView profileImage, fileThumbnailImage;

        public OtherVideoFileMessageHolder(View itemView) {
            super(itemView);

            timeText = itemView.findViewById(R.id.text_group_chat_time);
            nicknameText = itemView.findViewById(R.id.text_group_chat_nickname);
            fileThumbnailImage = itemView.findViewById(R.id.image_group_chat_file_thumbnail);
            profileImage = itemView.findViewById(R.id.image_group_chat_profile);
            dateText = itemView.findViewById(R.id.text_group_chat_date);

        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                dateText.setVisibility(View.VISIBLE);
                dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
            } else {
                dateText.setVisibility(View.GONE);
            }

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
                profileImage.setVisibility(View.INVISIBLE);
                nicknameText.setVisibility(View.GONE);
            } else {
//                profileImage.setVisibility(View.VISIBLE);
                ImageUtils.displayRoundCornerImageFromUrl(context, message.getSender().getProfileUrl(), profileImage);

//                nicknameText.setVisibility(View.VISIBLE);
                nicknameText.setText(message.getSender().getNickname());
            }

            // Get thumbnails from FileMessage
            ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size() > 0) {
                ImageUtils.displayRoundCornerImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage);
            }

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }
}


