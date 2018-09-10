import akka.http.scaladsl.model.DateTime
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import repo.StockDaoImpl

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class StockModelSpec extends PropSpec with PropertyChecks with Matchers {

  // inject instances
  val stockDao = Injector.globalInjector.getInstance(classOf[StockDaoImpl])

  property("getStockHistory") {
    // argument
    val ticker = "FB"
    val startDate = 0L
    val endDate = DateTime.now.clicks
    val interval = "1d"

    // process
    val response = Await
      .result(
        stockDao.getStockHistory(ticker, startDate, endDate, interval).unsafeRunSync(),
        20 seconds
      )

    // assert
    response.size should be > 0
  }
}
