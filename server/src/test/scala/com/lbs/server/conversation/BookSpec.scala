package com.lbs.server.conversation

import com.lbs.api.json.model.*
import com.lbs.bot.Bot
import com.lbs.bot.model.{Command, Message, MessageSource, TelegramMessageSourceSystem}
import com.lbs.server.conversation.Book.Tags
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.NoItemsFound
import com.lbs.server.conversation.base.ConversationTestProbe
import com.lbs.server.lang.{En, Localization}
import com.lbs.server.repository.model.Settings
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*

import java.time.{LocalDateTime, LocalTime}

class BookSpec extends AkkaTestKit {

  private val source = MessageSource(TelegramMessageSourceSystem, "1")
  private val userId = UserId(1L, "testuser", 1L, source)

  private def makeBot               = mock(classOf[Bot])
  private def makeApiService        = mock(classOf[ApiService])
  private def makeDataService       = mock(classOf[DataService])
  private def makeMonitoringService = mock(classOf[MonitoringService])

  private def makeLocalization: Localization = {
    val loc = mock(classOf[Localization])
    when(loc.lang(any())).thenReturn(En)
    loc
  }

  private def makeBook(
    bot: Bot                             = makeBot,
    apiService: ApiService               = makeApiService,
    dataService: DataService             = makeDataService,
    monitoringService: MonitoringService = makeMonitoringService
  )(
    datePickerProbe: ConversationTestProbe.ConversationTestProbe[DatePicker],
    timePickerProbe: ConversationTestProbe.ConversationTestProbe[TimePicker],
    staticDataProbe: ConversationTestProbe.ConversationTestProbe[StaticData],
    termsPagerProbe: ConversationTestProbe.ConversationTestProbe[Pager[TermExt]]
  ): Book =
    new Book(
      userId, bot, apiService, dataService, monitoringService, makeLocalization,
      datePickerFactory = (_, _) => datePickerProbe.conversation,
      timePickerFactory = (_, _) => timePickerProbe.conversation,
      staticDataFactory = (_, _) => staticDataProbe.conversation,
      termsPagerFactory = (_, _) => termsPagerProbe.conversation
    )(system)

  private def callbackCmd(tag: String) =
    Command(source, Message("1", Some(tag)), Some(tag))

  private def selectStaticData(book: Book): Unit = {
    book ! IdName(1L, "Wroclaw")
    book ! IdName(100L, "GP Consultation")
    book ! IdName(10L, "Swobodna Clinic")
    book ! IdName(50L, "Dr Smith")
  }

  private def selectDates(book: Book): Unit = {
    val now = LocalDateTime.now()
    book ! now
    book ! now.plusDays(7)
    book ! LocalTime.of(8, 0)
    book ! LocalTime.of(20, 0)
  }

  private def sampleTerm: TermExt = {
    val dt     = LuxmedFunnyDateTime(dateTimeLocal = Some(LocalDateTime.of(2026, 6, 1, 10, 0)))
    val doctor = Doctor("dr", Some(List(10L)), "John", Some(false), Some(1L), 50L, "Smith")
    val term   = Term("Clinic A", 10L, 5L, dt, dt, doctor, "",
                      isAdditional = false, isImpediment = false, isTelemedicine = false,
                      1L, 1000L, 100L)
    TermExt(AdditionalData(isPreparationRequired = false, preparationItems = Nil), term)
  }

  private def sampleLockResponse(changeTermAvailable: Boolean): ReservationLocktermResponse = {
    val doctor    = Doctor("dr", None, "John", None, None, 50L, "Smith")
    val valuation = Valuation(None, Some(1L), isExternalReferralAllowed = false,
                              isReferralRequired = false, Some(1L), Some(0.0),
                              Some(1L), Some(1L), Some(1L), requireReferralForPP = false, 1L)
    val relatedVisits = if (changeTermAvailable)
      List(RelatedVisit(doctor, "Clinic A", isTelemedicine = false, 777L,
                        LocalTime.of(10, 0), LocalTime.of(10, 30)))
    else Nil
    ReservationLocktermResponse(Nil, Nil, hasErrors = false, hasWarnings = false,
      ReservationLocktermResponseValue(
        changeTermAvailable    = changeTermAvailable,
        conflictedVisit        = None,
        doctorDetails          = doctor,
        relatedVisits          = relatedVisits,
        temporaryReservationId = 999L,
        valuations             = List(valuation)))
  }

  private def sampleConfirmResponse = ReservationConfirmResponse(
    Nil, Nil, hasErrors = false, hasWarnings = false,
    ReservationConfirmValue(canSelfConfirm = false, "nps-token", 12345L, 999L))

  // getAvailableTerms has 10 params: 9 explicit + default languageId
  private def stubGetTerms(apiService: ApiService, result: Either[Throwable, List[TermExt]]): Unit =
    when(apiService.getAvailableTerms(
      anyLong(), anyLong(), any(), anyLong(), any(), any(), any(), any(), any(), anyLong()
    )).thenReturn(result)

  private def stubBookingFlow(
    apiService: ApiService,
    dataService: DataService,
    lockResponse: ReservationLocktermResponse = sampleLockResponse(changeTermAvailable = false)
  ): Unit = {
    doNothing().when(dataService).storeAppointment(any(), any())
    stubGetTerms(apiService, Right(List(sampleTerm)))
    when(apiService.getXsrfToken(anyLong()))
      .thenReturn(Right(XsrfToken("tok", Seq())))
    when(apiService.reservationLockterm(anyLong(), any(), any()))
      .thenReturn(Right(lockResponse))
    when(apiService.reservationConfirm(anyLong(), any(), any()))
      .thenReturn(Right(sampleConfirmResponse))
  }

