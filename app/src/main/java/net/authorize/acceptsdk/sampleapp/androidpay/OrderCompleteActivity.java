/*
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.authorize.acceptsdk.sampleapp.androidpay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.PaymentMethodToken;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.authorize.acceptsdk.sampleapp.CheckoutActivity;
import net.authorize.acceptsdk.sampleapp.Constants;
import net.authorize.acceptsdk.sampleapp.R;

/**
 * Displays the credentials received in the {@code FullWallet}.
 */
public class OrderCompleteActivity extends Activity implements OnClickListener {

    private TextView androidPayBlobView;
    private TextView secBlobView;
    private FullWallet mFullWallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_complete);
        androidPayBlobView = (TextView) findViewById(R.id.encrypted_data_view_android_pay);
        secBlobView = (TextView) findViewById(R.id.encrypted_data_view_sec);

        mFullWallet = getIntent().getParcelableExtra(Constants.EXTRA_FULL_WALLET);
        Button continueButton = (Button) findViewById(R.id.button_continue_shopping);
        continueButton.setOnClickListener(this);
        populateEncryptedBlobs();
    }

    private void populateEncryptedBlobs(){
        // Get payment method token, if it exists
        PaymentMethodToken paymentMethodToken = mFullWallet.getPaymentMethodToken();
        if (paymentMethodToken != null) {
            String tokenJSON = paymentMethodToken.getToken();
            if (tokenJSON != null) {
                Log.d("AndroidPay", "AndroidPay token before encode :" + tokenJSON);
                String blob = getBase64Blob(tokenJSON);
                Log.d("AndroidPay", "AndroidPay Blob" + blob);

                String anetBlob = createSecServiceJson(blob);
                anetBlob = getBase64Blob(anetBlob);
                Log.d("ANet OpaqueData Blob" , anetBlob);
                androidPayBlobView.setText(anetBlob);
            }
            return;
        }
    }

    private String getBase64Blob(String token) {
        byte[] encodedTokenBytes = Base64.encode(token.getBytes(), Base64.NO_WRAP);
        String encodedToken = new String(encodedTokenBytes);
        return encodedToken;
        //return new String(encoded);
    }

    private String createSecServiceJson(String androidPayBlob){
        String secBlob = "{\"publicKeyHash\": \"" + Constants.PUBLIC_KEY_HASH + "\"," +
                "\"version\": \"1.0\"," +
                "\"data\":" + "\"" + androidPayBlob + "\"}";
        return secBlob;
    }


    @Override
    public void onClick(View v) {
        //FIXME : Need to fix this
        Intent intent = new Intent(OrderCompleteActivity.this, CheckoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        OrderCompleteActivity.this.startActivity(intent);
    }
}
