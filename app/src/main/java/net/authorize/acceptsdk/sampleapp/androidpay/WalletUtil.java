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

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.NotifyTransactionStatusRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.authorize.acceptsdk.sampleapp.Constants;

/**
 * A helper class to create {@link MaskedWalletRequest}, {@link FullWalletRequest} as well as
 * {@link NotifyTransactionStatusRequest} objects
 */
public class WalletUtil {

  private static final BigDecimal MICROS = new BigDecimal(1000000d);

  private WalletUtil() {
  }

  /**
   * Creates a MaskedWalletRequest for direct merchant integration (no payment processor)
   *
   * @param itemInfo {@link ItemInfo} containing details
   * of an item.
   * @return {@link MaskedWalletRequest} instance
   */
  public static MaskedWalletRequest createMaskedWalletRequest(ItemInfo itemInfo) {
    return createMaskedWalletRequest(itemInfo, null);
  }

  /**
   * Creates a MaskedWalletRequest for processing payments with Stripe
   *
   * @param itemInfo {@link ItemInfo} containing details
   * of an item.
   * @param parameters {@link PaymentMethodTokenizationParameters} object containing details
   * for payment processing with Stripe.
   * @return {@link MaskedWalletRequest} instance
   */
  public static MaskedWalletRequest createTokenizedMaskedWalletRequest(ItemInfo itemInfo,
      PaymentMethodTokenizationParameters parameters) {
    return createMaskedWalletRequest(itemInfo, parameters);
  }

  private static MaskedWalletRequest createMaskedWalletRequest(ItemInfo itemInfo,
      PaymentMethodTokenizationParameters parameters) {
    // Build a List of all line items
    List<LineItem> lineItems = buildLineItems(itemInfo, true);

    // Calculate the cart total by iterating over the line items.
    String cartTotal = calculateCartTotal(lineItems);

    MaskedWalletRequest.Builder builder = MaskedWalletRequest.newBuilder()
        .setMerchantName(Constants.MERCHANT_NAME)
        .setPhoneNumberRequired(true)
        .setShippingAddressRequired(true)
        .setCurrencyCode(Constants.CURRENCY_CODE_USD)
        .setEstimatedTotalPrice(cartTotal)
        // Create a Cart with the current line items. Provide all the information
        // available up to this point with estimates for shipping and tax included.
        .setCart(Cart.newBuilder()
            .setCurrencyCode(Constants.CURRENCY_CODE_USD)
            .setTotalPrice(cartTotal)
            .setLineItems(lineItems)
            .build());

    if (parameters != null) {
      builder.setPaymentMethodTokenizationParameters(parameters);
    }

    return builder.build();
  }

  /**
   * Build a list of line items based on the {@link ItemInfo} and a boolean that indicates
   * whether to use estimated values of tax and shipping for setting up the
   * {@link MaskedWalletRequest} or actual values in the case of a {@link FullWalletRequest}
   *
   * @param itemInfo {@link ItemInfo} used for building the
   * {@link com.google.android.gms.wallet.LineItem} list.
   * @param isEstimate {@code boolean} that indicates whether to use estimated values for
   * shipping and tax values.
   * @return list of line items
   */
  private static List<LineItem> buildLineItems(ItemInfo itemInfo, boolean isEstimate) {
    List<LineItem> list = new ArrayList<LineItem>();
    String itemPrice = toDollars(itemInfo.priceMicros);

    list.add(LineItem.newBuilder()
        .setCurrencyCode(Constants.CURRENCY_CODE_USD)
        .setDescription(itemInfo.name)
        .setQuantity("1")
        .setUnitPrice(itemPrice)
        .setTotalPrice(itemPrice)
        .build());

    String shippingPrice = toDollars(
        isEstimate ? itemInfo.estimatedShippingPriceMicros : itemInfo.shippingPriceMicros);

    list.add(LineItem.newBuilder()
        .setCurrencyCode(Constants.CURRENCY_CODE_USD)
        .setDescription(Constants.DESCRIPTION_LINE_ITEM_SHIPPING)
        .setRole(LineItem.Role.SHIPPING)
        .setTotalPrice(shippingPrice)
        .build());

    String tax = toDollars(isEstimate ? itemInfo.estimatedTaxMicros : itemInfo.taxMicros);

    list.add(LineItem.newBuilder()
        .setCurrencyCode(Constants.CURRENCY_CODE_USD)
        .setDescription(Constants.DESCRIPTION_LINE_ITEM_TAX)
        .setRole(LineItem.Role.TAX)
        .setTotalPrice(tax)
        .build());

    return list;
  }

  /**
   * @param lineItems List of {@link com.google.android.gms.wallet.LineItem} used for calculating
   * the cart total.
   * @return cart total.
   */
  private static String calculateCartTotal(List<LineItem> lineItems) {
    BigDecimal cartTotal = BigDecimal.ZERO;

    // Calculate the total price by adding up each of the line items
    for (LineItem lineItem : lineItems) {
      BigDecimal lineItemTotal =
          lineItem.getTotalPrice() == null ? new BigDecimal(lineItem.getUnitPrice()).multiply(
              new BigDecimal(lineItem.getQuantity())) : new BigDecimal(lineItem.getTotalPrice());

      cartTotal = cartTotal.add(lineItemTotal);
    }

    return cartTotal.setScale(2, RoundingMode.HALF_EVEN).toString();
  }

  /**
   * @param itemInfo {@link ItemInfo} to use for creating
   * the {@link com.google.android.gms.wallet.FullWalletRequest}
   * @return {@link FullWalletRequest} instance
   */
  public static FullWalletRequest createFullWalletRequest(ItemInfo itemInfo,
      String googleTransactionId) {

    List<LineItem> lineItems = buildLineItems(itemInfo, false);

    String cartTotal = calculateCartTotal(lineItems);

    return FullWalletRequest.newBuilder()
        .setGoogleTransactionId(googleTransactionId)
        .setCart(Cart.newBuilder()
            .setCurrencyCode(Constants.CURRENCY_CODE_USD)
            .setTotalPrice(cartTotal)
            .setLineItems(lineItems)
            .build())
        .build();
  }

  /**
   * @param status from {@link NotifyTransactionStatusRequest.Status} which could either be
   * {@code NotifyTransactionStatusRequest.Status.SUCCESS} or one of the error codes
   * from {@link NotifyTransactionStatusRequest.Status.Error}
   * @return {@link NotifyTransactionStatusRequest} instance
   */
  @SuppressWarnings("javadoc")
  public static NotifyTransactionStatusRequest createNotifyTransactionStatusRequest(
      String googleTransactionId, int status) {
    return NotifyTransactionStatusRequest.newBuilder()
        .setGoogleTransactionId(googleTransactionId)
        .setStatus(status)
        .build();
  }

  /**
   * @param micros Amount micros
   * @return string formatted as "0.00" required by the Instant Buy API.
   */
  private static String toDollars(double micros) {
    return new BigDecimal(micros).divide(MICROS).setScale(2, RoundingMode.HALF_EVEN).toString();
  }
}
