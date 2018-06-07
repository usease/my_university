package com.example.usease.myuniversity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Locale;

class TabViewPagerAdapater extends FragmentPagerAdapter {

    private String titles[];
    private Fragment frags[];
    private Locale locale;

    public TabViewPagerAdapater(FragmentManager fm, Context context, Locale locale) {
        super(fm);

//        Resources resources = context.getResources();
//        titles = resources.getStringArray(R.array.titles1);
//
//        frags = new Fragment[titles.length];
//
//        frags[0] = new GroupmatesFragment();
//        frags[1] = new ChatsFragment();
//        frags[2] = new RequestsFragment();

        this.locale = locale;

    }
    //This method responsible for the creation of fragments. Those fragments used for layout creation fo tabs
    @Override
    public Fragment getItem(int position) {
        switch  (position) {
            case 0:
                GroupmatesFragment groupmatesFragment = new GroupmatesFragment();
                return groupmatesFragment;
            case 1:
                ChatsFragment chatsFragment = new ChatsFragment();
                return chatsFragment;
            case 2:
                RequestsFragment requestsFragment = new RequestsFragment();
                return requestsFragment;
            default:
                return null;
            }
    }
    //This method is reponsible for number of items in the Tabs
    @Override
    public int getCount() {
        return 3; //Manually returning 3, since we have 3 tabs
    }

//    //Method responsible for setting titles for the tabs
//    public CharSequence getPageTitle(int position) {
//
//            switch (position) {
//                case 0:
//                    return "GROUPMATES";
//                case 1:
//                    return "CHATS";
//                case 2:
//                    return "REQUESTS";
//                default:
//                    return null;
//            }
//    }

    //Method responsible for setting titles for the tabs
    public CharSequence getPageTitle(int position) {

        if(locale == Locale.US) {
            switch (position) {
                case 0:
                    return "Groupmates";
                case 1:
                    return "Chats";
                case 2:
                    return "Requests";
                default:
                    return null;
            }
        } else {
            switch (position) {
                case 0:
                    return "Guruhdosh";
                case 1:
                    return "Chat";
                case 2:
                    return "Rikvest";
                default:
                    return null;
            }
        }
    }
}
