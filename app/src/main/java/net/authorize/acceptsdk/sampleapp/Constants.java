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

package net.authorize.acceptsdk.sampleapp;

import com.google.android.gms.wallet.WalletConstants;

/**
 * Constants used by Google Wallet SDK Sample.
 */
public class Constants {

    // API LOGIN ID to be used with Signature based authn/authz
    public static String API_LOGIN_ID = "android_pay_sample_login_id"; // replace with YOUR_API_LOGIN_ID

    // Public SEC key
    public static final String PUBLIC_KEY_SEC = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGswOubUOZchQkOJPt41PMduC4Ma5ldBTIhZUMINvuTuQOrWIl3GhLDEl/75hFoWRkp+d+KBism9+1LBBa2gBnw==";
    //public static final String PUBLIC_KEY_SEC = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEmCjUGTbI2HIjY7frBPpF11b9Tat7GfnKYytvZdmsUAgqTaA5pfQG8h6dQRh+SpZcgu79uU5dRkYy6Ogz9Vb1jw==";

    // Environment to use when creating an instance of Wallet.WalletOptions
    public static final int WALLET_ENVIRONMENT = WalletConstants.ENVIRONMENT_SANDBOX;

    public static final String MERCHANT_NAME = "AndroidPay Sample Merchant";

    // Intent extra keys
    public static final String EXTRA_ITEM_ID = "EXTRA_ITEM_ID";
    public static final String EXTRA_MASKED_WALLET = "EXTRA_MASKED_WALLET";
    public static final String EXTRA_FULL_WALLET = "EXTRA_FULL_WALLET";

    public static final String CURRENCY_CODE_USD = "USD";

    // values to use with KEY_DESCRIPTION
    public static final String DESCRIPTION_LINE_ITEM_SHIPPING = "Shipping";
    public static final String DESCRIPTION_LINE_ITEM_TAX = "Tax";

    public static final String EXTRA_ANDROID_PAY_CALLBACK_ACTION = "EXTRA_ANDROID_PAY_CALLBACK_ACTION";

}
