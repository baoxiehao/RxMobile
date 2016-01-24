package com.yekong.rxmobile.view;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yekong.rxmobile.R;
import com.yekong.rxmobile.model.DoubanUser;
import com.yekong.rxmobile.rx.RxAction;
import com.yekong.rxmobile.rx.RxBus;
import com.yekong.rxmobile.rx.RxEvent;

import uk.co.ribot.easyadapter.ItemViewHolder;
import uk.co.ribot.easyadapter.PositionInfo;
import uk.co.ribot.easyadapter.annotations.LayoutId;
import uk.co.ribot.easyadapter.annotations.ViewId;

/**
 * Created by baoxiehao on 16/1/15.
 */
@LayoutId(R.layout.item_douban_user)
public class DoubanUserViewHolder extends ItemViewHolder<DoubanUser> {
    private static final String TAG = "DoubanUserViewHolder";

    @ViewId(R.id.avatar)
    ImageView mAvatarImage;

    @ViewId(R.id.login)
    TextView mNameText;

    @ViewId(R.id.desc)
    TextView mDescText;

    @ViewId(R.id.close)
    Button mCloseButton;

    public DoubanUserViewHolder(View view) {
        super(view);
    }

    @Override
    public void onSetValues(final DoubanUser item, final PositionInfo positionInfo) {
        Glide.with(getContext())
                .load(TextUtils.isEmpty(item.avatar) ? R.mipmap.ic_launcher : Uri.parse(item.avatar))
                .placeholder(R.mipmap.ic_launcher)
                .into(mAvatarImage);
        mNameText.setText(item.name);
        if (!TextUtils.isEmpty(item.desc)) {
            mDescText.setText(item.desc);
            mDescText.setVisibility(View.VISIBLE);
        } else {
            mDescText.setVisibility(View.GONE);
        }
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxBus.singleton().send(RxAction.create(RxEvent.USER_ITEM_REFRESH, positionInfo.getPosition()));
            }
        });
        getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxBus.singleton().send(RxAction.create(RxEvent.USER_ITEM_OPEN, item));
            }
        });
    }
}
