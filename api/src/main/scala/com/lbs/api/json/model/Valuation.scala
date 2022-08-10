package com.lbs.api.json.model

/**
  * {
  *        "alternativePrice": null,
  *        "contractId": 555555,
  *        "isExternalReferralAllowed": false,
  *        "isReferralRequired": false,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                false,
  *        "payerId": 66666,
  *        "price": 0.0,
  *        "productElementId": 7777777,
  *        "productId": 888888,
  *        "productInContractId": 9999999,
  *        "requireReferralForPP": false,
  *        "valuationType": 1
  *    }
  */

case class Valuation(
  alternativePrice: Option[String],
  contractId: Long,
  isExternalReferralAllowed: Boolean,
  isReferralRequired: Boolean,
  payerId: Long,
  price: Double,
  productElementId: Option[Long],
  productId: Long,
  productInContractId: Long,
  requireReferralForPP: Boolean,
  valuationType: Long
) {}
