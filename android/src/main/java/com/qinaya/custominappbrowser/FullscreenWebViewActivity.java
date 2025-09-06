package com.qinaya.custominappbrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;                  // NEW
import android.os.Bundle;
import android.os.SystemClock;            // NEW
import android.view.ActionMode;           // NEW
import android.view.InputDevice;          // NEW
import android.view.KeyEvent;
import android.view.MotionEvent;          // NEW
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
    private ActionMode selectionMode;     // NEW

    // Track recent primary (left) mouse presses to disambiguate BACK from right-click
    private static final long RIGHT_CLICK_BACK_DETECT_WINDOW_MS = 1200L; // within 1.2s after left click
    private long lastPrimaryDownUptimeMs = 0L;
    private boolean hadPrimaryDownSinceLastRight = false;

    // Keep WebView simple; do NOT fight the IME here — that breaks accents.
    public class CustomWebView extends WebView {
        public CustomWebView(Context context) {
            super(context);
        }

        @Override
        public boolean requestFocus(int direction, android.graphics.Rect previouslyFocusedRect) {
            // Allow normal focus (needed for composition/accents)
            return super.requestFocus(direction, previouslyFocusedRect);
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

                long now = SystemClock.uptimeMillis();
                boolean withinWindow = hadPrimaryDownSinceLastRight && (now - lastPrimaryDownUptimeMs) <= RIGHT_CLICK_BACK_DETECT_WINDOW_MS;

                if (isMouseDevice && withinWindow) {
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

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return super.dispatchKeyEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return super.onTouchEvent(event);
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

        // Remote-desktop: prevent native text-selection/context CAB since we don't need it
        webView.setLongClickable(false);                                // NEW (optional for RDP)
        webView.setOnLongClickListener(v -> true);                      // NEW (optional for RDP)

        // API 23+: catch real context-clicks regardless of selection state
        if (Build.VERSION.SDK_INT >= 23) {                              // NEW
            webView.setOnContextClickListener(v -> {                    // NEW
                simulateShiftF10Keys();
                hadPrimaryDownSinceLastRight = false;
                return true;
            });
        }

        // Detect mouse RIGHT-CLICK via motion events (robust across OEMs)
        webView.setOnGenericMotionListener((v, e) -> {                  // UPDATED
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
                    lastPrimaryDownUptimeMs = SystemClock.uptimeMillis();
                    hadPrimaryDownSinceLastRight = true;
                }
            }
            return false;
        });

        // Some OEMs only fire it via onTouch
        webView.setOnTouchListener((v, e) -> {                          // UPDATED
            if ((e.getSource() & InputDevice.SOURCE_MOUSE) != 0) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                        && (e.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                    simulateShiftF10Keys();
                    hadPrimaryDownSinceLastRight = false;
                    return true;
                }

                if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                        && (e.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                    lastPrimaryDownUptimeMs = SystemClock.uptimeMillis();
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
    public void onActionModeStarted(ActionMode mode) {                  // NEW
        super.onActionModeStarted(mode);
        selectionMode = mode;
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {                 // NEW
        super.onActionModeFinished(mode);
        if (selectionMode == mode) selectionMode = null;
    }

    @Override
    public void onBackPressed() {
        // Kiosk-style: ignore back. If needed, implement webView.canGoBack() here.
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*
         * Remote mappings:
         * - Some remotes map BACK to "mouse right click" semantics → map to Shift+F10.
         * - Do not synthesize ENTER; let it flow normally.
         */
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // If WebView started selection/cab, finish it so BACK isn't eaten
                    if (selectionMode != null) {
                        selectionMode.finish();                         // NEW
                    }
                    simulateShiftF10Keys();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    simulateShiftF10Keys();
                }
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    // Reliable Shift+F10: send F10 with SHIFT meta state (no separate SHIFT events)
    private void simulateShiftF10Keys() {
            long t = SystemClock.uptimeMillis();
            KeyEvent d = new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F10, 0, KeyEvent.META_SHIFT_ON);
            KeyEvent u = new KeyEvent(t, t, KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_F10, 0, KeyEvent.META_SHIFT_ON);
            webView.dispatchKeyEvent(d);
            webView.dispatchKeyEvent(u);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let WebView consume keys first when focused; otherwise normal Activity handling.
        if (webView != null && webView.hasFocus()) {
            if (webView.dispatchKeyEvent(event)) return true;
        }
        return super.dispatchKeyEvent(event);
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
