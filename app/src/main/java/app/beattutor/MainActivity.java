package app.beattutor;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.InputStream;

/** Full-screen shell around the game page bundled in assets/index.html. */
public class MainActivity extends Activity {
    private static final int PICK_SONG = 41;
    private static final String HOST = "appassets.androidplatform.net";

    private WebView web;
    private ValueCallback<Uri[]> pendingPick;
    private volatile Uri sharedSong;   // a song another app handed to us
    private String sharedName;
    private boolean pageReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFF100D1A);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setSupportZoom(false);

        final WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (HOST.equals(url.getHost()) && "/shared/song".equals(url.getPath())) {
                    // The page fetches the shared song from here; it never touches the network.
                    Uri song = sharedSong;
                    if (song == null) return null;
                    try {
                        InputStream in = getContentResolver().openInputStream(song);
                        if (in == null) return null;
                        return new WebResourceResponse("application/octet-stream", null, in);
                    } catch (Exception e) {
                        return null;
                    }
                }
                return assets.shouldInterceptRequest(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                openSharedSong();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // The app only ever shows its own bundled page.
                return !HOST.equals(request.getUrl().getHost());
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingPick != null) pendingPick.onReceiveValue(null);
                pendingPick = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                        "audio/*", "application/ogg", "application/x-midi", "application/midi", "application/octet-stream"
                });
                try {
                    startActivityForResult(Intent.createChooser(pick, getString(R.string.choose_song)), PICK_SONG);
                } catch (Exception e) {
                    pendingPick.onReceiveValue(null);
                    pendingPick = null;
                }
                return true;
            }
        });

        readIntent(getIntent());
        if (savedInstanceState == null) {
            web.loadUrl("https://" + HOST + "/assets/index.html");
        } else {
            web.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readIntent(intent);
        openSharedSong();
    }

    /** "Open with" and "Share to" from a file manager or another app. */
    private void readIntent(Intent intent) {
        if (intent == null) return;
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (uri == null) return;
        String name = null;
        try (Cursor c = getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) {
        }
        if (name == null) name = uri.getLastPathSegment();
        sharedName = name == null ? "Shared song" : name;
        sharedSong = uri;
    }

    private void openSharedSong() {
        if (!pageReady || sharedSong == null || sharedName == null) return;
        String name = sharedName;
        sharedName = null; // hand each shared song to the page once
        web.evaluateJavascript("window.__openShared&&window.__openShared(" + JSONObject.quote(name) + ")", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SONG && pendingPick != null) {
            pendingPick.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            pendingPick = null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onBackPressed() {
        // Let the game handle Back first (pause, leave a screen); exit only from the home screen.
        web.evaluateJavascript("(window.__back&&window.__back())?1:0", value -> {
            if (!"1".equals(value)) finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
