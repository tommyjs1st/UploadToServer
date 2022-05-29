package kr.co.musi.uploadtoserver;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;

public class SubLayout extends LinearLayout {

    public SubLayout(Context context, AttributeSet attrs, WearItem wearItem) {
        super(context, attrs);
        init(context, wearItem);
    }

    public SubLayout(Context context, WearItem wearItem) {
        super(context);
        init(context, wearItem);
    }

    private void init(Context context, WearItem wearItem) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.layout_sub_layout, this, true);

        ImageView img = (ImageView)findViewById(R.id.glide_imageview);

        // 이미지 로드 라이브러리 사용 ImageUrl to Image
        Glide.with(this)
                .load(wearItem.getImageUrl().toString())
                .override(300,300)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(img);
    }
}
