package net.authorize.acceptsdk.sampleapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import net.authorize.acceptsdk.sampleapp.accept.AcceptFragment;
import net.authorize.acceptsdk.sampleapp.androidpay.BaseActivity;
import net.authorize.acceptsdk.sampleapp.androidpay.MposTransaction;
import net.authorize.acceptsdk.sampleapp.androidpay.WalletUtil;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Base64;

//import org.apache.commons.codec.binary.Base64;

public class CheckoutActivity extends BaseActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
  private static final String TAG_FRAGMENT_CHECKOUT = "TAG_FRAGMENT_CHECKOUT";

  private static final String TAG = "CheckoutActivity";

  // TODO: replace this string with your Stripe Publishable key
  public static final String PUBLISHABLE_KEY_STRIPE = "pk_test_3pbo0MsOST3FEBb7ORBQwzc0";
  // Faizan: My personal Publishable Stripe key

  //public static final String STRIPE_VERSION = com.stripe.Stripe.VERSION;
  public static final String GATEWAY_STRIPE = "stripe";
  private static final int REQUEST_CODE_MASKED_WALLET = 1001;
  private PaymentMethodTokenizationParameters mPaymentMethodParameters;

  protected GoogleApiClient mGoogleApiClient;
  private SupportWalletFragment mWalletFragment;

  private TextView itemNameView;
  private TextView itemPriceView;
  private FrameLayout walletButtonFragmentLayout;
  private LinearLayout notReadyLayout;
  private Button checkoutButton;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_checkout);
    setupViews();
    //createStripePaymentMethodParameters();
    createNetworkTokenPaymentMethodParameters();

    String accountName = Constants.API_LOGIN_ID;
    // Set up an API client to make 'isReadyToPay' check
    mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(Wallet.API,
            new Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .setTheme(WalletConstants.THEME_LIGHT)
                .build())
        .build();
  }

  private void setupViews() {
    itemNameView = (TextView) findViewById(R.id.item_name_checkout);
    itemPriceView = (TextView) findViewById(R.id.item_price_checkout);

    itemNameView.setText(MposTransaction.getInstance().getItemInfo().getName());
    itemPriceView.setText("$" + MposTransaction.getInstance().getItemInfo().getTotalPrice());

    walletButtonFragmentLayout = (FrameLayout) findViewById(R.id.dynamic_wallet_button_fragment);
    notReadyLayout = (LinearLayout) findViewById(R.id.not_ready_to_pay);
    checkoutButton = (Button) findViewById(R.id.button_regular_checkout);
    checkoutButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        launchAcceptFragment();
      }
    });
  }

  private void launchAcceptFragment() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    AcceptFragment checkoutFragment =
        (AcceptFragment) fragmentManager.findFragmentByTag(TAG_FRAGMENT_CHECKOUT);
    if (checkoutFragment == null) {
      checkoutFragment = new AcceptFragment();
      fragmentManager.beginTransaction()
          .replace(R.id.layout_container, checkoutFragment, TAG_FRAGMENT_CHECKOUT)
          .commit();
    }
  }

  @Override public void onStart() {
    super.onStart();

    // Connect to Google Play Services
    mGoogleApiClient.connect();
  }

  @Override public void onStop() {
    super.onStop();

    // Disconnect from Google Play Services
    mGoogleApiClient.disconnect();
  }

  @Override public void onConnected(Bundle connectionHint) {
    checkIsReadyToPayWithAndroidPay();
  }

  @Override public void onConnectionSuspended(int cause) {
    // don't need to do anything here
  }

  @Override public void onConnectionFailed(ConnectionResult result) {
    // don't need to do anything here
  }

  void checkIsReadyToPayWithAndroidPay() {
    if (mGoogleApiClient.isConnected()) {
      Wallet.Payments.isReadyToPay(mGoogleApiClient)
          .setResultCallback(new ResultCallback<BooleanResult>() {
            public void onResult(BooleanResult result) {
              if (result.getStatus().isSuccess()) {
                Log.d(TAG, "isReadyToPay = " + result.getValue());
                if (result.getValue()) {
                  Log.e(TAG, "User isReadyToPay");
                  toggleVisibility(true);
                  createAndAddWalletFragment();
                } else {
                  Log.e(TAG, "User is NOT ReadyToPay");
                  toggleVisibility(false);
                }
              } else { // handle error
                Log.e(TAG, "isReadyToPay failed error=" + result.getStatus().getStatusCode());
              }
            }
          });
    } else {
      Log.e(TAG, "GoogleApiClient is not connected");
    }
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // retrieve the error code, if available
    int errorCode = -1;
    if (data != null) {
      errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
    }
    switch (requestCode) {
      case REQUEST_CODE_MASKED_WALLET:
        switch (resultCode) {
          case Activity.RESULT_OK:
            MaskedWallet maskedWallet =
                data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
            Toast.makeText(this, "Got Masked Wallet", Toast.LENGTH_SHORT).show();
            launchConfirmationPage(maskedWallet);
            break;
          case Activity.RESULT_CANCELED:
            break;
          default:
            handleError(errorCode);
            break;
        }
        break;
      case WalletConstants.RESULT_ERROR:
        handleError(errorCode);
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
        break;
    }
  }

  private void createAndAddWalletFragment() {
    WalletFragmentStyle walletFragmentStyle =
        new WalletFragmentStyle().setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
            .setBuyButtonAppearance(
                WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_LIGHT_WITH_BORDER)
            .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);

    WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
        .setFragmentStyle(walletFragmentStyle)
        .setTheme(WalletConstants.THEME_LIGHT)
        //.setMode(WalletFragmentMode.BUY_BUTTON) //FIXME: May need to enable this
        .build();

    mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);

    MaskedWalletRequest maskedWalletRequest;
    // make the following call if you want to setup a PaymentTokenizationMethod for a specific Gateway
    createNetworkTokenPaymentMethodParameters();
    if (mPaymentMethodParameters != null) {
      maskedWalletRequest =
          WalletUtil.createTokenizedMaskedWalletRequest(MposTransaction.getInstance().getItemInfo(),
              mPaymentMethodParameters);
    } else {
      maskedWalletRequest =
          WalletUtil.createMaskedWalletRequest(MposTransaction.getInstance().getItemInfo());
    }

    WalletFragmentInitParams.Builder startParamsBuilder = WalletFragmentInitParams.newBuilder()
        .setMaskedWalletRequest(maskedWalletRequest)
        .setMaskedWalletRequestCode(REQUEST_CODE_MASKED_WALLET)
        .setAccountName(Constants.API_LOGIN_ID);
    mWalletFragment.initialize(startParamsBuilder.build());

    // add Wallet fragment to the UI
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.dynamic_wallet_button_fragment, mWalletFragment)
        .commit();
  }

  private void toggleVisibility(boolean ready) {
    if (ready) {
      if (walletButtonFragmentLayout.getVisibility() != View.VISIBLE) {
        walletButtonFragmentLayout.setVisibility(View.VISIBLE);
      }

      if (notReadyLayout.getVisibility() == View.VISIBLE) notReadyLayout.setVisibility(View.GONE);
    } else {
      if (walletButtonFragmentLayout.getVisibility() == View.VISIBLE) {
        walletButtonFragmentLayout.setVisibility(View.GONE);
      }

      if (notReadyLayout.getVisibility() != View.VISIBLE) {
        notReadyLayout.setVisibility(View.VISIBLE);
      }
    }
  }

  public void createStripePaymentMethodParameters() {
    mPaymentMethodParameters = PaymentMethodTokenizationParameters.newBuilder()
        .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.PAYMENT_GATEWAY)
        .addParameter(getString(R.string.gateway), GATEWAY_STRIPE)
        .addParameter("stripe:publishableKey", PUBLISHABLE_KEY_STRIPE)
        //.addParameter("stripe:version", STRIPE_VERSION)
        .build();
  }

  private void createNetworkTokenPaymentMethodParameters() {
    mPaymentMethodParameters = PaymentMethodTokenizationParameters.newBuilder()
        .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.NETWORK_TOKEN)
        .addParameter("publicKey", Constants.PUBLIC_KEY_SEC)

        //  .addParameter("publicKey", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGswOubUOZchQkOJPt41PMduC4Ma5ldBTIhZUMINvuTuQOrWIl3GhLDEl/75hFoWRkp+d+KBism9+1LBBa2gBnw==") // "[BASE 64 encoded publicKey]" - public encryption key
        //  .addParameter("publicKey", "BOdoXP+9Aq473SnGwg3JU1aiNpsd9vH2ognq4PtDtlLGa3Kj8TPf+jaQNPyDSkh3JUhiS0KyrrlWhAgNZKHYF2Y=") // "[BASE 64 encoded publicKey from Google]" - public encryption key
        //.addParameter("publicKey", "BJgo1Bk2yNhyI2O36wT6RddW/U2rexn5ymMrb2XZrFAIKk2gOaX0BvIenUEYfkqWXILu/blOXUZGMujoM/VW9Y8=") // "[BASE 64 encoded publicKey from Yinghao]" - public encryption key
        //.addParameter("publicKey", "BBrMDrm1DmXIUJDiT7eNTzHbguDGuZXQUyIWVDCDb7k7kDq1iJdxoSwxJf++YRaFkZKfnfigYrJvftSwQWtoAZ8=") // "[BASE 64 encoded publicKey from Chuong]" - public encryption key
        // .addParameter("publicKey", convertPublicKeyToPointEncoded()) //  Public key point
        .build();
  }

  public String convertPublicKeyToPointEncoded() {
    // setup SpongyCastle as the JCE provider
    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    String result = "";
    try {
      byte[] pubKeyBytes = Base64.decode(Constants.PUBLIC_KEY_SEC);
      KeyFactory ecKeyFactory = KeyFactory.getInstance("EC", "SC");
      X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(pubKeyBytes);
      PublicKey publicKey = ecKeyFactory.generatePublic(encodedKeySpec);
      ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
      ECPoint ecPoint = ecPublicKey.getQ();

      byte[] pointBytes = ecPoint.getEncoded(false);
      result = Base64.toBase64String(pointBytes);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (InvalidKeySpecException e) {
      e.printStackTrace();
    }
    return result;
  }

  private void launchConfirmationPage(MaskedWallet maskedWallet) {
    //FIXME : Need to check is it needed or not
    //Intent intent = new Intent(this, ConfirmationActivity.class);
    //intent.putExtra(Constants.EXTRA_MASKED_WALLET, maskedWallet);
    //startActivity(intent);
  }

  @Override protected Fragment getResultTargetFragment() {
    return null;
  }
}
