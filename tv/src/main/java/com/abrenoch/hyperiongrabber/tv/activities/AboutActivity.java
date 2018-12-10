package com.abrenoch.hyperiongrabber.tv.activities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.abrenoch.hyperiongrabber.tv.R;

public class AboutActivity extends Activity {
    LinearLayout mResourcesLinearLayout;
    LinearLayout mDevelopersLinearLayout;
    LinearLayout mTranslatorsLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setTitle(R.string.about_title);

        Context context = getApplicationContext();
        String versionName = null;
        int versionCode = 0;

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (versionCode != 0 && versionName != null) {
            Resources res = getResources();
            String versionText = String.format(res.getString(R.string.full_version_label), versionName,
                    String.valueOf(versionCode));
            TextView versionTextView = findViewById(R.id.appVersion);
            versionTextView.setText(versionText);
        }

        mResourcesLinearLayout = findViewById(R.id.resourcesLayout);
        mDevelopersLinearLayout = findViewById(R.id.developersLayout);
//        mTranslatorsLinearLayout = findViewById(R.id.translatorsLayout);

        fillValues(R.array.resource_links, mResourcesLinearLayout);
        fillValues(R.array.names_developers, mDevelopersLinearLayout);
//        fillValues(R.array.names_translators, mTranslatorsLinearLayout);
    }

    private void fillValues(int resourceID, LinearLayout target) {
        Resources res = getResources();
        String[] resourceVals = res.getStringArray(resourceID);
        for (String resourceVal : resourceVals) {
            Spanned sp = Html.fromHtml( resourceVal);
            target.addView( newListItem( sp ) );
        }
    }

    private TextView newListItem(Spanned text) {
        TextView tv = (TextView) getLayoutInflater().inflate(R.layout.activity_list_item, null);
        tv.setText(text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        return tv;
    }
}
