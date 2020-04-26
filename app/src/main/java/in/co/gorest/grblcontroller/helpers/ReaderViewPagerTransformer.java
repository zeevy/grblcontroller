/*
 *  /**
 *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation; either version 2 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  * <http://www.gnu.org/licenses/>
 *
 */

package in.co.gorest.grblcontroller.helpers;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import android.view.View;

public class ReaderViewPagerTransformer implements ViewPager.PageTransformer{

    public enum TransformType {FLOW, DEPTH, ZOOM, SLIDE_OVER }
    private final TransformType mTransformType;

    public ReaderViewPagerTransformer(TransformType transformType) {
        mTransformType = transformType;
    }

    private static final float MIN_SCALE_DEPTH = 0.75f;
    private static final float MIN_SCALE_ZOOM = 0.85f;
    private static final float MIN_ALPHA_ZOOM = 0.5f;
    private static final float SCALE_FACTOR_SLIDE = 0.85f;
    private static final float MIN_ALPHA_SLIDE = 0.35f;

    public void transformPage(@NonNull View page, float position) {
        final float alpha;
        final float scale;
        final float translationX;

        switch (mTransformType) {
            case FLOW:
                page.setRotationY(position * -30f);
                return;

            case SLIDE_OVER:
                if (position < 0 && position > -1) {
                    // this is the page to the left
                    scale = Math.abs(Math.abs(position) - 1) * (1.0f - SCALE_FACTOR_SLIDE) + SCALE_FACTOR_SLIDE;
                    alpha = Math.max(MIN_ALPHA_SLIDE, 1 - Math.abs(position));
                    int pageWidth = page.getWidth();
                    float translateValue = position * -pageWidth;
                    if (translateValue > -pageWidth) {
                        translationX = translateValue;
                    } else {
                        translationX = 0;
                    }
                } else {
                    alpha = 1;
                    scale = 1;
                    translationX = 0;
                }
                break;

            case DEPTH:
                if (position > 0 && position < 1) {
                    // moving to the right
                    alpha = (1 - position);
                    scale = MIN_SCALE_DEPTH + (1 - MIN_SCALE_DEPTH) * (1 - Math.abs(position));
                    translationX = (page.getWidth() * -position);
                } else {
                    // use default for all other cases
                    alpha = 1;
                    scale = 1;
                    translationX = 0;
                }
                break;

            case ZOOM:
                if (position >= -1 && position <= 1) {
                    scale = Math.max(MIN_SCALE_ZOOM, 1 - Math.abs(position));
                    alpha = MIN_ALPHA_ZOOM +
                            (scale - MIN_SCALE_ZOOM) / (1 - MIN_SCALE_ZOOM) * (1 - MIN_ALPHA_ZOOM);
                    float vMargin = page.getHeight() * (1 - scale) / 2;
                    float hMargin = page.getWidth() * (1 - scale) / 2;
                    if (position < 0) {
                        translationX = (hMargin - vMargin / 2);
                    } else {
                        translationX = (-hMargin + vMargin / 2);
                    }
                } else {
                    alpha = 1;
                    scale = 1;
                    translationX = 0;
                }
                break;

            default:
                return;
        }

        page.setAlpha(alpha);
        page.setTranslationX(translationX);
        page.setScaleX(scale);
        page.setScaleY(scale);
    }

}
