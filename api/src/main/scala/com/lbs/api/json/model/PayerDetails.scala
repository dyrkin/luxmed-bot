
package com.lbs.api.json.model

case class PayerDetails(brandId: Option[Long], contractId: Long, payerId: Long, payerName: String, productElementId: Option [Long],
                        productId: Long, productInContractId: Long, servaAppId: Long, servaId: Long) extends SerializableJsonObject
