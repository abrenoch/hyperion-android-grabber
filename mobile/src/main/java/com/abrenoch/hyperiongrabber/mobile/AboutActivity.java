package com.abrenoch.hyperiongrabber.mobile;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {
    LinearLayout mResourcesLinearLayout;
    LinearLayout mDevelopersLinearLayout;
    LinearLayout mTranslatorsLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mResourcesLinearLayout = findViewById(R.id.resources_layout);
        mDevelopersLinearLayout = findViewById(R.id.developers_layout);
        mTranslatorsLinearLayout = findViewById(R.id.translators_layout);

        fillValues(R.array.resource_links, mResourcesLinearLayout);
        fillValues(R.array.names_developers, mDevelopersLinearLayout);
        fillValues(R.array.names_translators, mTranslatorsLinearLayout);
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
