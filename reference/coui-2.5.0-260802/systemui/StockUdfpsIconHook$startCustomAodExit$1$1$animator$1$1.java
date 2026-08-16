package one.dot.couiexpressive.hooks.systemui;

import android.animation.ValueAnimator;

public final class StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$1 implements ValueAnimator.AnimatorUpdateListener {
    final StockUdfpsIconHook.StockFingerprintDrawable $drawable;

    public StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$1(StockUdfpsIconHook.StockFingerprintDrawable stockFingerprintDrawable) {
        this.$drawable = stockFingerprintDrawable;
    }

    @Override
    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        valueAnimator.getClass();
        StockUdfpsIconHook.StockFingerprintDrawable stockFingerprintDrawable = this.$drawable;
        Object animatedValue = valueAnimator.getAnimatedValue();
        animatedValue.getClass();
        stockFingerprintDrawable.setAlpha(((Integer) animatedValue).intValue());
    }
}