  "Book conversation" when {

    "following the happy path" must {

      "advance through all steps and book an appointment" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService  = makeApiService
        val dataService = makeDataService
        stubBookingFlow(apiService, dataService)
        val book = makeBook(apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! sampleTerm
        book ! callbackCmd(Tags.Book)
        awaitAssert(verify(apiService).reservationConfirm(anyLong(), any(), any()))
      }

      "return to requestDateFrom when ModifyDate is clicked in requestAction" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val dataService = makeDataService
        doNothing().when(dataService).storeAppointment(any(), any())
        val book = makeBook(dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.ModifyDate)
        val base = LocalDateTime.now().plusDays(3)
        book ! base
        book ! base.plusDays(7)
        book ! LocalTime.of(9, 0)
        book ! LocalTime.of(18, 0)
        succeed
      }

      "delete temporary reservation when Cancel is clicked" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService  = makeApiService
        val dataService = makeDataService
        stubBookingFlow(apiService, dataService)
        when(apiService.deleteTemporaryReservation(anyLong(), any(), anyLong()))
          .thenReturn(Right(()))
        val book = makeBook(apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! sampleTerm
        book ! callbackCmd(Tags.Cancel)
        awaitAssert(verify(apiService).deleteTemporaryReservation(anyLong(), any(), anyLong()))
      }
    }

    "no available terms" must {

      "create a manual monitoring after NoItemsFound -> CreateMonitoring" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService        = makeApiService
        val dataService       = makeDataService
        val monitoringService = makeMonitoringService
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(anyLong())).thenReturn(None)
        val book = makeBook(apiService = apiService, dataService = dataService,
                            monitoringService = monitoringService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! NoItemsFound
        book ! callbackCmd(Tags.CreateMonitoring)
        book ! callbackCmd(Tags.BookManually)
        awaitAssert(verify(monitoringService).createMonitoring(any()))
      }

      "return to requestDateFrom via ModifyDate from askNoTermsAction" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService  = makeApiService
        val dataService = makeDataService
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(anyLong())).thenReturn(None)
        val book = makeBook(apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! NoItemsFound
        book ! callbackCmd(Tags.ModifyDate)
        val base = LocalDateTime.now().plusDays(2)
        book ! base
        book ! base.plusDays(9)
        book ! LocalTime.of(7, 0)
        book ! LocalTime.of(21, 0)
        succeed
      }
    }

    "term is already booked (changeTermAvailable = true)" must {

      "rebook when user clicks Yes" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService  = makeApiService
        val dataService = makeDataService
        stubBookingFlow(apiService, dataService,
          lockResponse = sampleLockResponse(changeTermAvailable = true))
        when(apiService.reservationChangeTerm(anyLong(), any(), any()))
          .thenReturn(Right(sampleConfirmResponse))
        val book = makeBook(apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! sampleTerm
        book ! callbackCmd(Tags.Yes)
        awaitAssert(verify(apiService).reservationChangeTerm(anyLong(), any(), any()))
      }

      "not rebook when user clicks No" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService  = makeApiService
        val dataService = makeDataService
        stubBookingFlow(apiService, dataService,
          lockResponse = sampleLockResponse(changeTermAvailable = true))
        val book = makeBook(apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! sampleTerm
        book ! callbackCmd(Tags.No)
        awaitAssert {
          verify(apiService, never()).reservationChangeTerm(anyLong(), any(), any())
          verify(apiService, never()).reservationConfirm(anyLong(), any(), any())
        }
      }
    }

    "monitoring offset and autobook configuration" must {

      "ask for offset when alwaysAskOffset=true, then autobook with rebook" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService        = makeApiService
        val dataService       = makeDataService
        val monitoringService = makeMonitoringService
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(userId.userId))
          .thenReturn(Some(Settings(userId.userId, 0, 0, alwaysAskOffset = true)))
        val book = makeBook(apiService = apiService, dataService = dataService,
                            monitoringService = monitoringService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! NoItemsFound
        book ! callbackCmd(Tags.CreateMonitoring)
        book ! Command(source, Message("1", Some("30")), None) // enter offset
        book ! callbackCmd(Tags.BookByApplication)            // autobook = true
        book ! callbackCmd(Tags.Yes)                          // rebookIfExists = true
        awaitAssert(verify(monitoringService).createMonitoring(any()))
      }

      "skip offset when No clicked, then create manual monitoring" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService        = makeApiService
        val dataService       = makeDataService
        val monitoringService = makeMonitoringService
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(userId.userId))
          .thenReturn(Some(Settings(userId.userId, 0, 0, alwaysAskOffset = true)))
        val book = makeBook(apiService = apiService, dataService = dataService,
                            monitoringService = monitoringService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! NoItemsFound
        book ! callbackCmd(Tags.CreateMonitoring)
        book ! callbackCmd(Tags.No)           // skip offset
        book ! callbackCmd(Tags.BookManually) // manual booking
        awaitAssert(verify(monitoringService).createMonitoring(any()))
      }
    }
  }
}