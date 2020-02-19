
package de.guitarcollege.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.RelativeLayout;


public class ExtendedTouchImageView extends RelativeLayout {
    protected TouchImageView mImageView;

    protected Context mContext;

    public ExtendedTouchImageView(Context ctx)
    {
        super(ctx);
        mContext = ctx;
        init();

    }
    public ExtendedTouchImageView(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        mContext = ctx;
        init();
    }
    public TouchImageView getImageView() { return mImageView; }

    protected void init() {
        mImageView = new TouchImageView(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mImageView.setLayoutParams(params);
        this.addView(mImageView);
        mImageView.setVisibility(GONE);

        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.setMargins(30, 0, 30, 0);
       
    }
    
    public void setImageBitmap(Bitmap bitmap)
    {
    	mImageView.setImageBitmap(bitmap);
        mImageView.setVisibility(VISIBLE);
    }
    
}
