package com.lbs.api.json.model

/**
 * {
      "Message": "Obecnie zainstalowana wersja aplikacji nie jest wspierana przez nowy system Portalu Pacjenta. Zaktualizuj aplikację do najnowszej wersji, aby móc z niej korzystać.",
      "AdditionalData": {
        "ShopUrl": "itms-apps://apps.apple.com/pl/app/id552592684",
        "FallbackUrl": "itms-apps://apps.apple.com/pl/app/id552592684",
        "Title": "Zaktualizuj aplikację"
      }
    }
 */
case class LuxmedError(message: String) extends SerializableJsonObject with LuxmedBaseError
