package com.lbs.api.json.model

import io.circe.*
import io.circe.generic.semiauto.*

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, LocalTime, ZonedDateTime}
import scala.util.Try

object JsonCodecs {

  given Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  given Decoder[ZonedDateTime] = Decoder[String].emap { s =>
    Try(ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME)).toEither.left.map(_.getMessage)
  }

  given Encoder[LocalDateTime] = Encoder[String].contramap(_.toString)
  given Decoder[LocalDateTime] = Decoder[String].emap { s =>
    Try(LocalDateTime.parse(s)).toEither.left.map(_.getMessage)
  }

  given Encoder[LocalTime] = Encoder[String].contramap(_.toString)
  given Decoder[LocalTime] = Decoder[String].emap { s =>
    Try(LocalTime.parse(s)).toEither.left.map(_.getMessage)
  }

  given Decoder[LuxmedFunnyDateTime] = Decoder[String].emap { str =>
    Try(LocalDateTime.parse(str))
      .map(v => LuxmedFunnyDateTime(dateTimeLocal = Some(v)))
      .recoverWith { case _ =>
        Try(ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .map(v => LuxmedFunnyDateTime(dateTimeTz = Some(v)))
      }
      .toEither
      .left
      .map(_.getMessage)
  }

  given Encoder[LuxmedFunnyDateTime] = Encoder[String].contramap { dt =>
    dt.dateTimeLocal.map(_.toString).orElse(dt.dateTimeTz.map(_.toString)).getOrElse("")
  }

  given Codec[IdName]                       = deriveCodec
  given Codec[LoginResponse]                = deriveCodec
  given Codec[ForgeryTokenResponse]         = deriveCodec
  given Codec[LuxmedError]                  = deriveCodec
  given Codec[LuxmedErrorsList]             = deriveCodec
  given Codec[LuxmedErrorsMap]              = deriveCodec
  given Codec[Empty]                        = deriveCodec
  given Codec[Doctor]                       = deriveCodec
  given Codec[FacilitiesAndDoctors]         = deriveCodec
  given Codec[EventClinic]                  = deriveCodec
  given Codec[EventDoctor]                  = deriveCodec
  given Codec[Event]                        = deriveCodec
  given Codec[EventsResponse]               = deriveCodec
  given Codec[PreparationItem]              = deriveCodec
  given Codec[AdditionalData]               = deriveCodec

  given Decoder[DictionaryServiceVariants] = new Decoder[DictionaryServiceVariants] {
    lazy val underlying: Decoder[DictionaryServiceVariants] = deriveDecoder
    def apply(c: HCursor): Decoder.Result[DictionaryServiceVariants] = underlying(c)
  }
  given Encoder[DictionaryServiceVariants] = new Encoder.AsObject[DictionaryServiceVariants] {
    lazy val underlying: Encoder.AsObject[DictionaryServiceVariants] = deriveEncoder
    def encodeObject(a: DictionaryServiceVariants): JsonObject = underlying.encodeObject(a)
  }

  given Codec[DictionaryCity]               = deriveCodec
  given Codec[Valuation]                    = deriveCodec
  given Codec[RelatedVisit]                 = deriveCodec
  given Codec[ReservationLocktermResponseValue] = deriveCodec
  given Codec[ReservationLocktermResponse]  = deriveCodec
  given Codec[ReservationConfirmValue]      = deriveCodec
  given Codec[ReservationConfirmResponse]   = deriveCodec
  given Codec[NewTerm]                      = deriveCodec
  given Codec[ReservationChangetermRequest] = deriveCodec
  given Codec[ReservationConfirmRequest]    = deriveCodec
  given Codec[ReservationLocktermRequest]   = deriveCodec
  given Codec[Term]                         = deriveCodec
  given Codec[TermExt]                      = deriveCodec
  given Codec[TermsForDay]                  = deriveCodec
  given Codec[TermsForService]              = deriveCodec
  given Codec[TermsIndexResponse]           = deriveCodec
  given Codec[DateRange]                    = deriveCodec
  given Codec[SearchVisitInfo]              = deriveCodec
  given Codec[RehabProcedure]               = deriveCodec
  given Codec[ServiceVariantInfo]           = deriveCodec
  given Codec[Referral]                     = deriveCodec
  given Codec[ReferralsResponse]            = deriveCodec
  given Codec[RehabLocation]                = deriveCodec
  given Codec[RehabFacility]                = deriveCodec
  given Codec[RehabFacilitiesResponse]      = deriveCodec
  given Codec[ServiceReferralItem]          = deriveCodec
  given Codec[ServiceReferralResponse]      = deriveCodec
}



