package repo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.Materializer

import cats._
import cats.data._
import cats.syntax.applicative._
import cats.effect.IO
import scala.language.higherKinds
import com.google.inject.{AbstractModule, Inject, Singleton}
import com.typesafe.config.Config
import repo.utils.AkkaHttpClientUtils

import scala.concurrent.{ExecutionContext, Future}

case class StockDF
(date: String,
 open: Double,
 high: Double,
 low: Double,
 close: Double,
 adjClose: Double,
 volume: Long)

object StockDF {

  def mapDataToDF(data: String): Seq[StockDF] = {
    for {
      line <- data.split("\n").drop(1)
      stockDF = {
        val words = line.split(",")
        StockDF(
          date = words(0),
          open = words(1).toDouble,
          high = words(2).toDouble,
          low = words(3).toDouble,
          close = words(4).toDouble,
          adjClose = words(5).toDouble,
          volume = words(6).toLong
        )
      }
    } yield stockDF
  }
}

trait StockDAO {

  /**
    * retrieves the CSV from Yahoo finance and convert to JSON object list
    *
    * @param ticker    the symbol of stock
    * @param startDate the start date in epoch
    * @param endDate   the end date in epoch
    * @param interval  should be one of 1d, 1w, 1y
    * @return returns a Future for fetching stock history
    */
  def getStockHistory(ticker: String, startDate: Long, endDate: Long, interval: String): Future[IO[Seq[StockDF]]]
}

@Singleton
case class StockDAOImpl @Inject()
(akkaHttpUtils: AkkaHttpClientUtils)
(implicit config: Config,
 actorSystem: ActorSystem,
 materializer: Materializer,
 executionContext: ExecutionContext) extends StockDAO {

  type FutureStateM[M[+ _], +A] = StateT[M, Future, A]
  type FutureStateIO[+A] = FutureStateM[IO, A]

  override
  def getStockHistory(ticker: String,
                      startDate: Long,
                      endDate: Long,
                      interval: String): FutureStateIO[Seq[StockDF]] = for {
    // Future -> IO[Future]
    sessionResponse <- {
      val url = config.getString("url.getSession")
      Http().singleRequest(HttpRequest(
        method = HttpMethods.GET,
        uri = String.format(url, ticker)
      ).withHeaders(RawHeader("charset", "utf-8")))
    }.pure[FutureStateIO]
    cookies <- akkaHttpUtils.getCookies(sessionResponse)
    sessionBody <- akkaHttpUtils.getBody(sessionResponse)
    crumb = {
      val crumbString =
        """"CrumbStore":\{"crumb":"([^"]+)"\}""".r("crumb")
      crumbString.findFirstMatchIn(sessionBody).get
      .group("crumb")
      .replaceAll("\\u002F", "/")
    }
    response <- {
      val url = config.getString("url.getStockHistory")
      Http().singleRequest(HttpRequest(
        method = HttpMethods.GET,
        uri = String.format(url, ticker, startDate.toString, endDate.toString, interval, crumb)
      ).withHeaders(RawHeader("cookie", cookies.head)))
    }
    body <- akkaHttpUtils.getBody(response)
  } yield StockDF.mapDataToDF(body)
}

case class StockDAOModule() extends AbstractModule {

  override
  def configure(): Unit = {
    bind(classOf[StockDAO]).to(classOf[StockDAOImpl])
  }
}
