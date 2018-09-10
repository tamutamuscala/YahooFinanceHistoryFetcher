package repo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.Materializer
import cats.effect.IO

import scala.language.higherKinds
import com.google.inject.{AbstractModule, Inject, Singleton}
import com.typesafe.config.Config
import repo.utils.AkkaHttpClientUtils

import scala.concurrent.{ExecutionContext, Future}

case class StockDf(date: String,
                   open: Double,
                   high: Double,
                   low: Double,
                   close: Double,
                   adjClose: Double,
                   volume: Long)

object StockDf {

  def mapDataToDf(data: String): Seq[StockDf] = {
    for {
      line <- data.split("\n").drop(1)
      stockDf = {
        val words = line.split(",")
        StockDf(
          date = words(0),
          open = words(1).toDouble,
          high = words(2).toDouble,
          low = words(3).toDouble,
          close = words(4).toDouble,
          adjClose = words(5).toDouble,
          volume = words(6).toLong
        )
      }
    } yield stockDf
  }
}

trait StockDao {

  /**
    * retrieves the CSV from Yahoo finance and convert to JSON object list
    *
    * @param ticker    the symbol of stock
    * @param startDate the start date in epoch
    * @param endDate   the end date in epoch
    * @param interval  should be one of 1d, 1w, 1y
    * @return returns a Future for fetching stock history
    */
  def getStockHistory(ticker: String,
                      startDate: Long,
                      endDate: Long,
                      interval: String): IO[Future[Seq[StockDf]]]
}

@Singleton
case class StockDaoImpl @Inject()(akkaHttpUtils: AkkaHttpClientUtils)(
  implicit config: Config,
  actorSystem: ActorSystem,
  materializer: Materializer,
  executionContext: ExecutionContext
) extends StockDao {

  override def getStockHistory(ticker: String,
                               startDate: Long,
                               endDate: Long,
                               interval: String): IO[Future[Seq[StockDf]]] = {
    IO {
      for {
        sessionResponse <- {
          val url = config.getString("url.getSession")
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.GET,
              uri = String.format(url, ticker)
            ).withHeaders(RawHeader("charset", "utf-8"))
          )
        }
        cookies <- akkaHttpUtils.getCookies(sessionResponse)
        sessionBody <- akkaHttpUtils.getBody(sessionResponse)
        crumb <- parseCrumb(sessionBody)
        response <- {
          val url = config.getString("url.getStockHistory")
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.GET,
              uri = String.format(
                url,
                ticker,
                startDate.toString,
                endDate.toString,
                interval,
                crumb
              )
            ).withHeaders(RawHeader("cookie", cookies.head))
          )
        }
        body <- akkaHttpUtils.getBody(response)
        stock = StockDf.mapDataToDf(body)
      } yield stock
    }
  }

  private def parseCrumb(sessionBody: String): Future[String] = {
    Future {
      val crumbString = """"CrumbStore":\{"crumb":"([^"]+)"\}""".r("crumb")
      crumbString
        .findFirstMatchIn(sessionBody)
        .get
        .group("crumb")
        .replaceAll("\\u002F", "/")
    }
  }
}

case class StockDaoModule() extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[StockDao]).to(classOf[StockDaoImpl])
  }
}
