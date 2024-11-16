//
// DO NOT EDIT THIS FILE.
// Generated using AndroidAnnotations 4.8.0.
// 
// You can create a larger work that contains this file and distribute that work under terms of your choice.
//

package com.tomclaw.appsend.di.legacy;

import android.content.Context;

import org.androidannotations.api.view.OnViewChangedNotifier;

public final class LegacyInjector_
    extends LegacyInjector
{
    private Context context_;
    private Object rootFragment_;
    private static LegacyInjector_ instance_;

    private LegacyInjector_(Context context) {
        context_ = context;
    }

    private LegacyInjector_(Context context, Object rootFragment) {
        context_ = context;
        rootFragment_ = rootFragment;
    }

    public static LegacyInjector_ getInstance_(Context context) {
        if (instance_ == null) {
            OnViewChangedNotifier previousNotifier = OnViewChangedNotifier.replaceNotifier(null);
            instance_ = new LegacyInjector_(context.getApplicationContext());
            instance_.init_();
            OnViewChangedNotifier.replaceNotifier(previousNotifier);
        }
        return instance_;
    }

    private void init_() {
        init();
    }
}