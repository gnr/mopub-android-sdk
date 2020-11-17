package com.mopub.mobileads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebViewDatabase;
import android.widget.FrameLayout;

import com.mopub.common.AdFormat;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ManifestUtils;
import com.mopub.common.util.Visibility;
import com.mopub.mobileads.factories.AdViewControllerFactory;
import com.mopub.mobileads.factories.CustomEventBannerAdapterFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;

public class MoPubView extends FrameLayout {
    public interface BannerAdListener {
        public void onBannerLoaded(MoPubView banner);
        public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode);
        public void onBannerClicked(MoPubView banner);
        public void onBannerExpanded(MoPubView banner);
        public void onBannerCollapsed(MoPubView banner);
    }

    @Nullable
    protected AdViewController mAdViewController;
    protected CustomEventBannerAdapter mCustomEventBannerAdapter;

    private Context mContext;
    private int mScreenVisibility;
    private BroadcastReceiver mScreenStateReceiver;

    private BannerAdListener mBannerAdListener;

    public MoPubView(Context context) {
        this(context, null);
    }

    public MoPubView(Context context, AttributeSet attrs) {
        super(context, attrs);

        ManifestUtils.checkWebViewActivitiesDeclared(context);

        mContext = context;
        mScreenVisibility = getVisibility();

        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        // There is a rare bug in Froyo/2.2 where creation of a WebView causes a
        // NullPointerException. (https://code.google.com/p/android/issues/detail?id=10789)
        // It happens when the WebView can't access the local file store to make a cache file.
        // Here, we'll work around it by trying to create a file store and then just go inert
        // if it's not accessible.
        try {
            if (WebViewDatabase.getInstance(context) == null) {
                MoPubLog.e("Disabling MoPub. Local cache file is inaccessible so MoPub will " +
                        "fail if we try to create a WebView. Details of this Android bug found at:" +
                        "http://code.google.com/p/android/issues/detail?id=10789");
                return;
            }
        } catch(Throwable t) {
            MoPubLog.e("Disabling MoPub. Error accessing local cache file so MoPub will " +
                    "fail if we try to create a WebView. Details of this Android bug found at:" +
                    "https://code.google.com/p/android/issues/detail?id=10789");
            return;
        }

        mAdViewController = AdViewControllerFactory.create(context, this);
        registerScreenStateBroadcastReceiver();
    }

    private void registerScreenStateBroadcastReceiver() {
        mScreenStateReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                if (!Visibility.isScreenVisible(mScreenVisibility) || intent == null) {
                    return;
                }

                final String action = intent.getAction();

                if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    setAdVisibility(View.VISIBLE);
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    setAdVisibility(View.GONE);
                }
            }
        };

        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mScreenStateReceiver, filter);
    }

    private void unregisterScreenStateBroadcastReceiver() {
        try {
            mContext.unregisterReceiver(mScreenStateReceiver);
        } catch (Exception IllegalArgumentException) {
            MoPubLog.d("Failed to unregister screen state broadcast receiver (never registered).");
        }
    }

    public void loadAd() {
        if (mAdViewController != null) {
            mAdViewController.loadAd();
        }
    }

    /*
     * Tears down the ad view: no ads will be shown once this method executes. The parent
     * Activity's onDestroy implementation must include a call to this method.
     */
    public void destroy() {
        unregisterScreenStateBroadcastReceiver();
        removeAllViews();

        if (mAdViewController != null) {
            mAdViewController.cleanup();
            mAdViewController = null;
        }

        if (mCustomEventBannerAdapter != null) {
            mCustomEventBannerAdapter.invalidate();
            mCustomEventBannerAdapter = null;
        }
    }

    Integer getAdTimeoutDelay() {
        return (mAdViewController != null) ? mAdViewController.getAdTimeoutDelay() : null;
    }

    protected void loadFailUrl(MoPubErrorCode errorCode) {
        if (mAdViewController != null) mAdViewController.loadFailUrl(errorCode);
    }

    protected void loadCustomEvent(String customEventClassName, Map<String, String> serverExtras) {
        if (mAdViewController == null) {
            return;
        }
        if (TextUtils.isEmpty(customEventClassName)) {
            MoPubLog.d("Couldn't invoke custom event because the server did not specify one.");
            loadFailUrl(ADAPTER_NOT_FOUND);
            return;
        }

        if (mCustomEventBannerAdapter != null) {
            mCustomEventBannerAdapter.invalidate();
        }

        MoPubLog.d("Loading custom event adapter.");

        mCustomEventBannerAdapter = CustomEventBannerAdapterFactory.create(
                this,
                customEventClassName,
                serverExtras,
                mAdViewController.getBroadcastIdentifier(),
                mAdViewController.getAdReport());
        mCustomEventBannerAdapter.loadAd();
    }

    protected void registerClick() {
        if (mAdViewController != null) {
            mAdViewController.registerClick();

            // Let any listeners know that an ad was clicked
            adClicked();
        }
    }

    protected void trackNativeImpression() {
        MoPubLog.d("Tracking impression for native adapter.");
        if (mAdViewController != null) mAdViewController.trackImpression();
    }

    @Override
    protected void onWindowVisibilityChanged(final int visibility) {
        // Ignore transitions between View.GONE and View.INVISIBLE
        if (Visibility.hasScreenVisibilityChanged(mScreenVisibility, visibility)) {
            mScreenVisibility = visibility;
            setAdVisibility(mScreenVisibility);
        }
    }

    private void setAdVisibility(final int visibility) {
        if (mAdViewController == null) {
            return;
        }

        if (Visibility.isScreenVisible(visibility)) {
            mAdViewController.unpauseRefresh();
        } else {
            mAdViewController.pauseRefresh();
        }
    }

    protected void adLoaded() {
        MoPubLog.d("adLoaded");
        
        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerLoaded(this);
        }
    }

    protected void adFailed(MoPubErrorCode errorCode) {
        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerFailed(this, errorCode);
        }
    }

    protected void adPresentedOverlay() {
        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerExpanded(this);
        }
    }

    protected void adClosed() {
        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerCollapsed(this);
        }
    }

    protected void adClicked() {
        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerClicked(this);
        }
    }

    protected void nativeAdLoaded() {
        if (mAdViewController != null) mAdViewController.scheduleRefreshTimerIfEnabled();
        adLoaded();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setAdUnitId(String adUnitId) {
        if (mAdViewController != null) mAdViewController.setAdUnitId(adUnitId);
    }

    public String getAdUnitId() {
        return (mAdViewController != null) ? mAdViewController.getAdUnitId() : null;
    }

    public void setKeywords(String keywords) {
        if (mAdViewController != null) mAdViewController.setKeywords(keywords);
    }

    public String getKeywords() {
        return (mAdViewController != null) ? mAdViewController.getKeywords() : null;
    }

    public void setLocation(Location location) {
        if (mAdViewController != null) mAdViewController.setLocation(location);
    }

    public Location getLocation() {
        return (mAdViewController != null) ? mAdViewController.getLocation() : null;
    }

    public int getAdWidth() {
        return (mAdViewController != null) ? mAdViewController.getAdWidth() : 0;
    }

    public int getAdHeight() {
        return (mAdViewController != null) ? mAdViewController.getAdHeight() : 0;
    }

    public Activity getActivity() {
        return (Activity) mContext;
    }

    public void setBannerAdListener(BannerAdListener listener) {
        mBannerAdListener = listener;
    }

    public BannerAdListener getBannerAdListener() {
        return mBannerAdListener;
    }

    public void setLocalExtras(Map<String, Object> localExtras) {
        if (mAdViewController != null) mAdViewController.setLocalExtras(localExtras);
    }

    public Map<String, Object> getLocalExtras() {
        if (mAdViewController != null) {
            return mAdViewController.getLocalExtras();
        }
        return new TreeMap<String, Object>();
    }

    public void setAutorefreshEnabled(boolean enabled) {
        if (mAdViewController != null) {
            mAdViewController.forceSetAutorefreshEnabled(enabled);
        }
    }

    public boolean getAutorefreshEnabled() {
        if (mAdViewController != null) return mAdViewController.getAutorefreshEnabled();
        else {
            MoPubLog.d("Can't get autorefresh status for destroyed MoPubView. " +
                    "Returning false.");
            return false;
        }
    }

    public void setAdContentView(View view) {
        if (mAdViewController != null) mAdViewController.setAdContentView(view);
    }

    public void setTesting(boolean testing) {
        if (mAdViewController != null) mAdViewController.setTesting(testing);
    }

    public boolean getTesting() {
        if (mAdViewController != null) return mAdViewController.getTesting();
        else {
            MoPubLog.d("Can't get testing status for destroyed MoPubView. " +
                    "Returning false.");
            return false;
        }
    }

    public void forceRefresh() {
        if (mCustomEventBannerAdapter != null) {
            mCustomEventBannerAdapter.invalidate();
            mCustomEventBannerAdapter = null;
        }

        if (mAdViewController != null) mAdViewController.forceRefresh();
    }

    AdViewController getAdViewController() {
        return mAdViewController;
    }

    public AdFormat getAdFormat() {
        return AdFormat.BANNER;
    }

    /**
     * @deprecated As of release 4.4.0
     */
    @Deprecated
    public void setTimeout(int milliseconds) {
    }

    @Deprecated
    public String getResponseString() {
        return null;
    }

    @Deprecated
    public String getClickTrackingUrl() {
        return null;
    }
}
