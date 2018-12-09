
package com.lbs.api.json.model

case class PayerDetails(brandId: Option[Long], contractId: Long, payerId: Long, payerName: String, productElementId: Long,
                        productId: Long, productInContractId: Long, servaAppId: Long, servaId: Long) extends SerializableJsonObject
