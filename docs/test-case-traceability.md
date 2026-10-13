# Test case traceability (Test_Cases_Feed.xlsx, sheet 'Playwright-Java Feed')

Generated from the `@TmsLink` annotations in `src/test/java`. Status values:

- **implemented** - an enabled web test already covers the scenario (existing test tagged with the TC id; the mapping is by scenario title, since the sheet has no steps).
- **skeleton** - a disabled test with TODOs exists; needs steps, locators, data or access (see the TODO in the method).
- **needs-clarification** - the sheet flags the row (TC id shared by several scenarios, no TC id, or a non-descriptive name). Nothing was created. Confirm the mapping first.
- **disabled-needs-Appium** - none: every flow checked has a web equivalent.

| TC ID | Scenario | File / method | Status | Note |
|---|---|---|---|---|
| TC-401 | Login to Profile from Bengaluru City Selection | `FurlencoLoginTest#testLoginToProfileFromBengaluruCitySelection_TC401` | skeleton |  |
| TC-413 | Select Top City | - | needs-clarification | TC id TC-413 is used by 2 different scenarios |
| TC-413 | Verify City Pincode Visibility | - | needs-clarification | TC id TC-413 is used by 2 different scenarios |
| TC-416 | Create and Verify New User Account | - | needs-clarification | TC id TC-416 is used by 2 different scenarios |
| TC-416 | New sign up | - | needs-clarification | TC id TC-416 is used by 2 different scenarios |
| TC-417 | Navigation to GHP | - | needs-clarification | TC id TC-417 is used by 2 different scenarios |
| TC-417 | User Login with OTP Verification | - | needs-clarification | TC id TC-417 is used by 2 different scenarios |
| TC-419 | Select Other City | - | needs-clarification | TC id TC-419 is used by 2 different scenarios |
| TC-419 | Verify and Access Other Cities Pincode | - | needs-clarification | TC id TC-419 is used by 2 different scenarios |
| TC-421 | Navigation to Rent HP | `FurlencoPlpNavigationTest#verifyNavigateToRentFromGlobalHome` | implemented |  |
| TC-424 | PLP Listing | `FurlencoPlpNavigationTest#verifyNavigateToRentPlpFromCategory` | implemented |  |
| TC-425 | Add to cart | `FurlencoCheckoutFlowTest#verifyAddToCartFromRentPdp` | implemented |  |
| TC-426 | Category Navigation | `FurlencoPlpNavigationTest#verifySelectDifferentCategoriesOnRentPlp` | implemented |  |
| TC-427 | remove item from cart | `FurlencoCartQuantityAndPersistenceTest#verifyRemoveItem` | implemented |  |
| TC-428 | Add First Festive Deal Product to Cart | - | needs-clarification | TC id TC-428 is used by 2 different scenarios |
| TC-428 | Apply Offer | - | needs-clarification | TC id TC-428 is used by 2 different scenarios |
| TC-430 | remove item from cart | `FurlencoCartQuantityAndPersistenceTest#verifyRemoveItem` | implemented |  |
| TC-431 | update cart | `FurlencoCartQuantityAndPersistenceTest#verifyIncreaseQuantity` | implemented |  |
| TC-432 | update cart | `FurlencoCartQuantityAndPersistenceTest#verifyDecreaseQuantity` | implemented |  |
| TC-434 | Search address with Pincode | `FurlencoAddressTest#verifySearchAddressByPincodeAndArea` | implemented |  |
| TC-435 | Add New Address | `FurlencoAddressTest#verifyAddNewAddress` | implemented |  |
| TC-436 | Search address with area | `FurlencoAddressTest#verifySearchAddressByPincodeAndArea` | implemented |  |
| TC-437 | Invalid Pincode | `FurlencoAddressTest#verifyInvalidAddressSearchReturnsNoSuggestions` | implemented |  |
| TC-438 | Invalid address | `FurlencoAddressTest#verifyInvalidAddressSearchReturnsNoSuggestions` | implemented |  |
| TC-442 | remove item from cart | `FurlencoCartSummaryTest#testRemoveItemFromCartInCartSummary_TC442` | skeleton |  |
| TC-443 | Apply Offer | `FurlencoCartSummaryTest#testApplyOfferInCartSummary_TC443` | skeleton |  |
| TC-445 | Existing Address | `FurlencoAddressTest#verifyConfirmSavedAddressLeadsToOrderSummary` | implemented |  |
| TC-447 | Add VAS, Remove VAS | - | needs-clarification | TC id TC-447 is used by 2 different scenarios |
| TC-447 | Add VAS, Remove VAS - Partial | - | needs-clarification | TC id TC-447 is used by 2 different scenarios |
| TC-459 | Checkout functionality | `FurlencoCheckoutFlowTest#verifyAddressToOrderSummary` | implemented |  |
| TC-460 | Create New User Account with OTP Verification | `FurlencoNewUserSignupTest#verifyNewNumberRedirectsToSignupScreen` | implemented |  |
| TC-461 | Login and Add Item to Cart | `FurlencoLoginTest#verifyLoginFromCheckoutScreenRent` | implemented |  |
| TC-462 | Serviceable Pincode | - | needs-clarification | TC id TC-462 is used by 2 different scenarios |
| TC-462 | Verify Delivery Availability by Pincode | - | needs-clarification | TC id TC-462 is used by 2 different scenarios |
| TC-463 | Inline message for invalid offers | `FurlencoCartSummaryTest#testInlineMessageForInvalidOffersInCartSummary_TC463` | skeleton |  |
| TC-464 | Non serviceable Pincode | `FurlencoPincodeAvailabilityTest#verifyNonServiceablePincodeShowsError` | implemented |  |
| TC-465 | update cart | `FurlencoCartSummaryTest#testUpdateCartInCartSummary_TC465` | skeleton |  |
| TC-467 | Add Multiple Items to Cart with Verification | `FurlencoCartItemsTest#verifyAddMultipleItemsToCart` | implemented |  |
| TC-468 | Verify Out of Stock Status for Selected City and Category | `FurlencoStockAvailabilityTest#verifyOutOfStockStatusVariesByCity` | implemented |  |
| TC-469 | Create Account and Verify Product Details | - | needs-clarification | TC id TC-469 is used by 2 different scenarios |
| TC-469 | PDP layout | - | needs-clarification | TC id TC-469 is used by 2 different scenarios |
| TC-470 | Inline message for invalid offers | `FurlencoCartCouponTest#verifyApplyingInvalidCouponShowsError` | implemented |  |
| TC-472 | Replace Cart Item with Similar Product | - | needs-clarification | TC id TC-472 is used by 2 different scenarios |
| TC-472 | Replace similar items in cart | - | needs-clarification | TC id TC-472 is used by 2 different scenarios |
| TC-473 | Verify Out of Stock Item in Cart for Different Delivery City | `FurlencoStockAvailabilityTest#verifyOutOfStockItemInCartForDifferentCity` | implemented |  |
| TC-476 | Custom offer code | `FurlencoCartCouponTest#verifyApplyingOfferedCouponSucceeds` | implemented |  |
| TC-477 | Add VAS, Remove VAS | `FurlencoCartVasTest#verifyTogglingVasChangesAppliedCount` | implemented |  |
| TC-478 | Replace similar items in cart | `FurlencoCartSummaryTest#testReplaceSimilarItemsInCartInCartSummary_TC478` | skeleton |  |
| TC-479 | Custom offer code | `FurlencoCartSummaryTest#testCustomOfferCodeInCartSummary_TC479` | skeleton |  |
| TC-480 | Invalid Pincode | `FurlencoCityAndPincodeTest#verifyInvalidPincodeShowsError` | implemented |  |
| TC-484 | Address change from order summary | `FurlencoAddressTest#verifyChangeAddressFromOrderSummary` | implemented |  |
| TC-487 | Checkout functionality | `FurlencoSuccessfulOrderTest#verifySuccessfulRentOrder` | implemented |  |
| TC-497 | Checkout order with Autopay with new user | `FurlencoCheckoutAutopayTest#testCheckoutOrderWithAutopayWithNewUser_TC497` | skeleton |  |
| TC-500 | Add VAS | - | needs-clarification | TC id TC-500 is used by 2 different scenarios |
| TC-500 | Add VAS, Remove VAS | - | needs-clarification | TC id TC-500 is used by 2 different scenarios |
| TC-502 | Add Product to Cart for Rent | `FurlencoCheckoutFlowTest#verifyAddToCartFromRentPdp` | implemented |  |
| TC-505 | DIY return without Pay & X | `FurlencoProfileAndOrdersTest#verifyDiyReturnWithoutPay` | skeleton |  |
| TC-509 | Checkout order without Autopay with new user | `FurlencoCheckoutAutopayTest#testCheckoutOrderWithoutAutopayWithNewUser_TC509` | skeleton |  |
| TC-512 | Login Using OTP with Database-Driven Credentials | - | needs-clarification | TC id TC-512 is used by 3 different scenarios |
| TC-512 | New order with mandate Autopay | - | needs-clarification | TC id TC-512 is used by 3 different scenarios |
| TC-512 | Upsell order with mandate Autopay | - | needs-clarification | TC id TC-512 is used by 3 different scenarios |
| TC-514 | New order without mandate Autopay | - | needs-clarification | TC id TC-514 is used by 2 different scenarios |
| TC-514 | Verify Order Details Consistency | - | needs-clarification | TC id TC-514 is used by 2 different scenarios |
| TC-515 | DIY Order Cancel | - | needs-clarification | TC id TC-515 is used by 2 different scenarios |
| TC-515 | Login and View My Orders | - | needs-clarification | TC id TC-515 is used by 2 different scenarios |
| TC-524 | Access User Profile and Login | - | needs-clarification | TC id TC-524 is used by 2 different scenarios |
| TC-524 | DIY product - order cancel | - | needs-clarification | TC id TC-524 is used by 2 different scenarios |
| TC-550 | DIY Renewal with Pay & X - In-progress | `FurlencoRenewalTest#testDiyRenewalWithPayAndXInProgress_TC550` | skeleton |  |
| TC-554 | DIY Renewal without Pay & X - Inprogress | - | needs-clarification | TC id TC-554 is used by 2 different scenarios |
| TC-554 | DIY Renewal without Pay & X - Item with RO state without autopay | - | needs-clarification | TC id TC-554 is used by 2 different scenarios |
| TC-555 | Renew Subscription via Net Banking | `FurlencoRenewalTest#testRenewSubscriptionViaNetBanking_TC555` | skeleton |  |
| TC-557 | DIY Renewal without Pay & X - Bundle with RO State without Autopay | `FurlencoRenewalTest#testDiyRenewalWithoutPayAndXBundleWithRoStateWithoutAutopay_TC557` | skeleton |  |
| TC-560 | Login Using Database Phone Number | `FurlencoProfileAndOrdersTest#testLoginUsingDatabasePhoneNumber_TC560` | skeleton |  |
| TC-563 | Perform Order Management System Query | `FurlencoProfileAndOrdersTest#testPerformOrderManagementSystemQuery_TC563` | skeleton |  |
| TC-564 | DIY Renewal without Pay & X - Bundle with Active State without Autopay | - | needs-clarification | TC id TC-564 is used by 2 different scenarios |
| TC-564 | Renew Subscription via Net Banking | - | needs-clarification | TC id TC-564 is used by 2 different scenarios |
| TC-569 | DIY Renewal with Autopay - with Active state - Item | `FurlencoRenewalTest#testDiyRenewalWithAutopayWithActiveStateItem_TC569` | skeleton |  |
| TC-570 | Renew Subscription with Autopay Verification | - | needs-clarification | TC id TC-570 is used by 2 different scenarios |
| TC-570 | TC 71 | - | needs-clarification | non-descriptive name |
| TC-571 | TC-73 | - | needs-clarification | non-descriptive name |
| TC-572 | DIY Renewal with Autopay - with RO state - CI | - | needs-clarification | TC id TC-572 is used by 2 different scenarios |
| TC-572 | Renew Subscription Product via Online Payment | - | needs-clarification | TC id TC-572 is used by 2 different scenarios |
| TC-575 | Renew Subscription with Credit Card Payment | `FurlencoRenewalTest#testRenewSubscriptionWithCreditCardPayment_TC575` | skeleton |  |
| TC-577 | TC-76 | - | needs-clarification | non-descriptive name |
| NO-TC | Add vehicle, helper, champ, driver | - | needs-clarification | no TC id; Selenium run, not the Android app |
| NO-TC | Conditional Wait Based on Database Query Result | - | needs-clarification | no TC id; Selenium run, not the Android app |
| NO-TC | Final Labelling Flow | - | needs-clarification | no TC id; Selenium run, not the Android app |
| NO-TC | Perform Google Search with Provided Query | - | needs-clarification | no TC id; Selenium run, not the Android app |
| NO-TC | Plan Adhoc Trip | - | needs-clarification | no TC id; Selenium run, not the Android app |
| NO-TC | Retrieve and Process Shipment and Token Data | - | needs-clarification | no TC id; Selenium run, not the Android app |
| NO-TC | Select Appliance for Rent by Pincode | - | needs-clarification | no TC id |
| NO-TC | Verify Contact Number via OTP | - | needs-clarification | no TC id; Selenium run, not the Android app |

## Totals

- implemented: 28
- needs-clarification: 47
- skeleton: 17
