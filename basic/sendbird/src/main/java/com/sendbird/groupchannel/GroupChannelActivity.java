package com.sendbird.groupchannel;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sendbird.R;
import com.sendbird.android.Member;
import com.sendbird.main.sendBird.Chat;
import com.sendbird.main.sendBird.TutorActions;
import com.sendbird.main.sendBird.User;
import com.sendbird.main.model.UserData;
import com.sendbird.network.userModel.ConnectUserRequest;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;


public class GroupChannelActivity extends AppCompatActivity {

    private onBackPressedListener mOnBackPressedListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_channel);

        ConnectUserRequest connectUserRequest = new ConnectUserRequest("1827", "Taiwo Adebayo", "https://ulesson-staging.s3.eu-west-2.amazonaws.com/learners/avatars/defaults/thumb/missing.png", true);
//        ConnectUserRequest tutorUserData = new ConnectUserRequest("1347", "Tamilore Oyola", "https://ulesson-staging.s3.eu-west-2.amazonaws.com/learners/avatars/defaults/thumb/missing.png",
//                true);

        UserData hostUserData = new UserData("1827", "Taiwo Adebayo", "751d7e83ca2eabe5df2c7ff2855d39b09c5c3eac");
        UserData tutorUserData = new UserData("7", "Wapnen Gowok", "");

        new User().connectUser(connectUserRequest, "751d7e83ca2eabe5df2c7ff2855d39b09c5c3eac", (userResponse) -> {

            HashMap<String, Object> questionMap =  new HashMap<String, Object>();
            questionMap.put("questionId", "123");
            questionMap.put("subject", "11");
            questionMap.put("tutorId", "12");
            questionMap.put("questionText", "What is computer");
            questionMap.put("questionUrl", "https://ulesson-staging.s3.eu-west-2.amazonaws.com/learners/avatars/defaults/thumb/missing.png");
            questionMap.put("subjectName", "Maths");
            questionMap.put("subjectAvatar", "https://ulesson-staging.s3.eu-west-2.amazonaws.com/learners/avatars/defaults/thumb/missing.png");

//            new Chat().createChat(this, hostUserData, tutorUserData, questionMap, (channelUrl) -> {
//                Log.d("okh", channelUrl + "channelUrl");
//                return Unit.INSTANCE;
//            }, (errorData) -> {
//                Log.d("okh", errorData + "channel error");
//               return Unit.INSTANCE;
//            });

            new Chat().showChatList(this, R.id.container_group_channel, new UserData(userResponse.getUser_id(), userResponse.getNickname(), userResponse.getAccess_token()), new TutorActions() {
                @Override
                public void showTutorRating(Map<String, Object> questionMap) {
                    Log.d("okh", "show rating" + questionMap.toString());
                }

                @Override
                public void showTutorProfile(List<? extends Member> member) {
                    Log.d("okh", "show profile");
                }
            });
//            new Chat().createChat(this, hostUserData, tutorUserData, questionMap, (channelUrl) -> {
//                Log.d("okh", channelUrl + "channelUrl");
//                return Unit.INSTANCE;
//            }, (errorData) -> {
//                Log.d("okh", errorData + "channel error");
//               return Unit.INSTANCE;
//            });

            return Unit.INSTANCE;
        }, (errorData) -> {
            Log.d("okh", errorData.getMessage() + "message");

            return Unit.INSTANCE;
        }, (updateAccessToken) -> {
            Log.d("okh", updateAccessToken + "update token");
            return Unit.INSTANCE;
        });

        String channelUrl = getIntent().getStringExtra("groupChannelUrl");
        if (channelUrl != null) {
            // If started from notification
            Fragment fragment = GroupChatFragment.newInstance(channelUrl, "", new TutorActions() {
                @Override
                public void showTutorProfile(@NotNull List<? extends Member> members) {

                }

                @Override
                public void showTutorRating(Map<String, Object> questionMap) {
                }

            });

            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction()
                    .replace(R.id.container_group_channel, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (mOnBackPressedListener != null && mOnBackPressedListener.onBack()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    interface onBackPressedListener {
        boolean onBack();
    }

}
