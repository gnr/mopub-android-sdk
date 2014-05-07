package com.mopub.mobileads;

import java.util.Map;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdError.ErrorCode;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdTargetingOptions;

public class AmazonBanner extends CustomEventBanner implements AdListener {
    private CustomEventBannerListener mBannerListener;
    private AdLayout mAmazonAd;

    /*
     * Abstract methods from CustomEventBanner
     */
    @Override
	public void loadBanner(Activity activity, CustomEventBannerListener bannerListener,
                              Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mBannerListener = bannerListener;

        Log.d("AmazonBanner", "loadAd()");
        mAmazonAd = new AdLayout(activity);
        
        // auto size the banner to fit the screen width
        mAmazonAd.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // this is how to set the banner size explicitly
//        mAmazonAd = new AdLayout(activity, AdSize.SIZE_320x50);
//        float scale = activity.getApplicationContext().getResources().getDisplayMetrics().density;
//        LayoutParams layoutParams = new FrameLayout.LayoutParams((int) (320 * scale), (int) (50 * scale), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
//        mAmazonAd.setLayoutParams(layoutParams);
        
        mAmazonAd.setListener(this);
        mAmazonAd.loadAd(new AdTargetingOptions());
    }

    @Override
	public void onInvalidate() {
        Log.d("AmazonBanner", "onInvalidate()");
//        mAmazonAd.destroy();
//        mAmazonAd = null;
    }

    /*
     * Methods from Amazon's AdListener
     */

	@Override
	public void onAdFailedToLoad(Ad view, AdError error) {
		final ErrorCode code = error.getCode();
		MoPubErrorCode moPubCode = MoPubErrorCode.NO_FILL;
		if(code == ErrorCode.NETWORK_ERROR) {
			moPubCode = MoPubErrorCode.NETWORK_INVALID_STATE;
		} else if(code == ErrorCode.NO_FILL) {
			moPubCode = MoPubErrorCode.NO_FILL;
		} else if(code == ErrorCode.INTERNAL_ERROR) {
			moPubCode = MoPubErrorCode.INTERNAL_ERROR;
		} else if(code == ErrorCode.REQUEST_ERROR) {
			moPubCode = MoPubErrorCode.NETWORK_TIMEOUT;
		}
		
		Log.d("AmazonBanner", "onAdFailedToLoad()");
		mBannerListener.onBannerFailed(moPubCode);
	}

	@Override
	public void onAdLoaded(Ad view, AdProperties adProperties) {
        Log.d("AmazonBanner", "onAdLoaded()");
		mBannerListener.onBannerLoaded(mAmazonAd);
	}

	@Override
	public void onAdCollapsed(Ad view) {
		Log.d("AmazonBanner", "onAdCollapsed()");
	}

	@Override
	public void onAdDismissed(Ad view) {
		Log.d("AmazonBanner", "onAdDismissed()");
	}

	@Override
	public void onAdExpanded(Ad view) {
		Log.d("AmazonBanner", "onAdExpanded()");
	}
}