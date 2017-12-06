import akka.http.scaladsl.model.DateTime
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import repo.StockDAOImpl

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class StockModelSpec extends PropSpec with PropertyChecks with Matchers {

  // inject instances
  val stockDAO = Injector.globalInjector.getInstance(classOf[StockDAOImpl])

  property("getStockHistory") {
    //forAll { (n: Int, d: Int) =>
    //  whenever(true) {
    //  }
    //}

    // argument
    val ticker = "FB"
    val startDate = 0L
    val endDate = DateTime.now.clicks
    val interval = "1d"

    // process
    val response = Await.result(
      stockDAO.getStockHistory(ticker, startDate, endDate, interval),
      20 seconds
    ).unsafeRunSync()

    // assert
    response.size should be > 0
  }
}
