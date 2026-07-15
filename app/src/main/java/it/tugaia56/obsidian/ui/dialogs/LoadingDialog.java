package it.tugaia56.obsidian.ui.dialogs;
import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import it.tugaia56.obsidian.R;
public class LoadingDialog {
    private final Activity activity;
    private Dialog dialog;
    public LoadingDialog(Activity activity) { this.activity = activity; }
    public void show(String message) {
        if (dialog != null && dialog.isShowing()) return;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null);
        ((TextView) view.findViewById(R.id.loadingText)).setText(message);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.show();
    }
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
