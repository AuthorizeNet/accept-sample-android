package net.authorize.acceptsdk.sampleapp.androidpay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import net.authorize.acceptsdk.sampleapp.Constants;
import net.authorize.acceptsdk.sampleapp.R;

public class ConfirmationActivity extends BaseActivity {

    private static final int REQUEST_CODE_CHANGE_MASKED_WALLET = 1002;

    private SupportWalletFragment mWalletFragment;
    private MaskedWallet mMaskedWallet;

    private TextView itemNameView;
    private TextView itemPriceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);
        mMaskedWallet = getIntent().getParcelableExtra(Constants.EXTRA_MASKED_WALLET);
        setupViews();
        createAndAddWalletFragment();
    }

    private void setupViews(){
        itemNameView = (TextView) findViewById(R.id.item_name_checkout);
        itemPriceView = (TextView) findViewById(R.id.item_price_checkout);

        itemNameView.setText(MposTransaction.getInstance().getItemInfo().getName());
        itemPriceView.setText("$" + MposTransaction.getInstance().getItemInfo().getTotalPrice());
    }

    private void createAndAddWalletFragment() {
        WalletFragmentStyle walletFragmentStyle = new WalletFragmentStyle()
                .setMaskedWalletDetailsTextAppearance(
                        R.style.WalletFragmentDetailsTextAppearance)
                .setMaskedWalletDetailsHeaderTextAppearance(
                        R.style.WalletFragmentDetailsHeaderTextAppearance)
                .setMaskedWalletDetailsBackgroundColor(
                        getResources().getColor(R.color.very_lighter_gray));

        WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .setFragmentStyle(walletFragmentStyle)
                .setTheme(WalletConstants.THEME_LIGHT)
                .setMode(WalletFragmentMode.SELECTION_DETAILS)
                .build();
        mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);

        // Now initialize the Wallet Fragment
        String accountName = Constants.API_LOGIN_ID;
        WalletFragmentInitParams.Builder startParamsBuilder = WalletFragmentInitParams.newBuilder()
                .setMaskedWallet(mMaskedWallet)
                .setMaskedWalletRequestCode(REQUEST_CODE_CHANGE_MASKED_WALLET)
                .setAccountName(accountName);
        mWalletFragment.initialize(startParamsBuilder.build());

        // add Wallet fragment to the UI
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.dynamic_wallet_masked_wallet_fragment, mWalletFragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHANGE_MASKED_WALLET:
                if (resultCode == RESULT_OK &&
                        data.hasExtra(WalletConstants.EXTRA_MASKED_WALLET)) {
                    mMaskedWallet = data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                    ((FullWalletConfirmationButtonFragment) getResultTargetFragment())
                            .updateMaskedWallet(mMaskedWallet);
                }
                // you may also want to use the new masked wallet data here, say to recalculate
                // shipping or taxes if shipping address changed
                break;
            case WalletConstants.RESULT_ERROR:
                int errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, 0);
                handleError(errorCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }



    @Override
    protected Fragment getResultTargetFragment() {
        return getSupportFragmentManager().findFragmentById(
                R.id.full_wallet_confirmation_button_fragment);
    }
}
