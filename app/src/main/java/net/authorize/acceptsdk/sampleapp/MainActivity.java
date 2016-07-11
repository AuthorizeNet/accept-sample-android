package net.authorize.acceptsdk.sampleapp;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.authorize.acceptsdk.sampleapp.fragments.AcceptCheckoutFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG_FRAGMENT_CHECKOUT = "TAG_FRAGMENT_CHECKOUT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pullWebCheckoutFragment();
    }

    private void pullWebCheckoutFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        AcceptCheckoutFragment checkoutFragment = (AcceptCheckoutFragment)
                fragmentManager.findFragmentByTag(TAG_FRAGMENT_CHECKOUT);
        if (checkoutFragment == null) {
            checkoutFragment = new AcceptCheckoutFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.accept_checkout_fragment_container, checkoutFragment, TAG_FRAGMENT_CHECKOUT)
                    .commit();
        }
    }
}
