package com.sendbird.android.sample.groupchannel;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.sendbird.android.sample.R;
import com.sendbird.android.sample.utils.GenericDialog;

import kotlin.Unit;

import static com.sendbird.android.sample.utils.TextUtils.THEME_MATH;


public class DummyChatFragment extends Fragment {

    private Toolbar dummy_toolbar;
    private TextView dummy_message_text;
    private ProgressBar progressBar;

    GenericDialog joinChatDialog =
            new GenericDialog().newInstance(THEME_MATH);

    /**
     * To create an instance of this fragment, a Channel URL should be required.
     */
    public static DummyChatFragment newInstance(@NonNull String channelUrl) {
        DummyChatFragment fragment = new DummyChatFragment();

        Bundle args = new Bundle();
        args.putString(GroupChannelListFragment.EXTRA_GROUP_CHANNEL_URL, channelUrl);
        fragment.setArguments(args);

        return fragment;
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get question text and uri
        timer();
        joinChatDialog.setTitle(getString(R.string.tutor_is_ready))
                        .setMessage(getString(R.string.join_chat));

        joinChatDialog.setPositiveButton(R.string.join, R.drawable.bg_btn_complete, () -> {

            return Unit.INSTANCE;
        });

    }

    private void joinChat(){
        joinChatDialog.show(requireFragmentManager(), "");
        progressBar.setVisibility(View.GONE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dummy_group_chat, container, false);

        dummy_toolbar = rootView.findViewById(R.id.dummy_toolbar);
        dummy_message_text = rootView.findViewById(R.id.dummy_message_text);
        progressBar = rootView.findViewById(R.id.progressBar);

        dummy_message_text.setText(R.string.sample_question);

        dummy_toolbar.setNavigationOnClickListener(view -> {
            getActivity().getSupportFragmentManager().popBackStack();
        });

        return rootView;
    }

    private void timer() {

        new CountDownTimer(2000, 1000) {

            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                joinChat();
            }
        }.start();

    }

}
