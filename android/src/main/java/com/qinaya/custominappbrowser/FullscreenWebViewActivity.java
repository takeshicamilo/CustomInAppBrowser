package com.qinaya.custominappbrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.graphics.Color;
import java.lang.reflect.Method;

public class FullscreenWebViewActivity extends Activity {
    
    private CustomWebView webView;
    private String url;
    
    // Custom WebView class to disable virtual keyboard completely
    public class CustomWebView extends WebView {
        
        public CustomWebView(Context context) {
            super(context);
        }
        
        @Override
        public boolean onCheckIsTextEditor() {
            // Always return false to prevent virtual keyboard from showing
            return false;
        }
        
        @Override
        public boolean requestFocus(int direction, android.graphics.Rect previouslyFocusedRect) {
            // Allow focus but prevent keyboard from showing
            super.requestFocus(direction, previouslyFocusedRect);
            hideKeyboard();
            return true;
        }
        
        // Note: requestFocus() is final in View class, so we handle keyboard hiding elsewhere
        
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            // Handle all key events directly without triggering input methods
            return super.dispatchKeyEvent(event);
        }
        
        @Override
        public boolean onTouchEvent(android.view.MotionEvent event) {
            // Handle touch events but prevent keyboard from showing
            boolean result = super.onTouchEvent(event);
            hideKeyboard();
            return result;
        }
        
        @Override
        protected void onFocusChanged(boolean focused, int direction, android.graphics.Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (focused) {
                hideKeyboard();
            }
        }
        
        private void hideKeyboard() {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                imm.hideSoftInputFromWindow(getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Remove title bar and make fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Hide navigation bar and status bar for true fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        // Get URL from intent
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        
        if (url == null || url.isEmpty()) {
            finish();
            return;
        }
        
        // Create and configure custom WebView (prevents virtual keyboard)
        webView = new CustomWebView(this);
        webView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        
        // Remote Desktop specific optimizations
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Enable better input handling for remote desktop
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();
        
        // Set WebView background to prevent white flash
        webView.setBackgroundColor(Color.BLACK);
        
        // Set WebViewClient to handle page navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Hide system UI again after page load
                hideSystemUI();
            }
        });
        
        // Set WebChromeClient for better remote desktop support
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                // Handle fullscreen video/content if needed
            }
        });
        
        setContentView(webView);
        
        // Aggressive keyboard suppression for remote desktop
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN 
                                   | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        
        // Additional keyboard suppression using reflection (for stubborn keyboards)
        try {
            Method method = webView.getClass().getMethod("setShowSoftInputOnFocus", boolean.class);
            method.setAccessible(true);
            method.invoke(webView, false);
        } catch (Exception e) {
            // Fallback if reflection fails
            webView.setFocusable(true);
            webView.setFocusableInTouchMode(true);
        }
        
        // Force hide any existing keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(webView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        
        // Load the URL
        webView.loadUrl(url);
    }
    
    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
            // Also force hide keyboard when window gains focus
            forceHideKeyboard();
        }
    }
    
    @Override
    public void onBackPressed() {
        // Disabled for Android TV - prevents accidental closure via right-click/back button
        // If you need to enable back navigation, uncomment the code below:
        /*
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
        */
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /* 
         * Android TV Remote Control Key Mappings for Remote Desktop:
         * - Right Button → Shift+F10 (Context Menu)
         * - Center/OK Button → Enter Key (pass through)
         * - Back Button → Disabled (prevents accidental exit)
         * - Menu Button → Disabled (prevents interference)
         * - All other keys → Pass through to remote desktop
         */
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                // Prevent back button and escape key from closing the webview
                return true;
            case KeyEvent.KEYCODE_MENU:
                // Prevent menu key from interfering
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Android TV Remote: Right button → Shift+F10 (Context Menu)
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    simulateShiftF10();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // Android TV Remote: Center/OK button → Enter key
                if (webView != null) {
                    return webView.dispatchKeyEvent(event);
                }
                return true;
            default:
                // For remote desktop: let WebView handle all other key events
                if (webView != null) {
                    return webView.dispatchKeyEvent(event);
                }
                return super.onKeyDown(keyCode, event);
        }
    }
    
    private void simulateShiftF10() {
        if (webView == null) return;
        
        long eventTime = System.currentTimeMillis();
        
        // Create Shift key down event
        KeyEvent shiftDown = new KeyEvent(
            eventTime, eventTime,
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_SHIFT_LEFT,
            0, KeyEvent.META_SHIFT_ON
        );
        
        // Create F10 key down event with Shift modifier
        KeyEvent f10Down = new KeyEvent(
            eventTime, eventTime,
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_F10,
            0, KeyEvent.META_SHIFT_ON
        );
        
        // Create F10 key up event with Shift modifier
        KeyEvent f10Up = new KeyEvent(
            eventTime, eventTime,
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_F10,
            0, KeyEvent.META_SHIFT_ON
        );
        
        // Create Shift key up event
        KeyEvent shiftUp = new KeyEvent(
            eventTime, eventTime,
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_SHIFT_LEFT,
            0, 0
        );
        
        // Send the key sequence to WebView
        webView.dispatchKeyEvent(shiftDown);
        webView.dispatchKeyEvent(f10Down);
        webView.dispatchKeyEvent(f10Up);
        webView.dispatchKeyEvent(shiftUp);
        
        // Debug log
        android.util.Log.d("FullscreenWebView", "Simulated Shift+F10 for context menu");
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // For remote desktop applications: prioritize WebView input handling
        if (webView != null && webView.hasFocus()) {
            // Let WebView handle the key event first
            if (webView.dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.requestFocus();
            
            // Aggressive keyboard hiding for remote desktop
            forceHideKeyboard();
            
            // Additional suppression using reflection
            try {
                Method method = webView.getClass().getMethod("setShowSoftInputOnFocus", boolean.class);
                method.setAccessible(true);
                method.invoke(webView, false);
            } catch (Exception e) {
                // Silent fail
            }
        }
    }
    
    private void forceHideKeyboard() {
        // Multiple approaches to ensure keyboard stays hidden
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(webView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            imm.hideSoftInputFromWindow(webView.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            imm.hideSoftInputFromWindow(webView.getWindowToken(), 0);
        }
        
        // Set window flags to prevent keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN 
                                   | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}