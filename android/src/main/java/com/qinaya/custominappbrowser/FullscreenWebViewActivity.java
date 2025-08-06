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
import android.view.ContextMenu;

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
        
        // ADD THIS: Enable context menu support
        webView.setLongClickable(true);
        webView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                // Context menu will be created by the web page's JavaScript
                android.util.Log.d("FullscreenWebView", "Context menu created");
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
        // ADD LOGGING to debug key events
        android.util.Log.d("FullscreenWebView", "Key pressed: " + keyCode + " (" + KeyEvent.keyCodeToString(keyCode) + ")");
        
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
                android.util.Log.d("FullscreenWebView", "RIGHT button detected - triggering context menu");
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
        
        android.util.Log.d("FullscreenWebView", "Simulating Shift+F10 for virtual computer context menu");
        
        // For virtual computers/remote desktop: Send proper keyboard events via JavaScript
        // This ensures the virtual computer receives the actual key presses
        webView.evaluateJavascript(
            "(function() {" +
            "  console.log('Sending Shift+F10 to virtual computer');" +
            "  " +
            "  // Method 1: Dispatch keyboard events that virtual computers can intercept" +
            "  var shiftDownEvent = new KeyboardEvent('keydown', {" +
            "    key: 'Shift'," +
            "    code: 'ShiftLeft'," +
            "    keyCode: 16," +
            "    which: 16," +
            "    shiftKey: true," +
            "    bubbles: true," +
            "    cancelable: true" +
            "  });" +
            "  " +
            "  var f10DownEvent = new KeyboardEvent('keydown', {" +
            "    key: 'F10'," +
            "    code: 'F10'," +
            "    keyCode: 121," +
            "    which: 121," +
            "    shiftKey: true," +
            "    bubbles: true," +
            "    cancelable: true" +
            "  });" +
            "  " +
            "  var f10UpEvent = new KeyboardEvent('keyup', {" +
            "    key: 'F10'," +
            "    code: 'F10'," +
            "    keyCode: 121," +
            "    which: 121," +
            "    shiftKey: true," +
            "    bubbles: true," +
            "    cancelable: true" +
            "  });" +
            "  " +
            "  var shiftUpEvent = new KeyboardEvent('keyup', {" +
            "    key: 'Shift'," +
            "    code: 'ShiftLeft'," +
            "    keyCode: 16," +
            "    which: 16," +
            "    shiftKey: false," +
            "    bubbles: true," +
            "    cancelable: true" +
            "  });" +
            "  " +
            "  // Send events to the document (virtual computer will receive these)" +
            "  document.dispatchEvent(shiftDownEvent);" +
            "  document.dispatchEvent(f10DownEvent);" +
            "  document.dispatchEvent(f10UpEvent);" +
            "  document.dispatchEvent(shiftUpEvent);" +
            "  " +
            "  // Method 2: Also try sending to focused element (for better compatibility)" +
            "  if (document.activeElement) {" +
            "    document.activeElement.dispatchEvent(shiftDownEvent.cloneNode ? shiftDownEvent : new KeyboardEvent('keydown', shiftDownEvent));" +
            "    document.activeElement.dispatchEvent(f10DownEvent.cloneNode ? f10DownEvent : new KeyboardEvent('keydown', f10DownEvent));" +
            "    document.activeElement.dispatchEvent(f10UpEvent.cloneNode ? f10UpEvent : new KeyboardEvent('keyup', f10UpEvent));" +
            "    document.activeElement.dispatchEvent(shiftUpEvent.cloneNode ? shiftUpEvent : new KeyboardEvent('keyup', shiftUpEvent));" +
            "  }" +
            "  " +
            "  // Method 3: For some virtual computers, also try window events" +
            "  if (window.dispatchEvent) {" +
            "    window.dispatchEvent(shiftDownEvent);" +
            "    window.dispatchEvent(f10DownEvent);" +
            "    window.dispatchEvent(f10UpEvent);" +
            "    window.dispatchEvent(shiftUpEvent);" +
            "  }" +
            "  " +
            "  console.log('Shift+F10 events sent to virtual computer');" +
            "})()", 
            null
        );
        
        // Also keep the original Android key event dispatch as backup
        // Some virtual computer solutions might still need this
        long eventTime = System.currentTimeMillis();
        
        KeyEvent shiftDown = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, KeyEvent.META_SHIFT_ON);
        KeyEvent f10Down = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F10, 0, KeyEvent.META_SHIFT_ON);
        KeyEvent f10Up = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F10, 0, KeyEvent.META_SHIFT_ON);
        KeyEvent shiftUp = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
        
        webView.dispatchKeyEvent(shiftDown);
        webView.dispatchKeyEvent(f10Down);
        webView.dispatchKeyEvent(f10Up);
        webView.dispatchKeyEvent(shiftUp);
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // MODIFY THIS: Don't intercept DPAD_RIGHT events
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // Let onKeyDown handle DPAD_RIGHT events
            return super.dispatchKeyEvent(event);
        }
        
        // For remote desktop applications: prioritize WebView input handling for other keys
        if (webView != null && webView.hasFocus()) {
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