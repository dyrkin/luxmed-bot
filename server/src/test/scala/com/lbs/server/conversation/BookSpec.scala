package com.lbs.server.conversation

import com.lbs.api.json.model.*
import com.lbs.bot.Bot
import com.lbs.bot.model.{Command, InlineKeyboard, Message, MessageSource, TelegramMessageSourceSystem}
import com.lbs.server.conversation.DatePicker.DateRange
import com.lbs.server.conversation.Book.Tags
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.NoItemsFound
import com.lbs.server.conversation.base.ConversationTestProbe
import com.lbs.server.lang.{En, Localization}
import com.lbs.server.repository.model.{Monitoring, Settings}
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*

import java.time.{DayOfWeek, LocalDate, LocalDateTime, LocalTime}

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

  private def awaitClinicContinuationPrompt(bot: Bot, selectedClinic: String): Unit =
    awaitAssert(
      verify(bot, atLeastOnce()).sendMessage(
        any[MessageSource](),
        contains(selectedClinic),
        any[Option[InlineKeyboard]]()
      )
    )

  private def selectStaticData(book: Book, bot: Bot): Unit = {
    book ! IdName(1L, "Wroclaw")
    book ! IdName(100L, "GP Consultation")
    book ! IdName(10L, "Swobodna Clinic")
    awaitClinicContinuationPrompt(bot, "Swobodna Clinic")
    book ! callbackCmd(Tags.Continue)
    book ! IdName(50L, "Dr Smith")
  }

  private def selectDates(book: Book): Unit = {
    val now = LocalDateTime.now()
    book ! now
    book ! now.plusDays(7)
    book ! callbackCmd(Tags.Done)
    book ! callbackCmd(Tags.No)
    book ! LocalTime.of(8, 0)
    book ! LocalTime.of(20, 0)
  }

  private def sampleTerm: TermExt = {
    val dt     = LuxmedFunnyDateTime(dateTimeLocal = Some(LocalDateTime.of(2026, 6, 1, 10, 0)))
    val doctor = Doctor(Some("dr"), Some(List(10L)), "John", Some(false), Some(1L), 50L, "Smith")
    val term   = Term(Some("Clinic A"), 10L, 5L, dt, dt, doctor, Some(""),
                      isAdditional = false, isImpediment = false, isTelemedicine = false,
                      1L, 1000L, 100L)
    TermExt(AdditionalData(isPreparationRequired = false, preparationItems = Nil), term)
  }

  private def sampleLockResponse(changeTermAvailable: Boolean): ReservationLocktermResponse = {
    val doctor    = Doctor(Some("dr"), None, "John", None, None, 50L, "Smith")
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
        val bot         = makeBot
        stubBookingFlow(apiService, dataService)
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
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
        val bot         = makeBot
        doNothing().when(dataService).storeAppointment(any(), any())
        val book = makeBook(bot = bot, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
        selectDates(book)
        book ! callbackCmd(Tags.ModifyDate)
        val base = LocalDateTime.now().plusDays(3)
        book ! base
        book ! base.plusDays(7)
        book ! callbackCmd(Tags.Done)
        book ! callbackCmd(Tags.No)
        book ! LocalTime.of(9, 0)
        book ! LocalTime.of(18, 0)
        succeed
      }

      "accept a date range from date picker and skip requestDateTo" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService  = makeApiService
        val dataService = makeDataService
        val bot         = makeBot
        stubBookingFlow(apiService, dataService)
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        val from = LocalDateTime.now().plusDays(1)
        val to = from.plusDays(7)
        book.start()
        sp.expectMsgType[StaticData.StaticDataConfig]
        book ! IdName(1L, "Wroclaw")
        sp.expectMsgType[StaticData.StaticDataConfig]
        book ! IdName(100L, "GP Consultation")
        sp.expectMsgType[StaticData.StaticDataConfig]
        book ! IdName(10L, "Swobodna Clinic")
        awaitClinicContinuationPrompt(bot, "Swobodna Clinic")
        book ! callbackCmd(Tags.Continue)
        sp.expectMsgType[StaticData.StaticDataConfig]
        book ! IdName(50L, "Dr Smith")
        dp.expectMsg(DatePicker.DateFromMode)
        dp.expectMsgType[LocalDateTime]
        book ! DateRange(from, to)
        book ! callbackCmd(Tags.Done)
        book ! callbackCmd(Tags.No)
        tp.fishForMessage() {
          case TimePicker.TimeFromMode => true
          case _                       => false
        }
        tp.fishForMessage() {
          case _: LocalTime => true
          case _            => false
        }
        book ! LocalTime.of(8, 0)
        tp.fishForMessage() {
          case TimePicker.TimeToMode => true
          case _                     => false
        }
        tp.fishForMessage() {
          case _: LocalTime => true
          case _            => false
        }
        book ! LocalTime.of(20, 0)
        awaitAssert(verify(dataService, atLeastOnce()).storeAppointment(any(), any()))
        book ! callbackCmd(Tags.FindTerms)
        awaitAssert(verify(apiService, atLeastOnce()).getAvailableTerms(
          anyLong(), anyLong(), any(), anyLong(), any(), any(), any(), any(), any(), anyLong()
        ))
      }

      "delete temporary reservation when Cancel is clicked" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService  = makeApiService
        val dataService = makeDataService
        val bot         = makeBot
        stubBookingFlow(apiService, dataService)
        when(apiService.deleteTemporaryReservation(anyLong(), any(), anyLong()))
          .thenReturn(Right(()))
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
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
        val bot               = makeBot
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(anyLong())).thenReturn(None)
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService,
                            monitoringService = monitoringService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
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
        val bot         = makeBot
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(anyLong())).thenReturn(None)
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! NoItemsFound
        book ! callbackCmd(Tags.ModifyDate)
        val base = LocalDateTime.now().plusDays(2)
        book ! base
        book ! base.plusDays(9)
        book ! callbackCmd(Tags.Done)
        book ! callbackCmd(Tags.No)
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
        val bot         = makeBot
        stubBookingFlow(apiService, dataService,
          lockResponse = sampleLockResponse(changeTermAvailable = true))
        when(apiService.reservationChangeTerm(anyLong(), any(), any()))
          .thenReturn(Right(sampleConfirmResponse))
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
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
        val bot         = makeBot
        stubBookingFlow(apiService, dataService,
          lockResponse = sampleLockResponse(changeTermAvailable = true))
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
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
        val bot               = makeBot
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(userId.userId))
          .thenReturn(Some(Settings(userId.userId, 0, 0, alwaysAskOffset = true)))
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService,
                            monitoringService = monitoringService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
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
        val bot               = makeBot
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(userId.userId))
          .thenReturn(Some(Settings(userId.userId, 0, 0, alwaysAskOffset = true)))
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService,
                            monitoringService = monitoringService)(dp, tp, sp, pp)
        book.start()
        selectStaticData(book, bot)
        selectDates(book)
        book ! callbackCmd(Tags.FindTerms)
        book ! NoItemsFound
        book ! callbackCmd(Tags.CreateMonitoring)
        book ! callbackCmd(Tags.No)           // skip offset
        book ! callbackCmd(Tags.BookManually) // manual booking
        awaitAssert(verify(monitoringService).createMonitoring(any()))
      }

      "create one monitoring with multiple clinics and exclusions" in {
        val dp = ConversationTestProbe[DatePicker]()
        val tp = ConversationTestProbe[TimePicker]()
        val sp = ConversationTestProbe[StaticData]()
        val pp = ConversationTestProbe[Pager[TermExt]]()
        val apiService        = makeApiService
        val dataService       = makeDataService
        val monitoringService = makeMonitoringService
        val bot               = makeBot
        doNothing().when(dataService).storeAppointment(any(), any())
        stubGetTerms(apiService, Right(Nil))
        when(dataService.findSettings(anyLong())).thenReturn(None)
        val book = makeBook(bot = bot, apiService = apiService, dataService = dataService,
                            monitoringService = monitoringService)(dp, tp, sp, pp)
        book.start()
        book ! IdName(1L, "Wroclaw")
        book ! IdName(100L, "GP Consultation")
        book ! IdName(10L, "Swobodna Clinic")
        awaitClinicContinuationPrompt(bot, "Swobodna Clinic")
        book ! callbackCmd(Tags.AddAnotherClinic)
        book ! IdName(11L, "Legnicka Clinic")
        awaitClinicContinuationPrompt(bot, "Legnicka Clinic")
        book ! callbackCmd(Tags.Continue)
        book ! IdName(50L, "Dr Smith")
        val now = LocalDateTime.now()
        book ! now
        book ! now.plusDays(7)
        book ! callbackCmd(Tags.WeekdayPrefix + DayOfWeek.TUESDAY.getValue)
        book ! callbackCmd(Tags.Done)
        book ! Command(source, Message("1", Some("2026-06-10")), None)
        book ! LocalTime.of(8, 0)
        book ! LocalTime.of(20, 0)
        book ! callbackCmd(Tags.FindTerms)
        book ! NoItemsFound
        book ! callbackCmd(Tags.CreateMonitoring)
        book ! callbackCmd(Tags.BookManually)

        val captor = ArgumentCaptor.forClass(classOf[Monitoring])
        awaitAssert(verify(monitoringService).createMonitoring(captor.capture()))
        val monitoring = captor.getValue
        assert(monitoring.clinicOptions == Seq(Some(10L), Some(11L)))
        assert(monitoring.excludedWeekdaysSet == Set(DayOfWeek.TUESDAY))
        assert(monitoring.excludedDatesSet == Set(LocalDate.parse("2026-06-10")))
      }
    }
  }
}
