package com.yekong.rxmobile.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.trello.rxlifecycle.components.RxFragment;
import com.yekong.rxmobile.R;
import com.yekong.rxmobile.model.DoubanUser;

public class DoubanUserFragment extends RxFragment {
    private static final String ARG_JSON = "ARG_JSON";

    ImageView mAvatarImage;
    TextView mNameText;
    TextView mDescText;
    TextView mSignatureText;

    private String mJson;

    public DoubanUserFragment() {
        // Required empty public constructor
    }

    public static DoubanUserFragment newInstance(String json) {
        DoubanUserFragment fragment = new DoubanUserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, json);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mJson = getArguments().getString(ARG_JSON);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_douban_user, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
    }

    private void initView(View view) {
        mAvatarImage = (ImageView) view.findViewById(R.id.large_avatar);
        mNameText = (TextView) view.findViewById(R.id.name);
        mDescText = (TextView) view.findViewById(R.id.desc);
        mSignatureText = (TextView) view.findViewById(R.id.signature);
    }

    private void initData() {
        DoubanUser user = new Gson().fromJson(mJson, DoubanUser.class);
        Glide.with(getActivity())
                .load(user.large_avatar)
                .placeholder(R.mipmap.ic_launcher)
                .into(mAvatarImage);
        mNameText.setText(user.name);
        mDescText.setText(user.desc);
        mSignatureText.setText(user.signature);
    }
}
