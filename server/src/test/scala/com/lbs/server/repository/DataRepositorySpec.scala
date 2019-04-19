package com.lbs.server.repository

import java.time.ZonedDateTime

import com.lbs.server.repository.model.{Bug, CityHistory, ClinicHistory, Credentials, DoctorHistory, ServiceHistory}
import javax.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.{DataJpaTest, TestEntityManager}
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit4.SpringRunner

object DataRepositorySpec {

  @TestConfiguration
  class PostServiceTestContextConfiguration {
    @Autowired
    var em: EntityManager = _

    @Bean
    def dataRepository = new DataRepository(em)
  }

}

@RunWith(classOf[SpringRunner])
@DataJpaTest
class DataRepositorySpec {
  @Autowired
  private var em: TestEntityManager = _

  @Autowired
  private var dataRepository: DataRepository = _

  @Test
  def whenGetCityHistory_thenReturnLatestTwoRecords(): Unit = {
    val history1 = CityHistory(1L, 1L, "hello", ZonedDateTime.now().plusSeconds(1))
    val history2 = CityHistory(1L, 2L, "world1", ZonedDateTime.now().plusSeconds(2))
    val history3 = CityHistory(1L, 3L, "world2", ZonedDateTime.now().plusSeconds(3))
    em.persist(history1)
    em.persist(history2)
    em.persist(history3)
    em.flush()

    val found = dataRepository.getCityHistory(1L)
    assertThat(found).isEqualTo(Seq(history3, history2))
  }

  @Test
  def whenGetClinicHistory_thenReturnLatestTwoRecords(): Unit = {
    val history1 = ClinicHistory(1L, 1L, "hello", 1L, ZonedDateTime.now().plusSeconds(1))
    val history2 = ClinicHistory(1L, 2L, "world1", 1L, ZonedDateTime.now().plusSeconds(2))
    val history3 = ClinicHistory(1L, 3L, "world2", 1L, ZonedDateTime.now().plusSeconds(3))
    em.persist(history1)
    em.persist(history2)
    em.persist(history3)
    em.flush()

    val found = dataRepository.getClinicHistory(1L, 1L)
    assertThat(found).isEqualTo(Seq(history3, history2))
  }

  @Test
  def whenGetServiceHistory_thenReturnLatestTwoRecords(): Unit = {
    val history1 = ServiceHistory(1L, 1L, "hello", 1L, Some(1L), ZonedDateTime.now().plusSeconds(1))
    val history2 = ServiceHistory(1L, 2L, "world1", 1L, Some(1L), ZonedDateTime.now().plusSeconds(2))
    val history3 = ServiceHistory(1L, 3L, "world2", 1L, Some(1L), ZonedDateTime.now().plusSeconds(3))
    em.persist(history1)
    em.persist(history2)
    em.persist(history3)
    em.flush()

    val found = dataRepository.getServiceHistory(1L, 1L, Some(1L))
    assertThat(found).isEqualTo(Seq(history3, history2))
  }

  @Test
  def whenGetDoctorHistory_thenReturnLatestTwoRecords(): Unit = {
    val history1 = DoctorHistory(1L, 1L, "hello", 1L, Some(1L), 1L, ZonedDateTime.now().plusSeconds(1))
    val history2 = DoctorHistory(1L, 2L, "world1", 1L, Some(1L), 1L, ZonedDateTime.now().plusSeconds(2))
    val history3 = DoctorHistory(1L, 3L, "world2", 1L, Some(1L), 1L, ZonedDateTime.now().plusSeconds(3))
    em.persist(history1)
    em.persist(history2)
    em.persist(history3)
    em.flush()

    val found = dataRepository.getDoctorHistory(1L, 1L, Some(1L), 1L)
    assertThat(found).isEqualTo(Seq(history3, history2))
  }

  @Test
  def whenFindCredentials_thenReturnOneRecord(): Unit = {
    val credentials = Credentials(1L, 1L, "user1", "pwd1")
    em.persist(credentials)
    em.flush()

    val found = dataRepository.getUserCredentials(1L)
    assertThat(found).isEqualTo(Seq(credentials))
  }

  @Test
  def whenGetBugs_thenReturnABug(): Unit = {
    val bug1 = Bug(1L, 1L, "bug1", resolved = false, ZonedDateTime.now())
    val bug2 = Bug(1L, 2L, "bug2", resolved = false, ZonedDateTime.now())
    em.persist(bug1)
    em.persist(bug2)
    em.flush()

    val found = dataRepository.getBugs(1L)
    assertThat(found).isEqualTo(Seq(bug2, bug1))
  }
}
