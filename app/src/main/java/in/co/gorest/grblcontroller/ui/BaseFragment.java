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

package in.co.gorest.grblcontroller.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardedVideoAd;

import in.co.gorest.grblcontroller.GrblController;
import in.co.gorest.grblcontroller.R;

public class BaseFragment extends Fragment {

    private InterstitialAd interstitialAd;
    private RewardedVideoAd rewardedVideoAd;

    OnFragmentInteractionListener fragmentInteractionListener;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(GrblController.getInstance().isFreeVersion()){
            interstitialAd = new InterstitialAd(getActivity());
            interstitialAd.setAdUnitId(getString(R.string.admob_interstitial_ad_id));
            interstitialAd.loadAd(new AdRequest.Builder().build());

            rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(getActivity());
            rewardedVideoAd.loadAd(getString(R.string.admob_reward_video_ad_id), new AdRequest.Builder().build());
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        displayInterstitialAd();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            fragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    public interface OnFragmentInteractionListener {
        void onGcodeCommandReceived(String command);
        void onGrblRealTimeCommandReceived(byte command);
        void vibrateShort();
        void vibrateLong();
    }

    public void displayInterstitialAd(){
        if(GrblController.getInstance().isFreeVersion() && interstitialAd.isLoaded()){
            interstitialAd.show();
        }
    }

    public void displayRewardVideoAd(){
        if(GrblController.getInstance().isFreeVersion() && rewardedVideoAd.isLoaded()){
            rewardedVideoAd.show();
        }
    }

}
