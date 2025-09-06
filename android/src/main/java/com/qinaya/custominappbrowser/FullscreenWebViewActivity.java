package com.qinaya.custominappbrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ActionMode;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.graphics.Color;

public class FullscreenWebViewActivity extends Activity {

    private CustomWebView webView;
    private String url;

    // Track selection ActionMode to avoid first-Back being eaten after left click
    private ActionMode selectionMode;

    // After any primary (left) click, treat the next mouse BACK as right-click until consumed
    private boolean hadPrimaryDownSinceLastRight = false;

    // Keep WebView simple; do NOT fight the IME here — that breaks accents.
    public class CustomWebView extends WebView {
        public CustomWebView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchKeyEventPreIme(KeyEvent event) {
            // Intercept BACK generated from mouse right-click before WebView/text editor consumes it
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                boolean isMouseDevice = false;
                try {
                    InputDevice dev = InputDevice.getDevice(event.getDeviceId());
                    isMouseDevice = dev != null && dev.supportsSource(InputDevice.SOURCE_MOUSE);
                } catch (Throwable ignored) {}

                if (isMouseDevice && hadPrimaryDownSinceLastRight) {
                    if (selectionMode != null) {
                        selectionMode.finish();
                    }
                    simulateShiftF10Keys();
                    hadPrimaryDownSinceLastRight = false; // consume this pairing
                    return true;
                }
            }
            return super.dispatchKeyEventPreIme(event);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen chrome
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_FULLSCREEN
          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // URL
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        if (url == null || url.isEmpty()) {
            finish();
            return;
        }

        // WebView
        webView = new CustomWebView(this);
        webView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ));

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        // Remote-desktop friendly
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(); // do this once on create; do NOT keep re-requesting later

        webView.setBackgroundColor(Color.BLACK);

        // Keep navigation inside the WebView
        webView.setWebViewClient(new WebViewClient());

        // Prevent native text-selection/context CAB in the WebView
        webView.setLongClickable(false);
        webView.setOnLongClickListener(v -> true);

        // API 23+: catch real context-clicks regardless of selection state
        if (Build.VERSION.SDK_INT >= 23) {
            webView.setOnContextClickListener(v -> {
                simulateShiftF10Keys();
                hadPrimaryDownSinceLastRight = false;
                return true;
            });
        }

        // Detect mouse RIGHT-CLICK via motion events (robust across OEMs)
        webView.setOnGenericMotionListener((v, e) -> {
            if ((e.getSource() & InputDevice.SOURCE_MOUSE) != 0) {
                int action = e.getActionMasked();

                // Most reliable path: explicit button press with actionButton
                if (action == MotionEvent.ACTION_BUTTON_PRESS
                        && e.getActionButton() == MotionEvent.BUTTON_SECONDARY) {
                    simulateShiftF10Keys();
                    hadPrimaryDownSinceLastRight = false;
                    return true;
                }

                // Fallback for drivers that only tag ACTION_DOWN with a bitmask
                if (action == MotionEvent.ACTION_DOWN
                        && (e.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                    simulateShiftF10Keys();
                    hadPrimaryDownSinceLastRight = false;
                    return true;
                }

                // Record recent primary press to detect BACK→right-click translations
                if (action == MotionEvent.ACTION_BUTTON_PRESS
                        && e.getActionButton() == MotionEvent.BUTTON_PRIMARY) {
                    hadPrimaryDownSinceLastRight = true;
                }
            }
            return false;
        });

        // Some OEMs only fire it via onTouch
        webView.setOnTouchListener((v, e) -> {
            if ((e.getSource() & InputDevice.SOURCE_MOUSE) != 0) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                        && (e.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                    simulateShiftF10Keys();
                    hadPrimaryDownSinceLastRight = false;
                    return true;
                }

                if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                        && (e.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                    hadPrimaryDownSinceLastRight = true;
                }
            }
            return false;
        });

        setContentView(webView);

        // Hide any already-open IME once (non-destructive for composition)
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(webView.getWindowToken(), 0);
        }

        webView.loadUrl(url);
    }

    // Track selection ActionMode so right-click never needs a second BACK
    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        selectionMode = mode;
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        if (selectionMode == mode) selectionMode = null;
    }

    @Override
    public void onBackPressed() {
        // Kiosk-style: ignore back. If needed, implement webView.canGoBack() here.
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // If WebView started selection/cab, finish it so BACK isn't eaten
                    if (selectionMode != null) {
                        selectionMode.finish();
                    }
                    simulateShiftF10Keys();
                }
                return true;

           case KeyEvent.KEYCODE_ENTER:
                // Android TV Remote: Center/OK button → Classic Enter key
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                     if (selectionMode != null) {
                        selectionMode.finish();
                    }
                    simulateEnterKey();
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    // Reliable Shift+F10: send F10 with SHIFT meta state (no separate SHIFT events)
    private void simulateShiftF10Keys() {
            long t = SystemClock.uptimeMillis();
            KeyEvent shiftDown = new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0, 0, 0);
            KeyEvent f10Down   = new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F10, 0, KeyEvent.META_SHIFT_ON, 0, 0);
            KeyEvent f10Up     = new KeyEvent(t, t, KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_F10, 0, KeyEvent.META_SHIFT_ON, 0, 0);
            KeyEvent shiftUp   = new KeyEvent(t, t, KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0, 0, 0);

            webView.dispatchKeyEvent(shiftDown);
            webView.dispatchKeyEvent(f10Down);
            webView.dispatchKeyEvent(f10Up);
            webView.dispatchKeyEvent(shiftUp);
    }

    private void simulateEnterKey() {

            long t0 = SystemClock.uptimeMillis();
            KeyEvent enterDown = new KeyEvent(t0, t0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0);
            KeyEvent enterUp = new KeyEvent(t0 + 8, t0 + 8, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0, 0);
            
            webView.dispatchKeyEvent(enterDown);
            webView.dispatchKeyEvent(enterUp);
            
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            // DO NOT call webView.requestFocus() here — that resets the InputConnection and breaks accents.
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
