package one.dot.couiexpressive.hooks.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.ImageView;
import de.robv.android.xposed.XposedHelpers;

public final class StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$2 extends AnimatorListenerAdapter {
    final ImageView $icon;

    public StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$2(ImageView imageView) {
        this.$icon = imageView;
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        animator.getClass();
        if (XposedHelpers.getAdditionalInstanceField(this.$icon, "coe_stock_udfps_aod_exit_animator") == animator) {
            XposedHelpers.setAdditionalInstanceField(this.$icon, "coe_stock_udfps_aod_exit_animator", (Object) null);
        }
    }
}
