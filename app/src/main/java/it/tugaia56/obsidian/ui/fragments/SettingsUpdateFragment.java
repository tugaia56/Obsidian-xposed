package it.tugaia56.obsidian.ui.fragments;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SwitchWidgetAdapter;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;

/**
 * "Aggiornamento" — controllo reale via GitHub Releases API (repo tugaia56/Obsidian-xposed,
 * lo stesso che [[project_ci_cd_release_automation]] pubblica ad ogni tag). Confronta
 * BuildConfig.VERSION_NAME col tag_name della release più recente, mostra il changelog
 * (release body) e scarica/installa l'APK allegato tramite DownloadManager.
 *
 * Controllo automatico/Solo Wi-Fi restano placeholder: nessun controllo periodico in
 * background (richiederebbe WorkManager) — solo il pulsante "Controlla aggiornamenti" è reale.
 */
public class SettingsUpdateFragment extends Fragment {

    private static final String API_URL =
            "https://api.github.com/repos/tugaia56/Obsidian-xposed/releases/latest";

    private RecyclerView mRv;
    private long mDownloadId = -1;
    private BroadcastReceiver mDownloadReceiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setPadding(0, 8, 0, 24);
        rv.setClipToPadding(false);
        return rv;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRv = (RecyclerView) view;

        ListWidgetAdapter checkAdapter = new ListWidgetAdapter(List.of(
                new ListWidgetAdapter.ListItem(
                        getString(R.string.settings_check_updates),
                        BuildConfig.VERSION_NAME,
                        this::checkForUpdates)));

        SwitchWidgetAdapter.SwitchItem autoUpdateItem = new SwitchWidgetAdapter.SwitchItem(
                getString(R.string.settings_auto_update),
                getString(R.string.settings_auto_update_summary) + getString(R.string.wip_inline_suffix),
                ObsidianPrefs.getBoolean("PLACEHOLDER_auto_update", false), null);
        autoUpdateItem.onChanged = () ->
                ObsidianPrefs.putBoolean("PLACEHOLDER_auto_update", autoUpdateItem.checked);

        SwitchWidgetAdapter.SwitchItem wifiOnlyItem = new SwitchWidgetAdapter.SwitchItem(
                getString(R.string.settings_update_wifi_only),
                getString(R.string.settings_update_wifi_only_summary) + getString(R.string.wip_inline_suffix),
                ObsidianPrefs.getBoolean("PLACEHOLDER_update_wifi_only", false), null);
        wifiOnlyItem.onChanged = () ->
                ObsidianPrefs.putBoolean("PLACEHOLDER_update_wifi_only", wifiOnlyItem.checked);

        SwitchWidgetAdapter switchAdapter = new SwitchWidgetAdapter(List.of(autoUpdateItem, wifiOnlyItem));

        mRv.setAdapter(new ConcatAdapter(checkAdapter, switchAdapter));
    }

    // ── Controlla aggiornamenti (GitHub Releases API) ───────────────────────
    private void checkForUpdates() {
        Toast.makeText(requireContext(), R.string.update_checking, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                JSONObject release = fetchLatestRelease();
                String tag = release.getString("tag_name"); // es. "v1.0.3"
                String latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
                String changelog = release.optString("body", "");

                String downloadUrl = null;
                JSONArray assets = release.getJSONArray("assets");
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    if (asset.optString("name", "").endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url");
                        break;
                    }
                }
                final String finalDownloadUrl = downloadUrl;
                boolean isNewer = compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0;

                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isNewer) {
                        Toast.makeText(requireContext(), R.string.update_up_to_date, Toast.LENGTH_SHORT).show();
                    } else if (finalDownloadUrl == null) {
                        Toast.makeText(requireContext(), R.string.update_no_apk_asset, Toast.LENGTH_SHORT).show();
                    } else {
                        showUpdateDialog(latestVersion, changelog, finalDownloadUrl);
                    }
                });
            } catch (Throwable t) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.update_check_failed, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private JSONObject fetchLatestRelease() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return new JSONObject(sb.toString());
        } finally {
            conn.disconnect();
        }
    }

    /** Confronta due versioni "1.2.10" / "1.2.9" numero per numero, non lessicograficamente. */
    private int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int vb = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Throwable t) {
            return 0;
        }
    }

    private void showUpdateDialog(String version, String changelog, String downloadUrl) {
        String message = getString(R.string.update_available_body, version);
        if (changelog != null && !changelog.isBlank()) message += "\n\n" + changelog;
        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.update_available_title)
                .setMessage(message)
                .setPositiveButton(R.string.update_word, (d, w) -> startDownload(downloadUrl, version))
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    // ── Download + installazione ────────────────────────────────────────────
    private void startDownload(String url, String version) {
        DownloadManager dm = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.settings_check_updates))
                .setMimeType("application/vnd.android.package-archive")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Obsidian-" + version + ".apk")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        mDownloadId = dm.enqueue(request);
        Toast.makeText(requireContext(), R.string.update_download_started, Toast.LENGTH_SHORT).show();

        mDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (completedId != mDownloadId) return;
                try { requireContext().unregisterReceiver(this); } catch (Throwable ignored) {}
                mDownloadReceiver = null;
                promptInstall(dm, completedId);
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(mDownloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            requireContext().registerReceiver(mDownloadReceiver, filter);
        }
    }

    private void promptInstall(DownloadManager dm, long downloadId) {
        try {
            Uri uri = dm.getUriForDownloadedFile(downloadId);
            if (uri == null) {
                Toast.makeText(requireContext(), R.string.update_download_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent install = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), R.string.update_download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDownloadReceiver != null) {
            try { requireContext().unregisterReceiver(mDownloadReceiver); } catch (Throwable ignored) {}
            mDownloadReceiver = null;
        }
    }
}
